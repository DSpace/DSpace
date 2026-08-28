/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * CLI version of SOLR core export/import script.
 * This version can be executed freely from shell without authentication.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class SolrCoreExportImportCli<T extends ScriptConfiguration<?>> extends SolrCoreExportImport<T> {

    /**
     * Constructor for SolrCoreExportImportCli.
     * Command-line interface wrapper for SolrCoreExportImport script.
     * 
     * @param scriptConfiguration The CLI script configuration with command-line options
     */
    public SolrCoreExportImportCli(T scriptConfiguration) {
        super(scriptConfiguration);
    }

    @Override
    protected boolean requiresAuthentication() {
        // CLI version does not require authentication - can be run freely from shell
        return false;
    }
}
