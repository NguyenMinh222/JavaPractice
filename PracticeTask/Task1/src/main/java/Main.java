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
import org.checkerframework.checker.regex.qual.Regex;
import org.json.JSONArray;
import org.json.JSONObject;

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static SimpleFormatter formatter = new SimpleFormatter();
    private static FileHandler fileHandler;

    public static void main(String[] args) throws IOException, ParseException {
//        if(args.length < 3){
//            System.out.println("Необходимо указать пути к файлам: access.log, mysql-slow.log, localhost_access_log.2024-03-05.txt");
//            return;
//        }
//
//        String path_to_access = args[0];
//        String path_to_mysql = args[1];
//        String path_to_tomcat = args[2];
        fileHandler = new FileHandler("Main.log");
        fileHandler.setFormatter(formatter);
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL);

        String path_to_access = "D:\\PracticeTask\\nginx\\access.log";
        String path_to_mysql = "D:\\PracticeTask\\mysql\\mysql-slow.log";
        String path_to_tomcat = "D:\\PracticeTask\\tomсat\\localhost_access_log.2024-03-05.txt";

        List<String> nginxLogData = getData(path_to_access);
        List<String> tomcatLogData = getData(path_to_tomcat);
        List<String> mysqlLogData = getData(path_to_mysql);

        Map<String, List<String>> nginxLogRequests = getRequests(nginxLogData);
        Map<String, List<String>> tomcatLogRequests = getRequests(tomcatLogData);
        Map<String, List<String>> mysqlRequests = getMysqlRequests(mysqlLogData);

        Map<String, JSONObject> getMatchedRequests = matchedRequests(nginxLogRequests, tomcatLogRequests, mysqlRequests);
        writeToJSON("MatchedRequests.json", getMatchedRequests);

        String startDateStr = checkTheRightDate("начальную");
        String endDateStr = checkTheRightDate("конечную");;
        while(checkDiffBetweenEnd_and_Start(startDateStr, endDateStr)){
            if (checkDiffBetweenEnd_and_Start(startDateStr, endDateStr)) {
                System.out.println("Конечная дата и время не может быть раньше начального времени!");
                endDateStr = checkTheRightDate("конечную");
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");
        Date startDate = dateFormat.parse(startDateStr);
        Date endDate = dateFormat.parse(endDateStr);

        Map<String, JSONObject> getRequestsByTime = FindOutOfPeriod(getMatchedRequests, startDate, endDate);
        writeToJSON("getRequestsByTime.json", getRequestsByTime);
    }

    private static void writeToJSON(String nameOfFile, Map<String, JSONObject> Requests){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(Requests);

        try (FileWriter writeToJSON = new FileWriter(nameOfFile)) {
            writeToJSON.write(json);
            logger.info("Объекты были добавлены в JSON file.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при записи объектов в JSON файл: " + nameOfFile, e);
        }
    }

    private static String checkTheRightDate(String typeOfDate){
        //String RegexDate = "\\d{2}.\\d{2}.\\d{4}-\\d{2}.\\d{2}.\\d{2}";
        String RegexDate = "(0[1-9]|[12]\\d|3[01]).(0[1-9]|1[0-2]).(19|20)\\d{2}-([01]\\d|2[0-3]).([0-5]\\d).([0-5]\\d)";
        Pattern pattern = Pattern.compile(RegexDate);
        String inputDate = "";

        while(!pattern.matcher(inputDate).find()){
            System.out.print("Введите " + typeOfDate + " дату и время (dd.MM.yyyy-HH.mm.ss): ");
            inputDate = scanner.nextLine();
            if(!pattern.matcher(inputDate).find()){
                System.out.println("Вы неправильно ввели формат времени!");
            }
        }
        return inputDate;
    }

    private static boolean checkDiffBetweenEnd_and_Start(String startDateStr, String endDateStr){
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");

        try{
            Date startDate = dateFormat.parse(startDateStr);
            Date endDate = dateFormat.parse(endDateStr);
            return endDate.before(startDate);
        }catch(ParseException e){
            logger.log(Level.SEVERE, "Ошибка конвертации даты и времени: " + startDateStr + ", " + endDateStr, e);
            return true;
        }
    }

    private static Map<String, JSONObject> FindOutOfPeriod(Map<String, JSONObject> getMatchedRequests,  Date startDate, Date endDate){
        Map<String, JSONObject> requestsByTime = new TreeMap<>();

        for(Map.Entry<String, JSONObject> matchedRequestsEntry : getMatchedRequests.entrySet()){
            String get_key = matchedRequestsEntry.getKey();
            JSONObject matchedRequest = matchedRequestsEntry.getValue();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");
            Date requestDate;

            try{
                requestDate = dateFormat.parse(get_key);
                if(requestDate.after(startDate) && requestDate.before(endDate)){
                    requestsByTime.put(get_key, matchedRequest);
                    logger.info("Данные по соответствующему периоданы найдены и добавлены в JSON.");
                }
            }catch (ParseException e){
                logger.log(Level.SEVERE, "Ошибка конвертации даты и времени: " + get_key, e);
            }
        }

        return requestsByTime;
    }
    private static Map<String, JSONObject> matchedRequests(Map<String, List<String>> nginxLogRequests,
                                                           Map<String, List<String>> tomcatLogRequests,
                                                           Map<String, List<String>> mysqlRequests){

        //Map<String, JSONObject> matched_Requests = new TreeMap<>();
        Map<String, JSONObject> matched_Requests = new LinkedHashMap<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");

        for (Map.Entry<String, List<String>> mysqlEntry: mysqlRequests.entrySet()){
            //Основной ключ
            String time_key = mysqlEntry.getKey();
            //Ключ с учетом того, что мы отнимаем 0.5 секунд (если останется то же время то ключ для поиска будет один,
            // в ином случае - два)
            double milliseconds = Double.parseDouble(time_key.substring(17,26));
            StringBuilder anotherKey = new StringBuilder(time_key);
            anotherKey.replace(17,26, String.valueOf(milliseconds - 0.5));

            try {
                Date mainDate = inputFormat.parse(time_key);
                Date anotherDate = inputFormat.parse(anotherKey.toString());

                String mainDateStr = outputFormat.format(mainDate);
                String anotherDateStr = outputFormat.format(anotherDate);

                List<String> keys = new ArrayList<>();
                if (anotherDate.before(mainDate)) {
                    keys.add(mainDateStr);
                    keys.add(anotherDateStr);
                }
                else{
                    keys.add(mainDateStr);
                }

                List<String> value_of_mysqlReq = mysqlEntry.getValue();
                JSONObject matchedRequest = new JSONObject();

                for (String key : keys){
                    if(nginxLogRequests.containsKey(key) && tomcatLogRequests.containsKey(key)){
                        if (!matchedRequest.has("mysql-low.log")){
                            matchedRequest.put("mysql-low.log", value_of_mysqlReq);
                            time_key = key;
                        }
                        if (!matchedRequest.has("nginx")) {
                            matchedRequest.put("nginx", nginxLogRequests.get(key));
                        }
                        else {
                            JSONArray existingNginxListOfRequest = matchedRequest.getJSONArray("nginx");
                            existingNginxListOfRequest.put(nginxLogRequests.get(key));
                        }
                        if (!matchedRequest.has("tomcat")) {
                            matchedRequest.put("tomcat", tomcatLogRequests.get(key));
                        }
                        else {
                            JSONArray existingTomcatListOfRequest = matchedRequest.getJSONArray("tomcat");
                            existingTomcatListOfRequest.put(tomcatLogRequests.get(key));
                        }
                    }
                }

                if(matchedRequest.length() > 0){
                    matched_Requests.put(time_key, matchedRequest);
                    logger.info("Соответствующие данные успешно добавлены в файл!");
                }

            }catch(ParseException e){
                logger.log(Level.SEVERE, "Ошибка конвертации даты и времени: " + time_key, e);
            }
        }

        return matched_Requests;
    }

    private static Map<String, List<String>> getMysqlRequests(List<String> LogData){
        Map<String, List<String>> requests = new HashMap<>();

        String regexTarget = "(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.\\d{6})";
        String currentKey = null;

        int i = 0;

        try{
            while(i < LogData.size()){

                String logLine = LogData.get(i);

                Pattern pattern = Pattern.compile(regexTarget);
                Matcher match = pattern.matcher(logLine);

                if(match.find()){
                    String time = match.group(1);
                    List<String> values = new ArrayList<>();
                    values.add(LogData.get(i));
                    currentKey = time;
                    requests.put(currentKey, values);

                }else {
                    if(currentKey!=null){
                        List<String> values = requests.get(currentKey);
                        values.add(logLine);
                    }
                }

                i++;
            }
        }
        catch (Exception e){
            logger.log(Level.SEVERE, "Ошибка при обработке запросов MySQL", e);
        }

        return requests;
    }

    private static Map<String, List<String>> getRequests(List<String> LogData) throws IOException{
        Map<String, List<String>> requests = new HashMap<>();

        String regexTarget = "\\d{2}/[A-Za-z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2}";

        int i = 0;
        //FileWriter writer = new FileWriter("D:\\PracticeTask\\tomсat\\new_file.txt");
        while(i < LogData.size()){
            String logLine = LogData.get(i);

            Pattern pattern = Pattern.compile(regexTarget);
            Matcher match = pattern.matcher(logLine);

            if(match.find()){
                String Date_From_Log = match.group(0);
                SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss", Locale.ENGLISH);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");

                try{
                    Date date = inputFormat.parse(Date_From_Log);
                    String formattedDate = outputFormat.format(date);

                    if(requests.containsKey(formattedDate)){
                        List<String> values = requests.get(formattedDate);
                        values.add(logLine);
                        //.replaceAll(regexTarget, "")
                    }
                    else {
                        List<String> values = new ArrayList<>();
                        values.add(logLine);
                        //.replaceAll(regexTarget, "")
                        requests.put(formattedDate, values);
                    }
                    //requests.put(formattedDate, logLine.replaceAll(regexTarget, ""));
                   // writer.write(formattedDate + " " + logLine.replaceAll(regexTarget, "") +"\n");
                    i++;
                }catch (ParseException e){
                    logger.log(Level.SEVERE, "Ошибка при конвертации даты и времени: " + Date_From_Log, e);
                }
            }
        }

        //writer.close();
        return requests;
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

}

