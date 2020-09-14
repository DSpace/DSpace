/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * This Enum holds all the possible options and combinations for the Curation script
 *
 * @author Maria Verdonck (Atmire) on 23/06/2020
 */
public enum CurationClientOptions {
    TASK,
    QUEUE,
    HELP;

    private static List<String> taskOptions;

    /**
     * This method resolves the CommandLine parameters to figure out which action the curation script should perform
     *
     * @param commandLine The relevant CommandLine for the curation script
     * @return The curation option to be ran, parsed from the CommandLine
     */
    protected static CurationClientOptions getClientOption(CommandLine commandLine) {
        if (commandLine.hasOption("h")) {
            return CurationClientOptions.HELP;
        } else if (commandLine.hasOption("t") || commandLine.hasOption("T")) {
            return CurationClientOptions.TASK;
        } else if (commandLine.hasOption("q")) {
            return CurationClientOptions.QUEUE;
        }
        return null;
    }

    /**
     * This method will create all the possible Options for the {@link Curation} script.
     * This will be used by {@link CurationScriptConfiguration}
     * @return  The options for the {@link Curation} script
     */
    protected static Options constructOptions() {
        Options options = new Options();

        options.addOption("t", "task", true, "curation task name; options: " + getTaskOptions());
        options.addOption("T", "taskfile", true, "file containing curation task names");
        options.addOption("i", "id", true,
            "Id (handle) of object to perform task on, or 'all' to perform on whole repository");
        options.addOption("p", "parameter", true, "a task parameter 'NAME=VALUE'");
        options.addOption("q", "queue", true, "name of task queue to process");
        options.addOption("r", "reporter", true,
            "relative or absolute path to the desired report file. Use '-' to report to console. If absent, no " +
            "reporting");
        options.addOption("s", "scope", true,
            "transaction scope to impose: use 'object', 'curation', or 'open'. If absent, 'open' applies");
        options.addOption("v", "verbose", false, "report activity to stdout");
        options.addOption("h", "help", false, "help");

        return options;
    }

    /**
     * Creates list of the taskOptions' keys from the configs of plugin.named.org.dspace.curate.CurationTask
     *
     * @return List of the taskOptions' keys from the configs of plugin.named.org.dspace.curate.CurationTask
     */
    public static List<String> getTaskOptions() {
        if (taskOptions == null) {
            ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
            String[] taskConfigs = configurationService.getArrayProperty("plugin.named.org.dspace.curate.CurationTask");
            taskOptions = new ArrayList<>();
            for (String taskConfig : taskConfigs) {
                taskOptions.add(StringUtils.substringAfterLast(taskConfig, "=").trim());
            }
        }
        return taskOptions;
    }
}
