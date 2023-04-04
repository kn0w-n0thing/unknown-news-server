import com.alibaba.fastjson2.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GlobalContext {
    public static final String CONFIG_FILE_PATH = "src/main/resources/config.json";
    public JSONObject loadConfig() {
        try {
            Path path = Path.of(CONFIG_FILE_PATH);
            String content = Files.readString(path);
            JSONObject config = JSONObject.parseObject(content);
            return config;
        } catch (IOException exception) {
            // TODO: log
            return null;
        }
    }

    public void saveConfig(JSONObject config)throws IOException {
        try {
            String content = config.toString();
            Path path = Path.of(CONFIG_FILE_PATH);
            Files.writeString(path, content);
        } catch (IOException exception) {
            // TODO: log
        }

    }
}
