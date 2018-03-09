/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.dao;

import org.dspace.content.BitstreamFormat;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

import java.sql.SQLException;
import java.util.List;

/**
 * Database Access Object interface class for the BitstreamFormat object.
 * The implementation of this class is responsible for all database calls for the BitstreamFormat object and is autowired by spring
 * This class should only be accessed from a single service and should never be exposed outside of the API
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BitstreamFormatDAO extends GenericDAO<BitstreamFormat>
{

    public BitstreamFormat findByMIMEType(Context context, String mimeType, boolean includeInternal) throws SQLException;

    public BitstreamFormat findByShortDescription(Context context, String desc) throws SQLException;

    public int updateRemovedBitstreamFormat(Context context, BitstreamFormat deletedBitstreamFormat, BitstreamFormat newBitstreamFormat) throws SQLException;

    public List<BitstreamFormat> findNonInternal(Context context) throws SQLException;

    public List<BitstreamFormat> findByFileExtension(Context context, String extension) throws SQLException;
}
