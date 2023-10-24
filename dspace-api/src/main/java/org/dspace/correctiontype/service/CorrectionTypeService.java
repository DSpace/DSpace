/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.correctiontype.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.correctiontype.CorrectionType;

/**
 * Service interface class for the CorrectionType object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface CorrectionTypeService {

    public CorrectionType findOne(String id);

    public List<CorrectionType> findAll();

    public List<CorrectionType> findByItem(Context context, Item item) throws AuthorizeException, SQLException;

    public CorrectionType findByTopic(String topic);

}
