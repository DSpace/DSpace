/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.authority.Choices;
import org.dspace.content.dao.UmrestrictedDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.event.Event;
import org.dspace.harvest.HarvestedItem;
import org.dspace.harvest.service.HarvestedItemService;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.ConfigurationService;
import org.dspace.content.service.UmrestrictedService;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the Item object.
 * This class is responsible for all business logic calls for the Item object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class UmrestrictedServiceImpl  implements UmrestrictedService {

    /**
     * log4j category
     */
    //private static Logger log = Logger.getLogger(Umrestricted.class);

    @Autowired(required = true)
    protected UmrestrictedDAO umrestrictedDAO;

    protected UmrestrictedServiceImpl()
    {
        super();
    }

    //@Override
    //public void update(org.dspace.core.Context context, org.dspace.content.Umrestricted umrestricted) throws SQLException, AuthorizeException {
    //}	

    //@Override
    //public void delete(Context context, Umrestricted umrestricted) throws SQLException, AuthorizeException, IOException {
        //authorizeService.authorizeAction(context, umrestricted, Constants.DELETE);
        //rawDelete(context,  umrestricted);
    //}


    //@Override
    //public int getSupportsTypeConstant() {
        //return Constants.ITEM;
    //    return 10;
    //}i

    @Override
    public void createUmrestricted(Context context, String item_id, String date) throws SQLException {
        umrestrictedDAO.createUmrestricted(context, item_id, date);
    }

    @Override
    public void deleteUmrestricted(Context context, String item_id) throws SQLException {
        umrestrictedDAO.deleteUmrestricted(context, item_id);
    }


    @Override
    public Iterator<Umrestricted> findAllUmrestricted(Context context ) throws SQLException {
        return umrestrictedDAO.findAllUmrestricted(context );
    }

    @Override
    public Iterator<Umrestricted> findAllByItemIdUmrestricted(Context context, String item_id ) throws SQLException {
        return umrestrictedDAO.findAllByItemIdUmrestricted(context, item_id );
    }

    @Override
    public Iterator<Umrestricted> findAllByDateUmrestricted(Context context, String date ) throws SQLException {
        return umrestrictedDAO.findAllByDateUmrestricted(context, date );
    }

    //@Override
    //public void updateLastModified(Context context, Umrestricted umrestricted) throws SQLException, AuthorizeException {
        //item.setLastModified(new Date());
        //update(context, item);
        //Also fire a modified event since the item HAS been modified
        //context.addEvent(new Event(Event.MODIFY, Constants.ITEM, item.getID(), null, getIdentifiers(context, item)));
    //    return;
    //}

}
