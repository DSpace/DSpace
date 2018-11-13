/*
 */
package org.datadryad.rest.storage.rdbms;

import org.apache.log4j.Logger;
import org.datadryad.api.DashService;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DryadJournalConcept;
import org.datadryad.rest.models.Package;
import org.datadryad.rest.models.ResultSet;
import org.datadryad.rest.storage.AbstractPackageStorage;
import org.datadryad.rest.storage.StorageException;
import org.datadryad.rest.storage.StoragePath;
import org.dspace.JournalUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

import java.io.File;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PackageDatabaseStorageImpl extends AbstractPackageStorage {
    private static Logger log = Logger.getLogger(PackageDatabaseStorageImpl.class);

    private static boolean useDryadClassic = true;
    private static DashService dashService = null;

    static {
        String dryadSystem = ConfigurationManager.getProperty("dryad.system");
        if (dryadSystem != null && dryadSystem.toLowerCase().equals("dash")) {
            useDryadClassic = false;
            dashService = new DashService();
        }
    }
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

    @Override
    public Boolean objectExists(StoragePath path, Package pkg) {
        return true;
    }

    protected void addAll(StoragePath path, List<Package> pkgs) throws StorageException {
        // passing in a limit of null to addResults should return all records
        addResults(path, pkgs, null, null, 0);
    }

    @Override
    protected ResultSet addResults(StoragePath path, List<Package> packageList, String searchParam, Integer limit, Integer cursor) throws StorageException {
        Context context = null;
        ResultSet resultSet = null;
        try {
            context = getContext();
            DryadJournalConcept journal = JournalConceptDatabaseStorageImpl.getJournalConceptByCodeOrISSN(context, path.getJournalRef());
            TreeMap<Integer, Date> rawItemList = JournalUtils.getArchivedPackagesFromKeyset(context, journal, 0);
            resultSet = new ResultSet(rawItemList.keySet(), limit, cursor);
            packageList.addAll(Package.getPackagesForItemSet(resultSet.getCurrentSet(cursor), limit, context));
        } catch (SQLException ex) {
            log.error("error: " + ex.getMessage());
            abortContext(context);
            throw new StorageException("Exception reading packages", ex);
        }
        return resultSet;
    }

    @Override
    public ResultSet addResultsInDateRange(StoragePath path, List<Package> packageList, Date dateFrom, Date dateTo, Integer limit, Integer cursor) throws StorageException {
        Context context = null;
        ResultSet resultSet = null;
        super.addResultsInDateRange(path, packageList, dateFrom, dateTo, limit, cursor);

        try {
            context = getContext();
            DryadJournalConcept journal = JournalConceptDatabaseStorageImpl.getJournalConceptByCodeOrISSN(context, path.getJournalRef());
            TreeMap<Integer, Date> rawItemList = JournalUtils.getArchivedPackagesFromKeyset(context, journal, cursor);
            TreeSet<Integer> itemSet = new TreeSet<Integer>();
            for (Integer itemID : rawItemList.keySet()) {
                if (rawItemList.get(itemID).before(dateTo) && rawItemList.get(itemID).after(dateFrom)) {
                    itemSet.add(itemID);
                }
            }
            resultSet = new ResultSet(itemSet, limit, cursor);
            packageList.addAll(Package.getPackagesForItemSet(resultSet.getCurrentSet(cursor), limit, context));
        } catch (SQLException ex) {
            log.error("error: " + ex.getMessage());
            abortContext(context);
            throw new StorageException("Exception reading packages", ex);
        }
        return resultSet;
    }

    @Override
    protected void createObject(StoragePath path, Package pkg) throws StorageException {
        if (useDryadClassic) {
            throw new StorageException("can't create a package");
        } else {
            dashService.putDataset(pkg);
            dashService.setPublicationISSN(pkg, pkg.getJournalConcept().getISSN());
            dashService.setManuscriptNumber(pkg, pkg.getManuscriptNumber());
        }
    }

    @Override
    protected Package readObject(StoragePath path) throws StorageException {
        Package pkg = null;
        Context context = null;
        try {
            context = getContext();
            completeContext(context);
        } catch (Exception e) {
            abortContext(context);
            throw new StorageException("Exception reading package: " + e.getMessage());
        }
        return pkg;
    }

    @Override
    protected void deleteObject(StoragePath path) throws StorageException {
        throw new StorageException("can't delete an package");
    }

    @Override
    protected void updateObject(StoragePath path, Package pkg) throws StorageException {
        throw new StorageException("can't update an package");
    }


}
