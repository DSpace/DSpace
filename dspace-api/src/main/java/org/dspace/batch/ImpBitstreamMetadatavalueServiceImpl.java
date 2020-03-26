/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.batch;

import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.dspace.batch.dao.ImpBitstreamMetadatavalueDAO;
import org.dspace.batch.service.ImpBitstreamMetadatavalueService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * Service implementation used to access ImpBitstreamMetadatavalue entities.
 * 
 * @See {@link org.dspace.batch.ImpBitstreamMetadatavalue}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class ImpBitstreamMetadatavalueServiceImpl implements ImpBitstreamMetadatavalueService {
    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ImpBitstreamMetadatavalueServiceImpl.class);

    @Autowired(required = true)
    private ImpBitstreamMetadatavalueDAO impBitstreamMetadatavalueDAO;

    @Override
    public ImpBitstreamMetadatavalue create(Context context, ImpBitstreamMetadatavalue impBitstreamMetadatavalue)
            throws SQLException {
        impBitstreamMetadatavalue = impBitstreamMetadatavalueDAO.create(context, impBitstreamMetadatavalue);
        return impBitstreamMetadatavalue;
    }

    @Override
    public void setMetadata(ImpBitstreamMetadatavalue impBitstreamMetadatavalue, String schema, String element,
            String qualifier, String language, String value) {
        impBitstreamMetadatavalue.setImpSchema(schema);
        impBitstreamMetadatavalue.setImpElement(element);
        impBitstreamMetadatavalue.setImpQualifier(qualifier);
        impBitstreamMetadatavalue.setTextLang(language);
        impBitstreamMetadatavalue.setImpValue(value);
    }

    @Override
    public List<ImpBitstreamMetadatavalue> searchByImpBitstream(Context context, ImpBitstream impBitstream)
            throws SQLException {
        return impBitstreamMetadatavalueDAO.searchByImpBitstream(context, impBitstream);
    }

    @Override
    public void update(Context context, ImpBitstreamMetadatavalue impBitstreamMetadatavalue) throws SQLException {
        impBitstreamMetadatavalueDAO.save(context, impBitstreamMetadatavalue);
    }

    @Override
    public void delete(Context context, ImpBitstreamMetadatavalue impBitstreamMetadatavalue) throws SQLException {
        impBitstreamMetadatavalueDAO.delete(context, impBitstreamMetadatavalue);
    }
}