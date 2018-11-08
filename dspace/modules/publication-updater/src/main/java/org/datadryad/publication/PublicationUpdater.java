package org.datadryad.publication;

import org.apache.http.NameValuePair;
import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
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
import org.dspace.workflow.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.lang.RuntimeException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.*;

import org.apache.http.client.utils.URLEncodedUtils;

/**
 * Updates packages' associated publication metadata with the latest metadata from either journal-provided metadata or from CrossRef.
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

    private final static String UNARCHIVED_EMAIL_SUBJECT = "Data packages have been published";
    private final static String ARCHIVED_EMAIL_SUBJECT = "Archived data packages have been updated";

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
                            StringBuilder result = matchPackageToCrossref(context, new DryadDataPackage(item), null);
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
                    LOGGER.error("no or incorrect authorization token provided");
                    return;
                }
            }
            LOGGER.info("Automatic Publication Updater finished");
        } else {
            aResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "parameter not available for GET");
        }
    }

    private boolean isAuthorized(List<NameValuePair> queryParams) {
        if ("".equals(getUser(queryParams))) {
            return false;
        }
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

    private String getUser(List<NameValuePair> queryParams) {
        for (NameValuePair param : queryParams) {
            if (param.getName().equals("user")) {
                return param.getValue();
            }
        }
        return "";
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
        try {
            if (JournalUtils.isJournalConceptListedInCrossref(dryadJournalConcept)) {
                updateUnarchivedPackages(context, dryadJournalConcept);
                if (useDryadClassic()) {
                    updateArchivedPackages(context, dryadJournalConcept);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Publication updating failed: " + e.getMessage());
        }
        LOGGER.info("finished updating publication");
    }

    private void updateUnarchivedPackages(Context context, DryadJournalConcept dryadJournalConcept) {
        List<DryadDataPackage> packages = DryadDataPackage.findAllUnarchivedPackagesByISSN(context, dryadJournalConcept.getISSN(), true);
        ArrayList<String> updatedPackages = new ArrayList<>();
        LOGGER.debug("found " + packages.size() + " unarchived packages");
        if (packages.size() > 0) {
            for (DryadDataPackage dryadDataPackage : packages) {
                dryadDataPackage.setJournalConcept(dryadJournalConcept);
                StringBuilder message = new StringBuilder();
                LOGGER.debug(">>> processing unarchived package " + dryadDataPackage.getIdentifier());
                // First, compare this package with anything in manuscript metadata storage:
                // If this unarchived package does not have a msid, it might have come from a submitter
                // who didn't use a journal link.
                Manuscript databaseManuscript = null;
                try {
                    databaseManuscript = JournalUtils.getStoredManuscriptForPackage(context, dryadDataPackage);
                    // is this package in review?
                    if (dryadDataPackage.isPackageInReview(context) && databaseManuscript != null) {
                        StringBuilder provenance = new StringBuilder("Journal-provided metadata for msid " + databaseManuscript.getManuscriptId() + " with title '" + databaseManuscript.getTitle() + "' was added. ");
                        if (dryadDataPackage.updateMetadataFromManuscript(databaseManuscript, context, provenance)) {
                            message = provenance;
                        }
                        if (databaseManuscript.isAccepted()) {
                            // see if this can be pushed out of review
                            ApproveRejectReviewItem.processReviewPackageUsingManuscript(context, dryadDataPackage, databaseManuscript);
                        }
                    }
                } catch (ApproveRejectReviewItemException e) {
                    LOGGER.error("Exception caught while reviewing item " + dryadDataPackage.getItem().getID() + ": " + e.getMessage());
                }
                message.append(matchPackageToCrossref(context, dryadDataPackage, databaseManuscript));
                if (!"".equals(message.toString())) {
                    updatedPackages.add("Package " + dryadDataPackage.getIdentifier() + " and title \"" + dryadDataPackage.getTitle() + "\":\n\t" + message);
                }
            }
        }
        if (updatedPackages.size() > 0) {
            emailSummary(context, updatedPackages, dryadJournalConcept, UNARCHIVED_EMAIL_SUBJECT);
        }
    }

    private void updateArchivedPackages(Context context, DryadJournalConcept dryadJournalConcept) {
        ArrayList<String> updatedPackages = new ArrayList<>();
        LOGGER.debug("finding archived packages");
        HashSet<Item> items = findIncompleteArchivedItems(context, dryadJournalConcept);
        LOGGER.debug("processing " + items.size() + " packages");
        // For all found items, look for matches in CrossRef publications.
        for (Item item : items) {
            DryadDataPackage dryadDataPackage = new DryadDataPackage(item);
            dryadDataPackage.setJournalConcept(dryadJournalConcept);
            LOGGER.debug(">>> processing archived package " + dryadDataPackage.getIdentifier());
            StringBuilder message = matchPackageToCrossref(context, dryadDataPackage, null);
            if (!"".equals(message.toString())) {
                updatedPackages.add(buildPackageSummary(dryadDataPackage) + "\n\t" + message);
            }
        }

        if (updatedPackages.size() > 0) {
            emailSummary(context, updatedPackages, dryadJournalConcept, ARCHIVED_EMAIL_SUBJECT);
        }
    }

    private StringBuilder matchPackageToCrossref(Context context, DryadDataPackage dryadDataPackage, Manuscript databaseManuscript) {
        StringBuilder message = new StringBuilder();

        // look for this package in crossref:
        StringBuilder resultString = new StringBuilder();
        Manuscript matchedManuscript = null;
        try {
            matchedManuscript = JournalUtils.getCrossRefManuscriptMatchingManuscript(dryadDataPackage, resultString);
            LOGGER.debug("crossref lookup for package " + dryadDataPackage.getIdentifier() + " returned " + resultString);
            if (matchedManuscript != null) {
                String score = matchedManuscript.optionalProperties.get("crossref-score");
                if (!"".equals(dryadDataPackage.getPublicationDOI())){
                    LOGGER.debug("matching with given publication DOI " + dryadDataPackage.getPublicationDOI());
                    StringBuilder provenance = new StringBuilder("Associated publication for doi " + dryadDataPackage.getPublicationDOI() + " was found: \"" + matchedManuscript.getTitle() + "\".");
                    if (dryadDataPackage.updateMetadataFromManuscript(matchedManuscript, context, provenance)) {
                        message = provenance;
                    }
                } else {
                    LOGGER.debug("Package \"" + dryadDataPackage.getTitle() + "\" matched a title \"" + matchedManuscript.getTitle() + "\" with score " + score);
                    LOGGER.debug("matched publication DOI is " + matchedManuscript.getPublicationDOI());
                    // does the matched manuscript have the same authors?
                    StringBuilder authormatches = new StringBuilder();
                    if (matchedManuscript.getAuthorList().size() == JournalUtils.comparePackageAuthorsToManuscript(dryadDataPackage, matchedManuscript, authormatches)) {
                        LOGGER.debug("same authors");
                        // update the package's metadata
                        StringBuilder provenance = new StringBuilder("Associated publication (match score " + score + ") was found: \"" + matchedManuscript.getTitle() + "\".");
                        if (dryadDataPackage.updateMetadataFromManuscript(matchedManuscript, context, provenance)) {
                            message = provenance;
                        }
                    } else {
                        LOGGER.debug("different authors: " + authormatches);
                    }
                }
            } else {
                LOGGER.debug("Package \"" + dryadDataPackage.getTitle() + "\" didn't match anything in crossref");
            }
        } catch (RESTModelException e) {
            LOGGER.debug("crossref match wasn't valid: " + e.getMessage());
        }
        // was there a manuscript record saved for this? If so, update it.
        if (databaseManuscript != null && !"".equals(message.toString())) {
            databaseManuscript.setStatus(matchedManuscript.getLiteralStatus());
            databaseManuscript.setPublicationDate(matchedManuscript.getPublicationDate());
            databaseManuscript.setPublicationDOI(matchedManuscript.getPublicationDOI());
            try {
                LOGGER.debug("writing publication data for package " + dryadDataPackage.getIdentifier() + ", " + databaseManuscript.getPublicationDOI() + " back to " + databaseManuscript.getManuscriptId());
                JournalUtils.writeManuscriptToDB(databaseManuscript);
            } catch (Exception e) {
                LOGGER.debug("couldn't write manuscript " + databaseManuscript.getManuscriptId() + " to database, " + e.getMessage());
            }
        }
        return message;
    }

    // only used for Dspace-based Dryad; new Dryad won't have manually-updated citations.
    private HashSet<Item> findIncompleteArchivedItems(Context context, DryadJournalConcept dryadJournalConcept) {
        ArrayList<TableRow> rows = new ArrayList<>();   // hash set because the two queries might give duplicate item_ids
        HashSet<Item> items = new HashSet<>();

        // Find metadata field for publication name:
        MetadataField pubNameField = null;
        try {
            pubNameField = MetadataField.findByElement(PUBLICATION_NAME);
        } catch (SQLException e) {
            LOGGER.error("couldn't find " + PUBLICATION_NAME);
            return items;
        }

        // Find metadata field for citation:
        MetadataField citationField = null;
        try {
            citationField = MetadataField.findByElement(FULL_CITATION);
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
            citationInProgressField = MetadataField.findByElement(CITATION_IN_PROGRESS);
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

    private void emailSummary(Context c, ArrayList<String> updatedPackages, DryadJournalConcept dryadJournalConcept, String subject) {
        StringBuilder message = new StringBuilder();
        message.append("For journal ");
        message.append(dryadJournalConcept.getFullName());
        message.append(":\n");
        for (String p : updatedPackages) {
            message.append(p);
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

    private String buildPackageSummary(DryadDataPackage dryadDataPackage) {
        return "Package " + dryadDataPackage.getIdentifier() + " and title \"" + dryadDataPackage.getTitle() + "\":";
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

    private boolean useDryadClassic() {
        String dryadSystem = ConfigurationManager.getProperty("dryad.system");
        boolean useDryadClassic = true;
        if (dryadSystem != null && dryadSystem.toLowerCase().equals("dash")) {
            useDryadClassic = false;
        }
        return useDryadClassic;
    }
}
