package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.app.util.NoidGenerator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.doi.DryadDOIRegistrationHelper;
import org.dspace.doi.DOI;

import org.dspace.doi.DOIFormatException;
import org.dspace.doi.Minter;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.DryadWorkflowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 20-dec-2010
 * Time: 14:09:07
 * <p/>
 * The identifier service implementation using the NESCent doi webservices
 * This class will register a doi identifier for an item and can also be used to retrieve an item by its doi.
 */
@Component
public class DOIIdentifierProvider extends IdentifierProvider implements org.springframework.beans.factory.InitializingBean {

    private static Logger log = Logger.getLogger(DOIIdentifierProvider.class);

    private static DCValue identifierMetadata = new DCValue();

    private static final char DOT = '.';

    private static final char SLASH = '/';

    // Max number of files attached to a package; completely arbitrary
    private static final int MAX_NUM_OF_FILES = 150;

    private String myHdlPrefix;

    private String myDataPkgColl;

    private String myDataFileColl;

    private static String myLocalPartPrefix;

    private static String myDoiPrefix;
    private String myBlackoutURL;

    private int mySuffixVarLength;

    Minter perstMinter = null;

    private String[] supportedPrefixes = new String[]{"info:doi/", "doi:" , "http://dx.doi.org/"};

    static {
        identifierMetadata.schema = MetadataSchema.DC_SCHEMA;
        identifierMetadata.element = "identifier";
        identifierMetadata.qualifier = null;
    }


    public void afterPropertiesSet() throws Exception {

        myHdlPrefix = configurationService.getProperty("handle.prefix");
        myBlackoutURL = configurationService.getProperty("dryad.blackout.url");
        myDataPkgColl = configurationService.getProperty("stats.datapkgs.coll");
        myDataFileColl = configurationService.getProperty("stats.datafiles.coll");
        if (configurationService.getPropertyAsType("doi.service.testmode", true)) {
            myDoiPrefix = configurationService.getProperty("doi.testprefix");
            myLocalPartPrefix = configurationService.getProperty("doi.localpart.testsuffix");
        } else {
            myDoiPrefix = configurationService.getProperty("doi.prefix");
            myLocalPartPrefix = configurationService.getProperty("doi.localpart.suffix");
        }

        try{
            mySuffixVarLength = Integer.parseInt(configurationService.getProperty("doi.suffix.length"));
        }catch (NumberFormatException nfe){
            mySuffixVarLength=5;
        }

    }

    public static String getDryadDOIPrefix() {
        return myDoiPrefix + "/" + myLocalPartPrefix;
    }

    public boolean supports(String identifier)
    {
        for(String prefix : supportedPrefixes){
            if(identifier.startsWith(prefix))
                return true;
        }

        return false;
    }

    public String register(Context context, DSpaceObject dso) throws IdentifierException {
        try {
            if (dso instanceof Item && dso.getHandle() != null) {
                String doi = mintAndRegister(context, (Item) dso, true);
                updateItemDOIMetadata((Item) dso, doi);
            }
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to register doi", "Item id: " + dso.getID()));
            throw new IdentifierException("Error while registering doi identifier", e);
        }
        return null;

    }

    public String mint(Context context, DSpaceObject dso) throws IdentifierException {
        try {
            if (dso instanceof Item && dso.getHandle() != null) {
                Item item = (Item) dso;
                String doi = mintAndRegister(context, item, false);
                updateItemDOIMetadata(item, doi);
            }
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to mint doi", "Item id: " + dso.getID()));
            throw new IdentifierException("Error while retrieving doi identifier", e);
        }
        return null;
    }

    public void moveCanonical(Context context, DSpaceObject dso) throws IdentifierException {
        try{
            Item item = (Item) dso;
            String collection = getCollection(context, item);
            String doiString = getDoiValue((Item) dso);
            addNewDOItoItem(getCanonicalDOIString(doiString), item, true, collection);

            // if 1st version mint .1
            mintDOIAtVersion(context, doiString, item, 1);

        }catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to addNewDOItoItem doi", "Item id: " + dso.getID()));
            throw new IdentifierException("Error while moving doi identifier", e);
        }
    }


    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        try {
            if (dso instanceof Item) {
                Item item = (Item) dso;
                String doiString = getDoiValue((Item) dso);
                if(doiString!=null){
                    // Remove from DOI service only if the item is not registered
                    if(!item.isArchived()){
                        remove(doiString);
                    }
                    // if it is already registered it has to remain in DOI service and when someone looks for it go towards a "tombstone" page
                    // reassign the URL of the DOI
                    else {
                        DOI removedDOI = new DOI(doiString, DOI.Type.TOMBSTONE);
                        mint(removedDOI, true, null);
                    }

                    VersionHistory history = retrieveVersionHistory(context, item);
                    if (history != null && history.size() > 1) {
                        Item previous = history.getPrevious(history.getLatestVersion()).getItem();

                        // if the item we're deleting is the most recent version in the history
                        // switch the canonical DOI for the item to the previous version.
                        if (history.getLatestVersion().getItem().equals(item)) {
                            String collection = getCollection(context, previous);
                            addNewDOItoItem(getCanonicalDOIString(doiString), previous, true, collection);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while deleting doi identifier", "Item id: " + dso.getID()));
            throw new IdentifierException("Error while deleting doi identifier", e);
        }
    }


    private String mintAndRegister(Context context, Item item, boolean register) throws Exception {
        String doi = getDoiValue(item);
        String collection = getCollection(context, item);
        VersionHistory history = retrieveVersionHistory(context, item);

        // CASE A: it is a versioned datafile and the user is modifying its content (adding or removing bitstream): upgrade version number.
        if(item.isArchived()){
            if(!collection.equals(myDataPkgColl)){
                if(lookup(doi)!=null){
                    log.debug("case A -- updating DOI info for versioned data file");
                    DOI doi_= upgradeDOIDataFile(context, doi, item, history);
                    if(doi_!=null){
                        remove(doi);
                        // Not checking for blackout here because item is already archived
                        mint(doi_, register, createListMetadata(item));

                        item.clearMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null);
                        item.update();
                        if (doi == null || doi.equals("")) throw new Exception();
                    }
                }
            }
        }

        // CASE B: New Item or New version
        // FIRST time a VERSION is created 2 identifiers will be minted  and the canonical will be updated to point to the newer URL:
        //  - id.1-->old URL
        //  - id.2-->new URL
        //  - id(canonical)-- new URL
        // Next times 1 identifier will be minted  and the canonical will be updated to point to the newer URL
        //  - id.x-->new URL
        //  - id(canonical)-- new URL
        // If it is a new ITEM just 1 identifier will be minted

        else{
            DOI doi_ = calculateDOI(context, doi, item, history);

            log.info("DOI just minted: " + doi_);

            doi = doi_.toString();
            if(DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(item)) {
                mint(doi_, myBlackoutURL, register, createListMetadata(item));
            } else {
                mint(doi_, register, createListMetadata(item));
            }

        }
        return doi;
    }

    // When a DOI needs to be versioned for the first time:
    // if the item has a versionhistory and the previous version was the first version,
    // update that previous item to be version 1: dryad.xxxx.1
    private DOI mintDOIAtVersion(Context context, String doiString, Item item, int versionNumber) throws SQLException, IOException, AuthorizeException {
        VersionHistory history = retrieveVersionHistory(context, item);

        if (history != null) {
            Version version = history.getVersion(item);
            // if it is the first time that is called "create version": mint identifier ".1"
            Version previous = history.getPrevious(version);
            // if the previous version was the first version,
            // update that previous item's metadata with the .1 versioned identifier.
            if (history.isFirstVersion(previous)) {
                Item previousItem = previous.getItem();
                String previousDOI = getDoiValue(previousItem);
                String collection = getCollection(context, previousItem);
                if (collection.equals(myDataFileColl)) {
                    previousDOI = calculateDOIDataFileFirstTime(previousItem);
                    String oldDOIstring = getDoiValue(previousItem);
                    Item dataPackage = DryadWorkflowUtils.getDataPackage(context,previousItem);
                    if(dataPackage!=null) {
                        VersionHistory dpHistory = retrieveVersionHistory(context, dataPackage);

                        if (dpHistory != null) {
                            // if it is the first time that is called "create version": mint identifier ".1"
                            Version dpHistoryPrevious = dpHistory.getPrevious(dpHistory.getVersion(dataPackage));
                            if(dpHistoryPrevious!=null && oldDOIstring!=null)
                            {
                                Item previousDataPackage = dpHistoryPrevious.getItem();
                                // check to see if the version number for the previous package matches the previous file:
                                // if so, update the hasPart and isPart for that previous package.
                                if (getDOIVersion(getDoiValue(previousDataPackage)).equals(getDOIVersion(previousDOI))) {
                                    updateHasPartDataFile(context, previousDataPackage, previousDOI, oldDOIstring);
                                    updateIsPartDataFile(context, previousItem, previousDataPackage);
                                }
                            }
                        }
                    }
                } else {
                    previousDOI = getVersionedDataPackageDOIString(previousDOI,1);
                }
                updateItemDOIMetadata(previousItem, previousDOI);
                DOI firstDOI = new DOI(previousDOI, previousItem);
                if(DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(item)) {
                    mint(firstDOI, myBlackoutURL, true, createListMetadata(previousItem));
                } else {
                    mint(firstDOI, true, createListMetadata(previousItem));
                }
            }
            return getDOI(getDoiValue(item), item);
        } else {
            log.warn("Item " + doiString + " has no versions yet: start at " + versionNumber);
            VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
            versioningService.startNewVersionHistoryAt(context, item, "added file at version " + versionNumber, versionNumber);
            DOI versionedDOI = new DOI(getVersionedDataFileDOIString(doiString, versionNumber), item);
            return versionedDOI;
        }
    }

    private void addNewDOItoItem(String newDOIString, Item item, boolean register, String collection) throws IOException, SQLException
    {
        // make a new DOI that points to the item, then mint (and register)
        DOI newDOI = getDOI(newDOIString, item);
        mint(newDOI, register, createListMetadata(item));
    }

    private void mint(DOI doi, boolean register, Map<String, String> metadata) throws IOException {
        mint(doi, null, register, metadata);
    }

    private void mint(DOI doi, String target, boolean register, Map<String, String> metadata) throws IOException {
        perstMinter.mintDOI(doi);

        if(register) {
            perstMinter.register(doi, target, metadata);
        }

    }


    private Map<String, String> createListMetadata(Item item){
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.putAll(CDLDataCiteService.createMetadataList(item));
        return metadata;
    }



    /**
     * Returns the doi value in the metadata (if present, else null will be returned)
     *
     * @param item the item to check for a doi
     * @return the doi string
     */
    public static String getDoiValue(Item item) {
        DCValue[] doiVals = item.getMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, Item.ANY);
        if (doiVals != null && 0 < doiVals.length) {
            return doiVals[0].value;
        }
        return null;

    }

    /**
     * Returns a fully-formatted DOI URL
     *
     * @param item the item to check for a DOI
     * @return fully-formatted dx.doi.org url
     */

    public static String getFullDOIURL(Item item) {
        String doi_url = "";
        if (item != null) {
            String itemDOI = getDoiValue(item);
            if (itemDOI != null) {
                Matcher doimatcher = Pattern.compile("doi:(.+)").matcher(itemDOI);
                if (doimatcher.find()) {
                    doi_url = "http://dx.doi.org/" + doimatcher.group(1);
                } else {
                    doi_url = "http://dx.doi.org/" + itemDOI;
                }
            }
        }
        return doi_url;
    }

    public DSpaceObject resolve(Context context, String identifier, String... attributes) throws IdentifierNotFoundException, IdentifierNotResolvableException {
        // convert http DOIs to short form
        if (identifier.startsWith("http://dx.doi.org/")) {
            identifier = "doi:" + identifier.substring("http://dx.doi.org/".length());
        }
        // correct http DOIs to short form if a slash was removed by the browser/server
        if (identifier.startsWith("http:/dx.doi.org/")) {
            identifier = "doi:" + identifier.substring("http:/dx.doi.org/".length());
        }


        if (identifier != null && identifier.startsWith("doi:")) {
            DOI dbDOI = perstMinter.getKnownDOI(identifier);
            if(dbDOI==null) {
                throw new IdentifierNotFoundException("identifier " + identifier + " is not found");
            }
            String value = dbDOI.getInternalIdentifier();

            if (value != null) {
                // Ask Parent Service to retrieve internal reference to resource identified in the value.
                return parentService.resolve(context,value);
            }
        }
        return null;
    }

    public String lookup(String identifier) {
        String url=null;
        if (identifier != null && identifier.startsWith("doi:")) {
            DOI doi = perstMinter.getKnownDOI(identifier);
            if(doi!=null)
                url=doi.getTargetURL().toString();
        }
        return url;
    }


    public String lookupByURL(String url) {
        if (url != null) {
            Set<DOI> dois = perstMinter.getKnownDOIByURL(url);
            if (dois == null || dois.size() == 0)
                throw new RuntimeException("Unknown DOI for URL: " + url);

            String result = "";
            for (DOI d : dois)
            {
                result += d.toString() + " ";
            }

            return result;
        }
        return null;
    }

    public String lookupEzidRegistration(Item item) throws IOException {
        String aDOI = getDoiValue(item);
        return perstMinter.lookupDOIRegistration(aDOI);
    }

    public String getEzidRegistrationURL(Item item) {
        String aDoi = getDoiValue(item);
        return perstMinter.getRegistrationURL(aDoi);
    }

    public boolean remove(String identifier) {

        if (identifier != null && identifier.startsWith("doi:")) {
            DOI doi = perstMinter.getKnownDOI(identifier);
            return perstMinter.remove(doi);
        }
        return false;
    }

    /**
     * The field used for identification of DataPackage and DataFile in other areas of
     * codebase such as Workflow.
     *
     * @return
     */
    public static DCValue getIdentifierMetadata() {
        return identifierMetadata;
    }

    /**
     * The PerstMinter delivered from Spring.
     *
     * @param perstMinter
     */
    @Autowired
    @Required
    public void setPerstMinter(Minter perstMinter) {
        this.perstMinter = perstMinter;
    }


    // OLDER DryadDOIMinter Methods

    /**
     * Creates a DOI from the supplied DSpace URL string
     *
     * @param context
     * @param aDoi
     * @param item
     * @param vh
     * @return
     */
    private DOI calculateDOI(Context context, String aDoi, Item item, VersionHistory vh) {
        DOI doi = getDOI(aDoi, item);

        log.debug("calculateDOI() doi already exist? : " + (doi!=null));

        // If our DOI doesn't exist, then we need to mint one
        if (doi == null) {
            try {
                context.turnOffAuthorisationSystem();
                String collection = getCollection(context, item);

                // DATAPACKAGE
                if (collection.equals(myDataPkgColl)) {
                    doi = calculateDOIDataPackage(context, item, vh);
                }
                // DATAFILE
                else if (collection.equals(myDataFileColl)) {
                    doi = calculateDOIDataFile(context, item, vh);
                }

            } catch (ClassCastException details) {
                throw new RuntimeException(details);
            } catch (SQLException details) {
                if (context != null) {
                    context.abort();
                }
                throw new RuntimeException(details);
            } catch (Exception details) {
                throw new RuntimeException(details);
            }
        }
        return doi;
    }


    private DOI getDOI(String aDoi, Item item) {
        if (aDoi == null) return null;

        DOI doi = new DOI(aDoi, item);

        String dbDoiURL = lookup(doi.toString());
        String targetURL = doi.getTargetURL().toString();

        if (!targetURL.equals(dbDoiURL)) {
            return null;
        }

        return doi;
    }


    private synchronized DOI calculateDOIDataPackage(Context c, Item item, VersionHistory history) throws IOException, IdentifierException, AuthorizeException, SQLException {
        DOI doi = null;

        // Versioning: if it is a new version of an existing Item, the new DOI must be: oldDOI.(versionNumber), retrieve previous Item
        if (history != null) {
            Version version = history.getVersion(item);
            Version previous = history.getPrevious(version);
            String previousDOI = getDoiValue(previous.getItem());
            doi = new DOI(getVersionedDataPackageDOIString(previousDOI,version.getVersionNumber()), item);
        } else {
            String var = NoidGenerator.buildVar(mySuffixVarLength);
            doi = new DOI(myDoiPrefix, myLocalPartPrefix + var, item);

            if (doiAlreadyExists(doi.toString()))
                return calculateDOIDataPackage(c, item, history);
        }

        return doi;
    }


    // This method is used to check if a newly generated DOI String collides
    // with an existing DOI.  Since the DOIs are randomly-generated,
    // collisions are possible.
    private boolean doiAlreadyExists(String idDoi) {
        String dbDoiId = lookup(idDoi);

        if (dbDoiId != null && !dbDoiId.equals(""))
            return true;

        return false;
    }

    private DOI calculateDOIDataFile(Context context, Item item, VersionHistory history) throws IOException, IdentifierException, AuthorizeException, SQLException {
        DCValue[] pkgLink = item.getMetadata("dc.relation.ispartof");

        if (pkgLink == null) {
            throw new RuntimeException("Not linked to a data package");
        }

        if (pkgLink.length == 0) {
            throw new RuntimeException("Not linked to a data package");
        }

        String packageDOIString = pkgLink[0].value;
        if (!packageDOIString.startsWith("doi:")) {
            throw new DOIFormatException("isPartOf value doesn't start with 'doi:'");
        }

        DCValue[] titles = item.getMetadata("dc.title");

        if (titles.length > 0) {
            String itemTitle = titles[0].value;
            log.warn("calculateDOIDataFile() - " + itemTitle + " is part of: " + packageDOIString);
        }


        // Versioning: if it is a new version of an existing Item, the new DOI must be: oldDOI.(versionNumber)/filesuffix.(versionNumber)
        if (history != null) { // NEW VERSION OF AN EXISTING ITEM
            Version version = history.getVersion(item);
            Version previous = history.getPrevious(version);

            log.warn("calculateDOIDataFile() - new version of an existing - version: " + version.getVersionNumber());
            log.warn("calculateDOIDataFile() - new version of an existing - previous: " + previous.getVersionNumber());

            // we need to get the DOI of the previous versioned file so we can update it to the new version.
            String previousDOIString = getDoiValue(previous.getItem());
            // FIRST time a VERSION is created: we need to create the versioned DOIs.
            if (history.isFirstVersion(previous)) {
                previousDOIString = calculateDOIDataFileFirstTime(previous.getItem());
            }

            // mint NEW DOI: packageDOIString + fileIndex from previous DOI + new version number
            int fileSuffix = getDataFileSuffix(previousDOIString);
            int versionN = version.getVersionNumber();
            DOI childDOI = new DOI(getVersionedDataFileDOIString(packageDOIString, fileSuffix, versionN), item);
            log.warn("calculateDOIDataFile() - new version of an existing item: " + childDOI.toString());
            return childDOI;
        }
        else { // NEW ITEM: mint a new DOI with a new file suffix
            // has an arbitrary max; in reality much, much less
            for (int index = 1; index < MAX_NUM_OF_FILES; index++) {
                // check if canonical already exists
                String canonicalFileDOIString = getDataFileDOIString(getCanonicalDOIString(packageDOIString), index);
                if (doiAlreadyExists(canonicalFileDOIString)) {
                    String dbDoiURL = lookup(canonicalFileDOIString);
                    if (dbDoiURL.equals(DOI.getInternalForm(item))) {
                        log.warn("calculateDOIDataFile() - new item canonical exists: " + canonicalFileDOIString);
                        return new DOI(canonicalFileDOIString, item);
                    }
                }
                else {
                    log.warn("calculateDOIDataFile() - new item canonical not exists: " + canonicalFileDOIString);
                    // If versioning has already begun, we have to mint two DOIs: the current DOI, but also the canonical file DOI. Look for that first.
                    // mint the canonical file DOI:
                    DOI canonicalFileDOI = new DOI(canonicalFileDOIString, item);
                    mint(canonicalFileDOI,false,createListMetadata(item));

                    String packageVersion = getDOIVersion(packageDOIString);
                    if (packageVersion.equals("")) {
                        // no version
                        return new DOI(getDataFileDOIString(packageDOIString,index), item);
                    } else {
                        // versioned; file version needs to match package version
                        return mintDOIAtVersion(context, canonicalFileDOIString, item, Integer.valueOf(packageVersion));
                    }
                }
            }
        }
        return null;
    }

    /**
     * If a bitstream is added to or removed from the DataFile, we have to upgrade the version number
     * only if the item is already versioned and if it wasn't already upgraded.
     * @return
     */
    private DOI upgradeDOIDataFile(Context c, String idDoi, Item item, VersionHistory history) throws SQLException, AuthorizeException {
        DOI doi=null;
        if (history != null) { // only if it is already versioned
            Version version = history.getVersion(item);
            if(history.isLastVersion(version)){ // only if the user is modifying the last version
                Version previous = history.getPrevious(version);

                String idPrevious = getDoiValue(previous.getItem());
                int suffixIdPrevious = getDataFileSuffix(idPrevious);
                int suffixIdDoi = getDataFileSuffix(idDoi);

                if(suffixIdPrevious == suffixIdDoi){   // only if it is not upgraded
                    if(countBitstreams(previous.getItem())!= countBitstreams(item)){ // only if a bitstream was added or removed
                        int versionN = Integer.parseInt(getDOIVersion(idPrevious));
                        String newDoi = getVersionedDataFileDOIString(idDoi, suffixIdDoi,versionN+1);
                        updateHasPartDataPackage(c, item, newDoi, idDoi);
                    }
                }
            }
        }
        return doi;
    }


    private int countBitstreams(Item item) throws SQLException {
        int numberOfBitsream=0;
        for (Bundle b : item.getBundles())
        {
            for (Bitstream bit : b.getBitstreams())
            {
                numberOfBitsream++;
            }
        }
        return numberOfBitsream;
    }

    private String calculateDOIDataFileFirstTime(Item item) {
        String id = getDoiValue(item);
        if (id != null) {
            String packageDOIString = getDataPackageDOIString(id);
            int suffix = getDataFileSuffix(id);
            if (!isVersionedDOI(id)) { // file is not versioned, so version it.
                id = getVersionedDataFileDOIString(packageDOIString,suffix,1);
            }

            return id;
        }
        return "";
    }

    private void updateItemDOIMetadata(Item item, String doiString) throws AuthorizeException, SQLException {
        item.clearMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, Item.ANY);
        item.addMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null, doiString);
        item.update();
    }

    private void updateIsPartDataFile(Context c, Item dataFile, Item dataPackage) throws AuthorizeException, SQLException {
        //update current datafile's ispartof metadata
        String doi = getDoiValue(dataPackage);
        if(doi != null){
            dataFile.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema,"relation","ispartof",Item.ANY);
            dataFile.addMetadata(DOIIdentifierProvider.identifierMetadata.schema,"relation","ispartof",Item.ANY,doi);
            dataFile.update();
        }
    }

    private void updateHasPartDataFile(Context c, Item dataPackage, String idNew, String idOld) throws AuthorizeException, SQLException {
        DCValue[] doiVals = dataPackage.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", Item.ANY);

        dataPackage.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", null);

        for(DCValue value : doiVals){
            if(!value.value.equals(idOld))
                dataPackage.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", null, value.value);
        }
        dataPackage.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", null, idNew);
        dataPackage.update();
    }

    private void updateHasPartDataPackage(Context c, Item item, String idNew, String idOld) throws AuthorizeException, SQLException {
        //update current datafile's doi that is recorded in the datapackage
        Item dataPackage = org.dspace.workflow.DryadWorkflowUtils.getDataPackage(c, item);
        updateHasPartDataFile(c, dataPackage, idNew, idOld);
    }

    public String getVersionedDataFileDOIString(String doiString, int fileSuffix, int versionN) {
        return getCanonicalDOIString(getDataPackageDOIString(doiString)) + DOT + versionN + SLASH + fileSuffix + DOT + versionN;
    }

    public String getVersionedDataFileDOIString(String doiString, int versionN) {
        return getCanonicalDOIString(getDataPackageDOIString(doiString)) + DOT + versionN + SLASH + getDataFileSuffix(doiString) + DOT + versionN;
    }

    public String getVersionedDataPackageDOIString (String doiString, int versionN) {
        return getCanonicalDOIString(getDataPackageDOIString(doiString)) + DOT + versionN;
    }

    public String getDataFileDOIString (String doiString, int fileSuffix) {
        return getDataPackageDOIString(doiString) + SLASH + fileSuffix;
    }

    public static boolean isVersionedDOI (String doiString){
        // if a DOI has 2 or less dots, it is not a versioned DOI.
        // eg: doi:10.5061/dryad.xxxxx or doi:10.5061/dryad.xxxxx/4 (two dots)
        // instead of doi:10.5061/dryad.xxxxx.2 or doi:10.5061/dryad.xxxxx.2/4.2 (3 or 4 dots)
        short numDots=0;
        int indexDot = doiString.indexOf(DOT);
        while(indexDot != -1){
            indexDot = doiString.indexOf(DOT, indexDot+1);
            numDots++;
        }

        if (numDots <= 2) {
            return false;
        }
        return true;
    }

    public static boolean isDataPackageDOI (String doiString) {
        // if the last part of the DOI after the last slash contains the substring "dryad", it's a package.
        String suffix = doiString.substring(doiString.lastIndexOf(SLASH) + 1);
        return suffix.contains("dryad");
    }

    public static boolean isDataFileDOI (String doiString) {
        // if the last part of the DOI after the last slash is just numbers, it is a file.
        String suffix = doiString.substring(doiString.lastIndexOf(SLASH) + 1);
        return suffix.matches("\\d+\\.*\\d*");
    }

    public static String getCanonicalDOIString(String doiString) {
        // no version present
        if (!isVersionedDOI(doiString)) return doiString;

        // if it's a file, find the package DOI first.
        String packageDOIString = getDataPackageDOIString(doiString);
        int fileSuffix = getDataFileSuffix(doiString);
        String canonicalDP = packageDOIString.substring(0, packageDOIString.lastIndexOf(DOT));

        if (isDataFileDOI(doiString)) {
            return canonicalDP + SLASH + String.valueOf(fileSuffix);
        }
        if (isDataPackageDOI(doiString)) {
            return canonicalDP;
        }
        return "";
    }

    public static String getDataPackageDOIString(String doiString) {
        if (isDataFileDOI(doiString)) return doiString.substring(0,doiString.lastIndexOf(SLASH));
        return doiString;
    }

    // given a DOI (eg doi:10.5061/dryad.9054.1)
    // returns the version number of the package (eg 1)
    public static String getDOIVersion(String doiString) {
        // no version present
        if(!isVersionedDOI(doiString)) return "";
        String packageDOI = getDataPackageDOIString(doiString);
        return packageDOI.substring(packageDOI.lastIndexOf(DOT) + 1);
    }

    // given a file DOI (eg doi:10.5061/dryad.9054.1/3.1)
    // returns the file portion of the DOI (eg 3)
    public static int getDataFileSuffix(String doiString) {
        if (!isDataFileDOI(doiString)) return 0;
        String fileSuffix = doiString.substring(doiString.lastIndexOf(SLASH) + 1);
        if (fileSuffix.lastIndexOf(DOT) != -1) {
            fileSuffix = fileSuffix.substring(0,fileSuffix.lastIndexOf(DOT));
        }

        return Integer.parseInt(fileSuffix);
    }

    private String getCollection(Context context, Item item) throws SQLException {
        String collectionResult = null;

        if(item.getOwningCollection()!=null) {
            return item.getOwningCollection().getHandle();
        }

        // If our item is a workspaceitem it cannot have a collection, so we will need to get our collection from the workspace item
        return getCollectionFromWI(context, item.getID()).getHandle();
    }

    private Collection getCollectionFromWI(Context context, int itemId) throws SQLException {

        TableRow row = DatabaseManager.querySingleTable(context, "workspaceitem", "SELECT collection_id FROM workspaceitem WHERE item_id= ?", itemId);
        if (row != null) return Collection.find(context, row.getIntColumn("collection_id"));

        row = DatabaseManager.querySingleTable(context, "workflowitem", "SELECT collection_id FROM workflowitem WHERE item_id= ?", itemId);
        if (row != null) return Collection.find(context, row.getIntColumn("collection_id"));

        throw new RuntimeException("Collection not found for item: " + itemId);

    }

    /**
     * Breaks down the DSpace handle-like string (e.g.,
     * http://dev.datadryad.org/handle/12345/dryad.620) into a "12345/dryad.620"
     * part and a "dryad.620" part (in that order).
     *
     * @param aHDL
     * @return
     */
    public String[] stripHandle(String aHDL) {
        int start = aHDL.lastIndexOf(myHdlPrefix + SLASH) + 1
                + myHdlPrefix.length();
        String id;

        if (start > myHdlPrefix.length()) {
            id = aHDL.substring(start, aHDL.length());
            return new String[]{myHdlPrefix + SLASH + id, id};
        } else {
            return new String[]{myHdlPrefix + SLASH + aHDL, aHDL};
        }
    }

    private VersionHistory retrieveVersionHistory(Context c, Item item) {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        VersionHistory history = versioningService.findVersionHistory(c, item.getID());
        return history;
    }
}
