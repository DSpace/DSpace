/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.dto;

import java.util.List;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.list.LazyList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ResearchObject;

public class DynamicAnagraficaObjectDTO extends CrisAnagraficaObjectWithTypeDTO
{

    protected final Log log = LogFactory.getLog(getClass());


    public DynamicAnagraficaObjectDTO(ResearchObject ou)
    {
        super(ou);
        this.setTimeStampCreated(ou.getTimeStampInfo().getCreationTime());
        this.setTimeStampModified(ou.getTimeStampInfo()
                .getLastModificationTime());
    }


    /**
     * Decorate list for dynamic binding with spring mvc
     * 
     * @param list
     * @return lazy list temporary
     */
    private List getLazyList(List<String> list)
    {
        log.debug("Decorate list for dynamic binding with spring mvc");
        List lazyList = LazyList.decorate(list,
                FactoryUtils.instantiateFactory(String.class));

        return lazyList;
    }


}
