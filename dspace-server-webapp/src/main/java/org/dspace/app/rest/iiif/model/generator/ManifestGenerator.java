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
 * The Manifest is an overall description of the structure and properties of the digital representation
 * of an object. It carries information needed for the viewer to present the digitized content to the user,
 * such as a title and other descriptive information about the object or the intellectual work that
 * it conveys. Each manifest describes how to present a single object such as a book, a photograph,
 * or a statue.
 * 
 * Please note that this is a request scoped bean. This mean that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.
 */
@Component
@RequestScope
public class ManifestGenerator implements IIIFResource {

    private String identifier;
    private String label;
    private PropertyValue description;
    private ImageContent logo;
    private ViewingHint viewingHint;
    private Sequence sequence;
    private OtherContent seeAlso;
    private OtherContent related;
    private ImageContent thumbnail;
    private ContentSearchService searchService;
    private final List<URI> license = new ArrayList<>();
    private final List<MetadataEntry> metadata = new ArrayList<>();
    private final List<RangeGenerator> ranges = new ArrayList<>();

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
     * Sets the Manifest label.
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public void addLogo(ImageContentGenerator logo) {
        this.logo = (ImageContent) logo.getResource();
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
     * @param sequence canvas list model (sequence)
     */
    public void addSequence(CanvasItemsGenerator sequence) {
        this.sequence = (Sequence) sequence.getResource();
    }

    /**
     * Add otional seeAlso element to Manifest.
     * @param seeAlso other content model
     */
    public void addSeeAlso(ExternalLinksGenerator seeAlso) {
        this.seeAlso = (OtherContent) seeAlso.getResource();
    }

    /**
     * Add optional thumbnail image to manifest.
     * @param thumbnail image content model
     */
    public void addThumbnail(ImageContentGenerator thumbnail) {
        this.thumbnail = (ImageContent) thumbnail.getResource();
    }

    /**
     * Add optional related element to Manifest.
     * @param related other content model
     */
    public void addRelated(ExternalLinksGenerator related) {
        this.related = (OtherContent) related.getResource();
    }

    /**
     * Adds optional search service to Manifest.
     * @param searchService search service model
     */
    public void addService(ContentSearchGenerator searchService) {
        this.searchService = (ContentSearchService) searchService.getService();
    }

    /**
     * Adds single metadata field to Manifest.
     * @param field property field
     * @param value property value
     */
    public void addMetadata(String field, String value, String... rest) {
        metadataEntryGenerator.setField(field);
        metadataEntryGenerator.setValue(value, rest);
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
     * @param value the description value
     */
    public void addDescription(String value) {
        description = new PropertyValueGenerator().getPropertyValue(value).getValue();
    }

    /**
     * Adds optional Range to the manifest's structures element.
     * @param rangeGenerator to add
     */
    public void addRange(RangeGenerator rangeGenerator) {
        ranges.add(rangeGenerator);
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
        if (logo != null) {
            List<ImageContent> logos = new ArrayList<>();
            logos.add(logo);
            manifest.setLogos(logos);
        }
        if (sequence != null) {
            manifest.addSequence(sequence);
        }
        if (ranges != null && ranges.size() > 0) {
            for (RangeGenerator range : ranges) {
                manifest.addRange((Range) range.getResource());
            }
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
            manifest.addThumbnail(thumbnail);
        }
        if (viewingHint != null) {
            manifest.addViewingHint(viewingHint);
        }
        return manifest;
    }

}
