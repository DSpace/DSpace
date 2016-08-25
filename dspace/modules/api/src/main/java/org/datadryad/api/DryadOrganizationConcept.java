package org.datadryad.api;

import org.apache.log4j.Logger;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.authority.Term;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.datadryad.rest.storage.StorageException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.lang.*;
import java.lang.Exception;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;

import org.dspace.authorize.AuthorizeException;

/**
 *
 * @author Daisie Huang <daisieh@datadryad.org>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DryadOrganizationConcept implements Comparable<DryadOrganizationConcept> {
    // Organization Concepts can have the following metadata properties:
    public static final String ORGANIZATION_ID = "organizationID";
    public static final String FULLNAME = "fullname";

    public static final String SUBSCRIPTION_PLAN = "SUBSCRIPTION";
    public static final String PREPAID_PLAN = "PREPAID";
    public static final String DEFERRED_PLAN = "DEFERRED";
    public static final String NO_PLAN = "NONE";

    protected static Properties metadataProperties;
    protected static Properties defaultMetadataValues;

    private static Logger log = Logger.getLogger(DryadOrganizationConcept.class);

    static {
        metadataProperties = new Properties();
        defaultMetadataValues = new Properties();
    }

    // these are mandatory elements
    protected Concept underlyingConcept;
    protected String conceptIdentifier;
    protected String fullName;

    public DryadOrganizationConcept() {
    } // JAXB needs this

    public DryadOrganizationConcept(Context context, String fullName) throws StorageException {
        this();
        this.setFullName(fullName);
        try {
            context.commit();
        } catch (Exception e) {
            log.error("exception " + e.getMessage());
        }
    }

    @JsonIgnore
    public Concept getUnderlyingConcept() {
        return getUnderlyingConcept(null);
    }

    @JsonIgnore
    public Concept getUnderlyingConcept(Context context) {
        return underlyingConcept;
    }

    @JsonIgnore
    public void setUnderlyingConcept(Context context, Concept concept) {
        this.underlyingConcept = concept;
        this.conceptIdentifier = concept.getIdentifier();
    }

    @JsonIgnore
    public static Set<String> getMetadataPropertyNames() {
        return metadataProperties.stringPropertyNames();
    }

    @Override
    public int compareTo(DryadOrganizationConcept organizationConcept) {
        return this.getFullName().toUpperCase().compareTo(organizationConcept.getFullName().toUpperCase());
    }

    @JsonIgnore
    protected String getConceptMetadataValue(String mdString) {
        AuthorityMetadataValue authorityValue = getUnderlyingConcept().getSingleMetadata(mdString);
        String result = null;
        if (authorityValue != null) {
            result = authorityValue.getValue();
            return result;
        } else {
            result = defaultMetadataValues.getProperty(mdString);
        }
        if (result == null) {
            result = "";
        }
        return result;
    }

    @JsonIgnore
    protected void setConceptMetadataValue(String mdString, String value) {
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            Concept underlyingConcept = getUnderlyingConcept(context);
            AuthorityMetadataValue[] metadataValues = underlyingConcept.getMetadata(mdString);
            if (metadataValues == null || metadataValues.length == 0) {
                underlyingConcept.addMetadata(context, mdString, value);
            } else {
                for (AuthorityMetadataValue authorityMetadataValue : metadataValues) {
                    if (!authorityMetadataValue.value.equals(value)) {
                        underlyingConcept.clearMetadata(context, mdString);
                        underlyingConcept.addMetadata(context, mdString, value);
                        break;
                    }
                }
            }
            context.restoreAuthSystemState();
            context.commit();
            context.complete();
        } catch (Exception e) {
            log.error("Couldn't set metadata for " + fullName + ", " + e.getMessage());
            if (context != null) {
                context.abort();
            }
        }
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String value) throws StorageException {
        Context context = null;
        try {
            setConceptMetadataValue(metadataProperties.getProperty(FULLNAME), value);
            context = new Context();
            context.turnOffAuthorisationSystem();

            // add the new Term
            Term newTerm = getUnderlyingConcept(context).createTerm(context, value, Term.prefer_term);
            context.restoreAuthSystemState();
            context.complete();
        } catch (Exception e) {
            if (context != null) {
                context.abort();
            }
            throw new StorageException ("Couldn't set fullname for " + fullName + ", " + e.getMessage());
        }
        fullName = value;
    }

    public int getConceptID() {
        return this.getUnderlyingConcept().getID();
    }

    @JsonIgnore
    public String getIdentifier() {
        return conceptIdentifier;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            return "";
        }
    }

}
