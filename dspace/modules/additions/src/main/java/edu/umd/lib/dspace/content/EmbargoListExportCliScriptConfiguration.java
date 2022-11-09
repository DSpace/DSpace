/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package edu.umd.lib.dspace.content;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link EmbargoListExport} script
 */
public class EmbargoListExportCliScriptConfiguration extends EmbargoListExportScriptConfiguration<EmbargoListExportCli> {
    @Override
    public Options getOptions() {
      Options options = super.getOptions();
      options.addRequiredOption("f", "file", true, "the filename to export to");
      return super.options;
    }
}
