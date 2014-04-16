/*
 */
package org.dspace.curate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.doi.DryadDOIRegistrationHelper;
import org.jdom.input.SAXBuilder;
import org.apache.xerces.parsers.SAXParser;
import org.dspace.content.DCValue;
import org.dspace.core.ConfigurationManager;
import org.jdom.Document;
import org.jdom.JDOMException;


/**
 * Generates a report of Dryad items with invalid DataCite XML, resulting in
 * failed DOI Registration.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
@Distributive
public class DataCiteXMLValidator extends AbstractCurationTask {
    private static final Logger log = Logger.getLogger(DataCiteXMLValidator.class);
    private static final String HEADERS = "\"Identifier\",\"Status\",\"Result\",\"Detail\"";
    private static SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
    private void report(Item item, Integer status, String result, String detail) {
        DCValue[] dcIdentifier = item.getMetadata("dc.identifier");
        String identifier;
        if (dcIdentifier.length > 0) {
            identifier = dcIdentifier[0].value;
        } else {
            identifier = String.valueOf(item.getID());
        }

        String message = String.format("\"%s\",%d,\"%s\",\"%s\"",
                identifier,
                status,
                result,
                detail);
        report(message);
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        builder.setFeature("http://apache.org/xml/features/validation/schema", true);
        // This is stored locally to keep the validation quick.
        // If we change the datacite schema we must change this as well.
        String xsdFilePath = ConfigurationManager.getProperty("dspace.dir") + "/config/external-schema/datacite-kernel-2.2/metadata.xsd";
        builder.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                "http://datacite.org/schema/kernel-2.2 file://" + xsdFilePath);
        report(HEADERS);
        distribute(dso);
        return Curator.CURATE_SUCCESS;
    }

    /**
     * Check an individual item and report its XML validation status
     * @param item
     * @throws SQLException
     * @throws IOException
     */
    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        // Get the item
        // Determine if it should be registered
        Boolean shouldCheck = (
                item.isArchived() ||
                DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(item)
                );
        if(shouldCheck == false) {
            report(item, Curator.CURATE_SKIP, "not archived or in blackout",null);
            return;
        }
        // crosswalk it to get xml
        Map<String,String> metadata = CDLDataCiteService.createMetadataListXML(item);
        // get doi and target?
        String dataCiteXmlMetadata = metadata.get(CDLDataCiteService.DATACITE);
        // validate xml against datacite schema
        // from http://www.jdom.org/docs/faq.html#a0370
        try {
            InputStream in = new ByteArrayInputStream(dataCiteXmlMetadata.getBytes("UTF-8"));
            Document doc = builder.build(in);
            report(item, Curator.CURATE_SUCCESS, "Validated", null);
        } catch (JDOMException ex) {
            // There was an exception validating
            String cause = ex.getCause().getMessage();
            report(item, Curator.CURATE_FAIL, "Failed", cause);
        }
    }

}
