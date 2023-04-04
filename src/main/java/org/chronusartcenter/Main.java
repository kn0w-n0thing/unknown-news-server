package org.chronusartcenter;

import com.alibaba.fastjson2.JSONObject;
import org.chronusartcenter.news.NewsService;

public class Main {
    public static void main(String[] args) {
        Context context = new Context();
        try {
            NewsService news = new NewsService(context);
            System.out.println(news.translateHeadlines(news.fetchHeadlines()).size());
            for (var headline : news.translateHeadlines(news.fetchHeadlines())) {
                System.out.println(JSONObject.toJSONString(headline));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
