/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import java.io.Serializable;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.WorkspaceItem;

/**
 * Interface to retrieve information about section
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 *
 */
public interface AbstractRestProcessingStep {

	public <T extends Serializable> T getData(WorkspaceItem obj, SubmissionStepConfig config) throws Exception;


}
