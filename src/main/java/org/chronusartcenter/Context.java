package org.chronusartcenter;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Context {
    public static final String CONFIG_FILE_PATH = "src/main/resources/config.json";

    private Logger logger = Logger.getLogger(Context.class);

    public JSONObject loadConfig() {
        try {
            Path path = Path.of(CONFIG_FILE_PATH);
            String content = Files.readString(path);
            JSONObject config = JSONObject.parseObject(content);
            return config;
        } catch (IOException exception) {
            logger.error(exception.toString());
            return null;
        }
    }

    public void saveConfig(JSONObject config) throws IOException {
        try {
            String content = config.toJSONString(JSONWriter.Feature.PrettyFormat);
            Path path = Path.of(CONFIG_FILE_PATH);
            Files.writeString(path, content);
        } catch (IOException exception) {
            logger.error(exception.toString());
        }

    }
}
