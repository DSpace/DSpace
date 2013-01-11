/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.statistics;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

/**
 * A neutral data object to hold data for statistics.
 *
 */
public class DataTermsFacet {
    private List<TermsFacet> terms;

    public DataTermsFacet() {
        terms = new ArrayList<TermsFacet>();
    }
    public void addTermFacet(TermsFacet termsFacet ) {
        terms.add(termsFacet);
    }

    /**
     * Render this data object into JSON format.
     *
     * An example of the output could be of the format:
     * [{"term":"247166","count":10},{"term":"247168","count":6}]
     * @return JSON-formatted data.
     */
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(terms);
    }



    
    public static class TermsFacet {
        private String term;
        private Integer count;
        
        public TermsFacet(String term, Integer count) {
            setTerm(term);
            setCount(count);
        }

        public String getTerm() {
            return term;
        }

        public void setTerm(String term) {
            this.term = term;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }


    }
}
