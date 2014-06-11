/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import it.cilea.osd.common.dao.PaginableObjectDao;
import it.cilea.osd.jdyna.dao.TypeDaoSupport;
import it.cilea.osd.jdyna.model.ANestedPropertiesDefinition;
import it.cilea.osd.jdyna.model.ATypeNestedObject;

public interface ProjectTypeNestedObjectDao<NTP extends ANestedPropertiesDefinition, TTP extends ATypeNestedObject<NTP>> extends TypeDaoSupport<TTP, NTP>, PaginableObjectDao<TTP, Integer>
{

}
