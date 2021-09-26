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
import de.digitalcollections.iiif.model.MetadataEntry;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;

/**
 * Presentation API version 2.1.1 Canvas model.
 *
 * Changes a Presentation API version 3.0 will likely require updates for
 * multiple media types, etc.
 */
public class CanvasGenerator implements IIIFResource {

    private String identifier;
    private String label;
    private Integer height;
    private Integer width;
    private List<ImageContent> images = new ArrayList();
    private ImageContent thumbnail;

    private final List<MetadataEntry> metadata = new ArrayList<>();

    public  CanvasGenerator setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Every canvas must have a label to display.
     * @param label
     */
    public CanvasGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Every canvas must have an integer height.
     * @param height
     */
    public CanvasGenerator setHeight(int height) {
        this.height = height;
        return this;
    }

    /**
     * Every canvas must have an integer width.
     * @param width
     */
    public CanvasGenerator setWidth(int width) {
        this.width = width;
        return this;
    }

    /**
     * Add to ImageContent resources that will be assigned to the canvas.
     * @param imageContent
     */
    public CanvasGenerator addImage(Resource<ImageContent> imageContent) {
        images.add((ImageContent) imageContent);
        return this;
    }

    /**
     * The Thumbnail resource that will be assigned to the canvas.
     * @param thumbnail
     */
    public CanvasGenerator addThumbnail(Resource<ImageContent> thumbnail) {
        this.thumbnail = (ImageContent) thumbnail;
        return this;
    }

    /**
     * Adds single metadata field to Manifest.
     * @param field property field
     * @param value property value
     */
    public void addMetadata(String field, String value, String... rest) {
        MetadataEntryGenerator metadataEntryGenerator = new MetadataEntryGenerator();
        metadataEntryGenerator.setField(field);
        metadataEntryGenerator.setValue(value, rest);
        metadata.add(metadataEntryGenerator.getValue());
    }

    /**
     * Returns the canvas.
     * @return canvas model
     */
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
        if (images.size() > 0) {
            if (height == null || width == null) {
                throw new RuntimeException("The Canvas resource requires both height and width dimensions.");
            }
            canvas.setWidth(width);
            canvas.setHeight(height);
            for (ImageContent res : images) {
                canvas.addImage(res);
            }
            if (thumbnail != null) {
                canvas.addThumbnail(thumbnail);
            }
        }
        if (metadata.size() > 0) {
            for (MetadataEntry meta : metadata) {
                canvas.addMetadata(meta);
            }
        }
        return canvas;
    }

}
