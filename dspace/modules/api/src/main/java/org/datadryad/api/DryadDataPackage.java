/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;
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

    private static final String BLACKOUT_UNTIL_SCHEMA = "dc";
    private static final String BLACKOUT_UNTIL_ELEMENT = "date";
    private static final String BLACKOUT_UNTIL_QUALIFIER = "blackoutUntil";

    // Publication DOI in a data package
    private static final String RELATION_ISREFERENCEDBY_QUALIFIER = "isreferencedby";

    private static final String TITLE_SCHEMA = "dc";
    private static final String TITLE_ELEMENT = "title";

    private static final String ABSTRACT_SCHEMA = "dc";
    private static final String ABSTRACT_ELEMENT = "description";

    private static final String KEYWORD_SCHEMA = "dc";
    private static final String KEYWORD_ELEMENT = "subject";

    private Set<DryadDataFile> dataFiles;
    private static Logger log = Logger.getLogger(DryadDataPackage.class);

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
            dataFile.clearDataPackage(context);
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

    public static DryadDataPackage findByIdentifier(Context context, String doi) throws IdentifierException {
        DryadDataPackage dataPackage = null;
        IdentifierService service = new DSpace().getSingletonService(IdentifierService.class);
        DSpaceObject object = service.resolve(context, doi);
        if(object.getType() == Constants.ITEM) {
            dataPackage = new DryadDataPackage((Item)object);
        } else {
            throw new IdentifierException("DOI " + doi + " does not resolve to an item");
        }
        return dataPackage;
    }

    // From http://stackoverflow.com/questions/13592236/parse-the-uri-string-into-name-value-collection-in-java
    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    /**
     * Finds a data package by the reviewer URL. reviewer URL may have DOI or wfID
     * @param context Database context
     * @param reviewerURL a URL containing doi or wfID query parameters
     * @return a DryadDataPackage if one exists matching the identifier
     */
    public static DryadDataPackage findByReviewerURL(Context context, String reviewerURL) throws IdentifierException, SQLException {
        DryadDataPackage dataPackage = null;
        // Decompose the reviewer URL. Contains identifiers in query parameters:
        // wfID or doi
        try {
            URL url = new URL(reviewerURL);
            Map<String, String> queryMap = splitQuery(url);
            if(queryMap.containsKey("doi")) {
                String doi = queryMap.get("doi");
                return findByIdentifier(context, doi);
            } else if(queryMap.containsKey("wfID")) {
                Integer workflowItemId = Integer.valueOf(queryMap.get("wfID"));
                return findByWorkflowItemId(context, workflowItemId);
            }
        } catch (MalformedURLException ex) {
            log.error("Unable to parse URL: " + reviewerURL, ex);
        } catch (UnsupportedEncodingException ex) {
            log.error("Unable to decode URL:" + reviewerURL, ex);
        } catch (NumberFormatException ex) {
            log.error("Unable to read workflow id", ex);
        }
        return dataPackage;
    }

    public static DryadDataPackage findByWorkflowItemId(Context context, Integer workflowItemId) throws SQLException {
        DryadDataPackage dataPackage = null;
        try {
            WorkflowItem wfi = WorkflowItem.find(context, workflowItemId);
            dataPackage = new DryadDataPackage(wfi.getItem());
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting data package from Workflow Item ID", ex);
        } catch (IOException ex) {
            log.error("IO exception getting data package from Workflow Item ID", ex);
        }
        return dataPackage;
    }

    public static DryadDataPackage findByManuscriptNumber(Context context, String manuscriptNumber) throws SQLException {
        DryadDataPackage dataPackage = null;
        try {
            ItemIterator dataPackages = Item.findByMetadataField(context, MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER, manuscriptNumber);
            if(dataPackages.hasNext()) {
                dataPackage = new DryadDataPackage(dataPackages.next());
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting data package from manuscript number", ex);
        } catch (IOException ex) {
            log.error("IO exception getting data package from manuscript number", ex);
        }
        return dataPackage;
    }

    public String getManuscriptNumber() throws SQLException {
        return getSingleMetadataValue(MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER);
    }

    public void setManuscriptNumber(String manuscriptNumber) throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, MANUSCRIPT_NUMBER_SCHEMA, MANUSCRIPT_NUMBER_ELEMENT, MANUSCRIPT_NUMBER_QUALIFIER, manuscriptNumber);
    }

    public void setBlackoutUntilDate(Date blackoutUntilDate) throws SQLException {
        String dateString = null;
        if(blackoutUntilDate != null)  {
             dateString = new DCDate(blackoutUntilDate).toString();
        }
        addSingleMetadataValue(Boolean.TRUE, BLACKOUT_UNTIL_SCHEMA, BLACKOUT_UNTIL_ELEMENT, BLACKOUT_UNTIL_QUALIFIER, dateString);
    }

    public Date getBlackoutUntilDate() throws SQLException {
        Date blackoutUntilDate = null;
        String dateString =getSingleMetadataValue(BLACKOUT_UNTIL_SCHEMA, BLACKOUT_UNTIL_ELEMENT, BLACKOUT_UNTIL_QUALIFIER);
        if(dateString != null) {
            blackoutUntilDate = new DCDate(dateString).toDate();
        }
        return blackoutUntilDate;
    }

    public void setPublicationDOI(String publicationDOI) throws SQLException {
        // Need to filter just on metadata values that are publication DOIs
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
    public String getPublicationDOI() throws SQLException {
        return getSingleMetadataValue(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISREFERENCEDBY_QUALIFIER);
    }

    public void setTitle(String title) throws SQLException {
        // Need to filter just on metadata values that are publication DOIs
        addSingleMetadataValue(Boolean.TRUE, TITLE_SCHEMA, TITLE_ELEMENT, null, title);
    }

    public String getTitle() throws SQLException {
        return getSingleMetadataValue(TITLE_SCHEMA, TITLE_ELEMENT, null);
    }

    public void setAbstract(String theAbstract) throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, ABSTRACT_SCHEMA, ABSTRACT_ELEMENT, null, theAbstract);
    }

    public String getAbstract() throws SQLException {
        return getSingleMetadataValue(ABSTRACT_SCHEMA, ABSTRACT_ELEMENT, null);
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
}
