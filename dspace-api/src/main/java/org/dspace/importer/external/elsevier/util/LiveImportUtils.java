/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.elsevier.util;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.http.*;
import org.apache.commons.lang.*;
import org.apache.log4j.*;
import org.dspace.importer.external.datamodel.*;
import org.dspace.importer.external.exception.*;
import org.dspace.importer.external.service.*;
import org.springframework.beans.factory.annotation.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 06/10/15
 * Time: 13:59
 */
public class LiveImportUtils {
    @Autowired
    private ImportService importService;
    private AbstractImportMetadataSourceService importSource;

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
        String query = "";

        for (String fieldName : fieldValues.keySet()) {
            if(query.length()>0) {
                query += (" AND ");
            }
            query += (fieldName);
            String field = fieldValues.get(fieldName);
            if (StringUtils.isNotBlank(fieldName)) {
                query += ("(" + field + ")");
            } else {
                query +=(field);
            }
            }

        try {
            query = URLEncoder.encode(query, "UTF-8");
            query = query.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage(), e);
        }


        return query;
    }

    public HashMap<String, String> getFieldValues(HttpServletRequest request, AbstractImportMetadataSourceService source) {
        importSource = source;
        HashMap<String,String> fieldValues = new HashMap<>();
        Map<String, String> importFields = source.getImportFields();
        for (String field : importFields.keySet()) {
            String value = request.getParameter(field);

            if(StringUtils.isNotBlank(value)){
                fieldValues.put(importFields.get(field), value);

            }
        }
        return fieldValues;
    }

    public String getUrl() {
        return importSource.getImportSource();
    }
}
