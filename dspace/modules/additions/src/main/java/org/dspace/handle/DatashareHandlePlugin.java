package org.dspace.handle;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.identifier.HandleIdentifierProvider;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;

import net.handle.hdllib.HandleException;
import net.handle.hdllib.Util;

public class DatashareHandlePlugin extends HandlePlugin{
    private static Logger log = Logger.getLogger(DatashareHandlePlugin.class);
    
    /**
     * Return the raw values for this handle. This implementation returns a
     * single URL value.
     * 
     * @param theHandle
     *            byte array representation of handle
     * @param indexList
     *            ignored
     * @param typeList
     *            ignored
     * @return A byte array with the raw data for this handle. Currently, this
     *         consists of a single URL value.
     * @exception HandleException
     *                If an error occurs while calling the Handle API.
     */
    @Override
    public byte[][] getRawHandleValues(
            byte[] theHandle,
            int[] indexList,
            byte[][] typeList) throws HandleException{
        if(theHandle == null){
            throw new HandleException(HandleException.INTERNAL_ERROR);
        }

        String handle = Util.decodeString(theHandle);
        String parts[] = handle.split("/");
        String prefix = null;
        
        if(parts.length == 2){
            prefix = parts[0];
        }
        
        if(prefix != null && prefix.equals("10672")){
            // this is a sharegeo handle, convert it to a datashare handle
            Context context = null;

            try{
                context = new Context();
                TableRow row = DatabaseManager.findByUnique(
                        context,
                        "sharegeo_handle",
                        "original",
                        handle.toString());
                if(row != null){
                    theHandle = (HandleIdentifierProvider.getPrefix() + "/" +
                            row.getIntColumn("handle_id")).getBytes();
                    log.info("sharegeo handle " + handle + " mapped to " +
                            new String(theHandle));
                }
                else{
                    log.warn("Can't find sharegeo handle: " + handle);
                }
            }
            catch(SQLException ex){
                log.error("Problem with SQL: " + ex.getMessage());
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }
            catch(Exception ex){
                log.error(ex);
                throw new HandleException(HandleException.INTERNAL_ERROR);
            }            
            finally{
                try{
                    context.complete();
                }
                catch (SQLException sqle){}
            }
        }
        
        return super.getRawHandleValues(theHandle, indexList, typeList);
    }
}
