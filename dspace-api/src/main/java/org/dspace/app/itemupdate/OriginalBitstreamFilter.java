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
 * 		Filter all bitstreams in the ORIGINAL bundle
 *      Also delete all derivative bitstreams, i.e.
 *      all bitstreams in the TEXT and THUMBNAIL bundles
 */
public class OriginalBitstreamFilter extends BitstreamFilterByBundleName 
{	
	public OriginalBitstreamFilter()
	{
		//empty
	}
	
	/**
	 *   Tests bitstreams for containment in an ORIGINAL bundle
	 *  @param bitstream Bitstream
	 *  @return true if the bitstream is in the ORIGINAL bundle
	 *  
	 *  @throws BitstreamFilterException if filter error
	 */
	@Override
    public boolean accept(Bitstream bitstream)
	throws BitstreamFilterException
	{		
		try
		{
			List<Bundle> bundles = bitstream.getBundles();
			for (Bundle bundle : bundles)
			{
                if (bundle.getName().equals("ORIGINAL"))
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
