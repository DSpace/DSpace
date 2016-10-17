/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.widget;

import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.app.cris.model.jdyna.editor.DSpaceObjectPropertyEditor;
import org.dspace.app.cris.model.jdyna.value.EPersonValue;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

import it.cilea.osd.common.model.Selectable;
import it.cilea.osd.jdyna.editor.AdvancedPropertyEditorSupport;
import it.cilea.osd.jdyna.service.IPersistenceDynaService;
import it.cilea.osd.jdyna.util.ValidationMessage;
import it.cilea.osd.jdyna.utils.SelectableDTO;
import it.cilea.osd.jdyna.widget.WidgetCustomPointer;

@Entity
@Table(name = "cris_weperson")
public class WidgetEPerson extends WidgetCustomPointer<EPersonValue>
{
    @Transient
    protected Log log = LogFactory.getLog(getClass());
    
    @Override
    public EPersonValue getInstanceValore()
    {
        try {
            EPersonValue pointer = getValoreClass().newInstance();
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
        DSpaceObjectPropertyEditor pe = new DSpaceObjectPropertyEditor(Constants.EPERSON, EPerson.class, AdvancedPropertyEditorSupport.MODE_VIEW);
        return pe;
    }

    @Override
    public Class<EPersonValue> getValoreClass()
    {
       return EPersonValue.class;
    }

    @Override
    public ValidationMessage valida(Object valore)
    {
        return null;
    }

    @Override
    public Integer getType()
    {        
        return Constants.EPERSON;
    }

    @Override
    public List<Selectable> search(String query, String expression, String... filtro)
    {
        Context context = null;
        List<Selectable> results = new ArrayList<Selectable>();
        try
        {
            context = new Context();

            EPerson[] objects = EPerson.search(context, query);
            for (EPerson obj : objects)
            {
                String display = obj.getName();
                SelectableDTO dto = new SelectableDTO(
                        obj.getID(), display);
                results.add(dto);
            }

        }
        catch (Exception e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        
        return results;
    }

}
