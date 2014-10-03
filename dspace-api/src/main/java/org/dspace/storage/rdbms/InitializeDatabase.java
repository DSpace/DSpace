/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

import org.apache.log4j.Logger;

/**
 * Command-line executed class for initializing the DSpace database. This should
 * be invoked with a single argument, the filename of the database schema file.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class InitializeDatabase
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(InitializeDatabase.class);

    public static void main(String[] argv)
    {
      
        // Usage checks
        /*if (argv.length != 1)
        {
            log.warn("Schema file not specified");
            System.exit(1);
        }*/

        log.info("Initializing Database");

        try
        {
            // This is just to ensure DatabaseManager.initialize() gets called!
            String dbKeyword = DatabaseManager.getDbKeyword();

            log.info("Done initializing " + dbKeyword + " Database");
            /*if("clean-database.sql".equals(argv[0]))
            {
               DatabaseManager.loadSql(getScript(argv[0]));
            }
            else
            {
               DatabaseManager.loadSql(getScript(argv[0]));
            }*/
            
            System.exit(0);
        }
        catch (Exception e)
        {
            log.fatal("Caught exception:", e);
            System.exit(1);
        }
    }

    /**
     * Attempt to get the named script, with the following rules:
     * etc/<DBMS name>/<name>
     * etc/<name>
     * <name>
     */
    private static FileReader getScript(String name) throws FileNotFoundException, IOException
    {
        String dbName = DatabaseManager.getDbKeyword();

        File myFile = null;
        
        if (dbName != null)
        {
            myFile = new File("etc/" + dbName + "/" + name);
            if (myFile.exists())
            {
                return new FileReader(myFile.getCanonicalPath());
            }
        }
        
        myFile = new File("etc/" + name);
        if (myFile.exists())
        {
            return new FileReader(myFile.getCanonicalPath());
        }
        
        return new FileReader(name);
    }
}
