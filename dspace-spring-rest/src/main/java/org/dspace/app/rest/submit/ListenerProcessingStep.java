/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit;

import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public interface ListenerProcessingStep {

    public void doPreProcessing(Context context, InProgressSubmission wsi);

    public void doPostProcessing(Context context, InProgressSubmission wsi);

}
