/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent.script;

import java.io.InputStream;

import org.apache.commons.cli.Options;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Extension of {@link ScriptConfiguration} to perform a QAEvents import from
 * file.
 *
 * @author Alessandro Martelli (alessandro.martelli at 4science.it)
 *
 */
public class OpenaireEventsImportScriptConfiguration<T extends OpenaireEventsImport> extends ScriptConfiguration<T> {

    /*
    private AuthorizeService authorizeService;
     */
    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    /**
     * Generic setter for the dspaceRunnableClass
     * @param dspaceRunnableClass   The dspaceRunnableClass to be set on this OpenaireEventsImportScriptConfiguration
     */
    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }
/*
    @Override
    public boolean isAllowedToExecute(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException occurred when checking if the current user is an admin", e);
        }
    }
*/
    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("f", "file", true, "Import data from Openaire quality assurance broker JSON file."
                + " This parameter is mutually exclusive to the email parameter.");
            options.getOption("f").setType(InputStream.class);
            options.getOption("f").setRequired(false);

            options.addOption("e", "email", true, "Email related to the subscriptions to import data from Openaire "
                + "broker. This parameter is mutually exclusive to the file parameter.");
            options.getOption("e").setType(String.class);
            options.getOption("e").setRequired(false);

            super.options = options;
        }
        return options;
    }

}
