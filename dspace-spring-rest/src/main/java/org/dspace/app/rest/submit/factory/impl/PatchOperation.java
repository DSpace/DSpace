package org.dspace.app.rest.submit.factory.impl;

import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.services.model.Request;
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

public abstract class PatchOperation<T extends Object> {

	public abstract void perform(Context context, Request currentRequest, WorkspaceItem source, String path, Object value) throws Exception;
	
	public T[] evaluateObject(LateObjectEvaluator value) {
		T[] list = null;
		if(value!=null) {
			LateObjectEvaluator object = (LateObjectEvaluator)value;
			list = (T[])object.evaluate(getClassForEvaluation());
		}
		return list;
	}

	protected abstract Class<T[]> getClassForEvaluation();
	
}