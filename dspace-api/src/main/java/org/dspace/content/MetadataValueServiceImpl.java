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
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation for the MetadataValue object.
 * This class is responsible for all business logic calls for the MetadataValue object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataValueServiceImpl implements MetadataValueService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataValueServiceImpl.class);

    @Autowired(required = true)
    protected AuthorizeService authorizeService;
    @Autowired(required = true)
    protected MetadataValueDAO metadataValueDAO;
    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;

    protected MetadataValueServiceImpl() {

    }

    @Override
    public MetadataValue create(Context context, DSpaceObject dso, MetadataField metadataField) throws SQLException {
        MetadataValue metadataValue = new MetadataValue();
        metadataValue.setMetadataField(metadataField);
        metadataValue.setDSpaceObject(dso);
        dso.addMetadata(metadataValue);
//An update here isn't needed, this is persited upon the merge of the owning object
//        metadataValueDAO.save(context, metadataValue);
        metadataValue = metadataValueDAO.create(context, metadataValue);
        log.info(LogHelper.getHeader(context, "add_metadatavalue",
                                     "metadata_value_id=" + metadataValue.getID()));

        return metadataValue;
    }

    @Override
    public MetadataValue find(Context context, int valueId) throws IOException, SQLException {
        // Grab row from DB
        return metadataValueDAO.findByID(context, MetadataValue.class, valueId);
    }

    @Override
    public List<MetadataValue> findByField(Context context, MetadataField metadataField)
        throws IOException, SQLException {
        return metadataValueDAO.findByField(context, metadataField);
    }

    @Override
    public Iterator<MetadataValue> findByFieldAndValue(Context context, MetadataField metadataField, String value)
            throws SQLException {
        return metadataValueDAO.findItemValuesByFieldAndValue(context, metadataField, value);
    }

    @Override
    public void update(Context context, MetadataValue metadataValue) throws SQLException {
        metadataValueDAO.save(context, metadataValue);
        log.info(LogHelper.getHeader(context, "update_metadatavalue",
                                      "metadata_value_id=" + metadataValue.getID()));

    }

    @Override
    public void update(Context context, MetadataValue metadataValue, boolean updateLastModified)
        throws SQLException, AuthorizeException {
        if (updateLastModified) {
            authorizeService.authorizeAction(context, metadataValue.getDSpaceObject(), Constants.WRITE);
            DSpaceObjectService<DSpaceObject> dSpaceObjectService = contentServiceFactory
                .getDSpaceObjectService(metadataValue.getDSpaceObject());
            // get the right class for our dspaceobject not the DSpaceObject lazy proxy
            DSpaceObject dso = dSpaceObjectService.find(context, metadataValue.getDSpaceObject().getID());
            dSpaceObjectService.updateLastModified(context, dso);
        }
        update(context, metadataValue);
    }


    @Override
    public void delete(Context context, MetadataValue metadataValue) throws SQLException {
        log.info(LogHelper.getHeader(context, "delete_metadata_value",
                                      " metadata_value_id=" + metadataValue.getID()));
        metadataValueDAO.delete(context, metadataValue);
    }

    @Override
    public Iterator<MetadataValue> findByValueLike(Context context, String value) throws SQLException {
        return metadataValueDAO.findByValueLike(context, value);
    }

    @Override
    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException {
        metadataValueDAO.deleteByMetadataField(context, metadataField);
    }

    @Override
    public MetadataValue getMinimum(Context context, int metadataFieldId)
        throws SQLException {
        return metadataValueDAO.getMinimum(context,
                                           metadataFieldId);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return metadataValueDAO.countRows(context);
    }
}
