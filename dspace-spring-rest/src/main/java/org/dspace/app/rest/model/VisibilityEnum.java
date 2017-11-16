/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

/**
 * The possible restriction options for the visibility attributes in the
 * SubmissionPanel resource and InputFormPage resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public enum VisibilityEnum {
	HIDDEN("hidden"), READONLY("read-only"), EDITABLE("editable");

	private String text;

	VisibilityEnum(String text) {
		this.text = text;
	}

	@Override
	public String toString() {
		return this.text;
	}

	public static VisibilityEnum fromString(String text) {
		if (text == null) {
			return null;
		}
		for (VisibilityEnum b : VisibilityEnum.values()) {
			if (b.text.equalsIgnoreCase(text)) {
				return b;
			}
		}
		throw new IllegalArgumentException("No visibility enum with text " + text + " found");
	}
}
