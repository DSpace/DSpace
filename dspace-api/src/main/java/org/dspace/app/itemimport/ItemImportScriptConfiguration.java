/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemimport;

import java.io.InputStream;
import java.sql.SQLException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link ScriptConfiguration} for the {@link ItemImport} script
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class ItemImportScriptConfiguration<T extends ItemImport> extends ScriptConfiguration<T> {

    @Autowired
    private AuthorizeService authorizeService;

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
    public boolean isAllowedToExecute(final Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }

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
        options.addOption(Option.builder("z").longOpt("zip")
                .desc("name of zip file")
                .type(InputStream.class)
                .hasArg().required().build());
        options.addOption(Option.builder("c").longOpt("collection")
                .desc("destination collection(s) Handle or database ID")
                .hasArg().required(false).build());
        options.addOption(Option.builder("m").longOpt("mapfile")
                .desc("mapfile items in mapfile")
                .type(InputStream.class)
                .hasArg().required(false).build());
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
