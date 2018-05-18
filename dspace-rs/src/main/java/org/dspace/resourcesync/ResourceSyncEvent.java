/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import java.util.Date;
import java.util.List;
/**
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 */
public class ResourceSyncEvent {
	private int resource_id;
	private int resource_type;
	private String changetype;
	private Date datetime;
	private List<String> scopes;
	private String handle;
	private List<String> extra;
	public List<String> getExtra() {
		return extra;
	}
	public void setExtra(List<String> extra) {
		this.extra = extra;
	}
	public String getHandle() {
		return handle;
	}
	public void setHandle(String handle) {
		this.handle = handle;
	}
	public int getResource_id() {
		return resource_id;
	}
	public void setResource_id(int resource_id) {
		this.resource_id = resource_id;
	}
	public int getResource_type() {
		return resource_type;
	}
	public void setResource_type(int resource_type) {
		this.resource_type = resource_type;
	}
	public String getChangetype() {
		return changetype;
	}
	public void setChangetype(String changetype) {
		this.changetype = changetype;
	}
	public Date getDatetime() {
		return datetime;
	}
	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}
	public List<String> getScopes() {
		return scopes;
	}
	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

}
