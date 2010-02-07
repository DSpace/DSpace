/*
 * DownloadSpiderIPs.java
 *
 * Version: $Revision:$
 *
 * Date: $Date:$
 *
 * Copyright (c) 2002-2010, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

package org.dspace.statistics.util;

import org.apache.tools.ant.taskdefs.Get;
import org.dspace.core.ConfigurationManager;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;

/**
 * Class to download and process lists of search engine spider IP addresses
 *
 * @author Stuart Lewis
 * @author Mark Diggory (mdiggory at atmire.com)
 */
public class DownloadSpiderIPs
{
    /** Vector of entries */
    private static Vector<String> ips;

    /**
     * Main method to run the script
     *
     * @param args The command line arguments
     */
	public static void main(String[] args)
    {
	    try
        {
            System.out.println("Downloading latest spider IP addresses:");
            ips = new Vector<String>();

            // Get the list URLs to download from
            String urls = ConfigurationManager.getProperty("solr.spiderips.urls");
            if ((urls == null) || ("".equals(urls)))
            {
                System.err.println(" - Missing setting from dspace.cfg: solr.spiderips.urls");
                System.exit(0);
            }

            // Get the location of spiders directory
            File spiders = new File(ConfigurationManager.getProperty("dspace.dir"),"config/spiders");

            if(!spiders.exists())
                spiders.mkdirs();

            String[] values = urls.split(",");
            for (String value : values)
            {
                value = value.trim();
                System.out.println(" Downloading: " + value);

                URL url = new URL(value);

                Get get = new Get();
                get.setDest(new File(spiders, url.getHost() + url.getPath().replace("/","-")));
                get.setSrc(url);
                get.setUseTimestamp(true);
                get.execute();

            }


        } catch (Exception e)
        {
            System.err.println(" - Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }



}
