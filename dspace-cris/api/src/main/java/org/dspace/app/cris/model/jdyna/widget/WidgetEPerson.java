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

import org.dspace.app.cris.model.jdyna.value.EPersonValue;

import it.cilea.osd.jdyna.model.AWidget;
import it.cilea.osd.jdyna.service.IPersistenceDynaService;
import it.cilea.osd.jdyna.util.ValidationMessage;
import it.cilea.osd.jdyna.value.PointerValue;

@Entity
@Table(name = "cris_weperson")
public class WidgetEPerson extends AWidget<EPersonValue>
{

    @Override
    public String getTriview()
    {
        return "eperson";
    }

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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Class<EPersonValue> getValoreClass()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ValidationMessage valida(Object valore)
    {
        // TODO Auto-generated method stub
        return null;
    }

}
