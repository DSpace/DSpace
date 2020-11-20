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

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom.Document;
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

    private String idPrefix;

    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> elements, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (CollectionUtils.isEmpty(elements)) {
            throw new CrosswalkException("No CERIF root element to ingest found");
        }

        if (elements.size() > 1) {
            throw new CrosswalkException("Multiple CERIF elements ingestion not supported");
        }

        ingest(context, dso, elements.get(0), createMissingMetadataFields);

    }

    @Override
    public void ingest(Context context, DSpaceObject dso, Element cerifRootElement, boolean createMissingMetadataFields)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (dso.getType() != Constants.ITEM) {
            throw new IllegalArgumentException("Only items can be ingested by the CERIFIngestionCrosswalk");
        }

        Element dimRoot = buildDIM(cerifRootElement);

        IngestionCrosswalk dimIngestionCrosswalk = getDIMIngestionCrosswalk();
        dimIngestionCrosswalk.ingest(context, dso, dimRoot, createMissingMetadataFields);

    }

    private Element buildDIM(Element cerifRootElement) throws CrosswalkException {

        try {

            Source xslt = getXslt();
            Source xml = new JDOMSource(cerifRootElement);
            JDOMResult out = new JDOMResult();

            TransformerFactory factory = TransformerFactory.newInstance();

            Transformer transformer = factory.newTransformer(xslt);
            transformer.setParameter("nestedMetadataPlaceholder", CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE);
            if (idPrefix != null) {
                transformer.setParameter("idPrefix", AuthorityValueService.GENERATE + idPrefix);
            }

            transformer.transform(xml, out);

            Document document = out.getDocument();
            if (document == null) {
                throw new CrosswalkException("An error occurs converting the CERIF xml to the internal DIM xml");
            }

            return document.getRootElement();

        } catch (TransformerException e) {
            throw new CrosswalkException(e);
        }
    }

    private StreamSource getXslt() {
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        return new StreamSource(new File(parent, "crosswalks/oai/metadataFormats/oai_cerif_to_dim.xsl"));
    }

    private IngestionCrosswalk getDIMIngestionCrosswalk() {
        Object crosswalk = pluginService.getNamedPlugin(IngestionCrosswalk.class, "dim");
        if (crosswalk == null) {
            throw new IllegalArgumentException("No DIM ingestion crosswalk found");
        }
        return (IngestionCrosswalk) crosswalk;
    }

    public void setIdPrefix(String idPrefix) {
        this.idPrefix = idPrefix;
    }

}
