package org.datadryad.api;

import org.apache.log4j.Logger;
import org.dspace.JournalUtils;
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
    public static final String FULLNAME = "fullname";
    public static final String CUSTOMER_ID = "customerID";
    public static final String DESCRIPTION = "description";
    public static final String WEBSITE = "website";
    public static final String PAYMENT_PLAN = "paymentPlanType";

    public static final String SUBSCRIPTION_PLAN = "SUBSCRIPTION";
    public static final String PREPAID_PLAN = "PREPAID";
    public static final String DEFERRED_PLAN = "DEFERRED";
    public static final String NO_PLAN = "NONE";

    public static Properties metadataProperties;
    protected static Properties defaultMetadataValues;

    private static Logger log = Logger.getLogger(DryadOrganizationConcept.class);

    static {
        metadataProperties = new Properties();
        metadataProperties.setProperty(FULLNAME, "organization.fullName");
        metadataProperties.setProperty(PAYMENT_PLAN, "organization.paymentPlanType");
        metadataProperties.setProperty(DESCRIPTION, "organization.description");
        metadataProperties.setProperty(WEBSITE, "organization.website");
        metadataProperties.setProperty(CUSTOMER_ID, "organization.customerID");

        defaultMetadataValues = new Properties();
        defaultMetadataValues.setProperty(metadataProperties.getProperty(FULLNAME), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(PAYMENT_PLAN), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(DESCRIPTION), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(WEBSITE), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(CUSTOMER_ID), "");
    }

    // these are mandatory elements
    protected Concept underlyingConcept;
    protected String conceptIdentifier;
    protected String fullName;

    public DryadOrganizationConcept() {
    } // JAXB needs this

    public DryadOrganizationConcept(Context context, Concept concept) {
        this();
        setUnderlyingConcept(context, concept);
        AuthorityMetadataValue[] amvs = concept.getMetadata(metadataProperties.getProperty(FULLNAME));
        if (amvs.length > 0) {
            fullName = amvs[0].getValue();
        }
    }

    public void create(Context context) {
        try {
            context.turnOffAuthorisationSystem();
            Scheme funderScheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.dryad_organization"));
            Concept newConcept = funderScheme.createConcept(context);
            this.setUnderlyingConcept(context, newConcept);
            context.commit();
            context.restoreAuthSystemState();
        } catch (Exception e) {
            log.error("Couldn't make new concept: " + e.getMessage());
        }
    }

    public void delete(Context context) throws SQLException, AuthorizeException {
        this.getUnderlyingConcept(context).delete(context);
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
            log.error("Couldn't set metadata for " + fullName + ": " + mdString + ", " + value + " - " + e.getMessage());
            if (context != null) {
                context.abort();
            }
        }
    }

    @JsonIgnore
    protected void addConceptMetadataValue(String mdString, String value) {
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            Concept underlyingConcept = getUnderlyingConcept(context);
            underlyingConcept.addMetadata(context, mdString, value);
            context.restoreAuthSystemState();
            context.commit();
            context.complete();
        } catch (Exception e) {
            log.error("Couldn't add metadata for " + fullName + ": " + mdString + ", " + value + " - " + e.getMessage());
            if (context != null) {
                context.abort();
            }
        }
    }

    public String getFullName() {
        if (fullName == null) {
            fullName = "";
        }
        return fullName;
    }

    public void setFullName(String value) throws StorageException {
        Context context = null;
        if (!getFullName().equals(value)) {
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
                throw new StorageException("Couldn't set fullname for " + fullName + ", " + e.getMessage());
            }
            fullName = value;
        }
    }

    public int getConceptID() {
        return this.getUnderlyingConcept().getID();
    }

    @JsonIgnore
    public String getIdentifier() {
        return conceptIdentifier;
    }

    public String getCustomerID() {
        return getConceptMetadataValue(metadataProperties.getProperty(CUSTOMER_ID));
    }

    public void setCustomerID(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(CUSTOMER_ID), value);
    }

    public String getDescription() {
        return getConceptMetadataValue(metadataProperties.getProperty(DESCRIPTION));
    }

    public void setDescription(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(DESCRIPTION), value);
    }

    public String getWebsite() {
        return getConceptMetadataValue(metadataProperties.getProperty(WEBSITE));
    }

    public void setWebsite(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(WEBSITE), value);
    }

    public String getPaymentPlan() {
        return getConceptMetadataValue(metadataProperties.getProperty(PAYMENT_PLAN));
    }

    public void setPaymentPlan(String paymentPlan) {
        String paymentPlanType = "";
        if (SUBSCRIPTION_PLAN.equals(paymentPlan)) {
            paymentPlanType = SUBSCRIPTION_PLAN;
        } else if (DEFERRED_PLAN.equals(paymentPlan)) {
            paymentPlanType = DEFERRED_PLAN;
        } else if (PREPAID_PLAN.equals(paymentPlan)) {
            paymentPlanType = PREPAID_PLAN;
        }
        setConceptMetadataValue(metadataProperties.getProperty(PAYMENT_PLAN), paymentPlanType);
    }

    @JsonIgnore
    public Boolean getSubscriptionPaid() {
        String paymentPlan = getConceptMetadataValue(metadataProperties.getProperty(PAYMENT_PLAN));
        Boolean newSubscriptionPaid = false;
        if (SUBSCRIPTION_PLAN.equals(paymentPlan)) {
            newSubscriptionPaid = true;
        } else if (DEFERRED_PLAN.equals(paymentPlan)) {
            newSubscriptionPaid = true;
        } else if (PREPAID_PLAN.equals(paymentPlan)) {
            newSubscriptionPaid = true;
        }
        return newSubscriptionPaid;
    }

    @JsonIgnore
    public Boolean isValid() {
        return (getFullName() != null);
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

    public static DryadOrganizationConcept getOrganizationConceptMatchingName(Context context, String fullName) {
        DryadOrganizationConcept organizationConcept = null;
        Concept[] concepts = Concept.searchByMetadata(context, metadataProperties.getProperty(FULLNAME), fullName);
        if (concepts.length > 0) {
            organizationConcept = new DryadOrganizationConcept(context, concepts[0]);
        }
        return organizationConcept;
    }

    public static DryadOrganizationConcept getOrganizationConceptMatchingCustomerID(Context context, String customerID) {
        DryadOrganizationConcept organizationConcept = null;
        Concept[] concepts = Concept.searchByMetadata(context, metadataProperties.getProperty(CUSTOMER_ID), customerID);
        if (concepts.length > 0) {
            organizationConcept = new DryadOrganizationConcept(context, concepts[0]);
        }
        return organizationConcept;
    }

    public static DryadOrganizationConcept getOrganizationConceptMatchingConceptID(Context context, int conceptID) {
        DryadOrganizationConcept organizationConcept = null;
        try {
            Concept concept = Concept.find(context, conceptID);
            organizationConcept = new DryadOrganizationConcept(context, concept);
        } catch (SQLException e) {
            log.error("couldn't find a concept: " + e.getMessage());
        }
        return organizationConcept;
    }
}
