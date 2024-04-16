import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.*;

public class Test {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    public static void main(String[] args) {
        String path_to_access = filePath("Nginx");
        List<String> nginxLogData = getData(path_to_access);

        List<Integer> smth = new ArrayList<>();
        for(String log_data : nginxLogData){
            int commas = 0;
            for(int i = 0; i < log_data.length(); i++){
                if(log_data.charAt(i) == '\"'){
                    commas += 1 ;
                }
            }
            smth.add(commas);
        }

        Map<Integer, Integer> count = new LinkedHashMap<>();
        for(Integer s: smth){
            if(!count.containsKey(s)){
                count.put(s, 1);
            } else {
                int updatedCount = count.get(s) + 1;
                count.put(s, updatedCount);
            }
        }

        System.out.println(count);
    }

    private static List<String> getData(String path) {
        logger.info("Чтение из файла: " + path);
        List<String> read_All_Lines = null;
        try{
            read_All_Lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        }
        catch (IOException e){
            logger.log(Level.SEVERE, "Ошибки при чтении файла: " + path, e);
        }
        logger.info("Данные успешно прочитаны из файла " + path);
        return read_All_Lines;
    }

    private static String filePath(String text) {
        while(true){
            System.out.print("Введите имя/путь к " + text + " файлу: ");
            String fileName = scanner.nextLine();

            try{
                File file = new File(fileName);
                if(file.isAbsolute()){
                    return  file.getAbsolutePath();
                }
                else {
                    File[] files = new File(".").listFiles();
                    for(File file1: files){
                        if(file1.isFile() && file1.getName().equals(fileName)){
                            return file1.getAbsolutePath();
                        }
                    }
                }
                scanner.nextLine();
            }
            catch (Exception e){
                e.getMessage();
            }
        }
    }
}

