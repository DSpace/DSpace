/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.authority.PersonAuthorityValue;
import org.dspace.authority.orcid.model.Bio;
import org.dspace.authority.orcid.model.BioExternalIdentifier;
import org.dspace.authority.orcid.model.BioName;
import org.dspace.authority.orcid.model.BioResearcherUrl;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.authority.Concept;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.*;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class OrcidAuthorityValue extends PersonAuthorityValue {

    /**
     * log4j logger
     */
    private static Logger log = Logger.getLogger(OrcidAuthorityValue.class);
    public static final String ORCID = "orcid";

    public static final String ORCIDID = "meta_person_orcid_id";
    public static final String LABEL = "meta_person_label_";
    public static final String COUNTRY = "meta_person_country";
    public static final String BIOGRAPHY = "meta_person_biography";
    public static final String KEYWORD = "meta_person_keyword";
    public static final String RESEARCHERURL = "meta_person_researcher_url";
    public static final String EXIDENTIFIER = "meta_person_external_identifier";


    private String orcid_id;

    private boolean update; // used in setValues(Bio bio)


    /**
     * Creates an instance of OrcidAuthorityValue with only uninitialized fields.
     * This is meant to be filled in with values from an existing record.
     * To create a brand new OrcidAuthorityValue, use create()
     */
    public OrcidAuthorityValue() {
    }

    public OrcidAuthorityValue(SolrDocument document) {
        super(document);
    }

    public String getOrcid_id() {
        return orcid_id;
    }

    public void setOrcid_id(String orcid_id) {
        this.orcid_id = orcid_id;
    }


    @Override
    public SolrInputDocument getSolrInputDocument() {
        SolrInputDocument doc = super.getSolrInputDocument();
        if (StringUtils.isNotBlank(getOrcid_id())) {
            doc.addField(ORCIDID, getOrcid_id());
        }

        //TODO SEE IF THIS CAN GO INTO ALTERNATE LABEL and Concept.setAlternateTerm
        for (String t : getOtherMetadata().keySet()) {
            List<String> data = getOtherMetadata().get(t);
            for (String data_entry : data) {
                doc.addField(LABEL + t, data_entry);
            }
        }
        return doc;
    }

    @Override
    public void setValues(Concept concept) throws SQLException {
        super.setValues(concept);
        if(concept.getMetadata(PERSON,ORCID,"id",Item.ANY)!=null){
            this.orcid_id = concept.getMetadata(PERSON,ORCID,"id",Item.ANY)[0].value;
        }

    }

    @Override
    public void setValues(SolrDocument document) {
        super.setValues(document);
        this.orcid_id = String.valueOf(document.getFieldValue(ORCIDID));
    }

    public static OrcidAuthorityValue create() {
        OrcidAuthorityValue orcidAuthorityValue = new OrcidAuthorityValue();
        orcidAuthorityValue.setId(UUID.randomUUID().toString());
        orcidAuthorityValue.updateLastModifiedDate();
        orcidAuthorityValue.setCreationDate(new Date());
        return orcidAuthorityValue;
    }

    /**
     * Create an authority based on a given orcid bio
     */
    public static OrcidAuthorityValue create(Bio bio) {
        OrcidAuthorityValue authority = OrcidAuthorityValue.create();

        authority.setValues(bio);

        return authority;
    }

    public boolean setValues(Bio bio) {
        BioName name = bio.getName();

        if (updateValue(bio.getOrcid(), getOrcid_id())) {
            setOrcid_id(bio.getOrcid());
        }

        if (updateValue(name.getFamilyName(), getLastName())) {
            setLastName(name.getFamilyName());
        }

        if (updateValue(name.getGivenNames(), getFirstName())) {
            setFirstName(name.getGivenNames());
        }

        if (StringUtils.isNotBlank(name.getCreditName())) {
            if (!getNameVariants().contains(name.getCreditName())) {
                addNameVariant(name.getCreditName());
                update = true;
            }
        }
        for (String otherName : name.getOtherNames()) {
            if (!getNameVariants().contains(otherName)) {
                addNameVariant(otherName);
                update = true;
            }
        }

        if (updateOtherMetadata(COUNTRY, bio.getCountry())) {
            addOtherMetadata(COUNTRY, bio.getCountry());
        }

        for (String keyword : bio.getKeywords()) {
            if (updateOtherMetadata(KEYWORD, keyword)) {
                addOtherMetadata(KEYWORD, keyword);
            }
        }

        for (BioExternalIdentifier externalIdentifier : bio.getBioExternalIdentifiers()) {
            if (updateOtherMetadata(EXIDENTIFIER, externalIdentifier.toString())) {
                addOtherMetadata(EXIDENTIFIER, externalIdentifier.toString());
            }
        }

        for (BioResearcherUrl researcherUrl : bio.getResearcherUrls()) {
            if (updateOtherMetadata(RESEARCHERURL, researcherUrl.toString())) {
                addOtherMetadata(RESEARCHERURL, researcherUrl.toString());
            }
        }

        if (updateOtherMetadata(BIOGRAPHY, bio.getBiography())) {
            addOtherMetadata(BIOGRAPHY, bio.getBiography());
        }

        setValue(getName());

        if (update) {
            update();
        }
        boolean result = update;
        update = false;
        return result;
    }

    private boolean updateOtherMetadata(String label, String data) {
        List<String> strings = getOtherMetadata().get(label);
        boolean update;
        if (strings == null) {
            update = StringUtils.isNotBlank(data);
        } else {
            update = !strings.contains(data);
        }
        if (update) {
            this.update = true;
        }
        return update;
    }

    private boolean updateValue(String incoming, String resident) {
        boolean update = StringUtils.isNotBlank(incoming) && !incoming.equals(resident);
        if (update) {
            this.update = true;
        }
        return update;
    }

    @Override
    public Map<String, String> choiceSelectMap() {

        Map<String, String> map = super.choiceSelectMap();

        map.put(ORCID, getOrcid_id());

        return map;
    }

    public String getAuthorityType() {
        return ORCID;
    }

    @Override
    public String generateString() {
        String generateString = AuthorityValueGenerator.GENERATE + getAuthorityType() + AuthorityValueGenerator.SPLIT;
        if (StringUtils.isNotBlank(getOrcid_id())) {
            generateString += getOrcid_id();
        }
        return generateString;
    }


    @Override
    public AuthorityValue newInstance(String info) {
        AuthorityValue authorityValue = null;
        if (StringUtils.isNotBlank(info)) {
            Orcid orcid = Orcid.getOrcid();
            authorityValue = orcid.queryAuthorityID(info);
        } else {
            authorityValue = OrcidAuthorityValue.create();
        }
        return authorityValue;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OrcidAuthorityValue that = (OrcidAuthorityValue) o;

        if (orcid_id != null ? !orcid_id.equals(that.orcid_id) : that.orcid_id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return orcid_id != null ? orcid_id.hashCode() : 0;
    }

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

        OrcidAuthorityValue that = (OrcidAuthorityValue) o;

        if (orcid_id != null ? !orcid_id.equals(that.orcid_id) : that.orcid_id != null) {
            return false;
        }

        return true;
    }


    public void updateConceptFromAuthorityValue(Context context, Concept concept) throws SQLException,AuthorizeException {

        super.updateConceptFromAuthorityValue(context, concept);
        if (orcid_id!=null) {
            concept.addMetadata(context, PERSON,ORCID,"id",null,orcid_id,null,-1);
        }
    }
}
