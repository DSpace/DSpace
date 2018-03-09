/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;
import org.swordapp.server.UriRegistry;

import java.sql.SQLException;
import java.util.List;

/**
 * This implementation of WorkflowManager is unrestricted and allows UPDATE and DELETE operations
 * on items in any state (in workflow, in archive, or withdrawn).
 */
public class WorkflowManagerUnrestricted implements WorkflowManager
{
    public void retrieveServiceDoc(Context context) throws SwordError
    {
        // do nothing - operation allowed
    }

    public void listCollectionContents(Context context, Collection collection)
            throws SwordError
    {
        // do nothing - operation allowed
    }

    public void createResource(Context context, Collection collection)
            throws SwordError
    {
        // do nothing - operation allowed
    }

    public void retrieveContent(Context context, Item item) throws SwordError
    {
        // do nothing - operation allowed
    }

    public void retrieveBitstream(Context context, Bitstream bitstream)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void replaceResourceContent(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void replaceMetadata(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void replaceMetadataAndMediaResource(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void deleteMediaResource(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void deleteBitstream(Context context, Bitstream bitstream)
            throws SwordError, DSpaceSwordException
    {
        // this is equivalent to asking whether the media resource in the item can be deleted
        try
        {
            List<Bundle> bundles = bitstream.getBundles();
            for (Bundle bundle : bundles)
            {
                // is the bitstream in the ORIGINAL bundle?  If not, it can't be worked on
                if (!Constants.CONTENT_BUNDLE_NAME
                        .equals(bundle.getName()))
                {
                    throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED,
                            "The file is not in a bundle which can be modified");
                }

                List<Item> items = bundle.getItems();
                for (Item item : items)
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
            List<Bundle> bundles = bitstream.getBundles();
            for (Bundle bundle : bundles)
            {
                // is the bitstream in the ORIGINAL bundle?  If not, it can't be worked on
                if (!Constants.CONTENT_BUNDLE_NAME
                        .equals(bundle.getName()))
                {
                    throw new SwordError(UriRegistry.ERROR_METHOD_NOT_ALLOWED,
                            "The file is not in a bundle which can be modified");
                }

                List<Item> items = bundle.getItems();
                for (Item item : items)
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

    public void addResourceContent(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void addMetadata(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void deleteItem(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void retrieveStatement(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void modifyState(Context context, Item item)
            throws SwordError, DSpaceSwordException
    {
        // do nothing - operation allowed
    }

    public void resolveState(Context context, Deposit deposit,
            DepositResult result, VerboseDescription verboseDescription)
            throws DSpaceSwordException
    {
        this.resolveState(context, deposit, result, verboseDescription, true);
    }

    public void resolveState(Context context, Deposit deposit,
            DepositResult result, VerboseDescription verboseDescription,
            boolean containerOperation)
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
        // 0                0       0       1           NOTHING     the deposit finished, and the item is in the archive;
        // 0                0       1       0           NOTHING     the deposit finished, and the item is in the workflow.  Carry on as normal
        // 0                1       0       0           START WF    the deposit is finished, and the item is in the workflow, so we start it
        // 1                0       0       1           NOTHING     the deposit is not finished, and the item is in the archive;
        // 1                0       1       0           STOP WF     the deposit is not finished, and it is in the workflow.  Pull it out into the workspace
        // 1                1       0       0           NOTHING     the deposit is not finished, and is in the workspace; leave it there

        if (!deposit.isInProgress() && inarch)
        {
            verboseDescription
                    .append("The deposit is finished, and the item is already in the archive");
            // throw new DSpaceSwordException("Invalid workflow state");
        }

        if (!deposit.isInProgress() && inws)
        {
            verboseDescription
                    .append("The deposit is finished: moving it from the workspace to the workflow");
            wft.startWorkflow(context, item);
        }

        if (deposit.isInProgress() && inarch)
        {
            verboseDescription
                    .append("The deposit is not finished, and the item is already in the archive");
            // throw new DSpaceSwordException("Invalid workflow state");
        }

        if (deposit.isInProgress() && inwf)
        {
            verboseDescription
                    .append("The deposit is in progress, but is in the workflow; returning to the workspace");
            wft.stopWorkflow(context, item);
        }
    }
}
