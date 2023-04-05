package edu.umd.lib.dspace.content;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link EmbargoListExportCli} script
 */
public class EmbargoListExportCliScriptConfiguration
        extends EmbargoListExportScriptConfiguration<EmbargoListExportCli> {
    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        options.addRequiredOption("f", "file", true, "the filename to export to");
        return super.options;
    }
}
