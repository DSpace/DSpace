/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metrics;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ScriptConfiguration} for the {@link UpdateCrisMetricsWithExternalSource}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateCrisMetricsWithExternalSourceScriptConfiguration<T extends UpdateCrisMetricsWithExternalSource>
       extends ScriptConfiguration<T> {

    private static final Logger log = LoggerFactory
                                      .getLogger(UpdateCrisMetricsWithExternalSourceScriptConfiguration.class);

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("s", "service", true,
                "the name of the external service to use: scopus, wos, scopus-person, wos-person");
            options.getOption("s").setType(String.class);
            options.getOption("s").setRequired(true);

            options.addOption("p", "param", true, "the name of a specific metric to retrieve");
            options.getOption("p").setType(String.class);
            options.getOption("p").setRequired(false);

            options.addOption("l", "limit", true, "the max number of item to be updated. If no limit is provided, "
                + "the default one will be used");
            options.getOption("l").setType(Integer.class);
            options.getOption("l").setRequired(false);

            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}