/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link ItemImportCLI} script
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemImportCLIScriptConfiguration extends ItemImportScriptConfiguration<ItemImportCLI> {

    @Override
    public Options getOptions() {
        Options options = new Options();

        options.addOption(Option.builder("a").longOpt("add")
                .desc("add items to DSpace")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("r").longOpt("replace")
                .desc("replace items in mapfile")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("d").longOpt("delete")
                .desc("delete items listed in mapfile")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("s").longOpt("source")
                .desc("source of items (directory)")
                .hasArg().required(false).build());
        options.addOption(Option.builder("z").longOpt("zip")
                .desc("name of zip file")
                .hasArg().required(false).build());
        options.addOption(Option.builder("c").longOpt("collection")
                .desc("destination collection(s) Handle or database ID")
                .hasArg().required(false).build());
        options.addOption(Option.builder("m").longOpt("mapfile")
                .desc("mapfile items in mapfile")
                .hasArg().required().build());
        options.addOption(Option.builder("e").longOpt("eperson")
                .desc("email of eperson doing importing")
                .hasArg().required().build());
        options.addOption(Option.builder("w").longOpt("workflow")
                .desc("send submission through collection's workflow")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("n").longOpt("notify")
                .desc("if sending submissions through the workflow, send notification emails")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("v").longOpt("validate")
                .desc("test run - do not actually import items")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("x").longOpt("exclude-bitstreams")
                .desc("do not load or expect content bitstreams")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("p").longOpt("template")
                .desc("apply template")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("R").longOpt("resume")
                .desc("resume a failed import (add only)")
                .hasArg(false).required(false).build());
        options.addOption(Option.builder("q").longOpt("quiet")
                .desc("don't display metadata")
                .hasArg(false).required(false).build());

        options.addOption(Option.builder("h").longOpt("help")
                .desc("help")
                .hasArg(false).required(false).build());

        return options;
    }
}
