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
public class OriginalWithDerivativesBitstreamFilter extends BitstreamFilter 
{
    protected String[] bundlesToEmpty = { "ORIGINAL", "TEXT", "THUMBNAIL" };
	
	public OriginalWithDerivativesBitstreamFilter()
	{
		//empty
	}
	
	/**
	 * 	Tests bitstream for membership in specified bundles (ORIGINAL, TEXT, THUMBNAIL)
	 * 
	 * @param bitstream Bitstream
	 * @throws BitstreamFilterException if error
	 * @return true if bitstream is in specified bundles
	 */
	@Override
    public boolean accept(Bitstream bitstream)
	throws BitstreamFilterException
	{		
		try
		{
			List<Bundle> bundles = bitstream.getBundles();
			for (Bundle b : bundles)
			{
                for (String bn : bundlesToEmpty)
				{
					if (b.getName().equals(bn))
					{
						return true;
					}
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
