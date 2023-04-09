package org.chronusartcenter.dalle;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.chronusartcenter.Context;
import org.chronusartcenter.network.OkHttpWrapper;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DalleService {
    public static final int TIMEOUT_SEC = 100;

    private static String url;

    private final Logger logger = Logger.getLogger(DalleService.class);

    public DalleService(Context context) {
        if (context == null
            || context.loadConfig() == null
                || context.loadConfig().getString("dalleServerUrl") == null) {
            url = "http://127.0.0.1:8080";
        } else {
            url = context.loadConfig().getString("dalleServerUrl");
        }
    }

    public boolean check() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            return response.isSuccessful();
        } catch (IOException exception) {
            logger.info("dalle service is offline: " + exception.toString());
            return false;
        }
    }

    // return: List of images encoded by Base64 and their format
    public ImmutablePair<List<String>, String> generateImage(String text, int count) {
        JSONObject json = new JSONObject();
        json.put("text", text);
        json.put("num_images", count);
        Headers headers = new Headers.Builder()
                .add("Bypass-Tunnel-Reminder", "go")
                .build();

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(TIMEOUT_SEC, TimeUnit.SECONDS)
                .build();

        RequestBody body = RequestBody.create(json.toString(), OkHttpWrapper.JSON);
        Request request = new Request.Builder()
                .url(url + "/generate")
                .post(body)
                .headers(headers)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            JSONObject jsonObject = (JSONObject) JSON.parse(response.body().string());
            if (jsonObject != null) {
                var imageBase64List = jsonObject.getJSONArray("generatedImgs").stream().map(JSONObject::toJSONString).toList();
                return new ImmutablePair<>(imageBase64List, jsonObject.getString("generatedImgsFormat"));
            }
            return null;
        } catch (IOException exception) {
            logger.error(exception.toString());
            return null;
        }
    }
}
