/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.dspace.app.rest.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.ContentAsTextGenerator;
import org.dspace.app.rest.iiif.model.generator.ManifestGenerator;
import org.dspace.app.rest.iiif.model.generator.SearchResultGenerator;
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;


/**
 * This service provides methods for executing a solr search against the solr index. There should be a single
 * instance of this service per request. The {@code @RequestScope} provides a single instance created and
 * available during complete lifecycle of the HTTP request.
 * <p>
 * https://github.com/dbmdz/solr-ocrhighlighting
 */
@RequestScope
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
    SolrSearchCore solrSearchCore;

    @Autowired
    ManifestGenerator manifestGenerator;


    @Override
    public boolean getSearchPlugin(String className) {
        return className.contentEquals(WordHighlightSolrSearch.class.getSimpleName());
    }

    /**
     * The query requires these values before it can execute
     * @param endpoint the search service endpoint
     * @param manifestId the idea of the manifest to search within
     */
    @Override
    public void initializeQuerySettings(String endpoint, String manifestId) {
        this.endpoint = endpoint;
        this.manifestId = manifestId;
    }

    /**
     * Executes the Search API solr query.
     * @param query encoded query terms
     *
     * @return json query response
     */
    @Override
    public String getSolrSearchResponse(UUID uuid, String query) {
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
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        if (body == null) {
            log.warn("Unable to process json response.");
            return utils.asJson(searchResult.generate());
        }
        // outer ocr highlight element
        JsonObject highs = body.getAsJsonObject("ocrHighlighting");
        // highlight entries
        for (Map.Entry<String, JsonElement> ocrIds: highs.entrySet()) {
            // ocr_text
            JsonObject ocrObj = ocrIds.getValue().getAsJsonObject().getAsJsonObject("ocr_text");
            // snippets array
            if (ocrObj != null) {
                for (JsonElement snippetArray : ocrObj.getAsJsonObject().get("snippets").getAsJsonArray()) {
                    String pageId = getCanvasId(snippetArray.getAsJsonObject().get("pages"));
                    for (JsonElement highlights : snippetArray.getAsJsonObject().getAsJsonArray("highlights")) {
                        for (JsonElement highlight : highlights.getAsJsonArray()) {
                            searchResult.addResource(getAnnotation(highlight, pageId, uuid));
                        }
                    }
                }
            }
        }

        return utils.asJson(searchResult.generate());
    }

    /**
     * Returns the annotation generator for the highlight.
     * @param highlight highlight element from solor response
     * @param pageId page id from solr response
     * @return generator for a single annotation
     */
    private AnnotationGenerator getAnnotation(JsonElement highlight, String pageId, UUID uuid) {
        JsonObject hcoords = highlight.getAsJsonObject();
        String text = (hcoords.get("text").getAsString());
        int ulx = hcoords.get("ulx").getAsInt();
        int uly = hcoords.get("uly").getAsInt();
        int lrx = hcoords.get("lrx").getAsInt();
        int lry = hcoords.get("lry").getAsInt();
        String w = Integer.toString(lrx - ulx);
        String h = Integer.toString(lry - uly);
        String params = ulx + "," + uly + "," + w + "," + h;
        return createSearchResultAnnotation(params, text, pageId, uuid);
    }

    /**
     * Returns position of canvas. Uses the "pages" id attribute.
     * This method assumes that the solr response includes a "page" id attribute that is
     * delimited with a "." and that the integer corresponds to the
     * canvas identifier in the manifest. For METS/ALTO documents, the page
     * order can be derived from the METS file when loading the solr index.
     * @param element the pages element
     * @return canvas id
     */
    private String getCanvasId(JsonElement element) {
        JsonArray pages = element.getAsJsonArray();
        JsonObject page = pages.get(0).getAsJsonObject();
        String[] identArr = page.get("id").getAsString().split("\\.");
        // the canvas id.
        return "c" + identArr[1];
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
