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
 * Class to manage HTTP PATCH method operation MOVE
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 * @param <T>
 */
public abstract class MovePatchOperation<T extends Object> extends PatchOperation<T> {
	
	@Override
	public void perform(Context context, Request currentRequest, WorkspaceItem source, String path, Object from) throws Exception {
		move(context, currentRequest, source, path, from);
	}

	abstract void move(Context context, Request currentRequest, WorkspaceItem source, String path, Object from) throws Exception;
	
}
