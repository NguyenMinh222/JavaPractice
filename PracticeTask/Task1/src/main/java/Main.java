import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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
import com.google.gson.GsonBuilder;
import org.json.JSONObject;

public class Main{
    private static final Scanner scanner = new Scanner(System.in);
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    private static SimpleFormatter formatter = new SimpleFormatter();
    private static FileHandler fileHandler;
    private static List<String> nginxLogData = new ArrayList<>();
    private static List<String> tomcatLogData = new ArrayList<>();
    private static List<String> mysqlLogData = new ArrayList<>();

    public static void main(String[] args) throws IOException, ParseException {

        fileHandler = new FileHandler("Main.log");
        fileHandler.setFormatter(formatter);
        logger.addHandler(fileHandler);
        logger.setLevel(Level.ALL);

        String path_to_access = filePath("Nginx", "Nginx_Requests");
        checkTheRightFile(path_to_access, "Nginx");
        scanner.nextLine();

        String path_to_tomcat = filePath("Tomcat", "Tomcat_Requests");
        checkTheRightFile(path_to_tomcat, "Tomcat");
        scanner.nextLine();

        String path_to_mysql = filePath("MySQL (учтите, что файл MySQL имеет другой вид запросов)", "MySQL_Requests");
        checkTheRightFile(path_to_mysql, "MySQL");
        scanner.nextLine();

        Map<String, List<String>> nginxLogRequests = getRequests(nginxLogData);
        Map<String, List<String>> tomcatLogRequests = getRequests(tomcatLogData);
        Map<String, List<String>> mysqlRequests = getMysqlRequests(mysqlLogData);

        Double seconds = ChooseSeconds();
        Map<String, Object> getMatchedRequests = matchedRequests(nginxLogRequests, tomcatLogRequests, mysqlRequests, seconds);
        writeToJSON("MatchedRequests.json", getMatchedRequests);
        scanner.nextLine();

        String inputDate = checkTheRightDate("");
        String startDateStr = checkTheRightTime("начальное");
        String endDateStr = checkTheRightTime("конечное");

        String date_and_start_time = inputDate + "-" + startDateStr;
        String date_and_end_time = inputDate + "-" + endDateStr;
        while(checkDiffBetweenEnd_and_Start(date_and_start_time, date_and_end_time)){
            if (checkDiffBetweenEnd_and_Start(date_and_start_time, date_and_end_time)) {
                System.out.println("Конечная дата и время не может быть раньше начального времени!");
                endDateStr = checkTheRightTime("конечную");
                date_and_end_time = inputDate + "-" + endDateStr;
            }
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");
        Date startDate = dateFormat.parse(date_and_start_time);
        Date endDate = dateFormat.parse(date_and_end_time);

        Map<String, Object> getRequestsByTime = FindOutOfPeriod(getMatchedRequests, startDate, endDate);
        writeToJSON("getRequestsByTime.json", getRequestsByTime);
    }

    private static double ChooseSeconds() {
        DecimalFormat formatter = new DecimalFormat("0.00");
        double seconds = 0;
        while (true) {
            System.out.print("Введите кол-во секунд (не более 10 секунд): ");
            if (scanner.hasNextDouble()) {
                seconds = scanner.nextDouble();
                if (seconds <= 10 && seconds >= 0) {
                    return Double.parseDouble(formatter.format(seconds).replace(',','.'));
                } else {
                    System.out.println("Ошибка: количество секунд должно быть от 0 до 10.");
                }
            } else {
                System.out.println("Ошибка: введите время (секунды) в формате ss,SS.");
                logger.log(Level.INFO, "Ошибка ввода формата секунд.");
                scanner.next();
            }
        }
    }
    private static void check_Process(List<String> strings, String typeOfFile, String regex_str, int num_of_commans, List<String> logData, String error_message) throws IOException {
        Pattern pattern_req = Pattern.compile(regex_str);
        boolean validFile = false;
        for(String str: strings){
            if(pattern_req.matcher(str).find()){
                int commas = 0;
                for(int i = 0; i < str.length(); i++){
                    if (str.charAt(i) == '\"'){
                        commas += 1;
                    }
                }

                if(commas == num_of_commans){
                    logData.clear();
                    logData.addAll(strings);
                    validFile = true;
                    logger.log(Level.INFO, "Введенный файл является верным файлом " + typeOfFile + ".");
                    break;
                }
            }
        }

        if (!validFile) {
            logger.log(Level.SEVERE, error_message);
            scanner.nextLine();
            String new_path = filePath(typeOfFile, typeOfFile + "_Requests");
            checkTheRightFile(new_path, typeOfFile);
        }
    }

    private static void checkTheRightFile(String path_to_file, String typeOfFile) throws IOException {
        switch (typeOfFile){
            case "Nginx":
                check_Process(getData(path_to_file), typeOfFile, "\"\\b(GET)\\b.*?\"", 6, nginxLogData, "Этот файл не является файлом с запросами Nginx!");
                break;

            case "Tomcat":
                check_Process(getData(path_to_file), typeOfFile, "\"\\b(GET)\\b.*?\"", 2, tomcatLogData, "Этот файл не является файлом с запросами Tomcat!");
                break;

            case "MySQL":
                Pattern pat_sql = Pattern.compile("#\\s*Time:\\s*(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{6}Z)");
                boolean validSQL_File = false;
                List<String> get_strings = getData(path_to_file);
                for(String str: get_strings){
                    if(pat_sql.matcher(str).find()){
                        mysqlLogData = get_strings;
                        validSQL_File = true;
                        logger.log(Level.INFO, "Введенный файл является верным файлом  MySQL.");
                        break;
                    }
                }
                if (!validSQL_File) {
                    logger.log(Level.SEVERE, "Этот файл не является файлом с запросами MySQL");
                    scanner.nextLine();
                    String new_path = filePath(typeOfFile, typeOfFile + "_Requests");
                    checkTheRightFile(new_path, typeOfFile);
                }
                break;
        }
    }

    private static void writeToJSON(String nameOfFile, Map<String, Object> Requests){
        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String json = gson.toJson(Requests);

        try (FileWriter writeToJSON = new FileWriter(nameOfFile)) {
            writeToJSON.write(json);
            if(json.length() == 2){
                logger.info("Нет объектов для добавления в JSON file.");
                System.exit(0);
            }
            else{
                logger.info("Объекты были добавлены в JSON file.");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Ошибка при записи объектов в JSON файл: " + nameOfFile, e);
        }
    }

    private static String checkTheRightDate(String text){
        String RegexDate = "(0[1-9]|[12]\\d|3[01]).(0[1-9]|1[0-2]).(19|20)\\d{2}";
        Pattern datePattern = Pattern.compile(RegexDate);
        String inputDate = "";

        while(!datePattern.matcher(inputDate).find()){
            System.out.print("Введите дату в формате dd.MM.yyyy: ");
            inputDate = scanner.nextLine();
            if(!datePattern.matcher(inputDate).find()){
                System.out.println("Вы неправильно ввели формат даты!");
                logger.log(Level.INFO, "Неправильный ввод формата даты!");
                scanner.nextLine();
            }
        }
        return inputDate;
    }

    private static String checkTheRightTime(String typeOfDate){
        String RegexDate = "([01]\\d|2[0-3]).([0-5]\\d).([0-5]\\d)";
        Pattern pattern = Pattern.compile(RegexDate);
        String inputDate = "";

        while(!pattern.matcher(inputDate).find()){
            System.out.print("Введите " + typeOfDate + " время в формате HH.mm.ss: ");
            inputDate = scanner.nextLine();
            if(!pattern.matcher(inputDate).find()){
                System.out.println("Вы неправильно ввели формат времени!");
                logger.log(Level.INFO, "Неправильный ввод формата времени!");
                scanner.nextLine();
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

    private static Map<String, Object> FindOutOfPeriod(Map<String, Object> getMatchedRequests,  Date startDate, Date endDate){
        Map<String, Object> requestsByTime = new LinkedHashMap<>();

        for(Map.Entry<String, Object> matchedRequestsEntry : getMatchedRequests.entrySet()){
            String get_key = matchedRequestsEntry.getKey();
            Object matchedRequest = matchedRequestsEntry.getValue();

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");
            Date requestDate;

            try{
                requestDate = dateFormat.parse(get_key);
                if(requestDate.after(startDate) && requestDate.before(endDate)){
                    requestsByTime.put(get_key, matchedRequest);

                }
            }catch (ParseException e){
                logger.log(Level.SEVERE, "Ошибка конвертации даты и времени: " + get_key, e);
            }
        }

        if(requestsByTime.isEmpty()){
            logger.info("Зпросов по указанному периоду не были найдены.");
        }
        else{
            logger.info("Запросы по соответствующему периоду были найдены и успешно добавлены в JSON.");
        }

        return requestsByTime;
    }
    private static Map<String, Object> matchedRequests(Map<String, List<String>> nginxLogRequests,
                                                       Map<String, List<String>> tomcatLogRequests,
                                                       Map<String, List<String>> mysqlRequests,
                                                       double seconds){

        Map<String, Object> matched_Requests = new LinkedHashMap<>();
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");

        for (Map.Entry<String, List<String>> mysqlEntry: mysqlRequests.entrySet()){

            String time_key = mysqlEntry.getKey();

            double milliseconds = Double.parseDouble(time_key.substring(17,26));
            StringBuilder anotherKey = new StringBuilder(time_key);
            //Доработать с учетом минут
            anotherKey.replace(17,26, String.valueOf(milliseconds - seconds));

            try {
                //Главный ключ
                Date mainDate = inputFormat.parse(time_key);
                //Ключ после того как отняли некоторое время
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
                Map<String, Object> matchedRequest = new LinkedHashMap<>();

                for (String key : keys){
                    if(nginxLogRequests.containsKey(key) && tomcatLogRequests.containsKey(key)){
                        if (!matchedRequest.containsKey("mysql-low.log")){
                            matchedRequest.put("mysql-low.log", value_of_mysqlReq);
                            time_key = mainDateStr;
                        }
                        if (!matchedRequest.containsKey("nginx")) {
                            List<String> listOfRequests = new ArrayList<>(nginxLogRequests.get(key));
                            matchedRequest.put("nginx", listOfRequests);
                        }
                        else {
                            List<String> existingNginxListOfRequest = (List<String>) matchedRequest.get("nginx");
                            existingNginxListOfRequest.addAll(nginxLogRequests.get(key));
                        }
                        if (!matchedRequest.containsKey("tomcat")) {
                            List<String> listOfRequests = new ArrayList<>(tomcatLogRequests.get(key));
                            matchedRequest.put("tomcat", listOfRequests);
                        }
                        else {
                            List<String> existingTomcatListOfRequest = (List<String>) matchedRequest.get("tomcat");
                            existingTomcatListOfRequest.addAll((tomcatLogRequests.get(key)));
                        }
                    }
                }

                if(matchedRequest.size() > 0){
                    matched_Requests.put(time_key, matchedRequest);
                }

            }catch(ParseException e){
                logger.log(Level.SEVERE, "Ошибка конвертации даты и времени: " + time_key, e);
            }
        }

        if(matched_Requests.size() > 0){
            logger.info("Соответствующие данные успешно добавлены в файл!");
        }
        else{
            logger.info("Соответствующие данные не были найдены!");
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

        if(requests.isEmpty()){
            logger.info("Данный файл не содержит MySQL-запросы. Проверьте входной файл!");
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
            else{
                i++;
            }
        }

        if(requests.isEmpty()){
            logger.log(Level.INFO,"Запросов не найдено. Проверьте входной файл!");
        }
        //writer.close();
        return requests;
    }

    private static List<String> getData(String path_to_file) throws IOException {
//        logger.info("Чтение из файла: " + path);
//        List<String> read_All_Lines = null;
//        try{
//            read_All_Lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
//        }
//        catch (IOException e){
//            logger.log(Level.SEVERE, "Ошибки при чтении файла: " + path, e);
//        }
//        logger.info("Данные успешно прочитаны из файла " + path);
//        return read_All_Lines;
        logger.log(Level.INFO, "Чтение из файла: " + path_to_file);

        BufferedReader buffReader = new BufferedReader(new FileReader(path_to_file));
        List<String> getAll_Lines = new ArrayList<>();

        try {
            String line = buffReader.readLine();
            while(line != null){
                getAll_Lines.add(line);
                line = buffReader.readLine();
            }
        }catch (IOException e){
            logger.log(Level.SEVERE, "Ошибка при чтении файла " + path_to_file, e);
        }
        finally {
            buffReader.close();
        }

        logger.info("Данные успешно прочитаны из файла " + path_to_file);
        return  getAll_Lines;
    }

    private static String filePath(String text, String path_to_folder) {
        while(true){
            System.out.print("Введите имя/путь к " + text + " файлу: ");
            String fileName = scanner.nextLine();

            if(fileName.trim().isEmpty()) {
                logger.log(Level.INFO, "Вы ничего не ввели, нужно ввести имя/путь файла!");
                scanner.nextLine();
                continue;
            }

            try{
                File file = new File(fileName);
                if(file.isAbsolute()){
                    return  file.getAbsolutePath();
                }
                else {
                    File[] files = new File(path_to_folder).listFiles();
                    for(File file1: files){
                        if(file1.isFile() && file1.getName().equals(fileName)){
                            return file1.getAbsolutePath();
                        }
                    }
                }

                logger.log(Level.INFO, "Файл " + fileName + " не удалось найти!");
                scanner.nextLine();
            }
            catch (Exception e){
                logger.log(Level.SEVERE, "Ошибка!", e);
            }
        }
    }
}

