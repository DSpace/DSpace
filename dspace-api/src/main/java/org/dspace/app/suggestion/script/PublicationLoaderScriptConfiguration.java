/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.script;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.cli.Options;
import org.dspace.app.suggestion.runnable.PublicationLoaderRunnable;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Configuration class for the PublicationLoader script.
 * This class extends {@link ScriptConfiguration} to provide configuration settings
 * and execution permissions for the {@link PublicationLoaderRunnable} script.
 *
 * @param <T> The specific type of {@link PublicationLoaderRunnable} that this configuration supports.
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class PublicationLoaderScriptConfiguration<T extends PublicationLoaderRunnable>
    extends ScriptConfiguration<T> {

    @Autowired
    private AuthorizeService authorizeService;

    private Class<T> dspaceRunnableClass;

    /**
     * Retrieves the class type of the DSpace runnable script.
     *
     * @return the class of type {@link T} representing the script to execute.
     */
    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Sets the class type of the DSpace runnable script.
     *
     * @param dspaceRunnableClass The class of the script to be set.
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    /**
     * Determines whether the current user is authorized to execute the script.
     * This check is based on whether the user has administrator privileges.
     *
     * @param context               The DSpace context.
     * @param commandLineParameters The list of command line parameters provided.
     * @return true if the user has administrative privileges, false otherwise.
     * @throws RuntimeException if an SQL exception occurs while checking authorization.
     */
    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

    /**
     * Defines the command-line options available for the script.
     * These options allow customization of the script execution parameters.
     *
     * @return an {@link Options} object containing the available script options.
     */
    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("s", "single-researcher", true, "Single researcher UUID");
            options.getOption("s").setType(String.class);

            options.addOption("l", "loader", true, "Publication loader to be used");
            options.getOption("l").setType(String.class);
            options.getOption("l").setRequired(true);

            options.addOption("f", "solrfilter", true, "The additional SOLR filter to better refine results");
            options.getOption("f").setType(String.class);

            options.addOption("m", "max", true,
                              "The maximum number of researcher profiles to process. If no maximum is provided, then " +
                                  "the configured default will be used.");
            options.getOption("m").setType(Integer.class);

            super.options = options;
        }
        return options;
    }

}
