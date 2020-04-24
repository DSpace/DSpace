/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;
import org.apache.solr.common.SolrDocument;
import org.dspace.submit.util.SubmissionLookupPublication;
import org.json.JSONArray;
import org.json.JSONObject;

public class ADSUtils {
    private ADSUtils() {
    }

    public static Record convertADSDocumentToRecord(SolrDocument doc) {
        MutableRecord record = new SubmissionLookupPublication("");
        for (String field : doc.getFieldNames()) {
            Collection<Object> fieldValues = doc.getFieldValues(field);
            List<Value> vals = new LinkedList<Value>();
            for (Object obj : fieldValues) {
                vals.add(new StringValue(obj.toString()));
            }
            record.addField(field, vals);
        }
        return record;
    }

    public static Record convertADSDocumentToRecord(JSONObject doc) {
        MutableRecord record = new SubmissionLookupPublication("");

        for (String field : JSONObject.getNames(doc)) {
            Object obj = doc.get(field);
            if (obj instanceof JSONObject) {
                record.addValue(field, new StringValue(doc.getString(field)));
            } else if (obj instanceof JSONArray) {
                JSONArray ar = (JSONArray) obj;
                List<Value> vals = new LinkedList<Value>();
                for (int x = 0; x < ar.length(); x++) {
                    vals.add(new StringValue(ar.get(x).toString()));
                }
                record.addField(field, vals);
            } else {
                record.addValue(field, new StringValue(obj.toString()));
            }
        }

        Object typeObj = doc.get("doctype");
        record.addValue("adstype", new StringValue(typeObj.toString()));
        return record;
    }
}
