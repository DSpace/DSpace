package org.datadryad.publication;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.*;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;
import org.dspace.JournalUtils;
import org.datadryad.api.DryadJournalConcept;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
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
                updateWorkflowItems(context, dryadJournalConcept);
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
    }

    private void updateWorkflowItems(Context context, DryadJournalConcept dryadJournalConcept) {
        ArrayList<WorkflowItem> items = new ArrayList<WorkflowItem>();
        ArrayList<String> updatedItems = new ArrayList<String>();
        if (!"".equals(dryadJournalConcept.getISSN())) {
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
        }
        if (updatedItems.size() > 0) {
            emailWorkflowItemSummary(context, updatedItems, dryadJournalConcept);
        }
    }

    private void emailWorkflowItemSummary(Context c, ArrayList<String> updatedItems, DryadJournalConcept dryadJournalConcept) {
        LOGGER.error("emailing about journal " + dryadJournalConcept.getFullName());
        StringBuilder message = new StringBuilder();
        message.append("For journal ");
        message.append(dryadJournalConcept.getFullName());
        message.append(":\n");
        for (String item : updatedItems) {
            message.append(item);
            message.append("\n");
        }
        try {
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(c.getCurrentLocale(), "publication_updater"));
            email.addRecipient(ConfigurationManager.getProperty("curator.all.recipient"));
//            # Parameters: {0} Journal Name
//            #             {1} ISSN
//            #             {2} Data
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
        myPublicationUpdaterTimer.schedule(new PublicationHarvester(), 0, 1000 * 60 * 60 * 24 * timerInterval);
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
