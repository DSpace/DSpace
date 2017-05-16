/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.Util;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.PluginManager;

public class ValuePairsDisplayStrategy extends ASimpleDisplayStrategy
{

    private static final Logger log = Logger
            .getLogger(ValuePairsDisplayStrategy.class);

    private DCInputsReader dcInputsReader;

    private void init() throws DCInputsReaderException
    {
        if (dcInputsReader == null)
        {
            dcInputsReader = new DCInputsReader();
        }
    }

    @Override
    public String getMetadataDisplay(HttpServletRequest hrq, int limit,
            boolean viewFull, String browseType, int colIdx, int itemid,
            String field, Metadatum[] metadataArray, boolean disableCrossLinks,
            boolean emph) throws JspException
    {
        try
        {
            init();
        }
        catch (DCInputsReaderException e)
        {
            log.error(e.getMessage(), e);
        }

        String result = "";
        try
        {
            Context obtainContext = UIUtil.obtainContext(hrq);
            Map<String, List<String>> mappedValuePairs = dcInputsReader
                    .getMappedValuePairs();
            String pairsname = "";
            if (mappedValuePairs != null)
            {
                external : for (String key : mappedValuePairs.keySet())
                {
                    List<String> values = mappedValuePairs.get(key);
                    internal : for(String vv : values) {
                        if(StringUtils.equals(field, vv)) {
                            pairsname = key;
                            break external;
                        }
                    }
                }
            }
            
            ChoiceAuthority choice = (ChoiceAuthority) PluginManager
                    .getNamedPlugin(ChoiceAuthority.class,
                            pairsname);
            
            int ii = 0;
            for (Metadatum r : metadataArray)
            {
                if (ii > 0)
                {
                    result += " ";
                }
                result += choice.getLabel(field, r.value, obtainContext.getCurrentLocale().toString());
            }
        }
        catch (SQLException e)
        {
            throw new JspException(e);
        }
        return result;
    }

}
