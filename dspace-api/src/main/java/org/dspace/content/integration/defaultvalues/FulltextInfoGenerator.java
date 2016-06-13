/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.defaultvalues;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.I18nUtil;

public class FulltextInfoGenerator implements EnhancedValuesGenerator
{

    @Override
    public DefaultValuesBean generateValues(Item item, String schema,
            String element, String qualifier, String value)
    {
        DefaultValuesBean result = new DefaultValuesBean();
        result.setLanguage("en");
        result.setMetadataSchema(schema);
        result.setMetadataElement(element);
        result.setMetadataQualifier(qualifier);

        String values = I18nUtil
                .getMessage("defaultvalue.fulltextdescription.nofulltext");
        Bundle[] bnds;
        try
        {
            bnds = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
        external: for (Bundle bnd : bnds)
        {
            internal: for (Bitstream b : bnd.getBitstreams())
            {
                // stop on the first bitstream
                values = I18nUtil.getMessage(
                        "defaultvalue.fulltextdescription.fulltext");
                break external;
            }
        }

        result.setValues(values);
        return result;
    }

}
