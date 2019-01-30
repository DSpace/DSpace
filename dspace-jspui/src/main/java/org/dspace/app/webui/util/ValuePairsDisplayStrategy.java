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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.PluginManager;

public class ValuePairsDisplayStrategy extends ASimpleDisplayStrategy
{

    private static final Logger log = Logger
            .getLogger(ValuePairsDisplayStrategy.class);

    private Map<String, DCInputsReader> dcInputsReader = new HashMap<>();

    private void init() throws DCInputsReaderException
    {
        if(dcInputsReader.isEmpty()) {
            for (Locale locale : I18nUtil.getSupportedLocales())
            {
                dcInputsReader.put(locale.getLanguage(),
                    new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
            }
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
            Collection collection = Collection.find(obtainContext, colIdx);
            if (collection != null)
            {
                result = getResult(colIdx, field, metadataArray, result,
                        obtainContext, collection);
            }
            else
            {
                Item item = Item.find(obtainContext, itemid);
                collection = item.getParentObject();
                result = getResult(colIdx, field, metadataArray, result,
                        obtainContext, collection);
            }

            // workaround, a sort of fuzzy match search in all valuepairs (possible wrong result due to the same stored value in many valuepairs)
            if (StringUtils.isBlank(result))
            {
                String language = I18nUtil.getSupportedLocale(obtainContext.getCurrentLocale()).getLanguage();
                Map<String, List<String>> mappedValuePairs = dcInputsReader.get(language)
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

        }
        catch (SQLException | DCInputsReaderException e)
        {
            throw new JspException(e);
        }
        return result;
    }

    private String getResult(int colIdx, String field,
            Metadatum[] metadataArray, String result, Context obtainContext,
            Collection collection) throws DCInputsReaderException
    {
        String language = I18nUtil.getSupportedLocale(obtainContext.getCurrentLocale()).getLanguage();
        DCInputSet dcInputSet = dcInputsReader.get(language)
                .getInputs(collection.getHandle());
        for (int i = 0; i < dcInputSet.getNumberPages(); i++)
        {
            DCInput[] dcInput = dcInputSet.getPageRows(i, false, false);
            for (DCInput myInput : dcInput)
            {
                String key = myInput.getPairsType();
                if (StringUtils.isNotBlank(key))
                {
                    String inputField = myInput.getSchema() + "."
                            + myInput.getElement();
                    if (StringUtils.isNotBlank(myInput.getQualifier()))
                    {
                        inputField += "." + myInput.getQualifier();
                    }

                    if (inputField.equals(field))
                    {
                        ChoiceAuthority choice = (ChoiceAuthority) PluginManager
                                .getNamedPlugin(ChoiceAuthority.class, key);

                        int ii = 0;
                        for (Metadatum r : metadataArray)
                        {
                            if (ii > 0)
                            {
                                result += " ";
                            }
                            Choices choices = choice.getBestMatch(field,
                                    r.value, colIdx, obtainContext
                                            .getCurrentLocale().toString());
                            if (choices != null)
                            {
                                int iii = 0;
                                for (Choice ch : choices.values)
                                {
                                    result += ch.label;
                                    if (iii > 0)
                                    {
                                        result += " ";
                                    }
                                    iii++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

}
