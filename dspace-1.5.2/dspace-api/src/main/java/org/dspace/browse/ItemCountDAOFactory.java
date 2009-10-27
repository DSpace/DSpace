/*
 * ItemCountDAOFactory.java
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.browse;

import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

/**
 * Factory class to allow us to load the correct DAO for registering
 * item count information
 * 
 * @author Richard Jones
 *
 */
public class ItemCountDAOFactory
{
	/**
	 * Get an instance of ItemCountDAO which supports the correct database
	 * for the specific DSpace instance.
	 * 
	 * @param context
	 * @return
	 * @throws ItemCountException
	 */
	public static ItemCountDAO getInstance(Context context)
		throws ItemCountException
	{
		String db = ConfigurationManager.getProperty("db.name");
		ItemCountDAO dao;
		if ("postgres".equals(db))
		{
			dao = new ItemCountDAOPostgres();
		}
		else if ("oracle".equals(db))
		{
			dao = new ItemCountDAOOracle();
		}
		else
		{
			throw new ItemCountException("Database type: " + db + " is not currently supported");
		}
		
		dao.setContext(context);
		return dao;
	}
}
