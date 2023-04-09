package org.chronusartcenter;


import org.apache.log4j.Logger;

import static org.chronusartcenter.ServiceManager.SERVICE_TYPE.OSC_SERVICE;

public class Main {

    private static Logger logger = Logger.getLogger(Main.class);
    public static void main(String[] args) {
        Context context = new Context();
        try {
            SocketServer server = new SocketServer(context);
            server.start();
            new Thread(() -> {
                OscService oscService = (OscService) ServiceManager.getInstance().getService(OSC_SERVICE);
                oscService.start();
            }).start();
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }
}
