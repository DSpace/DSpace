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
import java.util.*;

import fr.cines.eudat.repopack.b2safe_rp_core.AVUMetaData;
import fr.cines.eudat.repopack.b2safe_rp_core.DataObject;
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

	static boolean replicationOn = ConfigurationManager.getBooleanProperty(
        "lr", "lr.replication.on", false);
	
	static final String WHO = ConfigurationManager.getProperty(
        "dspace.url");

	static DataSet replicationService = null;
	
	// mandatory from CINES: EUDAT_ROR, OTHER_From, OTHER_AckEmail
	public enum MANDATORY_METADATA {
		EUDAT_ROR,
		OTHER_From,
		OTHER_AckEmail
	}
	
	static Properties config = null;

	public static boolean initialize() throws Exception {
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
        boolean res = replicationService.initB2safeConnection();
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
        List<DataObject> dos = replicationService.listDOFromDirectory( "", false );
        return dos;
	}

	public static List<String> listMissingReplicas() throws Exception {
		List<DataObject> alreadyReplicatedItems = list();
		List<String> allPublicItems = getPublicItemHandles();
		List<String> notFound = new ArrayList<String>();
		for ( String publicItem : allPublicItems ) {
            boolean already_replicated = false;
            for ( DataObject one_do : alreadyReplicatedItems ) {
                String name = one_do.getFileName();
                if ( null == name ) {
                    name = one_do.getRemoteDirPath();
                }
		    	if ( null != name && name.contains(handleToFileName(publicItem))) {
			    	already_replicated = true;
                    break;
    			}
            }
            if ( !already_replicated ) {
                notFound.add(publicItem);
            }
		}		
		return notFound; 
	}

	public static Map<String, String> getMetadataOfDataObject(
			String dataObjectePath) throws Exception
    {
        DataObject one_do = new DataObject();
        one_do.setFileName(dataObjectePath);
        one_do = replicationService.getMetadataFromOneDOByPath(one_do);
        return getMetadataMap(one_do);
	}

	public static Map<String, String> getMetadataMap(
            DataObject one_do) throws Exception
    {
        Map<String, AVUMetaData> m = one_do.getEudatMetadata();
        Map<String, String> ret = new HashMap<String, String>();
    	for ( Map.Entry<String, AVUMetaData> entry : m.entrySet() ) {
            ret.put( entry.getKey(), entry.getValue().getValue() );
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

	public static void retrieveFile(String remoteFileName, String localFileName) throws Exception {
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
				if ( null != mdEmbargo && 0 < mdEmbargo.length ) {
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
			}catch(Exception e){
            }
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

		Thread runner = new Thread(new ReplicationThread(context.getCurrentUser(), handle, item, force));
		runner.setPriority(Thread.MIN_PRIORITY);
		runner.setDaemon(true);
		runner.start();
	}

    public static String handleToFileName(String handle) {
    	return handle.replace( "/", "_" ) + ".zip";
    }
    
    static void populateConfig(Properties config) {
        config.put(B2SAFE_CONFIGURATION.B2SAFE_TRANS_PROTOCOL.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.protocol"));
    	config.put(B2SAFE_CONFIGURATION.HOST.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.host"));
		config.put(B2SAFE_CONFIGURATION.PORT.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.port"));
		config.put(B2SAFE_CONFIGURATION.USER_NAME.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.username"));
		config.put(B2SAFE_CONFIGURATION.PASSWORD.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.password"));
		config.put(B2SAFE_CONFIGURATION.HOME_DIRECTORY.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.homedirectory"));
		config.put(B2SAFE_CONFIGURATION.ZONE.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.zone"));
		config.put(B2SAFE_CONFIGURATION.DEFAULT_STORAGE.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.defaultstorage"));
		config.put(B2SAFE_CONFIGURATION.RESOURCE_ID.name(),
            ConfigurationManager.getProperty("lr", "lr.replication.id"));
    }

    public static Map<String, String> getServerInformation() {
    	Map<String, String> info = replicationService.getServerInformation();
    	return info;
    }
    
} // class

@SuppressWarnings("deprecation")
class ReplicationThread implements Runnable {
	
	String handle;
	int itemId;
	int epersonId;
	boolean force;

	public ReplicationThread(EPerson eperson, String handle, Item item, boolean force) {
		this.handle = handle;
		this.itemId = item.getID();
		this.epersonId = eperson.getID();
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
				item = Item.find(context, itemId);
				if (item.getOwningCollection()!=null && item.isArchived()) {
					break;
				}
			} catch (SQLException e) {
			}
		}

		return item;
	}

	public void run() {
		Context context = null;
		try {
			context = new Context();
			context.setCurrentUser(EPerson.find(context, this.epersonId));
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
            one_do.setLocalFilePath( file.getAbsolutePath() );
            one_do.setFileName( file.getName() );
            one_do.setRemoteDirPath( "" );
            one_do.setRemoteDirPathIsAbsolute( false );

			ReplicationManager.getReplicationService().replicateOneDO( one_do );
		} catch (Exception e) {
			ReplicationManager.log.error(
                String.format("Could not replicate [%s] [%s]", this.handle, e.toString()), e);
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
