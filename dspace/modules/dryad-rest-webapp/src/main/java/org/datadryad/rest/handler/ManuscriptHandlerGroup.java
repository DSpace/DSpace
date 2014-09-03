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
        // Add some concrete handlers 
        addHandler(new ManuscriptXMLConverterHandler());
    }

}
