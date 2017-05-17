/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
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
            List<String> pairsnames = new ArrayList<String>();
            if (mappedValuePairs != null)
            {
                for (String key : mappedValuePairs.keySet())
                {
                    List<String> values = mappedValuePairs.get(key);
                    for (String vv : values)
                    {
                        if (StringUtils.equals(field, vv))
                        {
                            pairsnames.add(key);
                        }
                    }
                }
            }

            for (String pairsname : pairsnames)
            {
                ChoiceAuthority choice = (ChoiceAuthority) PluginManager
                        .getNamedPlugin(ChoiceAuthority.class, pairsname);

                int ii = 0;                
                for (Metadatum r : metadataArray)
                {
                    if (ii > 0)
                    {
                        result += " ";
                    }
                    Choices choices = choice.getBestMatch(field, r.value,
                            colIdx,
                            obtainContext.getCurrentLocale().toString());
                    if (choices != null)
                    {
                        for (Choice ch : choices.values)
                        {
                            result += ch.label;
                        }
                    }
                }
            }
        }
        catch (SQLException e)
        {
            throw new JspException(e);
        }
        return result;
    }

}
