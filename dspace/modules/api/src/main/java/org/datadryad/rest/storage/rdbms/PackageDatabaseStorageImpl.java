/*
 */
package org.datadryad.rest.storage.rdbms;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Package;
import org.datadryad.rest.storage.AbstractPackageStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.JournalUtils;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.File;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PackageDatabaseStorageImpl extends AbstractPackageStorage {
    private static Logger log = Logger.getLogger(PackageDatabaseStorageImpl.class);

    public PackageDatabaseStorageImpl(String configFileName) {
        setConfigFile(configFileName);
    }

    public PackageDatabaseStorageImpl() {
        // For use when ConfigurationManager is already configured
    }

    public final void setConfigFile(String configFileName) {
        File configFile = new File(configFileName);

        if (configFile != null) {
            if (configFile.exists() && configFile.canRead() && configFile.isFile()) {
                ConfigurationManager.loadConfig(configFile.getAbsolutePath());
            }
        }
    }

    private static Context getContext() {
        Context context = null;
        try {
            context = new Context();
        } catch (SQLException ex) {
            log.error("Unable to instantiate DSpace context", ex);
        }
        return context;
    }

    private static void completeContext(Context context) throws SQLException {
        try {
            context.complete();
        } catch (SQLException ex) {
            // Abort the context to force a new connection
            abortContext(context);
            throw ex;
        }
    }

    private static void abortContext(Context context) {
        if (context != null) {
            context.abort();
        }
    }

    @Override
    public Boolean objectExists(StoragePath path, Package packageConcept) {
        return true;
    }

    protected void addAll(StoragePath path, List<Package> packageConcepts) throws StorageException {
        // passing in a limit of null to addResults should return all records
        addResults(path, packageConcepts, null, null);
    }

    @Override
    protected void addResults(StoragePath path, List<Package> packageList, String searchParam, Integer limit) throws StorageException {
        Context context = null;
        try {
            context = getContext();
            DryadJournalConcept journal = JournalConceptDatabaseStorageImpl.getJournalConceptByCodeOrISSN(context, path.getJournalRef());
            LinkedHashMap<Item, String> packages = JournalUtils.getArchivedPackagesSortedRecent(context, journal.getFullName(), limit);
            for (Item item : packages.keySet()) {
                Package dataPackage = new Package(new DryadDataPackage(item));
                packageList.add(dataPackage);
            }
        } catch (SQLException ex) {
            log.error("error: " + ex.getMessage());
            abortContext(context);
            throw new StorageException("Exception reading packages", ex);
        }
    }

    @Override
    protected void createObject(StoragePath path, Package packageConcept) throws StorageException {
        throw new StorageException("can't create a package");
    }

    @Override
    protected Package readObject(StoragePath path) throws StorageException {
        Package packageConcept = null;
        Context context = null;
        try {
            context = getContext();
            completeContext(context);
        } catch (Exception e) {
            abortContext(context);
            throw new StorageException("Exception reading package: " + e.getMessage());
        }
        return packageConcept;
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        throw new StorageException("can't delete an package");
    }

    @Override
    protected void updateObject(StoragePath path, Package packageConcept) throws StorageException {
        throw new StorageException("can't update an package");
    }


}
