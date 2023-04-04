import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        GlobalContext context = new GlobalContext();
        try {
            System.out.println("start");
            BaiduTranslate baiduTranslate = new BaiduTranslate(context);
//            System.out.println(baiduTranslate.getAccessToken());
            List<String> input = new ArrayList<>();
            input.add("今天天气不错");
            input.add("我们出去玩吧");
            input.add("今天天气不错");
            input.add("今天天气不错");
            input.add("今天天气不错");
            var results = baiduTranslate.translate(input);
            for (var result : results) {
                System.out.println(result);
            }
            System.out.println("end");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
