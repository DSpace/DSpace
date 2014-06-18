/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.swordapp.server.AtomStatement;
import org.swordapp.server.Statement;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class AtomStatementDisseminator extends GenericStatementDisseminator implements SwordStatementDisseminator
{
	public Statement disseminate(Context context, Item item) throws DSpaceSwordException, SwordError, SwordServerException
	{
		SwordUrlManager urlManager = new SwordUrlManager(new SwordConfigurationDSpace(), context);
		String feedUri = urlManager.getAtomStatementUri(item);

		String authorField = ConfigurationManager.getProperty("swordv2-server", "author.field");
		String titleField = ConfigurationManager.getProperty("swordv2-server", "title.field");
		String updatedField = ConfigurationManager.getProperty("swordv2-server", "updated.field");

		String author = this.stringMetadata(item, authorField);
		String title = this.stringMetadata(item, titleField);
		String updated = this.stringMetadata(item, updatedField);

		Statement s = new AtomStatement(feedUri, author, title, updated);
		this.populateStatement(context, item, s);
		return s;
	}

	private String stringMetadata(Item item, String field)
	{
		if (field == null)
		{
			return null;
		}

		DCValue[] dcvs = item.getMetadata(field);
		if (dcvs == null || dcvs.length == 0)
		{
			return null;
		}

		StringBuilder md = new StringBuilder();
		for (DCValue dcv : dcvs)
		{
			if (md.length() > 0)
			{
				md.append(", ");
			}
			md.append(dcv.value);
		}
		return md.toString();
	}
}
