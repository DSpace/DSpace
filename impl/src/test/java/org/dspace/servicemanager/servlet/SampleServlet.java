/**
 * $Id: SampleServlet.java 3523 2009-03-05 14:58:10Z azeckoski $
 * $URL: https://scm.dspace.org/svn/repo/dspace2/core/trunk/impl/src/test/java/org/dspace/servicemanager/servlet/SampleServlet.java $
 * Example.java - entity-broker - 31 May 2007 7:01:11 PM - azeckoski
 **************************************************************************
 * Copyright (c) 2007, 2008 Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dspace.servicemanager.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.kernel.DSpaceKernel;
import org.dspace.kernel.DSpaceKernelManager;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.RequestService;
import org.dspace.services.SessionService;

/**
 * Test servlet for trying out the jetty server
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class SampleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

    private transient SessionService sessionService;
    private transient RequestService requestService;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            DSpaceKernel kernel = new DSpaceKernelManager().getKernel();
            if (kernel == null) {
                throw new IllegalStateException("Could not get the DSpace Kernel");
            }
            if (! kernel.isRunning()) {
                throw new IllegalStateException("DSpace Kernel is not running, cannot startup the DirectServlet");
            }
            ServiceManager serviceManager = kernel.getServiceManager();
            sessionService = serviceManager.getServiceByName(SessionService.class.getName(), SessionService.class);
            if (sessionService == null) {
                throw new IllegalStateException("Could not get the DSpace SessionService");
            }
            requestService = serviceManager.getServiceByName(RequestService.class.getName(), RequestService.class);
            if (requestService == null) {
                throw new IllegalStateException("Could not get the DSpace RequestService");
            }
            System.out.println("Servlet initialized");
        } catch (Exception e) {
            throw new IllegalStateException("FAILURE during init of direct servlet: " + e.getMessage(), e);
        }
    }

    /**
     * Now this will handle all kinds of requests and not just post and get
     */
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse res)
                throws ServletException, IOException {
        // force all response encoding to UTF-8 / html by default
        res.setContentType("text/html");
        res.setCharacterEncoding("UTF-8");

        // now handle the request
        PrintWriter writer = res.getWriter();
        writer.print(XML_HEADER);
        writer.print(XHTML_HEADER);
        writer.print("DSpaceTest:session=" + sessionService.getCurrentSessionId() + ":request=" + requestService.getCurrentRequestId());
        writer.print(XHTML_FOOTER);
        res.setStatus(HttpServletResponse.SC_OK);

        System.out.println("Serviced request:  DSpace");
    }

    protected static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n";
    protected static final String XHTML_HEADER = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" " +
    "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
    "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
    "<head>\n" +
    "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
    "  <title>DSpace title</title>\n" +
    "</head>\n" +
    "<body>\n";
    // include versions info in the footer now
    protected static final String XHTML_FOOTER = "\n</body>\n</html>\n";

}
