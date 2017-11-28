/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.model.Request;

/**
 * 
 * Class to manage HTTP PATCH method operation ADD
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 * @param <T>
 */
public abstract class AddPatchOperation<T extends Object> extends PatchOperation<T> {
	
	@Override
	public void perform(Context context, Request currentRequest, WorkspaceItem source, String string, Object value) throws Exception {
		add(context, currentRequest, source, string, value);
	}

	abstract void add(Context context,Request currentRequest,WorkspaceItem source,String string,Object value) throws Exception;
	
}
