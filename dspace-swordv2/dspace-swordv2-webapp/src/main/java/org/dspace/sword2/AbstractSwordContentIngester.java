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
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public abstract class AbstractSwordContentIngester implements SwordContentIngester
{
	public abstract DepositResult ingest(Context context, Deposit deposit, DSpaceObject dso, VerboseDescription verboseDescription)
			throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException;

    public abstract DepositResult ingest(Context context, Deposit deposit, DSpaceObject dso, VerboseDescription verboseDescription, DepositResult result)
			throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException;

	protected BitstreamFormat getFormat(Context context, String fileName)
			throws SQLException
	{
		String fext = null;
		int lastDot = fileName.lastIndexOf(".");
		if (lastDot > -1)
		{
			fext = fileName.substring(lastDot + 1);
		}

		if (fext == null)
		{
			return null;
		}

		BitstreamFormat[] formats = BitstreamFormat.findAll(context);
		for (BitstreamFormat format : formats)
		{
			String[] extensions = format.getExtensions();
			for (String ext : extensions)
			{
				if (ext.equals(fext))
				{
					return format;
				}
			}
		}
		return null;
	}
	
	/**
	 * Add the current date to the item metadata.  This looks up
	 * the field in which to store this metadata in the configuration
	 * sword.updated.field
	 *
	 * @param item
	 * @throws DSpaceSwordException
	 */
	protected void setUpdatedDate(Item item, VerboseDescription verboseDescription)
			throws DSpaceSwordException
	{
		String field = ConfigurationManager.getProperty("swordv2-server", "updated.field");
		if (field == null || "".equals(field))
		{
			throw new DSpaceSwordException("No configuration, or configuration is invalid for: sword.updated.field");
		}

		DCValue dc = this.configToDC(field, null);
		item.clearMetadata(dc.schema, dc.element, dc.qualifier, Item.ANY);
		DCDate date = new DCDate(new Date());
		item.addMetadata(dc.schema, dc.element, dc.qualifier, null, date.toString());

		verboseDescription.append("Updated date added to response from item metadata where available");
	}

	/**
	 * Store the given slug value (which is used for suggested identifiers,
	 * and which DSpace ignores) in the item metadata.  This looks up the
	 * field in which to store this metadata in the configuration
	 * sword.slug.field
	 *
	 * @param item
	 * @param slugVal
	 * @throws DSpaceSwordException
	 */
	protected void setSlug(Item item, String slugVal, VerboseDescription verboseDescription)
			throws DSpaceSwordException
	{
		// if there isn't a slug value, don't set it
		if (slugVal == null)
		{
			return;
		}

		String field = ConfigurationManager.getProperty("swordv2-server", "slug.field");
		if (field == null || "".equals(field))
		{
			throw new DSpaceSwordException("No configuration, or configuration is invalid for: sword.slug.field");
		}

		DCValue dc = this.configToDC(field, null);
		item.clearMetadata(dc.schema, dc.element, dc.qualifier, Item.ANY);
		item.addMetadata(dc.schema, dc.element, dc.qualifier, null, slugVal);

		verboseDescription.append("Slug value set in response where available");
	}

	/**
	 * utility method to turn given metadata fields of the form
	 * schema.element.qualifier into DCValue objects which can be
	 * used to access metadata in items.
	 *
	 * The def parameter should be null, * or "" depending on how
	 * you intend to use the DCValue object
	 *
	 * @param config
	 * @param def
	 * @return
	 */
	protected DCValue configToDC(String config, String def)
	{
		DCValue dcv = new DCValue();
		dcv.schema = def;
		dcv.element= def;
		dcv.qualifier = def;

		StringTokenizer stz = new StringTokenizer(config, ".");
		dcv.schema = stz.nextToken();
		dcv.element = stz.nextToken();
		if (stz.hasMoreTokens())
		{
			dcv.qualifier = stz.nextToken();
		}

		return dcv;
	}
}
