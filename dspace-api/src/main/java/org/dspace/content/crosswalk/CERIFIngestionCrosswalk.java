/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Element;
import org.jdom.transform.JDOMResult;
import org.jdom.transform.JDOMSource;

/**
 * Implementation of {@link IngestionCrosswalk} to add metadata to the given
 * Item taking the information from the CERIF xml.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CERIFIngestionCrosswalk implements IngestionCrosswalk {

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> metadata, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        for (Element singleMetadata : metadata) {
            ingest(context, dso, singleMetadata, createMissingMetadataFields);
        }

    }

    @Override
    public void ingest(Context context, DSpaceObject dso, Element metadata, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        IngestionCrosswalk dimIngestionCrosswalk = getDIMIngestionCrosswalk();
        Element dimRoot = buildDIM(metadata);

        dimIngestionCrosswalk.ingest(context, dso, dimRoot, createMissingMetadataFields);

    }

    private Element buildDIM(Element metadata) throws CrosswalkException {

        try {

            Source xslt = getXslt();
            Source xml = new JDOMSource(metadata);
            JDOMResult out = new JDOMResult();

            TransformerFactory factory = TransformerFactory.newInstance();

            Transformer transformer = factory.newTransformer(xslt);
            transformer.transform(xml, out);

            return out.getDocument().getRootElement();

        } catch (TransformerException e) {
            throw new CrosswalkException(e);
        }
    }

    private StreamSource getXslt() {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File templateFile = new File(parent, "");
        return new StreamSource(templateFile);
    }

    private IngestionCrosswalk getDIMIngestionCrosswalk() {
        Object crosswalk = pluginService.getNamedPlugin(IngestionCrosswalk.class, "dim");
        if (crosswalk == null) {
            throw new IllegalArgumentException("No DIM ingestion crosswalk found");
        }
        return (IngestionCrosswalk) crosswalk;
    }

}
