/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.openannotation.ContentAsText;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.stereotype.Component;

@Component
public class ContentAsTextGenerator implements IIIFResource {

    private String text;

    public void setText(String text) {
        this.text = text;
    }
    @Override
    public Resource<ContentAsText> getResource() {
        if (text == null) {
            throw new RuntimeException("ContextAsText requires text input.");
        }
        return new ContentAsText(text);
    }
}
