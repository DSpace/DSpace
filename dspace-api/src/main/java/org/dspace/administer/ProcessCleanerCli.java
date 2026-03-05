/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.administer;

import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ProcessCleaner} for CLI.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ProcessCleanerCli<T extends ScriptConfiguration<?>> extends ProcessCleaner<T> {


    /**
     * Constructor for ProcessCleanerCli.
     * Command-line interface wrapper for ProcessCleaner script.
     * 
     * @param scriptConfiguration The CLI script configuration with command-line options
     */
    public ProcessCleanerCli(T scriptConfiguration) {
        super(scriptConfiguration);
    }
}
