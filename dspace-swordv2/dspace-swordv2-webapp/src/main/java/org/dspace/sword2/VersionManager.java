/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class VersionManager
{
	public void emptyBundle(Item item, String name)
			throws SQLException, AuthorizeException, IOException
	{
		boolean keep = ConfigurationManager.getBooleanProperty("swordv2-server", "versions.keep");
		Bundle[] bundles = item.getBundles(name);
		for (Bundle b : bundles)
		{
			this.emptyBundle(item, b, keep);
		}
	}

	public void emptyBundle(Item item, Bundle source)
			throws SQLException, AuthorizeException, IOException
	{
		boolean keep = ConfigurationManager.getBooleanProperty("swordv2-server", "versions.keep");
		this.emptyBundle(item, source, keep);
	}

	public void emptyBundle(Item item, Bundle source, boolean archive)
			throws SQLException, AuthorizeException, IOException
	{
		if (archive)
		{
			this.archiveBundle(item, source);
		}

		for (Bitstream bitstream : source.getBitstreams())
		{
			source.removeBitstream(bitstream);
		}
	}

	public void removeBitstream(Item item, Bitstream bitstream)
			throws SQLException, AuthorizeException, IOException
	{
		boolean keep = ConfigurationManager.getBooleanProperty("swordv2-server", "versions.keep");
		this.removeBitstream(item, bitstream, keep);
	}

	public void removeBitstream(Item item, Bitstream bitstream, boolean keep)
			throws SQLException, AuthorizeException, IOException
	{
		Bundle exempt = null;
		if (keep)
		{
			exempt = this.archiveBitstream(item, bitstream);
		}

		Bundle[] bundles = bitstream.getBundles();
		for (Bundle bundle : bundles)
		{
			if (exempt != null && bundle.getID() != exempt.getID())
			{
				bundle.removeBitstream(bitstream);
			}
		}
	}

	public Bundle archiveBitstream(Item item, Bitstream bitstream)
			throws SQLException, AuthorizeException, IOException
	{
		String swordBundle = ConfigurationManager.getProperty("swordv2-server", "bundle.name");
		if (swordBundle == null)
		{
			swordBundle = "SWORD";
		}
		
		Bundle[] swords = item.getBundles(swordBundle);
		Bundle archive = null;
		if (swords.length == 0)
		{
			archive = item.createBundle(swordBundle);
		}
		else
		{
			archive = swords[0];
		}
		this.archiveBitstream(archive, bitstream);
		return archive;
	}

	public void archiveBitstream(Bundle target, Bitstream bitstream)
			throws SQLException, AuthorizeException, IOException
	{
		target.addBitstream(bitstream);
	}

	public void archiveBundle(Item item, Bundle source)
			throws SQLException, AuthorizeException, IOException
	{
		// get the datestamped root bundle name
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		String oldName = "V_" + sdf.format(new Date());
		oldName = this.getNumberedName(item, oldName, 0);

		Bundle old = item.createBundle(oldName);
		for (Bitstream bitstream : source.getBitstreams())
		{
			old.addBitstream(bitstream);
		}
	}

	private String getNumberedName(Item item, String name, int number)
			throws SQLException
	{
		String nName = name + "." + Integer.toString(number);
		if (item.getBundles(nName) == null || item.getBundles(nName).length == 0)
		{
			return nName;
		}
		else
		{
			return this.getNumberedName(item, name, number + 1);
		}
	}
}
