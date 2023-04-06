package org.chronusartcenter;

import io.socket.socketio.server.SocketIoNamespace;
import io.socket.socketio.server.SocketIoServer;
import io.socket.socketio.server.SocketIoSocket;
import org.chronusartcenter.network.ServerWrapper;

public class Server {
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

            socket.on("message", arg -> {
                System.out.println("[Client " + socket.getId() + "] " + arg);
                socket.send("message", "test message", 1);
            });

            socket.on("disconnect", arg -> {

            });

            socket.on("get-remote-data", arg -> {});
            socket.on("get-local-data", arg -> {});
            socket.on("save-config", arg -> {});
            socket.on("runwayml-status", arg -> {});
            socket.on("shutdown", arg -> {});

        });
    }
}
