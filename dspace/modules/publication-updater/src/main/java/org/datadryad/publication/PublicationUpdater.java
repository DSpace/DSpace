package org.datadryad.publication;

import org.apache.commons.lang.StringUtils;
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
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
            LOGGER.info("manually checking publications");
            String queryString = aRequest.getQueryString();
            if (queryString != null) {
                DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalID(queryString);
                if (journalConcept == null) {
                    journalConcept = JournalUtils.getJournalConceptByISSN(queryString);
                }
                if (journalConcept != null) {
                    checkSinglePublication(journalConcept);
                } else {
                    aResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "no journal concept found by the identifier " + queryString);
                }
            } else {
                checkPublications();
            }
        } else {
            aResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "parameter not available for GET");
        }
    }

    private void checkPublications() {
        Context context = null;
        try {
            context = new Context();
            context.turnOffAuthorisationSystem();
            List<DryadJournalConcept> journalConcepts = Arrays.asList(JournalUtils.getAllJournalConcepts());
            for (DryadJournalConcept dryadJournalConcept : journalConcepts) {
                if (!"".equals(dryadJournalConcept.getISSN())) {
                    updateWorkflowItems(context, dryadJournalConcept);
                    updateArchivedItems(context, dryadJournalConcept);
                }
            }
            context.restoreAuthSystemState();
            LOGGER.info("finished updating publications");
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
    }

    private void checkSinglePublication(DryadJournalConcept dryadJournalConcept) {
        Context context = null;
        try {
            LOGGER.info("checking single publication " + dryadJournalConcept.getFullName());
            context = new Context();
            context.turnOffAuthorisationSystem();
            updateWorkflowItems(context, dryadJournalConcept);
            updateArchivedItems(context, dryadJournalConcept);
            context.restoreAuthSystemState();
            LOGGER.info("finished updating publication");
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
    }

    private void updateWorkflowItems(Context context, DryadJournalConcept dryadJournalConcept) {
        ArrayList<WorkflowItem> items = new ArrayList<WorkflowItem>();
        ArrayList<String> updatedItems = new ArrayList<String>();
        try {
            WorkflowItem[] itemArray = WorkflowItem.findAllByISSN(context, dryadJournalConcept.getISSN());
            items.addAll(Arrays.asList(itemArray));
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
                    Manuscript queryManuscript = manuscriptFromItem(item, dryadJournalConcept);
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
                                message = "Journal-provided metadata for msid " + databaseManuscript.getManuscriptId() + " with title '" + databaseManuscript.getTitle() + "' was added. ";
                                updateItemMetadataFromManuscript(item, databaseManuscript, context, message);
                            }
                        }
                    } catch (ParseException e) {
                        // do we want to collect workflow items with faulty manuscript IDs?
                        LOGGER.error("Problem updating item " + item.getID() + ": Manuscript ID is incorrect.");
                    }
                    // look for this item in crossref:
                    Manuscript matchedManuscript = JournalUtils.getCrossRefManuscriptMatchingManuscript(queryManuscript);
                    if (matchedManuscript != null) {
                        // update the item's metadata
                        message = "Associated publication (match score " + matchedManuscript.optionalProperties.get("crossref-score") + ") was found: \"" + matchedManuscript.getTitle() + "\" ";
                        if (updateItemMetadataFromManuscript(item, matchedManuscript, context, message)) {
                            updatedItems.add(buildItemSummary(item) + "\n\t" + message);
                        }
                        // was there a manuscript record saved for this? If so, update it.
                        if (databaseManuscript != null) {
                            databaseManuscript.setPublicationDOI(matchedManuscript.getPublicationDOI());
                            databaseManuscript.setPublicationDate(matchedManuscript.getPublicationDate());
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
        HashSet<Item> items = findIncompleteArchivedItems(context, dryadJournalConcept);
        // For all found items, look for matches in CrossRef publications.
        for (Item item : items) {
            String message = "";
            Manuscript queryManuscript = manuscriptFromItem(item, dryadJournalConcept);

            // look for this item in crossref:
            Manuscript matchedManuscript = JournalUtils.getCrossRefManuscriptMatchingManuscript(queryManuscript);
            if (matchedManuscript != null) {
                // update the item's metadata
                String score = matchedManuscript.optionalProperties.get("crossref-score");
                if (updateItemMetadataFromManuscript(item, matchedManuscript, context, message)) {
                    message = "Associated publication (match score " + score + ") was found: \"" + matchedManuscript.getTitle() + "\" ";
                    updatedItems.add(buildItemSummary(item) + "\n\t" + message);
                }
            }
        }

        if (updatedItems.size() > 0) {
            emailSummary(context, updatedItems, dryadJournalConcept, ARCHIVED_EMAIL_SUBJECT);
        }
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

        // Add these items to the incomplete list
        try {
            for (TableRow row : rows) {
                int itemID = row.getIntColumn("item_id");
                Item item = Item.find(context, itemID);
                items.add(item);
            }
        } catch (SQLException e) {
            LOGGER.error("couldn't find item");
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

    private Manuscript manuscriptFromItem(Item item, DryadJournalConcept dryadJournalConcept) {
        // get metadata from item:
        Manuscript queryManuscript = new Manuscript(dryadJournalConcept);
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
        if (msids != null && msids.length > 0) {
            queryManuscript.setManuscriptId(msids[0].value);
        }

        DCValue[] itemPubDOIs = item.getMetadata(PUBLICATION_DOI);
        if (itemPubDOIs != null && itemPubDOIs.length > 0) {
            queryManuscript.setPublicationDOI(itemPubDOIs[0].value);
        }
        return queryManuscript;
    }

    private boolean updateItemMetadataFromManuscript(Item item, Manuscript manuscript, Context context, String provenance) {
        boolean changed = false;
        if (!"".equals(manuscript.getPublicationDOI()) && !item.hasMetadataEqualTo(PUBLICATION_DOI, manuscript.getPublicationDOI())) {
            changed = true;
            item.clearMetadata(PUBLICATION_DOI);
            item.addMetadata(PUBLICATION_DOI, null, manuscript.getPublicationDOI(), null, -1);
        }
        if (!"".equals(manuscript.getFullCitation()) && !item.hasMetadataEqualTo(FULL_CITATION, manuscript.getFullCitation())) {
            changed = true;
            item.clearMetadata(FULL_CITATION);
            item.addMetadata(FULL_CITATION, null, manuscript.getFullCitation(), null, -1);
        }
        if (!"".equals(manuscript.getManuscriptId()) && !item.hasMetadataEqualTo(MANUSCRIPT_NUMBER, manuscript.getManuscriptId())) {
            changed = true;
            item.clearMetadata(MANUSCRIPT_NUMBER);
            item.addMetadata(MANUSCRIPT_NUMBER, null, manuscript.getManuscriptId(), null, -1);
        }

        SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
        if (manuscript.getPublicationDate() != null) {
            String dateString = dateIso.format(manuscript.getPublicationDate());
            if (!item.hasMetadataEqualTo(PUBLICATION_DATE, dateString)) {
                changed = true;
                item.clearMetadata(PUBLICATION_DATE);
                item.addMetadata(PUBLICATION_DATE, null, dateString, null, -1);
            }
        }

        if (changed) {
            item.clearMetadata(CITATION_IN_PROGRESS);
            item.addMetadata(CITATION_IN_PROGRESS, null, "true", null, -1);

            if (!"".equals(provenance)) {
                item.addMetadata(PROVENANCE, "en", "PublicationUpdater: " + provenance + " on " + DCDate.getCurrent().toString() + " (GMT)", null, -1);
            }

            try {
                item.update();
                context.commit();
            } catch (Exception e) {
                LOGGER.error("couldn't save metadata: " + e.getMessage());
            }
        }
        return changed;
    }

    @Override
    public void init(ServletConfig aConfig) throws ServletException {
        super.init(aConfig);

        if (!ConfigurationManager.isConfigured()) {
            String config = getServletContext().getInitParameter("dspace.config");
            ConfigurationManager.loadConfig(config);
        }

        LOGGER.debug("scheduling publication checker");
//        myPublicationUpdaterTimer = new Timer();
        // schedule harvesting to the number of days set in the configuration:
        // timers are set in units of milliseconds.
//        int timerInterval = Integer.parseInt(ConfigurationManager.getProperty("publication.updater.timer"));
//        myPublicationUpdaterTimer.schedule(new PublicationHarvester(), 0, 1000 * 60 * 60 * 24 * timerInterval);
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
    private PrintWriter getWriter(HttpServletResponse aResponse)
            throws IOException {
        aResponse.setContentType("xml/application; charset=UTF-8");
        return aResponse.getWriter();
    }

    private class PublicationHarvester extends TimerTask {
        @Override
        public void run() {
            checkPublications();
        }
    }
}
