/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

public class SwordConfigurationDefault implements SwordConfiguration
{
    public boolean returnDepositReceipt()
    {
        return true;
    }

    public boolean returnStackTraceInError()
    {
        return true;
    }

    public boolean returnErrorBody()
    {
        return true;
    }

    public String generator()
    {
        return "http://www.swordapp.org/";
    }

    public String generatorVersion()
    {
        return "2.0";
    }

    public String administratorEmail()
    {
        return null;
    }

	public String getAuthType()
	{
		return "None";
	}

	public boolean storeAndCheckBinary()
	{
		return false;
	}

	public String getTempDirectory()
	{
		return null;
	}

	public int getMaxUploadSize()
	{
		return -1;
	}
}
