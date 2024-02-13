package ru.meshgroup.testtaskmeshgroupclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class TestClient {

    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            if (args[0].equals("1")) {
                addUsers(0, 100);
            } else if (args[0].equals("2")) {
                getUserList(8, 10);
            } else if (args[0].equals("3")) {
                testRequest(8);
            } else if (args[0].equals("4")) {
                updateUser(10, "2000-08-14");
            } else if (args[0].equals("5")) {
                transferMoney(5, 10L, BigDecimal.valueOf(50.12));
            } else if (args[0].equals("6")) {
                transferMoneyConcurrently();
            }
        }
    }

    static void transferMoneyConcurrently() throws InterruptedException {
        new Thread(() -> {
            try {
                transferMoney(5, 10L, BigDecimal.valueOf(50.12));
            } catch (Exception ex) {
                Logger.getLogger(TestClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
        Thread.sleep(2000);
        new Thread(() -> {
            try {
                transferMoney(10, 5L, BigDecimal.valueOf(50.12));
            } catch (Exception ex) {
                Logger.getLogger(TestClient.class.getName()).log(Level.SEVERE, null, ex);
            }
        }).start();
    }

    private static void updateUser(int index, String birthDay) throws Exception {
        String authRequest = new TestClient().sendPost("http://localhost:8080/authenticate", RequestMethod.POST, getAuthMap(), "{\"username\": \"user" + index + "\",\n\"password\": \"password" + index + "\"}");
        System.out.println(authRequest);
        String token = getToken(authRequest);
        String startResult = new TestClient().sendPost("http://localhost:8080/api/test-controller/updateUser", RequestMethod.POST, getAuthMap(token),
                "{\"id\": \"" + index + "\",\n\"name\": \"user" + index + "\",\n\"dateOfBirth\": \"" + birthDay + "\",\n\"password\": \"password" + index + "\"}");
        System.out.println(startResult);
    }

    private static void addUsers(int start, int amount) throws Exception {
        String token = getToken();
        for (int i = start; i < start + amount; i++) {
            LocalDate date = LocalDate.of(2000, 2, 2).plusDays(i);
            String dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            StringBuilder post = new StringBuilder("{\"id\": \"").append(i).append("\",\n\"name\": \"user").append(i).append("\",\n\"dateOfBirth\": \"").append(dateStr).append("\",\n\"password\": \"password").append(i).append("\"")
                    .append(",\n\"accountBeanList\": [{\"id\": \"").append(i).append("\",\"userId\": \"").append(i).append("\",\"balance\": \"200\"}]")
                    .append(",\n\"mailBeanList\": [{\"id\": \"").append(i).append("\",\"userId\": \"").append(i).append("\",\"email\": \"" + createMail(i) + "\"}]")
                    .append(",\n\"phoneBeanList\": [{\"id\": \"").append(i).append("\",\"userId\": \"").append(i).append("\",\"phone\": \"" + createTelephoneNumber(i) + "\"}]}");
            String startResult = new TestClient().sendPost("http://localhost:8080/api/test-controller/addUser", RequestMethod.POST, getAuthMap(token), post.toString());
            System.out.println(startResult);
        }
    }

    private static void transferMoney(int userFromIndex, Long userIdTo, BigDecimal money) throws Exception {
        String authRequest = new TestClient().sendPost("http://localhost:8080/authenticate", RequestMethod.POST, getAuthMap(), "{\"username\": \"user" + userFromIndex + "\",\n\"password\": \"password" + userFromIndex + "\"}");
        System.out.println(authRequest);
        String token = getToken(authRequest);
        String startResult = new TestClient().sendPost("http://localhost:8080/api/test-controller/transferMoney", RequestMethod.POST, getAuthMap(token),
                "{\"userIdTo\": \"" + userIdTo + "\",\n\"money\": \"" + money.toString() + "\"}");
        System.out.println(startResult);
    }

    private static void getUserList(int userFromIndex, int offset) throws Exception {
        String authRequest = new TestClient().sendPost("http://localhost:8080/authenticate", RequestMethod.POST, getAuthMap(), "{\"username\": \"user" + userFromIndex + "\",\n\"password\": \"password" + userFromIndex + "\"}");
        System.out.println(authRequest);
        String token = getToken(authRequest);
        String startResult = new TestClient().sendPost("http://localhost:8080/api/test-controller/getUserList?name=user&date=2000-04-02&offset=" + offset + "&size=20", RequestMethod.GET, getAuthMap(token), null);
        System.out.println(startResult);
    }

    private static void testRequest(int userFromIndex) throws Exception {
        String authRequest = new TestClient().sendPost("http://localhost:8080/authenticate", RequestMethod.POST, getAuthMap(), "{\"username\": \"user" + userFromIndex + "\",\n\"password\": \"password" + userFromIndex + "\"}");
        System.out.println(authRequest);
        String token = getToken(authRequest);
        String startResult = new TestClient().sendPost("http://localhost:8080/api/test-controller/test?str=Hello%20" + new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss:SSS").format(new Date()) + "!", RequestMethod.GET, getAuthMap(token), null);
        System.out.println(startResult);
        System.out.println(startResult);
    }

    private static String getToken() throws Exception {
        String authRequest = new TestClient().sendPost("http://localhost:8080/authenticate", RequestMethod.POST, getAuthMap(), "{\"username\": \"meshgroup_user\",\n\"password\": \"password\"}");
        System.out.println(authRequest);
        String token = getToken(authRequest);
        System.out.println("token:" + token + "!");
        return token;
    }

    private static int telephoneNumberLength = 11;
    private static Set<Integer> hypenSet = Set.of(2, 5, 7);

    private static String createTelephoneNumber(long digit) {
        String digitStr = digit + "";
        if (digitStr.length() > telephoneNumberLength - 1) {
            throw new RuntimeException("Wrong telephone number!");
        }
        StringBuilder stringToReturn = new StringBuilder("8-");
        String str = null;
        if (digitStr.length() < telephoneNumberLength - 1) {
            str = IntStream.range(digitStr.length(), telephoneNumberLength - 1).mapToObj(i -> "0").reduce((s1, s2) -> s1 + s2).get() + digitStr;
        } else {
            str = digitStr;
        }
        for (int i = 0; i < str.length(); i++) {
            stringToReturn.append(str.charAt(i));
            if (hypenSet.contains(i)) {
                stringToReturn.append("-");
            }
        }
        return stringToReturn.toString();
    }

    private static String createMail(int index) {
        return "hello" + index + "@world.ru";
    }

    private static final String TOKEN_HEAD = "{\"token\":\"";

    private static String getToken(String sendPost) {
        int indexOf = sendPost.indexOf(TOKEN_HEAD) + TOKEN_HEAD.length();
        int lastIndexOf = sendPost.indexOf("\"}");
        String token = sendPost.substring(indexOf, lastIndexOf);
        System.out.println("token:" + token + "!");
        return token;
    }

    private static LinkedHashMap<String, String> getAuthMap(String token) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Content-Type", "application/json");
        map.put("User-Agent", USER_AGENT);
        map.put("Authorization", "Bearer " + token);
        return map;
    }

    private static LinkedHashMap<String, String> getAuthMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("Content-Type", "application/json");
        map.put("User-Agent", USER_AGENT);
        return map;
    }
    private static final String USER_AGENT = "Apache-HttpClient/4.1.1 (java 1.5)";
    private static final int READ_TIMEOUT = 60000;

    private enum RequestMethod {
        GET, POST, PATCH
    }

    @SuppressWarnings("all")
    private String sendGet(String urlStr, RequestMethod requestMethod, Map<String, String> headers) throws Exception {
        return sendPost(urlStr, requestMethod, headers, null);
    }

    // HTTP POST request
    @SuppressWarnings("all")
    private String sendPost(String urlStr, RequestMethod requestMethod, Map<String, String> headers, String urlParameters) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setReadTimeout(READ_TIMEOUT);
        con.setRequestMethod(requestMethod.name());
        headers.entrySet().forEach(entry -> con.setRequestProperty(entry.getKey(), entry.getValue()));
        con.setDoOutput(true);
        if (!requestMethod.equals(RequestMethod.GET)) {
            try (OutputStream wr = con.getOutputStream()) {
                wr.write(urlParameters.getBytes());
                wr.flush();
            }
        }
        int responseCode = con.getResponseCode();
        InputStream errorStream = getErrorStream(con);
        try (InputStream inputStream = (errorStream != null ? errorStream : getInputStream(con))) {
            StringBuffer response = readResponse(con, inputStream);
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Response code = " + responseCode + "!");
            }
            return response.toString();
        }
    }

    private InputStream getInputStream(HttpURLConnection con) {
        try {
            return con.getInputStream();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private InputStream getErrorStream(HttpURLConnection con) {
        return con.getErrorStream();
    }

    private StringBuffer readResponse(HttpURLConnection con, InputStream inputStream) throws IOException {
        StringBuffer response = new StringBuffer();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response;
    }
}
