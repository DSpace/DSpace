/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.SelfNamedPlugin;

/**
 * ChoiceAuthority source that reads the same submission-forms which drive
 * configurable submission.
 *
 * Configuration:
 * This MUST be configured aas a self-named plugin, e.g.:
 * {@code
 * plugin.selfnamed.org.dspace.content.authority.ChoiceAuthority = \
 * org.dspace.content.authority.DCInputAuthority
 * }
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
public class DCInputAuthority extends SelfNamedPlugin implements ChoiceAuthority {
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(DCInputAuthority.class);

    private Map<String, String[]> values = null;
    private Map<String, String[]> labels = null;

    private static Map<Locale, DCInputsReader> dcis = null;
    private static String pluginNames[] = null;

    public DCInputAuthority() {
        super();
    }

    public static void reset() {
        pluginNames = null;
    }

    public static String[] getPluginNames() {
        if (pluginNames == null) {
            initPluginNames();
        }

        return (String[]) ArrayUtils.clone(pluginNames);
    }

    private static synchronized void initPluginNames() {
        Locale[] locales = I18nUtil.getSupportedLocales();
        if (pluginNames == null) {
            try {
                dcis = new HashMap<Locale, DCInputsReader>();
                for (Locale locale : locales) {
                    dcis.put(locale, new DCInputsReader(I18nUtil.getInputFormsFileName(locale)));
                }
            } catch (DCInputsReaderException e) {
                log.error("Failed reading DCInputs initialization: ", e);
            }
            List<String> names = new ArrayList<String>();
            Iterator pi = dcis.get(locales[0]).getPairsNameIterator();
            while (pi.hasNext()) {
                names.add((String) pi.next());
            }

            pluginNames = names.toArray(new String[names.size()]);
            log.debug("Got plugin names = " + Arrays.deepToString(pluginNames));
        }
    }

    // once-only load of values and labels
    private void init() {
        if (values == null) {
            values = new HashMap<String, String[]>();
            labels = new HashMap<String, String[]>();
            String pname = this.getPluginInstanceName();
            for (Locale l : dcis.keySet()) {
                DCInputsReader dci = dcis.get(l);
                List<String> pairs = dci.getPairs(pname);
                if (pairs != null) {
                    String[] valuesLocale = new String[pairs.size() / 2];
                    String[]labelsLocale = new String[pairs.size() / 2];
                    for (int i = 0; i < pairs.size(); i += 2) {
                        labelsLocale[i / 2] = pairs.get(i);
                        valuesLocale[i / 2] = pairs.get(i + 1);
                    }
                    values.put(l.getLanguage(), valuesLocale);
                    labels.put(l.getLanguage(), labelsLocale);
                    log.debug("Found pairs for name=" + pname + ",locale=" + l);
                } else {
                    log.error("Failed to find any pairs for name=" + pname, new IllegalStateException());
                }
            }

        }
    }


    @Override
    public Choices getMatches(String field, String query, Collection collection, int start, int limit, String locale) {
        init();
        Locale currentLocale = I18nUtil.getSupportedLocale(locale);
        String[] valuesLocale = values.get(currentLocale.getLanguage());
        String[] labelsLocale = labels.get(currentLocale.getLanguage());
        int dflt = -1;
        Choice v[] = new Choice[valuesLocale.length];
        for (int i = 0; i < valuesLocale.length; ++i) {
            v[i] = new Choice(valuesLocale[i], valuesLocale[i], labelsLocale[i]);
            if (valuesLocale[i].equalsIgnoreCase(query)) {
                dflt = i;
            }
        }
        return new Choices(v, 0, v.length, Choices.CF_AMBIGUOUS, false, dflt);
    }

    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale) {
        init();

        // Get default if locale is empty
        if (StringUtils.isBlank(locale)) {
            locale = getDefaultLocale();
        }

        String[] valuesLocale = values.get(locale);
        String[] labelsLocale = labels.get(locale);
        for (int i = 0; i < valuesLocale.length; ++i) {
            if (text.equalsIgnoreCase(valuesLocale[i])) {
                Choice v[] = new Choice[1];
                v[0] = new Choice(String.valueOf(i), valuesLocale[i], labelsLocale[i]);
                return new Choices(v, 0, v.length, Choices.CF_UNCERTAIN, false, 0);
            }
        }
        return new Choices(Choices.CF_NOTFOUND);
    }

    @Override
    public String getLabel(String field, String key, String locale) {
        init();

        // Get default if locale is empty
        if (StringUtils.isBlank(locale)) {
            locale = getDefaultLocale();
        }

        String[] valuesLocale = values.get(locale);
        String[] labelsLocale = labels.get(locale);
        int pos = -1;
        for (int i = 0; i < valuesLocale.length; i++) {
            if (valuesLocale[i].equals(key)) {
                pos = i;
                break;
            }
        }
        if (pos != -1) {
            return labelsLocale[pos];
        } else {
            return "UNKNOWN KEY " + key;
        }
    }

    protected String getDefaultLocale() {
        Context context = new Context();
        return context.getCurrentLocale().getLanguage();
    }
}
