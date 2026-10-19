/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.solr;

/**
 * CLI Script configuration for SOLR core export/import operations.
 *
 * @author Stefano Maffei (stefano.maffei at 4science.com)
 */
public class SolrCoreExportImportCliScriptConfiguration<T extends SolrCoreExportImportCli>
        extends SolrCoreExportImportScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }

}
