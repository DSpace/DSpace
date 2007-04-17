<%--
  - cancel.jsp
  -
  - Version: $Revision: 1.7 $
  -
  - Date: $Date: 2005/02/07 20:32:04 $
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
  - Cancel or save submission page
  -
  - This page is displayed whenever the user clicks "cancel" during a
  - submission.  It asks them whether they want to delete the incomplete
  - item or leave it so they can continue later.
  -
  - Attributes to pass in:
  -    submission.info  - the SubmissionInfo object
  -    step             - the step the user was at when the cancelled
  -                       (as a String)
  -    display.step -   - this is the step to display in the progress bar
  -                       (i.e. the step from the user's perspective, rather
  -                       than the exact JSP the user clicked cancel on)
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    String step = (String) request.getAttribute("step");
    String displayStep = (String) request.getAttribute("display.step");

    if (displayStep==null) displayStep = step;
%>

<dspace:layout locbar="off" navbar="off" title="Cancel or Save Submission" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method=post>

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= displayStep %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>

        <H1>Cancel or Save Submission</H1>

        <P>Do you want the half-finished submission removed, or do you want to keep
        it so you can carry on working on it later?  You can also return to where
        you where during the submission process if you clicked Cancel by accident.</P>
    
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=previous_step value=<%= step %>>
        <input type=hidden name=step value=<%= SubmitServlet.SUBMISSION_CANCELLED %>>

        <table align=center border=0 width=90%>
            <tr>
                <td align=left>
                    <input type=submit name="submit_back" value="Oops, continue submission">
                </td>
                <td align=center>
                    <input type=submit name="submit_remove" value="Remove the submission">
                </td>
                <td align=right>
                    <input type=submit name="submit_keep" value="Save it, I'll work on it later">
                </td>
            </tr>
        </table>
        
    </form>

</dspace:layout>
