/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.model.ErrorRest;

/**
 * Abstract class to manage errors on validation during submission process
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public abstract class AbstractValidation implements Validation {

	private String name;

	private List<ErrorRest> errors = new ArrayList<ErrorRest>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addError(String i18nKey, String path) {
		boolean found = false;
		if (StringUtils.isNotBlank(i18nKey)) {
			for (ErrorRest error : errors) {
				if (i18nKey.equals(error.getMessage())) {
					error.getPaths().add(path);
					found = true;
					break;
				}
			}
		}
		if(!found) {
			ErrorRest error = new ErrorRest();
			error.setMessage(i18nKey);		
			error.getPaths().add(path);
			errors.add(error);
		}
	}

	public List<ErrorRest> getErrors() {
		return errors;
	}
}
