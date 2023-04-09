package org.chronusartcenter;

import com.alibaba.fastjson2.JSONObject;
import com.illposed.osc.MessageSelector;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageEvent;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortOut;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.log4j.Logger;
import org.chronusartcenter.cache.CacheService;
import org.chronusartcenter.news.HeadlineModel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class OscService {
    private final Context context;
    // Triple(id, ip, port)
    private final ArrayList<ImmutableTriple<Integer, String, Integer>> oscClients = new ArrayList<>();

    private final Logger logger = Logger.getLogger(Context.class);

    public OscService(Context context) {
        this.context = context;
        readClientConfig(context, oscClients);
    }

    public void start() {

        try {
            int port;
            JSONObject config = context.loadConfig();
            if (config.get("oscServer") == null
                    && config.getJSONObject("oscServer").getInteger("port") == null) {
                port = 4003;
            } else {
                port = config.getJSONObject("oscServer").getInteger("port");
            }
            OSCPortIn server = new OSCPortIn(port);
            logger.info("Start osc server at localhost: " + port );
            server.getDispatcher().addListener(new MessageSelector() {
                @Override
                public boolean isInfoRequired() {
                    return false;
                }

                @Override
                public boolean matches(OSCMessageEvent oscMessageEvent) {
                    String from = oscMessageEvent.getMessage().getAddress();
                    if (from.startsWith("/screen")) {
                        return true;
                    } else {
                        logger.warn("Unknown osc from: " + from);
                        return false;
                    }
                }
            }, oscMessageEvent -> {

                String from = oscMessageEvent.getMessage().getAddress();
                logger.info("Receive osc message, from: " + from);
                // screen ID comes after "/screen"
                int screenId;
                try {
                    screenId = Integer.parseInt(from.substring("/screen".length()));
                } catch (NumberFormatException e) {
                    logger.error("Failed to parse screen id from [" + from + "]");
                    return;
                }

                var params = oscMessageEvent.getMessage().getArguments();
                if ("ready".equals(params.get(0))) {
                    onReady(screenId);
                } else if ("ping".equals(params.get(0))) {
                    onPing();
                } else {
                    logger.warn("Unhandled osc parameter: " + params.get(0));
                }

            });
            server.startListening();

        } catch (IOException exception) {
            logger.error(exception.toString());
        }
    }

    public void updateOscClient() {
        readClientConfig(context, oscClients);
    }

    public void shutDownOscClients() {
        for (var client : oscClients) {
            try {
                OSCPortOut oscPortOut = new OSCPortOut(InetAddress.getByName(client.middle), client.right);
                final OSCMessage msg = new OSCMessage("/shutdown" + client.left);
                oscPortOut.send(msg);
            } catch (IOException | OSCSerializeException e) {
                logger.error(e.toString());
            }
        }
    }

    private void onPing() {
        // notify gui that osc is online
    }

    private void onReady(int id) {
        var oscClient = oscClients.stream().filter(value -> value.left == id).toList();
        if (oscClient.size() == 0) {
            logger.error("id " + id + " doesn't exists!");
            return;
        } else if (oscClient.size() > 1) {
            logger.error("id " + id + " is duplicated!");
            return;
        }

        String ip = oscClient.get(0).middle;
        int port = oscClient.get(0).right;
        try {
            OSCPortOut client = new OSCPortOut(InetAddress.getByName(ip), port);
            final List<Object> arg = new LinkedList<>();
            var headline = getRandomHeadline();
            logger.info("Send to Screen " + id
                    + "of socket: " + client.getRemoteAddress()
                    + " of  headline: " + JSONObject.toJSONString(headline));
            arg.add(headline.getTitle());
            arg.add(":3000/image/" + headline.getIndex() + ".jpeg");
            final OSCMessage msg = new OSCMessage("/headline-" + id, arg);
            client.send(msg);
            logger.info("Title: " + headline.getTitle()
                    + ", image link: " + ":3000/image/" + headline.getIndex() + ".jpeg");
        } catch (IOException | OSCSerializeException exception) {
            logger.error(exception.toString());
        }
    }

    private HeadlineModel getRandomHeadline() {
        CacheService cacheService = new CacheService(context);
        var headlines = cacheService.loadHeadlines();

        Random rand = new Random(System.currentTimeMillis());
        return headlines.get(rand.nextInt(headlines.size()));
    }

    private void readClientConfig(Context context, ArrayList<ImmutableTriple<Integer, String, Integer>> oscClients) {
        var clientsJson = context.loadConfig().getJSONArray("oscClient");
        if (clientsJson == null) {
            logger.error("No config of osc clients.");
            return;
        }

        for (Object clientConfig : clientsJson) {
            if (!(clientConfig instanceof JSONObject)) {
                logger.warn("Invalid osc clients config: " + clientConfig.toString());
                break;
            }
            int id = ((JSONObject) clientConfig).getIntValue("id");
            String ip = ((JSONObject) clientConfig).getString("ip");
            int port = ((JSONObject) clientConfig).getIntValue("port");
            oscClients.add(new ImmutableTriple<>(id, ip, port));
        }
    }
}
