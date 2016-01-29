/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;

import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.jdyna.OUNestedObject;
import org.dspace.app.cris.model.jdyna.OUNestedPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUNestedProperty;
import org.dspace.app.cris.model.jdyna.OUPropertiesDefinition;
import org.dspace.app.cris.model.jdyna.OUProperty;
import org.dspace.app.cris.model.jdyna.OUTypeNestedObject;

public class CrisOURefDisplayStrategy extends ACrisRefDisplayStrategy<OUProperty, OUPropertiesDefinition, OUNestedProperty, OUNestedPropertiesDefinition, OUNestedObject, OUTypeNestedObject>
{

    @Override
    public Class<OrganizationUnit> getClassName()
    {
        return OrganizationUnit.class;
    }

    @Override
    public String getPublicPath()
    {
        return "ou";
    }


}
