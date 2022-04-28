/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import java.sql.SQLException;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

public class MediaFilterScriptConfiguration<T extends MediaFilterScript> extends ScriptConfiguration<T> {

    @Autowired
    private AuthorizeService authorizeService;

    private Class<T> dspaceRunnableClass;

    private static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";


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
        options.addOption("v", "verbose", false, "print all extracted text and other details to STDOUT");
        options.addOption("q", "quiet", false, "do not print anything except in the event of errors.");
        options.addOption("f", "force", false, "force all bitstreams to be processed");
        options.addOption("i", "identifier", true, "ONLY process bitstreams belonging to identifier");
        options.addOption("m", "maximum", true, "process no more than maximum items");
        options.addOption("h", "help", false, "help");

        Option pluginOption = Option.builder("p")
                                    .longOpt("plugins")
                                    .hasArg()
                                    .hasArgs()
                                    .valueSeparator(',')
                                    .desc(
                                            "ONLY run the specified Media Filter plugin(s)\n" +
                                                    "listed from '" + MEDIA_FILTER_PLUGINS_KEY + "' in dspace.cfg.\n" +
                                                    "Separate multiple with a comma (,)\n" +
                                                    "(e.g. MediaFilterManager -p \n\"Word Text Extractor\",\"PDF Text" +
                                                    " Extractor\")")
                                    .build();
        options.addOption(pluginOption);

        Option skipOption = Option.builder("s")
                                  .longOpt("skip")
                                  .hasArg()
                                  .hasArgs()
                                  .valueSeparator(',')
                                  .desc(
                                          "SKIP the bitstreams belonging to identifier\n" +
                                                  "Separate multiple identifiers with a comma (,)\n" +
                                                  "(e.g. MediaFilterManager -s \n 123456789/34,123456789/323)")
                                  .build();
        options.addOption(skipOption);

        return options;
    }
}
