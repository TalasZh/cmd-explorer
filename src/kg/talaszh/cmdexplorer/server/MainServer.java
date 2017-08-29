package kg.talaszh.cmdexplorer.server;

import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by talas on 8/29/17.
 */
public class MainServer {

    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) throws Exception {
        Logger rootLogger = Logger.getLogger("");
        java.util.logging.Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }

        FileHandler logfile = new FileHandler("cmd-explorer-server.log", true);
        logfile.setFormatter(new SimpleFormatter());
        logger.addHandler(logfile);

        ConsoleHandler consoleHandler = new ConsoleHandler();
        logger.addHandler(consoleHandler);

        if (args.length >= 1) {
            int port = 9001;

            if (args.length >= 2) {
                port = Integer.valueOf(args[1]);
            }
            if (args.length >= 3) {
                Level level = Level.parse(args[2]);
                logger.setLevel(level);
                rootLogger.setLevel(level);
                consoleHandler.setLevel(level);
            }

            logger.fine("Server logging successfully configured.");
            startServer(args[0], port);
        } else {
            logger.severe(String.format("Program usage: java -cp {classpath directory} %s "
                            + "{rootFolder:required} {serverPort:9001} {logginLevel:%s}",
                    MainServer.class.getCanonicalName(),
                    Arrays.toString(new String[]{Level.ALL.getName(),
                            Level.FINEST.getName(), Level.FINER.getName(), Level.FINE.getName(),
                            Level.INFO.getName(), Level.WARNING.getName(), Level.SEVERE.getName(),
                            Level.OFF.getName()})));
        }
    }

    private static void startServer(String rootPath, int port) {
        SocketServer server = new SocketServer(rootPath, port);
        server.start();

        new Thread(() -> {
            Scanner scan = new Scanner(System.in);
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                if ("exit".equals(line)) {
                    break;
                }
            }
            server.stopServer();
        }).start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stopServer));
    }
}
