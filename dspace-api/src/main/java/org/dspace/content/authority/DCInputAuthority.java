/*
 * DCInputAuthority.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2009/07/23 05:07:01 $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the DSpace Foundation nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content.authority;

import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.core.SelfNamedPlugin;

/**
 * ChoiceAuthority source that reads the same input-forms which drive
 * configurable submission.
 *
 * Configuration:
 *   This MUST be configured aas a self-named plugin, e.g.:
 *     plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority = \
 *        org.dspace.content.authority.DCInputAuthority
 *
 * It AUTOMATICALLY configures a plugin instance for each <value-pairs>
 * element (within <form-value-pairs>) of the input-forms.xml.  The name
 * of the instance is the "value-pairs-name" attribute, e.g.
 * the element: <value-pairs value-pairs-name="common_types" dc-term="type">
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
            try
            {
                if (dci == null)
                    dci = new DCInputsReader();
            }
            catch (DCInputsReaderException e)
            {
                log.error("Failed reading DCInputs initialization: ",e);
            }
            List<String> names = new ArrayList<String>();
            Iterator pi = dci.getPairsNameIterator();
            while (pi.hasNext())
                names.add((String)pi.next());
            pluginNames = names.toArray(new String[names.size()]);
            log.debug("Got plugin names = "+Arrays.deepToString(pluginNames));
        }
        return pluginNames;
    }

    // once-only load of values and labels
    private void init()
    {
        if (values == null)
        {
            String pname = this.getPluginInstanceName();
            List<String> pairs = (List<String>)dci.getPairs(pname);
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
                log.error("Failed to find any pairs for name="+pname, new IllegalStateException());
        }
    }


    public Choices getMatches(String query, int collection, int start, int limit, String locale)
    {
        init();

        int dflt = -1;
        Choice v[] = new Choice[values.length];
        for (int i = 0; i < values.length; ++i)
        {
            v[i] = new Choice(values[i], values[i], labels[i]);
            if (values[i].equalsIgnoreCase(query))
                dflt = i;
        }
        return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, dflt);
    }

    public Choices getBestMatch(String text, int collection, String locale)
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

    public String getLabel(String key, String locale)
    {
        init();
        return labels[Integer.parseInt(key)];
    }
}
