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
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import java.sql.SQLException;

public class WorkflowManagerDefault implements WorkflowManager
{
	public void retrieveServiceDoc(Context context) throws SwordError
	{
		// do nothing - operation allowed
	}

	public void listCollectionContents(Context context, Collection collection) throws SwordError
	{
		// do nothing - operation allowed
	}

	public void createResource(Context context, Collection collection) throws SwordError
	{
		// do nothing - operation allowed
	}

	public void retrieveContent(Context context, Item item) throws SwordError
	{
		// do nothing - operation allowed
	}

	public void retrieveBitstream(Context context, Bitstream bitstream) throws SwordError, DSpaceSwordException
	{
		// do nothing - operation allowed
	}

	public void replaceResourceContent(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();
		if (item.isArchived() || item.isWithdrawn())
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been archived, and can no longer be modified");
		}
		if (wft.isItemInWorkflow(context, item))
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been injected into the review workflow, and can no longer be modified");
		}
	}

	public void replaceMetadata(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();
		if (item.isArchived() || item.isWithdrawn())
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been archived, and can no longer be modified");
		}
		if (wft.isItemInWorkflow(context, item))
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been injected into the review workflow, and can no longer be modified");
		}
	}

	public void replaceMetadataAndMediaResource(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		this.replaceResourceContent(context, item);
		this.replaceMetadata(context, item);
	}

	public void deleteMediaResource(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();
		if (item.isArchived() || item.isWithdrawn())
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been archived, and can no longer be modified");
		}
		if (wft.isItemInWorkflow(context, item))
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been injected into the review workflow, and can no longer be modified");
		}
	}

	public void deleteBitstream(Context context, Bitstream bitstream) throws SwordError, DSpaceSwordException
	{
		// this is equivalent to asking whether the media resource in the item can be deleted
		try
		{
			for (Bundle bundle : bitstream.getBundles())
			{
                // is the bitstream in the ORIGINAL bundle?  If not, it can't be worked on
                if (!"ORIGINAL".equals(bundle.getName()))
                {
                    throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The file is not in a bundle which can be modified");
                }

				for (Item item : bundle.getItems())
				{
					this.deleteMediaResource(context, item);
				}
			}
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}
	}

    public void replaceBitstream(Context context, Bitstream bitstream)
            throws SwordError, DSpaceSwordException
    {
        // this is equivalent to asking whether the media resource in the item can be deleted
		try
		{
			for (Bundle bundle : bitstream.getBundles())
			{
                // is the bitstream in the ORIGINAL bundle?  If not, it can't be worked on
                if (!"ORIGINAL".equals(bundle.getName()))
                {
                    throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The file is not in a bundle which can be modified");
                }
                
				for (Item item : bundle.getItems())
				{
					this.replaceResourceContent(context, item);
				}
			}
		}
		catch (SQLException e)
		{
			throw new DSpaceSwordException(e);
		}
    }

    public void addResourceContent(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();
		if (item.isArchived() || item.isWithdrawn())
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been archived, and can no longer be modified");
		}
		if (wft.isItemInWorkflow(context, item))
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been injected into the review workflow, and can no longer be modified");
		}
	}

	public void addMetadata(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();
		if (item.isArchived() || item.isWithdrawn())
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been archived, and can no longer be modified");
		}
		if (wft.isItemInWorkflow(context, item))
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been injected into the review workflow, and can no longer be modified");
		}
	}

	public void deleteItem(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();
		if (item.isArchived() || item.isWithdrawn())
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been archived, and can no longer be modified");
		}
		if (wft.isItemInWorkflow(context, item))
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been injected into the review workflow, and can no longer be modified");
		}
	}

	public void retrieveStatement(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		// do nothing - operation allowed
	}

	public void modifyState(Context context, Item item) throws SwordError, DSpaceSwordException
	{
		WorkflowTools wft = new WorkflowTools();
		if (item.isArchived() || item.isWithdrawn())
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been archived, and can no longer be modified");
		}
		if (wft.isItemInWorkflow(context, item))
		{
			throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED, "The item has already been injected into the review workflow, and can no longer be modified");
		}
	}

	public void resolveState(Context context, Deposit deposit, DepositResult result, VerboseDescription verboseDescription)
            throws DSpaceSwordException
    {
		this.resolveState(context, deposit, result, verboseDescription, true);
	}
	
	public void resolveState(Context context, Deposit deposit, DepositResult result, VerboseDescription verboseDescription, boolean containerOperation)
            throws DSpaceSwordException
    {
		// the containerOperation flag tells us whether this method was called by an operation which happened on the
		// container.  This workflow implementation only changes workflow states on contaner operations, not media
		// resource operations, so we just bounce this right back.
		if (!containerOperation)
		{
			return;
		}

		// if we get to here this is a container operation, and we can decide how best to process
        Item item = result.getItem();

        // find out where the item is in the workflow
        WorkflowTools wft = new WorkflowTools();
        boolean inwf = wft.isItemInWorkflow(context, item);
        boolean inws = wft.isItemInWorkspace(context, item);

        // or find out if the item is in the archive
        boolean inarch = item.isArchived() || item.isWithdrawn();

        // in progress      inws    inwf    inarch      action      description
        // 0                0       0       1           ERROR       the deposit finished, and the item is in the archive; this should never be allowed to arise
        // 0                0       1       0           NOTHING     the deposit finished, and the item is in the workflow.  Carry on as normal
        // 0                1       0       0           START WF    the deposit is finished, and the item is in the workflow, so we start it
        // 1                0       0       1           ERROR       the deposit is not finished, and the item is in the archive; this should never be allowed to arise
        // 1                0       1       0           STOP WF     the deposit is not finished, and it is in the workflow.  Pull it out into the workspace
        // 1                1       0       0           NOTHING     the deposit is not finished, and is in the workspace; leave it there

        if (!deposit.isInProgress() && inarch)
        {
            verboseDescription.append("The deposit is finished, but the item is already in the archive");
            throw new DSpaceSwordException("Invalid workflow state");
        }

        if (!deposit.isInProgress() && inws)
        {
            verboseDescription.append("The deposit is finished: moving it from the workspace to the workflow");
            wft.startWorkflow(context, item);
        }

        if (deposit.isInProgress() && inarch)
        {
            verboseDescription.append("The deposit is not finished, but the item is already in the archive");
            throw new DSpaceSwordException("Invalid workflow state");
        }

        if (deposit.isInProgress() && inwf)
        {
            verboseDescription.append("The deposit is in progress, but is in the workflow; returning to the workspace");
            wft.stopWorkflow(context, item);
        }
    }
}
