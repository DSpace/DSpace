/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.widget;

import it.cilea.osd.jdyna.editor.FilePropertyEditor;
import it.cilea.osd.jdyna.service.IPersistenceDynaService;

import java.beans.PropertyEditor;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.dspace.app.cris.model.CrisConstants;
import org.dspace.app.cris.model.OrganizationUnit;
import org.dspace.app.cris.model.ResearchObject;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.core.ConfigurationManager;

@Entity
@Table(name = "cris_do_wfile")
public class WidgetFileDO extends AWidgetFileCris
{

    

    @Override
    public PropertyEditor getPropertyEditor(
            IPersistenceDynaService applicationService)
    {
        return new FilePropertyEditor<WidgetFileDO>(this);
    }



    @Override
    public String getBasePath()
    {
        return ConfigurationManager.getProperty(CrisConstants.CFG_MODULE, "otherresearchobject.file.path");
    }

    @Override
    public String getServletPath()
    {
        return ConfigurationManager
                .getProperty(CrisConstants.CFG_MODULE, "otherresearchobject.jdynafile.servlet.name");
    }

    @Override
    public String getCustomFolderByAuthority(String intAuth, String extAuth)
    {
        return ResearcherPageUtils.getPersistentIdentifier(Integer
                .parseInt(intAuth), ResearchObject.class) + "/" + extAuth;
    }
}
