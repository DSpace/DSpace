/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.util.ArrayList;
import java.util.Collection;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import net.minidev.json.JSONArray;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

public class CrossRefAuthorMetadataProcessor implements JsonPathMetadataProcessor {

    private String givenNameJsonPath;
    private String familyNameJsonPath;

    public void setGivenNameJsonPath(String givenNameJsonPath) {
        this.givenNameJsonPath = givenNameJsonPath;
    }

    public void setFamilyNameJsonPath(String familyNameJsonPath) {
        this.familyNameJsonPath = familyNameJsonPath;
    }

    @Override
    public Collection<String> processMetadata(String json) {
        ReadContext ctx = JsonPath.parse(json);
        JSONArray givenNames = ctx.read(givenNameJsonPath);
        JSONArray familyNames = ctx.read(familyNameJsonPath);
        Collection<String> values = new ArrayList<>();
        for (int i = 0; i < givenNames.size(); i++) {
            String name = givenNames.get(i).toString();
            values.add(name + " " + familyNames.get(i).toString());
        }
        return values;
    }

}
