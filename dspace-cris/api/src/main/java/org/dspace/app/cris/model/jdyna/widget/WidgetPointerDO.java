/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.widget;

import it.cilea.osd.jdyna.editor.ModelPropertyEditor;
import it.cilea.osd.jdyna.service.IPersistenceDynaService;
import it.cilea.osd.jdyna.widget.WidgetPointer;

import java.beans.PropertyEditor;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.dspace.app.cris.model.jdyna.editor.PointerPropertyEditor;
import org.dspace.app.cris.model.jdyna.value.DOPointer;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "cris_do_wpointer")
public class WidgetPointerDO extends WidgetPointer<DOPointer>
{

	@Type(type="org.hibernate.type.StringClobType")
	private String filterExtended;

    public void setFilterExtended(String filterExtended)
    {
        this.filterExtended = filterExtended;
    }

    public String getFilterExtended()
    {
        return filterExtended;
    }

    @Override
    public PropertyEditor getImportPropertyEditor(
            IPersistenceDynaService applicationService, String service)
    {
        PointerPropertyEditor propertyEditor = new PointerPropertyEditor(getTargetValoreClass(), service);
        propertyEditor.setApplicationService(applicationService);
        return propertyEditor;
    }
    
    
    
}
