package org.dspace.app.xmlui.aspect.general;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.acting.AbstractConfigurableAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;

import java.util.*;

/**
 * User: lantian @ atmire . com
 * Date: 11/13/13
 * Time: 5:30 PM
 */
public class LastModifiedDate extends AbstractConfigurableAction implements ThreadSafe {

        private FastDateFormat formatter = null;
        int days = 0;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;

        public void configure(Configuration configuration)
                throws ConfigurationException {
            super.configure(configuration);
        }

        public Map act(Redirector redirector, SourceResolver resolver,
                       Map objectModel, String source, Parameters parameters)
                throws Exception {
            Response response = ObjectModelHelper.getResponse(objectModel);

            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            Map values = new HashMap(3);
            Date date = new Date();
            String value = date.toString();

            if(dso instanceof Item)
            {
                Item item = (Item) dso;
                Date lastModified = item.getLastModified();
                /* Get the current time and output as the last modified header */
                value =  lastModified.toString();
            }
            response.setHeader("Last-Modified", value);
            values.put("last-modified",  value);

            /* Return the headers */
            return(Collections.unmodifiableMap(values));

        }
    }

