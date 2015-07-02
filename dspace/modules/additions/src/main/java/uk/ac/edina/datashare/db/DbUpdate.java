package uk.ac.edina.datashare.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * DataShare Dspace database updates.
 */
public class DbUpdate
{
	protected static Logger LOG = Logger.getLogger(DbUpdate.class);
	
    /**
     * Delete a batch import entry from database.
     * @param context DSpace context.
     * @param batchId batch id.
     */
    public static void deleteBatchImport(Context context, int batchId){
        PreparedStatement stmt = null;
        
        final String DELETE_BATCH = 
            "DELETE FROM batch_import WHERE id = ?";
        try
        {
            stmt = context.getDBConnection().prepareStatement(DELETE_BATCH);        
            stmt.setInt(1, batchId);
            stmt.executeUpdate();            
        }
        catch (SQLException e)
        {
            LOG.error("Problem deleting batch import entry. " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        finally
        {
            cleanUp(stmt);
        }
    }
    
    /**
     * Insert a batch import. 
     * @param context DSpace context.
     * @param mapFile Full path to batch mapfile.
     * @return The primary key of the new entry.
     */
    public static int insertBatchImport(Context context, String mapFile)
    {
        int key = -1;
        PreparedStatement stmt = null;
        
        final String INSERT_BATCH = 
            "INSERT INTO batch_import(eperson_id, map_file) VALUES (?,?) RETURNING id";
        try
        {
            stmt = context.getDBConnection().prepareStatement(INSERT_BATCH);
        
            stmt.setInt(1, context.getCurrentUser().getID());
            stmt.setString(2, mapFile);
            
            stmt.execute();
            
            ResultSet rs = stmt.getResultSet();
            if (rs.next()) {
                key = rs.getInt(1);
              }
        }
        catch (SQLException e)
        {
            LOG.error("Problem inserting batch import entry. " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        finally
        {
            cleanUp(stmt);
        }
        
        return key;
    }

    /**
     * Insert a new UUN/Email mapping entry 
     * @param context DSpace context
     * @param uun University User Name
     * @param email Email address
     */
    public static void insertUunEntry(Context context, String uun, String email)
    {
        PreparedStatement stmt = null;
        
        final String INSERT_UUN = 
            "INSERT INTO uun2email (uun, email) VALUES (?,?)";
        try
        {
            stmt = context.getDBConnection().prepareStatement(INSERT_UUN);
        
            stmt.setString(1, uun);
            stmt.setString(2, email);
            
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            LOG.error("Problem inserting uun/email entry. " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        finally
        {
            cleanUp(stmt);
        }
    }
    
    /**
     * Insert new SWORD API key.
     * @param context DSpace context.
     * @param eperson DSpace user.
     */
    public static void insertSwordKey(Context context, EPerson eperson)
    {
        PreparedStatement stmt = null;
        
        final String INSERT_UUN = 
            "INSERT INTO sword_keys (eperson_id, key) VALUES (?,?)";
        try
        {
            stmt = context.getDBConnection().prepareStatement(INSERT_UUN);
        
            stmt.setInt(1, eperson.getID());
            stmt.setString(2, UUID.randomUUID().toString());
            
            stmt.executeUpdate();
        }
        catch (SQLException e)
        {
            LOG.error("Problem inserting sword key entry. " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        finally
        {
            cleanUp(stmt);
        }
    }
    
    /**
     * Helper clean up method
     * @param stmt
     */
    protected static void cleanUp(PreparedStatement stmt)
    {
        if(stmt != null)
        {
            try
            {
                stmt.close();
            }
            catch(SQLException ex)
            {
                LOG.error("Problem in clean up statement. " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
    }

}
