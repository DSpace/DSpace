/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Presentation API version 2.1.1 Canvas model.
 *
 * Changes a Presentation API version 3.0 will likely require new fields for
 * this class to support multiple media types.
 */
@Component
@Scope("prototype")
public class CanvasGenerator implements IIIFResource {

    Canvas canvas;

    public CanvasGenerator(String identifier) {
        this.canvas = new Canvas(identifier);
    }

    /**
     * Every canvas must have a label to display.
     * @param label
     */
    public void setLabel(String label) {
        canvas.setLabel(new PropertyValue(label));
    }

    /**
     * Every canvas must have an integer height.
     * @param height
     */
    public void setHeight(int height) {
        canvas.setHeight(height);
    }

    /**
     * Every canvas must have an integer width.
     * @param width
     */
    public void setWidth(int width) {
        canvas.setWidth(width);
    }

    /**
     * Add to ImageContent resources that will be assigned to the canvas.
     * @param imageContent
     */
    public void addImage(Resource<ImageContent> imageContent) {
        canvas.addImage((ImageContent) imageContent);
    }

    /**
     * The Thumbnail resource that will be assigned to the canvas.
     * @param thumbnail
     */
    public void addThumbnail(ImageContentGenerator thumbnail) {
        canvas.addThumbnail((ImageContent) thumbnail.getResource());
    }

    /**
     * Returns the canvas.
     * @return canvas model
     */
    @Override
    public Resource<Canvas> getResource() {
//        if (canvas.getHeight() == null || canvas.getWidth() == null) {
//            throw new RuntimeException("The Canvas resource requires both height and width dimensions.");
//        }
        return canvas;
    }

}
