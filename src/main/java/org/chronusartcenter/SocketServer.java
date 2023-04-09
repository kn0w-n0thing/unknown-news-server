package org.chronusartcenter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
import org.apache.log4j.Logger;
import org.chronusartcenter.cache.CacheService;
import org.chronusartcenter.dalle.DalleService;
import org.chronusartcenter.network.ServerWrapper;
import org.chronusartcenter.news.NewsService;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.chronusartcenter.ServiceManager.SERVICE_TYPE.OSC_SERVICE;

public class SocketServer {
    private final Context context;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private static final String GUI_LOG_EVENT = "gui-log";
    private static final String GET_NEWS_DATA_EVENT = "get-news-data";
    private static final String LOAD_OSC_CONFIG_EVENT = "load-osc-config";
    private static final String SAVE_OSC_CONFIG_EVENT = "save-osc-config";

    private final Logger logger = Logger.getLogger(SocketServer.class);

    public SocketServer(Context context) {
        this.context = context;
    }

    private void listenEvents(SocketIoSocket socket) {
        if (socket == null) {
            return;
        }

        socket.on("disconnect", args -> {
            logger.info("Client " + socket.getId() + " (" + socket.getInitialHeaders().get("remote_addr") + ") " +
                    "has disconnected. Reason: " + args[0]);
        });

        socket.on(GET_NEWS_DATA_EVENT, args -> {
            logger.info("get-news-data, isProcessing: " + isProcessing.get());
            if (isProcessing.get()) {
                logger.info("Ignore get-news-data");
                socket.send(GET_NEWS_DATA_EVENT, false, "Processing, please wait...");
                return;
            }

            isProcessing.set(true);
            socket.send(GUI_LOG_EVENT, "Start to process.");
            NewsService newsService = new NewsService(context);
            var news = newsService.translateHeadlines(newsService.fetchHeadlines());

            if (news == null || news.size() == 0) {
                socket.send(GET_NEWS_DATA_EVENT, true, "Failed to get headlines. Please check local server!");
                isProcessing.set(false);
                return;
            }

            DalleService dalleService = new DalleService(context);
            CacheService cacheService = new CacheService(context);

            for (int index = 0; index < news.size(); index++) {
                var image = dalleService.generateImage(news.get(index).getTranslation(), 1);
                news.get(index).setIndex(index);
                cacheService.saveImage(index + "." + image.right, image.left.get(0));
                cacheService.saveHeadline(news.get(index));
            }

            socket.send(GET_NEWS_DATA_EVENT, true, "Completed.");
            isProcessing.set(false);
        });

        socket.on(LOAD_OSC_CONFIG_EVENT, arg -> {
            var oscConfig = context.loadConfig().getJSONArray("oscClient");
            socket.send(LOAD_OSC_CONFIG_EVENT, JSON.toJSONString(oscConfig));
            logger.info(LOAD_OSC_CONFIG_EVENT + ": " + oscConfig);
        });

        socket.on(SAVE_OSC_CONFIG_EVENT, arg -> {
            logger.info(SAVE_OSC_CONFIG_EVENT + ": " + arg[0]);
            var config = context.loadConfig();
            config.put("oscClient", JSON.parseArray((String) arg[0]));
            logger.info(config.toJSONString());
            try {
                context.saveConfig(config);
                socket.send(GUI_LOG_EVENT, "Success to save osc config.");
            } catch (IOException e) {
                logger.error(e.toString());
                socket.send(GUI_LOG_EVENT, "Failed to save osc config. Please check local server.");
            }
            OscService oscService = (OscService) ServiceManager.getInstance().getService(OSC_SERVICE);
            if (oscService != null) {
                oscService.updateOscClient();
            } else {
                logger.warn("OscService is null");
            }
        });

        socket.on("dalle-status", arg -> {
            DalleService dalleService = new DalleService(context);
            socket.send("dalle-status", dalleService.check());
        });
        socket.on("shutdown", arg -> {
            OscService oscService = (OscService) ServiceManager.getInstance().getService(OSC_SERVICE);
            if (oscService != null) {
                oscService.shutDownOscClients();
            } else {
                logger.warn("OscService is null");
            }
        });
    }

    public void start() {
        int port;
        JSONObject config = context.loadConfig();
        if (config.get("socketIoServer") == null
                && config.getJSONObject("socketIoServer").getInteger("port") == null) {
            port = 4000;
        } else {
            port = config.getJSONObject("socketIoServer").getInteger("port");
        }

        final ServerWrapper serverWrapper = new ServerWrapper("127.0.0.1", port, null);
        try {
            serverWrapper.startServer();
        } catch (Exception e) {
            logger.error(e.toString());
        }

        SocketIoServer server = serverWrapper.getSocketIoServer();
        SocketIoNamespace namespace = server.namespace("/");

        namespace.on("connection", args -> {
            SocketIoSocket socket = (SocketIoSocket) args[0];
            logger.info("Client " + socket.getId() + " (" + socket.getInitialHeaders().get("remote_addr") + ") has connected.");
            listenEvents(socket);
        });
    }
}
