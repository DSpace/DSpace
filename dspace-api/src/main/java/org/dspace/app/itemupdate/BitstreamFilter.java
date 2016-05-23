/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.io.IOException;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import org.dspace.content.Bitstream;

/**
 *   Filter interface to be used by ItemUpdate 
 *   to determine which bitstreams in an Item
 *   acceptable for removal.
 *
 */
public abstract class BitstreamFilter {

	protected Properties props = null; 
	
	/**
	 *  The filter method 
	 *  
	 * @param bitstream Bitstream
	 * @return whether the bitstream matches the criteria
	 * @throws BitstreamFilterException if filter error
	 */
	public abstract boolean accept(Bitstream bitstream) throws BitstreamFilterException;
	
	/**
	 * 
	 * @param filepath - The complete path for the properties file
	 * @throws IOException if IO error
	 */
	public void initProperties(String filepath)
	throws IOException
	{
		props = new Properties();
		
		InputStream in = null;

        try
        {
            in = new FileInputStream(filepath);
            props.load(in);
        }
        finally
        {
            if (in != null)
            {
                in.close();
            }
        }
	}
	
}
