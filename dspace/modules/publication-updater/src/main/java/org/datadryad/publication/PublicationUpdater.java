package org.datadryad.publication;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.*;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.JournalUtils;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowItem;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.lang.RuntimeException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Updates items' associated publication metadata with the latest metadata from either journal-provided metadata or from CrossRef.
 *
 * @author Daisie Huang <daisieh@datadryad.org>
 */
@SuppressWarnings("serial")
public class PublicationUpdater extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(PublicationUpdater.class);

    private final static String PUBLICATION_DOI = "dc.relation.isreferencedby";
    private final static String AUTHOR = "dc.contributor.author";
    private final static String FULL_CITATION = "dc.identifier.citation";
    private final static String MANUSCRIPT_NUMBER = "dc.identifier.manuscriptNumber";
    private final static String PUBLICATION_DATE = "dc.date.issued";
    private final static String TITLE = "dc.title";
    private final static String CITATION_IN_PROGRESS = "dryad.citationInProgress";
    private final static String DRYAD_DOI = "dc.identifier";
    private final static String PROVENANCE = "dc.description.provenance";
    private final static String PUBLICATION_NAME = "prism.publicationName";

    private final static String WORKFLOW_EMAIL_SUBJECT = "Data packages have been published";
    private final static String ARCHIVED_EMAIL_SUBJECT = "Archived data packages have been updated";

    // Timer for scheduled harvesting of publications from crossref
    private Timer myPublicationUpdaterTimer;

    @Override
    protected void doGet(HttpServletRequest aRequest,
                         HttpServletResponse aResponse) throws ServletException, IOException {
        String requestURI = aRequest.getRequestURI();
        if (requestURI.contains("retrieve")) {
            String queryString = aRequest.getQueryString();
            LOGGER.info("Automatic Publication Updater running with query " + queryString);
            if (queryString != null) {
                List<NameValuePair> queryParams = null;
                try {
                    queryParams = URLEncodedUtils.parse(queryString, Charset.defaultCharset());
                } catch (Exception e) {
                    aResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "could not parse query string");
                }
                if (isAuthorized(queryParams)) {
                    Context context = null;
                    try {
                        context = new Context();
                        context.turnOffAuthorisationSystem();
                        String issn = getISSN(queryParams);
                        Integer itemID = getItemID(queryParams);
                        String startLetter = getStartLetter(queryParams);
                        if (!startLetter.matches("[a-zA-Z]")) {
                            startLetter = "a";
                        }
                        if (issn != null) {
                            DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(issn);
                            if (journalConcept == null) {
                                journalConcept = JournalUtils.getJournalConceptByISSN(issn);
                            }
                            if (journalConcept != null) {
                                checkSinglePublication(context, journalConcept);
                            } else {
                                aResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "no journal concept found by the identifier " + issn);
                            }
                        } else if (itemID != null) {
                            Item item = Item.find(context, itemID);
                            String result = matchItemToCrossref(context, item);
                            LOGGER.info(result);
                        } else {
                            checkPublications(context, startLetter);
                        }
                        context.restoreAuthSystemState();
                    } catch (SQLException e) {
                        throw new RuntimeException("Couldn't get context", e);
                    }
                    finally {
                        try {
                            if (context != null) {
                                context.complete();
                            }
                        } catch (SQLException e) {
                            context.abort();
                            throw new RuntimeException("Context.complete threw an exception, aborting instead");
                        }
                    }

                } else {
                    aResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "no or incorrect authorization token provided");
                }
            }
            LOGGER.info("Automatic Publication Updater finished");
        } else {
            aResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "parameter not available for GET");
        }
    }

    private boolean isAuthorized(List<NameValuePair> queryParams) {
        for (NameValuePair param : queryParams) {
            if (param.getName().equals("auth")) {
                String token = ConfigurationManager.getProperty("publication.updater.token");
                if (token != null && param.getValue() != null) {
                    if (token.equals(param.getValue())) {
                        return true;
                    }
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private String getISSN(List<NameValuePair> queryParams) {
        for (NameValuePair param : queryParams) {
            if (param.getName().equals("issn")) {
                return param.getValue();
            }
        }
        return null;
    }

    private Integer getItemID(List<NameValuePair> queryParams) {
        for (NameValuePair param : queryParams) {
            if (param.getName().equals("item")) {
                return Integer.valueOf(param.getValue());
            }
        }
        return null;
    }

    private String getStartLetter(List<NameValuePair> queryParams) {
        for (NameValuePair param : queryParams) {
            if (param.getName().equals("start")) {
                return param.getValue();
            }
        }
        return "a";
    }

    private void checkPublications(Context context, String startLetter) {
        LOGGER.info("checking all publications starting from " + startLetter);
        List<DryadJournalConcept> journalConcepts = Arrays.asList(JournalUtils.getAllJournalConcepts());
        for (DryadJournalConcept dryadJournalConcept : journalConcepts) {
            String fullName = dryadJournalConcept.getFullName();
            if (fullName.length() > 0 && fullName.substring(0,1).compareToIgnoreCase(startLetter) >= 0) {
                if (!"".equals(dryadJournalConcept.getISSN())) {
                    checkSinglePublication(context, dryadJournalConcept);
                }
            } else {
                LOGGER.debug("skipping " + dryadJournalConcept.getFullName());
            }
        }
        LOGGER.info("finished updating publications");
    }

    private void checkSinglePublication(Context context, DryadJournalConcept dryadJournalConcept) {
        LOGGER.info("checking publication " + dryadJournalConcept.getFullName());
        updateWorkflowItems(context, dryadJournalConcept);
        updateArchivedItems(context, dryadJournalConcept);
        LOGGER.info("finished updating publication");
    }

    private void updateWorkflowItems(Context context, DryadJournalConcept dryadJournalConcept) {
        ArrayList<WorkflowItem> items = new ArrayList<WorkflowItem>();
        ArrayList<String> updatedItems = new ArrayList<String>();
        LOGGER.debug("finding workflow items");
        try {
            WorkflowItem[] itemArray = WorkflowItem.findAllByISSN(context, dryadJournalConcept.getISSN());
            items.addAll(Arrays.asList(itemArray));
            LOGGER.debug("processing " + items.size() + " items");
        } catch (Exception e) {
            LOGGER.error("couldn't find workflowItems for journal " + dryadJournalConcept.getJournalID());
            return;
        }
        if (items.size() > 0) {
            for (WorkflowItem wfi : items) {
                if (DryadWorkflowUtils.isDataPackage(wfi)) {
                    // is this package in review?
                    List<ClaimedTask> claimedTasks = null;
                    boolean isInReview = false;
                    try {
                        claimedTasks = ClaimedTask.findByWorkflowId(context, wfi.getID());
                        if (claimedTasks != null && claimedTasks.size() > 0 && claimedTasks.get(0).getActionID().equals("reviewAction")) {
                            isInReview = true;
                        }
                    } catch (SQLException e) {
                        LOGGER.debug("couldn't find claimed task for item " + wfi.getItem().getID());
                    }
                    String message = "";
                    Item item = wfi.getItem();
                    Manuscript queryManuscript = manuscriptFromItem(item);
                    LOGGER.debug(">>> processing workflow item with internal ID " + item.getID());
                    // First, compare this item with anything in manuscript metadata storage:
                    // If this workflow item does not have a msid, it might have come from a submitter
                    // who didn't use a journal link.
                    List<Manuscript> databaseManuscripts = null;
                    Manuscript databaseManuscript = null;
                    try {
                        databaseManuscripts = JournalUtils.getStoredManuscriptsMatchingManuscript(queryManuscript);
                        if (databaseManuscripts != null && databaseManuscripts.size() > 0) {
                            databaseManuscript = databaseManuscripts.get(0);
                            if (isInReview) {     // only update the metadata if the item is in review.
                                String provenance = "Journal-provided metadata for msid " + databaseManuscript.getManuscriptId() + " with title '" + databaseManuscript.getTitle() + "' was added. ";
                                if (updateItemMetadataFromManuscript(item, databaseManuscript, context, provenance)) {
                                    message = provenance;
                                }
                            }
                        }
                    } catch (ParseException e) {
                        // do we want to collect workflow items with faulty manuscript IDs?
                        LOGGER.error("Problem updating item " + item.getID() + ": Manuscript ID is incorrect.");
                    }

                    message = message + matchItemToCrossref(context, item);
                    if (!"".equals(message)) {
                        updatedItems.add(buildItemSummary(item) + "\n\t" + message);
                        // was there a manuscript record saved for this? If so, update it.
                        if (databaseManuscript != null) {
                            databaseManuscript.setStatus(Manuscript.STATUS_PUBLISHED);
                            try {
                                LOGGER.debug("writing publication data back to " + databaseManuscript.getManuscriptId());
                                JournalUtils.writeManuscriptToDB(databaseManuscript);
                            } catch (Exception e) {
                                LOGGER.debug("couldn't write manuscript " + databaseManuscript.getManuscriptId() + " to database, " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        if (updatedItems.size() > 0) {
            emailSummary(context, updatedItems, dryadJournalConcept, WORKFLOW_EMAIL_SUBJECT);
        }
    }

    private void updateArchivedItems(Context context, DryadJournalConcept dryadJournalConcept) {
        ArrayList<String> updatedItems = new ArrayList<String>();
        LOGGER.debug("finding archived items");
        HashSet<Item> items = findIncompleteArchivedItems(context, dryadJournalConcept);
        LOGGER.debug("processing " + items.size() + " items");
        // For all found items, look for matches in CrossRef publications.
        for (Item item : items) {
            LOGGER.debug(">>> processing archived item with internal ID " + item.getID());
            String message = matchItemToCrossref(context, item);
            if (!"".equals(message)) {
                updatedItems.add(buildItemSummary(item) + "\n\t" + message);
            }
        }

        if (updatedItems.size() > 0) {
            emailSummary(context, updatedItems, dryadJournalConcept, ARCHIVED_EMAIL_SUBJECT);
        }
    }

    private String matchItemToCrossref(Context context, Item item) {
        String message = "";
        Manuscript queryManuscript = manuscriptFromItem(item);

        // look for this item in crossref:
        StringBuilder resultString = new StringBuilder();
        Manuscript matchedManuscript = JournalUtils.getCrossRefManuscriptMatchingManuscript(queryManuscript, resultString);
        LOGGER.debug("crossref lookup for item " + item.getID() + " returned " + resultString);
        if (matchedManuscript != null) {
            String score = matchedManuscript.optionalProperties.get("crossref-score");
            if (!"".equals(queryManuscript.getPublicationDOI())){
                LOGGER.debug("matching with given publication DOI " + queryManuscript.getPublicationDOI());
            }
            LOGGER.debug("Item \"" + queryManuscript.getTitle() + "\" matched a title \"" + matchedManuscript.getTitle() + "\" with score " + score);
            LOGGER.debug("matched publication DOI is " + matchedManuscript.getPublicationDOI());
            // does the matched manuscript have the same authors?
            StringBuilder authormatches = new StringBuilder();
            if (JournalUtils.compareItemAuthorsToManuscript(item, matchedManuscript, authormatches)) {
                LOGGER.debug("same authors");
                // update the item's metadata
                String provenance = "Associated publication (match score " + score + ") was found: \"" + matchedManuscript.getTitle() + "\".";
                if (updateItemMetadataFromManuscript(item, matchedManuscript, context, provenance)) {
                    message = provenance;
                }
            } else {
                LOGGER.debug("different authors: " + authormatches);
            }
        } else {
            LOGGER.debug("Item \"" + queryManuscript.getTitle() + "\" didn't match anything in crossref");
        }
        return message;
    }

    private HashSet<Item> findIncompleteArchivedItems(Context context, DryadJournalConcept dryadJournalConcept) {
        ArrayList<TableRow> rows = new ArrayList<TableRow>();   // hash set because the two queries might give duplicate item_ids
        HashSet<Item> items = new HashSet<Item>();

        // Find metadata field for publication name:
        MetadataField pubNameField = null;
        try {
            pubNameField = MetadataField.findByElement(context, PUBLICATION_NAME);
        } catch (SQLException e) {
            LOGGER.error("couldn't find " + PUBLICATION_NAME);
            return items;
        }

        // Find metadata field for citation:
        MetadataField citationField = null;
        try {
            citationField = MetadataField.findByElement(context, FULL_CITATION);
        } catch (SQLException e) {
            LOGGER.error("couldn't find " + FULL_CITATION);
            return items;
        }

        // The items that have incomplete citations are of the following types:
        //   1) items without a FULL_CITATION:
        //       a) if these match to a reference, we will set CITATION_IN_PROGRESS to TRUE and add a citation
        //       b) if these don't match, leave them alone
        //   2) items with a FULL_CITATION && CITATION_IN_PROGRESS exists && CITATION_IN_PROGRESS == TRUE
        //       a) if these match to a reference, we will set CITATION_IN_PROGRESS to TRUE and update the citation
        //       b) if these don't match, leave them alone
        //   3) items with a FULL_CITATION && no CITATION_IN_PROGRESS: these are done
        //   4) items with a FULL CITATION && CITATION_IN_PROGRESS == FALSE: these are done

        // Look for items without a full citation
        if (citationField != null) {
            try {
                String query = "SELECT item_id FROM item WHERE in_archive = 't' AND owning_collection = 2 AND EXISTS (SELECT * from metadatavalue where metadata_field_id = ? and text_value = ? AND metadatavalue.item_id = item.item_id) " +
                        "AND NOT EXISTS (SELECT * FROM metadatavalue WHERE metadata_field_id = ? AND metadatavalue.item_id = item.item_id)";
                TableRowIterator noPubDOIRows = DatabaseManager.query(context, query, pubNameField.getFieldID(), dryadJournalConcept.getFullName(), citationField.getFieldID());
                if (noPubDOIRows != null) {
                    rows.addAll(noPubDOIRows.toList());
                }
            } catch (SQLException e) {
                LOGGER.error("couldn't find items without citations");
            }
        } else {
            LOGGER.error("no metadata field for FULL_CITATION");
            return items;
        }

        // Look for items with a FULL_CITATION && CITATION_IN_PROGRESS exists && CITATION_IN_PROGRESS == TRUE
        MetadataField citationInProgressField = null;
        try {
            citationInProgressField = MetadataField.findByElement(context, CITATION_IN_PROGRESS);
        } catch (SQLException e) {
            LOGGER.error("couldn't find " + CITATION_IN_PROGRESS);
        }
        if (citationInProgressField != null) {
            try {
                String query = "SELECT item_id FROM item WHERE in_archive = 't' AND owning_collection = 2 AND EXISTS (SELECT * from metadatavalue where metadata_field_id = ? and text_value = ? AND metadatavalue.item_id = item.item_id) " +
                        "AND EXISTS (SELECT * FROM metadatavalue WHERE metadata_field_id = ? AND metadatavalue.item_id = item.item_id) " +  // has a FULL_CITATION
                        "AND EXISTS (SELECT * FROM metadatavalue WHERE metadata_field_id = ? AND text_value like '%rue' AND metadatavalue.item_id = item.item_id)";  // has a TRUE CITATION_IN_PROGRESS field
                TableRowIterator inProgressRows = DatabaseManager.query(context, query, pubNameField.getFieldID(), dryadJournalConcept.getFullName(),
                        citationField.getFieldID(),
                        citationInProgressField.getFieldID()
                );
                if (inProgressRows != null) {
                    rows.addAll(inProgressRows.toList());
                }
            } catch (SQLException e) {
                LOGGER.error("couldn't find items with " + CITATION_IN_PROGRESS);
                return items;
            }
        }

        try {
            for (TableRow row : rows) {
                int itemID = row.getIntColumn("item_id");
                Item item = Item.find(context, itemID);
                items.add(item);
            }
        } catch (SQLException e) {
            LOGGER.error("couldn't find item");
        }

        return items;
    }

    private void emailSummary(Context c, ArrayList<String> updatedItems, DryadJournalConcept dryadJournalConcept, String subject) {
        StringBuilder message = new StringBuilder();
        message.append("For journal ");
        message.append(dryadJournalConcept.getFullName());
        message.append(":\n");
        for (String item : updatedItems) {
            message.append(item);
            message.append("\n\n");
        }
        try {
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "publication_updater"));
            email.addRecipient(ConfigurationManager.getProperty("curator.all.recipient"));
//            # Parameters: {0} Subject message
//            #             {1} Journal Name
//            #             {2} ISSN
//            #             {3} Data
            email.addArgument(subject);
            email.addArgument(dryadJournalConcept.getFullName());
            email.addArgument(dryadJournalConcept.getISSN());
            email.addArgument(message.toString());

            email.send();
        } catch (Exception e) {
            LOGGER.error("Error sending publication updater email for journal " + dryadJournalConcept.getFullName() + ": " + e.getMessage());
            LOGGER.error("message was: " + message.toString());
        }
    }

    private String buildItemSummary(Item item) {
        DCValue[] dryadDOIs = item.getMetadata(DRYAD_DOI);
        String dryadDOI = "";
        if (dryadDOIs != null && dryadDOIs.length > 0) {
            dryadDOI = dryadDOIs[0].value;
        }
        DCValue[] titles = item.getMetadata(TITLE);
        String title = "";
        if (titles != null && titles.length > 0) {
            title = titles[0].value;
        }
        return "Item " + item.getID() + " with DOI " + dryadDOI + " and title \"" + title + "\":";
    }

    private Manuscript manuscriptFromItem(Item item) {
        // get metadata from item:
        Manuscript queryManuscript = new Manuscript();
        String journalName = "";
        DCValue[] journalNames = item.getMetadata(PUBLICATION_NAME);
        if (journalNames != null && journalNames.length > 0) {
            DryadJournalConcept dryadJournalConcept = JournalUtils.getJournalConceptByJournalName(journalNames[0].value);
            queryManuscript.setJournalConcept(dryadJournalConcept);
        }

        String title = "";
        DCValue[] titles = item.getMetadata(TITLE);
        if (titles != null && titles.length > 0) {
            title = titles[0].value.replaceAll("Data from: ", "");
        }
        queryManuscript.setTitle(title);

        AuthorsList authorsList = new AuthorsList();
        List<DCValue> authorList = Arrays.asList(item.getMetadata(AUTHOR));
        for (DCValue a : authorList) {
            String lastName = StringUtils.substringBefore(a.value, ",");
            String givenNames = StringUtils.substringAfter(a.value, ",").trim();
            authorsList.author.add(new Author(lastName, givenNames));
        }
        queryManuscript.setAuthors(authorsList);

        DCValue[] msids = item.getMetadata(MANUSCRIPT_NUMBER);
        if (msids != null && msids.length > 0 && !"".equals(msids[0].value)) {
            queryManuscript.setManuscriptId(msids[0].value);
        }

        DCValue[] itemPubDOIs = item.getMetadata(PUBLICATION_DOI);
        if (itemPubDOIs != null && itemPubDOIs.length > 0 && !"".equals(itemPubDOIs[0].value)) {
            LOGGER.debug("found a DOI <" + itemPubDOIs[0].value + ">");
            queryManuscript.setPublicationDOI(itemPubDOIs[0].value);
        }
        return queryManuscript;
    }

    private boolean updateItemMetadataFromManuscript(Item item, Manuscript manuscript, Context context, String provenance) {
        boolean changed = false;
        LOGGER.debug("updating metadata for item " + item.getID() + " to manuscript " + manuscript.toString());
        // first, check to see if this is one of the known mismatches:
        if (isManuscriptMismatchForItem(item, manuscript)) {
            LOGGER.error("pub " + manuscript.getPublicationDOI() + " is known to be a mismatch for " + item.getID());
            return false;
        }

        if (!"".equals(manuscript.getPublicationDOI()) && !item.hasMetadataEqualTo(PUBLICATION_DOI, manuscript.getPublicationDOI())) {
            changed = true;
            item.clearMetadata(PUBLICATION_DOI);
            item.addMetadata(PUBLICATION_DOI, null, manuscript.getPublicationDOI(), null, -1);
            LOGGER.debug("adding publication DOI " + manuscript.getPublicationDOI());
            provenance = provenance + " " + PUBLICATION_DOI + " was updated.";
        }
        if (!"".equals(manuscript.getManuscriptId()) && !item.hasMetadataEqualTo(MANUSCRIPT_NUMBER, manuscript.getManuscriptId())) {
            changed = true;
            item.clearMetadata(MANUSCRIPT_NUMBER);
            item.addMetadata(MANUSCRIPT_NUMBER, null, manuscript.getManuscriptId(), null, -1);
            LOGGER.debug("adding msid " + manuscript.getManuscriptId());
            provenance = provenance + " " + MANUSCRIPT_NUMBER + " was updated.";
        }

        SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
        if (manuscript.getPublicationDate() != null) {
            String dateString = dateIso.format(manuscript.getPublicationDate());
            if (!item.hasMetadataEqualTo(PUBLICATION_DATE, dateString)) {
                changed = true;
                item.clearMetadata(PUBLICATION_DATE);
                item.addMetadata(PUBLICATION_DATE, null, dateString, null, -1);
                LOGGER.debug("adding pub date " + manuscript.getPublicationDate());
                provenance = provenance + " " + PUBLICATION_DATE + " was updated.";
            }
        }
        if (!"".equals(manuscript.getFullCitation())) {
            String itemCitation = "";
            DCValue[] citations = item.getMetadata(FULL_CITATION);
            if (citations != null && citations.length > 0) {
                itemCitation = citations[0].value;
            }
            double score = JournalUtils.getHamrScore(manuscript.getFullCitation().toLowerCase(), itemCitation.toLowerCase());
            LOGGER.debug("old citation was: " + itemCitation);
            LOGGER.debug("new citation is: " + manuscript.getFullCitation());
            LOGGER.debug("citation match score is " + score);
            if (score < 0.9) {
                changed = true;
                item.clearMetadata(FULL_CITATION);
                item.addMetadata(FULL_CITATION, null, manuscript.getFullCitation(), null, -1);
                LOGGER.debug("adding citation " + manuscript.getFullCitation());
                provenance = provenance + " " + FULL_CITATION + " was updated.";
            }
        }
        if (changed) {
            item.clearMetadata(CITATION_IN_PROGRESS);
            item.addMetadata(CITATION_IN_PROGRESS, null, "true", null, -1);
            if (!"".equals(provenance)) {
                LOGGER.info("writing provenance for item " + item.getID() + ": " + provenance);
                item.addMetadata(PROVENANCE, "en", "PublicationUpdater: " + provenance + " on " + DCDate.getCurrent().toString() + " (GMT)", null, -1);
            }

            try {
                item.update();
                context.commit();
            } catch (Exception e) {
                LOGGER.error("couldn't save metadata: " + e.getMessage());
            }
        } else {
            LOGGER.debug("nothing changed");
        }
        return changed;
    }

    private boolean isManuscriptMismatchForItem(Item item, Manuscript manuscript) {
        if ("".equals(manuscript.getPublicationDOI())) {
            return false;
        }
        LOGGER.debug("looking for mismatches for " + manuscript.getPublicationDOI());
        // normalize the pubDOI from the manuscript: remove leading "doi:" or "dx.doi.org/"
        String msDOI = null;
        Pattern doi = Pattern.compile(".*(10\\.\\d+/.+)");
        Matcher m = doi.matcher(manuscript.getPublicationDOI().toLowerCase());
        if (m.matches()) {
            msDOI = m.group(1);
        }
        if (msDOI == null) {
            LOGGER.error("msDOI not in correct format");
            return false;
        }
        DCValue[] itemMismatches = item.getMetadata("dryad", "citationMismatchedDOI", Item.ANY, Item.ANY);
        if (itemMismatches.length > 0) {
            for (DCValue dcv : itemMismatches) {
                m = doi.matcher(dcv.value.toLowerCase());
                if (m.matches()) {
                    if (msDOI.equals(m.group(1))) {
                        LOGGER.error("found a mismatch: " + m.group(1));
                        return true;
                    }
                }
            }
        }
        LOGGER.error("no mismatches");
        return false;
    }

    @Override
    public void init(ServletConfig aConfig) throws ServletException {
        super.init(aConfig);

        if (!ConfigurationManager.isConfigured()) {
            String config = getServletContext().getInitParameter("dspace.config");
            ConfigurationManager.loadConfig(config);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Harvests new publication data from Crossref.";
    }

    /**
     * Returns a PrintWriter with the correct character encoding set.
     *
     * @param aResponse In which to set the character encoding
     * @return A <code>PrintWriter</code> to send text through
     * @throws IOException If there is trouble getting a writer
     */
    private PrintWriter getWriter(HttpServletResponse aResponse) throws IOException {
        aResponse.setContentType("xml/application; charset=UTF-8");
        return aResponse.getWriter();
    }
}
