package org.chronusartcenter;


import org.apache.log4j.Logger;

public class Main {

    private static Logger logger = Logger.getLogger(Main.class);
    public static void main(String[] args) {
        Context context = new Context();
        try {
            SocketServer server = new SocketServer(context);
            server.start();
            new Thread(() -> {
                OscServer oscServer = new OscServer(context);
                oscServer.start();
            }).start();
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }
}
