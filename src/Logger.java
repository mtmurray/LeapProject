import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class implements basic logging functionality.
 *
 * @author Euan Freeman (euan.freeman@glasgow.ac.uk)
 */

public class Logger {
	private static final Object syncLock = new Object();

    private final String filename;

    public Logger(String filename) {
        this.filename = filename;
    }

    /**
     * Writes the given message to the log file.
     *
     * @return True if the message was written successfully.
     */
    public boolean write(String message) {
        return write(message, false);
    }

    /**
     * Writes the given message to the log file. If the timestamp param is true, the message will be
     * preceeded by a timestamp.
     *
     * @return True if the message was written successfully.
     */
    public boolean write(String message, boolean timestamp) {
        if (timestamp) {
            String now = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
            message = String.format("%s#%s", now, message);
        }

        return write(this.filename, message);
    }

    /**
     * Writes the given message to the specified filename.
     *
     * @return True if the message was written successfully.
     */
    public static boolean write(String filename, String message) {
        synchronized (syncLock) {
            try {
                FileManager.append(filename, message);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

}
