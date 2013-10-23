package org.dspace.rest.common;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType
public class CheckSum{
	String checkSumAlgorithm;
	String value;
	
	public CheckSum(){}

	@XmlAttribute(name="checkSumAlgorithm")
	public String getCheckSumAlgorith() {
		return checkSumAlgorithm;
	}

	public void setCheckSumAlgorith(String checkSumAlgorith) {
		this.checkSumAlgorithm = checkSumAlgorith;
	}

	@XmlValue
	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}