/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.model;

import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;

@Embeddable
@MappedSuperclass
public class RestrictedFieldWithLock extends RestrictedField
{
	private Integer lock;

	public RestrictedFieldWithLock()
	{
		super();
	}

	public Integer getLock()
	{
		return lock;
	}

	public void setLock(Integer lock)
	{
		this.lock = lock == null ? new Integer(0) : lock;
	}
}
