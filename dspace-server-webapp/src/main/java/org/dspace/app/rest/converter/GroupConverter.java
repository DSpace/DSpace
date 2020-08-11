/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.eperson.Group;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the Group in the DSpace API data model
 * and the REST data model
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class GroupConverter extends DSpaceObjectConverter<Group, GroupRest> {

    @Override
    public GroupRest convert(Group obj, Projection projection) {
        GroupRest epersongroup = super.convert(obj, projection);
        epersongroup.setPermanent(obj.isPermanent());
        return epersongroup;
    }

    @Override
    protected GroupRest newInstance() {
        return new GroupRest();
    }

    @Override
    public Class<Group> getModelClass() {
        return Group.class;
    }

}
