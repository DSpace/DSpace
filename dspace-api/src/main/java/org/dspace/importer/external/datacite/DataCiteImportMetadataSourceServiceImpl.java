/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.datacite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.el.MethodNotFoundException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.DoiCheck;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implements a data source for querying Datacite
 * Mainly copied from CrossRefImportMetadataSourceServiceImpl.
 *
 * optional Affiliation informations are not part of the API request.
 * https://support.datacite.org/docs/can-i-see-more-detailed-affiliation-information-in-the-rest-api
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 *
 */
public class DataCiteImportMetadataSourceServiceImpl
        extends AbstractImportMetadataSourceService<String> implements QuerySource {
    private final static Logger log = LogManager.getLogger();

    @Autowired
    private LiveImportClient liveImportClient;

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public String getImportSource() {
        return "datacite";
    }

    @Override
    public void init() throws Exception {
    }

    @Override
    public ImportRecord getRecord(String recordId) throws MetadataSourceException {
        Collection<ImportRecord> records = getRecords(recordId, 0, 1);
        if (records.size() == 0) {
            return null;
        }
        return records.stream().findFirst().get();
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        Collection<ImportRecord> records = getRecords(query, 0, -1);
        return records == null ? 0 : records.size();
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        String id = getID(query.toString());
        return getRecordsCount(StringUtils.isBlank(id) ? query.toString() : id);
    }


    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        List<ImportRecord> records = new ArrayList<>();
        String id = getID(query);
        Map<String, Map<String, String>> params = new HashMap<>();
        Map<String, String> uriParameters = new HashMap<>();
        params.put("uriParameters", uriParameters);
        if (StringUtils.isBlank(id)) {
            id = query;
        }
        uriParameters.put("query", id);
        int timeoutMs = configurationService.getIntProperty("datacite.timeout", 180000);
        String url = configurationService.getProperty("datacite.url", "https://api.datacite.org/dois/");
        String responseString = liveImportClient.executeHttpGetRequest(timeoutMs, url, params);
        JsonNode jsonNode = convertStringJsonToJsonNode(responseString);
        if (jsonNode == null) {
            log.warn("DataCite returned invalid JSON");
            return records;
        }
        JsonNode dataNode = jsonNode.at("/data");
        if (dataNode.isArray()) {
            Iterator<JsonNode> iterator = dataNode.iterator();
            while (iterator.hasNext()) {
                JsonNode singleDoiNode = iterator.next();
                String json = singleDoiNode.at("/attributes").toString();
                records.add(transformSourceRecords(json));
            }
        } else {
            String json = dataNode.at("/attributes").toString();
            records.add(transformSourceRecords(json));
        }

        return records;
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return null;
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        String id = getID(query.toString());
        return getRecords(StringUtils.isBlank(id) ? query.toString() : id, 0, -1);
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        String id = getID(query.toString());
        return getRecord(StringUtils.isBlank(id) ? query.toString() : id);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        String id = getID(query.toString());
        return getRecords(StringUtils.isBlank(id) ? query.toString() : id, 0, -1);
    }


    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for DataCite");
    }

    public String getID(String query) {
        if (DoiCheck.isDoi(query)) {
            return query;
        }
        // Workaround for encoded slashes.
        if (query.contains("%252F")) {
            query = query.replace("%252F", "/");
        }
        if (DoiCheck.isDoi(query)) {
            return query;
        }
        return StringUtils.EMPTY;
    }
}
