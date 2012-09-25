/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.usage;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Extends the standard usage event to contain search information
 * search information includes the query(s) used & the scope
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class UsageSearchEvent extends UsageEvent{

    private List<String> queries;
    private DSpaceObject scope;

    /** Optional search parameters **/
    private int rpp;
    private String sortBy;
    private String sortOrder;
    private int page;


    public UsageSearchEvent(Action action, HttpServletRequest request, Context context, DSpaceObject object, List<String> queries, DSpaceObject scope) {
        super(action, request, context, object);

        this.queries = queries;
        this.scope = scope;
        this.rpp = -1;
        this.sortBy = null;
        this.sortOrder = null;
        this.page = -1;
    }

    public List<String> getQueries() {
        return queries;
    }

    public DSpaceObject getScope() {
        return scope;
    }

    public int getRpp() {
        return rpp;
    }

    public void setRpp(int rpp) {
        this.rpp = rpp;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }
}
