/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.app.rest.RestResourceController;

/**
 * Base REST representation for all the DSpaceObjects
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class DSpaceObjectRest extends BaseObjectRest<String> {
	private String uuid;

	private String name;
	private String handle;

	List<MetadataEntryRest> metadata;

	@Override
	public String getId() {
		return uuid;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public List<MetadataEntryRest> getMetadata() {
		return metadata;
	}

	public void setMetadata(List<MetadataEntryRest> metadata) {
		this.metadata = metadata;
	}

	@Override
	public Class getController() {
		return RestResourceController.class;
	}
}
