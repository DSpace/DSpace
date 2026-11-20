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
 * {@link ScriptConfiguration} for the {@link StoreViewDownloadsCrisMetrics}.
 *
 * @author alba aliu
 */

public class StoreViewDownloadsCrisMetricsScriptConfiguration<T extends StoreViewDownloadsCrisMetrics>
        extends ScriptConfiguration<T> {

    private static final Logger log = LoggerFactory
            .getLogger(StoreViewDownloadsCrisMetricsScriptConfiguration.class);

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {

            super.options = new Options();
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
