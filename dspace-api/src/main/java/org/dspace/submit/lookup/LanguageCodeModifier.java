/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.submit.lookup;

import gr.ekt.bte.core.AbstractModifier;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;
import org.springframework.beans.factory.InitializingBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

/**
 * Modifier to covert ISO 639-2 alpha-3 code to ISO 639-1 alpha-2 code
 *
 * @author Keiji Suzuki
 */
public class LanguageCodeModifier extends AbstractModifier implements InitializingBean
{
    protected static Map<String, String> lang3to2 = null;

    @Override
    public void afterPropertiesSet() throws Exception
    {
        lang3to2 = new HashMap<String, String>();
        for (Locale locale : Locale.getAvailableLocales())
        {
            try
            {
                lang3to2.put(locale.getISO3Language(), locale.getLanguage());
            }
            catch (MissingResourceException e)
            {
                continue;
            }
        }
    } 

    public LanguageCodeModifier()
    {
        super("LanguageCodeModifier");
    }

    @Override
    public Record modify(MutableRecord rec)
    {
        List<Value> old_values = rec.getValues("language");
        if (old_values == null || old_values.size() == 0)
        {
            return rec;
        }

        List<Value> new_values = new ArrayList<Value>();
        for (Value value : old_values)
        {
            String lang3 = value.getAsString();
            String lang2 = lang3.length() == 3 ? getLang2(lang3) : lang3;
            new_values.add(new StringValue(lang2));
        }

        rec.updateField("language", new_values);

        return rec;
    }

    /**
     * Covert ISO 639-2 alpha-3 code to ISO 639-1 alpha-2 code
     *
     * @param lang3
     *            ISO 639-1 alpha-3 language code
     * 
     * @return String ISO 639-1 alpha-2 language code ("other" if code is not alpha-2)
     * 
     */
    protected String getLang2(String lang3)
    {
        return lang3to2.containsKey(lang3) ? lang3to2.get(lang3) : "other"; 
    }

}
