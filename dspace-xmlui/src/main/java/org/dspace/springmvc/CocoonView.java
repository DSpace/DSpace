/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.springmvc;

import org.apache.cocoon.servletservice.DynamicProxyRequestHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.View;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */


public class CocoonView implements View
{
    /**
     * The startup date of the Spring application context used to setup the  {@link #blockServletCollector}.
     */
    long applicationContextStartDate;
    /**
     * The servlet collector bean
     */
    Map blockServletCollector;

    public CocoonView()
    {
    }

    void getInterfaces(Set interfaces, Class clazz)
    {
        Class[] clazzInterfaces = clazz.getInterfaces();
        for (int i = 0; i < clazzInterfaces.length; i++)
        {
            //add all interfaces extended by this interface or directly
            //implemented by this class
            getInterfaces(interfaces, clazzInterfaces[i]);
        }

        // the superclazz is null if class is instanceof Object, is
        // an interface, a primitive type or void
        Class superclazz = clazz.getSuperclass();
        if (superclazz != null)
        {
            //add all interfaces of the superclass to the list
            getInterfaces(interfaces, superclazz);
        }

        interfaces.addAll(Arrays.asList(clazzInterfaces));
    }

    public String getContentType()
    {
        return null;
    }

    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception
    {

        final Map mountableServlets = getBlockServletMap(request);

        String path = request.getPathInfo();

        if (path == null)
        {
            path = "";
        }

        // find the servlet which mount path is the longest prefix of the path info
        int index = path.length();
        Servlet servlet = null;
        while (servlet == null && index != -1)
        {
            path = path.substring(0, index);
            servlet = (Servlet) mountableServlets.get(path);
            index = path.lastIndexOf('/');
        }
        //case when servlet is mounted at "/" must be handled separately
        servlet = servlet == null ? (Servlet) mountableServlets.get("/") : servlet;
        if (servlet == null)
        {
            String message = "No block for " + request.getPathInfo();
            response.sendError(HttpServletResponse.SC_NOT_FOUND, message);

            return;
        }

        // Create a dynamic proxy class that overwrites the getServletPath and
        // getPathInfo methods to provide reasonable values in the called servlet
        // the dynamic proxy implements all interfaces of the original request
        HttpServletRequest prequest = (HttpServletRequest) Proxy.newProxyInstance(
                request.getClass().getClassLoader(),
                getInterfaces(request.getClass()),
                new DynamicProxyRequestHandler(request, path));

        servlet.service(prequest, response);

    }

    Class[] getInterfaces(final Class clazz)
    {
        Set interfaces = new LinkedHashSet();
        getInterfaces(interfaces, clazz);
        return (Class[]) interfaces.toArray(new Class[interfaces.size()]);
    }

    public Map getBlockServletMap(HttpServletRequest request)
    {
        ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession(true).getServletContext());

        if (this.blockServletCollector == null || applicationContext.getStartupDate() != this.applicationContextStartDate)
        {
            this.applicationContextStartDate = applicationContext.getStartupDate();
            this.blockServletCollector = (Map) applicationContext.getBean("org.apache.cocoon.servletservice.spring.BlockServletMap");
        }

        return blockServletCollector;
    }
}