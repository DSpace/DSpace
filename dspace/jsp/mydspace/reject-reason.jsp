<%--
  - reject-reason.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
  --%>

<%--
  - Get a reason for rejecting a submission (or just reject reason itself -
  -  this is JSP programming after all!)
  -
  - Attributes:
  -    workflow.item: The workflow item being rejected
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>

<%
    WorkflowItem workflowItem =
        (WorkflowItem) request.getAttribute("workflow.item");
%>

<dspace:layout locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               title="reject-reason.title"
               nocache="true">

    <%-- <h1>Enter Reason for Rejection</h1> --%>
	<h1><fmt:message key="jsp.mydspace.reject-reason.title"/></h1>

    <%-- <p>Please enter the reason you are rejecting the submission into the box
    below.  Please indicate in your message whether the submitter should fix
    a problem and resubmit.</p> --%>
	<p><fmt:message key="jsp.mydspace.reject-reason.text1"/></p>
    
    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.REJECT_REASON_PAGE %>"/>
        <center>
            <textarea rows="6" cols="50" name="reason"></textarea>
        </center>

        <table border="0" width="90%" cellpadding="10" align="center">
            <tr>
                <td align="left">
                    <%-- <input type="submit" name="submit_send" value="Reject Item" /> --%>
					 <input type="submit" name="submit_send" value="<fmt:message key="jsp.mydspace.reject-reason.reject.button"/>" />
                </td>
                <td align="right">
                    <%-- <input type="submit" name="submit_cancel" value="Cancel Rejection" /> --%>
					<input type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.reject-reason.cancel.button"/>" />
                </td>
            </tr>
        </table>
    </form>
</dspace:layout>
