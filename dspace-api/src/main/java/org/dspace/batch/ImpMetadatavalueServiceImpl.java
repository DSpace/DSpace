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
import org.dspace.batch.dao.ImpMetadatavalueDAO;
import org.dspace.batch.service.ImpMetadatavalueService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/***
 * Service implementation used to access ImpMetadatavalue entities.
 * 
 * @See {@link org.dspace.batch.ImpMetadatavalue}
 * 
 * @author fcadili (francesco.cadili at 4science.it)
 *
 */
public class ImpMetadatavalueServiceImpl implements ImpMetadatavalueService {
    /**
     * log4j category
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(ImpMetadatavalueServiceImpl.class);

    @Autowired(required = true)
    private ImpMetadatavalueDAO impMetadatavalueDAO;

    @Override
    public ImpMetadatavalue create(Context context, ImpMetadatavalue impMetadatavalue) throws SQLException {
        impMetadatavalue = impMetadatavalueDAO.create(context, impMetadatavalue);
        return impMetadatavalue;
    }

    @Override
    public void setMetadata(ImpMetadatavalue impMetadatavalue, String schema, String element, String qualifier,
            String language, String value) {
        impMetadatavalue.setImpSchema(schema);
        impMetadatavalue.setImpElement(element);
        impMetadatavalue.setImpQualifier(qualifier);
        impMetadatavalue.setTextLang(language);
        impMetadatavalue.setImpValue(value);
    }

    @Override
    public List<ImpMetadatavalue> searchByImpRecordId(Context context, ImpRecord impRecord) throws SQLException {
        return impMetadatavalueDAO.searchByImpRecord(context, impRecord);
    }

    @Override
    public void update(Context context, ImpMetadatavalue impMetadatavalue) throws SQLException {
        impMetadatavalueDAO.save(context, impMetadatavalue);
    }

    @Override
    public void delete(Context context, ImpMetadatavalue impMetadatavalue) throws SQLException {
        impMetadatavalueDAO.delete(context, impMetadatavalue);
    }
}