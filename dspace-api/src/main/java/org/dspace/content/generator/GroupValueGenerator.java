/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.generator;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

public class GroupValueGenerator implements TemplateValueGenerator
{

    private static Logger log = Logger.getLogger(GroupValueGenerator.class);

    @Override
    public Metadatum[] generator(Context context, Item targetItem, Item templateItem,
            Metadatum metadatum, String extraParams)
    {
        String[] params = StringUtils.split(extraParams, "\\.");
        String prefix = params[0];
        String suffix = "";
        if (params.length > 1)
        {
            suffix = params[1];
        }
        String value = prefix;
        try
        {
            if (StringUtils.startsWith(prefix, "community"))
            {
                String metadata = prefix.substring("community[".length(),
                        prefix.length() - 1);

                value = templateItem.getParentObject().getParentObject()
                        .getMetadata(metadata);

            }
            else if (StringUtils.startsWith(prefix, "collection"))
            {
                String metadata = prefix.substring("collection[".length(),
                        prefix.length() - 1);
                value = templateItem.getParentObject().getMetadata(metadata);
            }
            else if (StringUtils.startsWith(prefix, "item"))
            {
                value = targetItem.getMetadata(prefix.replace("_", "."));
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage());
        }

        if (StringUtils.isNotBlank(suffix))
        {
            value = value + "-" + suffix;
        }

        Metadatum[] m = new Metadatum[1];
        m[0] = metadatum;
        Group group = null;
        try
        {
            group = Group.findByName(context, value);
        }
        catch (SQLException e)
        {
            log.error(e.getMessage());
        }
        String result = "";
        if(group!=null) {
            result = "" + group.getID();
        }        
        metadatum.value = result;
        return m;
    }
}
