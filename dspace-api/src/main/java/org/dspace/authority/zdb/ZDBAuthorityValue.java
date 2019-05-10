/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.zdb;

import org.apache.commons.lang.StringUtils;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.AuthorityValueGenerator;

public class ZDBAuthorityValue extends AuthorityValue {

	@Override
	public String generateString() {
		String generateString = AuthorityValueGenerator.GENERATE + getAuthorityType() + AuthorityValueGenerator.SPLIT;
		if (StringUtils.isNotBlank(getServiceId())) {
			generateString += getServiceId();
		}
		return generateString;
	}
	
	@Override
	public String getAuthorityType() {	
		return "zdb";
	}
	
}
