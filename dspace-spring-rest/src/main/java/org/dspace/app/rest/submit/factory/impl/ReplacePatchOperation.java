package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.model.Request;

public abstract class ReplacePatchOperation<T extends Object> extends PatchOperation<T> {

	@Override
	public void perform(Context context, Request currentRequest, WorkspaceItem source, String string, Object value)
			throws Exception {
		replace(context, currentRequest, source, string, value);
	}

	abstract void replace(Context context, Request currentRequest, WorkspaceItem source, String string, Object value)
			throws Exception;

}
