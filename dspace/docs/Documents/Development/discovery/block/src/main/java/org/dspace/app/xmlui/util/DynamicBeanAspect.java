package org.dspace.app.xmlui.util;

import org.dspace.app.xmlui.configuration.Aspect;
import org.dspace.app.xmlui.configuration.XMLUIConfiguration;
import org.springframework.beans.factory.InitializingBean;

/**
 * User: mdiggory
 * Date: Oct 18, 2009
 * Time: 2:35:23 PM
 */
public class DynamicBeanAspect extends Aspect implements InitializingBean {


    public DynamicBeanAspect(String name, String path) {
        super(name, path);
    }

    public void afterPropertiesSet() throws Exception {
        XMLUIConfiguration.getAspectChain().add(this);
    }
}
