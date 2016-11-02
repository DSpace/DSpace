/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model.jdyna.editor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import it.cilea.osd.jdyna.editor.AdvancedPropertyEditorSupport;

public class DSpaceObjectPropertyEditor extends AdvancedPropertyEditorSupport
{
    private Context context;

    /** Model Class */
    private Class clazz;

    private Integer type;

    /** The logger */
    private final static Log log = LogFactory
            .getLog(DSpaceObjectPropertyEditor.class);

    public DSpaceObjectPropertyEditor(Class model, String service)
    {
        this.clazz = model;
        setMode(service);
    }

    public DSpaceObjectPropertyEditor(Integer type, Class model, String service)
    {
        this.type = type;
        this.clazz = model;
        setMode(service);
    }

    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        log.debug("chiamato DSpaceObjectPropertyEditor - setAsText text: "
                + text);
        if (text == null || text.trim().equals(""))
        {
            setValue(null);
        }
        else
        {
            setValue(Integer.parseInt(text));
        }
    }

    private Context getContext() throws Exception
    {
        if (context != null && context.isValid())
        {
            return context;
        }
        else
        {
            context = new Context();
        }
        return context;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    @Override
    public String getAsText()
    {
        log.debug("chiamato DSpaceObjectPropertyEditor - getAsText");
        Integer valore = (Integer) getValue();
        if (MODE_CSV.equals(getMode()))
        {
            String displayValue = "";
            Context context = null;
            try
            {
                context = getContext();
                displayValue = DSpaceObject.find(context, type, valore)
                        .getName();
            }
            catch (Exception ex)
            {
                log.debug("error DSpaceObjectPropertyEditor - getAsText"
                        + ex.getMessage());
            }
            finally
            {
                if (context != null && context.isValid())
                {
                    context.abort();
                }
            }
            return valore == null ? "" : "[ID=" + valore + "]" + displayValue;
        }
        return (valore == null ? "" : "" + valore);
    }

    @Override
    public void setValue(Object value)
    {
        if (value != null && value instanceof String)
        {
            super.setValue(Integer.parseInt((String) value));
        }
        else
        {
            super.setValue(value);
        }
    }

    public String getCustomText()
    {
        Integer valore = (Integer) getValue();
        String displayValue = "";
        Context context = null;
        try
        {
            context = getContext();
            displayValue = DSpaceObject.find(context, type, valore).getName();
        }
        catch (Exception ex)
        {
            log.debug("error DSpaceObjectPropertyEditor - getAsText"
                    + ex.getMessage());
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        return valore == null ? "" : displayValue;

    }
}
