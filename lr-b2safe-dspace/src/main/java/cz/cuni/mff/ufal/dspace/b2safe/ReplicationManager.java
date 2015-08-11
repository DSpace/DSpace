/**
 * Institute of Formal and Applied Linguistics
 * Charles University in Prague, Czech Republic
 * 
 * http://ufal.mff.cuni.cz
 * 
 */

package cz.cuni.mff.ufal.dspace.b2safe;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Metadatum;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.packager.DSpaceAIPDisseminator;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;

import fr.cines.eudat.repopack.b2safe_rp_core.AVUMetaData;
import fr.cines.eudat.repopack.b2safe_rp_core.DataObject;
import fr.cines.eudat.repopack.b2safe_rp_core.DataSet;
import fr.cines.eudat.repopack.b2safe_rp_core.DataSet.B2SAFE_CONFIGURATION;

/**
 * This class is responsible for managing replication. It should be called
 * whenever a submission is complete.
 * 
 * The replication algorithm creates a temporary file in zip format (AIP) in
 * temp directory.
 * 
 * It replicates all files which meet the requirements in method isReplicatable().
 * 
 * At the moment, the only requirement is that the item has dc.rights.label set
 * to PUB.
 */
@SuppressWarnings("deprecation")
public class ReplicationManager {
		
	static final Logger log = Logger.getLogger(ReplicationManager.class);

	static boolean replicationOn = ConfigurationManager.getBooleanProperty("lr", "lr.replication.eudat.on", false);
		
	static DataSet replicationService = null;
		
	static Properties config = null;
	
	public static final List<String> replicationQueue = new ArrayList();
	public static final List<String> inProgress = new ArrayList<String>();
	public static final Map<String, Exception> failed = new HashMap<String, Exception>();
	
	public static final String replicadirectory = ConfigurationManager.getProperty("lr", "lr.replication.eudat.replicadirectory");
	
	private static boolean replicateAll = ConfigurationManager.getBooleanProperty("lr", "lr.replication.eudat.replicateall", false);
	
	//only one replication job at a time
	private static ExecutorService executor = Executors.newFixedThreadPool(1);	

	public static boolean initialize() throws Exception {
		boolean res = false;
		config = new Properties();
		populateConfig(config);

		// jargon specific
		String maxThreads = ConfigurationManager.getProperty(
				"lr", "lr.replication.jargon.numThreads");
		// must be set or b2_core crashes
		if (null == maxThreads) {
			maxThreads = "1";
		}
		if (null != maxThreads) {
			config.put(B2SAFE_CONFIGURATION.IRODS_TRANSFER_MAX_THREADS.name(), maxThreads);
		}

		replicationService = new HackedDataSet(config);
		res = replicationService.initB2safeConnection();
		return res;
	}
	
	public static Properties getConfiguration(){
		return config;
	}
	
	public static boolean isInitialized() {
		if(replicationService!=null) {
			return replicationService.isInitialized();
		} else {
			return false;
		}
	}
	
	public static boolean isReplicationOn() {
		return replicationOn;
	}

	//fixme shouldn't the connection be opened/closed with this flag? Also the executor should be terminated
	public static void setReplicationOn(boolean flag) {
		replicationOn = flag;
	}


	/**
	 * List all the file names inside configured directory homedir/replicadir
	 * @return
	 */
	public static List<String> listFilenames() {
		return listFilenames(replicadirectory, false);
	}

	/**
	 * List filenames of replicated DOs.
	 * @param remotePath is relative to homedir if not absolute
	 * @param isAbsolute
	 * @return
	 */
	public static List<String> listFilenames(String remotePath, boolean isAbsolute) {
		List<DataObject> dos = list(remotePath, isAbsolute);
		List<String> fileNames = new ArrayList<String>();
		for(DataObject one_do : dos) {
			String name = one_do.getFileName();
			fileNames.add(name);
		}
		return fileNames;
	}

	/**
	 * Just a wrapper around DataSet.lisDOFromDirectory(String, boolean)
	 * @param remotePath
	 * @param isAbsolute
	 * @return
	 */
	public static List<DataObject> list(String remotePath, boolean isAbsolute){
		return replicationService.listDOFromDirectory(remotePath, isAbsolute);
	}

	/**
	 * List DOs from the configured homedir/replicadir
	 * @return
	 */
	public static List<DataObject> list(){
		return list(replicadirectory, false);
	}

	/**
	 * Returns handles of items not found in the replicadirectory
	 * @return
	 */
	public static List<String> listMissingReplicas() throws SQLException{
		List<String> alreadyReplicatedItems = listFilenames();
		List<String> allPublicItems = getPublicItemHandles();
		List<String> notFound = new ArrayList<String>();
		for(String publicItem : allPublicItems) {
			if(!alreadyReplicatedItems.contains(handleToFileName(publicItem))) {
				notFound.add(publicItem);
			}
		}		
		return notFound; 
	}
	
	/**
	 * Return metadata map of the specified fileName. The file should be in replicadirectory
	 * @param fileName
	 * @return
	 */
	public static Map<String, String> getMetadataOfDataObject(
			String fileName){
		return getMetadataOfDataObject(fileName, replicadirectory, false);
	}

	/**
	 * Return metadata map for file in remoteDir (possibly absolute path)
	 * @param fileName
	 * @param remoteDir
	 * @param isAbsolute
	 * @return
	 */
	public static Map<String, String> getMetadataOfDataObject(String fileName, String remoteDir, boolean isAbsolute){
		DataObject one_do = new DataObject();
		one_do.setRemoteDirPath(remoteDir);
		one_do.setRemoteDirPathIsAbsolute(isAbsolute);
		one_do.setFileName(fileName);
		one_do = replicationService.getMetadataFromOneDOByPath(one_do);
		return getMetadataMap(one_do);
	}

	/**
	 * Returns the values of AVUMetadata, keys stays the same attribute and unit is ignored
	 * @param one_do
	 * @return
	 * @throws Exception
	 */
	private static Map<String, String> getMetadataMap(
			DataObject one_do) {
		Map<String, AVUMetaData> m = one_do.getEudatMetadata();
		Map<String, String> ret = new HashMap<String, String>();
		for (Map.Entry<String, AVUMetaData> entry : m.entrySet()) {
			ret.put(entry.getKey(), entry.getValue().getValue());
		}
		return ret;
	}


	/**
	 * Delete file from replicadirectory
	 * seeing JargonException caught and logged on delete:No access to item in catalog
	 * org.irods.jargon.core.exception.CatNoAccessException: No access to item in catalog
	 * might mean insufficient rights.
	 * @param fileName
	 * @return
	 */
	public static boolean delete(String fileName) {
		return delete(fileName, replicadirectory, false);
	}

	/**
	 * Delete file from remoteDir (possibly absolute path)
	 * seeing JargonException caught and logged on delete:No access to item in catalog
	 * org.irods.jargon.core.exception.CatNoAccessException: No access to item in catalog
	 * might mean insufficient rights.
	 * @param fileName
	 * @param remoteDir
	 * @param isAbsolute
	 * @return
	 */
	public static boolean delete(String fileName, String remoteDir, boolean isAbsolute){
        DataObject one_do = new DataObject();
        one_do.setFileName( fileName );
        one_do.setRemoteDirPath( remoteDir );
        one_do.setRemoteDirPathIsAbsolute( isAbsolute );
        one_do = replicationService.deleteDO(one_do);
        return one_do.getOperationIsSuccess();
	}

	/**
	 * Remote DO to be retrieved is identified by the fileName and the remoteDir.
	 * The remoteDir might be absolute.
	 * The local copy is stored in localDir directory
	 * @param fileName
	 * @param remoteDir
	 * @param isAbsolute
	 * @param localDir
	 */
	public static void retrieveFile(String fileName, String remoteDir, boolean isAbsolute, String localDir) {
        DataObject one_do = new DataObject();
		one_do.setFileName(fileName);
		one_do.setRemoteDirPath(remoteDir);
		one_do.setRemoteDirPathIsAbsolute(isAbsolute);
		one_do.setLocalFilePath(localDir);
		replicationService.retrieveOneDOByPath(one_do);
	}

	/**
	 * Retrieve a f ile from replicadir and store it in localDir
	 * @param fileName
	 * @param localDir
	 */
	public static void retrieveFile(String fileName, String localDir){
		retrieveFile(fileName, replicadirectory, false, localDir);
	}

	//fixme neither replicateMissing is ever called
	public static void replicateMissing(Context context) throws Exception {
		//fixme the -1 is odd
		replicateMissing(context, -1);
	}

	public static void replicateMissing(Context c, int max) throws Exception {
		for (String handle : listMissingReplicas()) {			
			if (max-- <= 0) {
				return;
			}			
			replicate(handle);
		}
	}



	public static boolean isReplicatable(String handle){
		Context context = null;
		boolean ret = false;
		try{
			context = new Context();
			Item item = (Item)HandleManager.resolveToObject(context, handle);
			ret = isReplicatable(item);
		}catch (SQLException e){
			log.error(e);
		}
		if(context != null) {
			context.abort();
		}
		return ret;
	}
	/**
	 * Must be PUB without embargo.
	 * @param item
	 * @return
	 */
	public static boolean isReplicatable(Item item) {
		
		Context context = null;
		
		try {
			
			context = new Context();

			// not even public
			if (!isPublic(item)) {
				return false;
			}

			// embargoes
			String embargoLiftField = ConfigurationManager.getProperty("embargo.field.lift");
			if(embargoLiftField!=null && !embargoLiftField.isEmpty()) {
				Metadatum[] mdEmbargo = item.getMetadataByMetadataString(embargoLiftField);
				if(mdEmbargo!=null && mdEmbargo.length>0) {
					return false;
				}				
			}
				
			// archived and withdrawn
			if (!item.isArchived() || item.isWithdrawn()) {
				return false;
			}

			// is authorised
			AuthorizeManager.authorizeAction(context, item, Constants.READ);
			
		} catch (Exception e) {
			return false;
		} finally {
			try {
				context.complete();
			}catch(Exception e){ }
		}

		// passed all tests
		return true;
	}

	private static boolean isPublic(Item i) {
		Metadatum[] pub_dc = i.getMetadata("dc", "rights", "label", Item.ANY);
		if (pub_dc.length > 0) {
			for (Metadatum dc : pub_dc) {
				if (dc.value.equals("PUB")) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static DataSet getReplicationService() {
		return replicationService;
	}

	public static List<String> getPublicItemHandles() throws SQLException {
		Context context = new Context();
		ItemIterator it = Item.findAll(context);
		List<String> handles = new ArrayList<String>();
		while (it.hasNext()) {
			Item item = it.next();
			if (isReplicatable(item)) {
				handles.add(item.getHandle());
			}
		}
		context.complete();
		return handles;
	}

	public static List<String> getNonPublicItemHandles() throws SQLException {
		Context context = new Context();
		ItemIterator it = Item.findAll(context);
		List<String> handles = new ArrayList<String>();
		while (it.hasNext()) {
			Item item = it.next();
			if (!isReplicatable(item)) {
				handles.add(item.getHandle());
			}
		}
		context.complete();
		return handles;
	}

	public static void replicate(String handle) throws UnsupportedOperationException, SQLException {
		replicate(handle, false);
	}

	public static void replicate(String handle, boolean force) throws UnsupportedOperationException, SQLException {
		//fixme DataSet doesn't allow passing the force flag, can't be overridden without use of reflections
		ReplicationManager.replicationQueue.remove(handle);
		
		// not set up
		if (!replicationService.isInitialized()) {
			String msg = String.format("Replication not set up - [%s] will not be processed", handle);
			log.warn(msg);
			throw new UnsupportedOperationException(msg);
		}

		// not turned on
		if (!isReplicationOn()) {
			String msg = String.format("Replication turned off - [%s] will not be processed", handle);
			log.warn(msg);
			throw new UnsupportedOperationException(msg);
		}

		if (!isReplicatable(handle)) {
			String msg = String.format("Cannot replicate non-public item [%s]", handle);
			log.warn(msg);
			throw new UnsupportedOperationException(msg);
		}

		Thread runner = new Thread(new ReplicationThread(handle, force));
		runner.setPriority(Thread.MIN_PRIORITY);
		runner.setDaemon(true);
		//don't start the thread if using executors
		//runner.start();
		executor.submit(runner);
	}

    public static String handleToFileName(String handle) {
    	return handle.replace( "/", "_" ) + ".zip";
    }
    
    static void populateConfig(Properties config) {
		config.put(B2SAFE_CONFIGURATION.B2SAFE_TRANS_PROTOCOL.name(),
		        ConfigurationManager.getProperty("lr", "lr.replication.eudat.protocol"));
		config.put(B2SAFE_CONFIGURATION.HOST.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.host"));
		config.put(B2SAFE_CONFIGURATION.PORT.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.port"));
		config.put(B2SAFE_CONFIGURATION.USER_NAME.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.username"));
		config.put(B2SAFE_CONFIGURATION.PASSWORD.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.password"));
		config.put(B2SAFE_CONFIGURATION.HOME_DIRECTORY.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.homedirectory"));
		config.put(B2SAFE_CONFIGURATION.ZONE.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.zone"));
		config.put(B2SAFE_CONFIGURATION.DEFAULT_STORAGE.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.defaultstorage"));		
		config.put(B2SAFE_CONFIGURATION.RESOURCE_ID.name(),
		    ConfigurationManager.getProperty("lr", "lr.replication.eudat.id"));
    }
    
    public static Map<String, String> getServerInformation() {
    	Map<String, String> info = replicationService.getServerInformation();
    	return info;
    }
    
    public static void setReplicateAll(boolean status) {
    	replicateAll = status;
    	if(status==true) {
    		// this will start the thread if not already running
    		ReplicateAllBackgroundThread.initiate();
    	} else {
    		replicationQueue.clear();
    	}
    }
    
    public static boolean isReplicateAllOn() {
    	return replicateAll;
    }

    @Override
    protected void finalize() throws Throwable {
    	super.finalize();
    	executor.shutdown();
    }
} // class

@SuppressWarnings("deprecation")
class ReplicationThread implements Runnable {
	
	String handle;
	boolean force;

	public ReplicationThread(String handle, boolean force) {
		this.handle = handle;
		this.force = force;
	}

	public void run() {
		Context context = null;
		try {

			context = new Context();
			Item item = (Item) HandleManager.resolveToObject(context, handle);
			//If retrying a failed item removed from failed
			if(ReplicationManager.failed.containsKey(handle)) ReplicationManager.failed.remove(handle);						
			ReplicationManager.inProgress.add(handle);
			ReplicationManager.log.info("Replication started for item: " + handle);	
			
			context.turnOffAuthorisationSystem();
			ReplicationManager.log.info("Replicating to IRODS");

			if (handle == null) {
				handle = item.getHandle();
			}

			if (handle == null) {
				ReplicationManager.log.warn(String.format(
						"Could not replicate [internal:%s] - no handle",
						item.getID()));
				return;
			}

			// prepare AIP
			File file = getTemporaryFile(ReplicationManager.handleToFileName(handle));
			file.deleteOnExit();

			new DSpaceAIPDisseminator().disseminate(context, item, new PackageParameters(), file);

			// AIP failure
			if (!file.exists()) {
				throw new IOException(String.format("AIP package has not been created [%s]", file.getCanonicalPath()));
			}

			// replicate
			Metadatum[] mdURI = item.getMetadataByMetadataString("dc.identifier.uri");
			if(mdURI==null || mdURI.length<=0) {
				throw new RuntimeException("dc.identifier.uri is missing for item " + item.getHandle());
			}
			String itemUrl = mdURI[0].value;
            DataObject one_do = new DataObject();
            one_do.setRor(itemUrl);
            one_do.setLocalFilePath(file.getAbsolutePath());
            one_do.setFileName(file.getName());
            one_do.setRemoteDirPath(ReplicationManager.replicadirectory);
            one_do.setRemoteDirPathIsAbsolute(false);			
            ReplicationManager.getReplicationService().replicateOneDO(one_do);
            ReplicationManager.log.info("Replication finished: " + handle);
			ReplicationManager.inProgress.remove(handle);	            
		} catch (Exception e) {
			ReplicationManager.log.error(String.format("Could not replicate [%s] [%s]", this.handle, e.toString()), e);
			ReplicationManager.inProgress.remove(handle);
			ReplicationManager.failed.put(handle, e);			
		}

		try {
			if (context != null) {
				context.restoreAuthSystemState();
				context.complete();
			}
		} catch (SQLException e) {
		}
	}

	private static File getTemporaryFile(String fileName) throws IOException {
		File file = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
		if (file.exists()) {
			if (!file.delete()) {
				return null;
			}
		}
		file.createNewFile();
		return file;
	}
}

class ReplicateAllBackgroundThread extends Thread {

	private static ReplicateAllBackgroundThread currentThread = null;	
	
	private ReplicateAllBackgroundThread() {
	}
	
	public static void initiate() {
		if(currentThread==null) {
			currentThread = new ReplicateAllBackgroundThread();
			currentThread.start();
		}
	}
	
	@Override
	public void run() {
		if(!ReplicationManager.isReplicationOn() || !ReplicationManager.isReplicateAllOn()) {
			currentThread = null;
			return;
		}

		try {
			ReplicationManager.replicationQueue.addAll(ReplicationManager.listMissingReplicas());
		} catch (Exception e) {
			ReplicationManager.log.error(e);
			currentThread = null;			
			return;
		}
		
		while (!ReplicationManager.replicationQueue.isEmpty()) {
		
			try {
				
				String handle = ReplicationManager.replicationQueue.remove(0);
				
				if(ReplicationManager.failed.containsKey(handle) || ReplicationManager.inProgress.contains(handle)) continue;
				ReplicationManager.replicate(handle);
				
				try {
					//wait for few seconds before starting next item 
					Thread.sleep(10000);
				} catch (InterruptedException e) {
				}
				
				// test again if the replication service and replicate all is still on
				if(!ReplicationManager.isReplicationOn() || !ReplicationManager.isReplicateAllOn()) {
					currentThread = null;
					break;
				}
										
			} catch (SQLException e) {
				ReplicationManager.log.error(e);
			} catch (Exception e) {
				ReplicationManager.log.error(e);
			}
		}
		currentThread = null;
		ReplicationManager.setReplicateAll(false);
		
	}
}