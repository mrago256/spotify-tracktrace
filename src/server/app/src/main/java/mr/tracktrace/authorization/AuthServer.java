package mr.tracktrace.authorization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AuthServer {
    public static String waitForAndRetrieveAuthCode() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
        server.createContext("/", new MyHandler());
        server.start();

        while (!MyHandler.receivedResponse) {
            Thread.sleep(250);
        }

        System.out.println("Received response. Shutting down web server...");

        server.stop(0);

        return MyHandler.responseCode;
    }

    static class MyHandler implements HttpHandler {
        static boolean receivedResponse = false;
        static String responseCode;

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> queryParams = parseQueryParams(exchange.getRequestURI().getQuery());

            String code = queryParams.get("code");

            if (code != null) {
                responseCode = code;
            }

            String response = "Received code. You may close this window.";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

            receivedResponse = true;
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> queryParams = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    queryParams.put(key, value);
                }
            }
            return queryParams;
        }
        }
    }
