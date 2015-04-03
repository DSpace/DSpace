/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.logging;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * 
 * @author Michal Josifko
 * 
 */
public class SimpleLogEntryParser implements RecordParser<SimpleLogEntry>
{

    private final static String LOG_ENTRY_PATTERN = "^(\\d{4}-\\d{2}-\\d{2} +\\d{2}:\\d{2}:\\d{2},\\d{3}) +([^ ]+) +([^ ]+) +@ +(.*)$";

    private final static Pattern pattern = Pattern.compile(LOG_ENTRY_PATTERN, Pattern.DOTALL);

    public SimpleLogEntry parse(String s)
    {

        SimpleLogEntry entry = new SimpleLogEntry();
        Matcher matcher = pattern.matcher(s);
        if (matcher.find())
        {
            String dateString = matcher.group(1);
            entry.setValid(true);
            entry.setLevel(matcher.group(2));
            entry.setLoggingClass(matcher.group(3));
            entry.setMessage(matcher.group(4));

            try
            {
                Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS")
                        .parse(dateString);
                entry.setDate(date);
            }
            catch (ParseException e)
            {
                entry.setValid(false);
            }
        }
        else
        {
            entry.setValid(false);
        }
        return entry;
    }

    public boolean matches(String s)
    {

        return pattern.matcher(s).matches();
    }

}
