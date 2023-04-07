package org.chronusartcenter;

public class Main {
    public static void main(String[] args) {
        Context context = new Context();
        try {
            SocketServer server = new SocketServer(context);
            server.start();
        } catch (Exception e) {
            // TODO: log
            e.printStackTrace();
        }
    }
}
