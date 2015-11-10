/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordError;

public interface WorkflowManager
{
    public void retrieveServiceDoc(Context context)
            throws SwordError, DSpaceSwordException;

    public void listCollectionContents(Context context, Collection collection)
            throws SwordError, DSpaceSwordException;

    public void createResource(Context context, Collection collection)
            throws SwordError, DSpaceSwordException;

    public void retrieveContent(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void retrieveBitstream(Context context, Bitstream bitstream)
            throws SwordError, DSpaceSwordException;

    public void replaceResourceContent(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void replaceBitstream(Context context, Bitstream bitstream)
            throws SwordError, DSpaceSwordException;

    public void replaceMetadata(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void replaceMetadataAndMediaResource(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void deleteMediaResource(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void deleteBitstream(Context context, Bitstream bitstream)
            throws SwordError, DSpaceSwordException;

    public void addResourceContent(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void addMetadata(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void deleteItem(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void retrieveStatement(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void modifyState(Context context, Item item)
            throws SwordError, DSpaceSwordException;

    public void resolveState(Context context, Deposit deposit,
            DepositResult result, VerboseDescription verboseDescription)
            throws DSpaceSwordException;

    public void resolveState(Context context, Deposit deposit,
            DepositResult result, VerboseDescription verboseDescription,
            boolean containerOperation)
            throws DSpaceSwordException;

}
