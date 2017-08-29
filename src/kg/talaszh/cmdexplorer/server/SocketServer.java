package kg.talaszh.cmdexplorer.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import kg.talaszh.cmdexplorer.server.stat.FileStatCounter;

/**
 * Created by talas on 8/24/17.
 */
public class SocketServer extends Thread {

    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static final String HELP_MSG = "Type 'help' for list of supported commands.";
    public static final String WELCOME_MSG = "Welcome! You have successfully connected to "
            + "CMD-Explorer server.\n" + HELP_MSG;

    private String rootPath;
    private Set<ClientHandler> clients;
    private int port;
    private volatile boolean stop = false;
    private ScheduledExecutorService scheduler;
    private ServerSocket serverSocket;

    public SocketServer(String rootPath, int port) {
        this.rootPath = rootPath;
        this.port = port;
        this.clients = new HashSet<>();
        scheduler = Executors.newScheduledThreadPool(10);
    }


    /**
     * Registers new clients and manages scheduled jobs
     */
    @Override
    public void run() {

        FileStatCounter fileStatCounter = new FileStatCounter(rootPath, "stats.txt");
        scheduler.scheduleAtFixedRate(fileStatCounter, 10, 30, TimeUnit.SECONDS);

        try {
            serverSocket = new ServerSocket(port);
            logger.info("CMD-Explorer is running...");
            while (!stop) {
                ClientHandler clientHandler = new ClientHandler(serverSocket.accept(), this,
                        fileStatCounter);
                scheduler.submit(clientHandler);

                clients.add(clientHandler);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    "Error starting server or accepting new connections on port: " + port, e);
        } finally {
            for (ClientHandler client : clients) {
                client.setStop(true);
            }
            scheduler.shutdown();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error stopping server.", e);
                }
            }
        }
    }

    public String getHelpMessage() {
        StringBuilder sb = new StringBuilder("Supported commands:\n");
        sb.append("help:\t\tPrint help.\n");
        sb.append("files:\t\tList available files.\n");
        sb.append("get:\t\tDownload file. Pass available filename as argument.\n");
        sb.append("exit:\t\tClose active session.");
        return sb.toString();
    }

    /**
     * List all files in current directory
     * @return list of files as string
     */
    public String listAvailableFiles() {
        File rootFolder = new File(rootPath);
        if (!rootFolder.isDirectory()) {
            throw new IllegalStateException("Root should be folder type.");
        }

        File[] files = rootFolder.listFiles(File::isFile);
        StringBuilder sb = new StringBuilder("Available files:");
        if (files != null) {
            for (File file : files) {
                sb.append("\n").append(file.getName());
            }
        }
        return sb.toString();
    }

    public Set<ClientHandler> getClients() {
        return clients;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void stopServer() {
        this.stop = true;
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error stopping server.", e);
            }
        }
    }
}
