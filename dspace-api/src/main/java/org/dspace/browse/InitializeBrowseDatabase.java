/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * Command-line executed class for initializing the Browse tables of the DSpace database.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class InitializeBrowseDatabase
{
    /** log4j category */
    private static Logger log = Logger.getLogger(InitializeBrowseDatabase.class);

    public static void main(String[] argv)
    {
    	
        // Usage checks
        if (argv.length != 1)
        {
            log.warn("Schema file not specified");
            System.exit(1);
        }

        ConfigurationManager.loadConfig(null);
        log.info("Initializing Browse Database");

        try
        {
            if("clean-database.sql".equals(argv[0]))
            {
                try
                {
                    IndexBrowse browse = new IndexBrowse();
                    browse.setDelete(true);
                    browse.setExecute(true);
                    browse.clearDatabase();
                }
                catch (BrowseException e)
                {
                    log.error(e.getMessage(),e);
                    throw new IllegalStateException(e.getMessage(),e);
                }
            }
            else
            {
                try
                {
                    IndexBrowse browse = new IndexBrowse();
                    browse.setRebuild(true);
                    browse.setExecute(true);
                    browse.initBrowse();
                }
                catch (BrowseException e)
                {
                    log.error(e.getMessage(),e);
                    throw new IllegalStateException(e.getMessage(),e);
                }
            }
            
            System.exit(0);
        }
        catch (Exception e)
        {
            log.fatal("Caught exception:", e);
            System.exit(1);
        }
    }


}
