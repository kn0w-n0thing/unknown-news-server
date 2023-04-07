package org.chronusartcenter;

import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
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

    public SocketServer(Context context) {
        this.context = context;
    }

    private void listenEvents(SocketIoSocket socket) {
        if (socket == null) {
            return;
        }

        socket.on("disconnect", args -> {
            // TODO: log
            System.out.println("Client " + socket.getId() + " (" + socket.getInitialHeaders().get("remote_addr") + ") has disconnected.");
            System.out.println("Reason: " + args[0]);
        });

        socket.on(GET_NEWS_DATA_EVENT, args -> {
            System.out.println("get-news-data, isProcessing: " + isProcessing.get());
            if (isProcessing.get()) {
                System.out.println("Ignore request");
                socket.send(GET_NEWS_DATA_EVENT, false, "Processing, please wait...");
                return;
            }

            isProcessing.set(true);
            socket.send(GUI_LOG_EVENT, "Start to process.");
            System.out.println("on get-news-data");
            NewsService newsService = new NewsService(context);
            var news = newsService.translateHeadlines(newsService.fetchHeadlines());
            System.out.println(news);
            // make dalle service singleton
            DalleService dalleService = new DalleService();
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
            // TODO.log
            System.out.println("On dalle-status");
            DalleService dalleService = new DalleService();
            socket.send("dalle-status", dalleService.check());
        });
        socket.on("shutdown", arg -> {});
    }

    public void start() {
        // TODO: read port from config file
        final ServerWrapper serverWrapper = new ServerWrapper("127.0.0.1", 4000, null);
        try {
            serverWrapper.startServer();
        } catch (Exception e) {
            // TODO: log
            e.printStackTrace();
        }
        SocketIoServer server = serverWrapper.getSocketIoServer();
        SocketIoNamespace namespace = server.namespace("/");

        namespace.on("connection", args -> {
            SocketIoSocket socket = (SocketIoSocket) args[0];
            // TODO: log
            System.out.println("Client " + socket.getId() + " (" + socket.getInitialHeaders().get("remote_addr") + ") has connected.");
            listenEvents(socket);
        });
    }
}
