package models;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import static utils.Constants.localServerPortNumber;

public class LocalSearchServer {

    /*
    * A high-level abstraction of local background server, that provides API such as start, stop, and address getter.
    * */

    private HttpServer server;

    public LocalSearchServer() {
        server = null;
    }

    /*
    * Start a local server that will be serving incoming HTTP request.
    * */
    public void start() {
        //  Guard against double-initiation.
        if (server != null) return;
        //  Then start the service.
        try {
            server = HttpServer.create(new InetSocketAddress(localServerPortNumber), 0); //  Currently hard-coded to be port 8888
            server.createContext("/", new HTTPHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("Local background service running at " + server.getAddress().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    * Getter of socket address, which will be used in the search handler for local communication.
    * */
    public InetSocketAddress getSocketAddress() {
        return server.getAddress();
    }

    /*
    * Stop the local service currently running.
    * */
    public void stop() {
        //  Guard against non-sense stop.
        if (server == null) return;
        //  Wait 3 seconds, but no more, for currently processing request to finish.
        server.stop(1);
    }

}
