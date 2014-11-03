package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.app.util.NoidGenerator;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
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
import org.dspace.versioning.DryadPublicationDataUtil;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.DryadWorkflowUtils;
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
                ((Item) dso).clearMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, Item.ANY);
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
                ((Item) dso).clearMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, Item.ANY);
                ((Item) dso).addMetadata(identifierMetadata.schema, identifierMetadata.element, identifierMetadata.qualifier, null, doi);
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
            String doi = getDoiValue((Item) dso);
            DOI doi_ = new DOI(doi, item);
            String collection = getCollection(context, item);
            moveCanonical(item, true, collection, myDataPkgColl, doi_);

            // if 1st version mint .1
            mintDOIFirstVersion(context, item, true);

        }catch (Exception e) {
            log.error(LogManager.getHeader(context, "Error while attempting to moveCanonical doi", "Item id: " + dso.getID()));
            throw new IdentifierException("Error while moving doi identifier", e);
        }
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
                    moveCanonical(previous, true, collection, myDataPkgColl, doi_);
                }

                //  IF Deleting a 1st version not archived yet:
                //  The DOI stored in the previous  should revert to the version without ".1".
                // Canonical DOI already point to the right item: no needs to move it
                if(history!=null && history.size() == 2 && !item.isArchived()){
                    revertDoisFirstItem(context, history);
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

        // CASE A: it is a versioned datafile and the user is modifying its content (adding or removing bitstream) upgrade version number.
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
                mint(doi_, "http://datadryad.org/publicationBlackout", register, createListMetadata(item));
            } else {
                mint(doi_, register, createListMetadata(item));
            }

        }
        return doi;
    }

    private void revertDoisFirstItem(Context context, VersionHistory history) throws SQLException, IOException, AuthorizeException{
        Item previous = history.getPrevious(history.getLatestVersion()).getItem();
        String collection = getCollection(context, previous);
        // remove doi from DOI service .1
        String doiPrevious = getDoiValue(previous);
        DOI removedDOI = new DOI(doiPrevious.toString(), DOI.Type.TOMBSTONE);
        mint(removedDOI, true, null);


        if (collection.equals(myDataPkgColl)) {
            // replace doi metadata: dryad.2335.1 with  dryad.2335
            revertIdentierItem(previous);
        } else {
            // replace doi metadata: dryad.2335.1/1.1 with  dryad.2335/1
            revertIdentifierDF(previous);

        }
    }


    private void mintDOIFirstVersion(Context context, Item item, boolean register) throws SQLException, IOException, AuthorizeException
    {
        VersionHistory history = retrieveVersionHistory(context, item);

        if (history != null) {
            Version version = history.getVersion(item);
            // if it is the first time that is called "create version": mint identifier ".1"
            Version previous = history.getPrevious(version);
            if (history.isFirstVersion(previous)) {
                String previousDOI= updateIdentifierPreviousItem(context,previous.getItem());
                DOI firstDOI = new DOI(previousDOI, previous.getItem());
                if(DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(item)) {
                    mint(firstDOI, "http://datadryad.org/publicationBlackout", register, createListMetadata(previous.getItem()));
                } else {
                    mint(firstDOI, register, createListMetadata(previous.getItem()));
                }
            }
        }
    }


    private void moveCanonical(Item item, boolean register, String collection, String myDataPkgColl, DOI doi_) throws IOException, SQLException
    {
        // move the canonical
        DOI canonical = null;
        if (collection.equals(myDataPkgColl)) {
            canonical = getCanonicalDataPackage(doi_, item);
        } else {
            canonical = getCanonicalDataFile(doi_, item);

        }

        mint(canonical, register, createListMetadata(item));
    }

    private void mint(DOI doi, boolean register, Map<String, String> metadata) throws IOException {
        mint(doi, null, register, metadata);
    }
    
    private void mint(DOI doi, String target, boolean register, Map<String, String> metadata) throws IOException {
        log.debug("mintDOI is going to be called on "+doi.toString());
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
                throw new IdentifierNotFoundException();
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
        return  perstMinter.getRegistrationURL(aDoi);
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
        URL itemURL;
        String url;
        DOI doi = null;

        doi = getDOI(aDoi, item);

        log.debug("calculateDOI() doi already exist? : " + (doi!=null));

        // If our DOI doesn't exist, then we need to mint one
        if (doi == null) {
            try {
                context.turnOffAuthorisationSystem();
                String collection = getCollection(context, item);
                log.debug("collection is " + collection);

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
            String canonical = DOIIdentifierProvider.getDoiValue(previous.getItem());
            //process the canonical id to remove the version number
            canonical = getCanonicalDataPackage(canonical);
            String versionNumber = "" + DOT + (version.getVersionNumber());
            doi = new DOI(canonical+ versionNumber, item);
        } else {
            String var = NoidGenerator.buildVar(mySuffixVarLength);
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

        String itemTitle = "";
        if (titles.length > 0) {
            itemTitle = titles[0].value;
        }

        log.warn("calculateDOIDataFile() - " + itemTitle + " is part of: " + packageDOIString);

        // Versioning: if it is a new version of an existing Item, the new DOI must be: oldDOI.(versionNumber)
        if (history != null) { // NEW VERSION OF AN EXISTING ITEM
            Version version = history.getVersion(item);
            Version previous = history.getPrevious(version);

            log.warn("calculateDOIDataFile() - new version of an existing - version: " + version.getVersionNumber());
            log.warn("calculateDOIDataFile() - new version of an existing - previous: " + previous.getVersionNumber());

            // we need to get the DOI of the previous versioned file so we can update it to the new version.
            String previousDOIString = null;
            // FIRST time a VERSION is created: we need to create the versioned DOIs.
            if (history.isFirstVersion(previous)) {
                log.warn("calculateDOIDataFile() - calculateDOIDataFileFirstTime()");
                previousDOIString = calculateDOIDataFileFirstTime(previous.getItem());
            }
            else {
                previousDOIString = getDoiValue(previous.getItem());
            }

            // mint NEW DOI: packageDOIString + fileIndex from previous DOI + new version number
            String fileSuffix = getDataFileSuffix(previousDOIString);
            String fileIndex = fileSuffix.substring(0,fileSuffix.indexOf(DOT));
            String versionN = String.valueOf(version.getVersionNumber());
            DOI childDOI = new DOI(packageDOIString + SLASH + fileIndex + DOT + versionN, item);
            log.warn("calculateDOIDataFile() - new version of an existing item: " + childDOI.toString());
            return childDOI;
        }
        else { // NEW ITEM: mint a new DOI
            // has an arbitrary max; in reality much, much less
            for (int index = 1; index < MAX_NUM_OF_FILES; index++) {
                // check if canonical already exists
                String canonicalFileDOIString = getCanonicalDataPackage(packageDOIString) + SLASH + index;
                if (existsIdDOI(canonicalFileDOIString)) {
                    String dbDoiURL = lookup(canonicalFileDOIString);
                    if (dbDoiURL.equals(DOI.getInternalForm(item))) {
                        log.warn("calculateDOIDataFile() - new item canonical exists: " + canonicalFileDOIString);
                        return new DOI(packageDOIString + SLASH + index, item);
                    }
                }
                else {
                    log.warn("calculateDOIDataFile() - new item canonical not exists: " + canonicalFileDOIString);
                    // If versioning has already begun, we have to mint two DOIs: the current DOI, but also the canonical file DOI. Look for that first.
                    // mint the canonical file DOI:
                    DOI canonicalFileDOI = new DOI(canonicalFileDOIString, item);
                    mint(canonicalFileDOI,false,createListMetadata(item));

                    String packageVersion = getDataPackageVersion(packageDOIString);
                    if (packageVersion.equals("")) {
                        // no version
                        return new DOI(packageDOIString + SLASH + index, item);
                    } else {
                        // versioned; file version needs to match package version
                        return new DOI(packageDOIString + SLASH + index + DOT + packageVersion, item);
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
        for (Bundle b : item.getBundles())
        {
            for (Bitstream bit : b.getBitstreams())
            {
                numberOfBitsream++;
            }
        }
        return numberOfBitsream;
    }

    private String updateIdentifierPreviousItem(Context context,Item item) throws AuthorizeException, SQLException {
        DCValue[] doiVals = item.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        String id = doiVals[0].value;
        String collection = getCollection(context, item);
        if (collection.equals(myDataFileColl)) {
            id=updateIdentifierPreviousDF(context,item);

        } else {
            item.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);
            id += DOT + "1";
            item.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, null, id);
            item.update();
        }

        return id;
    }
    private String calculateDOIDataFileFirstTime(Item item)
    {
        DCValue[] doiVals = item.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        String id = doiVals[0].value;

        String prefix = id.substring(0, id.lastIndexOf(SLASH));
        String suffix = id.substring(id.lastIndexOf(SLASH));
        if(!suffix.endsWith(".1")){
            id = prefix + DOT + "1" + suffix + DOT + "1";
        }
        else
        {
            if(!prefix.endsWith(".1")){
                id = prefix + DOT + "1" + suffix;
            }
        }
        return id;
    }
    private String updateIdentifierPreviousDF(Context context,Item item) throws AuthorizeException, SQLException {
        String id = calculateDOIDataFileFirstTime(item);
        DCValue[] oldDOI=  item.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        Item dataPackage = DryadWorkflowUtils.getDataPackage(context,item);
        item.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);
        if(dataPackage!=null)
        {
            VersionHistory history = retrieveVersionHistory(context, dataPackage);

            if (history != null) {
                Version version = history.getVersion(dataPackage);
                // if it is the first time that is called "create version": mint identifier ".1"
                Version previous = history.getPrevious(version);
                if(previous!=null&&oldDOI!=null&&oldDOI.length>0)
                {
                    Item previousDataPackage = previous.getItem();

                    updateHasPartDataFile(context,previousDataPackage,id,oldDOI[0].value);
                    updateIsPartDataFile(context,item,previousDataPackage);
                }
            }
        }
        item.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, null, id);
        item.update();
        return id;

    }


    private String revertIdentierItem(Item item) throws AuthorizeException, SQLException {
        DCValue[] doiVals = item.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        String id = doiVals[0].value;

        item.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        id = id.substring(0, id.lastIndexOf(DOT));

        item.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, null, id);
        item.update();
        return id;
    }

    private String revertIdentifierDF(Item item) throws AuthorizeException, SQLException {
        DCValue[] doiVals = item.getMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        String id = doiVals[0].value;

        item.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, Item.ANY);

        String prefix = id.substring(0, id.lastIndexOf(SLASH));
        String suffix = id.substring(id.lastIndexOf(SLASH));

        prefix = prefix.substring(0, prefix.lastIndexOf(DOT));
        suffix = suffix.substring(0, suffix.lastIndexOf(DOT));

        id = prefix + suffix;

        item.addMetadata(DOIIdentifierProvider.identifierMetadata.schema, DOIIdentifierProvider.identifierMetadata.element, DOIIdentifierProvider.identifierMetadata.qualifier, null, id);
        item.update();
        return id;
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

    private void updateIsPartDataFile(Context c, Item dataFile, Item previousDataPackage) throws AuthorizeException, SQLException {
        //update current datafiles ispartof metadata
        DCValue[] ids = previousDataPackage.getMetadata(DOIIdentifierProvider.identifierMetadata.schema,DOIIdentifierProvider.identifierMetadata.element,DOIIdentifierProvider.identifierMetadata.qualifier,Item.ANY);

        if(ids!=null&&ids.length>0){
            dataFile.clearMetadata(DOIIdentifierProvider.identifierMetadata.schema,"relation","ispartof",Item.ANY);

           dataFile.addMetadata(DOIIdentifierProvider.identifierMetadata.schema,"relation","ispartof",Item.ANY,ids[0].value);
           dataFile.update();
        }
    }

    private boolean existsIdDOI(String idDoi) {
        // This method is used to check if a newly generated DOI String collides
        // with an existing DOI.  Since the DOIs are randomly-generated,
        // collisions are possible.

        String dbDoiId = lookup(idDoi.toString());

        if (dbDoiId != null && !dbDoiId.equals(""))
            return true;

        return false;
    }


    private DOI getCanonicalDataPackage(DOI doi, Item item) {
        String canonicalID = getCanonicalDataPackage(doi.toString());
        return new DOI(canonicalID, item);
    }

    private String getCanonicalDataPackage(String doi) {
        // no version present
        if(!isVersionedDOI(doi)) return doi;
        return doi.toString().substring(0, doi.toString().lastIndexOf(DOT));
    }

    // given a package DOI (eg doi:10.5061/dryad.9054.1)
    // returns the version number of the package (eg 1)
    private String getDataPackageVersion(String doi) {
        // no version present
        if(!isVersionedDOI(doi)) return "";
        return doi.toString().substring(doi.toString().lastIndexOf(DOT) + 1);
    }

    // given a file DOI (eg doi:10.5061/dryad.9054.1/3.1)
    // returns the file portion of the DOI (eg 3.1)
    private String getDataFileSuffix(String doi) {
        // TODO: test to make sure this is a file DOI.
        return doi.toString().substring(doi.toString().lastIndexOf(SLASH) + 1);
    }

    private boolean isVersionedDOI(String doi){
        // if a DOI has 2 or less dots, it is not a versioned DOI.
        // eg: doi:10.5061/dryad.xxxxx or doi:10.5061/dryad.xxxxx/4 (two dots)
        // instead of doi:10.5061/dryad.xxxxx.2 or doi:10.5061/dryad.xxxxx.2/4.2 (3 or 4 dots)
        short numDots=0;
        int indexDot = doi.indexOf(DOT);
        while(indexDot != -1){
            indexDot = doi.indexOf(DOT, indexDot+1);
            numDots++;
        }

        if (numDots <= 2) {
            return false;
        }
        return true;
    }


    /**
     * input doi.toString()=   doi:10.5061/dryad.9054.1/1.1
     * output doi.toString()=  2rdfer334/1
     */
    private DOI getCanonicalDataFile(DOI doi, Item item) {
        // doi:10.5061/dryad.9054.1 (based on the input example)
        String idDP = doi.toString().substring(0, doi.toString().lastIndexOf(SLASH));

        // idDF=1.1
        String idDF = doi.toString().substring(doi.toString().lastIndexOf(SLASH) + 1);

        String canonicalDP = idDP.substring(0, idDP.lastIndexOf(DOT));
        String canonicalDF = idDF;
        if(idDF.lastIndexOf(DOT)!=-1){
            canonicalDF=idDF.substring(0, idDF.lastIndexOf(DOT));
        }
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
