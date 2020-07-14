/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import org.dspace.authority.AuthorityValue;

/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class ZDBTitleAuthority extends ZDBAuthority {
    private static final String ZDB_TITLE_SEARCH_FIELD = "title";
    private static final String JOURNALS_TITLE_FIELD = "crisjournals.journalsname";

    @Override
    protected String getZDBSearchField(String field) {
        return ZDB_TITLE_SEARCH_FIELD;
    }

    @Override
    protected String getZDBValue(String searchField, AuthorityValue val) {
        return val.getValue();
    }

    protected String getDefaultField() {
        return JOURNALS_TITLE_FIELD;
    }
}