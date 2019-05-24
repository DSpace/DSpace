package org.dspace.xmlworkflow.cristin;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.harvest.HarvestedItem;
import org.dspace.harvest.cristin.IngestionWorkflow;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import javax.mail.MessagingException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * <p>Implementation of the Harvester's IngestionWorkflow, providing hooks
 * for the OAI-PMH harvesting features.</p>
 *
 * <p>Provides implementations for preUpdate, postUpdate and postCreate.</p>
 *
 * <p>If a new item comes in the postCreate method will just start the workflow.</p>
 *
 * <p>If an updated item comes in, the preUpdate method will determine (based on
 * the Cristin workflow requirements) whether a clone of the item being updated is
 * necessary, and if so will handle that process.</p>
 *
 * <p><strong>Configuration</strong></p>
 *
 * <p>Configure this in the dspace.cfg file as a named plugin of the IngestionWorkflow
 * interface:</p>
 *
 * <pre>
 * plugin.named.org.dspace.harvest.cristin.IngestionWorkflow = \org.dspace.xmlworkflow.cristin.CristinIngestionWorkflow = cristin
 * </pre>
 */
public class CristinIngestionWorkflow implements IngestionWorkflow {
    private final ConfigurationService configurationService;
    private final ItemService itemService;
    private boolean allowUpdateBitstreams = true;
    private List<String> originalUnits;

    /* Namespaces */
    public static final Namespace ATOM_NS =
            Namespace.getNamespace("atom", "http://www.w3.org/2005/Atom");
    private static final Namespace ORE_ATOM =
            Namespace.getNamespace("oreatom", "http://www.openarchives.org/ore/atom/");
    private static final Namespace ORE_NS =
            Namespace.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
    private static final Namespace RDF_NS =
            Namespace.getNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    private static final Namespace DCTERMS_NS =
            Namespace.getNamespace("dcterms", "http://purl.org/dc/terms/");
    private static final Namespace DS_NS =
            Namespace.getNamespace("ds", "http://www.dspace.org/objectModel/");

    public CristinIngestionWorkflow() {
        configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        itemService = ContentServiceFactory.getInstance().getItemService();
    }


    /**
     * Carry out any activities required prior to updating an item, and return the item
     * which will ultimately be updated.
     * <p>
     * This will do the following things:
     * <p>
     * - if the item is archived and the incoming documents are different, it will create
     * a new version of the item in the workspace
     * - otherwise it will return the passed item unchanged
     *
     * @param context
     * @param item
     * @param targetCollection
     * @param harvestedItem
     * @param descMD
     * @param oreREM
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public Item preUpdate(Context context, Item item, Collection targetCollection, HarvestedItem harvestedItem, List<Element> descMD, Element oreREM)
            throws SQLException, IOException, AuthorizeException {
        this.originalUnits = this.getUnitCodes(item);

        boolean inarch = item.isArchived() || item.isWithdrawn();
        boolean docsChanged = false;
        if (inarch) {
            // if the item is in the archive, check to see if we need to update
            // the bitstreams
            docsChanged = this.haveDocsChanged(item, oreREM);
            if (docsChanged) {
                // if we need to update the bitstreams, then create a new
                // workspace item and return it
                Item newItem = this.newItem(context, targetCollection);

                HarvestedItemService harvestedItemService = HarvestServiceFactory.getInstance().getHarvestedItemService();
                harvestedItem.setItem(newItem);
                harvestedItemService.update(context, harvestedItem);
                this.allowUpdateBitstreams = true; // be explicit
                return newItem;
            } else {
                // in this case we don't want to do anything on the bitstreams,
                // so mark the bitstream update rule false
                this.allowUpdateBitstreams = false;
                return item;
            }
        } else {
            // the item is in the workspace or the workflow, in which case we
            // update it in-place
            this.allowUpdateBitstreams = true; // be explicit
            return item;
        }
    }

    /**
     * After an item has been updated, this method will be run to finish up.
     * <p>
     * This will carry out the following actions:
     * <p>
     * - it will determine if the item's unit codes have changed, and alert
     * the administrator if they have
     * - if the item is in the workspace, it will trigger the workflow
     * - if the item is in the workflow, it will restart the workflow from the start
     *
     * @param context
     * @param item
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public void postUpdate(Context context, Item item)
            throws SQLException, IOException, AuthorizeException {
        List<String> newUnits = this.getUnitCodes(item);
        this.actOnUnitCodes(item, this.originalUnits, newUnits);

        if (CristinWorkflowManager.isItemInWorkspace(context, item)) {
            CristinWorkflowManager.startWorkflow(context, item);
        } else if (CristinWorkflowManager.isItemInWorkflow(context, item)) {
            CristinWorkflowManager.restartWorkflow(context, item);
        }
        // if the item is in the archive, leave it where it is
    }

    /**
     * After creating an item fresh, this method will be run by the Harvester
     * <p>
     * This will trigger the workflow on the item in the workspace
     *
     * @param context
     * @param wsi
     * @param handle
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public Item postCreate(Context context, WorkspaceItem wsi, String handle)
            throws SQLException, IOException, AuthorizeException {
        // kick off the workflow
        CristinWorkflowManager.startWorkflow(context, wsi);

        // return the item, as that's what's required
        return wsi.getItem();
    }

    /**
     * The Harvester will call this method to determine whether it should update the
     * bitstreams on an item during update
     *
     * @param context
     * @param item
     * @param hi
     * @return
     */
    public boolean updateBitstreams(Context context, Item item, HarvestedItem hi) {
        // we already know whether we allow the bitstreams to be updated
        return this.allowUpdateBitstreams;
    }

    private Item newItem(Context context, Collection targetCollection)
            throws SQLException, AuthorizeException {
        WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        WorkspaceItem wi = workspaceItemService.create(context, targetCollection, false);
        return wi.getItem();
    }

    private void actOnUnitCodes(Item item, List<String> before, List<String> after)
            throws IOException {
        if (this.changedUnitCodes(before, after)) {
            this.sendEmail(item, before, after);
        }
    }

    private void sendEmail(Item item, List<String> before, List<String> after) throws IOException {
        Email email = Email.getEmail(I18nUtil.getEmailFilename(I18nUtil.getDefaultLocale(), "unitcodes"));
        String to = configurationService.getProperty("mail.admin");
        email.addRecipient(to);

        // add the arguments, which are:
        // {0} the item that has changed
        // {1} the old unit codes
        // {2} the new unit codes

        // {0} - the item that has changed
        String itemArg = "";
        List<MetadataValue> titles = itemService.getMetadataByMetadataString(item, "dc.title");
        if (titles.size() > 0) {
            itemArg += titles.get(0).getValue();
        } else {
            itemArg += "Untitled";
        }
        itemArg += " (" + item.getHandle() + ")";
        email.addArgument(itemArg);

        // {1} the old unit codes
        StringBuilder beforeArg = new StringBuilder();
        for (String code : before) {
            beforeArg.append(code + "\n");
        }
        email.addArgument(beforeArg.toString());

        // {2} the new unit codes
        StringBuilder afterArg = new StringBuilder();
        for (String code : after) {
            afterArg.append(code + "\n");
        }
        email.addArgument(afterArg.toString());

        // now send it
        try {
            email.send();
        } catch (MessagingException e) {
            throw new IOException(e);
        }
    }

    private boolean changedUnitCodes(List<String> before, List<String> after) {
        if (before.size() != after.size()) {
            return true;
        }

        for (String code : after) {
            if (!before.contains(code)) {
                return true;
            }
        }

        return false;
    }

    private List<String> getUnitCodes(Item item) {
        String field = configurationService.getProperty("cristin", "unitcode.field");
        List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, field);
        List<String> units = new ArrayList<String>();
        for (MetadataValue dcv : dcvs) {
            if (!units.contains(dcv.getValue())) {
                units.add(dcv.getValue());
            }
        }
        return units;
    }

    private boolean haveDocsChanged(Item item, Element oreREM)
            throws IOException, SQLException {
        /*
        A set of documents can be said to have changed if:
            a/	There are more files than before in the incoming list
            b/	There are fewer files than before in the incoming list
            c/	The set of files contain different checksums than before
            d/	The set of files are in a different order than before
	*/
        // NOTE: we do not get file checksums from the ORE interface, so it is currently
        // impossible to determine if the file list has changed

        Document doc = new Document();
        doc.addContent(oreREM.detach());

        // get a list of the aggregated resources in the ORIGINAL bundle
        CristinFileManager fm = new CristinFileManager();
        String bundleName = "ORIGINAL";
        List<CristinBitstream> cristinBitstreams = fm.listBitstreamsInBundle(doc, bundleName);
        List<Bitstream> existingBitstreams = fm.getExistingBitstreams(item, bundleName);

        // a/	There are more files than before in the incoming list
        // remember that there will be a metadata bitstream which we don't care
        // about couting
        if (cristinBitstreams.size() > existingBitstreams.size()) {
            return true;
        }

        // b/	There are fewer files than before in the incoming list
        // remember that there will be a metadata bitstream which we don't care
        // about couting
        if (cristinBitstreams.size() < existingBitstreams.size()) {
            return true;
        }

        // c/	The set of files contain different checksums than before
        // FIXME: we can't do this yet
        if (this.checksumsChanged(cristinBitstreams, existingBitstreams)) {
            return true;
        }

        // d/	The set of files are in a different order than before
        if (this.fileOrderChanged(cristinBitstreams, existingBitstreams, item, bundleName)) {
            return true;
        }

        return false;
    }

    private boolean fileOrderChanged(List<CristinBitstream> cristinBitstreams, List<Bitstream> existingBitstreams, Item item, String bundleName)
            throws SQLException {
        TreeMap<Integer, String> rawIncomingSeq = new TreeMap<Integer, String>();
        TreeMap<Integer, String> rawExistingSeq = new TreeMap<Integer, String>();

        CristinFileManager fm = new CristinFileManager();

        // calculate the match map for the incoming items
        // we need to remember that in the incoming item, the metadata bitstream
        // is in the ORIGINAL bundle, but not in the existing item, so when determining
        // the order of the incoming files, we need to close the gap left by the
        // metadata bitstream
        for (CristinBitstream ib : cristinBitstreams) {
            // FIXME: at the moment we do the matching on filename, but what we
            // really want is to do this by MD5.  This should be easy to sort out
            // by simply switching ib.getName() for ib.getMd5()
            rawIncomingSeq.put(ib.getOrder(), ib.getName());
        }

        // normalise the map order
        TreeMap<Integer, String> incomingSeq = this.normaliseSeq(rawIncomingSeq);

        // calculate the match map for the existing bitstreams
        for (Bitstream bitstream : existingBitstreams) {
            // FIXME: at the moment we do the matching on filename, but what we
            // really want is to do this by MD5.  This should be easy to sort out
            // by simply switching bitstream.getName() for bitstream.getChecksum()
            int order = fm.getBitstreamOrder(bitstream, item, bundleName);
            rawExistingSeq.put(order, bitstream.getName());
        }
        TreeMap<Integer, String> existingSeq = this.normaliseSeq(rawExistingSeq);

        // now do the comparison
        int highest = incomingSeq.lastKey() > existingSeq.lastKey() ? incomingSeq.lastKey() : existingSeq.lastKey();
        for (int i = 1; i <= highest; i++) {
            String in = incomingSeq.get(i);
            String ex = existingSeq.get(i);
            if (in == null || ex == null) {
                // if one is populated and the other not, then the file order has necessarily changed
                return true;
            }
            if (!in.equals(ex)) {
                // if two that are populated are not the same, the file order is different
                return true;
            }
        }

        // if we don't trip the checks, then the file order is the same
        return false;
    }

    private TreeMap<Integer, String> normaliseSeq(TreeMap<Integer, String> seqMap) {
        // go through the keys in order, looking for the gap left by the metadata
        // bitstream
        TreeMap<Integer, String> normSeq = new TreeMap<Integer, String>();
        int seq = 1;
        for (Integer key : seqMap.keySet()) {
            normSeq.put(seq, seqMap.get(key));
            seq++;
        }
        return normSeq;
    }

    private boolean checksumsChanged(List<CristinBitstream> cristinBitstreams, List<Bitstream> existingBitstreams) {
        // FIXME: we don't have enough information to do this yet
        return false;

        /*
        List<String> incomingChecks = new ArrayList<String>();
        List<String> existingChecks = new ArrayList<String>();

        for (CristinBitstream ib : cristinBitstreams)
        {
            incomingChecks.add(ib.getMd5());
        }

        for (Bitstream bs : existingBitstreams)
        {
            existingChecks.add(bs.getChecksum());
        }

        for (String check : incomingChecks)
        {
            if (!existingChecks.contains(check))
            {
                return true;
            }
        }
        return false;
        */
    }
}
