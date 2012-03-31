<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an internal server error
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ page import="java.io.PrintWriter" %>

<%@ page isErrorPage="true" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.error.internal.title">
    <%-- <h1>Internal System Error</h1> --%>
    <h1><fmt:message key="jsp.error.internal.title"/></h1>
    <%-- <p>Oops!  The system has experienced an internal error.  This is our fault,
    please pardon our dust during these early stages of the DSpace system!</p> --%>
    <p><fmt:message key="jsp.error.internal.text1"/></p>
    <%-- <p>The system has logged this error.  Please try to do what you were doing
    again, and if the problem persists, please contact us so we can fix the
    problem.</p> --%>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <%-- <a href="<%= request.getContextPath() %>/">Go to the DSpace home page</a> --%>
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>
        <!--
    <%
    Throwable ex = (Throwable) request.getAttribute("javax.servlet.error.exception");
    if(ex == null) out.println("No stack trace available<br/>");
    else {
                for(Throwable t = ex ; t!=null; t = t.getCause())
                {
                    out.println(t.getMessage());
                    out.println("=============================================");
                    t.printStackTrace(new PrintWriter(out));
                    out.println("\n\n\n");
                }
        }
        %>
      -->
</dspace:layout>
