package org.dspace.submit.step;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.Concept;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.usagelogging.EventLogger;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * User: @author kevinvandevelde (kevin at atmire.com)
 * Date: 27-jan-2010
 * Time: 15:12:46
 *
 * The processing of the first step in which a journal can be selected.
 */
public class SelectPublicationStep extends AbstractProcessingStep {

    public static final int STATUS_INVALID_PUBLICATION_ID = 1;
    public static final int STATUS_LICENSE_NOT_ACCEPTED = 2;
    public static final int ERROR_SELECT_JOURNAL = 3;
    public static final int ERROR_INVALID_JOURNAL = 4;
    public static final int ERROR_PUBMED_DOI = 8;
    public static final int ERROR_GENERIC = 9;
    public static final int ERROR_PUBMED_NAME = 11;

    public static final int DISPLAY_MANUSCRIPT_NUMBER = 5;
    public static final int DISPLAY_CONFIRM_MANUSCRIPT_ACCEPTANCE = 6;
    public static final int ENTER_MANUSCRIPT_NUMBER = 7;
    public static final int ERROR_SELECT_COUNTRY = 10;

    private static Logger log = Logger.getLogger(SelectPublicationStep.class);

    public final static int  ARTICLE_STATUS_PUBLISHED=0;
    public final static int  ARTICLE_STATUS_ACCEPTED=1;
    public final static int  ARTICLE_STATUS_IN_REVIEW=2;
    public final static int  ARTICLE_STATUS_NOT_YET_SUBMITTED=3;

    public final static int  UNKNOWN_DOI=5;
    public final static int  MANU_ACC=6;

    public final static String crossRefApiRoot = "http://api.crossref.org/works/";
    public final static String crossRefApiFormat = "/transform/application/vnd.crossref.unixref+xml";

    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo submissionInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        try {
            // First of all check if we have accepted our license
            if (request.getParameter("license_accept") == null || !Boolean.valueOf(request.getParameter("license_accept"))) {
                EventLogger.log(context, "submission-select-publication", "error=failed_license_accept");
                return STATUS_LICENSE_NOT_ACCEPTED;
            }

            String articleStatus = request.getParameter("article_status");

            // get the journalID selected by the user in the UI
            if (articleStatus==null) {
                EventLogger.log(context, "submission-select-publication", "error=exception_reselect_journal");
                return ERROR_SELECT_JOURNAL;
            } else {
                Item item = submissionInfo.getSubmissionItem().getItem();
                String manuscriptNumber = request.getParameter("manu");
                if (Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED) {
                    String manuscriptNumberAcc = request.getParameter("manu-number-status-accepted");
                    manuscriptNumber = manuscriptNumberAcc;
                    manuscriptNumber = manuscriptNumber.trim();

                    // try to get authority id first, its better than name
                    String journalUuid = request.getParameter("prism_publicationName_authority");
                    if(journalUuid != null) {
                        journalUuid = journalUuid.trim();
                    }
                    
                    String journal = request.getParameter("prism_publicationName");
                    if(journal!= null) {
                        journal=journal.replace("*", "");
                        journal=journal.trim();
                    }

                    if(journal==null||journal.equals("")){
                        EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                        return ERROR_INVALID_JOURNAL;
                    } else if(!processJournal(journal, null, journalUuid, manuscriptNumber, item, context, request, articleStatus)){
                        EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                        return ENTER_MANUSCRIPT_NUMBER;
                    }

                    EventLogger.log(context, "submission-select-publication", "journalID=" + journal +
                            ",articleStatus=" + articleStatus + ",manuscriptNumber=" + manuscriptNumber);

                } else if (Integer.parseInt(articleStatus)==ARTICLE_STATUS_PUBLISHED) {
                    //attempt to process a DOI or PMID entered in the UI
                    String identifier = request.getParameter("article_doi");

                    // This is the Journal name if they don't know the publication
                    String journal = request.getParameter("unknown_doi");

                    if (identifier!=null && !identifier.equals("")) {
                        if (identifier.indexOf('/')!=-1) {
                            if (!processDOI(context, item, identifier)) {
                                EventLogger.log(context, "submission-select-publication", "doi=" + identifier + ",error=failed_doi_lookup");
                                return ERROR_PUBMED_DOI;
                            } else {
                                EventLogger.log(context, "submission-select-publication", "doi=" + identifier);
                            }
                        } else {
                            if (!processPubMed(context, item, identifier)) {
                                EventLogger.log(context, "submission-select-publication", "pmid=" + identifier + ",error=failed_pubmed_lookup");
                                return ERROR_PUBMED_DOI;
                            } else {
                                EventLogger.log(context, "submission-select-publication", "pmid=" + identifier);
                            }
                        }
                    } else {
                        if (journal==null||journal.length()==0) {
                            EventLogger.log(context, "submission-select-publication", "error=no_journal_name");
                            return ERROR_PUBMED_NAME;
                        } else {
                            journal=journal.replace("*", "");
                            journal=journal.trim();

                            if (journal==null||journal.equals("")){
                                EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                                return ERROR_INVALID_JOURNAL;
                            } else if(!processJournal(journal, null, null, manuscriptNumber, item, context, request, articleStatus)){
                                if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED) return ENTER_MANUSCRIPT_NUMBER;

                                EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                                return ERROR_SELECT_JOURNAL;
                            }
                            EventLogger.log(context, "submission-select-publication", "journalID=" + journal +
                                    ",articleStatus=" + articleStatus + ",manuscriptNumber=" + manuscriptNumber);
                        }
                    }
                } else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_NOT_YET_SUBMITTED) {
                    String journal = request.getParameter("journalIDStatusNotYetSubmitted");

                    if(journal==null||journal.equals("")){
                        EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                        return ERROR_INVALID_JOURNAL;
                    } else if(!processJournal(journal, null, null, manuscriptNumber, item, context, request, articleStatus)){

                        EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                        return ERROR_SELECT_JOURNAL;
                    }
                } else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_IN_REVIEW) {
                    String journalID = request.getParameter("journalIDStatusInReview");

                    if(journalID==null||journalID.equals("")){
                        EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                        return ERROR_INVALID_JOURNAL;
                    } else if(!processJournal(null, journalID, null, manuscriptNumber, item, context, request, articleStatus)){
                        EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                        return ERROR_SELECT_JOURNAL;
                    }

                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY,"false");
                    item.update();
                } else {
                    // ########### TRYING TO SUBMIT FORM WITHOUT ARTICLE STATUS ###########
                    EventLogger.log(context, "submission-select-publication", "error=exception_reselect_journal");
                    return ERROR_SELECT_JOURNAL;
                }
            }

            EventLogger.log(context, "submission-select-publication", "status=complete");
            return STATUS_COMPLETE;

        }catch(Exception e){
            log.error(e);
        }

        EventLogger.log(context, "submission-select-publication", "error=exception_reselect_journal");
        return ERROR_SELECT_JOURNAL;
    }

    /**
     Process a DOI entered by the submitter. Use the DOI metadata to initialize publication information.
     **/
    private boolean processDOI(Context context, Item item, String identifier){

        // normalize and validate the identifier
        identifier = identifier.toLowerCase().trim();
        if(identifier.startsWith("doi:")) {
            identifier = identifier.replaceFirst("doi:", "");
        }

        try{
            Element jElement = retrieveXML(crossRefApiRoot + identifier + crossRefApiFormat);
            if(jElement != null){

                List<Element> children = jElement.getChildren();
                if(children.size()==0){
                    return false;
                }

                if(!isAValidDOI(jElement)) return false;

                // Use the ingest process to parse the XML document, transformation is done
                // using XSLT
                IngestionCrosswalk xwalk = (IngestionCrosswalk) PluginManager.getNamedPlugin(IngestionCrosswalk.class, "DOI");

                xwalk.ingest(context, item, jElement);
                return true;
            }
        }catch (Exception ex){
            log.error("unable to process DOI metadata", ex);
            return false;
        }
        return false;

    }


    /**
     Process a PMID entered by the submitter. Use the PMID metadata to initialize publication information.
     **/
    private boolean processPubMed(Context context, Item item, String identifier){

        // normalize and validate the identifier
        identifier = identifier.toLowerCase().trim();
        if(identifier.startsWith("pmid: ")) {
            identifier = identifier.substring("pmid: ".length());
        }
        if(identifier.startsWith("pmid ")) {
            identifier = identifier.substring("pmid ".length());
        }
        if(identifier.startsWith("pmid:")) {
            identifier = identifier.substring("pmid:".length());
        }
        if(identifier.startsWith("pmid")) {
            identifier = identifier.substring("pmid".length());
        }
        if(!isValidPubmedID(identifier)) return false;

        try{
            Element jElement = retrieveXML("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + identifier);
            if(jElement != null){

                List<Element> children = jElement.getChildren();
                if(jElement.getName().equals("ERROR") || children.size()==0){
                    return false;
                }

                // Use the ingest process to parse the XML document, transformation is done
                // using XSLT
                IngestionCrosswalk xwalk = (IngestionCrosswalk) PluginManager.getNamedPlugin(IngestionCrosswalk.class, "PUBMED");
                xwalk.ingest(context, item, jElement);
                return true;
            }
        }catch (Exception ex){
            log.error("unable to process PMID metadata", ex);
            return false;
        }
        return false;
    }

    private Element retrieveXML(String urls) throws Exception{
        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document doc = builder.build(urls);
        return doc.getRootElement();
    }



    private boolean isValidPubmedID(String pmid){
        try{
            // A valid PMID will be parseable as an integer
            return (Integer.parseInt(pmid, 10) > 0);
        }
        catch (NumberFormatException nfe){
            return false;
        }
    }


    private static boolean isAValidDOI(Element element) {
        List<Element> children = element.getChildren();
        for(Element e : children){
            if(e.getName().equals("doi_record")){
                List<Element> doiRecordsChildren = e.getChildren();
                for(Element e1 : doiRecordsChildren){

                    if(e1.getName().equals("crossref")){
                        List<Element> crossRefChildren = e1.getChildren();
                        for(Element e2 : crossRefChildren){
                            if(e2.getName().equals("error")){
                                return false;
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return true;
    }

    private void addEmailsAndEmbargoSettings(DryadJournalConcept journalConcept, Item item) {
        ArrayList<String> reviewEmailList = journalConcept.getEmailsToNotifyOnReview();
        String[] reviewEmails = reviewEmailList.toArray(new String[reviewEmailList.size()]);

        if(reviewEmails != null) {
            item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "review", "mailUsers", null, reviewEmails);
        }

        ArrayList<String> archiveEmailList = journalConcept.getEmailsToNotifyOnArchive();
        String[] archiveEmails = archiveEmailList.toArray(new String[archiveEmailList.size()]);

        if(archiveEmails != null) {
            item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "archive", "mailUsers", null, archiveEmails);
        }

        Boolean embargoAllowed = journalConcept.getAllowEmbargo();
        if(embargoAllowed != null && !embargoAllowed){
            //We don't need to show the embargo option to any of our data files
            item.addMetadata("internal", "submit", "showEmbargo", null, String.valueOf(embargoAllowed));
        }
    }


    private boolean processJournal(String journalName, String journalShortID, String journalUuid, String manuscriptNumber, Item item, Context context,
                                   HttpServletRequest request, String articleStatus) throws AuthorizeException, SQLException {
        DryadJournalConcept journalConcept = null;

        if (journalConcept==null && journalShortID != null && journalShortID.length() > 0) {
            journalConcept = JournalUtils.getJournalConceptByJournalID(journalShortID);
        }

        if (journalConcept==null && journalName != null && journalName.length() > 0) {
            journalConcept = JournalUtils.getJournalConceptByJournalName(journalName);
        }

        // if we still haven't found a matching journal concept, make a new, temporary one.
        if (journalConcept == null) {
            try {
                journalConcept = JournalUtils.createJournalConcept(journalName);
            } catch (Exception e) {
                // this should not happen because we've already checked to see if a matching concept existed.
                log.error("couldn't create a concept");
            }
        }

        Manuscript manuscript = new Manuscript(journalConcept);
        request.getSession().setAttribute("submit_error", "");
        if (journalConcept.getIntegrated()) {
            addEmailsAndEmbargoSettings(journalConcept, item);
            if (manuscriptNumber != null && manuscriptNumber.trim().equals("")) {
                // we just use this empty manuscript with the journal only.
                log.debug("manuscript number is empty or nonexistent");
            } else {
                manuscript = JournalUtils.getManuscriptFromManuscriptStorage(manuscriptNumber, journalConcept);
                if (manuscript.getMessage().equals("")) {
                    // No matter which radio button was chosen, if the manuscript is rejected, say so.
                    if (manuscript.getStatus() != null && Manuscript.statusIsRejected(manuscript.getStatus())) {
                        request.getSession().setAttribute("submit_error", "This manuscript has been rejected by the journal.");
                        return false;
                    }

                    if (articleStatus != null) {
                        boolean manuscriptNumberInvalid = true;
                        // the Article Status chosen must match the specified manuscript's status. Otherwise, it's invalid.
                        if (Integer.parseInt(articleStatus) == ARTICLE_STATUS_ACCEPTED) {
                            if (manuscript.isAccepted() || manuscript.isPublished()) {
                                item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY, "false");
                                manuscriptNumberInvalid = false;
                            }
                        } else if (Integer.parseInt(articleStatus) == ARTICLE_STATUS_IN_REVIEW) {
                            if (manuscript.isSubmitted() || manuscript.isNeedsRevision()) {
                                item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY, "false");
                                manuscriptNumberInvalid = false;
                            }
                        }

                        if (manuscriptNumberInvalid) {
                            request.getSession().setAttribute("submit_error", "This manuscript is not in the status you selected.");
                            return false;
                        }
                    }
                } else if (manuscript.getMessage().equals("Invalid manuscript number")) {
                    // We do not have metadata for this manuscript number
                    // Store the manuscriptNumber and continue as in-review
                    manuscript.setManuscriptId(manuscriptNumber);
                } else {
                    request.getSession().setAttribute("submit_error", manuscript.getMessage());
                    return false;
                }
            }
        } else {
            log.debug("Journal " + journalConcept.getJournalID() + " is not integrated");
        }
        manuscript.propagateMetadataToItem(context, item);
        item.update();
        return true;
    }

    public int getNumberOfPages(HttpServletRequest request, SubmissionInfo submissionInfo) throws ServletException {
        return 1;
    }

    @Override
    public boolean isStepAccessible(Context context, Item item) {
        //If we already have a handle there is no need to use this step
        boolean stepAccessible = true;
        if(item.getHandle() == null){
            try {
                WorkspaceItem wsItem = WorkspaceItem.findByItemId(context, item.getID());
                if(wsItem != null){
                    //Only allow this step if the user hasn't passed it
                    stepAccessible = 1 == wsItem.getStageReached() || -1 == wsItem.getStageReached();
                }
            } catch (SQLException e) {
                log.error("Error in isStepAccessible: " + e.getMessage(), e);
            }
        }else{
            stepAccessible = false;
        }

        return stepAccessible;
    }


}
