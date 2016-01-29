package it.cineca.surplus.ir.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.core.Context;

/**
 * Define interface to manage list of DSpaceObject. 
 * 
 * @author pascarelli
 *
 */
public interface StreamGenericDisseminationCrosswalk extends StreamDisseminationCrosswalk
{

    public void disseminate(Context context, List<DSpaceObject> dso, OutputStream out)
    throws CrosswalkException, IOException, SQLException, AuthorizeException;
    
}
