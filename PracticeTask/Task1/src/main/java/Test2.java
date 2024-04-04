import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;

public class Test2 {
    public static void main(String[] args) {
//        JSONObject json = new JSONObject();
//        List<String> listOfName = new ArrayList<>();
//        listOfName.add("Mahesh");
//        listOfName.add("Nilesh");
//        json.put("101", listOfName);
//        json.put("103", "Jugesh");
//        json.put("102", "Yulia");
//        System.out.println(json);
        Map<String, Object> jsonMap = new LinkedHashMap<>();

        List<String> listOfName = new ArrayList<>();
        listOfName.add("Mahesh");
        listOfName.add("Nilesh");
        jsonMap.put("101", listOfName);
        jsonMap.put("103", "Jugesh");
        jsonMap.put("102", "Yulia");

//        JSONObject json = new JSONObject();
//        json.put("101", listOfName);
//        json.put("103", "fff");
//        json.put("102", "fff");

        // Создаем JSONObject из LinkedHashMap
//        JSONObject json = new JSONObject(jsonMap);
//        System.out.println(json);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Преобразуем Map в JSON строку
        String jsonString = gson.toJson(jsonMap);

        // Выводим JSON строку
        System.out.println(jsonString);

    }
}
