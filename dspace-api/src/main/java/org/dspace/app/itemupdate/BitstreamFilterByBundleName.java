/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;

/**
 *   BitstreamFilter implementation to filter by bundle name
 *
 */
public class BitstreamFilterByBundleName extends BitstreamFilter {

	protected String bundleName;
	
	public BitstreamFilterByBundleName()
	{
		//empty
	}

	/**
	 *    Filter bitstream based on bundle name found in properties file
	 *    
	 *    @param bitstream Bitstream
	 *    @throws BitstreamFilterException if filter error
	 *    @return whether bitstream is in bundle
	 *    
	 */
	@Override
	public boolean accept(Bitstream bitstream)
	throws BitstreamFilterException
	{
		if (bundleName == null)
		{
			bundleName = props.getProperty("bundle");
			if (bundleName == null)
			{
				throw new BitstreamFilterException("Property 'bundle' not found.");
			}			
		}
		
		try
		{
			List<Bundle> bundles = bitstream.getBundles();
			for (Bundle b : bundles)
			{
                if (b.getName().equals(bundleName))
				{
					return true;
				}
			}		
		}
		catch(SQLException e)
		{
			throw new BitstreamFilterException(e);
		}
		return false;
	}
	
}
