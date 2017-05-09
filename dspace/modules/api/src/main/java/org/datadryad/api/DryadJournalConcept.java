package org.datadryad.api;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.datadryad.rest.storage.StorageException;
import org.dspace.JournalUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.lang.*;
import java.lang.Exception;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author Daisie Huang <daisieh@datadryad.org>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DryadJournalConcept extends DryadOrganizationConcept {
    // Journal Concepts can have the following metadata properties defined in the old properties file:
    public static final String JOURNAL_ID = "journalID";
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

    // Journal Concepts can also have the following fields:
    public static final String ISSN = "issn";
    public static final String MEMBERNAME = "memberName";
    public static final String COVER_IMAGE = "coverImage";

    public static final String HASJOURNALPAGE = "hasJournalPage";

    private static Logger log = Logger.getLogger(DryadJournalConcept.class);

    static {
        metadataProperties.setProperty(JOURNAL_ID, "journal.journalID");
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
        metadataProperties.setProperty(ISSN, "journal.issn");
        metadataProperties.setProperty(MEMBERNAME, "journal.memberName");
        metadataProperties.setProperty(HASJOURNALPAGE, "journal.hasJournalPage");
        metadataProperties.setProperty(COVER_IMAGE, "journal.coverImage");

        defaultMetadataValues.setProperty(metadataProperties.getProperty(JOURNAL_ID), "");
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
        defaultMetadataValues.setProperty(metadataProperties.getProperty(ISSN), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(MEMBERNAME), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(COVER_IMAGE), "");
        defaultMetadataValues.setProperty(metadataProperties.getProperty(HASJOURNALPAGE), "");
    }

    // these are mandatory elements

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
        super();
        setUnderlyingConcept(context, concept);
        fullName = getConceptMetadataValue(metadataProperties.getProperty(FULLNAME));
    }

    public DryadJournalConcept(Context context, String fullName) throws StorageException {
        create(context);
        this.setFullName(fullName);
        try {
            context.commit();
        } catch (Exception e) {
            log.error("exception " + e.getMessage());
        }
    }

    @Override
    public void create(Context context) {
        try {
            context.turnOffAuthorisationSystem();
            Scheme journalScheme = Scheme.findByIdentifier(context, ConfigurationManager.getProperty("solrauthority.searchscheme.prism_publicationName"));
            Concept newConcept = journalScheme.createConcept(context);
            this.setUnderlyingConcept(context, newConcept);
            JournalUtils.addDryadJournalConcept(context, this);
            context.commit();
            context.restoreAuthSystemState();
            for (String prop : metadataProperties.stringPropertyNames()) {
                String mdString = metadataProperties.getProperty(prop);
                this.setConceptMetadataValue(mdString, defaultMetadataValues.getProperty(mdString));
            }
        } catch (Exception e) {
            log.error("Couldn't make new concept: " + e.getMessage());
        }
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

    public void setFullName(String value) throws StorageException {
        super.setFullName(value);
        // update JournalUtils with this new concept
        JournalUtils.updateDryadJournalConcept(this);
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

    @Override
    public void setCustomerID(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(CUSTOMER_ID), value);
        JournalUtils.updateDryadJournalConcept(this);
    }

    public String getCoverImage() {
        return getConceptMetadataValue(metadataProperties.getProperty(COVER_IMAGE));
    }

    public void setCoverImage(String value) {
        setConceptMetadataValue(metadataProperties.getProperty(COVER_IMAGE), value);
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

    public static DryadJournalConcept getJournalConceptMatchingConceptID(Context context, int conceptID) {
        DryadJournalConcept journalConcept = null;
        try {
            Concept concept = Concept.find(context, conceptID);
            journalConcept = new DryadJournalConcept(context, concept);
        } catch (SQLException e) {
            log.error("couldn't find a journal concept: " + e.getMessage());
        }
        return journalConcept;
    }
}
