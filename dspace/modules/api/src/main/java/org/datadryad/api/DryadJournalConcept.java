package org.datadryad.api;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.authority.Term;
import org.dspace.content.authority.AuthorityMetadataValue;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.datadryad.rest.models.Journal;
import org.datadryad.rest.storage.StorageException;
import org.dspace.JournalUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.lang.*;
import java.lang.Exception;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

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
    public static final String PAYMENT_PLAN = "paymentPlanType";

    // Journal Concepts can also have the following fields:
    public static final String ISSN = "issn";
    public static final String CUSTOMER_ID = "customerID";
    public static final String DESCRIPTION = "description";
    public static final String MEMBERNAME = "memberName";
    public static final String WEBSITE = "website";
    public static final String COVER_IMAGE = "coverImage";

    public static final String HASJOURNALPAGE = "hasJournalPage";

    public static final String SUBSCRIPTION_PLAN = "SUBSCRIPTION";
    public static final String PREPAID_PLAN = "PREPAID";
    public static final String DEFERRED_PLAN = "DEFERRED";
    public static final String NO_PLAN = "NONE";

    private static Properties metadataProperties;
    private static Properties defaultMetadataValues;

    private static Logger log = Logger.getLogger(DryadJournalConcept.class);

    static {
        metadataProperties = new Properties();

        metadataProperties.setProperty(JOURNAL_ID, "journal.journalID");
        metadataProperties.setProperty(FULLNAME, "journal.fullname");
        metadataProperties.setProperty(CANONICAL_MANUSCRIPT_NUMBER_PATTERN, "journal.canonicalManuscriptNumberPattern");
        metadataProperties.setProperty(SPONSOR_NAME, "journal.sponsorName");
        metadataProperties.setProperty(PARSING_SCHEME, "journal.parsingScheme");
        metadataProperties.setProperty(METADATADIR, "journal.metadataDir");
        metadataProperties.setProperty(ALLOW_REVIEW_WORKFLOW, "journal.allowReviewWorkflow");
        metadataProperties.setProperty(EMBARGO_ALLOWED, "journal.embargoAllowed");
        metadataProperties.setProperty(INTEGRATED, "journal.integrated");
        metadataProperties.setProperty(NOTIFY_ON_ARCHIVE, "journal.notifyOnArchive");
        metadataProperties.setProperty(NOTIFY_ON_REVIEW, "journal.notifyOnReview");
        metadataProperties.setProperty(NOTIFY_WEEKLY, "journal.notifyWeekly");
        metadataProperties.setProperty(PUBLICATION_BLACKOUT, "journal.publicationBlackout");
        metadataProperties.setProperty(PAYMENT_PLAN, "journal.paymentPlanType");
        metadataProperties.setProperty(ISSN, "journal.issn");
        metadataProperties.setProperty(CUSTOMER_ID, "journal.customerID");
        metadataProperties.setProperty(DESCRIPTION, "journal.description");
        metadataProperties.setProperty(MEMBERNAME, "journal.memberName");
        metadataProperties.setProperty(HASJOURNALPAGE, "journal.hasJournalPage");
        metadataProperties.setProperty(WEBSITE, "journal.website");
        metadataProperties.setProperty(COVER_IMAGE, "journal.coverImage");

        defaultMetadataValues = new Properties();
        defaultMetadataValues.setProperty(metadataProperties.getProperty(JOURNAL_ID), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(FULLNAME), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(CANONICAL_MANUSCRIPT_NUMBER_PATTERN), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(SPONSOR_NAME), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(PARSING_SCHEME), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(METADATADIR), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(ALLOW_REVIEW_WORKFLOW), "false");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(EMBARGO_ALLOWED), "true");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(INTEGRATED), "false");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(NOTIFY_ON_ARCHIVE), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(NOTIFY_ON_REVIEW), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(NOTIFY_WEEKLY), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(PUBLICATION_BLACKOUT), "true");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(PAYMENT_PLAN), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(ISSN), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(CUSTOMER_ID), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(DESCRIPTION), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(MEMBERNAME), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(WEBSITE), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(COVER_IMAGE), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(HASJOURNALPAGE), "");
    }

    // these are mandatory elements
    private Concept underlyingConcept;
    private String conceptIdentifier;
    private String fullName;

    public DryadJournalConcept() {
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

    public DryadJournalConcept(Context context, Concept concept) {
        setUnderlyingConcept(context, concept);
        fullName = getConceptMetadataValue(metadataProperties.getProperty(FULLNAME));
    }

    public DryadJournalConcept(Context context, String fullName) throws StorageException {
        this();
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
    }

    @Override
    public int compareTo(DryadJournalConcept journalConcept) {
//        a negative number if our object comes before the one passed in;
//        a positive number if our object comes after the one passed in;
//        otherwise, zero (meaning they're equal in terms of ordering).
        if ("".equals(this.getFullName())) {
            return -1;
        } else if ("".equals(journalConcept.getFullName())) {
            return 1;
        }
        return this.getFullName().toUpperCase().compareTo(journalConcept.getFullName().toUpperCase());
    }

    @JsonIgnore
    private String getConceptMetadataValue(String mdString) {
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
    private void setConceptMetadataValue(String mdString, String value) {
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

    @JsonIgnore
    public static Set<String> getMetadataPropertyNames() {
        return metadataProperties.stringPropertyNames();
    }

    @JsonIgnore
    public void transferFromJournalConcept(Context context, DryadJournalConcept source) throws StorageException {
        // if FULLNAME isn't the same, this probably shouldn't be transferred
        if (!source.getFullName().equals(fullName)) {
            throw new StorageException("can't transfer metadata, names are not the same");
        }
        setStatus(source.getStatus());
        for (String prop : metadataProperties.stringPropertyNames()) {
            String sourceMetadataValue = source.getConceptMetadataValue(metadataProperties.getProperty(prop));
            this.setConceptMetadataValue(metadataProperties.getProperty(prop), sourceMetadataValue);
        }
    }

    public String getFullName() {
        if (fullName == null) {
            return "";
        }
        return fullName;
    }

    public void setFullName(String value) throws StorageException {
        Context context = null;
        if (value == null) {
            value = "";
        }
        try {
            setConceptMetadataValue(metadataProperties.getProperty(FULLNAME), value);
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
        return getConceptMetadataValue(metadataProperties.getProperty(JOURNAL_ID));
    }

    public void setJournalID(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(JOURNAL_ID), value);
        JournalUtils.updateDryadJournalConcept(this);
    }

    public String getCanonicalManuscriptNumberPattern() {
        return getConceptMetadataValue(metadataProperties.getProperty(CANONICAL_MANUSCRIPT_NUMBER_PATTERN));
    }

    public void setCanonicalManuscriptNumberPattern(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(CANONICAL_MANUSCRIPT_NUMBER_PATTERN), value);
    }

    public String getSponsorName() {
        return getConceptMetadataValue(metadataProperties.getProperty(SPONSOR_NAME));
    }

    public void setSponsorName(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(SPONSOR_NAME), value);
    }

    public String getParsingScheme() {
        return getConceptMetadataValue(metadataProperties.getProperty(PARSING_SCHEME));
    }

    public void setParsingScheme(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(PARSING_SCHEME), value);
    }

    public String getMetadataDir() {
        return getConceptMetadataValue(metadataProperties.getProperty(METADATADIR));
    }

    public void setMetadataDir(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(METADATADIR), value);
    }

    public String getISSN() {
        return getConceptMetadataValue(metadataProperties.getProperty(ISSN));
    }

    public void setISSN(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(ISSN), value);
    }

    public String getCustomerID() {
        return getConceptMetadataValue(metadataProperties.getProperty(CUSTOMER_ID));
    }

    public void setCustomerID(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(CUSTOMER_ID), value);
        JournalUtils.updateDryadJournalConcept(this);
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
        JournalUtils.updateDryadJournalConcept(this);
    }

    public String getCoverImage() {
        return getConceptMetadataValue(metadataProperties.getProperty(COVER_IMAGE));
    }

    public void setCoverImage(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(COVER_IMAGE), value);
        JournalUtils.updateDryadJournalConcept(this);
    }

    public String getMemberName() {
        return getConceptMetadataValue(metadataProperties.getProperty(MEMBERNAME));
    }

    public void setMemberName(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(MEMBERNAME), value);
    }

    public Boolean getHasJournalPage() {
        String metadataValue = getConceptMetadataValue(metadataProperties.getProperty(HASJOURNALPAGE));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    public void setHasJournalPage(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(HASJOURNALPAGE), value);
    }

    @JsonIgnore
    public void setBooleanHasJournalPage(Boolean value) {
        setHasJournalPage(value.toString());
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
        Context context = null;
        if (Concept.Status.CANDIDATE.name().equals(status) || Concept.Status.ACCEPTED.name().equals(status)) {
            try {
                context = new Context();
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
        String emailString = getConceptMetadataValue(metadataProperties.getProperty(NOTIFY_ON_ARCHIVE));
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
            setConceptMetadataValue(metadataProperties.getProperty(NOTIFY_ON_ARCHIVE), emailString);
        }
    }

    @JsonIgnore
    public void setEmailsToNotifyOnArchiveString(String emailString) {
        if (emailString != null) {
            setConceptMetadataValue(metadataProperties.getProperty(NOTIFY_ON_ARCHIVE), emailString);
        }
    }

    public ArrayList<String> getEmailsToNotifyOnReview() {
        ArrayList<String> emailArrayList = new ArrayList<String>();
        String emailString = getConceptMetadataValue(metadataProperties.getProperty(NOTIFY_ON_REVIEW));
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
            setConceptMetadataValue(metadataProperties.getProperty(NOTIFY_ON_REVIEW), emailString);
        }
    }

    @JsonIgnore
    public void setEmailsToNotifyOnReviewString(String emailString) {
        if (emailString != null) {
            setConceptMetadataValue(metadataProperties.getProperty(NOTIFY_ON_REVIEW), emailString);
        }
    }

    public ArrayList<String> getEmailsToNotifyWeekly() {
        ArrayList<String> emailArrayList = new ArrayList<String>();
        String emailString = getConceptMetadataValue(metadataProperties.getProperty(NOTIFY_WEEKLY));
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
            setConceptMetadataValue(metadataProperties.getProperty(NOTIFY_WEEKLY), emailString);
        }
    }

    @JsonIgnore
    public void setEmailsToNotifyWeeklyString(String emailString) {
        if (emailString != null) {
            setConceptMetadataValue(metadataProperties.getProperty(NOTIFY_WEEKLY), emailString);
        }
    }

    public Boolean getAllowReviewWorkflow() {
        String metadataValue = getConceptMetadataValue(metadataProperties.getProperty(ALLOW_REVIEW_WORKFLOW));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    public void setAllowReviewWorkflow(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(ALLOW_REVIEW_WORKFLOW), value);
    }

    @JsonIgnore
    public void setBooleanAllowReviewWorkflow(Boolean value) {
        setAllowReviewWorkflow(value.toString());
    }

    public Boolean getAllowEmbargo() {
        String metadataValue = getConceptMetadataValue(metadataProperties.getProperty(EMBARGO_ALLOWED));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    public void setAllowEmbargo(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(EMBARGO_ALLOWED), value);
    }

    @JsonIgnore
    public void setBooleanAllowEmbargo(Boolean value) {
        setAllowEmbargo(value.toString());
    }

    public void setIntegrated(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(INTEGRATED), value);
    }

    public Boolean getIntegrated() {
        String metadataValue = getConceptMetadataValue(metadataProperties.getProperty(INTEGRATED));
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
        String metadataValue = getConceptMetadataValue(metadataProperties.getProperty(PUBLICATION_BLACKOUT));
        Boolean result = false;
        if (metadataValue.equals("true")) {
            result = true;
        }
        return result;
    }

    public void setPublicationBlackout(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(PUBLICATION_BLACKOUT), value);
    }

    @JsonIgnore
    public void setBooleanPublicationBlackout(Boolean value) {
        setPublicationBlackout(value.toString());
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

    public String getPaymentPlan() {
        return getConceptMetadataValue(metadataProperties.getProperty(PAYMENT_PLAN));
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
        setConceptMetadataValue(metadataProperties.getProperty(PAYMENT_PLAN), paymentPlanType);
    }

    @JsonIgnore
    public Boolean isValid() {
        return (getFullName() != null);
    }

    @JsonIgnore
    public static Boolean conceptIsValidJournal(Concept concept) {
        return ((concept != null) && (concept.getSingleMetadata(metadataProperties.getProperty(FULLNAME)) != null));
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
    public Journal getJournalFromJournalConcept() {
        Journal journal = new Journal();
        journal.conceptID = getUnderlyingConcept().getID();
        journal.journalCode = getJournalID();
        journal.fullName = getFullName();
        journal.issn = getISSN();
        return journal;
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
