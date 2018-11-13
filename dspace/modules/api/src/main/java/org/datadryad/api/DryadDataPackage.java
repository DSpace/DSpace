/*
  The main class that provides access to Dryad Data Package objects. Each
  DryadDataPackage contains a DSpace Item object, and adds Dryad-specific functionality
  on top of it.
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.datadryad.rest.models.Author;
import org.datadryad.rest.models.Manuscript;
import org.datadryad.rest.models.Package;
import org.dspace.JournalUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.identifier.IdentifierException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.*;
import org.dspace.workflow.actions.WorkflowActionConfig;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadDataPackage extends DryadObject {
    private static final String PACKAGES_COLLECTION_HANDLE_KEY = "stats.datapkgs.coll";

    private static final String PROVENANCE_SCHEMA = "dc";
    private static final String PROVENANCE_ELEMENT = "description";
    private static final String PROVENANCE_QUALIFIER = "provenance";
    private static final String PROVENANCE_LANGUAGE = "en";

    private static final String WORKFLOWITEM_TABLE = "workflowitem";
    private static final String WORKFLOWITEM_COLUMN_ITEMID = "item_id";
    private static final String WORKFLOWITEM_COLUMN_COLLECTIONID = "collection_id";

    private static final String PUBLICATION_NAME_SCHEMA = "prism";
    private static final String PUBLICATION_NAME_ELEMENT = "publicationName";
    private static final String PUBLICATION_NAME_QUALIFIER = null;

    private static final String MANUSCRIPT_NUMBER_SCHEMA = "dc";
    private static final String MANUSCRIPT_NUMBER_ELEMENT = "identifier";
    private static final String MANUSCRIPT_NUMBER_QUALIFIER = "manuscriptNumber";

    private static final String FORMER_MANUSCRIPT_NUMBER_SCHEMA = "dryad";
    private static final String FORMER_MANUSCRIPT_NUMBER_ELEMENT = "formerManuscriptNumber";
    private static final String FORMER_MANUSCRIPT_NUMBER_QUALIFIER = null;

    private static final String MISMATCHED_DOI_SCHEMA = "dryad";
    private static final String MISMATCHED_DOI_ELEMENT = "citationMismatchedDOI";
    private static final String MISMATCHED_DOI_QUALIFIER = null;

    private static final String BLACKOUT_UNTIL_SCHEMA = "dc";
    private static final String BLACKOUT_UNTIL_ELEMENT = "date";
    private static final String BLACKOUT_UNTIL_QUALIFIER = "blackoutUntil";

    // Publication DOI in a data package
    private static final String RELATION_ISREFERENCEDBY_QUALIFIER = "isreferencedby";

    private static final String KEYWORD_SCHEMA = "dc";
    private static final String KEYWORD_ELEMENT = "subject";

    private final static String PUBLICATION_DATE_SCHEMA = "dc";
    private final static String PUBLICATION_DATE_ELEMENT = "date";
    private final static String PUBLICATION_DATE_QUALIFIER = "issued";

    private static final String DASH_TRANSFER_SCHEMA = "dryad";
    private static final String DASH_TRANSFER_ELEMENT = "dashTransferDate";

    private final static String PUBLICATION_DOI = "dc.relation.isreferencedby";
    private final static String FULL_CITATION = "dc.identifier.citation";
    private final static String MANUSCRIPT_NUMBER = "dc.identifier.manuscriptNumber";
    private final static String PUBLICATION_DATE = "dc.date.issued";
    private final static String CITATION_IN_PROGRESS = "dryad.citationInProgress";
    private final static String PROVENANCE = "dc.description.provenance";

    private Set<DryadDataFile> dataFiles;
    private String curationStatus = "";
    private String abstractString = "";
    private ArrayList<Author> authors = new ArrayList<>();
    private DryadJournalConcept journalConcept = null;

    private static Logger log = Logger.getLogger(DryadDataPackage.class);

    private static boolean useDryadClassic = true;

    static {
        String dryadSystem = ConfigurationManager.getProperty("dryad.system");
        if (dryadSystem != null && dryadSystem.toLowerCase().equals("dash")) {
            useDryadClassic = false;
        }
    }

    public DryadDataPackage(Item item) {
        super(item);
        String pubName = getSingleMetadataValue(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER);
        if (pubName != null && !pubName.equals("")) {
            journalConcept = JournalUtils.getJournalConceptByJournalName(getPublicationName());
        }
    }

    public static DryadDataPackage create(Context context) throws SQLException {
        Collection collection = DryadDataPackage.getCollection(context);
        DryadDataPackage dataPackage = null;
        try {
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, true);
            Item item = wsi.getItem();
            dataPackage = new DryadDataPackage(item);
            dataPackage.createIdentifier(context);
            dataPackage.addToCollectionAndArchive(collection);
            wsi.deleteWrapper();
            return dataPackage;
        } catch (IdentifierException ex) {
            log.error("Identifier exception creating a Data Package", ex);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data Package", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data Package", ex);
        }
        return dataPackage;
    }

    // Getters and setters for metadata and internal data
    public String getPublicationDate() {
        String result = "";
        if (getItem() != null) {
            result = getSingleMetadataValue(PUBLICATION_DATE_SCHEMA, PUBLICATION_DATE_ELEMENT, PUBLICATION_DATE_QUALIFIER);
            result = (result == null ? "" : result);
        } else {

        }
        return result;
    }

    public void setPublicationDate(String publicationDate) {
        if (getItem() != null) {
            addSingleMetadataValue(Boolean.TRUE, PUBLICATION_DATE_SCHEMA, PUBLICATION_DATE_ELEMENT, PUBLICATION_DATE_QUALIFIER, publicationDate);
        } else {

        }
    }

    public String getPublicationName() {
        String result = "";
        if (journalConcept != null) {
            result = journalConcept.getFullName();
        }
        return result;
    }

    public void setPublicationName(String publicationName) {
        journalConcept = JournalUtils.getJournalConceptByJournalName(publicationName);
        if (getItem() != null) {
            addSingleMetadataValue(Boolean.TRUE, PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, publicationName);
        } else {

        }
    }
    
    public DryadJournalConcept getJournalConcept() {
        return journalConcept;
    }

    public void setJournalConcept(DryadJournalConcept concept) {
        journalConcept = concept;
    }

    public String getManuscriptNumber() {
        String result = "";
        if (getItem() != null) {
            result = getSingleMetadataValue(MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER);
            result = (result == null ? "" : result);
        } else {

        }
        return result;
    }

    public void setManuscriptNumber(String manuscriptNumber) {
        if (getItem() != null) {
            addSingleMetadataValue(Boolean.TRUE, MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER, manuscriptNumber);
        } else {

        }
    }

    public List<String> getFormerManuscriptNumbers() {
        return getMultipleMetadataValues(FORMER_MANUSCRIPT_NUMBER_SCHEMA, FORMER_MANUSCRIPT_NUMBER_ELEMENT, FORMER_MANUSCRIPT_NUMBER_QUALIFIER);
    }

    public void setFormerManuscriptNumber(String manuscriptNumber) {
        addSingleMetadataValue(Boolean.TRUE, FORMER_MANUSCRIPT_NUMBER_SCHEMA, FORMER_MANUSCRIPT_NUMBER_ELEMENT, FORMER_MANUSCRIPT_NUMBER_QUALIFIER, manuscriptNumber);
    }

    public List<String> getMismatchedDOIs() {
        return getMultipleMetadataValues(MISMATCHED_DOI_SCHEMA, MISMATCHED_DOI_ELEMENT, MISMATCHED_DOI_QUALIFIER);
    }

    public void setMismatchedDOIs(String mismatchedDOI) {
        addSingleMetadataValue(Boolean.TRUE, MISMATCHED_DOI_SCHEMA, MISMATCHED_DOI_ELEMENT, MISMATCHED_DOI_QUALIFIER, mismatchedDOI);
    }

    public void setBlackoutUntilDate(Date blackoutUntilDate) {
        String dateString = null;
        if(blackoutUntilDate != null)  {
            dateString = new DCDate(blackoutUntilDate).toString();
        }
        if (getItem() != null) {
            addSingleMetadataValue(Boolean.TRUE, BLACKOUT_UNTIL_SCHEMA, BLACKOUT_UNTIL_ELEMENT, BLACKOUT_UNTIL_QUALIFIER, dateString);
        }
    }

    public Date getBlackoutUntilDate() {
        Date blackoutUntilDate = null;
        if (getItem() != null) {
            String dateString = getSingleMetadataValue(BLACKOUT_UNTIL_SCHEMA, BLACKOUT_UNTIL_ELEMENT, BLACKOUT_UNTIL_QUALIFIER);
            blackoutUntilDate = (dateString == null ? null : new DCDate(dateString).toDate());
        } else {

        }
        return blackoutUntilDate;
    }

    public void setPublicationDOI(String publicationDOI) {
        // check that this DOI starts with the doi: prefix. if not, add it.
        if (publicationDOI != null) {
            Pattern doiPattern = Pattern.compile("^doi:.*");
            Matcher matcher = doiPattern.matcher(publicationDOI);
            if (!("".equals(publicationDOI)) && !matcher.find()) {
                publicationDOI = "doi:" + publicationDOI;
            }
        }
        if (getItem() != null) {
            addSingleMetadataValue(Boolean.FALSE, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISREFERENCEDBY_QUALIFIER, publicationDOI);
        } else {

        }
    }

    public void clearPublicationDOI() {
        // Need to filter just on metadata values that are publication DOIs
        if (getItem() != null) {
            addSingleMetadataValue(Boolean.TRUE, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISREFERENCEDBY_QUALIFIER, null);
        } else {

        }
    }

    /**
     * Get the publication DOI. Does not account for pubmed IDs, assumes
     * first dc.relation.isreferencedby is the publication DOI
     * @return
     * @throws SQLException
     */
    public String getPublicationDOI() {
        String result = "";
        if (getItem() != null) {
            result = getSingleMetadataValue(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISREFERENCEDBY_QUALIFIER);
            result = (result == null ? "" : result);
        } else {

        }
        return result;
    }

    public String getAbstract() {
        if (getItem() != null) {
            String theAbstract = getSingleMetadataValue("dc", "description", null);
            String extraAbstract = getSingleMetadataValue("dc", "description", "abstract");

            if (extraAbstract != null && extraAbstract.length() > 0) {
                theAbstract = theAbstract + "\n" + extraAbstract;
            }
            return theAbstract;
        } else {
            return abstractString;
        }
    }

    public void setAbstract(String theAbstract) {
        if (getItem() != null) {
            addSingleMetadataValue(Boolean.TRUE, "dc", "description", null, theAbstract);
        } else {
            abstractString = theAbstract;
        }
    }

    public List<String> getKeywords() {
        return getMultipleMetadataValues(KEYWORD_SCHEMA, KEYWORD_ELEMENT, null);
    }

    public void setKeywords(List<String> keywords) {
        addMultipleMetadataValues(Boolean.TRUE, KEYWORD_SCHEMA, KEYWORD_ELEMENT, null, keywords);
    }

    public void addKeywords(List<String> keywords) {
        addMultipleMetadataValues(Boolean.FALSE, KEYWORD_SCHEMA, KEYWORD_ELEMENT, null, keywords);
    }

    public List<Author> getAuthors() {
        ArrayList<Author> authorList = new ArrayList<>();
        if (getItem() != null) {
            DCValue[] metadata = item.getMetadata("dc", "contributor", "author", Item.ANY);
            for (DCValue dcValue : metadata) {
                authorList.add(new Author(dcValue));
            }
            metadata = item.getMetadata("dc", "contributor", null, Item.ANY);
            for (DCValue dcValue : metadata) {
                authorList.add(new Author(dcValue));
            }
            metadata = item.getMetadata("dc", "creator", null, Item.ANY);
            for (DCValue dcValue : metadata) {
                authorList.add(new Author(dcValue));
            }
        } else {
            authorList.addAll(authors);
        }
        return authorList;
    }

    public void addAuthor(Author author) {
        authors.add(author);
    }

    public List<DryadDataPackage> getDuplicatePackages(Context context) {
        ArrayList<DryadDataPackage> resultList = new ArrayList<>();
        try {
            if (useDryadClassic) {
                DCValue[] duplicates = getItem().getMetadata("dryad.duplicateItem");
                if (duplicates != null) {
                    for (DCValue dup : duplicates) {
                        Item item = Item.find(context, Integer.valueOf(dup.value));
                        if (item != null) {
                            resultList.add(new DryadDataPackage(item));
                        }
                    }
                }
            } else {

            }
        } catch (Exception e) {

        }
        return resultList;
    }

    public void updateDuplicatePackages(Context context) {
        ArrayList<DryadDataPackage> resultList = new ArrayList<>();
        try {
            // get the current duplicate packages
            resultList.addAll(getDuplicatePackages(context));
            resultList.addAll(findAllByManuscript(context, new Manuscript(this)));
            // look for items that have the same journal + title + authors?

        } catch (Exception e) {
            log.error("Exception while finding items matching manuscript " + this.getIdentifier());
        }

        // update the data package's metadata:
        if (useDryadClassic) {
            getItem().clearMetadata("dryad.duplicateItem");
            for (DryadDataPackage dryadDataPackage : resultList) {
                getItem().addMetadata("dryad.duplicateItem", null, String.valueOf(getItem().getID()), null, Choices.CF_NOVALUE);
            }
        } else {

        }
    }

    // Curation status methods
    /**
     * Generate a Dryad-formatted 'Submitted by ...' provenance string
     * @param date
     * @param submitterName
     * @param submitterEmail
     * @param provenanceStartId
     * @param bitstreamProvenanceMessage
     * @return
     */
    static String makeSubmittedProvenance(DCDate date, String submitterName,
            String submitterEmail, String provenanceStartId, String bitstreamProvenanceMessage) {
        StringBuilder builder = new StringBuilder();
        builder.append("Submitted by ");
        if(submitterName == null || submitterEmail == null) {
            builder.append("unknown (probably automated)");
        } else {
            builder.append(submitterName);
            builder.append(" (");
            builder.append(submitterEmail);
            builder.append(")");
        }
        builder.append(" on ");
        builder.append(date.toString());
        builder.append(" workflow start=");
        builder.append(provenanceStartId);
        builder.append("\n");
        builder.append(bitstreamProvenanceMessage);
        return builder.toString();
    }

    /**
     * Gets the most-recent provenance metadata beginning with
     * 'Submitted by '
     * @return the provenance information
     */
    public String getSubmittedProvenance() {
        String provenance = null;
        // Assumes metadata are ordered by place
        DCValue[] metadata = item.getMetadata(PROVENANCE_SCHEMA, PROVENANCE_ELEMENT, PROVENANCE_QUALIFIER, PROVENANCE_LANGUAGE);
        // find the last entry that starts with "Submitted by "
        ArrayUtils.reverse(metadata);
        for(DCValue dcValue : metadata) {
            if(dcValue.value.startsWith("Submitted by ")) {
                provenance = dcValue.value;
                break;
            }
        }
        return provenance;
    }

    /**
     * Adds Dryad-formatted 'Submitted by ...' metadata to a data package. Does
     * not remove existing provenance metadata.
     * @param date
     * @param submitterName
     * @param submitterEmail
     * @param provenanceStartId
     * @param bitstreamProvenanceMessage
     * @throws SQLException
     */
    public void addSubmittedProvenance(DCDate date, String submitterName,
                                       String submitterEmail, String provenanceStartId, String bitstreamProvenanceMessage) throws SQLException {
        String metadataValue = makeSubmittedProvenance(date, submitterName, submitterEmail, provenanceStartId, bitstreamProvenanceMessage);
        addSingleMetadataValue(Boolean.FALSE,PROVENANCE_SCHEMA, PROVENANCE_ELEMENT, PROVENANCE_QUALIFIER, PROVENANCE_LANGUAGE, metadataValue);
    }

    public void changeCurationStatus(String status, String reason) {
        if (getItem() != null) {
            item.addMetadata(PROVENANCE, "en", "PublicationUpdater: " + reason + " on " + DCDate.getCurrent().toString() + " (GMT)", null, -1);
        } else {
            // add a curation activity note
        }
    }

    public Date getEnteredReviewDate() {
        if (useDryadClassic) {
            DCValue[] provenanceValues = item.getMetadata("dc.description.provenance");
            if (provenanceValues != null && provenanceValues.length > 0) {
                for (DCValue provenanceValue : provenanceValues) {
                    //Submitted by Ricardo Rodríguez (ricardo_eyre@yahoo.es) on 2014-01-30T12:35:00Z workflow start=Step: requiresReviewStep - action:noUserSelectionAction\r
                    String provenance = provenanceValue.value;
                    Pattern pattern = Pattern.compile(".* on (.+?)Z.+requiresReviewStep.*");
                    Matcher matcher = pattern.matcher(provenance);
                    if (matcher.find()) {
                        String dateString = matcher.group(1);
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                        Date reviewDate = null;
                        try {
                            reviewDate = sdf.parse(dateString);
                            log.info("item " + item.getID() + " entered review on " + reviewDate.toString());
                            return reviewDate;
                        } catch (Exception e) {
                            log.error("couldn't find date in provenance for item " + item.getID() + ": " + dateString);
                            return null;
                        }
                    }
                }
            }
        } else {
            //something
        }
        return null;
    }

    public boolean isPackageInReview(Context c) {
        if (useDryadClassic) {
            return DryadWorkflowUtils.isItemInReview(c, getWorkflowItem(c));
        } else {
            // get curation status of package
        }
        return false;
    }

    // DSpace-specific methods (without Dash equivalents)
    public static Collection getCollection(Context context) throws SQLException {
        String handle = ConfigurationManager.getProperty(PACKAGES_COLLECTION_HANDLE_KEY);
        return DryadObject.collectionFromHandle(context, handle);
    }

    public static DryadDataPackage createInWorkflow(Context context) throws SQLException {
        /*
         * WorkflowItems are normally created by WorkflowManager.start(),
         * but this method has a lot of side effects (activating steps, sending
         * emails) and generally heavyweight.
         * Instead we'll just create rows in the workflowitem table for now.
         */
        Collection collection = DryadDataPackage.getCollection(context);
        DryadDataPackage dataPackage = null;
        try {
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, true);
            Item item = wsi.getItem();
            TableRow row = DatabaseManager.create(context, WORKFLOWITEM_TABLE);
            row.setColumn(WORKFLOWITEM_COLUMN_ITEMID, item.getID());
            row.setColumn(WORKFLOWITEM_COLUMN_COLLECTIONID, collection.getID());
            DatabaseManager.update(context, row);
            dataPackage = new DryadDataPackage(item);
            dataPackage.createIdentifier(context);
            wsi.deleteWrapper();
        } catch (IdentifierException ex) {
            log.error("Identifier exception creating a Data Package", ex);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data Package", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data Package", ex);
        }
        return dataPackage;
    }

    /**
     * Creates a DryadDataPackage object in the workspace. Written for testing
     * automatic workflow processors like approving from blackout
     * @param context database context to use
     * @return a DryadDataPackage with corresponding row in the workspaceitem table
     * @throws SQLException
     */
    public static DryadDataPackage createInWorkspace(Context context) throws SQLException {
        Collection collection = DryadDataPackage.getCollection(context);
        DryadDataPackage dataPackage = null;
        try {
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, true);
            Item item = wsi.getItem();
            dataPackage = new DryadDataPackage(item);
            dataPackage.createIdentifier(context);
        } catch (IdentifierException ex) {
            log.error("Identifier exception creating a Data Package", ex);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data Package", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data Package", ex);
        }
        return dataPackage;
    }

    public WorkflowItem getWorkflowItem(Context context) {
        try {
            return WorkflowItem.findByItemId(context, getItem().getID());
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting workflow item for data package", ex);
        } catch (IOException ex) {
            log.error("IO exception getting workflow item for data package", ex);
        } catch (SQLException ex) {
            log.error("SQL exception getting workflow item for data package", ex);
        }
        return null;
    }

    public WorkspaceItem getWorkspaceItem(Context context) throws SQLException {
        return WorkspaceItem.findByItemId(context, getItem().getID());
    }
    /**
     * Find any data packages containing the file by identifier. Used to prevent
     * files from appearing in multiple packages
     * @param context database context
     * @param dataFile a data file with an identifier
     * @return a set of data packages where dc.relation.haspart = the file's identifier
     * @throws SQLException
     */
    static Set<DryadDataPackage> getPackagesContainingFile(Context context, DryadDataFile dataFile) throws SQLException {
        Set<DryadDataPackage> packageSet = new HashSet<DryadDataPackage>();
        String fileIdentifier = dataFile.getIdentifier();
        if(fileIdentifier == null || fileIdentifier.length() == 0) {
            throw new IllegalArgumentException("Data file must have an identifier");
        }
        try {
            ItemIterator dataPackages = Item.findByMetadataField(context, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, fileIdentifier);
            while(dataPackages.hasNext()) {
                packageSet.add(new DryadDataPackage(dataPackages.next()));
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting data packages for file", ex);
        } catch (IOException ex) {
            log.error("IO exception getting data packages for file", ex);
        }
        return packageSet;
    }

    static Set<DryadDataFile> getFilesInPackage(Context context, DryadDataPackage dataPackage) throws SQLException {
        // files and packages are linked by DOI
        Set<DryadDataFile> fileSet = new HashSet<DryadDataFile>();
        String packageIdentifier = dataPackage.getIdentifier();
        if(packageIdentifier == null || packageIdentifier.length() == 0) {
            throw new IllegalArgumentException("Data package must have an identifier");
        }
        try {
            ItemIterator dataFiles = Item.findByMetadataField(context, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISPARTOF_QUALIFIER, packageIdentifier);
            while(dataFiles.hasNext()) {
                fileSet.add(new DryadDataFile(dataFiles.next()));
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting files for data package", ex);
        } catch (IOException ex) {
            log.error("IO exception getting files for data package", ex);
        }
        return fileSet;
    }

    public Set<DryadDataFile> getDataFiles(Context context) throws SQLException {
        if(dataFiles == null) {
            // how are data files and packages linked? By DOI
            dataFiles = DryadDataPackage.getFilesInPackage(context, this);
        }
        return dataFiles;
    }

    void setHasPart(DryadDataFile dataFile) throws SQLException {
        String dataFileIdentifier = dataFile.getIdentifier();
        if(dataFileIdentifier == null || dataFileIdentifier.length() == 0) {
            throw new IllegalArgumentException("Data file must have an identifier");
        }
        addSingleMetadataValue(Boolean.FALSE, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, dataFileIdentifier);
    }

    public void addDataFile(Context context, DryadDataFile dataFile) throws SQLException {
        dataFile.setDataPackage(context, this);
    }

    void clearDataFilesCache() {
        this.dataFiles = null;
    }

    /**
     * Removes the identifier for a data file from this package's
     * dc.relation.haspart metadata.
     * @param dataFile
     * @throws SQLException
     */
    public void removeDataFile(Context context, DryadDataFile dataFile) throws SQLException {
        String dataFileIdentifier = dataFile.getIdentifier();
        if(dataFileIdentifier == null) {
            throw new IllegalArgumentException("Data file must have an identifier");
        }

        // Get the metadata
        DCValue[] hasPartValues = getItem().getMetadata(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, Item.ANY);

        Integer indexOfFileIdentifier = indexOfValue(hasPartValues, dataFileIdentifier);
        if(indexOfFileIdentifier >= 0) {
            // remove that element from the array
            hasPartValues = (DCValue[]) ArrayUtils.remove(hasPartValues, indexOfFileIdentifier);
            // clear the metadata in the database
            getItem().clearMetadata(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, Item.ANY);
            // set them
            for(DCValue value : hasPartValues) {
                getItem().addMetadata(value.schema, value.element, value.qualifier, value.language, value.value, value.authority, value.confidence);
            }
            try {
                getItem().update();
            } catch (AuthorizeException ex) {
                log.error("Authorize exception removing data file from data package", ex);
            }
            dataFile.clearDataPackage();
        }
    }

    private Integer indexOfValue(final DCValue[] dcValues, final String value) {
        Integer foundIndex = -1;
        for(Integer index = 0;index < dcValues.length;index++) {
            if(dcValues[index].value.equals(value)) {
                foundIndex = index;
            }
        }
        return foundIndex;
    }

    @Override
    Set<DryadObject> getRelatedObjects(final Context context) throws SQLException {
        return new HashSet<DryadObject>(getDataFiles(context));
    }

    // static methods
    public static DryadDataPackage findByWorkflowItemId(Context context, Integer workflowItemId) {
        DryadDataPackage dataPackage = null;
        try {
            WorkflowItem wfi = WorkflowItem.find(context, workflowItemId);
            dataPackage = new DryadDataPackage(wfi.getItem());
        } catch (Exception ex) {
            log.error("Exception getting data package from Workflow Item ID", ex);
        }
        return dataPackage;
    }

    public static List<DryadDataPackage> findAllUnarchivedPackagesByISSN(Context context, String ISSN, boolean onlyRecent) {
        ArrayList<DryadDataPackage> dataPackageList = new ArrayList<>();
        if (useDryadClassic) {
            try {
                WorkflowItem[] itemArray = WorkflowItem.findAllByISSN(context, ISSN);
                for (WorkflowItem wfi : itemArray) {
                    DryadDataPackage dryadDataPackage = new DryadDataPackage(wfi.getItem());
                    if (DryadWorkflowUtils.isDataPackage(wfi)) {
                        Item item = wfi.getItem();
                        if (onlyRecent) {
                            LocalDate twoYearsAgo = LocalDate.now().minusYears(2);
                            LocalDate dateItemModified = item.getLastModified().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            if (dateItemModified.isAfter(twoYearsAgo)) {
                                dataPackageList.add(new DryadDataPackage(wfi.getItem()));
                            } else {
                                log.debug("skipping item " + item.getID() + " because it's too old");
                            }
                        } else {
                            dataPackageList.add(dryadDataPackage);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("couldn't find unarchived packages for journal " + ISSN);
            }
        } else {
            // something
        }
        return dataPackageList;
    }

    public static List<DryadDataPackage> findAllByManuscript(Context context, Manuscript manuscript) {
        ArrayList<DryadDataPackage> dataPackageList = new ArrayList<>();
        if (useDryadClassic) {
            try {
                // find all with same Dryad DOI
                ItemIterator itemIterator = Item.findByMetadataField(context, "dc", "identifier", null, manuscript.getDryadDataDOI(), false);
                while (itemIterator.hasNext()) {
                    dataPackageList.add(new DryadDataPackage(itemIterator.next()));
                }
                // find all with same publication DOI
                itemIterator = Item.findByMetadataField(context, "dc", "relation", "isreferencedby", manuscript.getPublicationDOI(), false);
                while (itemIterator.hasNext()) {
                    dataPackageList.add(new DryadDataPackage(itemIterator.next()));
                }
                // find all with same manuscript ID (in the same journal)
                itemIterator = Item.findByMetadataField(context, MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER, manuscript.getManuscriptId(), false);
                while (itemIterator.hasNext()) {
                    DryadDataPackage dataPackage = new DryadDataPackage(itemIterator.next());
                    if (dataPackage.getPublicationName().equals(manuscript.getJournalName()))
                        dataPackageList.add(dataPackage);
                }
            } catch (Exception ex) {
                log.error("Exception getting data package from publication DOI", ex);
            }
        } else {
            //something
        }
        return dataPackageList;
    }

    public void addDashTransferDate() {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String transferDate = sdf.format(now);
        addSingleMetadataValue(Boolean.FALSE, DASH_TRANSFER_SCHEMA, DASH_TRANSFER_ELEMENT, null, transferDate);
    }

    /**
     * Copies manuscript metadata into a dryad data package
     * @param manuscript
     * @param message
     * @throws SQLException
     */
    private void associateWithManuscript(Manuscript manuscript, StringBuilder message) {
        if (manuscript != null) {
            // set publication DOI
            if (!"".equals(manuscript.getPublicationDOI()) && !this.getPublicationDOI().equals(manuscript.getPublicationDOI())) {
                String oldValue = this.getPublicationDOI();
                this.setPublicationDOI(manuscript.getPublicationDOI());
                message.append(" publication DOI was updated from " + oldValue + ".");
            }
            // set Manuscript ID
            if (!"".equals(manuscript.getManuscriptId()) && !this.getManuscriptNumber().equals(manuscript.getManuscriptId())) {
                String oldValue = this.getManuscriptNumber();
                this.setManuscriptNumber(manuscript.getManuscriptId());
                message.append(" manuscript number was updated from " + oldValue + ".");
            }
//            // union keywords
//            if (manuscript.getKeywords().size() > 0) {
//                ArrayList<String> unionKeywords = new ArrayList<String>();
//                unionKeywords.addAll(dataPackage.getKeywords());
//                for (String newKeyword : manuscript.getKeywords()) {
//                    if (!unionKeywords.contains(newKeyword)) {
//                        unionKeywords.add(newKeyword);
//                    }
//                }
//                dataPackage.setKeywords(unionKeywords);
//            }
            // set title
            if (!"".equals(manuscript.getTitle()) && !this.getTitle().equals(manuscript.getTitle())) {
                String oldValue = this.getTitle();
                this.setTitle(String.format("Data from: %s", manuscript.getTitle()));
                message.append(" article title was updated from \"" + oldValue + "\".");
            }
            // set abstract
            if (!"".equals(manuscript.getAbstract()) && !this.getAbstract().equals(manuscript.getAbstract())) {
                this.setAbstract(manuscript.getAbstract());
                message.append(" abstract was updated.");
            }
            // set publicationDate
            if (manuscript.getPublicationDate() != null) {
                SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = dateIso.format(manuscript.getPublicationDate());
                String oldValue = this.getPublicationDate();
                if (!dateString.equals(oldValue)) {
                    this.setPublicationDate(dateString);
                    message.append(" publication date was updated from " + oldValue + ".");
                }
            }
        }
    }

    private void disassociateFromManuscript(Manuscript manuscript) {
        if (manuscript != null) {
            // clear publication DOI
            this.setPublicationDOI(null);
            // If there is a manuscript number, move it to former msid
            this.setFormerManuscriptNumber(this.getManuscriptNumber());
            // clear Manuscript ID
            this.setManuscriptNumber(null);
            // disjoin keywords
            List<String> packageKeywords = this.getKeywords();
            List<String> manuscriptKeywords = manuscript.getKeywords();
            List<String> prunedKeywords = subtractList(packageKeywords, manuscriptKeywords);

            this.setKeywords(prunedKeywords);
            // clear publicationDate
            this.setBlackoutUntilDate(null);
        }
    }

    private static List<String> subtractList(List<String> list1, List<String> list2) {
        List<String> list = new ArrayList<String>(list1);
        for(String string : list2) {
            if(list.contains(string)) {
                list.remove(string);
            }
        }
        return list;
    }

    public void approvePackageUsingManuscript(Context c, Manuscript manuscript) {
        StringBuilder reason = new StringBuilder();
        // Add provenance to item
        String manuscriptNumber = "<null>";
        if (manuscript != null) {
            manuscriptNumber = manuscript.getManuscriptId();
        }
        reason.append("Approved by ApproveRejectReviewItem based on metadata for ").append(manuscriptNumber).append(" on ").append(DCDate.getCurrent().toString()).append(" (GMT)");

        if (useDryadClassic) {
            c.turnOffAuthorisationSystem();
            associateWithManuscript(manuscript, reason);
            try {
                WorkflowItem wfi = getWorkflowItem(c);
                List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(c, wfi.getID());
                ClaimedTask claimedTask = claimedTasks.get(0);
                Workflow workflow = WorkflowFactory.getWorkflow(wfi.getCollection());
                WorkflowActionConfig actionConfig = workflow.getStep(claimedTask.getStepID()).getActionConfig(claimedTask.getActionID());

                addSingleMetadataValue(true, WorkflowRequirementsManager.WORKFLOW_SCHEMA, "step", "approved", null, Manuscript.statusIsApproved(manuscript.getStatus()).toString());
                WorkflowManager.doState(c, c.getCurrentUser(), null, claimedTask.getWorkflowItemID(), workflow, actionConfig);
                addSingleMetadataValue(false, MetadataSchema.DC_SCHEMA, "description", "provenance", "en", reason.toString());
                c.commit();
            } catch (Exception e) {
                log.error("Exception approving package: " + e.getMessage());
            } finally {
                c.restoreAuthSystemState();
            }

        } else {
            changeCurationStatus("Curation", reason.toString());
        }
    }

    public void rejectPackageUsingManuscript(Context c, Manuscript manuscript, String reason) {
        if (useDryadClassic) {
            try {
                c.turnOffAuthorisationSystem();
                disassociateFromManuscript(manuscript);
                WorkflowItem wfi = getWorkflowItem(c);
                EPerson ePerson = EPerson.findByEmail(c, ConfigurationManager.getProperty("system.curator.account"));
                //Also reject all the data files
                Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wfi.getItem());
                for (Item dataFile : dataFiles) {
                    try {
                        WorkflowManager.rejectWorkflowItem(c, WorkflowItem.findByItemId(c, dataFile.getID()), ePerson, null, reason, false);
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
                WorkspaceItem wsi = WorkflowManager.rejectWorkflowItem(c, wfi, ePerson, null, reason, true);
            } catch (Exception e) {
                log.error("Exception approving package: " + e.getMessage());
            } finally {
                c.restoreAuthSystemState();
            }
        } else {

        }
    }

    public boolean updateMetadataFromManuscript(Manuscript manuscript, Context context, StringBuilder provenance) {
        HashSet<String> fieldsChanged = new HashSet<>();
        log.debug("comparing metadata for package " + getIdentifier() + " to manuscript " + manuscript.toString());
        // first, check to see if this is one of the known mismatches:
        if (isManuscriptMismatchForPackage(manuscript)) {
            log.error("pub " + manuscript.getPublicationDOI() + " is known to be a mismatch for " + getIdentifier());
            return false;
        }

        if (!"".equals(manuscript.getPublicationDOI()) && !getPublicationDOI().equals(manuscript.getPublicationDOI())) {
            fieldsChanged.add(PUBLICATION_DOI);
            setPublicationDOI(manuscript.getPublicationDOI());
            log.debug("adding publication DOI " + manuscript.getPublicationDOI());
            provenance.append(" " + PUBLICATION_DOI + " was updated.");
        }
        if (!"".equals(manuscript.getManuscriptId()) && !getManuscriptNumber().equals(manuscript.getManuscriptId())) {
            fieldsChanged.add(MANUSCRIPT_NUMBER);
            setManuscriptNumber(manuscript.getManuscriptId());
            log.debug("adding msid " + manuscript.getManuscriptId());
            provenance.append(" " + MANUSCRIPT_NUMBER + " was updated.");
        }

        if (manuscript.getPublicationDate() != null) {
            SimpleDateFormat dateIso = new SimpleDateFormat("yyyy-MM-dd");
            String msDateString = dateIso.format(manuscript.getPublicationDate());
            String pubDateString = getPublicationDate();
            if (pubDateString.length() > 6) {
                pubDateString = getPublicationDate().substring(0, 7);
            }
            if (!pubDateString.equals(msDateString.substring(0, 7))) {
                fieldsChanged.add(PUBLICATION_DATE);
                setPublicationDate(msDateString);
                log.debug("adding pub date " + msDateString);
                provenance.append(" " + PUBLICATION_DATE + " was updated.");
            }

        }

        // Only required for Dryad Classic, as Dash won't have manually-curated citation metadata
        if (useDryadClassic) {
            if (!"".equals(manuscript.getFullCitation())) {
                String itemCitation = "";
                DCValue[] citations = item.getMetadata(FULL_CITATION);
                if (citations != null && citations.length > 0) {
                    itemCitation = citations[0].value;
                }
                double score = JournalUtils.getHamrScore(manuscript.getFullCitation().toLowerCase(), itemCitation.toLowerCase());
                log.debug("old citation was: " + itemCitation);
                log.debug("new citation is: " + manuscript.getFullCitation());
                log.debug("citation match score is " + score);
                // old citation doesn't match new citation, or old citation had "null" for page number (match score is still v high)
                if ((score < 0.95) || (itemCitation.contains("null"))) {
                    fieldsChanged.add(FULL_CITATION);
                    item.clearMetadata(FULL_CITATION);
                    item.addMetadata(FULL_CITATION, null, manuscript.getFullCitation(), null, -1);
                    log.debug("adding citation " + manuscript.getFullCitation());
                    provenance.append(" " + FULL_CITATION + " was updated.");
                }
            }
            if (fieldsChanged.size() > 0) {
                item.clearMetadata(CITATION_IN_PROGRESS);
                item.addMetadata(CITATION_IN_PROGRESS, null, "true", null, -1);
            }
            try {
                item.update();
                context.commit();
            } catch (Exception e) {
                log.error("couldn't save metadata: " + e.getMessage());
            }
        }
        if (fieldsChanged.size() > 0) {
            if (!"".equals(provenance.toString())) {
                log.info("writing provenance for package " + getIdentifier() + ": " + provenance);
                changeCurationStatus("Status Unchanged", provenance.toString());
            }

            // only return true if we want to get email notifications about this update: FULL_CITATION or PUBLICATION_DOI was updated.
            return (fieldsChanged.contains(FULL_CITATION) || fieldsChanged.contains(PUBLICATION_DOI));
        } else {
            log.debug("nothing changed");
            return false;
        }
    }

    private boolean isManuscriptMismatchForPackage(Manuscript manuscript) {
        if ("".equals(manuscript.getPublicationDOI())) {
            return false;
        }
        log.debug("looking for mismatches for " + manuscript.getPublicationDOI());
        // normalize the pubDOI from the manuscript: remove leading "doi:" or "doi.org/"
        String msDOI = null;
        Pattern doi = Pattern.compile(".*(10\\.\\d+/.+)");
        Matcher m = doi.matcher(manuscript.getPublicationDOI().toLowerCase());
        if (m.matches()) {
            msDOI = m.group(1);
        }
        if (msDOI == null) {
            log.error("msDOI not in correct format");
            return false;
        }
        List<String> itemMismatches = getMismatchedDOIs();
        for (String dcv : itemMismatches) {
            m = doi.matcher(dcv.toLowerCase());
            if (m.matches()) {
                if (msDOI.equals(m.group(1))) {
                    log.error("found a mismatch: " + m.group(1));
                    return true;
                }
            }
        }
        log.error("no mismatches");
        return false;
    }

    // Convenience method to access a properly serialized JSON-LD string, formatted for Schema.org.
    public String getSchemaDotOrgJSON() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.registerModule(new SimpleModule().addSerializer(Author.class, new Author.SchemaDotOrgSerializer()));
            mapper.registerModule(new SimpleModule().addSerializer(Package.class, new Package.SchemaDotOrgSerializer()));
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Package(this));
        } catch (Exception e) {
            log.error("Unable to serialize Schema.org JSON", e);
            return "";
        }
    }

    // Convenience method to access a properly serialized JSON string, formatted for use with DASH.
    public String getDashJSON() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.registerModule(new SimpleModule().addSerializer(Author.class, new Author.DashSerializer()));
            mapper.registerModule(new SimpleModule().addSerializer(Package.class, new Package.DashSerializer()));
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new Package(this));
        } catch (Exception e) {
            log.error("Unable to serialize Dash-style JSON", e);
            return "";
        }
    }
}

