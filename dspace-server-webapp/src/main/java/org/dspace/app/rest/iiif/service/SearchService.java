/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dspace.app.rest.iiif.model.generator.AnnotationGenerator;
import org.dspace.app.rest.iiif.model.generator.CanvasGenerator;
import org.dspace.app.rest.iiif.model.generator.ContentAsTextGenerator;
import org.dspace.app.rest.iiif.model.generator.ManifestGenerator;
import org.dspace.app.rest.iiif.model.generator.SearchResultGenerator;
import org.dspace.app.rest.iiif.service.util.IIIFUtils;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Implements IIIF Search API queries and responses.
 */
@Component
@RequestScope
public class SearchService extends AbstractResourceService {

    private static final Logger log = Logger.getLogger(SearchService.class);

    @Autowired
    IIIFUtils utils;

    @Autowired
    ContentAsTextGenerator contentAsText;

    @Autowired
    CanvasGenerator canvas;

    @Autowired
    AnnotationGenerator annotation;

    @Autowired
    ManifestGenerator manifest;

    @Autowired
    SearchResultGenerator searchResult;

    public SearchService(ConfigurationService configurationService) {
        setConfiguration(configurationService);
    }

    /**
     * Executes a search that is scoped to the manifest.
     *
     * @param uuid the IIIF manifest uuid
     * @param query the solr query
     * @return IIIF json
     */
    public String searchWithinManifest(UUID uuid, String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String json = getSolrSearchResponse(createSearchUrl(encodedQuery, getManifestId(uuid)));
        return getAnnotationList(json, uuid, encodedQuery);
    }

    /**
     * Executes the Search API solr query.
     * @param url solr query url
     * @return json query response
     */
    private String getSolrSearchResponse(URL url) {
        InputStream jsonStream;
        String json;
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            jsonStream = connection.getInputStream();
            json = IOUtils.toString(jsonStream, StandardCharsets.UTF_8);
        } catch (IOException e)  {
            throw new RuntimeException("Unable to query solr at: " + url, e);
        }
        return json;
    }

    /**
     * Constructs a solr search URL.
     *
     * @param encodedQuery the search terms
     * @param manifestId the id of the manifest in which to search
     * @return solr query
     */
    private URL createSearchUrl(String encodedQuery, String manifestId) {

        String fullQuery = SEARCH_URL + "/select?" +
                "q=ocr_text:\"" + encodedQuery +
                "\"%20AND%20manifest_url:\"" + manifestId + "\"" +
                "&hl=true" +
                "&hl.ocr.fl=ocr_text" +
                "&hl.ocr.contextBlock=line" +
                "&hl.ocr.contextSize=2" +
                "&hl.snippets=10" +
                // "&hl.ocr.limitBlock=page" +
                "&hl.ocr.absoluteHighlights=true";

        log.debug(fullQuery);

        try {
            URL url = new URL(fullQuery);
            return url;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed query URL", e);
        }
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
     * The convention convention for Alto IDs must be followed when indexing ALTO files
     * into the word_highlighting solr index. If it is not, search responses will not
     * match canvases.
     *
     * @param json solr search result
     * @param uuid DSpace Item uuid
     * @param encodedQuery the solr query
     * @return a search response in JSON
     */
    private String getAnnotationList(String json, UUID uuid, String encodedQuery) {
        searchResult.setIdentifier(getManifestId(uuid) + "/search?q=" + encodedQuery);
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        JsonObject body = gson.fromJson(json, JsonObject.class);
        // outer ocr highlight element
        JsonObject highs = body.getAsJsonObject("ocrHighlighting");
        // highlight entries
        for (Map.Entry<String, JsonElement> ocrIds: highs.entrySet()) {
            // ocr_text
            JsonObject ocrObj = ocrIds.getValue().getAsJsonObject().getAsJsonObject("ocr_text");
            // snippets array
            if (ocrObj != null) {
                for (JsonElement snippetArray : ocrObj.getAsJsonObject().get("snippets").getAsJsonArray()) {
                    for (JsonElement highlights : snippetArray.getAsJsonObject().getAsJsonArray("highlights")) {
                        for (JsonElement highlight : highlights.getAsJsonArray()) {
                            JsonObject hcoords = highlight.getAsJsonObject();
                            String text = (hcoords.get("text").getAsString());
                            String pageId = getCanvasId((hcoords.get("page").getAsString()));
                            Integer ulx = hcoords.get("ulx").getAsInt();
                            Integer uly = hcoords.get("uly").getAsInt();
                            Integer lrx = hcoords.get("lrx").getAsInt();
                            Integer lry = hcoords.get("lry").getAsInt();
                            String w = Integer.toString(lrx - ulx);
                            String h = Integer.toString(lry - uly);
                            String params = ulx + "," + uly + "," + w + "," + h;
                            AnnotationGenerator annot = createSearchResultAnnotation(params, text, pageId, uuid);
                            searchResult.addResource(annot);
                        }
                    }
                }
            }
        }
        return utils.asJson(searchResult.getResource());
    }

    private String getCanvasId(String altoId) {
        String[] identArr = altoId.split("\\.");
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
        annotation.setIdentifier(IIIF_ENDPOINT + uuid + "/annot/" + pageId + "-"
                + params);
        canvas.setIdentifier(IIIF_ENDPOINT + uuid + "/canvas/" + pageId + "#xywh="
                + params);
        annotation.setOnCanvas(canvas);
        contentAsText.setText(text);
        annotation.setResource(contentAsText);
        annotation.setMotivation(AnnotationGenerator.PAINTING);
        List<ManifestGenerator> withinList = new ArrayList<>();
        manifest.setIdentifier(getManifestId(uuid));
        manifest.setLabel("Search within manifest.");
        withinList.add(manifest);
        annotation.setWithin(withinList);
        return annotation;
    }

}
