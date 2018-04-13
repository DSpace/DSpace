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
