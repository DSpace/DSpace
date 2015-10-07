package org.dspace.submit.step;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dspace.JournalUtils;
import org.dspace.content.*;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.Concept;
import org.dspace.content.authority.Scheme;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.submit.AbstractProcessingStep;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.app.util.SubmissionInfo;
import org.dspace.authorize.AuthorizeException;
import org.dspace.handle.HandleManager;
import org.dspace.submit.bean.PublicationBean;
import org.dspace.submit.model.ModelPublication;
import org.dspace.workflow.WorkflowRequirementsManager;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.management.RuntimeErrorException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.util.*;
import org.dspace.usagelogging.EventLogger;

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
    

    private static Map<String, DCValue> journalToMetadata = new HashMap<String, DCValue>();
    
    static {

        int counter = 1;
        String configLine = ConfigurationManager.getProperty("submit.journal.metadata." + counter);
        while(configLine != null){
            String journalField = configLine.split(":")[0];
            String metadataField = configLine.split(":")[1];
            DCValue dcVal = new DCValue();
            dcVal.schema = metadataField.split("\\.")[0];
            dcVal.element = metadataField.split("\\.")[1];
            if(metadataField.split("\\.").length == 3)
                dcVal.qualifier = metadataField.split("\\.")[2];

            //Add it our map
            journalToMetadata.put(journalField,dcVal);

            //Add one to our counter & read a new line
            counter++;
            configLine = ConfigurationManager.getProperty("submit.journal.metadata." + counter);
        }
    }


    public int doProcessing(Context context, HttpServletRequest request, HttpServletResponse response, SubmissionInfo submissionInfo) throws ServletException, IOException, SQLException, AuthorizeException {
        log.debug("processing new submission request");

        try{

            //First of all check if we have accepted our license
            if(request.getParameter("license_accept") == null || !Boolean.valueOf(request.getParameter("license_accept"))) {
                EventLogger.log(context, "submission-select-publication", "error=failed_license_accept");
                return STATUS_LICENSE_NOT_ACCEPTED;
            }

            String articleStatus = request.getParameter("article_status");

            // get the journalID selected by the user in the UI
            if(articleStatus==null)
            {
                EventLogger.log(context, "submission-select-publication", "error=exception_reselect_journal");
                return ERROR_SELECT_JOURNAL;
            }
            else
            {

                Item item = submissionInfo.getSubmissionItem().getItem();
                String manuscriptNumber = request.getParameter("manu");

                // ########### ARTICLE_STATUS_ACCEPTED ###########
                if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED)
                {

                    String manuscriptNumberAcc = request.getParameter("manu-number-status-accepted");
                    manuscriptNumber = manuscriptNumberAcc;
                    manuscriptNumber = manuscriptNumber.trim();

                    // try to get authority id first, its better than name
                    String journalUuid = request.getParameter("prism_publicationName_authority");
                    if(journalUuid != null) {
                        journalUuid = journalUuid.trim();
                    }
                    
                    String journal = request.getParameter("prism_publicationName");
                    if(journal!= null)
                    {
                        journal=journal.replace("*", "");
                        journal=journal.trim();
                    }

                    if(journal==null||journal.equals("")){
                        EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                        return ERROR_INVALID_JOURNAL;
                    }
                    else if(!processJournal(journal, null, journalUuid, manuscriptNumber, item, context, request, articleStatus)){
                        EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                        return ENTER_MANUSCRIPT_NUMBER;
                    }

                    EventLogger.log(context, "submission-select-publication", "journalID=" + journal +
                            ",articleStatus=" + articleStatus + ",manuscriptNumber=" + manuscriptNumber);

                }
                // ########### ARTICLE_STATUS_PUBLISHED ###########
                else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_PUBLISHED)
                {
                    //attempt to process a DOI or PMID entered in the UI
                    String identifier = request.getParameter("article_doi");

                    // This is the Journal name if they don't know the publication
                    String journal = request.getParameter("unknown_doi");

                    if(identifier!=null && !identifier.equals("")){

                        if(identifier.indexOf('/')!=-1){
                            if(!processDOI(context, item, identifier)) {
                                EventLogger.log(context, "submission-select-publication", "doi=" + identifier + ",error=failed_doi_lookup");
                                return ERROR_PUBMED_DOI;
                            } else {
                                EventLogger.log(context, "submission-select-publication", "doi=" + identifier);
                            }
                        }
                        else{
                            if(!processPubMed(context, item, identifier)) {
                                EventLogger.log(context, "submission-select-publication", "pmid=" + identifier + ",error=failed_pubmed_lookup");
                                return ERROR_PUBMED_DOI;
                            } else {
                                EventLogger.log(context, "submission-select-publication", "pmid=" + identifier);
                            }
                        }
                    }
                    else
                    {
                        if(journal==null||journal.length()==0)
                        {
                            EventLogger.log(context, "submission-select-publication", "error=no_journal_name");
                            return ERROR_PUBMED_NAME;
                        }
                        else{

                            journal=journal.replace("*", "");
                            journal=journal.trim();

                            if(journal==null||journal.equals("")){
                                EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                                return ERROR_INVALID_JOURNAL;
                            }
                            else if(!processJournal(journal, null, null, manuscriptNumber, item, context, request, articleStatus)){

                                if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED) return ENTER_MANUSCRIPT_NUMBER;

                                EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                                return ERROR_SELECT_JOURNAL;
                            }

                            EventLogger.log(context, "submission-select-publication", "journalID=" + journal +
                                    ",articleStatus=" + articleStatus + ",manuscriptNumber=" + manuscriptNumber);
                        }
                    }

                }
                // ########### ARTICLE_STATUS_NOT_YET_SUBMITTED ###########
                else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_NOT_YET_SUBMITTED)
                {
                    String journal = request.getParameter("journalIDStatusNotYetSubmitted");

                    if(journal==null||journal.equals("")){
                        EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                        return ERROR_INVALID_JOURNAL;
                    }
                    else if(!processJournal(journal, null, null, manuscriptNumber, item, context, request, articleStatus)){

                        EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                        return ERROR_SELECT_JOURNAL;
                    }


                }
                // ########### ARTICLE_STATUS_IN_REVIEW ###########
                else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_IN_REVIEW)
                {
                    String journalID = request.getParameter("journalIDStatusInReview");

                    if(journalID==null||journalID.equals("")){
                        EventLogger.log(context, "submission-select-publication", "error=invalid_journal");
                        return ERROR_INVALID_JOURNAL;
                    }
                    else if(!processJournal(null, journalID, null, manuscriptNumber, item, context, request, articleStatus)){
                        EventLogger.log(context, "submission-select-publication", "error=no_journal_selected");
                        return ERROR_SELECT_JOURNAL;
                    }

                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY,"false");
                    item.update();
                }
                // ########### TRYING TO SUBMIT FORM WITHOUT ARTICLE STATUS ###########
                else
                {
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

    private void addEmailsAndEmbargoSettings(Concept journalConcept, Item item) {
        String[] reviewEmails = JournalUtils.getListNotifyOnReview(journalConcept);

        if(reviewEmails != null) {
            item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "review", "mailUsers", null, reviewEmails);
        }

        String[] archiveEmails = JournalUtils.getListNotifyOnArchive(journalConcept);
        if(archiveEmails != null) {
            item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "archive", "mailUsers", null, archiveEmails);
        }

        Boolean embargoAllowed = JournalUtils.getBooleanEmbargoAllowed(journalConcept);
        if(!embargoAllowed){
            //We don't need to show the embargo option to any of our data files
            item.addMetadata("internal", "submit", "showEmbargo", null, String.valueOf(embargoAllowed));
        }
    }


    private boolean processJournal(String journalName, String journalShortID, String journalUuid, String manuscriptNumber, Item item, Context context,
                                   HttpServletRequest request, String articleStatus) throws AuthorizeException, SQLException {


        Concept journalConcept = null;

        if(journalConcept==null && journalUuid != null && journalUuid.length() > 0){
            journalConcept = JournalUtils.getJournalConceptById(context, journalUuid);
        }

        if(journalConcept==null && journalShortID != null && journalShortID.length() > 0){
            journalConcept = JournalUtils.getJournalConceptByShortID(context, journalShortID);
        }

        if(journalConcept==null && journalName != null && journalName.length() > 0){
            journalConcept = JournalUtils.getJournalConceptByName(context, journalName);
        }

        //We have selected to choose a journal, retrieve it
        if(journalConcept != null){

            String title = journalConcept.getPreferredLabel();

            if(!JournalUtils.getBooleanIntegrated(journalConcept) || (JournalUtils.getBooleanIntegrated(journalConcept) && manuscriptNumber != null && manuscriptNumber.trim().equals(""))){
                log.debug(JournalUtils.getJournalShortID(journalConcept) + " is not integrated OR manuscript number is null");
                //Just add the journal title

                //Should it end with a *, remove it.
                if(title.endsWith("*")) {
                    title = title.substring(0, title.length() - 1);
                }

                log.debug("adding journal title to item: " + title);
                addEmailsAndEmbargoSettings(journalConcept, item);
                addSingleMetadataValueFromJournal(context, item, "journalName", journalConcept.getPreferredLabel(), journalConcept.getIdentifier(), Choices.CF_ACCEPTED);
                item.update();
            }
            else {
                String journalPath = JournalUtils.getMetadataDir(journalConcept);
                log.debug("journalPath: " + journalPath);

                //We have a valid journal
                // Unescape the manuscriptNumber to get the filename
                String fileName = JournalUtils.unescapeFilename(manuscriptNumber);
                PublicationBean pBean = JournalUtils.getPublicationBeanFromManuscriptStorage(manuscriptNumber, JournalUtils.getJournalShortID(journalConcept));

                if (pBean.getMessage().equals((""))) {

                    // check if the status is "in review" or "rejected"
                    if(articleStatus!=null){

                        // case "Accepted"/ARTICLE_STATUS_ACCEPTED
                        // if the publication status is:
                        //    - Rejected ==> return "Invalid manuscript number."
                        //    - In Review ==> return "Invalid manuscript number."
                        //    - all the others ==> go through entering in PublicationDescriptionStep
                        if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED){
                            if(pBean.getStatus()!=null && (pBean.getStatus().equals(PublicationBean.STATUS_IN_REVIEW) || pBean.getStatus().equals(PublicationBean.STATUS_REJECTED))){
                                if(pBean.getStatus().equals(PublicationBean.STATUS_IN_REVIEW) ) {
                                    item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "submit", "skipReviewStage", Item.ANY,"false");
                                    item.update();
                                }
                                request.getSession().setAttribute("submit_error", "Invalid manuscript number.");
                                return false;
                            }
                        }
                        // case "IN Review"/ARTICLE_STATUS_IN_REVIEW
                        // if the publication status is:
                        //    - Rejected ==> return "Invalid manuscript number."
                        //    - all the others ==> go through entering in PublicationDescriptionStep
                        else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_IN_REVIEW){
                            if(pBean.getStatus()!=null && pBean.getStatus().equals(PublicationBean.STATUS_REJECTED)){
                                request.getSession().setAttribute("submit_error", "Invalid manuscript number.");
                                return false;
                            }
                        }
                    }

                    importJournalMetadata(context, item, pBean, journalConcept);
                    addEmailsAndEmbargoSettings(journalConcept, item);

                    item.update();
                } else if(pBean.getMessage().equals("Invalid manuscript number")) {
                    // We do not have metadata for this manuscript number
                    // Store the manuscriptNumber & journal title and continue as in-review
                    addEmailsAndEmbargoSettings(journalConcept, item);

                    title = journalConcept.getPreferredLabel();
                    log.debug("invalid manuscript nubmer. Setting journal title to: " + title);
                    addSingleMetadataValueFromJournal(context, item, "journalName", journalConcept.getPreferredLabel(), journalConcept.getIdentifier(), Choices.CF_ACCEPTED);
                    addSingleMetadataValueFromJournal(context, item, "manuscriptNumber", manuscriptNumber);

                    item.update();
                }else{
                    request.getSession().setAttribute("submit_error", pBean.getMessage());
                    return false;
                }
            }
        }
        else
        {
            log.debug("adding unknown journal title to item: " + journalName);
            item.addMetadata("prism", "publicationName", null, null, journalName);
            item.update();
        }
        return true;
    }


    /**
     Import metadata from the journal settings into the data package item. If data already exists in
     the pBean, it will take precedence over the journal metadata.
     **/
    private void importJournalMetadata(Context context, Item item, PublicationBean pBean, Concept journalConcept) throws SQLException {
        // These values are common to both Article Types
        addSingleMetadataValueFromJournal(context, item, "journalName", journalConcept.getPreferredLabel(), journalConcept.getIdentifier(), Choices.CF_ACCEPTED);
        addSingleMetadataValueFromJournal(context, item, "journalVolume", pBean.getJournalVolume());
        addSingleMetadataValueFromJournal(context, item, "abstract", pBean.getAbstract());
        addSingleMetadataValueFromJournal(context, item, "correspondingAuthor", pBean.getCorrespondingAuthor());
        addSingleMetadataValueFromJournal(context, item, "doi", pBean.getDOI());
        addMultiMetadataValueFromJournal(context, item, "authors", pBean.getAuthors());
        addMultiMetadataValueFromJournal(context, item, "subjectKeywords", pBean.getSubjectKeywords());
        addMultiMetadataValueFromJournal(context, item, "taxonomicNames", pBean.getTaxonomicNames());
        addMultiMetadataValueFromJournal(context, item, "coverageSpatial", pBean.getCoverageSpatial());
        addMultiMetadataValueFromJournal(context, item, "coverageTemporal", pBean.getCoverageTemporal());
        addSingleMetadataValueFromJournal(context, item, "publicationDate", pBean.getPublicationDate());
        addSingleMetadataValueFromJournal(context, item, "journalISSN", pBean.getJournalISSN());
        addSingleMetadataValueFromJournal(context, item, "journalNumber", pBean.getJournalNumber());
        addSingleMetadataValueFromJournal(context, item, "publisher", pBean.getPublisher());
        addSingleMetadataValueFromJournal(context, item, "manuscriptNumber", pBean.getManuscriptNumber());
        addSingleMetadataValueFromJournal(context, item, "journalID", pBean.getJournalID());
        addSingleMetadataValueFromJournal(context, item, "status", String.valueOf(pBean.isSkipReviewStep()));

        // These values differ based on the Article Type
        if(pBean.getArticleType().equals(PublicationBean.TYPE_GR_NOTE)) {
            final String title = String.format("\"%s\" in %s", pBean.getTitle(), pBean.getCitationTitle());
            addSingleMetadataValueFromJournal(context, item, "title", title);
            addSingleMetadataValueFromJournal(context, item, "citationTitle", pBean.getCitationTitle());
            // Citation Authors are not stored in the Item
        } else { // Assume Regular
            addSingleMetadataValueFromJournal(context, item, "title", pBean.getTitle());
        }
        String userInfo = "journal_id=" + pBean.getJournalID() + ",ms=" + pBean.getManuscriptNumber() + "";
        EventLogger.log(context, "submission-import-metadata", userInfo);
    }

    private void addSingleMetadataValueFromJournal(Context ctx, Item publication, String key, String value, String auth_id, int confidence ){
        DCValue dcVal = journalToMetadata.get(key);
        if(dcVal == null){
            log.error(LogManager.getHeader(ctx, "error importing field from journal", "Could not retrieve a metadata field for journal getter: " + key));
            return;
        }

        if(value != null)
            publication.addMetadata(dcVal.schema, dcVal.element, dcVal.qualifier, null, value, auth_id, confidence);

    }

    private void addSingleMetadataValueFromJournal(Context ctx, Item publication, String key, String value){
        DCValue dcVal = journalToMetadata.get(key);
        if(dcVal == null){
            log.error(LogManager.getHeader(ctx, "error importing field from journal", "Could not retrieve a metadata field for journal getter: " + key));
            return;
        }

        if(value != null)
            publication.addMetadata(dcVal.schema, dcVal.element, dcVal.qualifier, null, value);
    }

    private void addMultiMetadataValueFromJournal(Context ctx, Item publication, String key, List<String> values){
        DCValue dcVal = journalToMetadata.get(key);
        if(dcVal == null){
            log.error(LogManager.getHeader(ctx, "error importing field from journal", "Could not retrieve a metadata field for journal getter: " + key));
            return;
        }

        if(values != null && 0 < values.size())
            publication.addMetadata(dcVal.schema, dcVal.element, dcVal.qualifier, null, values.toArray(new String[values.size()]));
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
