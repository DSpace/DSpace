/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.dao;

import org.dspace.app.ldn.LDNMessage;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the LDNMessage object.
 *
 * The implementation of this class is responsible for all database calls for the LDNMessage object
 * and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface LDNMessageDao extends GenericDAO<LDNMessage> {

}
