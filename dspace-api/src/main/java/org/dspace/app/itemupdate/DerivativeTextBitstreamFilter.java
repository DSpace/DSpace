/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.util.Properties;

/**
 *   Bitstream filter to delete from TEXT bundle
 *
 */
public class DerivativeTextBitstreamFilter extends BitstreamFilterByBundleName {
	
	public DerivativeTextBitstreamFilter()
	{
		props = new Properties();
		props.setProperty("bundle", "TEXT");
	}

}
