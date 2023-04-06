package org.chronusartcenter;

import com.alibaba.fastjson2.JSONObject;
import org.chronusartcenter.news.NewsService;

public class Main {
    public static void main(String[] args) {
        Context context = new Context();
        try {
            Server server = new Server();
            server.start();
        } catch (Exception e) {
            // TODO: log
            e.printStackTrace();
        }
    }
}
