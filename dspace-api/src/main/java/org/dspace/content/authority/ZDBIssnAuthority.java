/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Collections;
import java.util.List;

import org.dspace.authority.AuthorityValue;

/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBIssnAuthority extends ZDBAuthority {
    private static final String ZDB_ISSN_SEARCH_FIELD = "iss";
    private static final String ZDB_ISSN_FIELD = "journalIssn";
    private static final String JOURNALS_ISSN_FIELD = "crisjournals.journalsissn";

    @Override
    protected String getZDBSearchField(String field) {
        return ZDB_ISSN_SEARCH_FIELD;
    }

    @Override
    protected String getZDBValue(String searchField, AuthorityValue val) {
        List<String> issns = val.getOtherMetadata().get(ZDB_ISSN_FIELD);
        if (issns != null && !issns.isEmpty()) {
            Collections.sort(issns);
            return issns.get(0);
        }
        // default get title
        return val.getValue();
    }

    protected String getDefaultField() {
        return JOURNALS_ISSN_FIELD;
    }
}