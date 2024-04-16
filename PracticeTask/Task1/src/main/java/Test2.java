import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test2 {
    static Scanner input = new Scanner(System.in);
    static DecimalFormat formatter = new DecimalFormat("0.00");

    public static void main(String[] args) {
        double a = Double.parseDouble("53.5555");
        double b = ChooseSeconds();
        System.out.println(a-b);
    }

    private static double ChooseSeconds() {
        double seconds = 0;
        while (true) {
            System.out.println("Введите кол-во секунд (не более 10 секунд): ");
            if (input.hasNextDouble()) {
                seconds = input.nextDouble();
                if (seconds <= 10 && seconds >= 0) {
                    return Double.parseDouble(formatter.format(seconds).replace(',','.'));
                } else {
                    System.out.println("Ошибка: количество секунд должно быть от 0 до 10.");
                }
            } else {
                System.out.println("Ошибка: введите время (секунды) в формате ss.SS.");
                input.next();
            }
        }
    }
}
