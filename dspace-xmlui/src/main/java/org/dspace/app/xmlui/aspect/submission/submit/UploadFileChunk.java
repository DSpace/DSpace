package org.dspace.app.xmlui.aspect.submission.submit;

import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.mortbay.log.Log;

public class UploadFileChunk extends AbstractAction{

    @Override
    public Map act(
            Redirector redirector,
            SourceResolver resolver,
            Map objectModel,
            String source,
            Parameters parameters) throws Exception
    {
        String handle = parameters.getParameter("handle", null);
        Log.info("=> " + handle);
        return null;
    }
}
