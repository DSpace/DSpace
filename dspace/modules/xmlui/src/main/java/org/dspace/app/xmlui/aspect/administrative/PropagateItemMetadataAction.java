/*
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;

/**
 * Propagate metadata to data files
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PropagateItemMetadataAction extends AbstractAction {

    private static Logger log = Logger.getLogger(PropagateItemMetadataAction.class);
    @Override
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Context context = ContextUtil.obtainContext(objectModel);
        Enumeration requestParams = request.getParameterNames();
        while(requestParams.hasMoreElements()) {
            String parameterName = (String) requestParams.nextElement();
            log.error("Parameter name: " + parameterName);
        }
        Map map = new HashMap();
        map.put("result", "value");
        return map;
    }
}
