package org.dspace.submit.step;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dspace.content.*;
import org.dspace.content.crosswalk.IngestionCrosswalk;
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
import org.dspace.submit.utils.DryadJournalSubmissionUtils;
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
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
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

    public static final int DISPLAY_MANUSCRIPT_NUMBER = 5;
    public static final int DISPLAY_CONFIRM_MANUSCRIPT_ACCEPTANCE = 6;
    public static final int ENTER_MANUSCRIPT_NUMBER = 7;


    private static Map<String, DCValue> journalToMetadata = new HashMap<String, DCValue>();
    public static List<String> integratedJournals = new ArrayList<String>();
    public static List<String> allowReviewWorkflowJournals = new ArrayList<String>();
    public static final List<String> journalNames = new ArrayList<String>();
    public static final List<String> journalVals = new ArrayList<String>();
    public static final List<String> journalDirs = new ArrayList<String>();
    public static final List<Boolean> journalEmbargo = new ArrayList<Boolean>();
    public static final Map<String, List<String>> journalNotifyOnReview = new HashMap<String, List<String>>();
    public static final Map<String, List<String>> journalNotifyOnArchive = new HashMap<String, List<String>>();
    private static Logger log = Logger.getLogger(SelectPublicationStep.class);


    public final static int  ARTICLE_STATUS_PUBLISHED=0;
    public final static int  ARTICLE_STATUS_ACCEPTED=1;
    public final static int  ARTICLE_STATUS_IN_REVIEW=2;
    public final static int  ARTICLE_STATUS_NOT_YET_SUBMITTED=3;


    public final static int  UNKNOWN_DOI=5;
    public final static int  MANU_ACC=6;

    static {
        journalVals.add("other");
        journalNames.add("(please select a journal)");
        journalDirs.add(null);
        journalEmbargo.add(false);

	// initialize settings from journal properties file
        String journalPropFile = ConfigurationManager.getProperty("submit.journal.config");
	log.info("initializing journal settings from property file " + journalPropFile);
        Properties properties = new Properties();
	
        try {
            properties.load(new FileInputStream(journalPropFile));
            String journalTypes = properties.getProperty("journal.order");
            for (int i = 0; i < journalTypes.split(",").length; i++) {
                String journalType = journalTypes.split(",")[i].trim();
                String journalDisplay = properties.getProperty("journal." + journalType + ".fullname");
                String metadataDir = properties.getProperty("journal." + journalType + ".metadataDir");
                String integrated = properties.getProperty("journal." + journalType + ".integrated");
                String embargo = properties.getProperty("journal." + journalType + ".embargoAllowed", "true");
                List<String> onReviewMails = Arrays.asList(properties.getProperty("journal." + journalType + ".notifyOnReview", "").replace(" ", "").split(","));
                List<String> onArchiveMails = Arrays.asList(properties.getProperty("journal." + journalType + ".notifyOnArchive", "").replace(" ", "").split(","));

                String allowReviewWorkflow = properties.getProperty("journal." + journalType + ".allowReviewWorkflow");

                journalVals.add(journalType);
                journalNames.add(journalDisplay);
                journalDirs.add(metadataDir);
                if(integrated != null && Boolean.valueOf(integrated))
                    integratedJournals.add(journalType);

                if(allowReviewWorkflow != null && Boolean.valueOf(allowReviewWorkflow))
                    allowReviewWorkflowJournals.add(journalType);

                journalEmbargo.add(Boolean.valueOf(embargo));
                journalNotifyOnReview.put(journalType, onReviewMails);
                journalNotifyOnArchive.put(journalType, onArchiveMails);

            }
        } catch (IOException e) {
            log.error("Error while loading journal properties", e);
        }

        journalVals.add("other");
        journalNames.add("OTHER JOURNAL");
        journalDirs.add(null);
        journalEmbargo.add(false);

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

            Item item = submissionInfo.getSubmissionItem().getItem();


            String journalID = null;
            String articleStatus = request.getParameter("article_status");
            String manuscriptNumber = request.getParameter("manu");

	    // get the journalID selected by the user in the UI
            if(articleStatus!=null){
                if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED){
                    String manuscriptNumberAcc = request.getParameter("manu-number-status-accepted");
                    String manuAcc = request.getParameter("manu_acc");
                    manuscriptNumber = manuscriptNumberAcc;
		    manuscriptNumber = manuscriptNumber.trim();

                    String journalName = request.getParameter("prism_publicationName");
                    journalName=journalName.replace("*", "");
		    journalName=journalName.trim();
                    journalID = DryadJournalSubmissionUtils.findKeyByFullname(journalName);
                    if(journalID==null) journalID=journalName;
                }
                else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_NOT_YET_SUBMITTED){
                    journalID = request.getParameter("journalIDStatusNotYetSubmitted");
                }
                else if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_IN_REVIEW){
                    journalID = request.getParameter("journalIDStatusInReview");
                }
            }


            //First of all check if we have accepted our license
            if(request.getParameter("license_accept") == null || !Boolean.valueOf(request.getParameter("license_accept")))
                return STATUS_LICENSE_NOT_ACCEPTED;


	    // attempt to process a DOI or PMID entered in the UI
            if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_PUBLISHED){
                String identifier = request.getParameter("article_doi");
                if(identifier!=null && !identifier.equals("")){

                    if(identifier.indexOf('/')!=-1){
                        if(!processDOI(context, item, identifier))
                            return ERROR_PUBMED_DOI;
                    }
                    else{
                       if(!processPubMed(context, item, identifier))
                            return ERROR_PUBMED_DOI;
                    }
                }

            }
	    
            // ARTICLE_STATUS_ACCEPTED ||  ARTICLE_STATUS_IN_REVIEW ||  ARTICLE_STATUS_NOT_YET_SUBMITTED
            else{
                if(!processJournal(journalID, manuscriptNumber, item, context, request, articleStatus)){

                    if(Integer.parseInt(articleStatus)==ARTICLE_STATUS_ACCEPTED) return ENTER_MANUSCRIPT_NUMBER;

                    return ERROR_SELECT_JOURNAL;
                }
            }

            return STATUS_COMPLETE;
        }catch(Exception e){
            log.error(e);
        }
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
            Element jElement = retrieveXML("http://api.labs.crossref.org/" + identifier + ".xml");
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


    private boolean processJournal(String journalID, String manuscriptNumber, Item item, Context context,
				   HttpServletRequest request, String articleStatus) throws AuthorizeException, SQLException {
	log.debug("processing journal ID");
	
        //We have selected to choose a journal, retrieve it
        if(!journalID.equals("other")){
            if(!integratedJournals.contains(journalID) || (integratedJournals.contains(journalID) && manuscriptNumber != null && manuscriptNumber.trim().equals(""))){
                //Just add the journal title
                String title= journalID;
                if(journalVals.indexOf(journalID)!=-1){
                    title = journalNames.get(journalVals.indexOf(journalID));
                    //Should it end with a *, remove it.
                    if(title.endsWith("*"))
                        title = title.substring(0, title.length() - 1);

                    Boolean embargoAllowed = Boolean.valueOf(journalEmbargo.get(journalVals.indexOf(journalID)));
                    if(!embargoAllowed){
                        //We don't need to show the embargo option to any of our data files
                        item.addMetadata("internal", "submit", "showEmbargo", null, String.valueOf(embargoAllowed));
                    }
                }
                item.addMetadata("prism", "publicationName", null, null, title);
                item.update();


            }
            else {
                if(journalVals.indexOf(journalID)!=-1){

                    String journalPath = journalDirs.get(journalVals.indexOf(journalID));
                    //We have a valid journal
                    // Unescape the manuscriptNumber to get the filename
                    String fileName = DryadJournalSubmissionUtils.unescapeFilename(manuscriptNumber);
                    PublicationBean pBean = ModelPublication.getDataFromPublisherFile(fileName, journalID, journalPath);
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

                        importJournalMetadata(context, item, pBean);
                        List<String> reviewEmails = journalNotifyOnReview.get(journalID);
                        item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "review", "mailUsers", null, reviewEmails.toArray(new String[reviewEmails.size()]));

                        List<String> archiveEmails = journalNotifyOnArchive.get(journalID);
                        item.addMetadata(WorkflowRequirementsManager.WORKFLOW_SCHEMA, "archive", "mailUsers", null, archiveEmails.toArray(new String[archiveEmails.size()]));

                        boolean embargoAllowed = journalEmbargo.get(journalVals.indexOf(journalID));
                        if(!embargoAllowed){
                            //We don't need to show the embargo option to any of our data files
                            item.addMetadata("internal", "submit", "showEmbargo", null, String.valueOf(embargoAllowed));
                        }
                        item.update();
                    }else{
                        request.getSession().setAttribute("submit_error", pBean.getMessage());
                        return false;
                    }
                }
            }
        }
        return true;
    }


    /**
       Import metadata from the journal settings into the data package item. If data already exists in
       the pBean, it will take precedence over the journal metadata.
     **/
    private void importJournalMetadata(Context context, Item item, PublicationBean pBean){
        // These values are common to both Article Types
        addSingleMetadataValueFromJournal(context, item, "journalName", pBean.getJournalName());
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
        if(pBean.getArticleType() == null || pBean.getArticleType().equals(PublicationBean.TYPE_REGULAR)) {
            addSingleMetadataValueFromJournal(context, item, "title", pBean.getTitle());
        } else if(pBean.getArticleType().equals(PublicationBean.TYPE_GR_NOTE)) {
            final String title = String.format("\"%s\" in %s", pBean.getTitle(), pBean.getCitationTitle());
            addSingleMetadataValueFromJournal(context, item, "title", title);
            addSingleMetadataValueFromJournal(context, item, "citationTitle", pBean.getCitationTitle());
            // Citation Authors are not stored in the Item
        }
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
