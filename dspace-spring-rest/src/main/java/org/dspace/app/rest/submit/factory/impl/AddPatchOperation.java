package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.model.Request;

public abstract class AddPatchOperation<T extends Object> extends PatchOperation<T> {
	
	@Override
	public void perform(Context context, Request currentRequest, WorkspaceItem source, String string, Object value) throws Exception {
		add(context, currentRequest, source, string, value);
	}

	abstract void add(Context context,Request currentRequest,WorkspaceItem source,String string,Object value) throws Exception;
	
}
