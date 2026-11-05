/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.Logger;
import org.dspace.app.client.DSpaceHttpClientFactory;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;

/**
 * ORE ingestion crosswalk
 * <p>
 * Processes an Atom-encoded ORE resource map and attempts to interpret it as a DSpace item.
 *
 * @author Alexey Maslov
 */
public class OREIngestionCrosswalk
        implements IngestionCrosswalk {
    /**
     * log4j category
     */
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

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

    private String[] forbiddenHostUrls = null;

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
            .getBitstreamFormatService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void ingest(Context context, DSpaceObject dso, List<Element> metadata, boolean createMissingMetadataFields)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {

        // If this list contains only the root already, just pass it on
        if (metadata.size() == 1) {
            ingest(context, dso, metadata.getFirst(), createMissingMetadataFields);
        } else {
            // Otherwise, wrap them up
            Element wrapper = new Element("wrap", metadata.getFirst().getNamespace());
            wrapper.addContent(metadata);

            ingest(context, dso, wrapper, createMissingMetadataFields);
        }
    }


    @Override
    public void ingest(Context context, DSpaceObject dso, Element root, boolean createMissingMetadataFields)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {

        Instant timeStart = Instant.now();

        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("OREIngestionCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item) dso;

        if (root == null) {
            System.err.println("The element received by ingest was null");
            return;
        }

        var followRedirects = configurationService
                .getBooleanProperty("oai.harvester.ore.follow-redirects", false);
        var requestConfig = RequestConfig.custom()
                .setRedirectsEnabled(followRedirects).build();

        Document doc = new Document();
        doc.addContent(root.detach());

        List<Element> aggregatedResources;
        String entryId;
        XPathExpression<Element> xpathLinks =
                XPathFactory.instance()
                        .compile("/atom:entry/atom:link[@rel=\"" + ORE_NS.getURI() + "aggregates" + "\"]",
                                Filters.element(), null, ATOM_NS);
        aggregatedResources = xpathLinks.evaluate(doc);

        XPathExpression<Attribute> xpathAltHref =
                XPathFactory.instance()
                        .compile("/atom:entry/atom:link[@rel='alternate']/@href",
                                Filters.attribute(), null, ATOM_NS);
        entryId = xpathAltHref.evaluateFirst(doc).getValue();

        // Next for each resource, create a bitstream
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(false);
        nf.setMinimumIntegerDigits(4);

        for (Element resource : aggregatedResources) {
            String href = resource.getAttributeValue("href");
            log.debug("ORE processing: " + href);
            String processedURL;
            try {
                processedURL = new URIBuilder(href).build().toString();
            } catch (URISyntaxException e) {
                throw new CrosswalkException("Could not parse URI: " + href, e);
            }

            String bundleName;
            Element desc = null;
            XPathExpression<Element> xpathDesc =
                    XPathFactory.instance()
                            .compile("/atom:entry/oreatom:triples/rdf:Description[@rdf:about=\"" +
                                            this.encodeForURL(href) + "\"][1]",
                                    Filters.element(), null, ATOM_NS, ORE_ATOM, RDF_NS);
            desc = xpathDesc.evaluateFirst(doc);

            if (desc != null && desc.getChild("type", RDF_NS).getAttributeValue("resource", RDF_NS)
                    .equals(DS_NS.getURI() + "DSpaceBitstream")) {
                bundleName = desc.getChildText("description", DCTERMS_NS);
                log.debug("Setting bundle name to: " + bundleName);
            } else {
                log.info("Could not obtain bundle name; using 'ORIGINAL'");
                bundleName = "ORIGINAL";
            }

            // Bundle names are not unique, so we just pick the first one if there's more than one.
            List<Bundle> targetBundles = itemService.getBundles(item, bundleName);
            Bundle targetBundle;

            // if null, create the new bundle and add it in
            if (targetBundles.isEmpty()) {
                targetBundle = bundleService.create(context, item, bundleName);
                itemService.addBundle(context, item, targetBundle);
            } else {
                targetBundle = targetBundles.getFirst();
            }

            if (StringUtils.isBlank(href)) {
                throw new CrosswalkException("The href attribute is required for the ORE ingestion.");
            }
            try (CloseableHttpClient httpClient = DSpaceHttpClientFactory.getInstance()
                    .buildWithRequestConfig(requestConfig)) {
                if (!validResourceUri(processedURL)) {
                    throw new FileNotFoundException("Invalid resource URI: " + processedURL);
                }
                // Generate a request for the aggregated resource
                HttpGet httpGet = new HttpGet(processedURL);
                HttpResponse response = httpClient.execute(httpGet);
                if (response == null || response.getEntity() == null) {
                    throw new FileNotFoundException(processedURL + " returned a null response or body");
                }
                if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                    throw new FileNotFoundException(processedURL
                            + " returned a " + response.getStatusLine() + " response");
                }
                if (response.getEntity() == null || response.getEntity().getContent() == null) {
                    throw new FileNotFoundException(processedURL + " returned an empty body");
                }
                // ingest and update
                try (InputStream in = response.getEntity().getContent()) {
                    ingestStreamAsBitstream(context, in, targetBundle, resource, entryId);
                }
            }
        }
        log.info(
                "OREIngest for Item " + item.getID() + " took: " +
                        (Instant.now().toEpochMilli() - timeStart.toEpochMilli()) + "ms.");
    }

    /**
     * Read an input stream and ingest it as a bitstream to the target bundle
     * @param context Dspace context
     * @param inputStream input stream containing bitstream content
     * @param targetBundle the target bundle for the new bitstream
     * @param resource the ORE resource Element
     * @param entryId the entry ID of the ORE resource
     * @throws AuthorizeException if current user does not have permission to add / create bitstream
     * @throws IOException if the input stream is null or unreadable
     * @throws SQLException if bitstream or bundle database operations fail
     */
    void ingestStreamAsBitstream(Context context, InputStream inputStream,
                                         Bundle targetBundle, Element resource, String entryId)
            throws AuthorizeException, IOException, SQLException {
        // ingest and update
        if (inputStream != null) {
            Bitstream newBitstream = bitstreamService.create(context, targetBundle, inputStream);

            String bsName = resource.getAttributeValue("title");
            newBitstream.setName(context, bsName);

            // Identify the format
            String mimeString = resource.getAttributeValue("type");
            BitstreamFormat bsFormat = bitstreamFormatService.findByMIMEType(context, mimeString);
            if (bsFormat == null) {
                bsFormat = bitstreamFormatService.guessFormat(context, newBitstream);
            }
            newBitstream.setFormat(context, bsFormat);
            bitstreamService.update(context, newBitstream);

            bundleService.addBitstream(context, targetBundle, newBitstream);
            bundleService.update(context, targetBundle);
        } else {
            throw new IOException("Could not read input stream for: " + entryId);
        }
    }

    /**
     * Helper method to escape all characters that are not part of the canon set
     *
     * @param sourceString source unescaped string
     */
    private String encodeForURL(String sourceString) {
        Character[] lowalpha = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
            'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        Character[] upalpha = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
        Character[] digit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
        Character[] mark = {'-', '_', '.', '!', '~', '*', '\'', '(', ')'};

        // reserved
        Character[] reserved = {';', '/', '?', ':', '@', '&', '=', '+', '$', ',', '%', '#'};

        Set<Character> URLcharsSet = new HashSet<Character>();
        URLcharsSet.addAll(Arrays.asList(lowalpha));
        URLcharsSet.addAll(Arrays.asList(upalpha));
        URLcharsSet.addAll(Arrays.asList(digit));
        URLcharsSet.addAll(Arrays.asList(mark));
        URLcharsSet.addAll(Arrays.asList(reserved));

        StringBuilder processedString = new StringBuilder();
        for (int i = 0; i < sourceString.length(); i++) {
            char ch = sourceString.charAt(i);
            if (URLcharsSet.contains(ch)) {
                processedString.append(ch);
            } else {
                processedString.append("%").append(Integer.toHexString((int) ch));
            }
        }

        return processedString.toString();
    }

    /**
     * Validate a resource URI against the host and scheme of the remote OAI endpoint, or a configured
     * list of allowed prefixes. Some default forbidden hosts are also included to prevent SSRF.
     * Even if the URL prefix validation is disabled, schemes will still be enforced to http(s) so file:/// and
     * other unwanted schemes cannot be used
     * @param resourceUrl the resource URL of the aggregated ORE resource
     * @return result of the validation
     */
    boolean validResourceUri(String resourceUrl) {
        URI resourceUri;
        try {
            resourceUri = new URI(resourceUrl).normalize();
        } catch (URISyntaxException | NullPointerException e) {
            log.error("Invalid resource URI: " + resourceUrl);
            return false;
        }
        String resourceHost = resourceUri.getHost();

        Set<String> allowedSchemes = Set.of("http", "https");
        String resourceScheme = resourceUri.getScheme();
        if (resourceScheme == null ||
                !allowedSchemes.contains(resourceScheme.toLowerCase(Locale.ROOT))) {
            log.warn("Illegal scheme requested for ORE resource: {}", resourceUrl);
            return false;
        }

        initializeForbiddenHosts();
        if (forbiddenHostUrls != null) {
            for (String forbiddenHostUrl : forbiddenHostUrls) {
                String forbiddenHost = extractHostname(forbiddenHostUrl);
                if (forbiddenHost != null && Objects.equals(forbiddenHost, resourceHost)) {
                    log.warn("Forbidden hostname in ORE resource URL: {}", resourceUrl);
                    return false;
                }
            }
        }

        // This is now very strict - it is up to administrators to configure a list of allowed URL prefixes.
        // If validateHost is set to 'true', and an empty list will cause every resource URL to be rejected
        // Unlike forbidden URLs, we expect these to be hostnames rather than partial or full URIs as well
        if (configurationService.getBooleanProperty("oai.harvester.ore.file.validateHost", false)) {
            for (String allowedHost : configurationService
                    .getArrayProperty("oai.harvester.ore.file.allowedHosts")) {
                if (Objects.equals(resourceUri.getHost().toLowerCase(Locale.ROOT),
                        allowedHost.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
    }

    /**
     * To keep config easier to maintain with both plain hostnames like 'bad.example.com' and references
     * to partial URLs elsewhere in configuration like ${dspace.ui.url}, we test to see which it is
     * and return the string representing the hostname.
     * @param urlOrHost URL, partial URL or hostname to extract and validate
     * @return extracted hostname or null
     */
    String extractHostname(String urlOrHost) {
        if (StringUtils.isBlank(urlOrHost)) {
            return null;
        }
        try {
            URI uri = new URI(urlOrHost);
            String host = uri.getHost();
            if (host != null) {
                return host.toLowerCase(Locale.ROOT);
            }
        } catch (URISyntaxException e) {
            // pass through
        }
        // Return plain lower-cased string as defaulg
        return urlOrHost.toLowerCase(Locale.ROOT);
    }

    /**
     * Initialize forbidden hosts from config, if not set already
     */
    void initializeForbiddenHosts() {
        if (forbiddenHostUrls == null) {
            String dspaceServerUrl = configurationService.getProperty("dspace.server.url");
            String dspaceUiUrl = configurationService.getProperty("dspace.ui.url");
            String[] localAndEC2Hosts = {"localhost", "127.0.0.1", "[::1]", "169.254.169.254", "[fd00:ec2::254]"};
            List<String> defaultForbiddenUrlPrefixes = new ArrayList<>();
            Collections.addAll(defaultForbiddenUrlPrefixes, localAndEC2Hosts);
            if (StringUtils.isNotBlank(dspaceServerUrl)) {
                defaultForbiddenUrlPrefixes.add(dspaceServerUrl);
            }
            if (StringUtils.isNotBlank(dspaceUiUrl)) {
                defaultForbiddenUrlPrefixes.add(dspaceUiUrl);
            }
            forbiddenHostUrls = configurationService.getArrayProperty(
                    "oai.harvester.ore.file.forbiddenHostUrls",
                    defaultForbiddenUrlPrefixes.toArray(new String[0]));
        }
    }

}
