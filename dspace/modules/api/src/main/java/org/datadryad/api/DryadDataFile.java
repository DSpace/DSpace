/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DryadDataFile extends DryadObject {
    private static final String FILES_COLLECTION_HANDLE_KEY = "stats.datafiles.coll";

    // This is configured in dspace.cfg but replicated here.
    private static final String EMBARGO_TYPE_SCHEMA = "dc";
    private static final String EMBARGO_TYPE_ELEMENT = "type";
    private static final String EMBARGO_TYPE_QUALIFIER = "embargo";

    private static final String EMBARGO_DATE_SCHEMA = "dc";
    private static final String EMBARGO_DATE_ELEMENT = "date";
    private static final String EMBARGO_DATE_QUALIFIER = "embargoedUntil";

    private DryadDataPackage dataPackage;
    private static Logger log = Logger.getLogger(DryadDataFile.class);

    public DryadDataFile(Item item) {
        super(item);
    }

    public static Collection getCollection(Context context) throws SQLException {
        String handle = ConfigurationManager.getProperty(FILES_COLLECTION_HANDLE_KEY);
        return DryadObject.collectionFromHandle(context, handle);
    }

    public static DryadDataFile create(Context context, DryadDataPackage dataPackage) throws SQLException {
        Collection collection = DryadDataFile.getCollection(context);
        DryadDataFile dataFile = null;
        try {
            WorkspaceItem wsi = WorkspaceItem.create(context, collection, true);
            Item item = wsi.getItem();
            dataFile = new DryadDataFile(item);
            dataFile.setIsPartOf(dataPackage);
            dataFile.createIdentifier(context);
            dataPackage.setHasPart(dataFile);
            dataFile.addToCollectionAndArchive(collection);
            wsi.deleteWrapper();
        } catch (IdentifierException ex) {
            log.error("Identifier exception creating a Data File", ex);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception creating a Data File", ex);
        } catch (IOException ex) {
            log.error("IO exception creating a Data File", ex);
        }
        return dataFile;
    }

    static DryadDataPackage getDataPackageContainingFile(Context context, DryadDataFile dataFile) throws SQLException {
        String fileIdentifier = dataFile.getIdentifier();
        DryadDataPackage dataPackage = null;
        if(fileIdentifier == null || fileIdentifier.length() == 0) {
            return dataPackage;
        }
        try {
            ItemIterator dataPackages = Item.findByMetadataField(context, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_HASPART_QUALIFIER, fileIdentifier);
            if(dataPackages.hasNext()) {
                dataPackage = new DryadDataPackage(dataPackages.next());
            }
        } catch (AuthorizeException ex) {
            log.error("Authorize exception getting files for data package", ex);
        } catch (IOException ex) {
            log.error("IO exception getting files for data package", ex);
        }
        return dataPackage;
    }

    public DryadDataPackage getDataPackage(Context context) throws SQLException {
        if(dataPackage == null) {
            // Find the data package for this file
            dataPackage = DryadDataFile.getDataPackageContainingFile(context, this);
        }
        return dataPackage;
    }

    private void clearIsPartOf() throws SQLException {
        getItem().clearMetadata(RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISPARTOF_QUALIFIER, Item.ANY);
    }

    private void setIsPartOf(DryadDataPackage aDataPackage) throws SQLException {
        String dataPackageIdentifier = aDataPackage.getIdentifier();
        if(dataPackageIdentifier == null) {
            throw new IllegalArgumentException("Attempted to assign a file to a package with no identifier");
        }
        // Files may only belong to one package, so clear any existing metadata for ispartof
        addSingleMetadataValue(Boolean.TRUE, RELATION_SCHEMA, RELATION_ELEMENT, RELATION_ISPARTOF_QUALIFIER, dataPackageIdentifier);
        if(this.dataPackage != null) {
            this.dataPackage.clearDataFilesCache();
        }
        this.dataPackage = aDataPackage;
        aDataPackage.clearDataFilesCache();
    }

    /**
     * Assigns a data file to a data package, updating the dc.relation metadata.
     * Enforces the invariant that a data package may contain many files, but a
     * file may only belong to one package. Requires that both have a valid identifier.
     * @param context database context
     * @param dataPackage the package to which this file should belong
     */
    void setDataPackage(Context context, DryadDataPackage dataPackage) throws SQLException {
        if(dataPackage == null) {
            throw new IllegalArgumentException("Cannot set a null dataPackage");
        }
        String dataFileIdentifier = getIdentifier();
        if(dataFileIdentifier == null) {
            throw new IllegalArgumentException("Data file has no identifier");
        }
        // Ensure 0 packages contain the file, then the 1 specified
        Set<DryadDataPackage> packagesContainingFile = DryadDataPackage.getPackagesContainingFile(context, this);
        if(packagesContainingFile.size() > 0) {
            // file is not contained by any other data packages
            // remove file from packages
            for(DryadDataPackage containingPackage : packagesContainingFile) {
                containingPackage.removeDataFile(context, this);
            }
        }
        setIsPartOf(dataPackage);
        dataPackage.setHasPart(this);
    }

    /**
     * Remove the ispartof metadata from this file.
     * @throws SQLException
     */
    void clearDataPackage() throws SQLException {
        dataPackage.clearDataFilesCache();
        clearIsPartOf();
        try {
            getItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception clearing file ispartof", ex);
        }
    }

    public boolean isEmbargoed() {
        boolean isEmbargoed = false;
        DCValue[] embargoLiftDateMetadata = getItem().getMetadata(EMBARGO_DATE_SCHEMA, EMBARGO_DATE_ELEMENT, EMBARGO_DATE_QUALIFIER, Item.ANY);
        if(embargoLiftDateMetadata.length > 0) {
            // has a lift date, compare to today
            Date today = new Date();
            Date embargoLiftDate = getEarliestDate(embargoLiftDateMetadata);
            // Embargoed if there is a lift date and it is in the future
            if(embargoLiftDate != null && (embargoLiftDate.compareTo(today) > 0)) {
                isEmbargoed = true;
            }
        }
        return isEmbargoed;
    }

    public void clearEmbargo() throws SQLException {
        addSingleMetadataValue(Boolean.TRUE, EMBARGO_TYPE_SCHEMA, EMBARGO_TYPE_ELEMENT, EMBARGO_TYPE_QUALIFIER, Item.ANY, null);
        addSingleMetadataValue(Boolean.TRUE, EMBARGO_DATE_SCHEMA, EMBARGO_DATE_ELEMENT, EMBARGO_DATE_QUALIFIER, Item.ANY, null);
    }

    public void setEmbargo(String embargoType, Date liftDate) throws SQLException {
        if(!DryadEmbargoTypes.validate(embargoType)) {
            throw new IllegalArgumentException("EmbargoType '"
                    + embargoType + "' is not valid");
        }
        if(liftDate == null) {
            throw new IllegalArgumentException("Unable to set embargo date with null liftDate");
        } else {
            String liftDateString = formatDate(liftDate);
            addSingleMetadataValue(Boolean.TRUE, EMBARGO_TYPE_SCHEMA, EMBARGO_TYPE_ELEMENT, EMBARGO_TYPE_QUALIFIER, embargoType);
            addSingleMetadataValue(Boolean.TRUE, EMBARGO_DATE_SCHEMA, EMBARGO_DATE_ELEMENT, EMBARGO_DATE_QUALIFIER, liftDateString);
        }
    }

    public void addBitstream(InputStream stream) throws SQLException, IOException {
        try {
            getItem().createSingleBitstream(stream);
        } catch (AuthorizeException ex) {
            log.error("Authorize exception adding bitstream", ex);
        }
    }

    /**
       Retrieves the first bitstream in an item. The bitstream must be in a bundle
       marked "ORIGINAL". Bitstreams for "readme" files are ignored.
    **/
    public Bitstream getFirstBitstream() throws SQLException, IOException {
        Bitstream result = null;
        Item item = getItem();

        Bundle[] bundles = item.getBundles("ORIGINAL");
        if (bundles.length == 0) {
            log.error("Didn't find any original bundles for " + item.getHandle());
            throw new IOException("data bundle for " + item.getHandle() + " not found");
        }
        log.debug("This object has " + bundles.length + " bundles");

        Bitstream[] bitstreams = bundles[0].getBitstreams();
        boolean found = false;
        for(int i = 0; i < bitstreams.length && !found; i++) {
            result = bitstreams[i];
            String name = result.getName();

            if (!name.toLowerCase().contains("readme.")) {
                log.debug("Retrieving bitstream " + name);
                found = true;
            }
        }
        if (!found) {
            log.error("unable to locate a valid bitstream within the first bundle of " + item.getHandle());
            throw new IOException(item.getHandle() + " -- first bitstream wasn't found");
        }

        return result;
    }

    public List<Bitstream> getAllBitstreams() {
        List<Bitstream> bitstreamList = new ArrayList<Bitstream>();
        Item item = getItem();
                
        Bitstream readme = getREADME();
        if(readme != null) {
            bitstreamList.add(readme);
        }

        Bitstream aBitstream = null;
        try {
            Bundle[] bundles = item.getBundles("ORIGINAL"); // anything not ORIGINAL is not a "real" bitstream
            if (bundles.length == 0) {
                log.error("Didn't find any original bundles for " + item.getHandle());
                throw new IOException("data bundle for " + item.getHandle() + " not found");
            }
            log.debug("This object has " + bundles.length + " bundles");

            for(int b = 0; b < bundles.length; b++) {
                Bitstream[] bitstreams = bundles[b].getBitstreams();
                for(int i = 0; i < bitstreams.length; i++) {
                    bitstreamList.add(bitstreams[i]);
                }
            }
        } catch (Exception e) {
            log.error("Unable to process bitstreams of type ORIGINAL", e);
        }

        return bitstreamList;
    }

    public Bitstream getREADME() {
        Item item = getItem();

        try {
            Bundle[] bundles = item.getBundles();

            if (bundles.length > 0) {
                Bitstream[] bitstreams = bundles[0].getBitstreams();
                for (Bitstream bitstream : bitstreams) {
                    if (bitstream.getName().toLowerCase().contains("readme.") || bitstream.getName().equalsIgnoreCase("readme")) {
                        return bitstream;
                    }
                }
            }
        } catch (Exception e) {
            log.error("couldn't get bundles for item " + item.getID());
        }
        return null;
    }

    public Long getTotalStorageSize() throws SQLException {
        // bundles and bitstreams
        Long size = 0L;
        for(Bundle bundle : getItem().getBundles()) {
            for(Bitstream bitstream : bundle.getBitstreams()) {
                // exclude READMEs?
                size += bitstream.getSize();
            }
        }
        return size;
    }

    public String getDescription() throws SQLException {
        String theAbstract = getSingleMetadataValue("dc", "description", null);
        String extraAbstract = getSingleMetadataValue("dc", "description", "abstract");

        if (extraAbstract != null && extraAbstract.length() > 0) {
            theAbstract = theAbstract + "\n" + extraAbstract;
        }

        return theAbstract;
    }

    @Override
    Set<DryadObject> getRelatedObjects(final Context context) throws SQLException {
        return new HashSet<DryadObject>() {{
            add(getDataPackage(context));
        }};
    }
}
