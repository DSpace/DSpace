package org.dspace.springmvc;

import org.apache.cocoon.servletservice.DynamicProxyRequestHandler;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 8/29/11
 * Time: 12:24 PM
 * To change this template use File | Settings | File Templates.
 */

@Controller(value = "cocoonForwardController")
public class CocoonForwardController {

    @RequestMapping
    public ModelAndView forwardRequest(HttpServletRequest request, HttpServletResponse response) throws SQLException {
        return new ModelAndView(new CocoonView());
    }


    class CocoonView implements View {

        /**
         * The startup date of the Spring application context used to setup the  {@link #blockServletCollector}.
         */
        private long applicationContextStartDate;

        /**
         * The servlet collector bean
         */
        private Map blockServletCollector;


        private void getInterfaces(Set interfaces, Class clazz) {
            Class[] clazzInterfaces = clazz.getInterfaces();
            for (int i = 0; i < clazzInterfaces.length; i++) {
                //add all interfaces extended by this interface or directly
                //implemented by this class
                getInterfaces(interfaces, clazzInterfaces[i]);
            }

            // the superclazz is null if class is instanceof Object, is
            // an interface, a primitive type or void
            Class superclazz = clazz.getSuperclass();
            if (superclazz != null) {
                //add all interfaces of the superclass to the list
                getInterfaces(interfaces, superclazz);
            }

            interfaces.addAll(Arrays.asList(clazzInterfaces));
        }


        public String getContentType() {
            return null;
        }

        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

            final Map mountableServlets = getBlockServletMap(request);

            String path = request.getPathInfo();

            if (path == null) {
                path = "";
            }

            // find the servlet which mount path is the longest prefix of the path info
            int index = path.length();
            Servlet servlet = null;
            while (servlet == null && index != -1) {
                path = path.substring(0, index);
                servlet = (Servlet) mountableServlets.get(path);
                index = path.lastIndexOf('/');
            }
            //case when servlet is mounted at "/" must be handled separately
            servlet = servlet == null ? (Servlet) mountableServlets.get("/") : servlet;
            if (servlet == null) {
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

        private Class[] getInterfaces(final Class clazz) {
            Set interfaces = new LinkedHashSet();
            getInterfaces(interfaces, clazz);
            return (Class[]) interfaces.toArray(new Class[interfaces.size()]);
        }

        public Map getBlockServletMap(HttpServletRequest request) {
            ApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(request.getSession(true).getServletContext());

            if (this.blockServletCollector == null || applicationContext.getStartupDate() != this.applicationContextStartDate) {
                this.applicationContextStartDate = applicationContext.getStartupDate();
                this.blockServletCollector = (Map) applicationContext.getBean("org.apache.cocoon.servletservice.spring.BlockServletMap");
            }

            return blockServletCollector;
        }
    }
}
