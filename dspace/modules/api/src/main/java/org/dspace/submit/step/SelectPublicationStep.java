package org.dspace.submit.step;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadFunderConcept;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Manuscript;
import org.dspace.JournalUtils;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;
import org.dspace.paymentsystem.PaymentSystemService;
import org.dspace.paymentsystem.ShoppingCart;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.usagelogging.EventLogger;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static final int ERROR_INVALID_GRANT = 12;

    public static final int DISPLAY_MANUSCRIPT_NUMBER = 5;
    public static final int DISPLAY_CONFIRM_MANUSCRIPT_ACCEPTANCE = 6;
    public static final int ENTER_MANUSCRIPT_NUMBER = 7;
    public static final int ERROR_SELECT_COUNTRY = 10;

    private static Logger log = Logger.getLogger(SelectPublicationStep.class);

    public final static int  ARTICLE_STATUS_PUBLISHED=0;
    public final static int  ARTICLE_STATUS_ACCEPTED=1;
    public final static int  ARTICLE_STATUS_IN_REVIEW=2;

    public final static int  UNKNOWN_DOI=5;
    public final static int  MANU_ACC=6;

    public final static String crossRefApiRoot = "http://api.crossref.org/works/";
    public final static String crossRefApiFormat = "/transform/application/vnd.crossref.unixref+xml";

    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo submissionInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        Item item = submissionInfo.getSubmissionItem().getItem();
        // First of all check if we have accepted our license
        if (request.getParameter("license_accept") == null || !Boolean.valueOf(request.getParameter("license_accept"))) {
            EventLogger.log(context, "submission-select-publication", "error=failed_license_accept");
            return STATUS_LICENSE_NOT_ACCEPTED;
        }

        if (!processJournal(item, context, request)) {
            EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
            return ERROR_SELECT_JOURNAL;
        }

        // clear the sponsor from the shoppingcart:
        PaymentSystemService paymentSystemService = new DSpace().getSingletonService(PaymentSystemService.class);
        ShoppingCart shoppingCart = null;
        try {
            shoppingCart = paymentSystemService.getShoppingCartByItemId(context,item.getID());
            if (shoppingCart != null) {
                shoppingCart.setSponsoringOrganization(null);
            }
        } catch (Exception e) {
            log.error("couldn't find cart for item " + item.getID());
        }

        item.clearMetadata("dryad.fundingEntity");
        item.update();
        String fundingStatus = request.getParameter("funding-status");
        String grantInfo = request.getParameter("grant-info");
        int confidence = 0;
        if (grantInfo != null && !grantInfo.equals("")) {
            if (!JournalUtils.isValidNSFGrantNumber(grantInfo)) {
//                return ERROR_INVALID_GRANT;
                log.error("invalid grant");
                confidence = Choices.CF_REJECTED;
            } else {
                log.error("valid grant");
                confidence = Choices.CF_ACCEPTED;
            }
            DryadFunderConcept nsfConcept = DryadFunderConcept.getFunderConceptMatchingFunderID(context, DryadFunderConcept.NSF_ID);
            item.addMetadata(DryadFunderConcept.createFundingEntityMetadata(nsfConcept, grantInfo, confidence));
            item.update();
        }
        EventLogger.log(context, "submission-select-publication", "status=complete");
        return STATUS_COMPLETE;
    }

    /**
     Process a DOI entered by the submitter. Use the DOI metadata to initialize publication information.
     **/
    private boolean processDOI(Context context, Item item, String identifier){
        try {
            Element jElement = retrieveXML(crossRefApiRoot + identifier + crossRefApiFormat);
            if (jElement != null) {
                if (!jElement.getName().equals("doi_records") || jElement.getChildren().size()==0) {
                    return false;
                }
                // Use the ingest process to parse the XML document, transformation is done using XSLT
                IngestionCrosswalk xwalk = (IngestionCrosswalk) PluginManager.getNamedPlugin(IngestionCrosswalk.class, "DOI");
                xwalk.ingest(context, item, jElement);
                return true;
            }
        } catch (Exception ex) {
            log.error("unable to process DOI metadata", ex);
        }
        return false;
    }


    /**
     Process a PMID entered by the submitter. Use the PMID metadata to initialize publication information.
     **/
    private boolean processPubMed(Context context, Item item, String identifier) {
        try{
            Element jElement = retrieveXML("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&retmode=xml&id=" + identifier);
            if(jElement != null){
                if (!jElement.getName().equals("PubmedArticleSet") || jElement.getChildren().size()==0) {
                    return false;
                }
                // Use the ingest process to parse the XML document, transformation is done using XSLT
                IngestionCrosswalk xwalk = (IngestionCrosswalk) PluginManager.getNamedPlugin(IngestionCrosswalk.class, "PUBMED");
                xwalk.ingest(context, item, jElement);
                return true;
            }
        }catch (Exception ex){
            log.error("unable to process PMID metadata", ex);
        }
        return false;
    }

    private Element retrieveXML(String urls) throws Exception {
        SAXBuilder builder = new SAXBuilder();
        org.jdom.Document doc = builder.build(urls);
        return doc.getRootElement();
    }

    private void addEmailsAndEmbargoSettings(DryadJournalConcept journalConcept, Item item) throws AuthorizeException, SQLException {
        ArrayList<String> reviewEmailList = journalConcept.getEmailsToNotifyOnReview();
        String[] reviewEmails = reviewEmailList.toArray(new String[reviewEmailList.size()]);
        item.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "review", "mailUsers", Item.ANY);
        item.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "archive", "mailUsers", Item.ANY);
        item.clearMetadata("internal", "submit", "showEmbargo", Item.ANY);
        item.update();

        if(reviewEmails != null) {
            item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "review", "mailUsers", null, reviewEmails);
        }

        ArrayList<String> archiveEmailList = journalConcept.getEmailsToNotifyOnArchive();
        String[] archiveEmails = archiveEmailList.toArray(new String[archiveEmailList.size()]);

        if (archiveEmails != null) {
            item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "archive", "mailUsers", null, archiveEmails);
        }

        Boolean embargoAllowed = journalConcept.getAllowEmbargo();
        if(embargoAllowed != null && !embargoAllowed){
            //We don't need to show the embargo option to any of our data files
            item.addMetadata("internal", "submit", "showEmbargo", null, String.valueOf(embargoAllowed));
        }
    }

    private boolean processJournal(Item item, Context context, HttpServletRequest request) throws AuthorizeException, SQLException {
        String articleStatus = request.getParameter("article_status");
        // if article status is ACCEPTED, user enters journal name and manuscript number
        // if article status is PUBLISHED, user can either enter journal name or pub DOI
        // if article status is ARTICLE_STATUS_IN_REVIEW, user chooses journal from pulldown and optionally enters manuscript number

        if (articleStatus==null) {
            EventLogger.log(context, "submission-select-publication", "error=exception_reselect_journal");
            return false;
        }

        // Look for a DOI or PMID entered in the UI
        // Then use crosswalk to pre-load metadata into the Item.
        String identifier = request.getParameter("article_doi");
        if (identifier!=null && !identifier.equals("")) {
            // normalize and validate the identifier
            Matcher doiMatcher = Pattern.compile("(doi:)*(.+/.+)").matcher(identifier);
            Matcher pmidMatcher = Pattern.compile("(\\d+)").matcher(identifier);
            if (doiMatcher.find()) {
                identifier = doiMatcher.group(2);
                if (!processDOI(context, item, identifier)) {
                    EventLogger.log(context, "submission-select-publication", "doi=" + identifier + ",error=failed_doi_lookup");
                    return false;
                } else {
                    EventLogger.log(context, "submission-select-publication", "doi=" + identifier);
                }
            } else if (pmidMatcher.find()) {
                identifier = pmidMatcher.group(1);
                if (!processPubMed(context, item, identifier)) {
                    EventLogger.log(context, "submission-select-publication", "pmid=" + identifier + ",error=failed_pubmed_lookup");
                    return false;
                } else {
                    EventLogger.log(context, "submission-select-publication", "pmid=" + identifier);
                }
            }
        }

        // Find the journal concept:
        DryadJournalConcept journalConcept = null;

        // Look for a journal ID, if it's in review
        if (Integer.parseInt(articleStatus)==ARTICLE_STATUS_IN_REVIEW) {
            String journalID = request.getParameter("journalIDStatusInReview");
            if (journalID != null && journalID.length() > 0) {
                journalConcept = JournalUtils.getJournalConceptByJournalID(journalID);
            }
        }

        DCValue[] dcValues = item.getMetadata("prism.publicationName");
        item.clearMetadata("prism.publicationName");
        item.update();

        String journal = null;
        if (journalConcept == null) {
            // look in the item's metadata, in case the journal name was loaded by a crosswalk.
            if (dcValues.length > 0) {
                journal = dcValues[0].value;
                item.clearMetadata("prism.publicationName");
            }

            // then look in the request parameter for publication name.
            if (journal == null || "".equals(journal)) {
                journal = request.getParameter("prism_publicationName");
            }

            // then look in the unknown_doi parameter.
            if (journal == null || "".equals(journal)) {
                journal = request.getParameter("unknown_doi");
            }

            // clean the name
            if (journal != null) {
                journal = journal.replace("*", "");
                journal = journal.trim();
            }

            if (journal != null && journal.length() > 0) {
                journalConcept = JournalUtils.getJournalConceptByJournalName(journal);
            }
        }

        if (journalConcept == null && journal != null) {
            // if article is PUBLISHED or ACCEPTED, can be any journal, so we should make a temp journal.
            if ((Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED) || (Integer.parseInt(articleStatus)==ARTICLE_STATUS_PUBLISHED)) {
                try {
                    // if we still haven't found a matching journal concept, make a new, temporary one.
                    journalConcept = JournalUtils.createJournalConcept(journal);
                } catch (Exception e) {
                    // this should not happen because we've already checked to see if a matching concept existed.
                    log.error("couldn't create a concept");
                }
            }
        }

        if (journalConcept == null) {
            return false;
        }

        // Set all of the publication's information in a manuscript that we will then propagate to the item.
        Manuscript manuscript = new Manuscript(journalConcept);

        // Look for a manuscript number
        String manuscriptNumber = request.getParameter("manu");
        if (manuscriptNumber != null) {
            manuscriptNumber = manuscriptNumber.trim();
        }

        request.getSession().setAttribute("submit_error", "");
        item.clearMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY);
        item.update();
        if (journalConcept.getIntegrated()) {
            addEmailsAndEmbargoSettings(journalConcept, item);
            if (manuscriptNumber != null && manuscriptNumber.equals("")) {
                // set the status of the manuscript to whatever the user specified:
                log.error("manuscript number is empty or nonexistent");
                manuscript.setStatus(translateStatus(Integer.parseInt(articleStatus)));
            } else {
                manuscript = JournalUtils.getManuscriptFromManuscriptStorage(manuscriptNumber, journalConcept);
                if (manuscript.getMessage().equals("Invalid manuscript number")) {
                    // We do not have metadata for this manuscript number. Return error.
                    request.getSession().setAttribute("submit_error", "Invalid manuscript number. Please recheck or leave blank to continue.");
                    return false;
                }
            }
            // No matter which radio button was chosen, if the manuscript is rejected, say so.
            if (manuscript.getStatus() != null && Manuscript.statusIsRejected(manuscript.getStatus())) {
                request.getSession().setAttribute("submit_error", "This manuscript has been rejected by the journal.");
                return false;
            }
        } else {
            log.debug("Journal " + journalConcept.getJournalID() + " is not integrated");
        }
        manuscript.propagateMetadataToItem(context, item);
        item.update();
        return true;
    }

    private String translateStatus(Integer status) {
        if (status.equals(ARTICLE_STATUS_PUBLISHED)) {
            return Manuscript.STATUS_PUBLISHED;
        } else if (status.equals(ARTICLE_STATUS_ACCEPTED)) {
            return Manuscript.STATUS_ACCEPTED;
        } else if (status.equals(ARTICLE_STATUS_IN_REVIEW)) {
            return Manuscript.STATUS_SUBMITTED;
        }
        return Manuscript.STATUS_INVALID;
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
