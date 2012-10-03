package org.dspace.doi;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;
import org.garret.perst.*;

public class DOIDatabase implements org.springframework.beans.factory.InitializingBean{

	private static Logger LOG = Logger.getLogger(DOIDatabase.class);

	private static DOIDatabase DATABASE;//= new DOIDatabase();

	private Storage myStorage;

    private ConfigurationService configurationService=null;



	private DOIDatabase(){}


    public void afterPropertiesSet() throws Exception {
        StorageFactory dbFactory = StorageFactory.getInstance();
		String dbPathPropValue = System.getProperty("doi.db.path");
		String dbPath;
		try {
			if (dbPathPropValue == null || dbPathPropValue.equals("")) {
				dbPath = configurationService.getProperty("doi.db.fspath");
			}
			else {
				dbPath = dbPathPropValue;
			}
		}
		catch (RuntimeException details) {
			// For running it out of DSpace services (rm this eventually)
			dbPath = "/opt/dryad/doi-minter/doi.db";
		}

		LOG.debug("Opening DOI database located at: " + dbPath);

		myStorage = dbFactory.createStorage();
		myStorage.setProperty("perst.multiclient.support", Boolean.TRUE);

		myStorage.setProperty("perst.lock.file", Boolean.TRUE);
		myStorage.open(dbPath, Storage.DEFAULT_PAGE_POOL_SIZE);

		myStorage.beginThreadTransaction(Storage.READ_WRITE_TRANSACTION);

		if (myStorage.getRoot() == null) {
			LOG.debug("Database root not found -- creating one");
			myStorage.setRoot(new DatabaseRoot());
		}

		myStorage.endThreadTransaction();
        DATABASE=this;
    }



	public static DOIDatabase getInstance() {
		return DATABASE;
	}

	public void close() {
		if (myStorage != null && myStorage.isOpened()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Closing DOI database");
			}

			myStorage.close();
		}
	}

	public boolean put(DOI aDOI) {
		boolean put;

		myStorage.beginThreadTransaction(Storage.READ_WRITE_TRANSACTION);
        put = ((DatabaseRoot) myStorage.getRoot()).put(aDOI);

		if (put) {
			myStorage.endThreadTransaction();
		}
		else {
			myStorage.rollbackThreadTransaction();
		}

		return put;
	}

	public DOI set(DOI aDOI) {
		DOI doi;

		myStorage.beginThreadTransaction(Storage.READ_WRITE_TRANSACTION);

		try {
			doi = ((DatabaseRoot) myStorage.getRoot()).set(aDOI);
		}
		catch (AssertionError details) {
			myStorage.rollbackThreadTransaction();
			throw details;
		}

		myStorage.endThreadTransaction();

		return doi;
	}

	public boolean remove(DOI aDOI) {
		boolean result;
		myStorage.beginThreadTransaction(Storage.READ_WRITE_TRANSACTION);

		try {
			result = ((DatabaseRoot) myStorage.getRoot()).remove(aDOI);
		}
		catch (AssertionError details) {
			myStorage.rollbackThreadTransaction();
			throw details;
		}

		myStorage.endThreadTransaction();
		return result;
	}

	public DOI getByDOI(String aDOIKey) {
		DOI doi;

		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
		doi = ((DatabaseRoot) myStorage.getRoot()).getByDOI(aDOIKey);
		myStorage.endThreadTransaction();

		return doi;
	}

    public Set<DOI> getByURL(String aURLKey) {
		Set<DOI> dois=null;
		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
        dois=((DatabaseRoot) myStorage.getRoot()).getByURL(aURLKey);
		myStorage.endThreadTransaction();
		return dois;
	}

    public Set<DOI> getALL() {
        Set<DOI> dois=null;
		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
		dois= ((DatabaseRoot) myStorage.getRoot()).getAll();
		myStorage.endThreadTransaction();
		return dois;
	}



//	public boolean contains(String aURLKey) {
//		boolean found;
//
//		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
//		found = ((DatabaseRoot) myStorage.getRoot()).contains(aURLKey);
//		myStorage.endThreadTransaction();
//
//		return found;
//	}

	public boolean contains(DOI aDOI) {
		boolean found;

		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
		found = ((DatabaseRoot) myStorage.getRoot()).contains(aDOI);
		myStorage.endThreadTransaction();

		return found;
	}

	public void dumpTo(FileWriter aFileWriter) throws IOException {
		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
		((DatabaseRoot) myStorage.getRoot()).dumpTo(aFileWriter);
		myStorage.endThreadTransaction();
	}

	public void dump(OutputStream aOut) throws IOException {
		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
		((DatabaseRoot) myStorage.getRoot()).dump(aOut);
		myStorage.endThreadTransaction();
	}

	public int size() {
		int size;

		myStorage.beginThreadTransaction(Storage.READ_ONLY_TRANSACTION);
		size = ((DatabaseRoot) myStorage.getRoot()).size();
		myStorage.endThreadTransaction();

		return size;
	}


	private class DatabaseRoot extends Persistent {

		//private Index<DOI> myURLIndex;
		private Index<DOI> myDOIIndex;


		private DatabaseRoot() {
			//myURLIndex = myStorage.createThickIndex(String.class);
			myDOIIndex = myStorage.createIndex(String.class, true);
		}

		private boolean put(DOI aDOI) {
			URL url = aDOI.getTargetURL();

			if (url == null) {
				throw new DOIFormatException("DOI is missing it's associated URL");
			}
			Key doiKey = new Key(aDOI.toString());

            // if exists remove the DOI before adding the new one.
            if(myDOIIndex.get(doiKey)!=null)
                remove(aDOI);

            if (myDOIIndex.put(aDOI.toString(), aDOI) == false) {
				LOG.error("Rollback b/c couldn't add: " + aDOI);
				return false;
			}
			else {
				return true;
			}
		}

		private DOI set(DOI aDOI) {
			URL url = aDOI.getTargetURL();

			if (url == null) {
				throw new DOIFormatException(
						"DOI is missing it's associated URL");
			}

			Key doiKey = new Key(aDOI.toString());
			myDOIIndex.set(doiKey, aDOI);
			return aDOI;
		}

		private DOI getByDOI(String aDOIKey) {
			try {
				DOI doi = myDOIIndex.get(new Key(aDOIKey));

				if (doi != null) {
					doi = doi.cloneDOI();
				}

				return doi;
			}
			catch (StorageError details) {
				if (LOG.isInfoEnabled()) {
					LOG.info(details.getMessage());
				}

				return null;
			}
		}


        private Set<DOI> getAll(){
            Set<DOI>setOfDoi=new HashSet<DOI>();
			Iterator iter = myDOIIndex.iterator();
            while(iter.hasNext()){
                setOfDoi.add((DOI)iter.next());
            }
            return setOfDoi;
		}


        private Set<DOI> getByURL(String aURLKey){
            Set<DOI>setOfDoi=new HashSet<DOI>();
			try {
                Iterator iter = myDOIIndex.iterator();
                while(iter.hasNext()){
                    DOI doi = (DOI)iter.next();
                    if(doi.getTargetURL().toString().equals(aURLKey.toString()))
                        setOfDoi.add(doi);
                }


                //DOI doi = new DOI();
                //doi.setTargetURL(new URL(aURLKey));


//                org.garret.perst.Query<DOI> query = myStorage.createQuery();
//                query.addIndex("index", myDOIIndex);
//                query.prepare(DOI.class, "myPrefix=?");
//                query.setParameter(1, "10.255");
//                IterableIterator<DOI> dois = query.execute(myDOIIndex.iterator());
//
//                while(dois.hasNext()){
//                    setOfDoi.add(dois.next());
//                }
			}catch (StorageError details) {
				if (LOG.isInfoEnabled()) {
					LOG.info(details.getMessage());
				}
			}
            return setOfDoi;
		}

		private boolean contains(DOI aDOI) {
			Key key = new Key(aDOI.toString());
			DOI doi = myDOIIndex.get(key);

			if (doi != null) {
				LOG.debug("Database says " + aDOI + " exists");
				return true;
			}
			else {
				return false;
			}
		}

		private int size() {
			return myDOIIndex.size();
		}

		private void dumpTo(FileWriter aFileWriter) throws IOException {
			BufferedWriter writer = new BufferedWriter(aFileWriter);
			Iterator<DOI> iterator = myDOIIndex.iterator();

			while (iterator.hasNext()) {
				DOI doi = iterator.next();

				writer.write(doi.toString() + " "
						+ doi.getTargetURL().toString());
				writer.newLine();
			}

			writer.close();
		}

		private void dump(OutputStream aOut) throws IOException {
			BufferedOutputStream out = new BufferedOutputStream(aOut);
			Iterator<DOI> iterator = myDOIIndex.iterator();
			byte[] eol = System.getProperty("line.separator").getBytes();

			while (iterator.hasNext()) {
				DOI doi = iterator.next();

				out.write(doi.toString().getBytes());
				out.write(" ".getBytes());
				out.write(doi.getTargetURL().toString().getBytes());
				out.write(eol);
			}

			out.close();
		}

		private boolean remove(DOI aDOI) {
			URL url = aDOI.getTargetURL();

			if (url == null) {
				throw new DOIFormatException(
						"DOI is missing it's associated URL");
			}

			try {
				Key doiKey = new Key(aDOI.toString());
				Key urlKey = new Key(url.toString());

				if (LOG.isDebugEnabled()) {
					LOG.debug("Trying to remove: " + aDOI.toString());
				}

				DOI doi1 = myDOIIndex.remove(doiKey);
				return doi1 != null;
			}
			catch (StorageError details) {
				LOG.warn(details.getMessage(), details);
				return false;
			}
		}
	}

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}