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
            checkPublications();
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
                    String message = "";
                    Item item = wfi.getItem();
                    Manuscript queryManuscript = manuscriptFromItem(item, dryadJournalConcept);
                    // First, compare this item with anything in manuscript metadata storage:
                    // If this workflow item does not have a msid, it might have come from a submitter
                    // who didn't use a journal link.
                    List<Manuscript> databaseManuscripts = null;
                    Manuscript databaseManuscript = null;
                    try {
                        if (!"".equals(queryManuscript.getManuscriptId())) {
                            databaseManuscripts = JournalUtils.getStoredManuscriptsMatchingManuscript(queryManuscript);
                            if (databaseManuscripts != null && databaseManuscripts.size() > 0) {
                                databaseManuscript = databaseManuscripts.get(0);
                                message = "Journal-provided metadata for msid " + databaseManuscript.getManuscriptId() + " with title '" + databaseManuscript.getTitle() + "' was added. ";
                                databaseManuscript.optionalProperties.put("provenance", message);
                                updateItemMetadataFromManuscript(item, databaseManuscript, context);
                            }
                        }
                    } catch (ParseException e) {
                        // do we want to collect workflow items with faulty manuscript IDs?
                        message = "Problem: Manuscript ID is incorrect. ";
                    }
                    // look for this item in crossref:
                    Manuscript matchedManuscript = JournalUtils.getCrossRefManuscriptMatchingManuscript(queryManuscript);
                    if (matchedManuscript != null) {
                        // update the item's metadata
                        String score = matchedManuscript.optionalProperties.get("crossref-score");
                        message = "Associated publication (match score " + score + ") was found: \"" + matchedManuscript.getTitle() + "\" ";
                        matchedManuscript.optionalProperties.put("provenance", message);
                        updateItemMetadataFromManuscript(item, matchedManuscript, context);

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
                    if (!"".equals(message)) {
                        updatedItems.add(buildItemSummary(item) + "\n\t" + message);
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
                message = "Associated publication (match score " + score + ") was found: \"" + matchedManuscript.getTitle() + "\" ";
                matchedManuscript.optionalProperties.put("provenance", message);
                updateItemMetadataFromManuscript(item, matchedManuscript, context);
            }

            if (!"".equals(message)) {
                updatedItems.add(buildItemSummary(item) + "\n\t" + message);
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

        // Query for packages in archive (owning_collection = 2) that don't have a metadatavalue for publication doi (dc.relation.isreferencedby)
        MetadataField pubDoiField = null;
        try {
            pubDoiField = MetadataField.findByElement(context, PUBLICATION_DOI);
        } catch (SQLException e) {
            LOGGER.error("couldn't find " + PUBLICATION_DOI);
            return items;
        }
        if (pubDoiField != null) {
            try {
                String query = "SELECT item_id FROM item WHERE in_archive = 't' AND owning_collection = 2 AND NOT EXISTS (SELECT * FROM metadatavalue WHERE metadata_field_id = ? AND metadatavalue.item_id = item.item_id) AND EXISTS (SELECT * from metadatavalue where metadata_field_id = ? and text_value = ? AND metadatavalue.item_id = item.item_id)";
                TableRowIterator noPubDOIRows = DatabaseManager.query(context, query, pubDoiField.getFieldID(), pubNameField.getFieldID(), dryadJournalConcept.getFullName());
                if (noPubDOIRows != null) {
                    rows.addAll(noPubDOIRows.toList());
                }
            } catch (SQLException e) {
                LOGGER.error("couldn't find items without pub DOIs");
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

        // Query for packages in archive (owning_collection = 2) that have citations in progress (dryad.citationInProgress exists)
        MetadataField citationInProgressField = null;
        try {
            citationInProgressField = MetadataField.findByElement(context, CITATION_IN_PROGRESS);
        } catch (SQLException e) {
            LOGGER.error("couldn't find " + CITATION_IN_PROGRESS);
        }
        if (citationInProgressField != null) {
            try {
                String query = "SELECT item_id FROM item WHERE in_archive = 't' AND owning_collection = 2 AND EXISTS (SELECT * FROM metadatavalue WHERE metadata_field_id = ? AND metadatavalue.item_id = item.item_id) AND EXISTS (SELECT * from metadatavalue where metadata_field_id = ? and text_value = ? AND metadatavalue.item_id = item.item_id)";
                TableRowIterator inProgressRows = DatabaseManager.query(context, query, citationInProgressField.getFieldID(), pubNameField.getFieldID(), dryadJournalConcept.getFullName());
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
            LOGGER.error("Error sending publication updater email for journal " + dryadJournalConcept.getFullName());
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

    private void updateItemMetadataFromManuscript(Item item, Manuscript manuscript, Context context) {
        if (!"".equals(manuscript.getPublicationDOI())) {
            item.clearMetadata(PUBLICATION_DOI);
            item.addMetadata(PUBLICATION_DOI, null, manuscript.getPublicationDOI(), null, -1);
        }
        if (!"".equals(manuscript.getFullCitation())) {
            item.clearMetadata(FULL_CITATION);
            item.addMetadata(FULL_CITATION, null, manuscript.getFullCitation(), null, -1);
        }
        if (!"".equals(manuscript.getManuscriptId())) {
            item.clearMetadata(MANUSCRIPT_NUMBER);
            item.addMetadata(MANUSCRIPT_NUMBER, null, manuscript.getManuscriptId(), null, -1);
        }
        if (manuscript.getPublicationDate() != null) {
            SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
            item.clearMetadata(PUBLICATION_DATE);
            item.addMetadata(PUBLICATION_DATE, null, dateIso.format(manuscript.getPublicationDate()), null, -1);
        }
        item.clearMetadata(CITATION_IN_PROGRESS);
        item.addMetadata(CITATION_IN_PROGRESS, null, "true", null, -1);

        if (manuscript.optionalProperties.containsKey("provenance")) {
            item.addMetadata(PROVENANCE, "en", "PublicationUpdater: " + manuscript.optionalProperties.get("provenance") + " on " + DCDate.getCurrent().toString() + " (GMT)", null, -1);
        }

        try {
            item.update();
            context.commit();
        } catch (Exception e) {
            LOGGER.error("couldn't save metadata: " + e.getMessage());
        }
    }

    @Override
    public void init(ServletConfig aConfig) throws ServletException {
        super.init(aConfig);

        if (!ConfigurationManager.isConfigured()) {
            String config = getServletContext().getInitParameter("dspace.config");
            ConfigurationManager.loadConfig(config);
        }

        LOGGER.debug("scheduling publication checker");
        myPublicationUpdaterTimer = new Timer();
        // schedule harvesting to the number of days set in the configuration:
        // timers are set in units of milliseconds.
        int timerInterval = Integer.parseInt(ConfigurationManager.getProperty("publication.updater.timer"));
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
