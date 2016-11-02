/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.controller.jdyna;

import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.ACrisObject;

import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.model.PropertiesDefinition;
import it.cilea.osd.jdyna.utils.SelectableDTO;
import it.cilea.osd.jdyna.web.controller.SearchPointerController;
import it.cilea.osd.jdyna.widget.WidgetCustomPointer;

public class CrisSearchCustomPointerController
        extends SearchPointerController<PropertiesDefinition, ACrisObject>
{

    @Transient
    protected Log log = LogFactory.getLog(getClass());

    @Override
    protected List<SelectableDTO> getResult(AWidget widget, String query,
            String expression, String... filtro)
    {
        return ((WidgetCustomPointer) widget).search(query, expression, filtro);
    }

    @Override
    protected String[] getFilter(AWidget widget)
    {
        return null;
    }

    @Override
    protected String getDisplay(AWidget widget)
    {
        return null;
    }

}
