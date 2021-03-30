/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueServiceImpl;
import org.dspace.authority.PersonAuthorityValue;
import org.dspace.utils.DSpace;
import org.orcid.jaxb.model.v3.release.record.Keyword;
import org.orcid.jaxb.model.v3.release.record.Name;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifiers;
import org.orcid.jaxb.model.v3.release.record.ResearcherUrl;

/**
 * An {@link AuthorityValue} encapsulating information retrieved from ORCID
 *
 * @author Jonas Van Goolen (jonas at atmire dot com)
 */
public class Orcidv3AuthorityValue extends PersonAuthorityValue {

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
     * Creates an instance of Orcidv3AuthorityValue with only uninitialized fields.
     * This is meant to be filled in with values from an existing record.
     * To create a brand new Orcidv3AuthorityValue, use create()
     */
    public Orcidv3AuthorityValue() {
    }

    public Orcidv3AuthorityValue(SolrDocument document) {
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
    public static Orcidv3AuthorityValue create() {
        Orcidv3AuthorityValue orcidAuthorityValue = new Orcidv3AuthorityValue();
        orcidAuthorityValue.setId(UUID.randomUUID().toString());
        orcidAuthorityValue.updateLastModifiedDate();
        orcidAuthorityValue.setCreationDate(new Date());
        return orcidAuthorityValue;
    }

    /**
     * Create an authority based on a given orcid bio
     * @return OrcidAuthorityValue
     */
    public static Orcidv3AuthorityValue create(Person person) {
        if (person == null) {
            return null;
        }
        Orcidv3AuthorityValue authority = Orcidv3AuthorityValue.create();

        authority.setValues(person);

        return authority;
    }

    /**
     * Initialize this instance based on a Person object
     * @param person Person
     */
    protected void setValues(Person person) {
        Name name = person.getName();

        if (!StringUtils.equals(name.getPath(), this.getOrcid_id())) {
            this.setOrcid_id(name.getPath());
        }

        if (!StringUtils.equals(name.getFamilyName().getContent(), this.getLastName())) {
            this.setLastName(name.getFamilyName().getContent());
        }

        if (!StringUtils.equals(name.getGivenNames().getContent(), this.getFirstName())) {
            this.setFirstName(name.getGivenNames().getContent());
        }

        if (name.getCreditName() != null && StringUtils.isNotBlank(name.getCreditName().getContent())) {
            if (!this.getNameVariants().contains(name.getCreditName().getContent())) {
                this.addNameVariant(name.getCreditName().getContent());
            }
        }

        if (person.getKeywords() != null) {
            for (Keyword keyword : person.getKeywords().getKeywords()) {
                if (this.isNewMetadata("keyword", keyword.getContent())) {
                    this.addOtherMetadata("keyword", keyword.getContent());
                }
            }
        }

        PersonExternalIdentifiers externalIdentifiers = person.getExternalIdentifiers();
        if (externalIdentifiers != null) {
            for (PersonExternalIdentifier externalIdentifier : externalIdentifiers.getExternalIdentifiers()) {
                if (this.isNewMetadata("external_identifier", externalIdentifier.getValue())) {
                    this.addOtherMetadata("external_identifier", externalIdentifier.getValue());
                }
            }
        }
        if (person.getResearcherUrls() != null) {
            for (ResearcherUrl researcherUrl : person.getResearcherUrls().getResearcherUrls()) {
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
            Orcidv3SolrAuthorityImpl orcid = new DSpace().getServiceManager().getServiceByName("AuthoritySource",
                    Orcidv3SolrAuthorityImpl.class);
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
     * Provides a string that will allow this AuthorityType to be recognized and
     * provides information to create a new instance to be created using public
     * Orcidv3AuthorityValue newInstance(String info).
     * 
     * @return see
     *         {@link org.dspace.authority.service.AuthorityValueService#GENERATE
     *         AuthorityValueService.GENERATE}
     */
    @Override
    public String generateString() {
        String generateString = AuthorityValueServiceImpl.GENERATE + getAuthorityType()
                + AuthorityValueServiceImpl.SPLIT;
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

        Orcidv3AuthorityValue that = (Orcidv3AuthorityValue) o;

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

        Orcidv3AuthorityValue that = (Orcidv3AuthorityValue) o;

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

    @Override
    public void setValues(SolrDocument document) {
        super.setValues(document);
        this.orcid_id = ObjectUtils.toString(document.getFieldValue("orcid_id"));
        for (String key : document.getFieldNames()) {
            if (key.startsWith("label_")) {
                String keyInternalMap = key.substring(key.indexOf("_") + 1);
                Collection<Object> valuesSolr = document.getFieldValues(key);
                for (Object valueInternal : valuesSolr) {
                    addOtherMetadata(keyInternalMap, (String) valueInternal);
                }
            }
        }
    }
}