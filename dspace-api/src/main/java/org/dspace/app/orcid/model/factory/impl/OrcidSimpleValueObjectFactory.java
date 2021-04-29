/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.COUNTRY;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.KEYWORDS;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.OTHER_NAMES;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.RESEARCHER_URLS;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.orcid.jaxb.model.common.Iso3166Country;
import org.orcid.jaxb.model.v3.release.common.Country;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.Address;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.OtherName;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrl;

/**
 * Implementation of {@link OrcidProfileSectionFactory} that creates ORCID
 * objects with a single value.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidSimpleValueObjectFactory extends AbstractOrcidProfileSectionFactory {

    protected final List<String> metadataFields;

    public OrcidSimpleValueObjectFactory(OrcidProfileSectionType sectionType, OrcidProfileSyncPreference preference,
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
    public Object create(Context context, List<MetadataValue> metadataValues) {

        if (CollectionUtils.isEmpty(metadataValues)) {
            return null;
        }

        if (metadataValues.size() > 1) {
            throw new IllegalArgumentException("Multiple metadata values not supported: " + metadataValues);
        }

        MetadataValue metadataValue = metadataValues.get(0);
        String currentMetadataField = metadataValue.getMetadataField().toString('.');

        if (!metadataFields.contains(currentMetadataField)) {
            throw new IllegalArgumentException("Metadata field not supported: " + currentMetadataField);
        }

        return create(metadataValues.get(0));
    }

    @Override
    public List<String> getMetadataSignatures(Context context, Item item) {
        return metadataFields.stream()
            .flatMap(metadataField -> getMetadataValues(item, metadataField).stream())
            .map(metadataValue -> metadataSignatureGenerator.generate(context, List.of(metadataValue)))
            .collect(Collectors.toList());
    }

    @Override
    public String getDescription(Context context, Item item, String signature) {
        List<MetadataValue> metadataValues = metadataSignatureGenerator.findBySignature(context, item, signature);
        if (CollectionUtils.isEmpty(metadataValues) || metadataValues.size() > 1) {
            return null;
        }
        return metadataValues.get(0).getValue();
    }

    protected Object create(MetadataValue metadataValue) {
        switch (getProfileSectionType()) {
            case COUNTRY:
                return createAddress(metadataValue);
            case KEYWORDS:
                return createKeyword(metadataValue);
            case OTHER_NAMES:
                return createOtherName(metadataValue);
            case RESEARCHER_URLS:
                return createResearcherUrl(metadataValue);
            default:
                throw new IllegalStateException("OrcidSimpleValueObjectFactory does not support type "
                    + getProfileSectionType());
        }
    }

    private ResearcherUrl createResearcherUrl(MetadataValue metadataValue) {
        ResearcherUrl researcherUrl = new ResearcherUrl();
        researcherUrl.setUrl(new Url(metadataValue.getValue()));
        return researcherUrl;
    }

    private OtherName createOtherName(MetadataValue metadataValue) {
        OtherName otherName = new OtherName();
        otherName.setContent(metadataValue.getValue());
        return otherName;
    }

    private Keyword createKeyword(MetadataValue metadataValue) {
        Keyword keyword = new Keyword();
        keyword.setContent(metadataValue.getValue());
        return keyword;
    }

    private Address createAddress(MetadataValue metadataValue) {
        Address address = new Address();
        address.setCountry(new Country(Iso3166Country.fromValue(metadataValue.getValue())));
        return address;
    }

}
