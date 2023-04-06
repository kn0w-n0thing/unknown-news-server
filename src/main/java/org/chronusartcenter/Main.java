package org.chronusartcenter;

import com.alibaba.fastjson2.JSONObject;
import org.chronusartcenter.dalle.DalleService;
import org.chronusartcenter.news.NewsService;

public class Main {
    public static void main(String[] args) {
        Context context = new Context();
        try {
//            Server server = new Server();
//            server.start();
            DalleService dalleService = new DalleService();
            System.out.println(dalleService.generateImage("Donald Trump loves China", 1));
        } catch (Exception e) {
            // TODO: log
            e.printStackTrace();
        }
    }
}
