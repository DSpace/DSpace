package org.dspace.identifier;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;

import java.sql.SQLException;


/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 8/30/11
 * Time: 12:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class InternalIdentifierProvider extends IdentifierProvider {

    private static final String SLASH = "/";
    private static final String TOMBSTONE = "tombstone";

    private String[] supportedPrefixes = new String[]{"info:dspace/", "dspace:/"};


    public boolean supports(String identifier)
    {
        for(String prefix : supportedPrefixes){
            if(identifier.startsWith(prefix))
                return true;
        }

        return false;
    }




    public String register(Context context, DSpaceObject item) throws IdentifierException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String mint(Context context, DSpaceObject dso) throws IdentifierException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }


    //identifier = info:dspace/item/10
    public DSpaceObject resolve(Context context, String identifier, String... attributes) throws IdentifierNotFoundException, IdentifierNotResolvableException{

        if(identifier.contains(TOMBSTONE))
            throw new IdentifierNotResolvableException();

        // item/10
        String temp = identifier.substring(identifier.indexOf(SLASH)+1);

        String[] typeAndId = temp.split(SLASH);

        try {
            return DSpaceObject.find(context, Constants.getTypeID(typeAndId[0]), Integer.parseInt(typeAndId[1]));
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find DspaceObject: " + identifier);
        }
    }

    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        //To change body of implemented methods use File | Settings | File Templates.
    }





}
