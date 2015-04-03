/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.logging;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

import org.junit.*;

public class SimpleLogFileTest
{
   
    @Test
    public void testLogEntriesCount()
    {
        URL url = this.getClass().getResource("/dspaceFolder/log/dspace.log");
        File file = new File(url.getFile());
        
        assertTrue("Test log file not found", file.exists());
        assertTrue("Test log file not readable", file.canRead());
        
        SimpleLogFile logFile = new SimpleLogFile(file);
        int i = 0;
        for (SimpleLogEntry logEntry : logFile)
        {
            i++;
        }
        assertEquals("Log entries count failed", 77, i);
    }

}
