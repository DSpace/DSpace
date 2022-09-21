/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.curate.Mutative;
import org.dspace.disseminate.factory.DisseminateServiceFactory;
import org.dspace.disseminate.service.CitationDocumentService;

/**
 * CitationPage
 *
 * This task is used to generate a cover page with citation information for text
 * documents and then to add that cover page to a PDF version of the document
 * replacing the originally uploaded document form the user's perspective.
 *
 * @author Ryan McGowan
 */

@Distributive
@Mutative
public class CitationPage extends AbstractCurationTask {
    /**
     * Class Logger
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(CitationPage.class);

    protected int status = Curator.CURATE_UNSET;
    protected String result = null;
    /**
     * A StringBuilder to handle result string building process.
     */
    protected StringBuilder resBuilder;


    /**
     * The name to give the bundle we add the cited pages to.
     */
    protected static final String DISPLAY_BUNDLE_NAME = "DISPLAY";
    /**
     * The name of the bundle to move source documents into after they have been
     * cited.
     */
    protected static final String PRESERVATION_BUNDLE_NAME = "PRESERVATION";

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance()
            .getResourcePolicyService();

    private Map<String,Bitstream> displayMap = new HashMap<String,Bitstream>();

    /**
     * {@inheritDoc}
     *
     * @see CurationTask#perform(DSpaceObject)
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {

        // Deal with status and result as well as call distribute.
        this.resBuilder = new StringBuilder();
        this.distribute(dso);
        this.result = this.resBuilder.toString();
        this.setResult(this.result);
        this.report(this.result);

        return this.status;
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractCurationTask#performItem(Item)
     */
    @Override
    protected void performItem(Item item) throws SQLException {
        //Determine if the DISPLAY bundle exits. If not, create it.
        List<Bundle> dBundles = itemService.getBundles(item, CitationPage.DISPLAY_BUNDLE_NAME);
        Bundle original = itemService.getBundles(item, "ORIGINAL").get(0);
        Bundle dBundle = null;
        if (dBundles == null || dBundles.isEmpty()) {
            try {
                dBundle = bundleService.create(Curator.curationContext(), item, CitationPage.DISPLAY_BUNDLE_NAME);
                // don't inherit now otherwise they will be copied over the moved bitstreams
                resourcePolicyService.removeAllPolicies(Curator.curationContext(), dBundle);
            } catch (AuthorizeException e) {
                log.error("User not authroized to create bundle on item \"{}\": {}",
                        item::getName, e::getMessage);
                return;
            }
        } else {
            dBundle = dBundles.get(0);
        }

        //Create a map of the bitstreams in the displayBundle. This is used to
        //check if the bundle being cited is already in the display bundle.
        for (Bitstream bs : dBundle.getBitstreams()) {
            displayMap.put(bs.getName(), bs);
        }

        //Determine if the preservation bundle exists and add it if we need to.
        //Also, set up bundles so it contains all ORIGINAL and PRESERVATION
        //bitstreams.
        List<Bundle> pBundles = itemService.getBundles(item, CitationPage.PRESERVATION_BUNDLE_NAME);
        Bundle pBundle = null;
        List<Bundle> bundles = new ArrayList<>();
        if (pBundles != null && !pBundles.isEmpty()) {
            pBundle = pBundles.get(0);
            bundles.addAll(itemService.getBundles(item, "ORIGINAL"));
            bundles.addAll(pBundles);
        } else {
            try {
                pBundle = bundleService.create(Curator.curationContext(), item, CitationPage.PRESERVATION_BUNDLE_NAME);
                // don't inherit now otherwise they will be copied over the moved bitstreams
                resourcePolicyService.removeAllPolicies(Curator.curationContext(), pBundle);
            } catch (AuthorizeException e) {
                log.error("User not authroized to create bundle on item \""
                              + item.getName() + "\": " + e.getMessage());
            }
            bundles = itemService.getBundles(item, "ORIGINAL");
        }

        //Start looping through our bundles. Anything that is citable in these
        //bundles will be cited.
        for (Bundle bundle : bundles) {
            List<Bitstream> bitstreams = bundle.getBitstreams();

            // Loop through each file and generate a cover page for documents
            // that are PDFs.
            for (Bitstream bitstream : bitstreams) {

                //If bitstream is a PDF document then it is citable.
                CitationDocumentService citationDocument = DisseminateServiceFactory.getInstance()
                                                                                    .getCitationDocumentService();

                if (citationDocument.canGenerateCitationVersion(Curator.curationContext(), bitstream)) {
                    this.resBuilder.append(item.getHandle())
                            .append(" - ")
                            .append(bitstream.getName())
                            .append(" is citable.");
                    try {
                        //Create the cited document
                        InputStream citedInputStream =
                            new ByteArrayInputStream(
                                citationDocument.makeCitedDocument(Curator.curationContext(), bitstream).getLeft());
                        //Add the cited document to the approiate bundle
                        this.addCitedPageToItem(citedInputStream, bundle, pBundle,
                                                dBundle, item, bitstream);
                        // now set the policies of the preservation and display bundle
                        clonePolicies(Curator.curationContext(), original, pBundle);
                        clonePolicies(Curator.curationContext(), original, dBundle);
                    } catch (Exception e) {
                        //Could be many things, but nothing that should be
                        //expected.
                        //Print out some detailed information for debugging.
                        e.printStackTrace();
                        StackTraceElement[] stackTrace = e.getStackTrace();
                        StringBuilder stack = new StringBuilder();
                        int numLines = Math.min(stackTrace.length, 12);
                        for (int j = 0; j < numLines; j++) {
                            stack.append("\t")
                                    .append(stackTrace[j].toString())
                                    .append("\n");
                        }
                        if (stackTrace.length > numLines) {
                            stack.append("\t. . .\n");
                        }

                        log.error(e.toString() + " -> \n" + stack.toString());
                        this.resBuilder.append(", but there was an error generating the PDF.\n");
                        this.status = Curator.CURATE_ERROR;
                    }
                } else {
                    //bitstream is not a document
                    this.resBuilder.append(item.getHandle())
                            .append(" - ")
                            .append(bitstream.getName())
                            .append(" is not citable.\n");
                    this.status = Curator.CURATE_SUCCESS;
                }
            }
        }
    }

    /**
     * A helper function for {@link CitationPage#performItem(Item)}. This function takes in the
     * cited document as a File and adds it to DSpace properly.
     *
     * @param citedDoc The inputstream that is the cited document.
     * @param bundle The bundle the cited file is from.
     * @param pBundle The preservation bundle. The original document should be
     * put in here if it is not already.
     * @param dBundle The display bundle. The cited document gets put in here.
     * @param item       The item containing the bundles being used.
     * @param bitstream  The original source bitstream.
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     * @throws IOException        if IO error
     */
    protected void addCitedPageToItem(InputStream citedDoc, Bundle bundle, Bundle pBundle,
                                      Bundle dBundle, Item item,
                                      Bitstream bitstream) throws SQLException, AuthorizeException, IOException {
        //If we are modifying a file that is not in the
        //preservation bundle then we have to move it there.
        Context context = Curator.curationContext();
        if (!bundle.getID().equals(pBundle.getID())) {
            bundleService.addBitstream(context, pBundle, bitstream);
            bundleService.removeBitstream(context, bundle, bitstream);
            List<Bitstream> bitstreams = bundle.getBitstreams();
            if (bitstreams == null || bitstreams.isEmpty()) {
                itemService.removeBundle(context, item, bundle);
            }
        }

        //Create an input stream form the temporary file
        //that is the cited document and create a
        //bitstream from it.
        if (displayMap.containsKey(bitstream.getName())) {
            bundleService.removeBitstream(context, dBundle, displayMap.get(bitstream.getName()));
        }
        Bitstream citedBitstream = bitstreamService.create(context, dBundle, citedDoc);
        citedDoc.close(); //Close up the temporary InputStream

        //Setup a good name for our bitstream and make
        //it the same format as the source document.
        citedBitstream.setName(context, bitstream.getName());
        bitstreamService.setFormat(context, citedBitstream, bitstream.getFormat(Curator.curationContext()));
        citedBitstream.setDescription(context, bitstream.getDescription());
        displayMap.put(bitstream.getName(), citedBitstream);
        clonePolicies(context, bitstream, citedBitstream);
        this.resBuilder.append(" Added ")
                .append(citedBitstream.getName())
                .append(" to the ")
                .append(CitationPage.DISPLAY_BUNDLE_NAME)
                .append(" bundle.\n");

        //Run update to propagate changes to the
        //database.
        itemService.update(context, item);
        this.status = Curator.CURATE_SUCCESS;
    }

    private void clonePolicies(Context context, DSpaceObject source,DSpaceObject target)
            throws SQLException, AuthorizeException {
        resourcePolicyService.removeAllPolicies(context, target);
        for (ResourcePolicy rp: source.getResourcePolicies()) {
            ResourcePolicy newPolicy = resourcePolicyService.clone(context, rp);
            newPolicy.setdSpaceObject(target);
            newPolicy.setAction(rp.getAction());
            resourcePolicyService.update(context, newPolicy);
        }

    }
}
