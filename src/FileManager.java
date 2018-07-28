import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;


/**
 * This class defines simple utility functions for performing I/O.
 *
 * @author Euan Freeman (euan.freeman@glasgow.ac.uk)
 */

public class FileManager {
	public static void append(String filepath, String s) throws IOException {
        Path path = FileSystems.getDefault().getPath(filepath);
        BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        bufferedWriter.write(s);
        bufferedWriter.newLine();
        bufferedWriter.close();
    }

    public static boolean fileExists(String filepath) {
        return FileSystems.getDefault().getPath(filepath).toFile().exists();
    }
}
