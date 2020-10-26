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
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} to produce a document
 * in a specific format (pdf, rtf etc...) from an item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DocumentCrosswalk implements StreamDisseminationCrosswalk, FileNameDisseminator {

    private final ConfigurationService configurationService;

    private final String fileName;

    private final String mimeType;

    private final String templateFileName;

    private final ReferCrosswalk referCrosswalk;

    public DocumentCrosswalk(ConfigurationService configurationService, ReferCrosswalk referCrosswalk, String fileName,
        String templateFileName, String mimeType) {
        this.configurationService = configurationService;
        this.fileName = fileName;
        this.referCrosswalk = referCrosswalk;
        this.templateFileName = templateFileName;
        this.mimeType = mimeType;
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("ReferCrosswalk can only crosswalk an Item.");
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
        return (dso.getType() == Constants.ITEM);
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

}
