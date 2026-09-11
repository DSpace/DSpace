/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

public class MediaFilterScriptConfiguration<T extends MediaFilterScript> extends ScriptConfiguration<T> {

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
    public Options getOptions() {
        Options options = new Options();
        options.addOption("v", "verbose", false, "Print all extracted text and other details to STDOUT");
        options.addOption("q", "quiet", false, "Do not print anything except in the event of errors");
        options.addOption("f", "force", false, "Force all bitstreams (can be restricted with -d or -i) " +
                                                                    "to be processed");
        options.addOption("i", "identifier", true,
            "ONLY process bitstreams belonging to the provided handle identifier");
        options.addOption("m", "maximum", true, "Process no more than maximum items");
        options.addOption("h", "help", false, "Help");

        Option pluginOption = Option.builder("p")
                                    .longOpt("plugins")
                                    .hasArg()
                                    .hasArgs()
                                    .valueSeparator(',')
                                    .desc(
                                            "ONLY run the specified Media Filter plugin(s)\n" +
                                                    "listed from '" + MEDIA_FILTER_PLUGINS_KEY + "' in dspace.cfg.\n" +
                                                    "Separate multiple with a comma (,)\n" +
                                                    "(e.g. filter-media -p \n\"Word Text Extractor\",\"PDF Text" +
                                                    " Extractor\")")
                                    .build();
        options.addOption(pluginOption);

        options.addOption("d", "fromdate", true, "Process only items modified after specified date (YYYY-MM-DD)");

        options.addOption("a", "autodate", false, "Process items updated after the last --autodate -based site-wide " +
                                    "run (stores the time at the start of the run regardless of the filters used),\n" +
                                    "can be combined with -d to set a custom date");

        Option skipOption = Option.builder("s")
                                  .longOpt("skip")
                                  .hasArg()
                                  .hasArgs()
                                  .valueSeparator(',')
                                  .desc(
                                          "SKIP the bitstreams belonging to identifier\n" +
                                                  "Separate multiple identifiers with a comma (,)\n" +
                                                  "(e.g. filter-media -s \n 123456789/34,123456789/323)")
                                  .build();
        options.addOption(skipOption);

        return options;
    }
}
