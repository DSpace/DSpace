/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import java.util.ArrayList;
import java.util.List;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Facade for Presentation API version 2.1.1 Canvas model.
 *
 * Changes a Presentation API version 3.0 will likely require new fields for
 * this class to support multiple media types.
 */
@Component
@Scope("prototype")
public class CanvasGenerator implements IIIFResource {

    String identifier;
    String label;
    Integer height;
    Integer width;
    List<Resource<ImageContent>> imageContent = new ArrayList<>();
    Resource<ImageContent> thumbContent;

    /**
     * Canvases must be identified by a URI and it must be an HTTP(s) URI.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Every canvas must have a label to display.
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Every canvas must have an integer height.
     * @param height
     */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
     * Every canvas must have an integer width.
     * @param width
     */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
     * The ImageContent resource to be assigned to the canvas.
     * @param imageContent
     */
    public void addImage(Resource<ImageContent> imageContent) {
        this.imageContent.add(imageContent);
    }

    /**
     * The ImageContent resource to be assigned as the thumbnail the canvas.
     * @param thumbnail
     */
    public void addThumbnail(ImageContentGenerator thumbnail) {
        this.thumbContent = thumbnail.getResource();
    }

    @Override
    public Resource<Canvas> getResource() {
        /**
         * The Canvas resource typically includes image content.
         */
        Canvas canvas;
        if (identifier == null) {
            throw new RuntimeException("The Canvas resource requires an identifier.");
        }
        if (label != null) {
            canvas = new Canvas(identifier, label);
        } else {
            canvas = new Canvas(identifier);
        }
        if (imageContent.size() > 0) {
            if (height == null || width == null) {
                throw new RuntimeException("The Canvas resource requires both height and width dimensions.");
            }
            canvas.setWidth(width);
            canvas.setHeight(height);
            for (Resource<ImageContent> res : imageContent) {
                canvas.addImage((ImageContent) res);
            }
            if (thumbContent != null) {
                canvas.addThumbnail((ImageContent) thumbContent);
            }
        }
        // Reset properties after each use.
        identifier = null;
        imageContent.clear();
        label = null;

        return canvas;
    }
}
