/* SWORDIngesterFactory.java
 * 
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */ 

package org.dspace.sword;

import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Collection;
import org.dspace.content.Item;

import org.purl.sword.base.Deposit;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.ErrorCodes;
import org.apache.log4j.Logger;

/**
 * Factory class which will mint objects conforming to the
 * SWORDIngester interface.
 * 
 * @author Richard Jones
 *
 */
public class SWORDIngesterFactory
{
	private static Logger log = Logger.getLogger(SWORDIngesterFactory.class);

	/**
	 * Generate an object which conforms to the SWORDIngester interface.
	 * This Factory method may use the given DSpace context and the given
	 * SWORD Deposit request to decide on the most appropriate implementation
	 * of the interface to return.
	 * 
	 * To configure how this method will respond, configure the package ingester
	 * for the appropriate media types and defaults.  See the sword configuration
	 * documentation for more details.
	 * 
	 * @param context
	 * @param deposit
	 * @return
	 * @throws DSpaceSWORDException
	 */
	public static SWORDIngester getInstance(Context context, Deposit deposit, DSpaceObject dso)
            throws DSpaceSWORDException, SWORDErrorException
    {
		if (dso instanceof Collection)
		{
			SWORDIngester ingester = (SWORDIngester) PluginManager.getNamedPlugin(SWORDIngester.class, deposit.getPackaging());
			if (ingester == null)
			{
				throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "No ingester configured for this package type");
			}
			return ingester;
		}
		else if (dso instanceof Item)
		{
			SWORDIngester ingester = (SWORDIngester) PluginManager.getNamedPlugin(SWORDIngester.class, "SimpleFileIngester");
			if (ingester == null)
			{
				throw new DSpaceSWORDException("SimpleFileIngester is not configured in plugin manager");
			}
			return ingester;
		}

		throw new DSpaceSWORDException("No ingester could be found which works for this DSpace Object");
	}
}
