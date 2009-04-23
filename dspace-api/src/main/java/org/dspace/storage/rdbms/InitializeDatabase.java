/*
 * InitializeDatabase.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.storage.rdbms;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.dspace.browse.BrowseException;
import org.dspace.browse.IndexBrowse;
import org.dspace.core.ConfigurationManager;

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
    private static Logger log = Logger.getLogger(InitializeDatabase.class);

    public static void main(String[] argv)
    {
    	
        // Usage checks
        if (argv.length != 1)
        {
            log.warn("Schema file not specified");
            System.exit(1);
        }

        ConfigurationManager.loadConfig(null);
        log.info("Initializing Database");

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
                    throw new RuntimeException(e.getMessage(),e);
                }
                
                DatabaseManager.loadSql(getScript(argv[0]));
                
            }
            else
            {
                
                DatabaseManager.loadSql(getScript(argv[0]));
                
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
                    throw new RuntimeException(e.getMessage(),e);
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

    /**
     * Attempt to get the named script, with the following rules:
     * etc/<db.name>/<name>
     * etc/<name>
     * <name>
     */
    private static FileReader getScript(String name) throws FileNotFoundException, IOException
    {
        String dbName = ConfigurationManager.getProperty("db.name");
        File myFile = null;
        
        if (dbName != null)
        {
            myFile = new File("etc/" + dbName + "/" + name);
            if (myFile.exists())
                return new FileReader(myFile.getCanonicalPath());
        }
        
        myFile = new File("etc/" + name);
        if (myFile.exists())
            return new FileReader(myFile.getCanonicalPath());
        
        return new FileReader(name);
    }
}
