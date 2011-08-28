/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.swordapp.server.OriginalDeposit;
import org.swordapp.server.ResourcePart;
import org.swordapp.server.Statement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GenericStatementDisseminator implements SwordStatementDisseminator
{
	protected SwordUrlManager urlManager;

	protected void populateStatement(Context context, Item item, Statement statement)
			throws DSpaceSwordException
	{
		this.urlManager = new SwordUrlManager(new SwordConfigurationDSpace(), context);
		List<OriginalDeposit> originalDeposits = this.getOriginalDeposits(context, item);
		Map<String, String> states = this.getStates(context, item);
		List<ResourcePart> resources = this.getResourceParts(context, item);
        Date lastModified = this.getLastModified(context, item);

        statement.setLastModified(lastModified);
		statement.setOriginalDeposits(originalDeposits);
		statement.setStates(states);
		statement.setResources(resources);
	}

	protected List<OriginalDeposit> getOriginalDeposits(Context context, Item item)
			throws DSpaceSwordException
	{
		try
		{
			// an original deposit is everything in the SWORD bundle
			List<OriginalDeposit> originalDeposits = new ArrayList<OriginalDeposit>();
            String swordBundle = ConfigurationManager.getProperty("swordv2-server", "bundle.name");
            if (swordBundle == null)
            {
                swordBundle = "SWORD";
            }
			Bundle[] swords = item.getBundles(swordBundle);
			for (Bundle sword : swords)
			{
				for (Bitstream bitstream : sword.getBitstreams())
				{
					// note that original deposits don't have actionable urls
					OriginalDeposit deposit = new OriginalDeposit(this.urlManager.getBitstreamUrl(bitstream));
					deposit.setMediaType(bitstream.getFormat().getMIMEType());
					originalDeposits.add(deposit);
				}
			}
			return originalDeposits;
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}
	}

	protected Map<String, String> getStates(Context context, Item item)
			throws DSpaceSwordException
	{
		SwordConfigurationDSpace config = new SwordConfigurationDSpace();
		WorkflowTools wft = new WorkflowTools();
		Map<String, String> states = new HashMap<String, String>();
		if (item.isWithdrawn())
		{
			String uri = config.getStateUri("withdrawn");
			String desc = config.getStateDescription("withdrawn");
			states.put(uri, desc);
		}
		else if (item.isArchived())
		{
			String uri = config.getStateUri("archive");
			String desc = config.getStateDescription("archive");
			states.put(uri, desc);
		}
		else if (wft.isItemInWorkflow(context, item))
		{
			String uri = config.getStateUri("workflow");
			String desc = config.getStateDescription("workflow");
			states.put(uri, desc);
		}
		else if (wft.isItemInWorkspace(context, item))
		{
			String uri = config.getStateUri("workspace");
			String desc = config.getStateDescription("workspace");
			states.put(uri, desc);
		}
		return states;
	}

	protected List<ResourcePart> getResourceParts(Context context, Item item)
			throws DSpaceSwordException
	{
		try
		{
			// the list of resource parts is everything in the ORIGINAL bundle
			List<ResourcePart> resources = new ArrayList<ResourcePart>();
			Bundle[] originals = item.getBundles("ORIGINAL");
			for (Bundle original : originals)
			{
				for (Bitstream bitstream : original.getBitstreams())
				{
					// note that individual bitstreams have actionable urls
					ResourcePart part = new ResourcePart(this.urlManager.getActionableBitstreamUrl(bitstream));
					part.setMediaType(bitstream.getFormat().getMIMEType());
					resources.add(part);

				}
			}
			return resources;
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}
	}

    protected Date getLastModified(Context context, Item item)
    {
        return item.getLastModified();
    }
}
