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
 * this interface responsible for the Automation Processing of {@link QAEvent}
 * by returning the expected action related to QAEvent to be taken
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface QAEventAutomaticProcessingEvaluation {

    /**
     * evaluate automatic processing and return the expected action or null
     *
     * @param context the context
     * @param qaEvent the quality assurance event
     * @return an action of {@link AutomaticProcessingAction} or null
     */
    AutomaticProcessingAction evaluateAutomaticProcessing(Context context, QAEvent qaEvent);

}
