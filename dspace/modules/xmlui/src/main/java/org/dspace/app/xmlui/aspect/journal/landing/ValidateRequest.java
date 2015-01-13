/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.app.xmlui.aspect.journal.landing;

import java.util.Map;
import java.util.HashMap;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import static org.dspace.app.xmlui.aspect.journal.landing.Const.*;

/**
 *
 * @author Nathan Day
 */
public class ValidateRequest extends AbstractAction {
    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, 
                    String source, Parameters parameters) throws Exception 
    {
        String journalAbbr = parameters.getParameter(PARAM_JOURNAL_ABBR);
        if (journalAbbr == null || journalAbbr.length() == 0) return null;

        // verify we have an accurate journal
        if (true) {
            Map map = new HashMap();
            map.put(PARAM_JOURNAL_NAME, "Dryad Testing Journal");
            map.put(PARAM_JOURNAL_ABBR, "DTJ");
            return map;
        }
        // journal not found, signal invalid request with failed action
        // by passing back a non-map object
        return null;
    }
}
