/*
 */
package org.dspace.workflow;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.MessagingException;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.JournalUtils;
import org.datadryad.api.DryadJournalConcept;

/**
 * Refactoring email notification methods into this class.
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class WorkflowEmailManager {
    private static Logger log = Logger.getLogger(WorkflowEmailManager.class);
    /**
     * Notify the submitter that the item has been approved but sent to blackout
     */

    public static void notifyOfBlackout(Context c, Item i)
        throws SQLException, IOException {
        notifyOfApproval(c, i, "submit_datapackage_blackout");
    }

    public static void notifyOfArchive(Context c, Item i)
        throws SQLException, IOException {
        String emailFilename = itemWasBlackedOut(c, i) ? "submit_datapackage_blackout_archive" : "submit_datapackage_archive";
        notifyOfApproval(c, i, emailFilename);
    }
    /**
     * notify the submitter that the item is approved
     */
    private static void notifyOfApproval(Context c, Item i, String emailFilename)
            throws SQLException, IOException {
        try {
            // Get submitter
            EPerson ep = i.getSubmitter();
            // Get the Locale
            Locale supportedLocale = I18nUtil.getEPersonLocale(ep);

            // If the item went through publication blackout,
            //   use "submit_datapackage_blackout_archive"
            // otherwise, use "submit_datapackage_archive"
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale, emailFilename));

            // Get the item handle to email to user
//            String handle = HandleManager.findHandle(c, i);

            // Get title
            DCValue[] titles = i.getDC("title", null, Item.ANY);
            String title = "";
            try {
                title = I18nUtil.getMessage("org.dspace.workflow.WorkflowManager.untitled");
            }
            catch (MissingResourceException e) {
                title = "Untitled";
            }
            if (titles.length > 0) {
                title = titles[0].value;
            }
            String dataFileTitles = "";
            String dataFilesDoi = "";
            String datapackageDoi = DOIIdentifierProvider.getDoiValue(i);
            if (datapackageDoi == null) {
                datapackageDoi = "";
            }

            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, i);
            for (Item dataFile : dataFiles) {
                dataFileTitles += dataFile.getName() + "\n";
                dataFilesDoi += DOIIdentifierProvider.getDoiValue(dataFile) + "\n";
            }


            email.addRecipient(ep.getEmail());

            addJournalNotifyOnArchive(i, email);


            String submitter = "";
            if(i.getSubmitter() != null){
                submitter = i.getSubmitter().getFullName();
            }
            String manuscriptIdentifier = "";
            DCValue[] manuscriptIdentifiers = i.getMetadata(MetadataSchema.DC_SCHEMA, "identifier", "manuscriptNumber", Item.ANY);
            if(0 < manuscriptIdentifiers.length){
                manuscriptIdentifier = manuscriptIdentifiers[0].value;
            }

            if(manuscriptIdentifier.length() == 0) {
                manuscriptIdentifier = "none available";
            }

            // add a DOI URL as well:
            String doi_url = "";
            Matcher doimatcher = Pattern.compile("doi:(.+)").matcher(datapackageDoi);
            if (doimatcher.find()) {
                doi_url = "http://dx.doi.org/" + doimatcher.group(1);
            }

            email.addArgument(title);                 // {0}  Title of data package
            email.addArgument(datapackageDoi);        // {1}  The doi identifier of the data package
            email.addArgument(dataFileTitles);        // {2}  The title(s) of the data file(s)
            email.addArgument(dataFilesDoi);          // {3}  The dois of the data files
            email.addArgument(submitter);             // {4}  The submitter's full name
            email.addArgument(manuscriptIdentifier);  // {5}  The manuscript identifier (or "none available" if the metadata doesn't contain one)
            email.addArgument(doi_url);               // {6}  The formatted dx.doi.org URL
            email.send();
        }
        catch (MessagingException e) {
            log.warn(LogManager.getHeader(c, "notifyOfApproval",
                    "cannot email user" + " item_id=" + i.getID()));
        }
    }

    private static void addJournalNotifyOnArchive(Item item, Email email) {
        DCValue[] values=item.getMetadata("prism.publicationName");
        if (values!=null && values.length> 0) {
            String journal = values[0].value;
            if (journal!=null) {
                DryadJournalConcept journalConcept = JournalUtils.getJournalConceptByJournalName(journal);
                if (journalConcept != null) {
                    ArrayList<String> emails = journalConcept.getEmailsToNotifyOnArchive();
                    for (String emailAddr : emails) {
                        email.addRecipient(emailAddr);
                    }
                }
            }
        }
    }

    public static void notifyOfReject(Context c, WorkflowItem wi, EPerson e,
        String reason)
    {
        try
        {
            // Get the item title
            String title = wi.getItem().getName();
            String doi = DOIIdentifierProvider.getDoiValue(wi.getItem());
            String dataFileTitles = "";
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wi.getItem());
            for (Item dataFile : dataFiles) {
                dataFileTitles += dataFile.getName() + "\n";
            }

            // Get rejector's name
            String rejector = e == null ? "" : WorkflowManager.getEPersonName(e);
            Locale supportedLocale = I18nUtil.getEPersonLocale(e);
            Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale,"submit_datapackage_reject"));

            email.addRecipient(wi.getSubmitter().getEmail());

            // email the eperson who rejected this as well
            email.addRecipient(e.getEmail());

            email.addArgument(title);                                                           // {0}  Title of submission
            email.addArgument(doi);                                                             // {1}  Package DOI
            email.addArgument(dataFileTitles);                                                  // {2}  Name(s) of the data file(s)
            email.addArgument(reason);                                                          // {3}  Reason for the rejection
            email.addArgument(ConfigurationManager.getProperty("dspace.url") + "/mydspace");    // {4}  Link to 'My DSpace' page

            email.send();
        }
        catch (RuntimeException re)
        {
            // log this email error
            log.warn(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + e.getID()
                            + " eperson_email" + e.getEmail()
                            + " workflow_item_id" + wi.getID()));

            throw re;
        }
        catch (Exception ex)
        {
            // log this email error
            log.warn(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + e.getID()
                            + " eperson_email" + e.getEmail()
                            + " workflow_item_id" + wi.getID()));
        }
    }

    // send notices of curation activity
    public static void notifyOfCuration(Context c, WorkflowItem wi, EPerson[] epa,
           String taskName, String action, String message) throws SQLException, IOException
    {
        try
        {
            // Get the item title
            String title = WorkflowManager.getItemTitle(wi);

            // Get the submitter's name
            String submitter = WorkflowManager.getSubmitterName(wi);

            // Get the collection
            Collection coll = wi.getCollection();

            for (int i = 0; i < epa.length; i++)
            {
                Locale supportedLocale = I18nUtil.getEPersonLocale(epa[i]);
                Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale,
                                                                                  "flowtask_notify"));
                email.addArgument(title);
                email.addArgument(coll.getMetadata("name"));
                email.addArgument(submitter);
                email.addArgument(taskName);
                email.addArgument(message);
                email.addArgument(action);
                email.addRecipient(epa[i].getEmail());
                email.send();
            }
        }
        catch (MessagingException e)
        {
            log.warn(LogManager.getHeader(c, "notifyOfCuration", "cannot email users" +
                                          " of workflow_item_id" + wi.getID()));
        }
    }

    private static List<EPerson> getCurators(Context c, Collection coll) throws IOException, SQLException {
        List<EPerson> curators = new ArrayList<EPerson>();
        Role role = null;
        try {
            role = WorkflowUtils.getCollectionRoles(coll).get("curator");
        } catch (WorkflowConfigurationException e) {
            log.error("Unable to get curator role for collection " + coll, e);
        }
        if(role != null) {
            Group curatorsGroup = WorkflowUtils.getRoleGroup(c, coll.getID(), role);
            if(curatorsGroup != null) {
                curators.addAll(Arrays.asList(curatorsGroup.getMembers()));
            }
        }
        return curators;
    }

    public static void notifyOfReAuthorizationPayment(Context c, WorkflowItem wi)
    throws SQLException, IOException {
        try
        {
            // Notify curators and submitter
            List<EPerson> recipients = getCurators(c, wi.getCollection());
            recipients.add(wi.getSubmitter());

            // Get the item title
            String title = WorkflowManager.getItemTitle(wi);

            // Get the submitter's name
            String submitter = WorkflowManager.getSubmitterName(wi);

            // Get the collection
            Collection coll = wi.getCollection();

            // Get the package DOI
            String doi = DOIIdentifierProvider.getDoiValue(wi.getItem());

            for(EPerson recipient : recipients) {
                Locale supportedLocale = I18nUtil.getEPersonLocale(recipient);
                Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(supportedLocale, "payment_needs_reauthorization"));
                email.addArgument(title);      // {0}  Title of submission
                email.addArgument(doi);        // {1}  package DOI
                email.addArgument(submitter);  // {2}  Submitter's name
                email.addRecipient(recipient.getEmail());
                email.send();
            }
        }
        catch (MessagingException e)
        {
            log.warn(LogManager.getHeader(c, "notifyOfReAuthorizationPayment", "cannot email users" +
                                          " of workflow_item_id" + wi.getID()));
        }
    }

    private static Boolean itemWasBlackedOut(Context c, Item item) {
        DCValue provenance[] =  item.getMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en");
        Boolean wasBlackedOut = false;
        for(DCValue dcValue : provenance) {
            if(dcValue.value != null)
                if(dcValue.value.contains("Entered publication blackout")) {
                    wasBlackedOut = true;
                }
            }
        return wasBlackedOut;
    }
}
