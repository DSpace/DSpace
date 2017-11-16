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
import org.springframework.data.rest.webmvc.json.patch.LateObjectEvaluator;

/**
 * Class to abstract the HTTP PATCH method operation 
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 * @param <T>
 */
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