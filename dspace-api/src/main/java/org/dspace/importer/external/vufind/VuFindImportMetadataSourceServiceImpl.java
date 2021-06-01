/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.vufind;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import javax.el.MethodNotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import com.google.gson.Gson;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.service.AbstractImportMetadataSourceService;
import org.dspace.importer.external.service.components.QuerySource;
import org.dspace.importer.external.vufind.callable.CountByQueryCallable;
import org.dspace.importer.external.vufind.callable.FindMatchingRecordsCallable;
import org.dspace.importer.external.vufind.callable.GetByVuFindIdCallable;
import org.dspace.importer.external.vufind.callable.SearchByQueryCallable;

public class VuFindImportMetadataSourceServiceImpl extends AbstractImportMetadataSourceService<String>
    implements QuerySource {

    private static final Logger log = Logger.getLogger(VuFindImportMetadataSourceServiceImpl.class);

    private String fields;

    private final WebTarget searchWebTarget;
    private final WebTarget getWebTarget;

    public VuFindImportMetadataSourceServiceImpl(String vufindSearchAddress, String vufindGetAddress, String fields) {
        Client client = ClientBuilder.newClient();
        searchWebTarget = client.target(vufindSearchAddress);
        getWebTarget = client.target(vufindGetAddress);
        this.fields = fields;
    }

    @Override
    public String getImportSource() {
        return "VuFind";
    }

    @Override
    public ImportRecord getRecord(String id) throws MetadataSourceException {
        String records = retry(new GetByVuFindIdCallable(id, getWebTarget, fields));
        List<ImportRecord> importRecords = extractMetadataFromRecordList(records);
        return importRecords != null && !importRecords.isEmpty() ? importRecords.get(0) : null;
    }

    @Override
    public int getRecordsCount(String query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query, searchWebTarget));
    }

    @Override
    public int getRecordsCount(Query query) throws MetadataSourceException {
        return retry(new CountByQueryCallable(query, searchWebTarget));
    }

    @Override
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, count, start, searchWebTarget, fields));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, searchWebTarget, fields));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public ImportRecord getRecord(Query query) throws MetadataSourceException {
        String records = retry(new SearchByQueryCallable(query, searchWebTarget, fields));
        List<ImportRecord> importRecords = extractMetadataFromRecordList(records);
        return importRecords != null && !importRecords.isEmpty() ? importRecords.get(0) : null;
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException {
        String records = retry(new FindMatchingRecordsCallable(query, getWebTarget));
        return extractMetadataFromRecordList(records);
    }

    @Override
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException {
        throw new MethodNotFoundException("This method is not implemented for VuFind");
    }

    @Override
    public void init() throws Exception {

    }

    private List<ImportRecord> extractMetadataFromRecordList(String records) {
        List<ImportRecord> recordsResult = new ArrayList<>();
        ReadContext ctx = JsonPath.parse(records);
        try {
            Object o = ctx.read("$.records[*]");
            if (o.getClass().isAssignableFrom(JSONArray.class)) {
                JSONArray array = (JSONArray)o;
                int size = array.size();
                for (int index = 0; index < size; index++) {
                    Gson gson = new Gson();
                    String innerJson = gson.toJson(array.get(index), LinkedHashMap.class);
                    recordsResult.add(transformSourceRecords(innerJson));
                }
            } else {
                recordsResult.add(transformSourceRecords(o.toString()));
            }
        } catch (Exception e) {
            log.error("Error reading data from VuFind " + e.getMessage(), e);
        }
        return recordsResult;
    }

}