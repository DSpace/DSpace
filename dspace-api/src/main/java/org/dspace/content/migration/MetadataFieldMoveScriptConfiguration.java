/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link MetadataFieldMoveScript} script.
 *
 * @param <T> the script class type
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class MetadataFieldMoveScriptConfiguration<T extends MetadataFieldMoveScript>
    extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options newOptions = new Options();

            Option sourceOption = Option.builder("s").longOpt("source-field")
                .desc("Source metadata field as a Java regex over the canonical name schema.element[.qualifier], "
                    + "e.g. '^cris\\.(.+)$'")
                .hasArg().required(true).build();
            newOptions.addOption(sourceOption);

            Option targetOption = Option.builder("t").longOpt("target-field")
                .desc("Target field template, supporting $1 backreferences, e.g. 'dspace.$1'")
                .hasArg().required(true).build();
            newOptions.addOption(targetOption);

            newOptions.addOption("n", "dry-run", false,
                "Dry run: list the resolved field pairs and counts without committing changes");
            newOptions.addOption("b", "batch-size", true,
                "Number of objects to process per batch (default 1000)");
            newOptions.addOption("h", "help", false,
                "Display help for this script");

            super.options = newOptions;
        }
        return options;
    }
}
