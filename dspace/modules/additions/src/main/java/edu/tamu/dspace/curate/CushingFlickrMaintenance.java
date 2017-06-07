package edu.tamu.dspace.curate;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

public class CushingFlickrMaintenance extends AbstractCurationTask {

    private int result = Curator.CURATE_SUCCESS;
    private StringBuilder sb = new StringBuilder();

    private final String COLLECTION_HANDLE = "1969.1/97043";
    private final String COMMUNITY_HANDLE = "1969.1/7";
    private final String BLANK_CHECKSUM = "2fd34771a39e7cbc08cc7c1195b692a7";
    private final String FLIRK_API_URL = "https://api.flickr.com/services/rest/?api_key=cdf3f8fe0b47dbdac22758fde335dacf&method=flickr.photos.getSizes&photo_id=";

    private SAXBuilder builder = new SAXBuilder();

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        sb = new StringBuilder();
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        if (dso.getType() == Constants.SITE) {
            sb.append("Cannot perform this task at site level.");
            this.setResult(sb.toString());
            return Curator.CURATE_FAIL;
        } else if (dso.getType() == Constants.COMMUNITY && !dso.getHandle().equals(COMMUNITY_HANDLE)) {
            sb.append("Cannot perform this task on a non-Cushing community (" + COMMUNITY_HANDLE + ").");
            this.setResult(sb.toString());
            return Curator.CURATE_FAIL;
        } else if ((dso.getType() == Constants.COLLECTION && !dso.getHandle().equals(COLLECTION_HANDLE)) && false) {
            sb.append("Cannot perform this task on a non-Cushing collection (" + COLLECTION_HANDLE + ").");
            this.setResult(sb.toString());
            return Curator.CURATE_FAIL;
        } else {
            distribute(dso);
            this.setResult(sb.toString());
            return result;
        }
    }

    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        String flikrId = "";
        String flikrSizesUrl = "";

        try {
            int count = 0;

            for (Bundle bundle : item.getBundles("ORIGINAL")) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                    // If we got a hit, remove the offending bitstream, download a replacement and delete the thumbnails
                    if (bitstream.getChecksum().equals(BLANK_CHECKSUM)) {

                        // Extract image id from the bitstream name
                        sb.append("Bitstream name: " + bitstream.getName());

                        flikrId = bitstream.getName().split("_")[0];
                        flikrSizesUrl = FLIRK_API_URL + flikrId;

                        sb.append("flickrId:" + flikrId);

                        // Contact Flikr API
                        Document availableSizes = builder.build(flikrSizesUrl);
                        Element root = availableSizes.getRootElement();

                        // Verify that the response is error-free
                        if (!root.getAttributeValue("stat").equals("ok")) {
                            throw new JDOMException("Status was in error on id: " + flikrId);
                        }

                        // Check to see if a large image exists (which it probably won't or this item wouldn't be in error in the first place)
                        XPath xpath = XPath.newInstance("//size[@label='Large']/@source");
                        Attribute sourceUrl = (Attribute) xpath.selectSingleNode(root);
                        URL imageUrl;
                        if (sourceUrl == null) {
                            xpath = XPath.newInstance("//size[@label='Original']/@source");
                            sourceUrl = (Attribute) xpath.selectSingleNode(root);
                        }
                        imageUrl = new URL(sourceUrl.getValue());

                        // Create a bitstream from the image source
                        Bitstream bs = bundle.createBitstream(imageUrl.openStream());
                        bs.setDescription("Updated Flikr image");
                        bs.setName(bitstream.getName());

                        bundle.removeBitstream(bitstream);

                        for (Bundle tbundle : item.getBundles("THUMBNAIL")) {
                            for (Bitstream tbitstream : tbundle.getBitstreams()) {
                                tbundle.removeBitstream(tbitstream);
                            }
                            item.removeBundle(tbundle);
                        }

                    } // end of if the bitstream has the blank image checksum
                } // end of for each bitstream in the bundle's bitstreams
            } // end of for each bundle that is the "ORIGINAL" bundle

            item.update();
            sb.append(item.getHandle() + ": " + count + " images deleted. \n");
        } catch (AuthorizeException e) {
            result = Curator.CURATE_ERROR;
            sb.append("Authorization failure on item: " + item.getHandle() + "\nAborting...");
        } catch (JDOMException e) {
            result = Curator.CURATE_ERROR;
            sb.append("JDOM read failure on item: " + item.getHandle() + "\n. URL(" + flikrSizesUrl + ").\n" + "Error message: '" + e.getMessage() + "'.\nAborting...");
        }
    }

}
