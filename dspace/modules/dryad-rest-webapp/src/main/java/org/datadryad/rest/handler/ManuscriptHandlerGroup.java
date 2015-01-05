/*
 */
package org.datadryad.rest.handler;

import org.datadryad.rest.models.Manuscript;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class ManuscriptHandlerGroup extends AbstractHandlerGroup<Manuscript> {
    public ManuscriptHandlerGroup() {
        // Log incoming create/update/delete requests
        addHandler(new LoggingHandler<Manuscript>());
        // Save manuscripts as XML files for submission system
        addHandler(new ManuscriptXMLConverterHandler());
        // If publication date present, update package metadata
        addHandler(new ManuscriptMetadataSynchronizationHandler());
        // If review status changed, process the item
        addHandler(new ManuscriptReviewStatusChangeHandler());
    }

}
