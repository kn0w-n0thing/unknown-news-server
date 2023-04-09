package org.chronusartcenter;

import com.alibaba.fastjson2.JSONObject;
import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
import org.apache.log4j.Logger;
import org.chronusartcenter.cache.CacheService;
import org.chronusartcenter.dalle.DalleService;
import org.chronusartcenter.network.ServerWrapper;
import org.chronusartcenter.news.NewsService;

import java.util.concurrent.atomic.AtomicBoolean;

public class SocketServer {
    private final Context context;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private static final String GUI_LOG_EVENT = "gui-log";
    private static final String GET_NEWS_DATA_EVENT = "get-news-data";

    private final Logger logger = Logger.getLogger(SocketServer.class);

    public SocketServer(Context context) {
        this.context = context;
    }

    private void listenEvents(SocketIoSocket socket) {
        if (socket == null) {
            return;
        }

        socket.on("disconnect", args -> {
            logger.info("Client " + socket.getId() + " (" + socket.getInitialHeaders().get("remote address") + ") " +
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

            DalleService dalleService = new DalleService(context);
            CacheService cacheService = new CacheService(context);

            for (int index = 0; index < news.size(); index++) {
                var image = dalleService.generateImage(news.get(index).getTranslation(), 1);
                news.get(index).setIndex(index);
                cacheService.saveImage(index + "." + image.right, image.left.get(0));
            }

            cacheService.saveHeadlines(news.stream().filter(headlineModel -> headlineModel.getIndex() >= 0).toList());

            socket.send(GET_NEWS_DATA_EVENT, true, "Completed.");
            isProcessing.set(false);
        });

        socket.on("get-local-data", arg -> {});
        socket.on("save-config", arg -> {});
        socket.on("dalle-status", arg -> {
            DalleService dalleService = new DalleService(context);
            socket.send("dalle-status", dalleService.check());
        });
        socket.on("shutdown", arg -> {});
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
            logger.info("Client " + socket.getId() + " (" + socket.getInitialHeaders().get("remote address") + ") has connected.");
            listenEvents(socket);
        });
    }
}
