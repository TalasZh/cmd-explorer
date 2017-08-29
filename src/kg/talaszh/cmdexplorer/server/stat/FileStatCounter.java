package kg.talaszh.cmdexplorer.server.stat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by talas on 8/28/17.
 */
public class FileStatCounter implements Runnable {

    private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private volatile Map<String, Integer> stats;
    private String rootFolderPath;
    private String statsFilePath;

    public FileStatCounter(String rootFolderPath, String statsFilePath) {
        stats = new ConcurrentHashMap<>();
        this.rootFolderPath = rootFolderPath;
        this.statsFilePath = statsFilePath;
    }

    @Override
    public void run() {
        logger.fine("Triggered repeated job");

        File rootFolder = new File(rootFolderPath);
        if (!rootFolder.isDirectory()) {
            throw new IllegalStateException("Root should be folder type.");
        }

        File[] files = rootFolder.listFiles(File::isFile);
        if (files != null) {
            for (File file : files) {
                stats.putIfAbsent(file.getName(), 0);
            }
        }

        StringBuilder sb = new StringBuilder("Download stats:");

        String longestStr = stats.keySet()
                .stream()
                .max((o1, o2) -> (o1.length() < o2.length()) ? -1
                        : ((o1.length() == o2.length()) ? 0 : 1))
                .orElse("");
        stats.forEach((key, value) -> sb.append("\n")
                .append(String.format("%-" + longestStr.length() + "s | %,d", key, value)));
        logger.fine(sb.toString());
        try {
            File statFile = new File(statsFilePath);
            Files.write(statFile.toPath(), sb.toString().getBytes(), StandardOpenOption
                    .CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error writing stats to file.", e);
        }
    }

    public void openRead(String filename) throws IOException {
        increment(filename);
    }

    private void increment(String filename) {
        stats.compute(filename, (fName, counter) -> counter == null ? 1 : ++counter);
    }
}
