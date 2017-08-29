package kg.talaszh.cmdexplorer.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import kg.talaszh.cmdexplorer.ConsoleHelper;
import kg.talaszh.cmdexplorer.Constants;

/**
 * Created by talas on 8/24/17.
 */
public class SocketClient {

    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    private int port;
    private volatile boolean stop = false;

    interface ProgressListener {

        void bytesRead(int bytes);
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {

        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        if (handlers.length > 0 && handlers[0] instanceof ConsoleHandler) {
            rootLogger.removeHandler(handlers[0]);
        }

        FileHandler logfile = new FileHandler("cmd-explorer-client.log", true);
        logfile.setFormatter(new SimpleFormatter());
        logger.addHandler(logfile);

        int serverPort = 9001;
        if (args.length >= 1) {
            serverPort = Integer.valueOf(args[0]);
            if (args.length >= 2) {
                logger.setLevel(Level.parse(args[1]));
            }
        } else {
            System.out.println(String.format("Program usage: java -cp {classpath} %s "
                            + "{serverPort:9001} \n"
                            + "{loggingLevel:%s}", SocketClient.class.getCanonicalName(),
                    Arrays.toString(new String[]{Level.ALL.getName(),
                            Level.FINEST.getName(), Level.FINER.getName(), Level.FINE.getName(),
                            Level.INFO.getName(), Level.WARNING.getName(), Level.SEVERE.getName(),
                            Level.OFF.getName()})));
        }
        logger.fine("Client application logging successfully configured.");
        SocketClient client = new SocketClient(serverPort);
        client.run();
    }

    public SocketClient(int port) {
        this.port = port;
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() {

        // Make connection and initialize streams
        String serverAddress = "localhost";
        try (Socket socket = new Socket(serverAddress, port);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                InputStream is = socket.getInputStream()) {

            spawnAroundUserInput(out);
            listenServerResponse(out, is);

            while (!stop) {
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to establish server connection.", e);
        }
    }

    private void spawnAroundUserInput(PrintWriter out) {
        new Thread(() -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                while (!stop) {
                    while (!reader.ready() && !stop) {
                        Thread.sleep(200);
                    }
                    if (stop) {
                        ConsoleHelper.printlnRedBold("\nSession closed.");
                        break;
                    }
                    String input = reader.readLine();
                    out.println(input);
                    if ("exit".equals(input)) {
                        ConsoleHelper.printlnRedBold("Session closed.");
                        stop = true;
                        break;
                    }
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error to read user input.", e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Thread sleep interrupted.", e);
            }
        }).start();
    }

    private void listenServerResponse(PrintWriter out, InputStream is) {
        new Thread(() -> {
            out.println("client:" + UUID.randomUUID().toString().substring(0, 8));
            ConsoleHelper.printYellow("Connection established");
            while (!stop) {
                // Server should send welcome message.
                try {
                    handleResponse(is);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Failed to handle server response");
                    stop = true;
                }
                if (!stop) {
                    ConsoleHelper.printGreenBold("console_> ");
                }
            }
            out.println("exit");
        }).start();
    }

    /**
     * Process all messages from server, according to the protocol.
     * General Server/Client communication protocol is as follows:
     * {Type}@{Args}
     *
     * For the moment there is two types of message formats:
     * 1. Text ({@link SocketClient#handleMessage(InputStream, String)})
     * 2. File ({@link SocketClient#handleFile(InputStream, String)})
     */
    private void handleResponse(InputStream is) throws IOException {
        String lineS = readLine(is).trim();
        if (lineS.startsWith("0")) {
            handleMessage(is, lineS);
        } else if (lineS.startsWith("1")) {
            handleFile(is, lineS);
        } else if (lineS.startsWith("-1")) {
            logger.info("Should stop all threads");
            stop = true;
        }
    }

    /**
     * Read bytes from stream till new line is read.
     *
     * @param is stream to read bytes from
     * @return line read
     * @throws IOException if I/O error occurs
     */
    private String readLine(InputStream is) throws IOException {
        StringBuilder line = new StringBuilder();
        String str;
        Integer b;
        while ((b = is.read()) != 10) {
            str = new String(new byte[]{b.byteValue()}, Charset.forName("windows-1251"));
            line.append(str);
        }
        return line.toString();
    }

    /**
     * Message format: {@code {Type}@{Lines}}
     * where {Type}: 0
     * and {Lines}: is integer type value.
     * After follows number of {Lines} strings
     *
     * @param is stream to read data from
     * @param msgParams message stream arguments
     * @throws IOException if I/O error occurs
     */
    private void handleMessage(InputStream is, String msgParams) throws IOException {
        String[] msgArgs = msgParams.split("@");
        if (msgArgs.length == 2) {
            int numLines = Integer.valueOf(msgArgs[1]);
            for (; numLines > 0; numLines--) {
                System.out.println(readLine(is));
            }
        } else {
            logger.warning(String.format("Message (%s) doesn't match format(%s)", msgParams,
                    "{Type}@{Lines}"));
        }
    }

    /**
     * Message format: {@code {Type}@{Filename}@{ByteSize}}
     * where
     * {Type}: 1
     * {Filename}: filename
     * {ByteSize}: file size in bytes
     * After follows number of {ByteSize} bytes of data
     *
     * @param is stream to read data from
     * @param fileParams message stream arguments
     * @throws IOException if I/O error occurs
     */
    private void handleFile(InputStream is, String fileParams) throws IOException {
        String[] fileArgs = fileParams.split("@");
        if (fileArgs.length == 3) {
            File file = new File(fileArgs[1]);

            System.out.println(file.getAbsolutePath());
            int len = Integer.valueOf(fileArgs[2]);
            writeToFile(is, file, len);
        } else {
            logger.warning(String.format("Message (%s) doesn't match format(%s)", fileParams,
                    "{Type}@{Filename}@{ByteSize}"));
        }
    }

    /**
     * Write bytes to file
     * @param ins data stream
     * @param file output file
     * @param len total number of bytes
     * @throws java.io.IOException if I/O error occurs
     */
    private void writeToFile(InputStream ins, File file, int len) throws
            java.io.IOException {

        FileOutputStream fos = new FileOutputStream(file);
        ProgressBar progressBar = new ProgressBar(len, file.getName());
        toFile(ins, fos, len, Constants.FILE_CHUNK, progressBar::setVal);
        fos.flush();
        fos.close();
        progressBar.finish();
    }

    /**
     * Read bytes from stream and track progress
     * @param ins data stream
     * @param fos file stream
     * @param len bytes left to read
     * @param buf_size buffer size
     * @param listener progress listener
     * @throws java.io.IOException if I/O error occurs
     */
    private void toFile(InputStream ins, FileOutputStream fos, int len, int buf_size,
            ProgressListener listener) throws
            java.io.IOException {

        byte[] buffer = new byte[buf_size];

        int len_read = 0;
        int total_len_read = 0;

        while (total_len_read + buf_size <= len) {
            len_read = ins.read(buffer);
            total_len_read += len_read;
            listener.bytesRead(len_read);
            fos.write(buffer, 0, len_read);
        }

        if (total_len_read < len) {
            toFile(ins, fos, len - total_len_read, buf_size / 2, listener);
        }
    }
}
