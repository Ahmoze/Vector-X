import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.ahmoze.vector.manager.repo.model.OnlineModule;

public class TestGsonFinal {
    private static class ModuleListResponse {
        public List<OnlineModule> modules;
        public int pageCount;
    }
    public static void main(String[] args) throws Exception {
        String bodyString = new String(Files.readAllBytes(Paths.get("modules.json")), "UTF-8");
        Gson gson = new Gson();
        try {
            java.lang.reflect.Type type = new TypeToken<List<OnlineModule>>(){}.getType();
            List<OnlineModule> res = gson.fromJson(bodyString, type);
            System.out.println("Parsed as List: " + (res != null ? res.size() : "null"));
        } catch (Exception e) {
            System.out.println("Failed to parse as List: " + e.getMessage());
            try {
                ModuleListResponse res = gson.fromJson(bodyString, ModuleListResponse.class);
                System.out.println("Parsed as ModuleListResponse: " + (res != null && res.modules != null ? res.modules.size() : "null"));
            } catch (Exception e2) {
                System.out.println("Failed to parse as ModuleListResponse: " + e2.getMessage());
                e2.printStackTrace();
            }
        }
    }
}
