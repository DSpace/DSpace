/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.logging;

import java.util.Date;

public class SimpleLogEntry implements Record
{
    private String filename;

    private int lineNumber;

    private Date date;

    private String level;

    private String loggingClass;

    private String message;

    private boolean valid;

    public static final String ERROR_LEVEL = "ERROR";

    public static final String WARNING_LEVEL = "WARN";

    public static final String INFO_LEVEL = "INFO";

    public static final String DEBUG_LEVEL = "DEBUG";

    public static final String FATAL_LEVEL = "FATAL";

    public static final String CRITICAL_LEVEL = "CRIT";

    public SimpleLogEntry()
    {
        this.lineNumber = 0;
        this.date = null;
        this.level = null;
        this.loggingClass = null;
        this.message = null;
    }

    public SimpleLogEntry(Date date, String level, String loggingClass,
            String message)
    {
        this.lineNumber = 0;
        this.date = date;
        this.level = level;
        this.loggingClass = loggingClass;
        this.message = message;
    }

    public String getFilename()
    {
        return filename;
    }

    public int getLineNumber()
    {
        return lineNumber;
    }

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public void setLevel(String level)
    {
        this.level = level;
    }

    public void setLoggingClass(String loggingClass)
    {
        this.loggingClass = loggingClass;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getLevel()
    {
        return level;
    }

    public String getLoggingClass()
    {
        return loggingClass;
    }

    public String getMessage()
    {
        return message;
    }

    public void setLineNumber(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public boolean isValid()
    {
        return valid;
    }

    public void setValid(boolean valid)
    {
        this.valid = valid;
    }

    public boolean isError()
    {
        return ERROR_LEVEL.equals(level);
    }

    public boolean isWarning()
    {
        return WARNING_LEVEL.equals(level);
    }

    public boolean isInfo()
    {
        return INFO_LEVEL.equals(level);
    }

    public boolean isDebug()
    {
        return DEBUG_LEVEL.equals(level);
    }

    public boolean isFatal()
    {
        return FATAL_LEVEL.equals(level);
    }

    public boolean isCritical()
    {
        return CRITICAL_LEVEL.equals(level);
    }

    public String toString()
    {
        return String.format("" + "Date: %s\n" + "Level: %s\n"
                + "Logging class: %s\n" + "Message: %s\n", date.toString(),
                level, loggingClass, message);
    }

}
