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

public class GetByVuFindIdCallable implements Callable<String> {

    private String id;

    private final WebTarget webTarget;

    private String fields;

    public GetByVuFindIdCallable(String id, WebTarget webTarget, String fields) {
        this.id = id;
        this.webTarget = webTarget;
        if (fields != null && fields.length() > 0) {
            this.fields = fields;
        } else {
            this.fields = null;
        }
    }

    @Override
    public String call() throws Exception {
        WebTarget localTarget = webTarget.queryParam("id", id);
        if (fields != null && !fields.isEmpty()) {
            for (String field : fields.split(",")) {
                localTarget = localTarget.queryParam("field[]", field);
            }
        }
        localTarget = localTarget.queryParam("prettyPrint", false);
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
