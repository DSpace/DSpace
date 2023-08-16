/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn;

import java.sql.SQLException;

import org.dspace.app.ldn.service.LDNMessageService;
import org.dspace.core.Context;

/**
 * Implementation of {@link LDNMessageService}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class LDNMessageServiceImpl implements LDNMessageService {

    protected LDNMessageServiceImpl() {

    }

    @Override
    public void create(Context context, String id) throws SQLException {

    }

}
