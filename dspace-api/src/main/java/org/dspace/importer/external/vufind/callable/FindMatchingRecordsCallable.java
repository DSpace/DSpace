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

import org.apache.http.HttpException;
import org.dspace.importer.external.datamodel.Query;

public class FindMatchingRecordsCallable implements Callable<String> {

    private Query query;

    private WebTarget webTarget;

    private String fields;

    public FindMatchingRecordsCallable(String queryString, Integer maxResult, Integer start, WebTarget webTarget,
            String fields) {
        query = new Query();
        query.addParameter("query", queryString);
        query.addParameter("count", maxResult);
        query.addParameter("start", start);
        this.webTarget = webTarget;
        if (fields != null && fields.length() > 0) {
            this.fields = fields;
        } else {
            this.fields = null;
        }
    }

    public FindMatchingRecordsCallable(Query query, WebTarget webTarget) {
        this.query = query;
        this.webTarget = webTarget;
    }

    @Override
    public String call() throws Exception {
        String author = query.getParameterAsClass("author", String.class);
        String title = query.getParameterAsClass("title", String.class);
        Integer start = query.getParameterAsClass("start", Integer.class);
        Integer count = query.getParameterAsClass("count", Integer.class);
        int page = count != 0 ? start / count : 0;
        WebTarget localTarget = webTarget.queryParam("type", "AllField");
        //pagination is 1 based (first page: start = 0, count = 20 -> page = 0 -> +1 = 1)
        localTarget = localTarget.queryParam("page", page + 1);
        localTarget = localTarget.queryParam("limit", count);
        localTarget = localTarget.queryParam("prettyPrint", true);
        if (fields != null && !fields.isEmpty()) {
            for (String field : fields.split(",")) {
                localTarget = localTarget.queryParam("field[]", field);
            }
        }
        String filter = null;
        if (author != null && !author.isEmpty()) {
            filter = "author:" + author;
        }
        if (title != null && !title.isEmpty()) {
            if (filter != null) {
                filter = filter + " AND title:" + title;
            } else {
                filter = "title:" + title;
            }
        }
        localTarget = localTarget.queryParam("lookfor", filter);
        Invocation.Builder invocationBuilder = localTarget.request();
        Response response = invocationBuilder.get();
        if (response.getStatus() == 200) {
            return response.readEntity(String.class);
        } else {
            //this exception is manager by the caller
            throw new HttpException();
        }

    }

}
