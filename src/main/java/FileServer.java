import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;


public class FileServer {

    private static String BASE_DIRECTORY;

    public static void main(String[] args) throws Exception {
        File root = null;
        int port = 8000;

        if (args.length != 1) {
            System.out.println("Wrong number of arguments");
            System.exit(-1);
        } else {
            root = new File(args[0]);
        }

        if (!root.exists()) {
            System.out.println("Dirtectory does not exist");
            System.exit(-1);
        }

        if (!root.isDirectory()) {
            System.out.println("Path does not point to directory");
            System.exit(-1);
        }
        BASE_DIRECTORY = root.getCanonicalPath();


        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());

        System.out.println("Starting server on port: " + port);
        server.start();
    }
    static class RootHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String uriPath = exchange.getRequestURI().getPath();
            File file = new File(BASE_DIRECTORY, uriPath);

            if (!validatePath(file.getCanonicalPath())) {
                handleForbidden(exchange);
            }

            if (!file.exists()) {
                handleNotFound(exchange);
            } else if (file.isDirectory()){
                handleDirectory(file, exchange);
            } else if (file.isFile()) {
                handleFile(file, exchange);
            }
        }
    }
    private static boolean validatePath(String path) {
        if (path.startsWith(BASE_DIRECTORY)) {
            return true;
        } else {
            return false;
        }
    }

    private static void handleForbidden(HttpExchange exchange) throws IOException {
        handleError(exchange, 403, "403 Forbidden");
    }

    private static void handleNotFound(HttpExchange exchange) throws IOException {
        handleError(exchange, 404, "404 File not found");
    }
    private static void handleError (HttpExchange exchange, int err_code, String err_msg) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(err_code, err_msg.length());
        OutputStream os = exchange.getResponseBody();
        os.write(err_msg.getBytes());
        os.close();
    }
    private static void handleDirectory(File dir, HttpExchange exchange) throws IOException{
        byte [] result;

        String subDir = getSubDir(BASE_DIRECTORY, dir.getCanonicalPath());
        StringBuilder template = new StringBuilder();
        template.append("<!DOCTYPE html>\n");
        template.append("<html lang=\"en\">\n");
        template.append("<head>\n");
        template.append("<meta charset=\"UTF-8\">\n");
        template.append("<title>Title</title>\n");
        template.append("</head>\n");
        template.append("<body>\n");
        template.append("<h1>Paweł Kusz Zadanie: B1</h1>\n");

        if (!subDir.equals(""))
            template.append("<a href=\"..\">" + ".." + "</a><br />");
        for (String file : dir.list()) {
            template.append("<a href=\"" + subDir + "/" + file + "\">" + file + "</a><br />");
        }
        template.append("</body>\n");
        template.append("</html>\n");

        result = template.toString().getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, result.length);
        OutputStream os = exchange.getResponseBody();
        os.write(result);
        os.close();

    }

    private static void handleFile(File file, HttpExchange exchange) throws IOException {
        byte[] fileContent = Files.readAllBytes(Paths.get(file.getCanonicalPath()));
        Path path = file.toPath();
        String mimeType = Files.probeContentType(path);

        if (mimeType != null) {
            if (mimeType.startsWith("text/")) {
                mimeType += ";charset=utf-8";
            }
            exchange.getResponseHeaders().set("Content-Type", mimeType);
        }
        exchange.sendResponseHeaders(200, fileContent.length);
        OutputStream os = exchange.getResponseBody();
        os.write(fileContent);
        os.close();

    }
    private static String getSubDir(String basePath, String dirPath) {
        int baseLen = basePath.length();
        int dirLen  = dirPath.length();
        int len =  dirLen - baseLen;

        if (len >- 0) {
            return dirPath.substring(dirLen - len + 1, dirLen);
        } else return "";
    }
}
