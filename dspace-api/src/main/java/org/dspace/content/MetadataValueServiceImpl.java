/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.MetadataValueDAO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Service implementation for the MetadataValue object.
 * This class is responsible for all business logic calls for the MetadataValue object and is autowired by spring.
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class MetadataValueServiceImpl implements MetadataValueService {

    private static final Logger log = Logger.getLogger(MetadataValueServiceImpl.class);

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

        return metadataValue;
    }

    @Override
    public MetadataValue find(Context context, int valueId) throws IOException, SQLException {
        // Grab row from DB
        return metadataValueDAO.findByID(context, MetadataValue.class, valueId);
    }

    @Override
    public List<MetadataValue> findByField(Context context, MetadataField metadataField) throws IOException, SQLException {
        return metadataValueDAO.findByField(context, metadataField);
    }

    @Override
    public void update(Context context, MetadataValue metadataValue) throws SQLException {
        metadataValueDAO.save(context, metadataValue);
        log.info(LogManager.getHeader(context, "update_metadatavalue",
                "metadata_value_id=" + metadataValue.getValueId()));

    }

    @Override
    public void update(Context context, MetadataValue metadataValue, boolean updateLastModified) throws SQLException, AuthorizeException {
        if(updateLastModified){
            authorizeService.authorizeAction(context, metadataValue.getDSpaceObject(), Constants.WRITE);
            contentServiceFactory.getDSpaceObjectService(metadataValue.getDSpaceObject()).updateLastModified(context, metadataValue.getDSpaceObject());
        }
        update(context, metadataValue);
    }


    @Override
    public void delete(Context context, MetadataValue metadataValue) throws SQLException {
        log.info(LogManager.getHeader(context, "delete_metadata_value",
                " metadata_value_id=" + metadataValue.getValueId()));
        metadataValueDAO.delete(context, metadataValue);
    }

    @Override
    public void delete(Context context, DSpaceObject dso) throws SQLException {
        log.info(LogManager.getHeader(context, "delete_metadata_values",
                " dso=" + dso.getID()));
        metadataValueDAO.delete(context, dso);
    }

    @Override
    public List<MetadataValue> findByValueLike(Context context, String value) throws SQLException {
        return metadataValueDAO.findByValueLike(context, value);
    }

    @Override
    public void deleteByMetadataField(Context context, MetadataField metadataField) throws SQLException {
        metadataValueDAO.deleteByMetadataField(context, metadataField);
    }

    @Override
    public MetadataValue getMinimum(Context context, int metadataFieldId)
            throws SQLException
    {
        return metadataValueDAO.getMinimum(context,
                metadataFieldId);
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return metadataValueDAO.countRows(context);
    }
}
