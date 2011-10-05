package org.dspace.identifier;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 9/1/11
 * Time: 10:37 AM
 * To change this template use File | Settings | File Templates.
 */
public interface IdentifierService {

    void reserve(Context context, Item item) throws AuthorizeException, SQLException, IdentifierException;

    String register(Context context, Item item) throws AuthorizeException, SQLException, IdentifierException;

    DSpaceObject resolve(Context context, String identifier) throws IdentifierNotFoundException, IdentifierNotResolvableException;

    void delete(Context context, Item item) throws AuthorizeException, SQLException, IdentifierException;
}
