package org.chronusartcenter;

import com.alibaba.fastjson2.JSONObject;
import com.illposed.osc.MessageSelector;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCMessageEvent;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortIn;
import com.illposed.osc.transport.udp.OSCPortOut;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.chronusartcenter.cache.CacheService;
import org.chronusartcenter.news.HeadlineModel;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class OscServer {
    private final Context context;
    private final ArrayList<ImmutablePair<String, Integer>> oscClients = new ArrayList<>();

    private final Logger logger = Logger.getLogger(Context.class);

    public OscServer(Context context) {
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

            // block current thread
//            waitLock.wait();
//            while (true) {
//                Thread.sleep(1000);
//            }
        } catch (IOException exception) {
            logger.error(exception.toString());
        }
    }

    private void onPing() {
        // notify gui that osc is online
    }

    private void onReady(int id) {
        if (id > oscClients.size()) {
            return;
        }
        // bad code
        String ip = oscClients.get(id - 1).left;
        int port = oscClients.get(id - 1).right;
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

    private void readClientConfig(Context context, ArrayList<ImmutablePair<String, Integer>> oscClients) {
        var clientsJson = context.loadConfig().getJSONArray("oscClient");
        if (clientsJson == null) {
            logger.error("No config of osc clients.");
            return;
        }

        for (Object clientConfig : clientsJson) {
            if (!(clientConfig instanceof JSONObject)) {
                logger.warn("Invalid osc clients config.");
                // add default ip and port
                oscClients.add(new ImmutablePair<>("127.0.0.1", 5001));
                break;
            }
            String ip = ((JSONObject) clientConfig).getString("ip");
            int port = ((JSONObject) clientConfig).getIntValue("port");
            oscClients.add(new ImmutablePair<>(ip, port));
        }
    }
}
