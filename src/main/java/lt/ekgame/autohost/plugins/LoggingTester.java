package lt.ekgame.autohost.plugins;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LoggingTester {
    private final Logger logger = Logger.getLogger(LoggingTester.class
            .getName());
    private FileHandler fh = null;

    public LoggingTester() {
        //just to make our log file nicer :)
        SimpleDateFormat format = new SimpleDateFormat("M-d_HHmmss");
        try {
            fh = new FileHandler("C:/Users/Tomas/git/osu-auto-host/MyLogFile_"
                + format.format(Calendar.getInstance().getTime()) + ".log");
        } catch (Exception e) {
            e.printStackTrace();
        }

        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
    }

    public void doLogging() {
        logger.info("info msg");
        logger.severe("error message");
        logger.fine("fine message"); //won't show because to high level of logging
    }
}   