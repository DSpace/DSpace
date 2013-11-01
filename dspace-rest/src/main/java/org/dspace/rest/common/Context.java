/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rest.common;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public class Context {
	
	private int limit;
	private int offset;
	private long total_count;
	private String query;
	private String query_date;
	
	private static String dateFormat="yyyy-MM-dd'T'HH:mm:ss";
	
	private static SimpleDateFormat sdf;
	
	static{
		sdf=new SimpleDateFormat(dateFormat);
	}
	
	public Context(){
		query_date = sdf.format(new Date());
	}
	
	public long getTotal_count() {
		return total_count;
	}
	public void setTotal_count(long total_count) {
		this.total_count = total_count;
	}
	
	@XmlElement
	public String getQuery_date() {
		return query_date;
	}
	public void setQuery_date(Date query_date) {
		this.query_date = sdf.format(query_date);
	}
	
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}

}
