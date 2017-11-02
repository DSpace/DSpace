/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.WorkspaceItemRest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the WorkspaceItem in the DSpace API data model and the
 * REST data model
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class WorkspaceItemConverter extends DSpaceConverter<org.dspace.content.WorkspaceItem, org.dspace.app.rest.model.WorkspaceItemRest> {

	private static final Logger log = Logger.getLogger(WorkspaceItemConverter.class);
	
	@Autowired
	private EPersonConverter epersonConverter;
	
	@Autowired
	private ItemConverter itemConverter;
	
	@Override
	public WorkspaceItemRest fromModel(org.dspace.content.WorkspaceItem obj) {
		WorkspaceItemRest witem = new WorkspaceItemRest();
		witem.setId(obj.getID());
		witem.setItem(itemConverter.convert(obj.getItem()));
		try {
			witem.setSubmitter(epersonConverter.convert(obj.getSubmitter()));
		} catch (SQLException e) {
			throw new RuntimeException(e.getMessage(), e);
		}

		// 1. retrieve the submission definition
		// 2. iterate over the submission section to allow to plugin additional info
		return witem;
	}

	@Override
	public org.dspace.content.WorkspaceItem toModel(WorkspaceItemRest obj) {
		return null;
	}

}
