package org.chronusartcenter.news;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.log4j.Logger;
import org.chronusartcenter.Context;
import org.chronusartcenter.network.OkHttpWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BaiduTranslate {
    private final Context context;
    private static final String SEPARATOR = "\n\n";

    private Logger logger = Logger.getLogger(BaiduTranslate.class);

    public BaiduTranslate(Context globalContext) {
        this.context = globalContext;
    }

    // TODO: cache access token locally
    public String getAccessToken() throws IOException {

        if (context == null
                || context.loadConfig() == null
                || context.loadConfig().getJSONObject("baidu") == null) {
            logger.error("No api key and secret key for baidu translation!");
            return "";
        }

        String apiKey = context.loadConfig().getJSONObject("baidu").getString("apiKey");
        String secretKey = context.loadConfig().getJSONObject("baidu").getString("secretKey");

        OkHttpWrapper httpWrapper = new OkHttpWrapper();
        final String url = "https://aip.baidubce.com/oauth/2.0/token?client_id="
                + apiKey + "&client_secret=" + secretKey + "&grant_type=client_credentials";
        String response = httpWrapper.post(url, "");

        return JSONObject.parseObject(response).getString("access_token");
    }

    public List<String> translateRpc(String text) throws Exception {
        ArrayList<String> result = new ArrayList<>();
        String accessToken = getAccessToken();

        if (accessToken == null || accessToken.isEmpty()) {
            throw new Exception("Access token is invalid!");
        }

        OkHttpWrapper httpWrapper = new OkHttpWrapper();
        final String url = "https://aip.baidubce.com/rpc/2.0/mt/texttrans/v1?access_token=" + accessToken;
        JSONObject bodyObject = new JSONObject();
        bodyObject.put("q", text);
        bodyObject.put("from", "zh");
        bodyObject.put("to", "en");
        String response = httpWrapper.post(url, bodyObject.toString());

        if (JSONObject.parseObject(response) == null
                || JSONObject.parseObject(response).getJSONObject("result") == null) {
            return result;
        }
        JSONArray jsonArray = JSONObject.parseObject(response).getJSONObject("result").getJSONArray("trans_result");

        for (Object transResult : jsonArray) {
            String translated = ((JSONObject) transResult).getString("dst");
            result.add(translated);
        }
        return result;
    }

    // Referring to the document of Baidu, input limit size of one translate RPC is 6000 BYTES, namely 3000 CHARACTERS.
    // In case of rpc failure, here we set the limit to 2000 CHARACTERS.
    public List<String> translate(List<String> textList) {
        if (textList == null || textList.size() == 0) {
            logger.info("Empty text list to translate.");
            return null;
        }

        final int TRANSLATE_CHARACTER_LENGTH_LIMIT = 2000;
        ArrayList<String> results = new ArrayList<>();
        int startIndex = 0, endIndex = 0;
        StringBuilder stringBuilder = new StringBuilder();

        try {
            while (startIndex < textList.size()) {
                if (endIndex >= textList.size()) {
                    results.addAll(translateRpc(stringBuilder.toString()));
                    break;
                }

                if (stringBuilder.length() + textList.get(endIndex).length() < TRANSLATE_CHARACTER_LENGTH_LIMIT) {
                    stringBuilder.append(textList.get(endIndex) + SEPARATOR);
                    endIndex++;
                } else {
                    results.addAll(translateRpc(stringBuilder.toString()));
                    startIndex = endIndex;
                    stringBuilder.delete(0, stringBuilder.length());
                }
            }
        } catch (Exception exception) {
            logger.error(exception.toString());
        }
        return results;
    }
}
