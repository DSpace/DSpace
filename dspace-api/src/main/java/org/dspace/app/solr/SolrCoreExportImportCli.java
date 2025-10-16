/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

import org.dspace.utils.DSpace;

/**
 * CLI version of SOLR core export/import script.
 * This version can be executed freely from shell without authentication.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class SolrCoreExportImportCli extends SolrCoreExportImport {

    @Override
    public SolrCoreExportImportCliScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("solr-core-management",
                SolrCoreExportImportCliScriptConfiguration.class);
    }

    @Override
    protected boolean requiresAuthentication() {
        // CLI version does not require authentication - can be run freely from shell
        return false;
    }
}
