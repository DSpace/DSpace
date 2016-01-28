/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.disseminate.CitationDocument;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
    private static Logger log = Logger.getLogger(CitationPage.class);

    private int status = Curator.CURATE_UNSET;
    private String result = null;
    /**
     * A StringBuilder to handle result string building process.
     */
    private StringBuilder resBuilder;




    /**
     * The name to give the bundle we add the cited pages to.
     */
    private static final String DISPLAY_BUNDLE_NAME = "DISPLAY";
    /**
     * The name of the bundle to move source documents into after they have been
     * cited.
     */
    private static final String PRESERVATION_BUNDLE_NAME = "PRESERVATION";

    /**
     * {@inheritDoc}
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
     * @see AbstractCurationTask#performItem(Item)
     */
    @Override
    protected void performItem(Item item) throws SQLException {
        //Determine if the DISPLAY bundle exits. If not, create it.
        Bundle[] dBundles = item.getBundles(CitationPage.DISPLAY_BUNDLE_NAME);
        Bundle dBundle = null;
        if (dBundles == null || dBundles.length == 0) {
            try {
                dBundle = item.createBundle(CitationPage.DISPLAY_BUNDLE_NAME);
            } catch (AuthorizeException e) {
                log.error("User not authroized to create bundle on item \""
                        + item.getName() + "\": " + e.getMessage());
            }
        } else {
            dBundle = dBundles[0];
        }

        //Create a map of the bitstreams in the displayBundle. This is used to
        //check if the bundle being cited is already in the display bundle.
        Map<String,Bitstream> displayMap = new HashMap<String,Bitstream>();
        for (Bitstream bs : dBundle.getBitstreams()) {
            displayMap.put(bs.getName(), bs);
        }

        //Determine if the preservation bundle exists and add it if we need to.
        //Also, set up bundles so it contains all ORIGINAL and PRESERVATION
        //bitstreams.
        Bundle[] pBundles = item.getBundles(CitationPage.PRESERVATION_BUNDLE_NAME);
        Bundle pBundle = null;
        Bundle[] bundles = null;
        if (pBundles != null && pBundles.length > 0) {
            pBundle = pBundles[0];
            bundles = (Bundle[]) ArrayUtils.addAll(item.getBundles("ORIGINAL"), pBundles);
        } else {
            try {
                pBundle = item.createBundle(CitationPage.PRESERVATION_BUNDLE_NAME);
            } catch (AuthorizeException e) {
                log.error("User not authroized to create bundle on item \""
                        + item.getName() + "\": " + e.getMessage());
            }
            bundles = item.getBundles("ORIGINAL");
        }

        //Start looping through our bundles. Anything that is citable in these
        //bundles will be cited.
        for (Bundle bundle : bundles) {
            Bitstream[] bitstreams = bundle.getBitstreams();

            // Loop through each file and generate a cover page for documents
            // that are PDFs.
            for (Bitstream bitstream : bitstreams) {
                BitstreamFormat format = bitstream.getFormat();

                //If bitstream is a PDF document then it is citable.
                CitationDocument citationDocument = new CitationDocument();

                if(citationDocument.canGenerateCitationVersion(bitstream)) {
                    this.resBuilder.append(item.getHandle() + " - "
                            + bitstream.getName() + " is citable.");
                    try {
                        //Create the cited document
                        File citedDocument = citationDocument.makeCitedDocument(bitstream);
                        //Add the cited document to the approiate bundle
                        this.addCitedPageToItem(citedDocument, bundle, pBundle,
                                dBundle, displayMap, item, bitstream);
                    } catch (Exception e) {
                        //Could be many things, but nothing that should be
                        //expected.
                        //Print out some detailed information for debugging.
                        e.printStackTrace();
                        StackTraceElement[] stackTrace = e.getStackTrace();
                        StringBuilder stack = new StringBuilder();
                        int numLines = Math.min(stackTrace.length, 12);
                        for (int j = 0; j < numLines; j++) {
                            stack.append("\t" + stackTrace[j].toString() + "\n");
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
                    this.resBuilder.append(item.getHandle() + " - "
                            + bitstream.getName() + " is not citable.\n");
                    this.status = Curator.CURATE_SUCCESS;
                }
            }
        }
    }

    /**
     * A helper function for {@link CitationPage#performItem(Item)}. This function takes in the
     * cited document as a File and adds it to DSpace properly.
     *
     * @param citedTemp The temporary File that is the cited document.
     * @param bundle The bundle the cited file is from.
     * @param pBundle The preservation bundle. The original document should be
     * put in here if it is not already.
     * @param dBundle The display bundle. The cited document gets put in here.
     * @param displayMap The map of bitstream names to bitstreams in the display
     * bundle.
     * @param item The item containing the bundles being used.
     * @param bitstream The original source bitstream.
     * @throws SQLException
     * @throws AuthorizeException
     * @throws IOException
     */
    private void addCitedPageToItem(File citedTemp, Bundle bundle, Bundle pBundle,
                                    Bundle dBundle, Map<String,Bitstream> displayMap, Item item,
                                    Bitstream bitstream) throws SQLException, AuthorizeException, IOException {
        //If we are modifying a file that is not in the
        //preservation bundle then we have to move it there.
        if (bundle.getID() != pBundle.getID()) {
            pBundle.addBitstream(bitstream);
            bundle.removeBitstream(bitstream);
            Bitstream[] originalBits = bundle.getBitstreams();
            if (originalBits == null || originalBits.length == 0) {
                item.removeBundle(bundle);
            }
        }

        //Create an input stream form the temporary file
        //that is the cited document and create a
        //bitstream from it.
        InputStream inp = new FileInputStream(citedTemp);
        if (displayMap.containsKey(bitstream.getName())) {
            dBundle.removeBitstream(displayMap.get(bitstream.getName()));
        }
        Bitstream citedBitstream = dBundle.createBitstream(inp);
        inp.close(); //Close up the temporary InputStream

        //Setup a good name for our bitstream and make
        //it the same format as the source document.
        citedBitstream.setName(bitstream.getName());
        citedBitstream.setFormat(bitstream.getFormat());
        citedBitstream.setDescription(bitstream.getDescription());

        this.resBuilder.append(" Added "
                + citedBitstream.getName()
                + " to the " + CitationPage.DISPLAY_BUNDLE_NAME + " bundle.\n");

        //Run update to propagate changes to the
        //database.
        item.update();
        this.status = Curator.CURATE_SUCCESS;
    }
}
