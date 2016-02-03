/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.dao;

import java.util.List;

import org.dspace.app.cris.model.jdyna.DynamicTypeNestedObject;

import it.cilea.osd.common.dao.PaginableObjectDao;
import it.cilea.osd.jdyna.dao.TypeDaoSupport;
import it.cilea.osd.jdyna.model.AType;
import it.cilea.osd.jdyna.model.PropertiesDefinition;

public interface DynamicObjectTypeDao<T extends AType<PD>, PD extends PropertiesDefinition> extends TypeDaoSupport<T, PD>, PaginableObjectDao<T, Integer>
{
    public List<DynamicTypeNestedObject> findNestedMaskById(Integer id);
}
