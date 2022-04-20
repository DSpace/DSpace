/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.EtdUnitDAO;
import org.dspace.content.service.EtdUnitService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.event.Event;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the EtdUnit object.
 * This class is responsible for all business logic calls for the EtdUnit object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author mohideen at umd.edu
 */
public class EtdUnitServiceImpl extends DSpaceObjectServiceImpl<EtdUnit> implements EtdUnitService {

    /** log4j category */
    private static Logger log = Logger.getLogger(EtdUnitServiceImpl.class);

    @Autowired(required = true)
    protected EtdUnitDAO etdunitDAO;
    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    protected EtdUnitServiceImpl()
    {
        super();

    }

    @Override
    public EtdUnit create(Context context) throws SQLException, AuthorizeException {
        // Authorize
        canEdit(context);

        EtdUnit newEtdunit = etdunitDAO.create(context, new EtdUnit());

        etdunitDAO.save(context, newEtdunit);

        context.addEvent(new Event(Event.CREATE, Constants.ETDUNIT, newEtdunit.getID(), newEtdunit.getName()));

        log.info(LogHelper.getHeader(context, "create_etdunit",
                "etdunit_id=" + newEtdunit.getID()));

        return newEtdunit;
    }

    @Override
    public EtdUnit find(Context context, UUID id) throws SQLException {
        return etdunitDAO.findByID(context, EtdUnit.class, id);
    }

    @Override
    public EtdUnit findByName(Context context, String name) throws SQLException {
      return etdunitDAO.findByName(context, name);
    }

    @Override
    public List<EtdUnit> findAll(Context context) throws SQLException {
        return etdunitDAO.findAllSortedByName(context);
    }

    @Override
    public List<EtdUnit> findAllByCollection(Context context, Collection collection) throws SQLException {
      return etdunitDAO.findAllByCollection(context, collection);
    }

    @Override
    public List<EtdUnit> search(Context context, String query) throws SQLException {
      return search(context, query, -1, -1);
    }

    @Override
    public List<EtdUnit> search(Context context, String query, int offset, int limit) throws SQLException {
      return etdunitDAO.searchByName(context, query, offset, limit);
    }

    @Override
    public int searchResultCount(Context context, String query) throws SQLException {
      return etdunitDAO.searchByNameResultCount(context, query);
    }

    
    @Override
    public void update(Context context, EtdUnit etdunit) throws SQLException, AuthorizeException {
        // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "update_etdunit",
                "etdunit_id=" + etdunit.getID()));

        super.update(context, etdunit);

        etdunitDAO.save(context, etdunit);
        if (etdunit.isModified())
        {
            context.addEvent(new Event(Event.MODIFY, Constants.ETDUNIT, etdunit.getID(), etdunit.getName()));
            etdunit.clearModified();
        }
        etdunit.clearDetails();
    }

   
    @Override
    public List<Collection> getAllCollections(Context context, EtdUnit etdunit) throws SQLException {
        return etdunit.getCollections();
    }
    
    @Override
    public void addCollection(Context context, EtdUnit etdunit, Collection collection) throws SQLException, AuthorizeException {
         // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "add_collection",
                "etdunit_id=" + etdunit.getID() + ",collection_id=" + collection.getID()));

        etdunit.addCollection(collection);
        
        context.addEvent(new Event(Event.ADD, Constants.ETDUNIT, etdunit.getID(),
        Constants.COLLECTION, collection.getID(), etdunit.getName()));
    }


    @Override
    public void removeCollection(Context context, EtdUnit etdunit, Collection collection) throws SQLException, AuthorizeException {
         // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "add_collection",
                "etdunit_id=" + etdunit.getID() + ",collection_id=" + collection.getID()));

        etdunit.removeCollection(collection);
        
        context.addEvent(new Event(Event.REMOVE, Constants.ETDUNIT, etdunit.getID(),
        Constants.COLLECTION, collection.getID(), etdunit.getName()));
    }

    @Override
    public boolean isMember(EtdUnit etdunit, Collection collection) {
      return etdunit.isMember(collection);
    }

   
    @Override
    public void delete(Context context, EtdUnit etdunit) throws SQLException, AuthorizeException, IOException {
        // Authorize
        canEdit(context);

        log.info(LogHelper.getHeader(context, "delete_etdunit",
                "etdunit_id=" + etdunit.getID()));

        UUID removedId = etdunit.getID();

        // Remove all collection references to this etdunit.
        for (Collection collection : etdunit.getCollections()) {
            etdunit.removeCollection(collection);
        }

        etdunitDAO.delete(context, etdunit);

        context.addEvent(new Event(Event.REMOVE, Constants.ETDUNIT, removedId, etdunit.getName()));
    }

    @Override
    public int getSupportsTypeConstant() {
        return Constants.ETDUNIT;
    }



    @Override
    public boolean canEditBoolean(Context context) throws SQLException {
        try
        {
            canEdit(context);

            return true;
        }
        catch (AuthorizeException e)
        {
            return false;
        }
    }

    @Override
    public void canEdit(Context context) throws AuthorizeException, SQLException {
        // Check authorisation
        if (!(authorizeService.isAdmin(context)))
        {
            throw new AuthorizeException("Only administrators can create or modify etdunits and its associations");
        }
    }

    @Override
    public void updateLastModified(Context context, EtdUnit etdunit) {
        //Also fire a modified event since the etdunit HAS been modified
        context.addEvent(new Event(Event.MODIFY, Constants.ETDUNIT,
                etdunit.getID(), etdunit.getName()));

    }

    @Override
    public EtdUnit findByIdOrLegacyId(Context context, String id) throws SQLException {
        if(StringUtils.isNumeric(id))
        {
            return findByLegacyId(context, Integer.parseInt(id));
        }
        else
        {
            return find(context, UUID.fromString(id));
        }
    }

    @Override
    public EtdUnit findByLegacyId(Context context, int id) throws SQLException {
        return etdunitDAO.findByLegacyId(context, id, EtdUnit.class);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return etdunitDAO.countRows(context);
    }
}
