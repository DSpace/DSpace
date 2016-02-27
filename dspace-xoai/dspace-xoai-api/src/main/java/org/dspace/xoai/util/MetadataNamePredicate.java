package org.dspace.xoai.util;

import org.dspace.content.Item;

import com.google.common.base.Predicate;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;

public class MetadataNamePredicate implements Predicate<Element> {
	private String name;
	
	public MetadataNamePredicate (String n) {
		name = n;
	}

	@Override
	public boolean apply(Element arg0) {
		if (name == null) return false;
		else if (name.equals(Item.ANY)) return true;
		else return (name.toLowerCase().equals(arg0.getName().toLowerCase()));
	}
	
	
}
