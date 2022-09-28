/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.dspace.orcid.model.factory.OrcidFactoryUtils.parseConfigurations;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.util.SimpleMapConverter;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.v3.release.record.Work;

/**
 * Class that contains all the mapping between {@link Work} and DSpace metadata
 * fields.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidWorkFieldMapping {

    /**
     * The metadata fields related to the work contributors.
     */
    private Map<String, ContributorRole> contributorFields = new HashMap<>();

    /**
     * The metadata fields related to the work external identifiers.
     */
    private Map<String, String> externalIdentifierFields = new HashMap<>();

    /**
     * The metadata field related to the work publication date.
     */
    private String publicationDateField;

    /**
     * The metadata field related to the work title.
     */
    private String titleField;

    /**
     * The metadata field related to the work type.
     */
    private String typeField;

    /**
     * The metadata field related to the work journal title.
     */
    private String journalTitleField;

    /**
     * The metadata field related to the work description.
     */
    private String shortDescriptionField;

    /**
     * The metadata field related to the work language.
     */
    private String languageField;

    /**
     * The metadata field related to the work sub title.
     */
    private String subTitleField;

    /**
     * The work type converter.
     */
    private SimpleMapConverter typeConverter;

    /**
     * The work language converter.
     */
    private SimpleMapConverter languageConverter;

    public String convertType(String type) {
        return typeConverter != null ? typeConverter.getValue(type) : type;
    }

    public String convertLanguage(String language) {
        return languageConverter != null ? languageConverter.getValue(language) : language;
    }

    public String getTitleField() {
        return titleField;
    }

    public void setTitleField(String titleField) {
        this.titleField = titleField;
    }

    public String getTypeField() {
        return typeField;
    }

    public void setTypeField(String typeField) {
        this.typeField = typeField;
    }

    public void setTypeConverter(SimpleMapConverter typeConverter) {
        this.typeConverter = typeConverter;
    }

    public Map<String, ContributorRole> getContributorFields() {
        return contributorFields;
    }

    public void setContributorFields(String contributorFields) {
        this.contributorFields = parseContributors(contributorFields);
    }

    public Map<String, String> getExternalIdentifierFields() {
        return externalIdentifierFields;
    }

    public void setExternalIdentifierFields(String externalIdentifierFields) {
        this.externalIdentifierFields = parseConfigurations(externalIdentifierFields);
    }

    public String getPublicationDateField() {
        return publicationDateField;
    }

    public void setPublicationDateField(String publicationDateField) {
        this.publicationDateField = publicationDateField;
    }

    public String getJournalTitleField() {
        return journalTitleField;
    }

    public void setJournalTitleField(String journalTitleField) {
        this.journalTitleField = journalTitleField;
    }

    public String getShortDescriptionField() {
        return shortDescriptionField;
    }

    public void setShortDescriptionField(String shortDescriptionField) {
        this.shortDescriptionField = shortDescriptionField;
    }

    public String getLanguageField() {
        return languageField;
    }

    public void setLanguageField(String languageField) {
        this.languageField = languageField;
    }

    public void setLanguageConverter(SimpleMapConverter languageConverter) {
        this.languageConverter = languageConverter;
    }

    public String getSubTitleField() {
        return subTitleField;
    }

    public void setSubTitleField(String subTitleField) {
        this.subTitleField = subTitleField;
    }

    private Map<String, ContributorRole> parseContributors(String contributors) {
        Map<String, String> contributorsMap = parseConfigurations(contributors);
        return contributorsMap.keySet().stream()
            .collect(toMap(identity(), field -> parseContributorRole(contributorsMap.get(field))));
    }

    private ContributorRole parseContributorRole(String contributorRole) {
        try {
            return ContributorRole.fromValue(contributorRole);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The contributor role " + contributorRole +
                " is invalid, allowed values are " + getAllowedContributorRoles(), ex);
        }
    }

    private List<String> getAllowedContributorRoles() {
        return Arrays.asList(ContributorRole.values()).stream()
            .map(ContributorRole::value)
            .collect(Collectors.toList());
    }

}
