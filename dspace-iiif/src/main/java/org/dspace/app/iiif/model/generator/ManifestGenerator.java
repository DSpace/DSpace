/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model.generator;

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
 * This generator wraps a domain model for the {@code Manifest}.
 * <p>
 * Please note that this is a request scoped bean. This mean that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.</p>
 * <p>
 *  The Manifest is an overall description of the structure and properties of the digital representation
 *  of an object. It carries information needed for the viewer to present the digitized content to the user,
 *  such as a title and other descriptive information about the object or the intellectual work that
 *  it conveys. Each manifest describes how to present a single object such as a book, a photograph,
 *  or a statue.</p>
 *
 * Please note that this is a request scoped bean. This means that for each http request a
 * different instance will be initialized by Spring and used to serve this specific request.
 *
 * @author Michael Spalti  mspalti@willamette.edu
 * @author Andrea Bollini (andrea.bollini at 4science.it)
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
    private List<OtherContent> renderings = new ArrayList<>();
    private final List<URI> license = new ArrayList<>();
    private final List<MetadataEntry> metadata = new ArrayList<>();
    private final List<Range> ranges = new ArrayList<>();

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
        this.logo = (ImageContent) logo.generateResource();
    }

    /**
     * Sets the viewing hint. In IIIF Presentation API version 3.0 semantics this becomes the "behavior"
     * @param viewingHint a viewing hint
     */
    public void addViewingHint(String viewingHint) {
        BehaviorGenerator hint = new BehaviorGenerator().setType(viewingHint);
        this.viewingHint = hint.generateValue();
    }

    /**
     * Adds add single (mandatory) {@ode sequence} to the manifest. In IIIF Presentation API 3.0 "sequence"
     * is replaced by "items"
     * @param sequence canvas list model (sequence)
     */
    public void addSequence(CanvasItemsGenerator sequence) {
        this.sequence = (Sequence) sequence.generateResource();
    }

    /**
     * Adds an optional {@code seeAlso} element to Manifest.
     * @param seeAlso other content model
     */
    public void addSeeAlso(ExternalLinksGenerator seeAlso) {
        this.seeAlso = (OtherContent) seeAlso.generateResource();
    }

    /**
     * Adds optional thumbnail image resource to manifest.
     * @param thumbnail an image content generator
     */
    public void addThumbnail(ImageContentGenerator thumbnail) {
        this.thumbnail = (ImageContent) thumbnail.generateResource();
    }

    /**
     * Adds an optional {@code related} field to the manifest.
     * @param related other content generator
     */
    public void addRelated(ExternalLinksGenerator related) {
        this.related = (OtherContent) related.generateResource();
    }

    /**
     * Adds optional search service to the manifest.
     * @param searchService search service generator
     */
    public void addService(ContentSearchGenerator searchService) {
        this.searchService = (ContentSearchService) searchService.generateService();
    }

    /**
     * Adds a single metadata field to Manifest.
     * @param field property field
     * @param value property value
     */
    public void addMetadata(String field, String value, String... rest) {
        MetadataEntryGenerator meg = new MetadataEntryGenerator().setField(field).setValue(value, rest);
        metadata.add(meg.generateValue());
    }

    /**
     * Adds an optional license to manifest.
     * @param license license terms
     */
    public void addLicense(String license) {
        this.license.add(URI.create(license));
    }

    /**
     * Adds optional description to Manifest.
     * @param value the description value
     */
    public void addDescription(String value) {
        description = new PropertyValueGenerator().getPropertyValue(value).generateValue();
    }

    /**
     * Adds optional Range to the manifest's structures element.
     * @param rangeGenerator to add
     */
    public void addRange(RangeGenerator rangeGenerator) {
        ranges.add((Range) rangeGenerator.generateResource());
    }

    /**
     * Adds a rendering annotation to the Sequence. The rendering is a link to an external resource intended
     * for display or download by a human user. This is typically going to be a PDF file.
     * @param otherContent generator for the resource
     */
    public void addRendering(ExternalLinksGenerator otherContent) {
        this.renderings.add((OtherContent) otherContent.generateResource());
    }

    @Override
    public Resource<Manifest> generateResource() {

        if (identifier == null) {
            throw new RuntimeException("The Manifest resource requires an identifier.");
        }
        Manifest manifest;
        if (label != null) {
            manifest = new Manifest(identifier, label);
        } else {
            manifest = new Manifest(identifier);
        }
        if (renderings.size() > 0) {
            manifest.setRenderings(renderings);
        }
        if (logo != null) {
            List<ImageContent> logos = new ArrayList<>();
            logos.add(logo);
            manifest.setLogos(logos);
        }
        if (sequence != null) {
            manifest.addSequence(sequence);
        }
        if (ranges.size() > 0) {
            for (Range range : ranges) {
                manifest.addRange(range);
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
