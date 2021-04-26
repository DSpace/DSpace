/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.builder;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.COUNTRY;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.KEYWORDS;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.OTHER_NAMES;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.RESEARCHER_URLS;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.v3.release.common.Country;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.OtherName;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrl;

/**
 * Implementation of {@OrcidProfileSectionConfiguration} that build ORCID
 * objects with a single value.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidSimpleValueBuilder extends OrcidProfileSectionBuilder {

    protected final List<String> metadataFields;

    public OrcidSimpleValueBuilder(OrcidProfileSectionType sectionType, OrcidProfileSyncPreference preference,
        String metadataFields) {
        super(sectionType, preference);
        this.metadataFields = metadataFields != null ? asList(metadataFields.split(",")) : emptyList();
    }

    @Override
    public List<String> getMetadataFields() {
        return metadataFields;
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(COUNTRY, KEYWORDS, OTHER_NAMES, RESEARCHER_URLS);
    }

    @Override
    public List<Object> buildOrcidObjects(Context context, Item item, OrcidProfileSectionType type) {
        return metadataFields.stream()
            .flatMap(metadataField -> getMetadataValues(item, metadataField).stream())
            .map(this::buildOrcidObject)
            .collect(Collectors.toList());
    }

    private Object buildOrcidObject(String metadataValue) {
        switch (getSectionType()) {
            case COUNTRY:
                return buildAddress(metadataValue);
            case KEYWORDS:
                return buildKeyword(metadataValue);
            case OTHER_NAMES:
                return buildOtherName(metadataValue);
            case RESEARCHER_URLS:
                return buildResearcherUrl(metadataValue);
            default:
                throw new IllegalStateException("OrcidSimpleValueBuilder does not support type " + getSectionType());
        }
    }

    private ResearcherUrl buildResearcherUrl(String metadataValue) {
        ResearcherUrl researcherUrl = new ResearcherUrl();
        researcherUrl.setUrl(new Url(metadataValue));
        return researcherUrl;
    }

    private OtherName buildOtherName(String metadataValue) {
        OtherName otherName = new OtherName();
        otherName.setContent(metadataValue);
        return otherName;
    }

    private Keyword buildKeyword(String metadataValue) {
        Keyword keyword = new Keyword();
        keyword.setContent(metadataValue);
        return keyword;
    }

    private Address buildAddress(String metadataValue) {
        Address address = new Address();
        address.setCountry(new Country(Iso3166Country.fromValue(metadataValue)));
        return address;
    }

}
