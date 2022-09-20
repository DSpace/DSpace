/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemexport;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link ItemExportCLI} script
 * 
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemExportCLIScriptConfiguration extends ItemExportScriptConfiguration<ItemExportCLI> {

    @Override
    public Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder("t").longOpt("type")
                .desc("type: COLLECTION or ITEM")
                .hasArg().required().build());
        options.addOption(Option.builder("i").longOpt("id")
                .desc("ID or handle of thing to export")
                .hasArg().required().build());
        options.addOption(Option.builder("d").longOpt("dest")
                .desc("destination where you want items to go")
                .hasArg().required().build());
        options.addOption(Option.builder("n").longOpt("number")
                .desc("sequence number to begin exporting items with")
                .hasArg().required().build());
        options.addOption(Option.builder("z").longOpt("zip")
                .desc("export as zip file (specify filename e.g. export.zip)")
                .hasArg().required(false).build());
        options.addOption(Option.builder("m").longOpt("migrate")
                .desc("export for migration (remove handle and metadata that will be re-created in new system)")
                .hasArg(false).required(false).build());

        // as pointed out by Peter Dietz this provides similar functionality to export metadata
        // but it is needed since it directly exports to Simple Archive Format (SAF)
        options.addOption(Option.builder("x").longOpt("exclude-bitstreams")
                .desc("do not export bitstreams")
                .hasArg(false).required(false).build());

        options.addOption(Option.builder("h").longOpt("help")
                .desc("help")
                .hasArg(false).required(false).build());

        return options;
    }
}
