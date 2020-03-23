/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.scripts.configuration;

import org.dspace.app.bulkedit.MetadataImportCLI;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.scripts.DSpaceRunnable;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The {@link ScriptConfiguration} for the {@link org.dspace.app.bulkedit.MetadataImportCLI} CLI script
 */
public class MetadataImportCliScriptConfiguration extends MetadataImportScriptConfiguration {

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public Class<? extends DSpaceRunnable> getDspaceRunnableClass() {
        return MetadataImportCLI.class;
    }
}
