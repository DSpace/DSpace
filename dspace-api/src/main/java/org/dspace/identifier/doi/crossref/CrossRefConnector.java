/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.crossref;

import static org.dspace.identifier.DOIIdentifierProvider.CFG_DOI_METADATA;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.doi.DOIConnector;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CrossRefConnector implements DOIConnector {

    private static final Logger LOG = LoggerFactory.getLogger(CrossRefConnector.class);

    private final ItemService itemService;
    private final DOIService doiService;
    private final DOIResolverClient doiResolverClient;
    private final CrossRefClient crossRefClient;
    private final CrossRefPayloadService crossRefPayloadService;
    private final HandleService handleService;
    private final ConfigurationService configurationService;
    private final boolean crossRefOverride;
    private static final String CROSSREF_SCHEMA = "http://www.crossref.org/schema/4.4.2";

    // dc.identifier.issn
    private String parentIssnField;

    private String registrant;
    private String depositor;
    private String depositorEmail;
    private String SCHEME = "https";

    @Autowired
    public CrossRefConnector(ItemService itemService,
            DOIService doiService,
            DOIResolverClient doiResolverClient,
            CrossRefClient crossRefClient,
            CrossRefPayloadService crossRefPayloadService,
            HandleService handleService,
            ConfigurationService configurationService,
            @Value("${identifier.doi.crossref.override:false}") boolean crossRefOverride,
            @Value("${identifier.doi.crossref.parentIssnField:dc.identifier.issn}") String parentIssnField,
            @Value("${identifier.doi.crossref.registrant:REGISTRANT}") String registrant,
            @Value("${identifier.doi.crossref.depositor:DEPOSITOR}") String depositor,
            @Value("${identifier.doi.crossref.depositorEmail:DEPOSITOREMAIL}") String depositorEmail) {
        this.handleService = handleService;
        this.configurationService = configurationService;
        this.itemService = itemService;
        this.doiService = doiService;
        this.doiResolverClient = doiResolverClient;
        this.crossRefClient = crossRefClient;
        this.crossRefPayloadService = crossRefPayloadService;
        this.crossRefOverride = crossRefOverride;
        this.parentIssnField = parentIssnField;
        this.registrant = registrant;
        this.depositor = depositor;
        this.depositorEmail = depositorEmail;

    }

    @Override
    public boolean isDOIReserved(Context context, String doi) {
        // Crossref does not require reservation of DOIs, we just return true
        return true;
    }

    @Override
    public boolean isDOIRegistered(Context context, String doi) throws DOIIdentifierException {
        return isDOIRegistered(context, null, doi);
    }

    private boolean isDOIRegistered(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {
        var response = doiResolverClient.sendDOIGetRequest(doi);

        if (response.statusCode() == getDoiGetSuccessStatusCode()) {

            if (dso == null) {
                return true;
            }

            var doiUrl = response.url();

            if (doiUrl == null) {
                LOG.error("Received a status code 302 without a response content. DOI: {}.", doi);
                throw new DOIIdentifierException("Received a http status code 302 without a response content.",
                        DOIIdentifierException.BAD_ANSWER);
            }

            var dsoUrl = getDsoUrl(context, dso);

            if (dsoUrl == null) {
                // the handle of the dso was not found in our db?!
                LOG.error("The HandleManager was unable to find the handle of a DSpaceObject in the database! "
                          + "Type: {} ID: {}", getTypeText(dso), dso.getID());
                throw new RuntimeException(
                        "The HandleManager was unable to find the handle of a DSpaceObject in the database!");
            }

            return (dsoUrl.equals(doiUrl));
        }

        // 404 "Not Found" means DOI is neither reserved nor registered.
        if (response.statusCode() == 404) {
            return false;
        }

        // Catch all other http status code in case we forgot one.
        LOG.warn(
                "While checking if the DOI {} is registered, we got a "
                + "http status code {} and the message \"{}\".", doi, response.statusCode(), response.content());
        throw new DOIIdentifierException(
                "Unable to parse an answer from " + "DataCite API. Please have a look into DSpace logs.",
                DOIIdentifierException.BAD_ANSWER);
    }

    /**
     * This method returns the URL the doi should resolve to.
     */
    private String getDsoUrl(Context context, DSpaceObject dso) throws DOIIdentifierException {
        try {
            return handleService.resolveToURL(context, dso.getHandle());
        } catch (SQLException e) {
            throw new DOIIdentifierException("could not lookup url", e);
        }
    }

    static String getTypeText(DSpaceObject dso) {
        var type = dso.getType();

        return Constants.typeText[type];
    }

    private int getDoiGetSuccessStatusCode() {
        // We are resolving the DOI and if that is successful, we'll get a 302 Found
        // back
        return 302;
    }

    @Override
    public void deleteDOI(Context context, String doi) {
        LOG.warn("Skipping deletion of DOI as Crossref does not allow deactivation of DOIs.");
    }

    @Override
    public void reserveDOI(Context context, DSpaceObject dso, String doi) {
        LOG.warn("Skipping reserving DOI as Crossref does not require this step.");
    }

    @Override
    public void registerDOI(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {

        if (!(dso instanceof Item item)) {
            throw new IllegalArgumentException("cannot register DOIs for non-Item objects!");
        }

        // check if the DOI is already registered online
        if (isDOIRegistered(context, doi)) {
            // if it is registered for another object we should notify an admin
            if (!crossRefOverride && !isDOIRegistered(context, dso, doi)) {
                LOG.warn("DOI {} is registered for another object already.", doi);
                throw new DOIIdentifierException(DOIIdentifierException.DOI_ALREADY_EXISTS);
            }
        }

        List<MetadataValue> typeMetaData = itemService.getMetadata(item, "dc", "type", Item.ANY, Item.ANY);

        if (typeMetaData == null || typeMetaData.isEmpty()) {
            throw new DOIIdentifierException("Type of record is missing.");
        }

        String type = typeMetaData.get(0).getValue();

        validateItemMetadata(item, type);

        var metadataDOI = extractDOI(item);
        if (metadataDOI != null) {
            var existingDoi = stripDoiUrl(metadataDOI);
            // Are we trying to update the wrong object?
            if (!existingDoi.equals(doi)) {
                LOG.info("DSO with type {} and id {} already has DOI {} which doesn't match {}. Won't register object.",
                        getTypeText(dso), dso.getID(), metadataDOI, doi);
                return;
            }
        }

        Element payload = crossRefPayloadService.disseminate(context, dso, type, doi);

        // Add additional XML elements not derived from metadata
        addHeadInfo(payload, dso);
        try {
            addDoi(context, payload, doi, dso);
        } catch (SQLException ex) {
            LOG.debug("Error while ingesting doi into crossref xml", ex);
            throw new RuntimeException(ex);
        }

        HttpResponse resp = crossRefClient.sendDepositRequest(payload);
        LOG.debug("Deposit DOI response: {}", resp);
    }

    private String stripDoiUrl(String doiUrl) {
        return doiUrl.replaceFirst("https?://(dx\\.)?doi\\.org(:\\d+)?/", "doi:");
    }

    private void validateItemMetadata(Item item, String type) throws DOIIdentifierException {
        if ("article".equals(type)) {
            // Journal articles must have a journal ISSN to provide as journal metadata
            String[] issnFieldParts = parentIssnField.split("\\.");
            if (issnFieldParts.length < 2) {
                LOG.error("Invalid ISSN field configured in spring. Should be eg. dc.identifier.issn");
                throw new DOIIdentifierException(DOIIdentifierException.CONVERSION_ERROR);
            }
            // Get ISSNs
            var issns = itemService.getMetadata(item, issnFieldParts[0], issnFieldParts[1],
                    (issnFieldParts.length == 3 ? issnFieldParts[2] : null), Item.ANY);
            if (issns.isEmpty()) {
                LOG.error("article type must supply at least one ISSN or DOI to identify the parent publication.");
                throw new DOIIdentifierException(DOIIdentifierException.CONVERSION_ERROR);
            }
        }
    }

    @Override
    public void updateMetadata(Context context, DSpaceObject dso, String doi) throws DOIIdentifierException {
        if (!crossRefOverride && isDOIRegistered(context, dso, doi)) {
            LOG.warn("Trying to update metadata for DOI {}, that is reserved for another dso.", doi);
            throw new DOIIdentifierException("Trying to update metadata for a DOI that is reserved for another object.",
                    DOIIdentifierException.DOI_ALREADY_EXISTS);
        }

        // We can simply make another deposit request to Crossref for updating metadata
        this.registerDOI(context, dso, doi);
    }

    /**
     * This method gets the doi from the passed object if it already has one.
     *
     * @param dso the object to extract the doi from
     * @return the doi of the object or null
     */
    private String extractDOI(Item dso) {
        MetadataFieldName doiMetadataFieldName = new MetadataFieldName(
                this.configurationService.getProperty(CFG_DOI_METADATA, "dc.identifier.doi"));
        List<MetadataValue> metadataValues = itemService.getMetadata(dso,
                doiMetadataFieldName.schema,
                doiMetadataFieldName.element,
                doiMetadataFieldName.qualifier,
                Item.ANY);

        if (metadataValues.size() == 0) {
            return null;
        } else {
            if (metadataValues.size() > 1) {
                LOG.warn("Found multiple DOI metadata fields for item {}, using the first one", dso.getID());
            }
            return metadataValues.get(0).getValue();
        }
    }

    /**
     * Crossref requires any request to have an integer version of date and time
     * upon which it decides if a record needs to be updated if it already exits.
     *
     * @param root
     * @return
     */
    protected Element addTimestamp(Element root) {
        Element identifier = new Element("timestamp", CROSSREF_SCHEMA);
        identifier.addContent(new Date().getTime() + "");
        return root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(identifier);
    }

    /**
     * Crossref requires a batch id. This method adds a batch id created out of the handle
     * of a DSpaceObject to the head element.
     *
     * @param root The root of the XML.
     * @return The parent of the new created element (<code>head</code>)
     */
    protected Element addBatchId(Element root, DSpaceObject dso) {
        Element batchId = new Element("doi_batch_id", CROSSREF_SCHEMA);
        batchId.addContent(dso.getHandle().replaceAll("/", "_"));
        LOG.info("Set id: " + dso.getHandle().replaceAll("/", "_"));
        return root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(0, batchId);
    }

    /**
     * Adds the <code>head</code> information to the XML document to be submitted
     * to Crossref. Specifically, this method adds timestamp, batch id, depositor,
     * and registrant information. Depositor and registrant values should be configured
     * in dspace.cfg.
     *
     * @param root The root of the XML.
     * @param dso The DSpaceObject for which the XML is being created.
     * @return The parent of the new created element (<code>head</code>)
     */
    protected Element addHeadInfo(Element root, DSpaceObject dso) {
        // add batch id
        addBatchId(root, dso);

        // add timestamp
        addTimestamp(root);

        // add depositor element
        Element depositor = new Element("depositor", CROSSREF_SCHEMA);
        Element depositorName = new Element("depositor_name", CROSSREF_SCHEMA);
        depositorName.addContent(this.depositor);
        Element depositorEmail = new Element("email_address", CROSSREF_SCHEMA);
        depositorEmail.addContent(this.depositorEmail);

        depositor.addContent(depositorName);
        depositor.addContent(depositorEmail);

        root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(depositor);

        // add registrant
        Element registrant = new Element("registrant", CROSSREF_SCHEMA);
        registrant.addContent(this.registrant);

        return root.getChild("head", Namespace.getNamespace(CROSSREF_SCHEMA)).addContent(registrant);
    }

    /**
     * Add the doi to the xml that will be send to CrossRef. We expect the XML to contain exactly one node doi_data
     * into which we will ingest the doi information.
     * @param c org.dspace.core.Context
     * @param root The XML into which the doi information shall be ingested.
     * @param doi The doi to ingest.
     * @param dso The DSpaceObject for which the DOI is registered, to create the URL to which the DOI shall point to.
     * @return The XML with the ingested DOI information.
     * @throws SQLException
     */
    protected Element addDoi(Context c, Element root, String doi, DSpaceObject dso) throws SQLException {
        // create the information to ingest (doi and url)
        Element doiInformation = new Element("doi", CROSSREF_SCHEMA);
        doiInformation.addContent(doi.substring(SCHEME.length() - 1));
        Element resource = new Element("resource", CROSSREF_SCHEMA);
        resource.addContent(handleService.resolveToURL(c, dso.getHandle()));

        // find the node into which the information shall be ingested
        List<Element> nodes = null;
        try {
            XPathFactory xPathFactory = XPathFactory.instance();
            Namespace xns = Namespace.getNamespace("x", root.getNamespaceURI());
            XPathExpression<Element> expr = xPathFactory.compile(".//x:doi_data", Filters.element(), null, xns);
            nodes = expr.evaluate(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (nodes.size() != 1 ) {
            LOG.error("Trying to create invalid XML for Crossref. There should be exactly one node 'doi_data'.");
            throw new IllegalStateException("Trying to create invalid XML for Crossref. " +
                                            "There should be one node 'doi_data' only.");
        }

        Element doiData = nodes.get(0);
        doiData.addContent(doiInformation);
        doiData.addContent(resource);

        if (LOG.isDebugEnabled()) {
            Format format = Format.getCompactFormat();
            format.setEncoding("UTF-8");
            XMLOutputter xout = new XMLOutputter(format);
            LOG.debug("Ingested " + doi + ":");
            LOG.debug(xout.outputString(root));
        }
        return root;
    }
}
