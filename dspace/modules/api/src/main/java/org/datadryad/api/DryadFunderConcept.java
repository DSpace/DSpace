package org.datadryad.api;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.datadryad.rest.storage.StorageException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.lang.*;
import java.lang.Exception;
import java.sql.SQLException;

/**
 *
 * @author Daisie Huang <daisieh@datadryad.org>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DryadFunderConcept extends DryadOrganizationConcept {
    // Journal Concepts can have the following metadata properties defined in the old properties file:
    public static final String FULLNAME = "fullname";
    public static final String PAYMENT_PLAN = "paymentPlanType";

    // Journal Concepts can also have the following fields:
    public static final String CUSTOMER_ID = "customerID";
    public static final String DESCRIPTION = "description";
    public static final String WEBSITE = "website";

    private static Logger log = Logger.getLogger(DryadFunderConcept.class);

    static {
        metadataProperties.setProperty(FULLNAME, "journal.fullname");
        metadataProperties.setProperty(PAYMENT_PLAN, "journal.paymentPlanType");
        metadataProperties.setProperty(CUSTOMER_ID, "journal.customerID");
        metadataProperties.setProperty(DESCRIPTION, "journal.description");
        metadataProperties.setProperty(WEBSITE, "journal.website");

        defaultMetadataValues.setProperty(metadataProperties.getProperty(FULLNAME), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(PAYMENT_PLAN), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(CUSTOMER_ID), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(DESCRIPTION), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(WEBSITE), "");
    }

    // these are mandatory elements

    public DryadFunderConcept() {
        Context context = null;
        try {
            context = new Context();
            create(context);
            context.commit();
            for (String prop : metadataProperties.stringPropertyNames()) {
                String mdString = metadataProperties.getProperty(prop);
                this.setConceptMetadataValue(mdString, defaultMetadataValues.getProperty(mdString));
            }
        } catch (Exception e) {
            log.error("Couldn't make new concept: " + e.getMessage());
            if (context != null) {
                context.abort();
            }
        }
    } // JAXB needs this

    public DryadFunderConcept(Context context, Concept concept) {
        super();
        setUnderlyingConcept(context, concept);
        fullName = getConceptMetadataValue(metadataProperties.getProperty(FULLNAME));
    }

    public DryadFunderConcept(Context context, String fullName) throws StorageException {
        this.setFullName(fullName);
        try {
            context.commit();
        } catch (Exception e) {
            log.error("exception " + e.getMessage());
        }
    }

    public void create(Context context) {
        try {
            context.turnOffAuthorisationSystem();
            Scheme journalScheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
            Concept newConcept = journalScheme.createConcept(context);
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
    public Boolean isValid() {
        return (getFullName() != null);
    }

    @JsonIgnore
    public static Boolean conceptIsValidFunder(Concept concept) {
        return ((concept != null) && (concept.getSingleMetadata(metadataProperties.getProperty(FULLNAME)) != null));
    }
}
