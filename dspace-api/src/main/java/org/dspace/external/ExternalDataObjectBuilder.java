/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external;

import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;

public class ExternalDataObjectBuilder {

    private ExternalDataObject externalDataObject;

    public ExternalDataObjectBuilder() {
        externalDataObject = new ExternalDataObject();
    }

    public ExternalDataObjectBuilder create() {
        externalDataObject = new ExternalDataObject();
        return this;
    }

    public ExternalDataObjectBuilder create(String source) {
        externalDataObject = new ExternalDataObject(source);
        return addMetadata("dc", "source", null, null, source);
    }

    public ExternalDataObject build() {
        return externalDataObject;
    }

    public ExternalDataObjectBuilder withId(String id) {
        this.externalDataObject.setId(id);
        return addMetadata("dc", "identifier", "uri", null, id);
    }

    public ExternalDataObjectBuilder withSource(String source) {
        this.externalDataObject.setSource(source);
        return addMetadata("dc", "source", null, null, source);
    }

    public ExternalDataObjectBuilder withValue(String value) {
        this.externalDataObject.setValue(value);
        return this;
    }

    public ExternalDataObjectBuilder withDisplayValue(String displayValue) {
        this.externalDataObject.setDisplayValue(displayValue);
        return this;
    }

    public ExternalDataObjectBuilder withLastModified(String lastModified) {
        return addMetadata("gnd", "date", "modified", null, lastModified);
    }

    public ExternalDataObjectBuilder withType(String... values) {
        return addMetadata("gnd", "type", null, null, values);
    }

    public ExternalDataObjectBuilder withTitle(String... values) {
        return addMetadata("dc", "title", null, null, values);
    }

    public ExternalDataObjectBuilder withGeographicAreaCodeId(String... values) {
        return addMetadata("gnd", "geographicAreaCode", "id", null, values);
    }

    public ExternalDataObjectBuilder withGeographicAreaCodeLabel(String... values) {
        return addMetadata("gnd", "geographicAreaCode", "label", null, values);
    }

    public ExternalDataObjectBuilder withBio(String... values) {
        return addMetadata("gnd", "biographicalOrHistoricalInformation", null, null, values);
    }

    public ExternalDataObjectBuilder withHomepage(String... values) {
        return addMetadata("gnd", "homepage", null, null, values);
    }

    public ExternalDataObjectBuilder withVariantNames(String... values) {
        return addMetadata("gnd", "variantName", null, null, 2, values);
    }

    public ExternalDataObjectBuilder withDepiction(String... values) {
        return addMetadata("gnd", "depiction", "thumbnail", null, values);
    }

    public ExternalDataObjectBuilder withDepictionLicense(String... values) {
        return addMetadata("gnd", "depiction", "license", null, values);
    }

    public ExternalDataObjectBuilder withSameAs(String key, String... values) {
        return addMetadata("gnd", "sameAs", key, null, values);
    }

    public ExternalDataObjectBuilder withSubjectCategory(String... values) {
        return addMetadata("gnd", "subjectCategory", null, null, values);
    }

    public ExternalDataObjectBuilder withEventDate(String... values) {
        return addMetadata("gnd", "dateOfConferenceOrEvent", null, null, values);
    }

    public ExternalDataObjectBuilder withEventPlace(String... values) {
        return addMetadata("gnd", "placeOfConferenceOrEvent", null, null, values);
    }

    public ExternalDataObjectBuilder withHierarchicalSuperior(String... values) {
        return addMetadata("gnd", "hierarchicalSuperiorOfTheConferenceOrEvent",
                null, null, values);
    }

    public ExternalDataObjectBuilder withContributorAuthor(String... values) {
        return addMetadata("dc", "contributor", "author",
                null, values);
    }

    public ExternalDataObjectBuilder withComposer(String... values) {
        return addMetadata("dc", "contributor", "author",
                null, values);
    }

    public ExternalDataObjectBuilder withDefinition(String... values) {
        return addMetadata("dc", "description", null,
                null, values);
    }

    public ExternalDataObjectBuilder withBroaderTerm(String... values) {
        return addMetadata("gnd", "broaderTermGeneric", "label",
                null, values);
    }

    public ExternalDataObjectBuilder withOrgAddressLocality(String... values) {
        return addMetadata("organization", "address", "addressLocality", null, values);
    }

    public ExternalDataObjectBuilder withOrgAddressCountry(String... values) {
        return addMetadata("organization", "address", "addressCountry", null, values);
    }

    public ExternalDataObjectBuilder withOrgName(String... values) {
        return addMetadata("organization", "legalName", null, null, values);
    }

    public ExternalDataObjectBuilder withDeprecatedUri(String... values) {
        return addMetadata("gnd", "deprecatedUri", null, null, values);
    }

    public ExternalDataObjectBuilder withOrgISNI(String... values) {
        return addMetadata("organization", "identifier", "isni", null, values);
    }

    public ExternalDataObjectBuilder withPersonISNI(String... values) {
        return addMetadata("person", "identifier", "isni", null, values);
    }

    public ExternalDataObjectBuilder withORCID(String... values) {
        return addMetadata("person", "identifier", "orcid", null, values);
    }

    public ExternalDataObjectBuilder withGivenName(String... values) {
        return addMetadata("person", "givenName", null, null, values);
    }

    public ExternalDataObjectBuilder withFamilyName(String... values) {
        return addMetadata("person", "familyName", null, null, values);
    }

    public ExternalDataObjectBuilder withFamilyNamePrefix(String... values) {
        return addMetadata("gnd", "familyName", "prefix", null, values);
    }

    public ExternalDataObjectBuilder withPersonNameVariant(String... values) {
        return addMetadata("person", "name", "variant", null, values);
    }

    public ExternalDataObjectBuilder withPersonAffiliation(String... values) {
        return addMetadata("person", "affiliation", "name", null, values);
    }

    public ExternalDataObjectBuilder withGeospatialPoint(String... values) {
        return addMetadata("dcterms", "spatial", null, null, values);
    }

    public ExternalDataObjectBuilder withLatitude(String... values) {
        return addMetadata("gnd", "spatial", "latitude", null, values);
    }

    public ExternalDataObjectBuilder withLongitude(String... values) {
        return addMetadata("gnd", "spatial", "longitude", null, values);
    }

    public ExternalDataObjectBuilder withBoundingBox(String... values) {
        return addMetadata("gnd", "spatial", "bbox", null, values);
    }

    public ExternalDataObjectBuilder withMetadataValueDTO(MetadataValueDTO value) {
        this.externalDataObject.addMetadata(value);
        return this;
    }

    private ExternalDataObjectBuilder addMetadata(String schema, String element, String qualifier, String language, String... values) {
        for (String v : values) {
            this.externalDataObject.addMetadata(
                    new MetadataValueDTO(schema, element, qualifier,
                            language, v));
        }
        return this;
    }

    private ExternalDataObjectBuilder addMetadata(String schema, String element, String qualifier, String language, int limit, String... values) {
        int i = 0;
        for (String v : values) {
            if (i < limit) {
                this.externalDataObject.addMetadata(
                        new MetadataValueDTO(schema, element, qualifier,
                                language, v));
            } else {
                break;
            }
            i++;
        }
        return this;
    }
}
