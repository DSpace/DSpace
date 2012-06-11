package org.dspace.dataonemn;

import nu.xom.Attribute;
import nu.xom.Element;

public class ListObjects extends Element {
	
	public ListObjects() {
	    super("d1:objectList", "http://ns.dataone.org/service/types/v1");
	}
	
	public void setCount(int aCount) {
		addAttribute(new Attribute("count", Integer.toString(aCount)));
	}
	
	public void setStart(int aStart) {
		addAttribute(new Attribute("start", Integer.toString(aStart)));
	}
	
	public void setTotal(int aTotal) {
		addAttribute(new Attribute("total", Integer.toString(aTotal)));
	}
	
	public String toString() {
		return toXML();
	}
	
	public void addObjectInfo(ObjectInfo aObjInfo) {
		appendChild(aObjInfo);
	}
}
