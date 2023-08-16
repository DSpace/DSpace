/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import java.sql.SQLException;

import org.dspace.core.Context;

/**
 * Service interface class for the {@link LDNMessage} object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface LDNMessageService {

    public void create(Context context, String id) throws SQLException;

}
