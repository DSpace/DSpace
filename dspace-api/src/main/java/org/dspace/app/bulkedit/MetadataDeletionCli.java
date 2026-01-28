/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link MetadataDeletion} for CLI.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class MetadataDeletionCli<T extends ScriptConfiguration<?>> extends MetadataDeletion<T> {

    /**
     * Constructor for MetadataDeletionCli.
     * Command-line interface wrapper for MetadataDeletion script.
     * 
     * @param scriptConfiguration The CLI script configuration with command-line options
     */
    public MetadataDeletionCli(T scriptConfiguration) {
        super(scriptConfiguration);
    }
}
