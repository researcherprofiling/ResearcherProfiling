package utils;


import java.io.*;
import java.net.*;
import java.util.Map;

public class MyHTTP {

    /*
    * Method to be called outside this class - post params to target url.
    * Timeout values are set by util.Constant
    * */
    public static String post(String url, Map<String, String> params) {

        //  Prepare parameters for sending
        String paramString = "";
        if (params != null) {
            for (String k : params.keySet()) {
                paramString += k + "=" + params.get(k) + "&";
            }
            paramString = paramString.substring(0, paramString.length()-1);
        }

        try {
            //  Initialize connection
            URL urlObject = new URL(url);

            HttpURLConnection connection = (HttpURLConnection)urlObject.openConnection();
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(Constants.connectTimeout);
            connection.setReadTimeout(Constants.readTimeout);
            connection.connect();

            //  Write to server
            OutputStream out = connection.getOutputStream();
            out.write(paramString.getBytes());
            out.flush();
            out.close();

            //  Read from server
            InputStream in = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String response = "";
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response += line;
            }
            return response;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /*
    * Method to be called outside this class - get content from specified url with parameters appended.
    * Timeout values are set by util.Constant
    * */
    public static String get(String url, Map<String, String> params) {

        //  Get full URI
        String uri = getURI(url, params);

        try {
            //  Get connection and set parameter values
            URL urlObject = new URL(uri);

            HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setConnectTimeout(Constants.connectTimeout);
            connection.setReadTimeout(Constants.readTimeout);
            connection.connect();

            //  Get input stream and try reading
            InputStream in = connection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
            String response = "";
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response += line;
            }
            return response;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (SocketTimeoutException e) {
            System.out.println("Request to " + uri + " timed out");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /*
    * Given url and parameters, as mappings from string to string, this method constructs URI for a HTTP request.
    * URL and key names, i.e. parameter names, are not escaped during this process, while values associated to keys are escaped.
    * */
    private static String getURI(String url, Map<String, String> params) {
        String uri = url;
        if (params != null && !params.isEmpty()) {
            uri += "?";
            for (String key : params.keySet()) {
                String data = params.get(key);
                if (data == null || data.trim().length() == 0) {
                    System.out.println("Failed to instantiate value for parameter " + key);
                    return null;
                }
                try {
                    uri += key + "=" + URLEncoder.encode(data, "UTF-8") + "&";
                } catch (IOException e) {
                    System.out.println("Skipped key-value pair: " + key + ", " + data);
                }
            }
            uri = uri.substring(0, uri.length()-1);
        }
        return uri;
    }
}
