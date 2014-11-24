/*
 */
package org.dspace.workflow;

import java.util.HashMap;
import java.util.Map;
import org.dspace.workflow.actions.processingaction.EditMetadataAction;

/**
 * HTTP Request that contains parameters that would be present when curator
 * clicks to archive a submission in blackout
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DummyApproveFromBlackoutRequest extends DummyHttpRequest {
    private static final Map<String, String> PARAMETERS = new HashMap<String, String>() {{
        put("page", String.valueOf(EditMetadataAction.MAIN_PAGE));
        put("after_blackout_submit", String.valueOf(Boolean.TRUE));
        put("submit_approve", String.valueOf(Boolean.TRUE));
    }};

    protected Map<String, String> getParameters() {
        return PARAMETERS;
    }

}
