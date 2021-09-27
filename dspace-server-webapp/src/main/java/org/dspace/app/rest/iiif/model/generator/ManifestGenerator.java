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
import javax.validation.constraints.NotNull;

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
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This generator wraps a domain model for the {@code Manifest}. There should be a single instance of
 * this object per request. The {@code @RequestScope} provides a single instance created and available during
 * complete lifecycle of the HTTP request.
 * <p>
 *  The Manifest is an overall description of the structure and properties of the digital representation
 *  of an object. It carries information needed for the viewer to present the digitized content to the user,
 *  such as a title and other descriptive information about the object or the intellectual work that
 *  it conveys. Each manifest describes how to present a single object such as a book, a photograph,
 *  or a statue.
 * </p>
 */
@RequestScope
@Component
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
    private List<RangeGenerator> ranges = new ArrayList<>();

    /**
     * Sets the mandatory manifest identifier.
     * @param identifier manifest identifier
     */
    public void setIdentifier(@NotNull String identifier) {

        if (identifier.isEmpty()) {
            throw new RuntimeException("Invalid manifest identifier. Cannot be an empty string.");
        }
        this.identifier = identifier;
    }

    /**
     * Sets the manifest label.
     * @param label manifest label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    public void addLogo(ImageContentGenerator logo) {
        this.logo = (ImageContent) logo.generate();
    }

    /**
     * Sets the viewing hint. In IIIF Presentation API version 3.0 semantics this becomes the "behavior"
     * @param viewingHint a viewing hint
     */
    public void addViewingHint(String viewingHint) {
        BehaviorGenerator hint = new BehaviorGenerator().setType(viewingHint);
        this.viewingHint = hint.generate();
    }

    /**
     * Adds add single (mandatory) {@ode sequence} to the manifest. In IIIF Presentation API 3.0 "sequence"
     * is replaced by "items"
     * @param sequence canvas list model (sequence)
     */
    public void addSequence(CanvasItemsGenerator sequence) {
        this.sequence = (Sequence) sequence.generate();
    }

    /**
     * Adds an optional {@code seeAlso} element to Manifest.
     * @param seeAlso other content model
     */
    public void addSeeAlso(ExternalLinksGenerator seeAlso) {
        this.seeAlso = (OtherContent) seeAlso.generate();
    }

    /**
     * Adds optional thumbnail image resource to manifest.
     * @param thumbnail an image content generator
     */
    public void addThumbnail(ImageContentGenerator thumbnail) {
        this.thumbnail = (ImageContent) thumbnail.generate();
    }

    /**
     * Adds an optional {@code related} field to the manifest.
     * @param related other content generator
     */
    public void addRelated(ExternalLinksGenerator related) {
        this.related = (OtherContent) related.generate();
    }

    /**
     * Adds optional search service to the manifest.
     * @param searchService search service generator
     */
    public void addService(ContentSearchGenerator searchService) {
        this.searchService = (ContentSearchService) searchService.generate();
    }

    /**
     * Adds a single metadata field to Manifest.
     * @param field property field
     * @param value property value
     */
    public void addMetadata(String field, String value) {
        MetadataEntryGenerator meta = new MetadataEntryGenerator().setField(field).setValue(value);
        metadata.add(meta.generate());
    }

    /**
     * Adds an optional license to manifest.
     * @param license license terms
     */
    public void addLicense(String license) {
        this.license.add(URI.create(license));
    }

    /**
     * Adds an optional description to the manifest.
     * @param field property field
     * @param value property value
     */
    public void addDescription(String field, String value) {
        description = new PropertyValueGenerator().getPropertyValue(field, value).generate();
    }

    /**
     * Adds an optional {@code range} to the manifest's {@code structures} element.
     * @param rangeGenerator list of range generators
     */
    public void setRange(List<RangeGenerator> rangeGenerator) {
        ranges =  rangeGenerator;
    }

    @Override
    public Resource<Manifest> generate() {

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
                manifest.addRange((Range) range.generate());
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
