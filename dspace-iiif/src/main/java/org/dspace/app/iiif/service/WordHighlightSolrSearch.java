/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.dspace.app.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.iiif.model.generator.CanvasGenerator;
import org.dspace.app.iiif.model.generator.ContentAsTextGenerator;
import org.dspace.app.iiif.model.generator.ManifestGenerator;
import org.dspace.app.iiif.model.generator.SearchResultGenerator;
import org.dspace.app.iiif.service.utils.IIIFUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


/**
 * This service implements methods for executing a solr search and creating IIIF search result annotations.
 * <p>
 * https://github.com/dbmdz/solr-ocrhighlighting
 */
@Scope("prototype")
@Component
public class WordHighlightSolrSearch implements SearchAnnotationService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(WordHighlightSolrSearch.class);

    private String endpoint;
    private String manifestId;

    @Autowired
    IIIFUtils utils;

    @Autowired
    ContentAsTextGenerator contentAsText;

    @Autowired
    SearchResultGenerator searchResult;

    @Autowired
    ManifestGenerator manifestGenerator;


    @Override
    public boolean useSearchPlugin(String className) {
        return className.contentEquals(WordHighlightSolrSearch.class.getCanonicalName());
    }

    @Override
    public void initializeQuerySettings(String endpoint, String manifestId) {
        this.endpoint = endpoint;
        this.manifestId = manifestId;
    }

    @Override
    public String getSearchResponse(UUID uuid, String query) {
        String json = "";
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String solrService = configurationService.getProperty("iiif.search.url");
        boolean validationEnabled =  configurationService
                .getBooleanProperty("discovery.solr.url.validation.enabled");
        UrlValidator urlValidator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);
        if (urlValidator.isValid(solrService) || validationEnabled) {
            HttpSolrClient solrServer = new HttpSolrClient.Builder(solrService).build();
            solrServer.setUseMultiPartPost(true);
            SolrQuery solrQuery = getSolrQuery(adjustQuery(query), manifestId);
            QueryRequest req = new QueryRequest(solrQuery);
            // returns raw json response.
            req.setResponseParser(new NoOpResponseParser("json"));
            NamedList<Object> resp;
            try {
                resp = solrServer.request(req);
                json =  (String) resp.get("response");
            } catch (SolrServerException | IOException e) {
                throw new RuntimeException("Unable to retrieve search response.", e);
            }
        } else {
            log.error("Error while initializing solr, invalid url: " + solrService);
        }
        return getAnnotationList(uuid, json, query);
    }

    /**
     * Wraps multi-word queries in parens.
     * @param query the search query
     * @return
     */
    private String adjustQuery(String query) {
        if (query.split(" ").length > 1) {
            return '(' + query + ')';
        }
        return query;
    }

    /**
     * Constructs a solr search URL.
     *
     * @param query the search terms
     * @param manifestId the id of the manifest in which to search
     * @return solr query
     */
    private SolrQuery getSolrQuery(String query, String manifestId) {
        SolrQuery solrQuery = new SolrQuery();
        solrQuery.set("q", "ocr_text:" + query + " AND manifest_url:\"" + manifestId + "\"");
        solrQuery.set(CommonParams.WT, "json");
        solrQuery.set("hl", "true");
        solrQuery.set("hl.ocr.fl", "ocr_text");
        solrQuery.set("hl.ocr.contextBlock", "line");
        solrQuery.set("hl.ocr.contextSize", "2");
        solrQuery.set("hl.snippets", "10");
        solrQuery.set("hl.ocr.trackPages", "off");
        solrQuery.set("hl.ocr.limitBlock","page");
        solrQuery.set("hl.ocr.absoluteHighlights", "true");

        return solrQuery;
    }

    /**
     * Generates a Search API response from the word_highlighting solr query response.
     *
     * The function assumes that the solr query responses contains page IDs
     * (taken from the ALTO Page ID element) in the following format:
     * Page.0, Page.1, Page.2....
     *
     * The identifier values must be aligned with zero-based IIIF canvas identifiers:
     * c0, c1, c2....
     *
     * This convention must be followed when indexing ALTO files into the word_highlighting
     * solr index. If it is not followed, word highlights will not align canvases.
     *
     * @param json solr search result
     * @param query the solr query
     * @return a search response in JSON
     */
    private String getAnnotationList(UUID uuid, String json, String query) {
        searchResult.setIdentifier(manifestId + "/search?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        // If error occurred or no body, return immediately
        if (body == null) {
            return utils.asJson(searchResult.generateResource());
        }

        // Example structure of Solr response available at
        // https://github.com/dbmdz/solr-ocrhighlighting/blob/main/docs/query.md
        // Get the outer ocrHighlighting node
        JsonNode highs = body.get("ocrHighlighting");
        if (highs != null) {
            // Loop through each highlight entry under ocrHighlighting
            for (final JsonNode highEntry : highs) {
                // Get the ocr_text node under the entry
                JsonNode ocrNode = highEntry.get("ocr_text");
                if (ocrNode != null) {
                    // Loop through the snippets array under that
                    for (final JsonNode snippet : ocrNode.get("snippets")) {
                        if (snippet != null) {
                            // Get a canvas ID based on snippet's pages
                            String pageId = getCanvasId(snippet.get("pages"));
                            if (pageId != null) {
                                // Loop through array of highlights for each snippet.
                                for (final JsonNode highlights : snippet.get("highlights")) {
                                    if (highlights != null) {
                                        // May be multiple word highlights on a page, so loop through them.
                                        for (int i = 0; i < highlights.size(); i++) {
                                            // Add annotation associated with each highlight
                                            AnnotationGenerator anno = getAnnotation(highlights.get(i), pageId, uuid);
                                            if (anno != null) {
                                                searchResult.addResource(anno);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return utils.asJson(searchResult.generateResource());
    }

    /**
     * Returns the annotation generator for the highlight.
     * @param highlight highlight node from Solr response
     * @param pageId page id from solr response
     * @return generator for a single annotation
     */
    private AnnotationGenerator getAnnotation(JsonNode highlight, String pageId, UUID uuid) {
        String text = highlight.get("text") != null ? highlight.get("text").asText() : null;
        int ulx = highlight.get("ulx") != null ? highlight.get("ulx").asInt() : -1;
        int uly = highlight.get("uly") != null ? highlight.get("uly").asInt() : -1;
        int lrx = highlight.get("lrx") != null ? highlight.get("lrx").asInt() : -1;
        int lry = highlight.get("lry") != null ? highlight.get("lry").asInt() : -1;
        String w = (lrx >= 0 && ulx >= 0) ? Integer.toString(lrx - ulx) : null;
        String h = (lry >= 0 && uly >= 0) ? Integer.toString(lry - uly) : null;

        if (text != null && w != null && h != null) {
            String params = ulx + "," + uly + "," + w + "," + h;
            return createSearchResultAnnotation(params, text, pageId, uuid);
        }
        return null;
    }

    /**
     * Returns position of canvas. Uses the "pages" id attribute.
     * This method assumes that the solr response includes a "page" id attribute that is
     * delimited with a "." and that the integer corresponds to the
     * canvas identifier in the manifest. For METS/ALTO documents, the page
     * order can be derived from the METS file when loading the solr index.
     * @param pagesNode the pages node
     * @return canvas id or null if node was null
     */
    private String getCanvasId(JsonNode pagesNode) {
        if (pagesNode != null) {
            JsonNode page = pagesNode.get(0);
            if (page != null) {
                JsonNode pageId = page.get("id");
                if (pageId != null) {
                    String[] identArr = pageId.asText().split("\\.");
                    // the canvas id.
                    return "c" + identArr[1];
                }
            }
        }
        return null;
    }

    /**
     * Creates annotation with word highlight coordinates.
     *
     * @param params word coordinate parameters used for highlighting.
     * @param text word text
     * @param pageId the page id returned by solr
     * @param uuid the dspace item identifier
     * @return a single annotation object that contains word highlights on a single page (canvas)
     */
    private AnnotationGenerator createSearchResultAnnotation(String params, String text, String pageId, UUID uuid) {
        String annotationIdentifier = this.endpoint + uuid + "/annot/" + pageId + "-" + params;
        String canvasIdentifier = this.endpoint + uuid + "/canvas/" + pageId + "#xywh=" + params;
        contentAsText.setText(text);
        CanvasGenerator canvas = new CanvasGenerator(canvasIdentifier);

        AnnotationGenerator annotationGenerator = new AnnotationGenerator(annotationIdentifier,
                AnnotationGenerator.PAINTING)
                .setOnCanvas(canvas)
                .setResource(contentAsText)
                .setWithin(getWithinManifest());

        return annotationGenerator;
    }

    private List<ManifestGenerator> getWithinManifest() {
        List<ManifestGenerator> withinList = new ArrayList<>();
        manifestGenerator.setIdentifier(manifestId);
        withinList.add(manifestGenerator);
        return withinList;
    }

}
