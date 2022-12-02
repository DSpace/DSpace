/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao.clarin;

import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.core.GenericDAO;

/**
 * Database Access Object interface class for the Clarin License Label object.
 * The implementation of this class is responsible for all database calls for the Clarin License Label object
 * and is autowired by spring This class should only be accessed from a single service and should never be exposed
 * outside the API
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public interface ClarinLicenseLabelDAO extends GenericDAO<ClarinLicenseLabel> {
}
