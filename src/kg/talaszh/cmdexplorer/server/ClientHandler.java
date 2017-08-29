package kg.talaszh.cmdexplorer.server;

import static kg.talaszh.cmdexplorer.server.SocketServer.HELP_MSG;
import static kg.talaszh.cmdexplorer.server.SocketServer.WELCOME_MSG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import kg.talaszh.cmdexplorer.Constants;
import kg.talaszh.cmdexplorer.server.stat.FileStatCounter;

/**
 * Created by talas on 8/29/17.
 */
class ClientHandler extends Thread {

    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private Socket socket;
    private BufferedReader in;
    private OutputStream os;
    private String name;
    private SocketServer server;
    private volatile boolean stop = false;
    private FileStatCounter fileStatCounter;

    public ClientHandler(Socket socket, SocketServer socketServer,
            FileStatCounter fileStatCounter) {
        this.socket = socket;
        this.server = socketServer;
        this.fileStatCounter = fileStatCounter;
        logger.info("Registered new client");
    }

    /**
     * Back and force user data flow
     */
    public void run() {
        try {
            // Create character streams for the socket.
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            os = socket.getOutputStream();

            logger.info("Welcoming new client.");

            streamMessage(WELCOME_MSG);

            while (!stop) {
                String input = in.readLine();
                logger.info(String.format("client: %s input: %s", name, input));

                if (input == null) {
                    return;
                }

                if (input.startsWith("files")) {
                    streamMessage(server.listAvailableFiles());
                } else if (input.startsWith("help")) {
                    streamMessage(server.getHelpMessage());
                } else if (input.startsWith("get")) {
                    streamFile(input.substring(4).trim());
                } else if (input.startsWith("exit")) {
                    break;
                } else if (input.startsWith("client:")) {
                    name = input.substring("client:".length());
                } else {
                    String msg = "Unknown command: " + input + "\n" + HELP_MSG;
                    streamMessage(msg);
                    logger.warning(msg);
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,
                    String.format("Error establishing connection with client (%s)", name), e);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE,
                        String.format("Error closing client (%s) connection.", name), e);
            }
            server.getClients().remove(this);
            logger.info(String.format("Client(%s) closed connection.", name));
        }
    }

    private void println(String line) throws IOException {
        os.write((line + "\n").getBytes(Charset.forName("windows-1251")));
        os.flush();
    }

    private void streamMessage(String message) throws IOException {
        println("0@" + message.split("\r\n|\r|\n").length);
        println(message);
    }

    private void streamFile(String filename) throws IOException {
        String rootPath = server.getRootPath();
        String path = (rootPath.endsWith("/") ? rootPath : rootPath + "/") + filename;
        File file = new File(path);

        println(String.format("1@%s@%d", filename, (int) file.length()));

        fileStatCounter.openRead(filename);

        byte b[] = new byte[Constants.FILE_CHUNK];
        InputStream is = new FileInputStream(file);
        int numRead;
        int counter = 0;

        while ((numRead = is.read(b)) >= 0) {
            os.write(b, 0, numRead);
            counter += numRead;
        }
        os.flush();

        is.close();
        logger.fine(String.format("%d bytes of %s (file) are streamed to client: %s", counter,
                filename, name));
    }

    public void setStop(boolean stop) {
        this.stop = stop;
        try {
            println("-1");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write message to socket", e);
        }
    }
}
