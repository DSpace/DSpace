package org.dspace.dataonemn;

import nu.xom.Attribute;
import nu.xom.Element;

public class ListObjects extends Element implements Constants{
	
	public ListObjects() {
	    super("d1:objectList", D1_TYPES_NAMESPACE);
	}
	
	public void setCount(int aCount) {
		addAttribute(new Attribute("count", Integer.toString(aCount)));
	}
	
	public void setStart(int aStart) {
		addAttribute(new Attribute("start", Integer.toString(aStart)));
	}
	
	public void setTotal(long aTotal) {
		addAttribute(new Attribute("total", Long.toString(aTotal)));
	}
        
	public String toString() {
		return toXML();
	}
	
	public void addObjectInfo(ObjectInfo aObjInfo) {
		appendChild(aObjInfo);
	}
}
