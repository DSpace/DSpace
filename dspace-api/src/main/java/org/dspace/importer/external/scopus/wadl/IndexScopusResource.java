/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.scopus.wadl;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("index:scopus")
public interface IndexScopusResource {

    @GET
    @Produces("application/json")
    Response simple(@HeaderParam("Accept") @DefaultValue("application/json") String accept, @HeaderParam("Authorization") String authorization, @HeaderParam("X-ELS-APIKey") String x_ELS_APIKey, @HeaderParam("X-ELS-Authtoken") String x_ELS_Authtoken,
                    @HeaderParam("X-ELS-ReqId") String x_ELS_ReqId, @HeaderParam("X-ELS-ResourceVersion") String x_ELS_ResourceVersion, @QueryParam("access_token") String access_token, @QueryParam("apiKey") String apiKey,
                    @QueryParam("reqId") String reqId, @QueryParam("ver") String ver, @QueryParam("query") String query, @QueryParam("view") @DefaultValue("STANDARD") String view,
                    @QueryParam("field") String field, @QueryParam("date") String date, @QueryParam("start") String start, @QueryParam("count") String count,
                    @QueryParam("sort") String sort, @QueryParam("content") @DefaultValue("all") String content, @QueryParam("subj") String subj, @QueryParam("alias") @DefaultValue("true") String alias,
                    @QueryParam("facets") String facets);

}