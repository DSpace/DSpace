/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.vufind.callable;

import java.util.concurrent.Callable;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.dspace.importer.external.datamodel.Query;

public class CountByQueryCallable implements Callable<Integer> {

    private Query query;

    private WebTarget webTarget;

    public CountByQueryCallable(String queryString, WebTarget webTarget) {
        query = new Query();
        query.addParameter("query", queryString);
        this.webTarget = webTarget;
    }

    public CountByQueryCallable(Query query, WebTarget webTarget) {
        this.query = query;
        this.webTarget = webTarget;
    }

    @Override
    public Integer call() throws Exception {
        Integer start = 0;
        Integer count = 1;
        int page = start / count + 1;
        WebTarget localTarget = webTarget.queryParam("type", "AllField");
        localTarget = localTarget.queryParam("page", page);
        localTarget = localTarget.queryParam("limit", count);
        localTarget = localTarget.queryParam("prettyPrint", true);
        localTarget = localTarget.queryParam("lookfor", query.getParameterAsClass("query", String.class));
        Invocation.Builder invocationBuilder = localTarget.request();
        Response response = invocationBuilder.get();
        String responseString = response.readEntity(String.class);
        ReadContext ctx = JsonPath.parse(responseString);
        return ctx.read("$.resultCount");
    }
}
