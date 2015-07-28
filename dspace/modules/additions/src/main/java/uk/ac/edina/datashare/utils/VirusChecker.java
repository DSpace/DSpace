package uk.ac.edina.datashare.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Check file for viruses using clamscan.
 */
public class VirusChecker
{
    private static final Logger LOG = Logger.getLogger(VirusChecker.class);
    private static final Pattern PATTERN = Pattern.compile(".*Infected files: 0.*");
    
    private File file = null;
    
    /**
     * Initialise virus checker.
     * @param file The file to check.
     */
    public VirusChecker(File file)
    {
        if(!file.canRead())
        {
            throw new IllegalStateException(
                    "Can't virus check file: " + file.getName());
        }
        
        this.file = file;
    }
    
    /**
     * Initialise virus checker.
     * @param file The file to check.
     */
    public VirusChecker(String file)
    {
        this(new File(file));
    }
    
    /**
     * Is the file free of viruses?
     * @return True if the file is free of viruses.
     */
    public boolean isVirusFree()
    {
        boolean virusFree = false;
        BufferedReader stdInput = null;
        
        try
        {
            // spawn clamscan process and wait for result
            Process p = Runtime.getRuntime().exec(new String[] {"clamdscan", file.getPath()});
           
            p.waitFor();
            int retVal = p.exitValue();
            if(retVal == 0)
            {
                // good status returned check if pattern is in output 
                stdInput = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                
                String s = null;
                while((s = stdInput.readLine()) != null)
                {
                    Matcher matchHandle = PATTERN.matcher(s);
                    if(matchHandle.find())
                    {                
                        virusFree = true;
                        break;
                    }
                }
            }
        }
        catch(InterruptedException ex)
        {
            throw new RuntimeException(ex);
        }
        catch(IOException ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            if(stdInput != null)
            {
                try{stdInput.close();} catch (Exception e){LOG.warn(e);}
            }
        }

        if(!virusFree)
        {
            LOG.warn("*** File " + file + " has failed virus check.");
        }

        return virusFree;
    }
}

