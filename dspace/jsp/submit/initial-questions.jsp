<%--
  - initial-questions.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
  - Initial questions for keeping UI as simple as possible.
  -
  - Attributes to pass in:
  -    submission.info    - the SubmissionInfo object
  --%>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");
%>

<dspace:layout locbar="off" navbar="off" title="Describe Your Item">

    <form action="<%= request.getContextPath() %>/submit" method=post>

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.INITIAL_QUESTIONS %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
        </jsp:include>

        <H1>Submit: Describe Your Item</H1>
    
        <P>Please check the boxes next to the statements that apply to your
        submission.
        <dspace:popup page="/help/index.html#describe1">(More Help...)</dspace:popup></P>

        <center>
            <table class="miscTable">
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border=0>
                            <tr>
                                <td valign=top><input type=checkbox name="multiple_titles" value="true" <%= (si.submission.hasMultipleTitles() ? "CHECKED" : "") %>></td>
                                <td class="submitFormLabel" nowrap>The item has more than one title, e.g. a translated title</td>
                            </tr>
                        </table>
                    </td>
                </tr>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border=0>
                            <tr>
                                <td valign=top><input type=checkbox name="published_before" value="true" <%= (si.submission.isPublishedBefore() ? "CHECKED" : "") %>></td>
                                <td class="submitFormLabel" nowrap>The item has been published or publicly distributed before</td>
                            </tr>
                        </table>
                    </td>
                </tr>
<%
    // Don't display file or thesis questions in workflow mode
    if (!SubmitServlet.isWorkflow(si))
    {
%>
                <tr class="oddRowOddCol">
                    <td class="oddRowOddCol" align="left">
                        <table border=0>
                            <tr>
                                <td valign=top><input type=checkbox name="multiple_files" value="true" <%= (si.submission.hasMultipleFiles() ? "CHECKED" : "") %>></td>
                                <td class="submitFormLabel" nowrap>The item consists of <em>more than one</em> file</td>
                            </tr>
                        </table>
                    </td>
                </tr>
<%
        if (ConfigurationManager.getBooleanProperty("webui.submit.blocktheses"))
        {
%>
                <tr class="evenRowOddCol">
                    <td class="evenRowOddCol" align="left">
                        <table border=0>
                            <tr>
                                <td valign=top><input type=checkbox name="is_thesis" value="true"></td>
                                <td class="submitFormLabel" nowrap>The item is a thesis</td>
                            </tr>
                        </table>
                    </td>
                </tr>
<%
        }
    }
%>
            </table>
        </center>

        <P>&nbsp;</P>

<%-- Hidden fields needed for submit servlet to know which item to deal with --%>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=step value=<%= SubmitServlet.INITIAL_QUESTIONS %>>
        <center>
            <table border=0 width="80%">
                <tr>
                    <td width="100%">
                        &nbsp;
                    </td>
                    <td>
                        <input type=submit name=submit_next value="Next &gt;">
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align=right>
                        <input type=submit name=submit_cancel value="Cancel/Save">
                    </td>
                </tr>
            </table>
        </center>
    </form>

</dspace:layout>
