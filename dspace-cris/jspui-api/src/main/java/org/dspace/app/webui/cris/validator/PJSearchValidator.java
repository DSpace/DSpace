/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.validator;

import org.dspace.app.webui.cris.dto.PJSearchDTO;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

public class PJSearchValidator implements Validator {

	private Class clazz;

	public boolean supports(Class arg0) {
		return clazz.isAssignableFrom(arg0);
	}

	public void validate(Object arg0, Errors arg1) {
		PJSearchDTO dto = (PJSearchDTO) arg0;

		if (dto.getSearchMode() != null) {
			if (dto.getAdvancedSyntax()) {
				ValidationUtils.rejectIfEmptyOrWhitespace(arg1, "queryString",
						"error.validation.hku.search.query.empty");
			}
		} else if (dto.getCodeSearchMode() != null) {
			ValidationUtils.rejectIfEmptyOrWhitespace(arg1, "codeQuery",
					"error.validation.hku.search.query.empty");
		} else {
			arg1.reject("error.validation.hku.search.unknow-mode");
		}
	}

	public void setClazz(Class clazz) {
		this.clazz = clazz;
	}
}
