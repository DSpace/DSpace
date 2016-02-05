/*
 */
package org.dspace.workflow;

import java.util.HashMap;
import java.util.Map;
import org.dspace.workflow.actions.processingaction.EditMetadataAction;

/**
 * HTTP Request containing parameters that would exist when curator clicks
 * to approve an item and sent it to blackout
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DummyCurateToBlackoutRequest extends DummyHttpRequest {
    private static final Map<String, String> PARAMETERS = new HashMap<String, String>() {{
        put("page", String.valueOf(EditMetadataAction.MAIN_PAGE));
        put("submit_blackout", String.valueOf(Boolean.TRUE));
    }};

    protected Map<String, String> getParameters() {
        return PARAMETERS;
    }

}
