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
 * The {@link ScriptConfiguration} for the {@link org.dspace.app.bulkedit.MetadataExport} CLI script
 * Overwritten to provide a different file parameter description
 */
public class MetadataExportCLIScriptConfiguration extends MetadataExportScriptConfiguration<MetadataExport> {

    @Override
    protected String getFileParameterDescription() {
        return "destination where you want file written";
    }
}
