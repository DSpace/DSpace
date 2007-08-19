/*
 * BrowseDAOFactory.java
 *
 * Version: $Revision: $
 *
 * Date: $Date:  $
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

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Factory class to generate DAOs based on the configuration
 * 
 * @author Richard Jones
 *
 */
public class BrowseDAOFactory
{
	/**
	 * Get an instance of the relevant Read Only DAO class, which will
	 * conform to the BrowseDAO interface
	 * 
	 * @param context	the DSpace context
	 * @return			the relevant DAO
	 * @throws BrowseException
	 */
	public static BrowseDAO getInstance(Context context)
		throws BrowseException
	{
		String db = ConfigurationManager.getProperty("db.name");
		if ("postgres".equals(db))
		{
			return new BrowseDAOPostgres(context);
		}
		else if ("oracle".equals(db))
		{
            return new BrowseDAOOracle(context);
		}
		else
		{
			throw new BrowseException("The configuration for db.name is either invalid, or contains an unrecognised database");
		}
	}
	
	/**
	 * Get an instance of the relevant Write Only DAO class, which will
	 * conform to the BrowseCreateDAO interface
	 * 
	 * @param context	the DSpace context
	 * @return			the relevant DAO
	 * @throws BrowseException
	 */
	public static BrowseCreateDAO getCreateInstance(Context context)
		throws BrowseException
	{
		String db = ConfigurationManager.getProperty("db.name");
		if ("postgres".equals(db))
		{
			return new BrowseCreateDAOPostgres(context);
		}
		else if ("oracle".equals(db))
		{
            return new BrowseCreateDAOOracle(context);
		}
		else
		{
			throw new BrowseException("The configuration for db.name is either invalid, or contains an unrecognised database");
		}
	}

    /**
     * Get an instance of the relevant Read Only DAO class, which will
     * conform to the BrowseItemDAO interface
     *
     * @param context	the DSpace context
     * @return			the relevant DAO
     * @throws BrowseException
     */
    public static BrowseItemDAO getItemInstance(Context context)
        throws BrowseException
    {
        String db = ConfigurationManager.getProperty("db.name");
        if ("postgres".equals(db))
        {
            return new BrowseItemDAOPostgres(context);
        }
        else if ("oracle".equals(db))
        {
            return new BrowseItemDAOOracle(context);
        }
        else
        {
            throw new BrowseException("The configuration for db.name is either invalid, or contains an unrecognised database");
        }
    }

    /**
	 * Get an instance of the relevant DAO Utilities class, which will
	 * conform to the BrowseDAOUtils interface
	 * 
	 * @param context	the DSpace context
	 * @return			the relevant DAO
	 * @throws BrowseException
	 */
	public static BrowseDAOUtils getUtils(Context context)
		throws BrowseException
	{
		String db = ConfigurationManager.getProperty("db.name");
		if ("postgres".equals(db))
		{
			return new BrowseDAOUtilsPostgres();
		}
		else if ("oracle".equals(db))
		{
            return new BrowseDAOUtilsOracle();
		}
		else
		{
			throw new BrowseException("The configuration for db.name is either invalid, or contains an unrecognised database");
		}
	}
}
