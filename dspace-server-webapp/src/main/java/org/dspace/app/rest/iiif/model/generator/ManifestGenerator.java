/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model.generator;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.digitalcollections.iiif.model.ImageContent;
import de.digitalcollections.iiif.model.MetadataEntry;
import de.digitalcollections.iiif.model.OtherContent;
import de.digitalcollections.iiif.model.PropertyValue;
import de.digitalcollections.iiif.model.enums.ViewingHint;
import de.digitalcollections.iiif.model.search.ContentSearchService;
import de.digitalcollections.iiif.model.sharedcanvas.Manifest;
import de.digitalcollections.iiif.model.sharedcanvas.Range;
import de.digitalcollections.iiif.model.sharedcanvas.Resource;
import de.digitalcollections.iiif.model.sharedcanvas.Sequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Facade for the IIIF Presentation API version 2.2.1 domain model.
 *
 * The Manifest is an overall description of the structure and properties of the digital representation
 * of an object. It carries information needed for the viewer to present the digitized content to the user,
 * such as a title and other descriptive information about the object or the intellectual work that
 * it conveys. Each manifest describes how to present a single object such as a book, a photograph,
 * or a statue.
 */
@Component
@RequestScope
public class ManifestGenerator implements IIIFResource {

    private String identifier;
    private String label;
    private OtherContent seeAlso;
    // Becomes the "items" element in IIIF version 3.0
    private CanvasItemsGenerator sequence;
    // Renamed to "homepage" in IIIF version 3.0
    private OtherContent related;
    // Renamed to "behavior" with IIIF version 3.0
    private ViewingHint viewingHint;
    private Resource<ImageContent> thumbnail;
    private ContentSearchService searchService;
    private PropertyValue description;
    private final List<MetadataEntry> metadata = new ArrayList<>();
    // Renamed to "rights" with IIIF version 3.0
    private final List<URI> license = new ArrayList<>();
    private List<Range> ranges = new ArrayList<>();

    @Autowired
    PropertyValueGenerator propertyValue;

    @Autowired
    MetadataEntryGenerator metadataEntryGenerator;

    @Autowired
    BehaviorGenerator behaviorGenerator;

    /**
     * Sets the mandatory Manifest ID.
     * @param identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Sets the manditory Manifest label.
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Sets the behavior. A hint to the client as to the most appropriate method of displaying the resource
     * In IIIF Presentation API version 3.0 semantics this is the "behavior"
     * @param viewingHint
     */
    public void addViewingHint(String viewingHint) {
        behaviorGenerator.setType(viewingHint);
        this.viewingHint = behaviorGenerator.getValue();
    }

    /**
     * Use to add single mandatory sequence to Manifest. In IIIF Presentation API 3.0 "sequence"
     * is replaced by "items"
     * @param sequence
     */
    public void addSequence(CanvasItemsGenerator sequence) {
        this.sequence = sequence;
    }

    /**
     * Add otional seeAlso element to Manifest.
     * @param seeAlso
     */
    public void addSeeAlso(org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator seeAlso) {
        this.seeAlso = (OtherContent) seeAlso.getResource();
    }

    /**
     * Add optional thumbnail image to manifest.
     * @param thumbnail
     */
    public void addThumbnail(ImageContentGenerator thumbnail) {
        this.thumbnail = thumbnail.getResource();
    }

    /**
     * Add optional related element to Manifest.
     * @param related
     */
    public void addRelated(org.dspace.app.rest.iiif.model.generator.ExternalLinksGenerator related) {
        this.related = (OtherContent) related.getResource();
    }

    /**
     * Adds optional search service to Manifest.
     * @param searchService
     */
    public void addService(org.dspace.app.rest.iiif.model.generator.ContentSearchGenerator searchService) {
        this.searchService = (ContentSearchService) searchService.getService();
    }

    /**
     * Adds single metadata field to Manifest.
     * @param field
     * @param value
     */
    public void addMetadata(String field, String value) {
        metadataEntryGenerator.setField(field);
        metadataEntryGenerator.setValue(value);
        metadata.add(metadataEntryGenerator.getValue());
    }

    /**
     * Adds optional license to Manifest.
     * @param license
     */
    public void addLicense(String license) {
        this.license.add(URI.create(license));
    }

    /**
     * Adds optional description to Manifest.
     * @param field
     * @param value
     */
    public void addDescription(String field, String value) {
        propertyValue.setPropertyValue(field, value);
        description = propertyValue.getValue();
    }

    /**
     * Adds optional Range to the manifest's structures element.
     * @param range
     */
    public void addRange(RangeGenerator range) {
        ranges.add((Range) range.getResource());
    }

    @Override
    public Resource<Manifest> getResource() {
        if (identifier == null) {
            throw new RuntimeException("The Manifest resource requires an identifier.");
        }
        Manifest manifest;
        if (label != null) {
            manifest = new Manifest(identifier, label);
        } else {
            manifest = new Manifest(identifier);
        }
        if (sequence != null) {
            manifest.addSequence((Sequence) sequence.getResource());
        }
        if (ranges.size() > 0) {
            manifest.setRanges(ranges);
        }
        if (metadata.size() > 0) {
            for (MetadataEntry meta : metadata) {
                manifest.addMetadata(meta);
            }
        }
        if (seeAlso != null) {
            manifest.addSeeAlso(seeAlso);
        }
        if (related != null) {
            manifest.addRelated(related);
        }
        if (searchService != null) {
            manifest.addService(searchService);
        }
        if (license.size() > 0) {
            manifest.setLicenses(license);
        }
        if (description != null) {
            manifest.setDescription(description);
        }
        if (thumbnail != null) {
            manifest.addThumbnail((ImageContent) thumbnail);
        }
        if (viewingHint != null) {
            manifest.addViewingHint(viewingHint);
        }
        return manifest;
    }

}
