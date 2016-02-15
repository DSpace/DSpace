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
import org.dspace.app.cris.model.ACrisObject;

import it.cilea.osd.jdyna.editor.ModelPropertyEditor;

public class PointerPropertyEditor extends ModelPropertyEditor
{

    /** The logger */
    private final static Log log = LogFactory
            .getLog(PointerPropertyEditor.class);

    public PointerPropertyEditor(Class model, String service)
    {
        super(model);
        setMode(service);
    }

    @Override
    public String getAsText()
    {
        log.debug("chiamato PointerPropertyEditor - getAsText");
        ACrisObject valore = (ACrisObject) getValue();
        if (MODE_CSV.equals(getMode()))
        {
            return valore == null ? "" : "[CRISID=" + (valore.getCrisID() != null
                    ? valore.getCrisID()
                    : "") + "]" + valore.getName();
        }
        return super.getAsText();
    }

}
