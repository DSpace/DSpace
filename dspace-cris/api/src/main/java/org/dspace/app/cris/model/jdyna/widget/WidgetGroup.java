/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.widget;

import java.beans.PropertyEditor;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.dspace.app.cris.model.jdyna.editor.DSpaceObjectPropertyEditor;
import org.dspace.app.cris.model.jdyna.value.EPersonValue;
import org.dspace.app.cris.model.jdyna.value.GroupValue;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import it.cilea.osd.jdyna.editor.AdvancedPropertyEditorSupport;
import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.service.IPersistenceDynaService;
import it.cilea.osd.jdyna.util.ValidationMessage;

@Entity
@Table(name = "cris_wgroup")
public class WidgetGroup extends AWidget<GroupValue>
{

    private String regex;
    
    @Override
    public String getTriview()
    {
        return "group";
    }

    @Override
    public GroupValue getInstanceValore()
    {
        try {
            GroupValue pointer = getValoreClass().newInstance();
            return pointer;
        } catch (Exception e) {
            log.error(e);
            throw new IllegalStateException("Illegal type for this widget",e);
        }
    }

    @Override
    public PropertyEditor getPropertyEditor(
            IPersistenceDynaService applicationService)
    {
        DSpaceObjectPropertyEditor pe = new DSpaceObjectPropertyEditor(Group.class, AdvancedPropertyEditorSupport.MODE_VIEW);
        return pe;
    }

    @Override
    public Class<GroupValue> getValoreClass()
    {
       return GroupValue.class;
    }

    @Override
    public ValidationMessage valida(Object valore)
    {
        return null;
    }

    public String getRegex()
    {
        return regex;
    }

    public void setRegex(String regex)
    {
        this.regex = regex;
    }

}
