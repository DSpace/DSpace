/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

import java.util.ArrayList;
import java.util.List;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.MetadataEntry;
import de.digitalcollections.iiif.model.sharedcanvas.Canvas;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import jakarta.validation.constraints.NotNull;

/**
 * This generator wraps the domain model for a single {@code Canvas}.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class CanvasGenerator implements IIIFResource {

    private final String identifier;
    private final List<MetadataEntry> metadata = new ArrayList<>();
    private final List<ImageContent> images = new ArrayList();
    private String label;
    private Integer height;
    private Integer width;
    private ImageContent thumbnail;

    /**
     * Constructor
     * @param identifier the canvas identifier
     */
    public CanvasGenerator(@NotNull String identifier) {
        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid canvas identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }

    /**
     * Adds a canvas label.
     * @param label
     */
    public CanvasGenerator setLabel(String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the canvas height. A canvas annotation with motivation {@code sc:painting} must have an pixel height.
     * @param height canvas height in pixels
     */
    public CanvasGenerator setHeight(int height) {
        this.height = height;
        return this;
    }

    /**
     * Sets the canvas width. A canvas annotation with motivation {@code sc:painting} must have a pixel width.
     * @param width canvas width in pixels
     */
    public CanvasGenerator setWidth(int width) {
        this.width = width;
        return this;
    }

    /**
     * Add to the list of image content resources for the canvas.
     * @param imageContent image content model
     */
    public CanvasGenerator addImage(Resource<ImageContent> imageContent) {
        images.add((ImageContent) imageContent);
        return this;
    }

    /**
     * Adds the thumbnail resource that will be assigned to the canvas.
     * @param thumbnail image content model
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
        metadata.add(metadataEntryGenerator.generateValue());
    }

    /**
     * Returns the canvas.
     * @return canvas model
     */
    @Override
    public Resource<Canvas> generateResource() {
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
