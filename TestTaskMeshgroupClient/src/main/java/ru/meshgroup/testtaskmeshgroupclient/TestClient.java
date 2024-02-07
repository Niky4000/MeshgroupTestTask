package ru.meshgroup.testtaskmeshgroupclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestClient {

    public static void main(String[] args) throws Exception {
//        String startResult = new TestClient().sendPost("http://localhost:8080/api/test-controller/test?str=Hello%20" + new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss:SSS").format(new Date()) + "!", RequestMethod.GET, getAuthMap(), null);
//        System.out.println(startResult);
//		String startResult = new WebExampleClientStart().sendPost("http://localhost:8080/api/test-controller/test?str=Hello%20" + new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss:SSS").format(new Date()) + "!", RequestMethod.GET, getAuthMap(), null);
//		System.out.println(startResult);
//        String startResult2 = new TestClient().sendPost("http://localhost:8080/api/test-controller/test2", RequestMethod.POST, getAuthMap(), "Hello! Hello post method " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()) + "!");
//        System.out.println(startResult2);
//		String startResult2 = new WebExampleClientStart().sendPost("http://localhost:8080/api/url-controller/shorten-url", RequestMethod.POST, getAuthMap(), "http://codenco.us/very-long-string-fkfdonsdjkbfkjsdbkdsnbfkdsnbfksdbfdskbdekjblnfd");
//		System.out.println(startResult2);
//		String startResult2 = new WebExampleClientStart().sendPost("http://localhost:8080/", RequestMethod.POST, getAuthMap(), "Message");
//		System.out.println(startResult2);
//		String startResult2 = new WebExampleClientStart().sendPost("http://localhost:8080/api/url-controller/get-url", RequestMethod.POST, getAuthMap(), "231caeb5-3748-4c9b-9265-81e2453cb623");
//		System.out.println(startResult2);
//		String startResult = new WebExampleClientStart().sendPost("http://localhost:8080/?str=TestMessage", RequestMethod.GET, getAuthMap(), null);
//		System.out.println(startResult);

        String authRequest = new TestClient().sendPost("http://localhost:8080/authenticate", RequestMethod.POST, getAuthMap(), "{\"username\": \"meshgroup_user\",\n\"password\": \"password\"}");
        System.out.println(authRequest);
        String token = getToken(authRequest);
        System.out.println("token:" + token + "!");
//        String helloRequest = new TestClient().sendPost("http://localhost:8080/hello", RequestMethod.GET, getAuthMap(token), null);
//        System.out.println(helloRequest);
//        String startResult = new TestClient().sendPost("http://localhost:8080/api/test-controller/test?str=Hello%20" + new SimpleDateFormat("yyyy-MM-dd%20HH:mm:ss:SSS").format(new Date()) + "!", RequestMethod.GET, getAuthMap(token), null);
//        System.out.println(startResult);
//        String startResult2 = new TestClient().sendPost("http://localhost:8080/api/test-controller/test2", RequestMethod.POST, getAuthMap(token), "Hello! Hello post method " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()) + "!");
//        System.out.println(startResult2);
        String startResult2 = new TestClient().sendPost("http://localhost:8080/api/test-controller/addUser", RequestMethod.POST, getAuthMap(token), "{\"id\": \"2\",\n\"name\": \"user2\",\n\"dateOfBirth\": \"2023-05-12\",\n\"password\": \"password\""
                + ",\n\"accountBeanList\": [{\"id\": \"2\",\"userId\": \"2\",\"balance\": \"200\"}]"
                + ",\n\"mailBeanList\": [{\"id\": \"2\",\"userId\": \"2\",\"email\": \"hello@world.ru\"}]"
                + ",\n\"phoneBeanList\": [{\"id\": \"2\",\"userId\": \"2\",\"phone\": \"8-927-777-77-77\"}]}");
//        System.out.println(startResult2);
//        String startResult2 = new TestClient().sendPost("http://localhost:8080/api/test-controller/addUser", RequestMethod.POST, getAuthMap(token), "{\"name\": \"user2\",\n\"password\": \"password\"}");
        System.out.println(startResult2);
    }

    private static final String TOKEN_HEAD = "{\"token\":\"";

    private static String getToken(String sendPost) {
        int indexOf = sendPost.indexOf(TOKEN_HEAD) + TOKEN_HEAD.length();
        int lastIndexOf = sendPost.indexOf("\"}");
        String token = sendPost.substring(indexOf, lastIndexOf);
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
        GET, POST
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
        if (requestMethod.equals(RequestMethod.POST)) {
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
