/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.qaevent;

import org.dspace.content.QAEvent;
import org.dspace.core.Context;

/**
 * This interface allows the implemnetation of Automation Processing rules
 * defining which {@link AutomaticProcessingAction} should be eventually
 * performed on a specific {@link QAEvent}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface QAEventAutomaticProcessingEvaluation {

    /**
     * Evaluate a {@link QAEvent} to decide which, if any, {@link AutomaticProcessingAction} should be performed
     *
     * @param context the DSpace context
     * @param qaEvent the quality assurance event
     * @return        an action of {@link AutomaticProcessingAction} or null if no automatic action should be performed
     */
    AutomaticProcessingAction evaluateAutomaticProcessing(Context context, QAEvent qaEvent);

}
