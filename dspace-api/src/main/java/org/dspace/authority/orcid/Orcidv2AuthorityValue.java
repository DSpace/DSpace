/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueServiceImpl;
import org.dspace.authority.PersonAuthorityValue;
import org.dspace.utils.DSpace;
import org.orcid.jaxb.model.common_v2.ExternalId;
import org.orcid.jaxb.model.record_v2.*;

import java.util.*;

/**
 * @author Jonas Van Goolen (jonas at atmire dot com)
 */
public class Orcidv2AuthorityValue extends PersonAuthorityValue {

    /*
     * The ORCID identifier
     */
    private String orcid_id;

    /*
     * Map containing key-value pairs filled in by "setValues(Person person)".
     * This represents all dynamic information of the object.
     */
    private Map<String, List<String>> otherMetadata = new HashMap<String, List<String>>();

    /**
     * The syntax that the ORCID id needs to conform to
     */
    public static final String ORCID_ID_SYNTAX = "\\d{4}-\\d{4}-\\d{4}-(\\d{3}X|\\d{4})";


    /**
     * Creates an instance of Orcidv2AuthorityValue with only uninitialized fields.
     * This is meant to be filled in with values from an existing record.
     * To create a brand new Orcidv2AuthorityValue, use create()
     */
    public Orcidv2AuthorityValue() {
    }

    public Orcidv2AuthorityValue(SolrDocument document) {
        super(document);
    }


    public String getOrcid_id() {
        return orcid_id;
    }

    public void setOrcid_id(String orcid_id) {
        this.orcid_id = orcid_id;
    }

    /**
     * Create an empty authority.
     * @return OrcidAuthorityValue
     */
    public static Orcidv2AuthorityValue create() {
        Orcidv2AuthorityValue orcidAuthorityValue = new Orcidv2AuthorityValue();
        orcidAuthorityValue.setId(UUID.randomUUID().toString());
        orcidAuthorityValue.updateLastModifiedDate();
        orcidAuthorityValue.setCreationDate(new Date());
        return orcidAuthorityValue;
    }

    /**
     * Create an authority based on a given orcid bio
     * @return OrcidAuthorityValue
     */
    public static Orcidv2AuthorityValue create(Person person) {
        if (person == null) {
            return null;
        }
        Orcidv2AuthorityValue authority = Orcidv2AuthorityValue.create();

        authority.setValues(person);

        return authority;
    }

    /**
     * Initialize this instance based on a Person object
     * @param person Person
     */
    protected void setValues(Person person) {
        NameType name = person.getName();

        if (!StringUtils.equals(name.getPath(), this.getOrcid_id())) {
            this.setOrcid_id(name.getPath());
        }

        if (!StringUtils.equals(name.getFamilyName().getValue(), this.getLastName())) {
            this.setLastName(name.getFamilyName().getValue());
        }

        if (!StringUtils.equals(name.getGivenNames().getValue(), this.getFirstName())) {
            this.setFirstName(name.getGivenNames().getValue());
        }

        if (name.getCreditName() != null && StringUtils.isNotBlank(name.getCreditName().getValue())) {
            if (!this.getNameVariants().contains(name.getCreditName())) {
                this.addNameVariant(name.getCreditName().getValue());
            }
        }

        if (person.getKeywords() != null) {
            for (KeywordType keyword : person.getKeywords().getKeyword()) {
                if (this.isNewMetadata("keyword", keyword.getContent())) {
                    this.addOtherMetadata("keyword", keyword.getContent());
                }
            }
        }

        ExternalIdentifiers externalIdentifiers = person.getExternalIdentifiers();
        if (externalIdentifiers != null) {
            for (ExternalId externalIdentifier : externalIdentifiers.getExternalIdentifier()) {
                if (this.isNewMetadata("external_identifier", externalIdentifier.getExternalIdValue())) {
                    this.addOtherMetadata("external_identifier", externalIdentifier.getExternalIdValue());

                }
            }
        }
        if (person.getResearcherUrls() != null) {
            for (ResearcherUrlType researcherUrl : person.getResearcherUrls().getResearcherUrl()) {
                if (this.isNewMetadata("researcher_url", researcherUrl.getUrl().getValue())) {
                    this.addOtherMetadata("researcher_url", researcherUrl.getUrl().getValue());
                }
            }

        }
        if (person.getBiography() != null) {
            if (this.isNewMetadata("biography", person.getBiography().getContent())) {
                this.addOtherMetadata("biography", person.getBiography().getContent());
            }
        }

        this.setValue(this.getName());

    }

    /**
     * Makes an instance of the AuthorityValue with the given information.
     * @param info string info
     * @return AuthorityValue
     */
    @Override
    public AuthorityValue newInstance(String info) {
        AuthorityValue authorityValue = null;
        if (StringUtils.isNotBlank(info)) {
            Orcidv2 orcid = new DSpace().getServiceManager().getServiceByName("AuthoritySource", Orcidv2.class);
            authorityValue = orcid.queryAuthorityID(info);
        } else {
            authorityValue = this.create();
        }
        return authorityValue;
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
    }

    /**
     * Check to see if the provided label / data pair is already present in the "otherMetadata" or not
     * */
    public boolean isNewMetadata(String label, String data) {
        List<String> strings = getOtherMetadata().get(label);
        boolean update;
        if (strings == null) {
            update = StringUtils.isNotBlank(data);
        } else {
            update = !strings.contains(data);
        }
        return update;
    }

    /**
     * Add additional metadata to the otherMetadata map*/
    public void addOtherMetadata(String label, String data) {
        List<String> strings = otherMetadata.get(label);
        if (strings == null) {
            strings = new ArrayList<>();
        }
        strings.add(data);
        otherMetadata.put(label, strings);
    }

    public Map<String, List<String>> getOtherMetadata() {
        return otherMetadata;
    }


    /**
     * Generate a solr record from this instance
     * @return SolrInputDocument
     */
    @Override
    public SolrInputDocument getSolrInputDocument() {
        SolrInputDocument doc = super.getSolrInputDocument();
        if (StringUtils.isNotBlank(getOrcid_id())) {
            doc.addField("orcid_id", getOrcid_id());
        }

        for (String t : otherMetadata.keySet()) {
            List<String> data = otherMetadata.get(t);
            for (String data_entry : data) {
                doc.addField("label_" + t, data_entry);
            }
        }
        return doc;
    }

    /**
     * Information that can be used the choice ui
     * @return map
     */
    @Override
    public Map<String, String> choiceSelectMap() {

        Map<String, String> map = super.choiceSelectMap();

        String orcid_id = getOrcid_id();
        if (StringUtils.isNotBlank(orcid_id)) {
            map.put("orcid", orcid_id);
        }

        return map;
    }

    @Override
    public String getAuthorityType() {
        return "orcid";
    }

    /**
     * Provides a string that will allow this AuthorityType to be recognized and provides information to create a new instance to be created using public Orcidv2AuthorityValue newInstance(String info).
     * @return see {@link org.dspace.authority.service.AuthorityValueService#GENERATE AuthorityValueService.GENERATE}
     */
    @Override
    public String generateString() {
        String generateString = AuthorityValueServiceImpl.GENERATE + getAuthorityType() + AuthorityValueServiceImpl.SPLIT;
        if (StringUtils.isNotBlank(getOrcid_id())) {
            generateString += getOrcid_id();
        }
        return generateString;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Orcidv2AuthorityValue that = (Orcidv2AuthorityValue) o;

        if (orcid_id != null ? !orcid_id.equals(that.orcid_id) : that.orcid_id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return orcid_id != null ? orcid_id.hashCode() : 0;
    }

    /**
     * The regular equals() only checks if both AuthorityValues describe the same authority.
     * This method checks if the AuthorityValues have different information
     * E.g. it is used to decide when lastModified should be updated.
     * @param o object
     * @return true or false
     */
    @Override
    public boolean hasTheSameInformationAs(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.hasTheSameInformationAs(o)) {
            return false;
        }

        Orcidv2AuthorityValue that = (Orcidv2AuthorityValue) o;

        if (orcid_id != null ? !orcid_id.equals(that.orcid_id) : that.orcid_id != null) {
            return false;
        }

        for (String key : otherMetadata.keySet()) {
            if (otherMetadata.get(key) != null) {
                List<String> metadata = otherMetadata.get(key);
                List<String> otherMetadata = that.otherMetadata.get(key);
                if (otherMetadata == null) {
                    return false;
                } else {
                    HashSet<String> metadataSet = new HashSet<String>(metadata);
                    HashSet<String> otherMetadataSet = new HashSet<String>(otherMetadata);
                    if (!metadataSet.equals(otherMetadataSet)) {
                        return false;
                    }
                }
            } else {
                if (that.otherMetadata.get(key) != null) {
                    return false;
                }
            }
        }

        return true;
    }
}
