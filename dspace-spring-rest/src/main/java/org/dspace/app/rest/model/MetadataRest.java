/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.springframework.hateoas.core.Relation;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Relation(collectionRelation = MetadataRest.RELATION)
public class MetadataRest {
    public static final String RELATION = "metadata";

    @JsonAnySetter
    private SortedMap<String, List<MetadataValueRest>> map = new TreeMap();

    @JsonAnyGetter
    public SortedMap<String, List<MetadataValueRest>> getMap() {
        return map;
    }
}
