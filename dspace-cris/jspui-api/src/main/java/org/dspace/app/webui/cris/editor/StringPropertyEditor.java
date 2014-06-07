/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.editor;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Property editor needs to the Spring Binder framework to work with the
 * {@link java.lang.String}
 * 
 * @see PropertyEditor 
 * @author cilea
 * 
 */
public class StringPropertyEditor extends PropertyEditorSupport {

	private final static Log log = LogFactory.getLog(StringPropertyEditor.class);

	   /** Model Class */
    private Class clazz;

    public StringPropertyEditor(Class clazz) {
        this.clazz = clazz;
    }
    
	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		log.debug("call StringPropertyEditor - setAsText text: " + text);		
		if (text == null || text.trim().equals("")) {
			setValue(null);
		} else {			
			setValue(text);
		}

	}

	@Override
	public String getAsText() {
		log.debug("chiamato StringPropertyEditor - getAsText");
		Object value = getValue();
		if(value==null) {
			return "";
		}
		return value.toString();
	}


}
