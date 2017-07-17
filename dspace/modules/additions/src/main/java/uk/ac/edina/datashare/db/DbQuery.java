package uk.ac.edina.datashare.db;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

import uk.ac.edina.datashare.eperson.RegistrationDetails;

/**
 * EDINA DSpace database queries
 */
public class DbQuery
{ 
    /** log4j category */
    protected static final Logger LOG = Logger.getLogger(DbQuery.class);
    
    /**
     * Get the batch file for a given batch id and logged in user.
     * @param context DSpace context.
     * @param id The batch import id.
     * @return The map file associated with a batch import.
     */
    public static String fetchBatchMapFile(Context context, int id)
    {
        String file = null;
        
        try
        {
            TableRow row = DatabaseManager.querySingle(context,
                    "SELECT map_file FROM batch_import WHERE id = ? AND eperson_id = ?",
                    new Object[] {id, context.getCurrentUser().getID()});
            
            if(row != null)
            {
                file = row.getStringColumn("map_file");
            }  
        }
        catch(SQLException ex)
        {
            LOG.error("Cannot fetch batch map file: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
        
        return file;
    }

    /**
     * For a given bitstream id fetch the bitstream name
     * @param context DSace context
     * @param bitstreamId Bitstream identifier
     * @return The bitstream name
     */
    public static String fetchBitstreamName(
            Context context,
            int bitstreamId)
    {
        String name = null;
        
        final String QUERY =
            "SELECT name " +
            "FROM   bitstream " +
            "WHERE  bitstream_id = ?";
        
        try
        {
            TableRow row = DatabaseManager.querySingle(
                    context,
                    QUERY,
                    new Object[] {bitstreamId});
                
            name = row.getStringColumn("name");
        }
        catch(SQLException ex)
        {
            LOG.error("Failed to fetch bitstream name: " + ex);
            DatabaseManager.freeConnection(context.getDBConnection());
        }
        
        return name;
    }
    
    public static String fetchDatasetChecksum(
            Context context,
            Item item)
    {
        String checksum = null;
        
        final String QUERY =
            "SELECT checksum " +
            "FROM   dataset " +
            "WHERE  item_id = ?";
        
        try
        {
            TableRow row = DatabaseManager.querySingle(
                    context,
                    QUERY,
                    new Object[] {item.getID()});
            if(row != null){
                checksum = row.getStringColumn("checksum");
            }
        }
        catch(SQLException ex)
        {
            LOG.error("Failed to fetch file name: " + ex);
            DatabaseManager.freeConnection(context.getDBConnection());
        }
        
        return checksum;
    }
    
    public static List<Integer> fetchDatasetIds(Context context)
    {
        ArrayList<Integer> ds = new ArrayList<Integer>(10000);
        
        final String QUERY =
            "SELECT item_id FROM dataset";
        
        try
        {
            TableRowIterator iterator = DatabaseManager.query(context, QUERY);
            while (iterator.hasNext()) {
                ds.add(iterator.next().getIntColumn("item_id"));
            }
        }
        catch(SQLException ex)
        {
            LOG.error("Failed to fetch file name: " + ex);
            DatabaseManager.freeConnection(context.getDBConnection());
        }
        
        return ds;
    }
    
    /**
     * Fetch the owning item for a given bitstream.
     * @param context DSpace context.
     * @param bitstream DSpace bistream.
     * @return DSpace item.
     */
    public static Item fetchItem(
            Context context,
            Bitstream bitstream)
    {
        Item item = null;
        
        final String QUERY = new StringBuffer(
            "SELECT i.item_id ").append(
            "FROM bitstream b, bundle2bitstream b2b, item2bundle i2b, item i ").append(
            "WHERE b.bitstream_id = ? ").append(
            "AND b.bitstream_id   = b2b.bitstream_id ").append(
            "AND b2b.bundle_id    = i2b.bundle_id ").append(
            "AND i2b.item_id      = i.item_id ").toString();

        try
        {
            TableRow row = DatabaseManager.querySingle(
                    context,
                    QUERY,
                    new Object[] {bitstream.getID()});
            
            if(row != null)
            {
                int itemId = row.getIntColumn("item_id");
                item = Item.find(context, itemId);
            }
        }
        catch(SQLException ex)
        {
            LOG.error("Failed to fetch bitstream name: " + ex);
            DatabaseManager.freeConnection(context.getDBConnection());
        }
        
        return item;
    }
    
    /**
     * Get the registration details for a given token
     * @param context DSpace context
     * @param token Registration token
     * @return The registration details
     */
    public static RegistrationDetails fetchRegistrationDetails(Context context, String token)
    {
        RegistrationDetails details = null;
        
        try
        {
            TableRow rd = DatabaseManager.findByUnique(
                    context, "RegistrationData",
                    "token", token);
            
            if (rd != null) 
            {   
                details = new RegistrationDetails(rd.getStringColumn("email"),
                                                  rd.getStringColumn("uun"));
            }
        }
        catch(SQLException ex)
        {
            LOG.error("Cannot fetch registration details: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
        
        return details;
    }
    
    /**
     * @param context DSpace context.
     * @return SWORD API key.
     */
    public static String fetchSwordKey(Context context)
    {
        return fetchSwordKey(context, null);
    }
    
    /**
     * @param context DSpace context.
     * @param eperson DSpace user.
     * @return SWORD API key.
     */
    public static String fetchSwordKey(Context context, EPerson eperson)
    {
        String key = null;
        
        try
        {
            if(eperson == null){
                eperson = context.getCurrentUser();
            }
            
            TableRow rd = DatabaseManager.findByUnique(
                    context,
                    "sword_keys",
                    "eperson_id",
                    eperson.getID());
            
            if (rd != null) 
            {   
                key = rd.getStringColumn("key");
            }
        }
        catch(SQLException ex)
        {
            LOG.error("Cannot SWORD key: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
        
        return key;
    }

    /**
     * Fetch the university user name from the registration data table with an
     * email address
     * @param context DSpace context
     * @param email Email address
     * @return The university user name
     */
    public static String fetchUun(Context context, String email)
    {
        String uun = null;
        
        try
        {
            TableRow rd = DatabaseManager.findByUnique(
                    context, "RegistrationData",
                    "email", email);
            
            if (rd != null) 
            {   
                uun = rd.getStringColumn("uun");
            }
        }
        catch(SQLException ex)
        {
            LOG.error("Cannot fetch UUN: " + ex.getMessage());
            throw new RuntimeException(ex);
        }
        
        return uun;
    }
}
