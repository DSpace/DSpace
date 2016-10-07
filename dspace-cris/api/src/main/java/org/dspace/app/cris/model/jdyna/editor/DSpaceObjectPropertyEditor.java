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

    /** The logger */
    private final static Log log = LogFactory
            .getLog(DSpaceObjectPropertyEditor.class);

    public DSpaceObjectPropertyEditor(Class model, String service)
    {
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
            int type = 2;
            if (EPerson.class.isAssignableFrom(clazz))
            {
                type = 5;
            }
            else if (Group.class.isAssignableFrom(clazz))
            {
                type = 6;
            }
            else if (Collection.class.isAssignableFrom(clazz))
            {
                type = 3;
            }
            else if (Community.class.isAssignableFrom(clazz))
            {
                type = 4;
            }
            Context context = null;
            try
            {
                context = getContext();
                setValue(DSpaceObject.find(context, type,
                        Integer.parseInt(text)));
            }
            catch (Exception ex)
            {

            }
            finally
            {
                if (context != null && context.isValid())
                {
                    context.abort();
                }
            }

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
        DSpaceObject valore = (DSpaceObject) getValue();
        return (valore == null ? "" : "" + valore.getID());
    }

}
