/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Service interface class for the Item object.
 * The implementation of this class is responsible for all business logic calls for the Item object and is autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface UmrestrictedService
{

    public void createUmrestricted(Context context, String item_id, String date) throws SQLException;

    public void deleteUmrestricted(Context context, String item_id) throws SQLException;

    public Iterator<Umrestricted> findAllUmrestricted(Context context ) throws SQLException;

    public Iterator<Umrestricted> findAllByItemIdUmrestricted(Context context, String item_id ) throws SQLException;

    public Iterator<Umrestricted> findAllByDateUmrestricted(Context context, String date ) throws SQLException;


}
