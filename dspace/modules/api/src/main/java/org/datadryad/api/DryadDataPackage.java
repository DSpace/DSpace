/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.workflow.WorkflowItem;

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

    private static final String BLACKOUT_UNTIL_SCHEMA = "dc";
    private static final String BLACKOUT_UNTIL_ELEMENT = "date";
    private static final String BLACKOUT_UNTIL_QUALIFIER = "blackoutUntil";

    // Publication DOI in a data package
    private static final String RELATION_ISREFERENCEDBY_QUALIFIER = "isreferencedby";

    private static final String TITLE_SCHEMA = "dc";
    private static final String TITLE_ELEMENT = "title";

    private static final String KEYWORD_SCHEMA = "dc";
    private static final String KEYWORD_ELEMENT = "subject";

    private final static String PUBLICATION_DATE_SCHEMA = "dc";
    private final static String PUBLICATION_DATE_ELEMENT = "date";
    private final static String PUBLICATION_DATE_QUALIFIER = "issued";

    private static final String DASH_TRANSFER_SCHEMA = "dryad";
    private static final String DASH_TRANSFER_ELEMENT = "dashTransferDate";
    
    private Set<DryadDataFile> dataFiles;
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
    }

    public static Collection getCollection(Context context) throws SQLException {
        String handle = ConfigurationManager.getProperty(PACKAGES_COLLECTION_HANDLE_KEY);
        return DryadObject.collectionFromHandle(context, handle);
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

    public WorkflowItem getWorkflowItem(Context context) throws SQLException {
        try {
            return WorkflowItem.findByItemId(context, getItem().getID());
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting workflow item for data package", ex);
        } catch (IOException ex) {
            log.error("IO exception getting workflow item for data package", ex);
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

    static Integer indexOfValue(final DCValue[] dcValues, final String value) {
        Integer foundIndex = -1;
        for(Integer index = 0;index < dcValues.length;index++) {
            if(dcValues[index].value.equals(value)) {
                foundIndex = index;
            }
        }
        return foundIndex;
    }

    public String getPublicationDate() {
        return getSingleMetadataValue(PUBLICATION_DATE_SCHEMA, PUBLICATION_DATE_ELEMENT, PUBLICATION_DATE_QUALIFIER);
    }

    public void setPublicationDate(String publicationDate) throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, PUBLICATION_DATE_SCHEMA, PUBLICATION_DATE_ELEMENT, PUBLICATION_DATE_QUALIFIER, publicationDate);
    }

    public String getPublicationName() {
        return getSingleMetadataValue(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER);
    }

    public void setPublicationName(String publicationName) throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, publicationName);
    }

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

    
    @Override
    Set<DryadObject> getRelatedObjects(final Context context) throws SQLException {
        return new HashSet<DryadObject>(getDataFiles(context));
    }

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

    // this currently uses the Item lookup; we will need to make a more abstract one that can encompass Dash
    private static List<DryadDataPackage> findByManuscriptNumber(Context context, String manuscriptNumber) throws SQLException {
        ArrayList<DryadDataPackage> dataPackageList = new ArrayList<>();
        try {
            ItemIterator itemIterator = Item.findByMetadataField(context, MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER, manuscriptNumber);
            while (itemIterator.hasNext()) {
                dataPackageList.add(new DryadDataPackage(itemIterator.next()));
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting data package from manuscript number", ex);
        } catch (IOException ex) {
            log.error("IO exception getting data package from manuscript number", ex);
        }
        return dataPackageList;
    }

    // this currently uses the Item lookup; we will need to make a more abstract one that can encompass Dash
    private static List<DryadDataPackage> findByPublicationDOI(Context context, String pubDOI) throws SQLException {
        ArrayList<DryadDataPackage> dataPackageList = new ArrayList<>();
        try {
            ItemIterator itemIterator = Item.findByMetadataField(context, "dc", "relation", "isreferencedby", pubDOI, false);
            while (itemIterator.hasNext()) {
                dataPackageList.add(new DryadDataPackage(itemIterator.next()));
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting data package from publication DOI", ex);
        } catch (IOException ex) {
            log.error("IO exception getting data package from publication DOI", ex);
        }
        return dataPackageList;
    }

    // this currently uses Item-based lookup; we will need to make a more abstract one that can encompass Dash
    public Date getEnteredReviewDate() {
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
        return null;
    }

    public String getManuscriptNumber() {
        return getSingleMetadataValue(MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER);
    }

    public void setManuscriptNumber(String manuscriptNumber) throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER, manuscriptNumber);
    }

    public List<String> getFormerManuscriptNumbers() {
        return getMultipleMetadataValues(FORMER_MANUSCRIPT_NUMBER_SCHEMA, FORMER_MANUSCRIPT_NUMBER_ELEMENT, FORMER_MANUSCRIPT_NUMBER_QUALIFIER);
    }

    public void setFormerManuscriptNumber(String manuscriptNumber) throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, FORMER_MANUSCRIPT_NUMBER_SCHEMA, FORMER_MANUSCRIPT_NUMBER_ELEMENT, FORMER_MANUSCRIPT_NUMBER_QUALIFIER, manuscriptNumber);
    }
    public void setBlackoutUntilDate(Date blackoutUntilDate) throws SQLException {
        String dateString = null;
        if(blackoutUntilDate != null)  {
             dateString = new DCDate(blackoutUntilDate).toString();
        }
        addSingleMetadataValue(Boolean.TRUE, BLACKOUT_UNTIL_SCHEMA, BLACKOUT_UNTIL_ELEMENT, BLACKOUT_UNTIL_QUALIFIER, dateString);
    }

    public Date getBlackoutUntilDate() {
        Date blackoutUntilDate = null;
        String dateString =getSingleMetadataValue(BLACKOUT_UNTIL_SCHEMA, BLACKOUT_UNTIL_ELEMENT, BLACKOUT_UNTIL_QUALIFIER);
        if(dateString != null) {
            blackoutUntilDate = new DCDate(dateString).toDate();
        }
        return blackoutUntilDate;
    }

    public void setPublicationDOI(String publicationDOI) throws SQLException {
        // check that this DOI starts with the doi: prefix. if not, add it.
        if (publicationDOI != null) {
            Pattern doiPattern = Pattern.compile("^doi:.*");
            Matcher matcher = doiPattern.matcher(publicationDOI);
            if (!("".equals(publicationDOI)) && !matcher.find()) {
                publicationDOI = "doi:" + publicationDOI;
            }
        }
        addSingleMetadataValue(Boolean.FALSE, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISREFERENCEDBY_QUALIFIER, publicationDOI);
    }

    public void clearPublicationDOI() throws SQLException {
        // Need to filter just on metadata values that are publication DOIs
        addSingleMetadataValue(Boolean.TRUE, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISREFERENCEDBY_QUALIFIER, null);
    }

    /**
     * Get the publication DOI. Does not account for pubmed IDs, assumes
     * first dc.relation.isreferencedby is the publication DOI
     * @return
     * @throws SQLException
     */
    public String getPublicationDOI() {
        return getSingleMetadataValue(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISREFERENCEDBY_QUALIFIER);
    }

    public String getTitle() {
        return getSingleMetadataValue(TITLE_SCHEMA, TITLE_ELEMENT, null);
    }

    public void setAbstract(String theAbstract) throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, "dc", "description", null, theAbstract);
    }

    public String getAbstract() {
        String theAbstract = getSingleMetadataValue("dc", "description", null);
        String extraAbstract = getSingleMetadataValue("dc", "description", "abstract");

        if (extraAbstract != null && extraAbstract.length() > 0) {
            theAbstract = theAbstract + "\n" + extraAbstract;
        }

        return theAbstract;
    }

    public List<String> getKeywords() throws SQLException {
        return getMultipleMetadataValues(KEYWORD_SCHEMA, KEYWORD_ELEMENT, null);
    }

    public void setKeywords(List<String> keywords) throws SQLException {
        addMultipleMetadataValues(Boolean.TRUE, KEYWORD_SCHEMA, KEYWORD_ELEMENT, null, keywords);
    }

    public void addKeywords(List<String> keywords) throws SQLException {
        addMultipleMetadataValues(Boolean.FALSE, KEYWORD_SCHEMA, KEYWORD_ELEMENT, null, keywords);
    }

    public void addDashTransferDate() throws SQLException {
        Date now = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd'T'HH:mm:ss.SSSZ");
        String transferDate = sdf.format(now);
        addSingleMetadataValue(Boolean.FALSE, DASH_TRANSFER_SCHEMA, DASH_TRANSFER_ELEMENT, null, transferDate);
    }

    public List<Author> getAuthors() {
        ArrayList<Author> authors = new ArrayList<Author>();
        DCValue[] metadata = item.getMetadata("dc", "contributor", "author", Item.ANY);
        for(DCValue dcValue : metadata) {
            authors.add(new Author(dcValue));
        }
        metadata = item.getMetadata("dc", "contributor", null, Item.ANY);
        for(DCValue dcValue : metadata) {
            authors.add(new Author(dcValue));
        }
        metadata = item.getMetadata("dc", "creator", null, Item.ANY);
        for(DCValue dcValue : metadata) {
            authors.add(new Author(dcValue));
        }
        return authors;
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
            // look for items that have the same pub DOI
            if (this.getPublicationDOI() != null && !this.getPublicationDOI().equals("")) {
                log.debug("looking for items with doi " + this.getPublicationDOI());
                List<DryadDataPackage> packagesWithDOI = findByPublicationDOI(context, this.getPublicationDOI());
                for (DryadDataPackage aPackage: packagesWithDOI) {
                    if (!resultList.contains(aPackage)) {
                        resultList.add(aPackage);
                    }
                }
            }
            // look for items that have the same msid
            if (this.getManuscriptNumber() != null && !this.getManuscriptNumber().equals("")) {
                log.debug("looking for items with msid " + this.getManuscriptNumber());
                List<DryadDataPackage> packagesWithMSID = findByManuscriptNumber(context, this.getManuscriptNumber());
                for (DryadDataPackage aPackage : packagesWithMSID) {
                    // check journal name
                    String journalName = this.getPublicationName();
                    log.debug("found an item " + this.getIdentifier() + " with same msid " + this.getManuscriptNumber());
                    if (!resultList.contains(aPackage)) {
                        if (this.getPublicationName().equals(journalName)) {
                            resultList.add(aPackage);
                        }
                    }
                }
            }
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

    /**
     * Copies manuscript metadata into a dryad data package
     * @param manuscript
     * @param message
     * @throws SQLException
     */
    public void associateWithManuscript(Manuscript manuscript, StringBuilder message) throws SQLException {
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

    public void disassociateFromManuscript(Manuscript manuscript) throws SQLException {
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

