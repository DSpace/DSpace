/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} to produce a document
 * in a specific format (pdf, rtf etc...) from an item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DocumentCrosswalk implements StreamDisseminationCrosswalk, FileNameDisseminator {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    private String fileName;

    private String mimeType;

    private String templateFileName;

    private String entityType;

    private ReferCrosswalk referCrosswalk;

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (!canDisseminate(context, dso)) {
            throw new CrosswalkObjectNotSupported("Can only crosswalk an Item with the configured type: " + entityType);
        }

        Item item = (Item) dso;

        ByteArrayInputStream xmlInputStream = getItemAsXml(context, item);

        try {
            transformToDocument(out, xmlInputStream);
        } catch (Exception e) {
            throw new CrosswalkException(e);
        }

    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return (dso.getType() == Constants.ITEM) && hasExpectedEntityType((Item) dso);
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMIMEType() {
        return mimeType;
    }

    private ByteArrayInputStream getItemAsXml(Context context, Item item)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, item, baos);
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private void transformToDocument(OutputStream out, ByteArrayInputStream xmlInputStream) throws Exception {
        // the XML file which provides the input
        StreamSource xmlSource = new StreamSource(xmlInputStream);
        // create an instance of fop factory
        FopFactory fopFactory = FopFactory.newInstance(new File(".").toURI());
        // a user agent is needed for transformation
        FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
        // Construct fop with desired output format
        Fop fop = fopFactory.newFop(getMIMEType(), foUserAgent, out);

        // Setup XSLT
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(getTemplate());
        transformer.setParameter("imageDir", getImageDir());

        // Resulting SAX events (the generated FO) must be piped through to FOP
        Result res = new SAXResult(fop.getDefaultHandler());

        // Start XSLT transformation and FOP processing
        // That's where the XML is first transformed to XSL-FO and then PDF is created
        transformer.transform(xmlSource, res);
    }

    private StreamSource getTemplate() {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File templateFile = new File(parent, templateFileName);
        return new StreamSource(templateFile);
    }

    private String getImageDir() {
        String tempDir = configurationService.getProperty("crosswalk.virtualfield.bitstream.tempdir", " export");
        return new File(System.getProperty("java.io.tmpdir"), tempDir).getAbsolutePath();
    }

    private boolean hasExpectedEntityType(Item item) {
        String relationshipType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return Objects.equals(relationshipType, entityType);
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    public void setReferCrosswalk(ReferCrosswalk referCrosswalk) {
        this.referCrosswalk = referCrosswalk;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Optional<String> getEntityType() {
        return Optional.ofNullable(entityType);
    }


}
