package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.doi.DOI;

import org.dspace.doi.DOIFormatException;
import org.dspace.doi.Minter;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.*;

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
    
    private String myHostname;
    
    private String myDataPkgColl;
    
    private String myDataFileColl;
    
    private String myLocalPartPrefix;
    
    private String myDoiPrefix;

    private int mySuffixVarLength;

    private final SecureRandom myRandom = new SecureRandom();
    
    Minter perstMinter = null;

    private String[] supportedPrefixes = new String[]{"info:doi/", "doi:" , "http://dx.doi.org/"};


    public void afterPropertiesSet() throws Exception {

        myHdlPrefix = configurationService.getProperty("handle.prefix");
        myHostname = configurationService.getProperty("dryad.url");
        myDataPkgColl = configurationService.getProperty("stats.datapkgs.coll");
        myDataFileColl = configurationService.getProperty("stats.datafiles.coll");
        myDoiPrefix = configurationService.getProperty("doi.prefix");
        myLocalPartPrefix = configurationService.getProperty("doi.localpart.suffix");

        try{
            mySuffixVarLength = Integer.parseInt(configurationService.getProperty("doi.suffix.length"));
        }catch (NumberFormatException nfe){
            mySuffixVarLength=5;
        }

        identifierMetadata.schema = MetadataSchema.DC_SCHEMA;
        identifierMetadata.element = "identifier";
        identifierMetadata.qualifier = null;
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
                ((Item) dso).clearMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null);
                ((Item) dso).addMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null, doi);
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
                String doi = mintAndRegister(context, (Item) dso, false);
                ((Item) dso).clearMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null);
                ((Item) dso).addMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null, doi);
            }
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to mint doi", "Item id: " + dso.getID()));
            throw new IdentifierException("Error while retrieving doi identifier", e);
        }
        return null;
    }


    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        try {
            if (dso instanceof Item) {
                Item item = (Item) dso;
                String doi = getDoiValue((Item) dso);

                // Remove from DOI service only if the item is not registered
                if(doi!=null){
                    if(!item.isArchived()){
                        remove(doi.toString());
                    }
                    // if it is already registered it has to remain in DOI service and when someone looks for it go towards a "tombstone" page
                    // reassign the URL of the DOI
                    else{
                        DOI removedDOI = new DOI(doi.toString(), DOI.Type.TOMBSTONE);
                        mint(removedDOI, true, null);
                    }
                }


                // If it is the most current version occurs to move the canonical to the previous version
                VersionHistory history = retrieveVersionHistory(context, item);
                if(history!=null && history.getLatestVersion().getItem().equals(item) && history.size() > 1){
                    Item previous = history.getPrevious(history.getLatestVersion()).getItem();
                    DOI doi_ = new DOI(doi, previous);


                    String collection = getCollection(context, previous);
                    String myDataPkgColl = configurationService.getProperty("stats.datapkgs.coll");
                    DOI canonical=null;

                    if (collection.equals(myDataPkgColl)) {
                        canonical = getCanonicalDataPackage(doi_, item);
                    } else {
                        canonical = getCanonicalDataFile(doi_, item);

                    }
                    mint(canonical, true, null);
                }
            }
        } catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to register doi", "Item id: " + dso.getID()));
            throw new IdentifierException("Error while moving doi identifier", e);
        }
    }


    private String mintAndRegister(Context context, Item item, boolean register) throws Exception {
        String doi = getDoiValue(item);
        String collection = getCollection(context, item);
        String myDataPkgColl = configurationService.getProperty("stats.datapkgs.coll");
        VersionHistory history = retrieveVersionHistory(context, item);


        // CASE A:  it is a versioned datafile and the user is modifying its content (adding or removing bitstream) upgrade version number.
        if(item.isArchived()){
            if(!collection.equals(myDataPkgColl)){
                if(lookup(doi)!=null){
                    DOI doi_= upgradeDOIDataFile(context, doi, item, history);
                    if(doi_!=null){
                        remove(doi);
                        mint(doi_, register, createListMetadata(item));

                        item.clearMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null);
                        item.update();
                        if (doi == null || doi.equals("")) throw new Exception();
                    }
                }
            }
        }

        // CASE B: New DataPackage or New version
        // FIRST time a VERSION is created 2 identifiers will be minted  and the canonical will be updated to point to the newer URL:
        //  - id.1-->old URL
        //  - id.2-->new URL
        //  - id(canonical)-- new URL
        // Next times 1 identifier will be minted  and the canonical will be updated to point to the newer URL
        //  - id.x-->new URL
        //  - id(canonical)-- new URL
        // If it is a new ITEM just 1 identifier will be minted

        else{
            // only if it is in workflow.
            // MINT Identifier || .
            DOI doi_ = calculateDOI(context, doi, item, history);

            log.warn("DOI just minted: " + doi_);

            doi = doi_.toString();
            mint(doi_, register, createListMetadata(item));

            if (history != null) {
                 Version version = history.getVersion(item);
                // if it is the first time that is called "create version": mint identifier ".1"
                Version previous = history.getPrevious(version);
                if (history.isFirstVersion(previous)) {
                    DOI firstDOI = calculateDOIFirstVersion(context, previous);
                    mint(firstDOI, register, createListMetadata(previous.getItem()));
                }

                // move the canonical
                DOI canonical = null;
                if (collection.equals(myDataPkgColl)) {
                    canonical = getCanonicalDataPackage(doi_, item);
                } else {
                    canonical = getCanonicalDataFile(doi_, item);

                }

                mint(canonical, register, createListMetadata(item));


            }
        }
        return doi;
    }


    private void mint(DOI doi, boolean register, Map<String, String> metadata) throws IOException {

        perstMinter.mintDOI(doi);

        if(register)
            perstMinter.register(doi, metadata);

    }


    private Map<String, String> createListMetadata(Item item){
        Map<String, String> metadata = new HashMap<String, String>();
        CDLDataCiteService.createMetadataList(item);
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

    public DSpaceObject resolve(Context context, String identifier, String... attributes) throws IdentifierNotFoundException, IdentifierNotResolvableException {
        //Check if we really have a doi identifier

        log.warn("DOIIdentofierService: start resolve() identifier ==>>  " + identifier);

        if (identifier != null && identifier.startsWith("doi:")) {

            DOI dbDOI = perstMinter.getKnownDOI(identifier);
            if(dbDOI==null)
                throw new IdentifierNotFoundException();

            String value = dbDOI.getInternalIdentifier();

            log.warn("DOIIdentofierService: resolve to (before replace) ==>>" + value);

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
            if (configurationService.getPropertyAsType("doi.service.testmode", false)) {
                url = identifier.replace("doi:", "");
            } else {
                DOI doi = perstMinter.getKnownDOI(identifier);
                if(doi!=null)
                    url=doi.getTargetURL().toString();
            }
        }
        return url;
    }


    public String lookupByURL(String url) {
        if (url != null) {
            Set<DOI> dois = perstMinter.getKnownDOIByURL(url);
            if (dois == null || dois.size() == 0)
                throw new RuntimeException("Unknown DOI for URL: " + url);

            String result = "";
            for (DOI d : dois) result += d.toString() + " ";

            return result;
        }
        return null;
    }

    public boolean remove(String identifier) {

        if (identifier != null && identifier.startsWith("doi:")) {
            if (!configurationService.getPropertyAsType("doi.service.testmode", false)) {
                DOI doi = perstMinter.getKnownDOI(identifier);
                return perstMinter.remove(doi);
            }
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
        URL itemURL;
        String url;
        DOI doi = null;

        doi = getDOI(aDoi, item);

        log.warn("calculateDOI() doi already exist? : " + (doi!=null));

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
                    doi = calculateDOIDataFile(item, vh);
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


    private DOI calculateDOIFirstVersion(Context c, Version previous) throws SQLException {
        DOI doi;
        String idDoi = DOIIdentifierProvider.getDoiValue(previous.getItem());
        doi = new DOI(idDoi, previous.getItem());
        return doi;
    }


    private DOI getDOI(String aDoi, Item item) {
        DOI doi = null;
        if (aDoi == null) return null;


        doi = new DOI(aDoi, item);
        if (!exists(doi)) return null;

        return doi;
    }


    private synchronized DOI calculateDOIDataPackage(Context c, Item item, VersionHistory history) throws IOException, IdentifierException, AuthorizeException, SQLException {
        DOI doi, oldDoi = null;

        // Versioning: if it is a new version of an existing Item, the new DOI must be: oldDOI.(versionNumber), retrieve previous Item
        if (history != null) {
            Version version = history.getVersion(item);
            Version previous = history.getPrevious(version);
            String previousDOI = DOIIdentifierProvider.getDoiValue(previous.getItem());

            // FIRST time a VERSION is created: update identifier of the previous item adding ".1"
            if (history.isFirstVersion(previous)) {
                previousDOI=updateIdentierPreviousItem(previous.getItem());
            }

            String canonical = previousDOI.substring(0, previousDOI.lastIndexOf(DOT));
            String versionNumber = "" + DOT + (version.getVersionNumber());
            doi = new DOI(canonical + versionNumber, item);
        } else {
            String var = buildVar();
            doi = new DOI(myDoiPrefix, myLocalPartPrefix + var, item);

            if (existsIdDOI(doi.toString()))
                return calculateDOIDataPackage(c, item, history);
        }

        return doi;
    }


    private boolean exists(DOI doi) {
        String dbDoiURL = lookup(doi.toString());

        if (doi.getTargetURL().toString().equals(dbDoiURL))
            return true;

        return false;
    }

    private DOI calculateDOIDataFile(Item item, VersionHistory history) throws IOException, IdentifierException, AuthorizeException, SQLException {
        String doiString;
        DCValue[] pkgLink = item.getMetadata("dc.relation.ispartof");

        if (pkgLink == null) {
            throw new RuntimeException("Not linked to a data package");
        }
        if (!(doiString = pkgLink[0].value).startsWith("doi:")) {
            throw new DOIFormatException("isPartOf value doesn't start with 'doi:'");
        }

        log.warn("calculateDOIDataFile() - is part of: " + doiString);

        // Versioning: if it is a new version of an existing Item, the new DOI must be: oldDOI.(versionNumber)
        if (history != null) { // NEW VERSION OF AN EXISTING ITEM
            Version version = history.getVersion(item);
            Version previous = history.getPrevious(version);

            log.warn("calculateDOIDataFile() - new version of an existing - version: " + version.getVersionNumber());
            log.warn("calculateDOIDataFile() - new version of an existing - previous: " + previous.getVersionNumber());

            String idPrevious=null;
            // FIRST time a VERSION is created: update identifier of the previous item adding ".1" before /
            if (history.isFirstVersion(previous)) {
                log.warn("calculateDOIDataFile() - updateIdentierPreviousDF()");
                idPrevious=updateIdentierPreviousDF(previous.getItem());
            }
            else
                idPrevious = DOIIdentifierProvider.getDoiValue(previous.getItem());

            // mint NEW DOI: taking first part from id dataPackage father (until the /) + taking last part from id previous dataFile (after the slash)  e.g., 1111.3 / 1.1
            log.warn("calculateDOIDataFile() - new version of an existing - idPrevious: " + idPrevious);

            String suffixDF = idPrevious.substring(idPrevious.lastIndexOf(SLASH) + 1);

            log.warn("calculateDOIDataFile() - new version of an existing - suffixDF: " + suffixDF);


            // the item has been modified? if yes: increment version number
            DOI childDOI=null;
            if(countBitstreams(previous.getItem())!= countBitstreams(item)){

                log.warn("calculateDOIDataFile() - new version of an existing - dataFile modified");

                int versionN = Integer.parseInt(suffixDF.substring(suffixDF.lastIndexOf(DOT)+1));

                log.warn("calculateDOIDataFile() - new version of an existing - dataFile modified -  doiString" + doiString);
                log.warn("calculateDOIDataFile() - new version of an existing - dataFile modified -  suffixDF" + suffixDF);
                log.warn("calculateDOIDataFile() - new version of an existing - dataFile modified -  versionN" + versionN);

                childDOI = new DOI(doiString + "/" + suffixDF.substring(0, suffixDF.lastIndexOf(DOT)) + DOT  + (versionN+1), item);
            }
            else{
                log.warn("calculateDOIDataFile() - new version of an existing - dataFile not modified -  doiString" + doiString);
                log.warn("calculateDOIDataFile() - new version of an existing - dataFile not modified -  suffixDF" + suffixDF);
                childDOI = new DOI(doiString + "/" + suffixDF, item);
            }
            log.warn("calculateDOIDataFile() - new version of an existing: " + childDOI);
            return childDOI;
        }
        else { // NEW ITEM: mint a new DOI
            // has an arbitrary max; in reality much, much less
            for (int index = 1; index < MAX_NUM_OF_FILES; index++) {

                // check if canonical already exists
                String idDOI = getCanonicalDataPackage(doiString) + "/" + index;
                if (existsIdDOI(idDOI)) {
                    String dbDoiURL = lookup(idDOI);
                    if (dbDoiURL.equals(DOI.getInternalForm(item))) {
                        log.warn("calculateDOIDataFile() - new item canonical exists: " + (doiString + "/" + index));
                        return new DOI(doiString + "/" + index, item);
                    }
                }
                else {
                    log.warn("calculateDOIDataFile() - new item canonical not exists: " + (doiString + "/" + index));
                    DOI childDOI = new DOI(doiString + "/" + index, item);
                    return childDOI;
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

                String idPrevious = DOIIdentifierProvider.getDoiValue(previous.getItem());
                String suffixIdPrevious=idPrevious.substring(idPrevious.lastIndexOf(SLASH)+1);
                String suffixIdDoi=idDoi.substring(idDoi.lastIndexOf(SLASH)+1);


                if(suffixIdPrevious.equals(suffixIdDoi)){   // only if it is not upgraded
                    if(countBitstreams(previous.getItem())!= countBitstreams(item)){ // only if a bitstream was added or removed
                        int versionN = Integer.parseInt(suffixIdPrevious.substring(suffixIdPrevious.lastIndexOf(DOT)+1));

                        String prefix=idDoi.substring(0, idDoi.lastIndexOf(DOT));
                        String newDoi=prefix + DOT + (versionN+1);
                        doi = new DOI(newDoi, item);
                        updateHasPartDataPackage(c, item, doi.toString(), idDoi);
                    }
                }
            }
        }
        return doi;
    }


    private int countBitstreams(Item item) throws SQLException {
        int numberOfBitsream=0;
        for(Bundle b : item.getBundles())
            for(Bitstream bit : b.getBitstreams())
                numberOfBitsream++;
        return numberOfBitsream;
    }

    private String updateIdentierPreviousItem(Item item) throws AuthorizeException, SQLException {
        DCValue[] doiVals = item.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        String id = doiVals[0].value;
        item.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        id += DOT + "1";
        item.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, null, id);
        item.update();
        return id;
    }

    private String updateIdentierPreviousDF(Item item) throws AuthorizeException, SQLException {
        DCValue[] doiVals = item.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        log.warn("updateIdentierPreviousDF() - doiVals.length : " + doiVals.length);

        String id = doiVals[0].value;

        log.warn("updateIdentierPreviousDF() - id : " + id);

        item.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        String prefix = id.substring(0, id.lastIndexOf(SLASH));
        String suffix = id.substring(id.lastIndexOf(SLASH));

        log.warn("updateIdentierPreviousDF() - prefix;suffix : " + prefix + ";" + suffix);

        id = prefix + DOT + "1" + suffix + DOT + "1";


        log.warn("updateIdentierPreviousDF() - id before db update : " + id);

        item.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, null, id);
        item.update();
        return id;
    }


    private void updateHasPartDataPackage(Context c, Item item, String idNew, String idOld) throws AuthorizeException, SQLException {
        Item dataPackage =org.dspace.workflow.DryadWorkflowUtils.getDataPackage(c, item);
        DCValue[] doiVals = dataPackage.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", Item.ANY);


        dataPackage.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", null);

        for(DCValue value : doiVals){
            if(!value.value.equals(idOld))
                dataPackage.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", null, value.value);
        }
        dataPackage.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, "relation", "haspart", null, idNew);
        dataPackage.update();
    }



    private boolean existsIdDOI(String idDoi) {
        String dbDoiId = lookup(idDoi.toString());

        if (dbDoiId != null && !dbDoiId.equals(""))
            return true;

        return false;
    }


    private DOI getCanonicalDataPackage(DOI doi, Item item) {
        String canonicalID = doi.toString().substring(0, doi.toString().lastIndexOf(DOT));
        DOI canonical = new DOI(canonicalID, item);
        return canonical;
    }

    private String getCanonicalDataPackage(String doi) {
        // no version present
        if(countDots(doi) <=2) return doi;
        String canonicalID = doi.toString().substring(0, doi.toString().lastIndexOf(DOT));
        return canonicalID;
    }

    private short countDots(String doi){
        short index=0;
        int indexDot=0;
        while( (indexDot=doi.indexOf(DOT))!=-1){
            doi=doi.substring(indexDot+1);
            index++;
        }

        return index;
    }


    /**
     * input doi.toString()=   doi:10.5061/dryad.9054.1/1.1
     * output doi.toString()=  2rdfer334/1
     */
    private DOI getCanonicalDataFile(DOI doi, Item item) {

        log.warn("getCanonicalDataFile() doi in input: " + doi);


        // doi:10.5061/dryad.9054.1 (based on the input example)
        String idDP = doi.toString().substring(0, doi.toString().lastIndexOf(SLASH));

        // idDF=1.1
        String idDF = doi.toString().substring(doi.toString().lastIndexOf(SLASH) + 1);


        String canonicalDP = idDP.substring(0, idDP.lastIndexOf(DOT));
        String canonicalDF = idDF.substring(0, idDF.lastIndexOf(DOT));


        DOI canonical = new DOI(canonicalDP + SLASH + canonicalDF, item);
        return canonical;
    }


    private String getCollection(Context context, Item item) throws SQLException {
        String collectionResult = null;

        if(item.getOwningCollection()!=null)
            return item.getOwningCollection().getHandle();

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

    private URL getTarget(String aDSpaceURL) {
        URL target;
        try {
            target = new URL(aDSpaceURL);
        }
        catch (MalformedURLException details) {
            try {
                log.debug("Using " + myHostname + " for URL domain name");
                // If we aren't given a full URL, create one with config value
                if (aDSpaceURL.startsWith("/")) {
                    target = new URL(myHostname + aDSpaceURL);
                } else {
                    target = new URL(myHostname + "/handle/" + aDSpaceURL);
                }
            }
            catch (MalformedURLException moreDetails) {
                throw new RuntimeException("Passed URL isn't a valid URL: " + aDSpaceURL);
            }
        }
        return target;
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
        int start = aHDL.lastIndexOf(myHdlPrefix + "/") + 1
                + myHdlPrefix.length();
        String id;

        if (start > myHdlPrefix.length()) {
            id = aHDL.substring(start, aHDL.length());
            return new String[]{myHdlPrefix + "/" + id, id};
        } else {
            return new String[]{myHdlPrefix + "/" + aHDL, aHDL};
        }
    }


    private String buildVar() {
        String bigInt = new BigInteger(mySuffixVarLength * 5, myRandom).toString(32);
        StringBuilder buffer = new StringBuilder(bigInt);
        int charCount = 0;

        while (buffer.length() < mySuffixVarLength) {
            buffer.append('0');
        }

        for (int index = 0; index < buffer.length(); index++) {
            char character = buffer.charAt(index);
            int random;

            if (character == 'a' | character == 'l' | character == 'e'
                    | character == 'i' | character == 'o' | character == 'u') {
                random = myRandom.nextInt(9);
                buffer.setCharAt(index, String.valueOf(random).charAt(0));
                charCount = 0;
            } else if (Character.isLetter(character)) {
                charCount += 1;

                if (charCount > 2) {
                    random = myRandom.nextInt(9);
                    buffer.setCharAt(index, String.valueOf(random).charAt(0));
                    charCount = 0;
                }
            }
        }

        return buffer.toString();
    }


    private VersionHistory retrieveVersionHistory(Context c, Item item) {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        VersionHistory history = versioningService.findVersionHistory(c, item.getID());
        return history;
    }

    private Version getVersion(Context c, Item item) {
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        return versioningService.getVersion(c, item);
    }



}
