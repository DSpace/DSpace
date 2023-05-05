/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.exception.RESTBitstreamNotFoundException;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * A PATCH operation for removing bitstreams in bulk from the repository.
 *
 * Example: <code>
 * curl -X PATCH http://${dspace.server.url}/api/core/bitstreams -H "Content-Type: application/json"
 * -d '[
 *       {"op": "remove", "path": "/bitstreams/${bitstream1UUID}"},
 *       {"op": "remove", "path": "/bitstreams/${bitstream2UUID}"},
 *       {"op": "remove", "path": "/bitstreams/${bitstream3UUID}"}
 *     ]'
 * </code>
 *
 * @author Jens Vannerum (jens.vannerum@atmire.com)
 */
@Component
public class BitstreamRemoveOperation extends PatchOperation<Bitstream> {
    @Autowired
    BitstreamService bitstreamService;
    public static final String OPERATION_PATH_BITSTREAM_REMOVE = "/bitstreams/";

    @Override
    public Bitstream perform(Context context, Bitstream resource, Operation operation) throws SQLException {
        String bitstreamIDtoDelete = operation.getPath().replace(OPERATION_PATH_BITSTREAM_REMOVE, "");
        Bitstream bitstreamToDelete = bitstreamService.find(context, UUID.fromString(bitstreamIDtoDelete));
        if (bitstreamToDelete == null) {
            throw new RESTBitstreamNotFoundException(bitstreamIDtoDelete);
        }

        try {
            bitstreamService.delete(context, bitstreamToDelete);
            bitstreamService.update(context, bitstreamToDelete);
        } catch (AuthorizeException | IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean supports(Object objectToMatch, Operation operation) {
        return objectToMatch == null && operation.getOp().trim().equalsIgnoreCase(OPERATION_REMOVE) &&
            operation.getPath().trim().startsWith(OPERATION_PATH_BITSTREAM_REMOVE);
    }
}
