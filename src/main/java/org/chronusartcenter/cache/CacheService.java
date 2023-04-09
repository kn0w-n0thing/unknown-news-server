package org.chronusartcenter.cache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.chronusartcenter.Context;
import org.chronusartcenter.news.HeadlineModel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.alibaba.fastjson2.JSONWriter.Feature.PrettyFormat;

public class CacheService {
    private final Context context;

    private Logger logger = Logger.getLogger(CacheService.class);

    public CacheService(Context context) {
        this.context = context;
    }

    public String saveHeadline(HeadlineModel insertHeadline) {
        if (insertHeadline == null) {
            return null;
        }

        var headlineList = loadHeadlines();
        for (var headline : headlineList) {
            if (headline.getIndex() == insertHeadline.getIndex()) {
                headline.set(insertHeadline);
                break;
            }
        }
        return saveHeadlines(headlineList);
    }

    public String saveHeadlines(List<HeadlineModel> headlineModelList) {
        if (headlineModelList == null || headlineModelList.size() == 0) {
            return null;
        }

        String directory = getHeadlineDirectory();
        // The directory doesn't exist but fail to create it!
        if (!createDirectoryIfNecessary(directory)) {
            return null;
        }

        // Overwrite old cache file if it exists!
        try (BufferedWriter writer =
                     new BufferedWriter(new FileWriter(directory + File.separator + getHeadlineFileName()))) {
            String jsonString = JSON.toJSONString(headlineModelList, PrettyFormat);
            writer.write(jsonString);
            return jsonString;
        } catch (IOException exception) {
            logger.error(exception.toString());
            return null;
        }
    }

    public List<HeadlineModel> loadHeadlines() {
        try (InputStream stream =
                     new FileInputStream(getHeadlineDirectory() + File.separator + getHeadlineFileName())) {
            byte buffer[] = new byte[stream.available()];
            stream.read(buffer);
            String jsonString = new String(buffer);
            JSONArray jsonArray = JSON.parseArray(jsonString);
            ArrayList<HeadlineModel> list = new ArrayList<>();
            for (var headlineJson : jsonArray) {
                list.add(((JSONObject)headlineJson).toJavaObject(HeadlineModel.class));
            }
            return list;
        } catch (IOException exception) {
            logger.error(exception.toString());
            return null;
        }
    }

    public void saveImage(String fileName, String base64Image) {
        // Bad design due to adaption to the osc client!!!
        String directory = "../unknown-gui/public/image";
        if (!createDirectoryIfNecessary(directory)) {
            return;
        }

        try ( OutputStream stream = new FileOutputStream(directory + File.separator + fileName) ){
            byte[] decodedBytes = Base64.decodeBase64(base64Image.getBytes(StandardCharsets.UTF_8));
            stream.write(decodedBytes);
        } catch (IOException exception) {
            logger.error(exception.toString());
        }
    }

    public String loadImage(String fileName) {
        String directory = "src/main/resources/cache/image";
        try (InputStream stream = new FileInputStream(directory + File.separator + fileName)) {
            byte byteImage[] = new byte[stream.available()];
            stream.read(byteImage);
            return new String(Base64.encodeBase64(byteImage));
        } catch (IOException exception) {
            logger.error(exception.toString());
            return null;
        }
    }

    private Path getHeadlinePath() throws InvalidPathException {
        String headlinePath = context.loadConfig().getString("headlinePath");
        return Paths.get(headlinePath);
    }
    private String getHeadlineDirectory() {
        try {
            return getHeadlinePath().getParent().toString();
        } catch (InvalidPathException exception) {
            return "src/main/resources/cache";
        }
    }

    private String getHeadlineFileName() {
        try {
            return getHeadlinePath().getFileName().toString();
        } catch (InvalidPathException exception) {
            return "headlines.json";
        }
    }

    private boolean createDirectoryIfNecessary(String directory) {
        File cacheDirectory = new File(directory);
        return cacheDirectory.exists() || cacheDirectory.mkdir();
    }


}
