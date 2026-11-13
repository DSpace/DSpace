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
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class UpdateCrisMetricsInSolrDocScriptConfiguration<T extends UpdateCrisMetricsInSolrDoc>
                                                             extends ScriptConfiguration<T> {

    private static final Logger log = LoggerFactory
            .getLogger(UpdateCrisMetricsInSolrDocScriptConfiguration.class);

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();
            options.addOption("o", "optimize", false,
                "If set, performs solr search optimization after the metrics update. It might take a long time");
            options.getOption("o").setType(boolean.class);
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