package uk.ac.edina.datashare.sword;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.BinaryContentIngester;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.DepositResult;
import org.dspace.sword2.VerboseDescription;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;
import org.swordapp.server.SwordServerException;

import uk.ac.edina.datashare.utils.VirusChecker;

/**
 * DataShare specific binary ingester.
 */
public class BinaryIngester extends BinaryContentIngester{
    //private static Logger LOG = Logger.getLogger(BinaryIngester.class);
    
    @Override
    public DepositResult ingest(
            Context context,
            Deposit deposit,
            DSpaceObject dso,
            VerboseDescription verboseDescription,
            DepositResult result)
            throws DSpaceSwordException, SwordError, SwordAuthException, SwordServerException
    {
        DepositResult dr = null;

        if(!new VirusChecker(deposit.getFile()).isVirusFree()){
            throw new DSpaceSwordException(deposit.getFilename() + " has failed virus check.");
        }
        
        try{
            dr = super.ingest(context, deposit, dso, verboseDescription, result);
            
            // DSpace doesn't set owning collection
            Item item = dr.getItem();
            if(dso instanceof Collection && item.getOwningCollection() == null){
                item.setOwningCollection((Collection)dso);
                item.update();
            }
        }
        catch(SQLException ex){
            throw new DSpaceSwordException(ex);
        }
        catch(AuthorizeException ex){
            throw new DSpaceSwordException(ex);
        }
        
        return dr;
    }
}
