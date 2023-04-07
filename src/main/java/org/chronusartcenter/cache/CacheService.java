package org.chronusartcenter.cache;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.codec.binary.Base64;
import org.chronusartcenter.Context;
import org.chronusartcenter.news.HeadlineModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat;

public class CacheService {
    private final Context context;

    public CacheService(Context context) {
        this.context = context;
    }

    public String saveHeadlines(List<HeadlineModel> headlineModelList) {
        if (headlineModelList == null || headlineModelList.size() == 0) {
            return null;
        }

        // TODO: get from config file
        String directory = "src/main/resources/cache";
        // The directory doesn't exist but fail to create it!
        if (!createDirectoryIfNecessary(directory)) {
            return null;
        }

        // Overwrite old cache file if it exists!
        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(directory + File.separator + "headlines.json"))) {
            String jsonString = JSON.toJSONString(headlineModelList, PrettyFormat);
            writer.write(jsonString);
            return jsonString;
        } catch (IOException exception) {
            // TODO: log
            System.out.println(exception);
            return null;
        }
    }

    public void saveImage(String fileName, String base64Image) {
        // TODO: get from config file
        String directory = "src/main/resources/cache/image";
        if (!createDirectoryIfNecessary(directory)) {
            return;
        }

        try ( OutputStream stream = new FileOutputStream(directory + File.separator + fileName) ){
            byte[] decodedBytes = Base64.decodeBase64(base64Image.getBytes(StandardCharsets.UTF_8));
            stream.write(decodedBytes);
        } catch (IOException exception) {
            // TODO: log
        }
    }

    private boolean createDirectoryIfNecessary(String directory) {
        File cacheDirectory = new File(directory);
        return cacheDirectory.exists() || cacheDirectory.mkdir();
    }
}
