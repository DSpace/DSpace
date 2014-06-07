/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;


import org.dspace.app.webui.cris.dto.MailDTO;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class MailValidator implements Validator {
	
	private Class clazz;
	
	
	public boolean supports(Class arg0) {
		return clazz.isAssignableFrom(arg0);
	}
	
	public void validate(Object arg0, Errors arg1) {
	    MailDTO dto = (MailDTO)arg0;
	    if(dto.getText()==null || dto.getText().isEmpty()) {
	        arg1.reject("error.textmail.mandatory", "Mail text is mandatory");
	    }		
	}

	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}
}
