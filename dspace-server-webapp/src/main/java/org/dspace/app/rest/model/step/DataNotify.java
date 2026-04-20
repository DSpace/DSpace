/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Java Bean to expose the COAR Notify Section during in progress submission.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class DataNotify implements SectionData {

    private Map<String, List<Integer>> patterns = new HashMap<>();

    public DataNotify() {

    }

    @JsonAnySetter
    public void add(String key, List<Integer> values) {
        patterns.put(key, values);
    }

    public DataNotify(Map<String, List<Integer>> patterns) {
        this.patterns = patterns;
    }

    @JsonIgnore
    public void setPatterns(Map<String, List<Integer>> patterns) {
        this.patterns = patterns;
    }

    @JsonAnyGetter
    public Map<String, List<Integer>> getPatterns() {
        return patterns;
    }
}
