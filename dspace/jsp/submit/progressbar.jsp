<%--
  - progressbar.jsp
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
  - Progress bar for submission form.  Note this must be included within
  - the FORM element in the submission form, since it contains navigation
  - buttons.
  -
  - Parameters:
  -    step (int):  The current step.  One of constants in
  -                 org.dspace.app.webui.servlets.SubmitServlet.
  -    stage_reached:  The most advanced step the user has reached (i.e. the
  -                    most advanced step they can jump to)
  -                    One of constants in org.dspace.servlets.SubmitServlet.
  -                    It can also be "-1" - in this case, the progress bar
  -                    is in "Workflow Item" mode - i.e. the "license" and
  -                    "complete" stages aren't shown
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.license.CreativeCommons" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    // These are the filename stubs for the images used for each step
    String[] imageNames =
    {
        "select",
        "describe",
        "describe",
        "describe",
        "upload",
        "verify",
        "license",
        "license",
        "complete"
    };

    // Step names for ALT text in images
    String[] stepNames =
    {
        "Select",
        "Describe",
        "Describe",
        "Describe",
        "Upload",
        "Verify",
        "License",
        "License",
        "Complete"
    };

    int step = Integer.parseInt(request.getParameter("current_stage"));
    int stageReached = Integer.parseInt(request.getParameter("stage_reached"));

    // Are we in workflow mode?
    boolean workflowMode = false;
    if (stageReached == -1)
    {
        workflowMode = true;
        stageReached = SubmitServlet.REVIEW_SUBMISSION;
    }
%>
<center>
    <table class=submitProgressTable border=0 cellspacing=0 cellpadding=0>
        <tr>
<%
    // Show previous (done by definition!) steps
    for (int i = 1; i < step; i++)
    {
        // Hack for skipping CC step if not enabled
        if (!CreativeCommons.isEnabled() && i==SubmitServlet.CC_LICENSE)
        {
          continue;
        }
        // If the step has been done, and we're not on the final step,
        // the user can jump back
        if (step != SubmitServlet.SUBMISSION_COMPLETE)
        {
    %>
            <%-- HACK: border=0 for non-CSS compliant Netscape 4.x --%>
            <td><input class="submitProgressButton" border=0 type=image name="submit_jump_<%= i %>" src="<%= request.getContextPath() %>/image/submit/<%= imageNames[i] %>-done.gif" value=" <%= stepNames[i] %> (Done) - " alt=" <%= stepNames[i] %> (Done) - "></td>
    <%
        }
        else
        {
            // User has reached final step, cannot jump back
    %>
            <td><IMG SRC="<%= request.getContextPath() %>/image/submit/<%= imageNames[i] %>-done.gif" ALT=" <%= stepNames[i] %> (Done) - "></td>
    <%
        }
    }

    // Show current step, but only if it's not the select collection step,
    // which isn't shown in the progress bar
    if (step > 0)
    {
%>
            <td><IMG SRC="<%= request.getContextPath() %>/image/submit/<%= imageNames[step] %>-current.gif" ALT=" <%= stepNames[step] %> (Current) - "></td>
<%
    }
    
    // We only go up to the "verify" step if we're on a workflow item
    int lastStep = (workflowMode ? SubmitServlet.REVIEW_SUBMISSION+1
                                 : stepNames.length);

    // Show next steps (some of which may have been done)
    for (int i = step + 1; i < lastStep; i++)
    {
        // Hack for skipping CC step if not enabled
        if (!CreativeCommons.isEnabled() && i==SubmitServlet.CC_LICENSE)
        {
          continue;
        }
        if (i <= stageReached)
        {
            // Stage has been previously accessed, so user may jump to it
%>
<%-- HACK: border=0 for non-CSS compliant Netscape 4.x --%>
            <td><input class="submitProgressButton" border=0 type=image name="submit_jump_<%= i %>" src="<%= request.getContextPath() %>/image/submit/<%= imageNames[i] %>-done.gif" value=" <%= stepNames[i] %> (Done) - " alt=" <%= stepNames[i] %> (Done) - "></td>
<%
        }
        else
        {
            // Stage hasn't been reached yet (can't be jumped to)
%>
            <td><IMG SRC="<%= request.getContextPath() %>/image/submit/<%= imageNames[i] %>-notdone.gif" ALT=" <%= stepNames[i] %> (Not Done) - "></td>
<%
        }
    }
%>
        </tr>
    </table>
</center>
