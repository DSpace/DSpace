/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;
import java.util.UUID;

/**
 * JSON body that starts a script process from previously staged uploads instead of a multipart request.
 * @param properties the script parameters, as for a multipart invocation
 * @param uploads identifiers of staged uploads to attach as input files, in order; may be empty
 */
public record StagedScriptInvocationRest(List<ParameterValueRest> properties, List<UUID> uploads) {
    /** Treat absent lists as empty. */
    public StagedScriptInvocationRest {
        properties = properties == null ? List.of() : List.copyOf(properties);
        uploads = uploads == null ? List.of() : List.copyOf(uploads);
    }
}
