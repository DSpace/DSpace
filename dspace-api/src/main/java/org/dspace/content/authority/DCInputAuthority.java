/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.core.SelfNamedPlugin;

/**
 * ChoiceAuthority source that reads the same submission-forms which drive
 * configurable submission.
 *
 * Configuration:
 *   This MUST be configured aas a self-named plugin, e.g.:
 *   {@code
 *     plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority = \
 *        org.dspace.content.authority.DCInputAuthority
 *   }
 *
 * It AUTOMATICALLY configures a plugin instance for each {@code <value-pairs>}
 * element (within {@code <form-value-pairs>}) of the submission-forms.xml.  The name
 * of the instance is the "value-pairs-name" attribute, e.g.
 * the element: {@code <value-pairs value-pairs-name="common_types" dc-term="type">}
 * defines a plugin instance "common_types".
 *
 * IMPORTANT NOTE: Since these value-pairs do NOT include authority keys,
 * the choice lists derived from them do not include authority values.
 * So you should not use them as the choice source for authority-controlled
 * fields.
 */
public class DCInputAuthority extends SelfNamedPlugin implements ChoiceAuthority
{
    private static Logger log = Logger.getLogger(DCInputAuthority.class);

    private String values[] = null;
    private String labels[] = null;

    private static DCInputsReader dci = null;
    private static String pluginNames[] = null;

    public DCInputAuthority()
    {
        super();
    }

    public static String[] getPluginNames()
    {
        if (pluginNames == null)
        {
            initPluginNames();
        }
        
        return (String[]) ArrayUtils.clone(pluginNames);
    }

    private static synchronized void initPluginNames()
    {
        if (pluginNames == null)
        {
            try
            {
                if (dci == null)
                {
                    dci = new DCInputsReader();
                }
            }
            catch (DCInputsReaderException e)
            {
                log.error("Failed reading DCInputs initialization: ",e);
            }
            List<String> names = new ArrayList<String>();
            Iterator pi = dci.getPairsNameIterator();
            while (pi.hasNext())
            {
                names.add((String)pi.next());
            }

            pluginNames = names.toArray(new String[names.size()]);
            log.debug("Got plugin names = "+Arrays.deepToString(pluginNames));
        }
    }

    // once-only load of values and labels
    private void init()
    {
        if (values == null)
        {
            String pname = this.getPluginInstanceName();
            List<String> pairs = dci.getPairs(pname);
            if (pairs != null)
            {
                values = new String[pairs.size()/2];
                labels = new String[pairs.size()/2];
                for (int i = 0; i < pairs.size(); i += 2)
                {
                    labels[i/2] = pairs.get(i);
                    values[i/2] = pairs.get(i+1);
                }
                log.debug("Found pairs for name="+pname);
            }
            else
            {
                log.error("Failed to find any pairs for name=" + pname, new IllegalStateException());
            }
        }
    }


    @Override
    public Choices getMatches(String field, String query, Collection collection, int start, int limit, String locale)
    {
        init();

        int dflt = -1;
        Choice v[] = new Choice[values.length];
        for (int i = 0; i < values.length; ++i)
        {
            v[i] = new Choice(values[i], values[i], labels[i]);
            if (values[i].equalsIgnoreCase(query))
            {
                dflt = i;
            }
        }
        return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, dflt);
    }

    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale)
    {
        init();
        for (int i = 0; i < values.length; ++i)
        {
            if (text.equalsIgnoreCase(values[i]))
            {
                Choice v[] = new Choice[1];
                v[0] = new Choice(String.valueOf(i), values[i], labels[i]);
                return new Choices(v, 0, v.length, Choices.CF_UNCERTAIN, false, 0);
            }
        }
        return new Choices(Choices.CF_NOTFOUND);
    }

    @Override
    public String getLabel(String field, String key, String locale)
    {
    	init();
        int pos=-1;
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(key)){
                 pos = i;
                 break;
            }
        }
        if (pos != -1)
            return labels[pos];
        else
            return "UNKNOWN KEY "+key;
    }
}
