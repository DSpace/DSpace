/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

/**
 * The SubmissionVisibility REST Resource. It is not addressable directly, only used as
 * inline object in the SubmissionPanel resource and InputFormPage resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class SubmissionVisibilityRest {
	private VisibilityEnum main;
	private VisibilityEnum other;
	
	
	public SubmissionVisibilityRest(VisibilityEnum main, VisibilityEnum other) {
		super();
		this.main = main;
		this.other = other;
	}
	
	public VisibilityEnum getMain() {
		return main;
	}

	public VisibilityEnum getOther() {
		return other;
	}
}
