/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scidir.util;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.service.ImportService;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 06/10/15
 * Time: 13:59
 */
public class LiveImportUtils {
    @Autowired
    private ImportService importService;

    private HashMap<String, String> liveImportFields;
    private String url;

    Logger log = Logger.getLogger(LiveImportUtils.class);

    public Collection<ImportRecord> getRecords(HashMap<String, String> fieldValues, int start, int rpp){
        Collection<ImportRecord> records = new ArrayList<>();

        try {
            records = importService.getRecords(getUrl(), getQuery(fieldValues), start, rpp);
        } catch (MetadataSourceException e) {
            log.error(e.getMessage(),e);
        }

        return records;
    }

    public int getNbRecords(HashMap<String, String> fieldValues){
        int total = 0;
        try {
            total = importService.getNbRecords(getUrl(), getQuery(fieldValues));
        } catch (MetadataSourceException e) {
            log.error(e.getMessage(),e);
        }

        return total;
    }

    public String getQuery(HashMap<String, String> fieldValues) {
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldValues.keySet()) {
            if(query.length()>0) {
                query.append(" AND ");
            }

            query.append(fieldName + "(" + fieldValues.get(fieldName) + ")");
        }

        return query.toString();
    }

    public HashMap<String, String> getFieldValues(HttpServletRequest request){
        HashMap<String,String> fieldValues = new HashMap<>();
        for (String field : getLiveImportFields().keySet()) {
            String value = request.getParameter(field);

            if(StringUtils.isNotBlank(value)){
                fieldValues.put(getLiveImportFields().get(field), value);

            }
        }
        return fieldValues;
    }

    public String getUrl() {
        if(url==null){
            url = ConfigurationManager.getProperty("elsevier-sciencedirect", "api.scidir.url");
        }
        return url;
    }

    public HashMap<String, String> getLiveImportFields() {
        if(liveImportFields==null){
            liveImportFields = new DSpace().getServiceManager().getServiceByName("LiveImportFields", HashMap.class);
        }

        return liveImportFields;
    }
}
