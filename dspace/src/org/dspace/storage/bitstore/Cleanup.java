/*
 * Cleanup.java
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
package org.dspace.storage.bitstore;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.apache.log4j.Logger;

/**
 * Cleans up asset store.
 * 
 * @author Peter Breton
 * @version $Revision$
 */
public class Cleanup
{
    /** log4j log */
    private static Logger log = Logger.getLogger(Cleanup.class);

    /**
     * Cleans up asset store.
     * 
     * @param argv -
     *            Command-line arguments
     */
    public static void main(String[] argv)
    {
        try
        {
            log.info("Cleaning up asset store");
            
            // set up command line parser
            CommandLineParser parser = new PosixParser();
            CommandLine line = null;

            // create an options object and populate it
            Options options = new Options();

            options.addOption("l", "leave", false, "Leave database records but delete file from assetstore");
            options.addOption("h", "help", false, "Help");
            
            try
            {            	
                line = parser.parse(options, argv);
            }
            catch (ParseException e)
            {
                log.fatal(e);
                System.exit(1);
            }
            
            // user asks for help
            if (line.hasOption('h'))
            {
                printHelp(options);
                System.exit(0);
            }

            boolean deleteDbRecords = true;
            // Prune stage
            if (line.hasOption('l'))
            {
            	log.debug("option l used setting flag to leave db records");
                deleteDbRecords = false;    
            }
           	log.debug("leave db records = " + deleteDbRecords);
            BitstreamStorageManager.cleanup(deleteDbRecords);
            
            System.exit(0);
        }
        catch (Exception e)
        {
            log.fatal("Caught exception:", e);
            System.exit(1);
        }
    }
    
    private static void printHelp(Options options)
    {
        HelpFormatter myhelp = new HelpFormatter();
        myhelp.printHelp("Cleanup\n", options);
    }

}
