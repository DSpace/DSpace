package uk.ac.edina.datashare.sword;

import java.sql.SQLException;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.core.Context;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.DepositResult;
import org.dspace.sword2.SimpleDCEntryIngester;
import org.dspace.sword2.VerboseDescription;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

public class MetadataIngester extends SimpleDCEntryIngester{
    //private static Logger LOG = Logger.getLogger(MetadataIngester.class);
    
    @Override
    public DepositResult ingest(
            Context context,
            Deposit deposit,
            DSpaceObject dso,
            VerboseDescription verboseDescription,
            DepositResult result,
            boolean replace)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        DepositResult ds = super.ingest(context, deposit, dso, verboseDescription, result, replace);
        
        try{
            Item item = ds.getItem();
            SwordUtil.isMetadataValid(context, item);
            
            // clear description created by sword
            item.clearMetadata("dc", "description", null, Item.ANY);
        }
        catch(PackageValidationException ex){
            throw new DSpaceSwordException(ex.getMessage());
        }
        catch(SQLException ex){
            throw new RuntimeException(ex);
        }

        return ds;
    }    
}
