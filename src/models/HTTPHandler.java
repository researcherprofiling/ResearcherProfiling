package models;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import models.schema.Schema;
import models.wrapper.GeneralWrapper;
import models.wrapper.sourceWrapper.DirectSourceWrapper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static utils.Constants.sourceExecutionTimeout;
import static utils.Utilities.constructFullSourceName;

public class HTTPHandler implements HttpHandler {

    /*
    * This class implements HttpHandler protocol and will be instantiated by LocalSearchServer class,
    * serving as the handler for every single HTTP request received.
    * */

    private Map<String, DirectSourceWrapper> localSources;

    public HTTPHandler() {
        resetRegisteredSources();
    }


    /*
    * This function dispatches a HTTP request, i.e. HttpExchange object, to specific handlers,
    * according to its method.
    * */
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String method = httpExchange.getRequestMethod();
        if (method.equals("GET")) {
            handleGET(httpExchange);
        } else if (method.equals("POST")) {
            String path = httpExchange.getRequestURI().getPath();
            if (path.equals("/reset")) {
                //  For post to /reset, reset registration regardless of anything.
                //  The body is not read.
                resetRegisteredSources();
                sendResponse(httpExchange, 200, "Successfully reset registration.");
            }
        }
    }

    /*
    * Common helper function: respond with some code and response body specified as parameters.
    * */
    private void sendResponse(HttpExchange httpExchange, int code, String response) throws IOException{
        if (response != null && code >= 200 && code <= 600) {
            httpExchange.sendResponseHeaders(code, response.getBytes().length);
            OutputStream output = httpExchange.getResponseBody();
            output.write(response.getBytes());
            output.close();
        }
    }

    /*
    * Common helper function: respond with code 400 - Bad Request.
    * */
    private void sendError400(HttpExchange httpExchange) throws IOException{
        sendResponse(httpExchange, 400, "400 - Bad Request");
    }

    /*
    * Assuming every GET method represents a query for data pertaining to some given source.
    * Context path: /aspect/source/
    * Parameters: Mappings from string to string indicating search conditions.
    * Response: JSON array whose elements represent answer to the query.
    * */
    private void handleGET(final HttpExchange httpExchange) throws IOException {

        //  Build search condition object
        Map<String, String> mappings = extractParameters(httpExchange);
        final JSONObject searchConditions = JSONObject.fromObject(mappings);
        if (searchConditions == null) {
            sendError400(httpExchange);
            return;
        }
        if (searchConditions.containsKey("kws")) {
            searchConditions.replace("kws", JSONObject.fromObject(searchConditions.get("kws")));
        }

        //  Parse context and determine target source
        String path = httpExchange.getRequestURI().getPath();
        Pattern regex = Pattern.compile("\\A/([^/]+)/([^/]+)\\z");
        Matcher matcher = regex.matcher(path);
        if (!matcher.matches()) {
            sendError400(httpExchange);
            return;
        }
        final String aspect = URLDecoder.decode(matcher.group(1), "UTF-8");
        final String source = URLDecoder.decode(matcher.group(2), "UTF-8");

        //  Retrieve partial result
        String fullSourceName = constructFullSourceName(aspect, source);
        final DirectSourceWrapper wrapper = localSources.get(fullSourceName);
        String cwd = System.getProperty("user.dir");
        System.setProperty(
                "user.dir",
                cwd + (cwd.endsWith("/") ? "" : "/") + GeneralWrapper.basePath + "/" + aspect + "/" + source + "/"
        );
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray result = wrapper.getResultAsJSON(searchConditions);
                    sendResponse(httpExchange, 200, result.toString());
                } catch (IOException e) {
                    System.out.println("Failed to respond to httpExchange from " + httpExchange.getRemoteAddress().toString());
                    e.printStackTrace();
                } catch (Exception e) {
                    System.out.println("Source " + source + " execution interrupted.");
                }
            }
        });
        try {
            thread.run();
            thread.join(sourceExecutionTimeout);
            thread.interrupt();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.setProperty("user.dir", cwd);
        }
    }

    /*
    * Helper function for handleGET: extracting parameters from URI in the type of Map<String,String>.
    * */
    private Map<String, String> extractParameters(HttpExchange httpExchange) {
        String query = httpExchange.getRequestURI().getQuery();
        //  Following code that parses query string into mappings is directly taken from the following website
        //  http://www.rgagnon.com/javadetails/java-get-url-parameters-using-jdk-http-server.html
        Map<String, String> result = new HashMap<String, String>();
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            try {
                if (pair.length>1) {
                    result.put(URLDecoder.decode(pair[0], "UTF-8"), URLDecoder.decode(pair[1], "UTF-8"));
                }else{
                    result.put(URLDecoder.decode(pair[0], "UTF-8"), "");
                }
            } catch (IOException e) {
                System.out.println("Skipped parameter pair: " + pair);
            }
        }
        return result;
    }

    /*
    * This method is called for initialization or resetting of local source registration.
    * It dives into the aspect folder specified by GeneralWrapper.basePath, and parse the file system into wrappers.
    * */
    private void resetRegisteredSources() {
        File aspDir = new File(GeneralWrapper.basePath);
        this.localSources = new HashMap<String, DirectSourceWrapper>();
        if (aspDir.exists() && aspDir.isDirectory()) {
            File[] aspects = aspDir.listFiles();
            try {
                //  Dive into each aspect folder
                for (File aspect : aspects) {
                    if (aspect.getName().startsWith(".") || !(aspect.isDirectory())) {
                        continue;
                    }
                    //  Read schema
                    String aspectPath = GeneralWrapper.basePath + "/" + aspect.getName();
                    Schema schema = Schema.readFromFile(aspectPath + "/schema.tsv");
                    //  Dive into each source folder
                    File[] sources = aspect.listFiles();
                    for(File source : sources) {
                        if (source.getName().startsWith(".") || !(source.isDirectory())) {
                            continue;
                        }
                        //  Initialize wrappers
                        localSources.put(
                                constructFullSourceName(aspect.getName(), source.getName()),
                                new DirectSourceWrapper(schema, source.getName(), aspect.getName())
                        );
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

}
