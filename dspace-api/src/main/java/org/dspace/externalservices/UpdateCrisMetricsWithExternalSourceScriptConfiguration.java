/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.externalservices;
import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAllowedToExecute(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("s", "service", true,
                "the name of the external service to use, i.e. SCOPUS");
            options.getOption("s").setType(String.class);
            options.getOption("s").setRequired(true);

            options.addOption("p", "param", true, "the param of the MetricType to performe update");
            options.getOption("p").setType(String.class);
            //options.getOption("i").setRequired(false);
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