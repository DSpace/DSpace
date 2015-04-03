/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.log4j.Logger;

public class SimpleLogFile implements Iterable<SimpleLogEntry>
{
    private static final Logger log = Logger.getLogger(SimpleLogFile.class);

    private InputStreamReader inputReader;

    public SimpleLogFile(String dirname, String filename)
    {
        this(new File(dirname, filename));
    }

    public SimpleLogFile(String filePath)
    {
        this(new File(filePath));
    }

    public SimpleLogFile(File file)
    {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }    
        else if (!file.exists())
        {
            log.warn(String.format("Logfile [%s] not found at [%s]",
                    file.getName(), file.getPath()));            
        }
        else if (file.exists() && file.length() == 0)
        {
            log.warn(String.format("Logfile [%s] at [%s] is empty",
                    file.getName(), file.getPath()));
        }
        else
        {
            try
            {
                this.inputReader = new InputStreamReader(new FileInputStream(
                        file), Charset.forName("UTF8"));
            }
            catch (FileNotFoundException e)
            {
                log.error(e.toString());
            }
        }

    }
    
    public RecordFileLineIterator<SimpleLogEntry> iterator()
    {
        return new RecordFileLineIterator<SimpleLogEntry>(inputReader,
                new SimpleLogEntryParser());
    }

}
