package org.datadryad.api;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.authority.Term;
import org.dspace.content.authority.AuthorityObject;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.datadryad.rest.models.Organization;
import org.datadryad.rest.storage.StorageException;
import org.dspace.JournalUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.lang.*;
import java.lang.Exception;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import org.dspace.authorize.AuthorizeException;

/**
 *
 * @author Daisie Huang <daisieh@datadryad.org>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DryadJournalConcept implements Comparable<DryadJournalConcept> {
    // Journal Concepts can have the following metadata properties defined in the old properties file:
    public static final String JOURNAL_ID = "journalID";
    public static final String FULLNAME = "fullname";
    public static final String CANONICAL_MANUSCRIPT_NUMBER_PATTERN = "canonicalManuscriptNumberPattern";
    public static final String SPONSOR_NAME = "sponsorName";
    public static final String PARSING_SCHEME = "parsingScheme";
    public static final String METADATADIR = "metadataDir";
    public static final String ALLOW_REVIEW_WORKFLOW = "allowReviewWorkflow";
    public static final String EMBARGO_ALLOWED = "embargoAllowed";
    public static final String INTEGRATED = "integrated";
    public static final String NOTIFY_ON_ARCHIVE = "notifyOnArchive";
    public static final String NOTIFY_ON_REVIEW = "notifyOnReview";
    public static final String NOTIFY_WEEKLY = "notifyWeekly";
    public static final String PUBLICATION_BLACKOUT = "publicationBlackout";
    public static final String SUBSCRIPTION_PAID = "subscriptionPaid";
    public static final String PAYMENT_PLAN = "paymentPlanType";

    // Journal Concepts can also have the following fields:
    public static final String ISSN = "issn";
    public static final String CUSTOMER_ID = "customerID";
    public static final String DESCRIPTION = "description";
    public static final String MEMBERNAME = "memberName";

    public static final String SUBSCRIPTION_PLAN = "SUBSCRIPTION";
    public static final String PREPAID_PLAN = "PREPAID";
    public static final String DEFERRED_PLAN = "DEFERRED";
    public static final String NO_PLAN = "NONE";

    public static Properties journalMetadata;

    private Context context = null;

    private static Logger log = Logger.getLogger(DryadJournalConcept.class);

    static {
        journalMetadata = new Properties();

        journalMetadata.setProperty(JOURNAL_ID, "journal.journalID");
        journalMetadata.setProperty(FULLNAME, "journal.fullname");
        journalMetadata.setProperty(CANONICAL_MANUSCRIPT_NUMBER_PATTERN, "journal.canonicalManuscriptNumberPattern");
        journalMetadata.setProperty(SPONSOR_NAME, "journal.sponsorName");
        journalMetadata.setProperty(PARSING_SCHEME, "journal.parsingScheme");
        journalMetadata.setProperty(METADATADIR, "journal.metadataDir");
        journalMetadata.setProperty(ALLOW_REVIEW_WORKFLOW, "journal.allowReviewWorkflow");
        journalMetadata.setProperty(EMBARGO_ALLOWED, "journal.embargoAllowed");
        journalMetadata.setProperty(INTEGRATED, "journal.integrated");
        journalMetadata.setProperty(NOTIFY_ON_ARCHIVE, "journal.notifyOnArchive");
        journalMetadata.setProperty(NOTIFY_ON_REVIEW, "journal.notifyOnReview");
        journalMetadata.setProperty(NOTIFY_WEEKLY, "journal.notifyWeekly");
        journalMetadata.setProperty(PUBLICATION_BLACKOUT, "journal.publicationBlackout");
        journalMetadata.setProperty(SUBSCRIPTION_PAID, "journal.subscriptionPaid");
        journalMetadata.setProperty(PAYMENT_PLAN, "journal.paymentPlanType");
        journalMetadata.setProperty(ISSN, "journal.issn");
        journalMetadata.setProperty(CUSTOMER_ID, "journal.customerID");
        journalMetadata.setProperty(DESCRIPTION, "journal.description");
        journalMetadata.setProperty(MEMBERNAME, "journal.memberName");
    }

    // these are mandatory elements
    private Concept underlyingConcept;
    private String conceptIdentifier;
    private int conceptID;
    private String fullName;

    public DryadJournalConcept() {
        Context context = null;
        try {
            context = new Context();
            create(context);
            context.commit();
        } catch (Exception e) {
            log.error("Couldn't make new concept: " + e.getMessage());
            if (context != null) {
                context.abort();
            }
        }
    } // JAXB needs this

    public DryadJournalConcept(Context context, Concept concept) {
        setUnderlyingConcept(context, concept);
        fullName = getConceptMetadataValue(journalMetadata.getProperty(FULLNAME));
    }

    public DryadJournalConcept(Context context, String fullName) throws StorageException {
        this();
        this.setFullName(fullName);
        this.setJournalID(fullName);
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
            JournalUtils.addDryadJournalConcept(context, this);
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
        this.conceptID = concept.getID();
    }

    @Override
    public int compareTo(DryadJournalConcept journalConcept) {
        return this.getFullName().toUpperCase().compareTo(journalConcept.getFullName().toUpperCase());
    }

    @JsonIgnore
    private String getConceptMetadataValue(String mdString) {
        AuthorityMetadataValue authorityValue = getUnderlyingConcept().getSingleMetadata(mdString);
        String result = "";
        if (authorityValue != null) {
            result = authorityValue.getValue();
            return result;
        }
        return result;
    }

    @JsonIgnore
    private void setConceptMetadataValue(String mdString, String value) {
        if ("".equals(value)) {
            return;
        }
        try {
            Context context = new Context();
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

    @JsonIgnore
    public void transferFromJournalConcept(Context context, DryadJournalConcept source) throws StorageException {
        // if FULLNAME isn't the same, this probably shouldn't be transferred
        if (!source.getFullName().equals(fullName)) {
            throw new StorageException("can't transfer metadata, names are not the same");
        }
        setStatus(source.getStatus());
        for (String prop : journalMetadata.stringPropertyNames()) {
            String sourceMetadataValue = source.getConceptMetadataValue(journalMetadata.getProperty(prop));
            this.setConceptMetadataValue(journalMetadata.getProperty(prop), sourceMetadataValue);
        }
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String value) throws StorageException {
        Context context = null;
        try {
            setConceptMetadataValue(journalMetadata.getProperty(FULLNAME), value);
            // update JournalUtils with this new concept
            JournalUtils.updateDryadJournalConcept(this);

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

    public String getJournalID() {
        return getConceptMetadataValue(journalMetadata.getProperty(JOURNAL_ID));
    }

    public void setJournalID(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(JOURNAL_ID), value);
        JournalUtils.updateDryadJournalConcept(this);
    }

    public String getCanonicalManuscriptNumberPattern() {
        return getConceptMetadataValue(journalMetadata.getProperty(CANONICAL_MANUSCRIPT_NUMBER_PATTERN));
    }

    public void setCanonicalManuscriptNumberPattern(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(CANONICAL_MANUSCRIPT_NUMBER_PATTERN), value);
    }

    public String getSponsorName() {
        return getConceptMetadataValue(journalMetadata.getProperty(SPONSOR_NAME));
    }

    public void setSponsorName(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(SPONSOR_NAME), value);
    }

    public String getParsingScheme() {
        return getConceptMetadataValue(journalMetadata.getProperty(PARSING_SCHEME));
    }

    public void setParsingScheme(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(PARSING_SCHEME), value);
    }

    public String getMetadataDir() {
        return getConceptMetadataValue(journalMetadata.getProperty(METADATADIR));
    }

    public void setMetadataDir(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(METADATADIR), value);
    }

    public String getISSN() {
        return getConceptMetadataValue(journalMetadata.getProperty(ISSN));
    }

    public void setISSN(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(ISSN), value);
    }

    public String getCustomerID() {
        return getConceptMetadataValue(journalMetadata.getProperty(CUSTOMER_ID));
    }

    public void setCustomerID(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(CUSTOMER_ID), value);
        JournalUtils.updateDryadJournalConcept(this);
    }

    public String getDescription() {
        return getConceptMetadataValue(journalMetadata.getProperty(DESCRIPTION));
    }

    public void setDescription(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(DESCRIPTION), value);
    }

    public String getMemberName() {
        return getConceptMetadataValue(journalMetadata.getProperty(MEMBERNAME));
    }

    public void setMemberName(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(MEMBERNAME), value);
    }

    @JsonIgnore
    public String getIdentifier() {
        return conceptIdentifier;
    }

    public String getStatus() {
        if (getUnderlyingConcept().getStatus() != null) {
            return getUnderlyingConcept().getStatus();
        }
        return "";
    }

    public void setStatus(String status) {
        if (Concept.Status.CANDIDATE.name().equals(status) || Concept.Status.ACCEPTED.name().equals(status)) {
            try {
                Context context = new Context();
                Concept concept = getUnderlyingConcept(context);
                concept.setStatus(context, status);
                context.complete();
            } catch (Exception e) {
                if (context != null) {
                    context.abort();
                }
            }
        }
    }

    public ArrayList<String> getEmailsToNotifyOnArchive() {
        ArrayList<String> emailArrayList = new ArrayList<String>();
        String emailString = getConceptMetadataValue(journalMetadata.getProperty(NOTIFY_ON_ARCHIVE));
        if (emailString != null && !(emailString.equals(""))) {
            String[] emails = emailString.split("\\s*,\\s*");
            for (int i = 0; i < emails.length; i++) {
                if (emails[i].matches(".+@.+\\..+")) {
                    emailArrayList.add(emails[i]);
                }
            }
        }
        return emailArrayList;
    }

    public void setEmailsToNotifyOnArchive(ArrayList<String> emails) {
        if (emails != null) {
            String emailString = StringUtils.join(emails.toArray(new String[emails.size()]),",");
            setConceptMetadataValue(journalMetadata.getProperty(NOTIFY_ON_ARCHIVE), emailString);
        }
    }

    @JsonIgnore
    public void setEmailsToNotifyOnArchiveString(String emailString) {
        if (emailString != null && !(emailString.equals(""))) {
            setConceptMetadataValue(journalMetadata.getProperty(NOTIFY_ON_ARCHIVE), emailString);
        }
    }

    public ArrayList<String> getEmailsToNotifyOnReview() {
        ArrayList<String> emailArrayList = new ArrayList<String>();
        String emailString = getConceptMetadataValue(journalMetadata.getProperty(NOTIFY_ON_REVIEW));
        if (emailString != null && !(emailString.equals(""))) {
            String[] emails = emailString.split("\\s*,\\s*");
            for (int i = 0; i < emails.length; i++) {
                if (emails[i].matches(".+@.+\\..+")) {
                    emailArrayList.add(emails[i]);
                }
            }
        }
        return emailArrayList;
    }

    public void setEmailsToNotifyOnReview(ArrayList<String> emails) {
        if (emails != null) {
            String emailString = StringUtils.join(emails.toArray(new String[emails.size()]),",");
            setConceptMetadataValue(journalMetadata.getProperty(NOTIFY_ON_REVIEW), emailString);
        }
    }

    @JsonIgnore
    public void setEmailsToNotifyOnReviewString(String emailString) {
        if (emailString != null && !(emailString.equals(""))) {
            setConceptMetadataValue(journalMetadata.getProperty(NOTIFY_ON_REVIEW), emailString);
        }
    }

    public ArrayList<String> getEmailsToNotifyWeekly() {
        ArrayList<String> emailArrayList = new ArrayList<String>();
        String emailString = getConceptMetadataValue(journalMetadata.getProperty(NOTIFY_WEEKLY));
        if (emailString != null && !(emailString.equals(""))) {
            String[] emails = emailString.split("\\s*,\\s*");
            for (int i = 0; i < emails.length; i++) {
                if (emails[i].matches(".+@.+\\..+")) {
                    emailArrayList.add(emails[i]);
                }
            }
        }
        return emailArrayList;
    }

    public void setEmailsToNotifyWeekly(ArrayList<String> emails) {
        if (emails != null) {
            String emailString = StringUtils.join(emails.toArray(new String[emails.size()]),",");
            setConceptMetadataValue(journalMetadata.getProperty(NOTIFY_WEEKLY), emailString);
        }
    }

    @JsonIgnore
    public void setEmailsToNotifyWeeklyString(String emailString) {
        if (emailString != null && !(emailString.equals(""))) {
            setConceptMetadataValue(journalMetadata.getProperty(NOTIFY_WEEKLY), emailString);
        }
    }

    public Boolean getAllowReviewWorkflow() {
        String metadataValue = getConceptMetadataValue(journalMetadata.getProperty(ALLOW_REVIEW_WORKFLOW));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    public void setAllowReviewWorkflow(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(ALLOW_REVIEW_WORKFLOW), value);
    }

    @JsonIgnore
    public void setBooleanAllowReviewWorkflow(Boolean value) {
        setAllowReviewWorkflow(value.toString());
    }

    public Boolean getAllowEmbargo() {
        String metadataValue = getConceptMetadataValue(journalMetadata.getProperty(EMBARGO_ALLOWED));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    public void setAllowEmbargo(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(EMBARGO_ALLOWED), value);
    }

    @JsonIgnore
    public void setBooleanAllowEmbargo(Boolean value) {
        setAllowEmbargo(value.toString());
    }

    public void setIntegrated(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(INTEGRATED), value);
    }

    public Boolean getIntegrated() {
        String metadataValue = getConceptMetadataValue(journalMetadata.getProperty(INTEGRATED));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    @JsonIgnore
    public void setBooleanIntegrated(Boolean value) {
        setIntegrated(value.toString());
    }

    public Boolean getPublicationBlackout() {
        String metadataValue = getConceptMetadataValue(journalMetadata.getProperty(PUBLICATION_BLACKOUT));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    public void setPublicationBlackout(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(PUBLICATION_BLACKOUT), value);
    }

    @JsonIgnore
    public void setBooleanPublicationBlackout(Boolean value) {
        setPublicationBlackout(value.toString());
    }

    @JsonIgnore
    public Boolean getSubscriptionPaid() {
        String paymentPlan = getConceptMetadataValue(journalMetadata.getProperty(PAYMENT_PLAN));
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
    private void setSubscriptionPaid(String value) {
        setConceptMetadataValue(journalMetadata.getProperty(SUBSCRIPTION_PAID), value);
    }

    public String getPaymentPlan() {
        return getConceptMetadataValue(journalMetadata.getProperty(PAYMENT_PLAN));
    }

    public void setPaymentPlan(String paymentPlan) {
        String paymentPlanType = "";
        Boolean newSubscriptionPaid = false;
        if (SUBSCRIPTION_PLAN.equals(paymentPlan)) {
            newSubscriptionPaid = true;
            paymentPlanType = SUBSCRIPTION_PLAN;
        } else if (DEFERRED_PLAN.equals(paymentPlan)) {
            newSubscriptionPaid = true;
            paymentPlanType = DEFERRED_PLAN;
        } else if (PREPAID_PLAN.equals(paymentPlan)) {
            newSubscriptionPaid = true;
            paymentPlanType = PREPAID_PLAN;
        }
        setSubscriptionPaid(newSubscriptionPaid.toString());
        setConceptMetadataValue(journalMetadata.getProperty(PAYMENT_PLAN), paymentPlanType);
    }

    @JsonIgnore
    public Boolean isValid() {
        return (getFullName() != null);
    }

    @JsonIgnore
    public static Boolean conceptIsValidJournal(Concept concept) {
        return ((concept != null) && (concept.getSingleMetadata(journalMetadata.getProperty(FULLNAME)) != null));
    }

    @JsonIgnore
    public Boolean isCandidate() {
        return Concept.Status.CANDIDATE.name().equals(getStatus());
    }

    @JsonIgnore
    public Boolean isAccepted() {
        return Concept.Status.ACCEPTED.name().equals(getStatus());
    }

    @JsonIgnore
    public void setConceptStatusToAccepted() throws SQLException {
        getUnderlyingConcept().setStatus(context, Concept.Status.ACCEPTED.name());
    }

    @JsonIgnore
    public Organization getOrganizationFromJournalConcept() {
        Organization organization = new Organization();
        organization.organizationId = getUnderlyingConcept().getID();
        organization.organizationCode = getJournalID();
        organization.organizationName = getFullName();
        organization.organizationISSN = getISSN();
        return organization;
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
