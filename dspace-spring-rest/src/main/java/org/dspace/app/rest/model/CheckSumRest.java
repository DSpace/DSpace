/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

/**
 * The CheckSum REST Resource. It is not addressable directly, only used as
 * inline object in the Bitstream resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class CheckSumRest {
	String checkSumAlgorithm;
	String value;

	public String getCheckSumAlgorithm() {
		return checkSumAlgorithm;
	}

	public void setCheckSumAlgorithm(String checkSumAlgorithm) {
		this.checkSumAlgorithm = checkSumAlgorithm;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
