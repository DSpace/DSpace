/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Term;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.dspace.core.Context;

/**
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class PersonAuthorityValue extends AuthorityValue {


    public static final String LASTNAME = "meta_person_familyName";
    public static final String familyName = "familyName";
    public static final String FIRSTNAME = "meta_person_givenName";
    public static final String givenName = "givenName";
    public static final String INSTITUTION = "meta_person_instition";
    public static final String instite = "instition";
    public static final String EMAIL = "meta_person_email";
    public static final String email = "email";

    public static final String PERSON = "person";
    private String firstName;
    private String lastName;

    private String institution;
    private List<String> emails = new ArrayList<String>();

    public PersonAuthorityValue() {
    }

    public PersonAuthorityValue(SolrDocument document) {
        super(document);
    }

    public String getName() {
        String name = "";
        if (StringUtils.isNotBlank(lastName)) {
            name = lastName;
            if (StringUtils.isNotBlank(firstName)) {
                name += ", ";
            }
        }
        if (StringUtils.isNotBlank(firstName)) {
            name += firstName;
        }
        return name;
    }

    public void setName(String name) {
        if (StringUtils.isNotBlank(name)) {
            String[] split = name.split(",");
            if (split.length > 0) {
                setLastName(split[0].trim());
                if (split.length > 1) {
                    setFirstName(split[1].trim());
                }
            }
        }
        if (!StringUtils.equals(getValue(), name)) {
            setValue(name);
        }
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        setName(value);
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void addEmail(String email) {
        if (StringUtils.isNotBlank(email)) {
            emails.add(email);
        }
    }

    @Override
    public SolrInputDocument getSolrInputDocument() {
        SolrInputDocument doc = super.getSolrInputDocument();
        if (StringUtils.isNotBlank(getFirstName())) {
            doc.addField(FIRSTNAME, getFirstName());
        }
        if (StringUtils.isNotBlank(getLastName())) {
            doc.addField(LASTNAME, getLastName());
        }
        for (String nameVariant : getNameVariants()) {
            doc.addField(ALTERNATE_LABEL, nameVariant);
        }

        for (String email : emails) {
            doc.addField(EMAIL, email);
        }
        doc.addField(INSTITUTION, getInstitution());
        return doc;
    }

    @Override
    public void setValues(SolrDocument document) {
        super.setValues(document);
        try{
            ArrayList<String>firstNames= (ArrayList<String>)document.getFieldValue(FIRSTNAME);
            ArrayList<String>lastNames = (ArrayList<String>)document.getFieldValue(LASTNAME);
            if(firstNames!=null&&firstNames.size()>0)
            this.firstName= firstNames.get(0);
            if(lastNames!=null&&lastNames.size()>0)
            this.lastName = lastNames.get(0);
        }catch (Exception e)
        {
            this.firstName= (String)document.getFieldValue(FIRSTNAME);
            this.lastName = (String)document.getFieldValue(LASTNAME);
        }
        if (document.getFieldValue(INSTITUTION) != null) {
            this.institution = (String)document.getFieldValue(INSTITUTION);
        }

        Collection<Object> emails =document.getFieldValues(EMAIL);
        if (emails != null) {
            for (Object email : emails) {
                addEmail(email.toString());
            }
        }
    }
    @Override
    public void setValues(Concept concept) throws SQLException{
        super.setValues(concept);
        //the name is set in the setName function
        if(concept.getMetadata(PERSON,familyName,null, Item.ANY)!=null)
        {
             this.lastName = concept.getMetadata(PERSON,familyName,null, Item.ANY)[0].getValue();
        }
        if(concept.getMetadata(PERSON,givenName,null, Item.ANY)!=null)
        {
            this.firstName = concept.getMetadata(PERSON,givenName,null, Item.ANY)[0].getValue();
        }
        if(concept.getMetadata(PERSON,instite,null, Item.ANY)!=null&&concept.getMetadata(PERSON,instite,null, Item.ANY).length>0)
        {
            this.institution = concept.getMetadata(PERSON,instite,null, Item.ANY)[0].getValue();
        }
        if(concept.getMetadata(PERSON,email,null, Item.ANY)!=null&&concept.getMetadata(PERSON,email,null, Item.ANY).length>0)
        {
            for(AuthorityMetadataValue metadataValue: concept.getMetadata(PERSON,email,null, Item.ANY))
            {
                this.emails.add(metadataValue.getValue());
            }
        }
    }
    @Override
    public Map<String, String> choiceSelectMap() {

        Map<String, String> map = super.choiceSelectMap();

        if (StringUtils.isNotBlank(getFirstName())) {
            map.put("first-name", getFirstName());
        } else {
            map.put("first-name", "/");
        }

        if (StringUtils.isNotBlank(getLastName())) {
            map.put("last-name", getLastName());
        } else {
            map.put("last-name", "/");
        }

        if (!getEmails().isEmpty()) {
            boolean added = false;
            for (String email : getEmails()) {
                if (!added && StringUtils.isNotBlank(email)) {
                    map.put("email",email);
                    added = true;
                }
            }
        }
        if (StringUtils.isNotBlank(getInstitution())) {
            map.put("institution", getInstitution());
        }

        return map;
    }

    @Override
    public String getAuthorityType() {
        return PERSON;
    }

    @Override
    public String generateString() {
        return AuthorityValueGenerator.GENERATE + getAuthorityType() + AuthorityValueGenerator.SPLIT + getName();
        // the part after "AuthorityValueGenerator.GENERATE + getAuthorityType() + AuthorityValueGenerator.SPLIT" is the value of the "info" parameter in public AuthorityValue newInstance(String info)
    }

    @Override
    public AuthorityValue newInstance(String info) {
        PersonAuthorityValue authorityValue = new PersonAuthorityValue();
        authorityValue.setValue(info);
        return authorityValue;
    }

    @Override
    public String toString() {
        return "PersonAuthorityValue{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", institution='" + institution + '\'' +
                ", emails=" + emails +
                "} " + super.toString();
    }

    public boolean hasTheSameInformationAs(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if(!super.hasTheSameInformationAs(o)){
            return false;
        }

        PersonAuthorityValue that = (PersonAuthorityValue) o;

        if (emails != null ? !emails.equals(that.emails) : that.emails != null) {
            return false;
        }
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) {
            return false;
        }
        if (institution != null ? !institution.equals(that.institution) : that.institution != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) {
            return false;
        }

        return true;
    }


    public void updateConceptFromAuthorityValue(Context context, Concept concept) throws SQLException,AuthorizeException {
        super.updateConceptFromAuthorityValue(context, concept);
        concept.addMetadata(context, PERSON,"familyName",null,"",lastName,null,-1);
        concept.addMetadata(context, PERSON,"givenName",null,"",firstName,null,-1);
        if(institution!=null)
        concept.addMetadata(context, PERSON,"institution",null,"",institution,null,-1);
        for(String email:emails) {
            concept.addMetadata(context, PERSON,"email",null,"",email,null,-1);
        }
    }
}
