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
	
	private static String replicadirectory = ConfigurationManager.getProperty("lr", "lr.replication.eudat.replicadirectory");
	
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
        if ( null == maxThreads ) {
            maxThreads = "1";
        }         
        if ( null != maxThreads ) {
            config.put(B2SAFE_CONFIGURATION.IRODS_TRANSFER_MAX_THREADS.name(), maxThreads );
        }
        
		replicationService = new DataSet(config);
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

	public static void setReplicationOn(boolean flag) {
		replicationOn = flag;
	}

	public static List<DataObject> list() throws Exception {
		List<DataObject> dos = replicationService.listDOFromDirectory(replicadirectory, false);
        return dos;
	}
	
	public static List<String> listFilenames() throws Exception {
		return listFilenames(false);
	}
	
	public static List<String> listFilenames(boolean returnAbsPath) throws Exception {
		List<DataObject> dos = list();
		List<String> fileNames = new ArrayList<String>();
		for(DataObject one_do : dos) {
			String name = one_do.getFileName();
			if ( null == name ) {
                name = one_do.getRemoteDirPath();
            }
			fileNames.add(name);
		}
        return fileNames;
	}	

	public static List<DataObject> list(boolean returnAbsPath) throws Exception {
		return replicationService.listDOFromDirectory("", true);
	}

	public static List<DataObject> list(String remoteDirectory, boolean returnAbsPath) throws Exception {
		return replicationService.listDOFromDirectory(remoteDirectory, returnAbsPath);
	}

	public static List<String> listMissingReplicas() throws Exception {
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
	
	// search is not supported by eudaat replication service implementation
	/* public static List<String> search(Map<String, String> metadata) throws Exception {
		return replicationService.search(metadata);
	}*/

	public static Map<String, String> getMetadataOfDataObject(
			String dataObjectAbsolutePath) throws Exception {
		DataObject one_do = new DataObject();
		one_do = replicationService.getMetadataFromOneDOByPath(one_do);
		return getMetadataMap(one_do);
	}
	
	public static Map<String, String> getMetadataMap(
			DataObject one_do) throws Exception {
        Map<String, AVUMetaData> m = one_do.getEudatMetadata();
        Map<String, String> ret = new HashMap<String, String>();
    	for(Map.Entry<String, AVUMetaData> entry : m.entrySet()) {
            ret.put(entry.getKey(), entry.getValue().getValue());
        }
        return ret;
	}	

	
	public static boolean delete(String path) throws Exception  {
        DataObject one_do = new DataObject();
        one_do.setFileName( path );
        one_do.setRemoteDirPath( "" );
        one_do.setRemoteDirPathIsAbsolute( true );
        one_do = replicationService.deleteDO(one_do);
        return one_do.getOperationIsSuccess();
	}

	public static void retriveFile(String remoteFileName, String localFileName) throws Exception {
        DataObject one_do = new DataObject();
        one_do.setLocalFilePath( localFileName );
        one_do.setFileName( remoteFileName );
		replicationService.retrieveOneDOByPath(one_do);
	}

	public static void replicateMissing(Context context) throws Exception {
		replicateMissing(context, -1);
	}

	public static void replicateMissing(Context c, int max) throws Exception {
		for (String handle : listMissingReplicas()) {			
			if (max-- <= 0) {
				return;
			}			
			DSpaceObject dso = HandleManager.resolveToObject(c, handle);
			replicate(c, handle, (Item) dso);
		}
	}


	// Must be PUB without embargo.
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

	public static void replicate(Context context, String handle, Item item) throws UnsupportedOperationException, SQLException {
		replicate(context, handle, item, false);
	}

	public static void replicate(Context context, String handle, Item item, boolean force) throws UnsupportedOperationException, SQLException {
		
		//If replication queue still contains this handle remove it
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

		if (!isReplicatable(item)) {
			String msg = String.format("Cannot replicate non-public item [%s]", handle);
			log.warn(msg);
			throw new UnsupportedOperationException(msg);
		}

		Thread runner = new Thread(new ReplicationThread(context, handle, item, force));
		runner.setPriority(Thread.MIN_PRIORITY);
		runner.setDaemon(true);
		runner.start();
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
	Item item;
	boolean force;
	Context context;

	public ReplicationThread(Context context, String handle, Item item, boolean force) {
		this.context = context;
		this.handle = handle;
		this.item = item;
		this.force = force;
	}

	public Item waitForDspaceItem(Context context) {
		Item item = null;
		// loop for few secs
		for (int i = 0; i < 20; ++i) {
			// sleep 1 sec
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			try {
				item = Item.find(context, item.getID());
				if (item.getOwningCollection()!=null && item.isArchived()) {
					break;
				}
			} catch (SQLException e) {
			}
		}

		return item;
	}

	public void run() {
		try {

			//If retrying a failed item removed from failed
			if(ReplicationManager.failed.containsKey(handle)) ReplicationManager.failed.remove(handle);						
			ReplicationManager.inProgress.add(handle);
			ReplicationManager.log.info("Replication started for item: " + handle);	
			
			context.turnOffAuthorisationSystem();
			ReplicationManager.log.info("Replicating to IRODS");

			// wait for DSpace for submitting the item
			// should not be needed with the new event listener - investigate!
			Item item = waitForDspaceItem(context);
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
            one_do.setRemoteDirPath("");
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
	
	private Context context = null;
	
	private ReplicateAllBackgroundThread() {
		try {
			this.context = new Context();
			this.context.setCurrentUser(EPerson.find(context, 1));
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
				DSpaceObject dso = HandleManager.resolveToObject(context, handle);
				ReplicationManager.replicate(context, handle, (Item) dso);
				
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
		
		try {
			context.complete();
		} catch (SQLException e) {
			ReplicationManager.log.error(e);
		}
		currentThread = null;		
		ReplicationManager.setReplicateAll(false);
		
	}
}