<%--
  - edit-metadata-2.jsp
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
  - Edit metadata form page 2
  -
  - Attributes to pass in to this page:
  -    submission.info  - the SubmissionInfo object
  --%>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");

    Item item = si.submission.getItem();

    final int formWidth = 50;
    final int subjectWidth = 15;
%>

<dspace:layout locbar="nolink" navbar="off" parenttitle="Submit"
title="Describe Your Item">

    <form action="<%= request.getContextPath() %>/submit#field" method=post>

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="step" value="<%= SubmitServlet.EDIT_METADATA_2 %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
        </jsp:include>

        <H1>Submit: Describe Your Item</H1>
    
        <P>Please fill further information about your submission below. <A TARGET="dspace.help" HREF="<%= request.getContextPath() %>/help/index.html#describe3">(More Help...)</A></P>

<%-- HACK: a <center> tag seems to be the only way to convince certain --%>
<%--       browsers to center the table. --%>
        <center>
            <table>

<%-- ================================================ --%>
<%--                  Subject keywords                --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=3 class="submitFormHelp">
<%
    if (si.jumpToField != null && si.jumpToField.equals("subject"))
    {
%>
                        <a name="field"></a>
<%
    }
%>
                        Enter appropriate subject keywords or phrases below.
                    </td>
                </tr>
<%
    DCValue[] subjects = item.getDC("subject", null, Item.ANY);
    int subjectFieldCount = subjects.length + 1;
    
    if (si.moreBoxesFor != null && si.moreBoxesFor.equals("subject"))
    {
        subjectFieldCount += 2;
    }

    for (int i = 0; i < subjectFieldCount; i += 2)
    {
%>
                <tr>
<%-- HACK: nowrap used since browsers do not act on "white-space" CSS property --%>
<%
        if (i == 0)
        {
%>
                    <td nowrap class="submitFormLabel">Subject Keywords</td>
<%
        }
        else
        {
%>
                    <td>&nbsp;</td>
<%
        }
%>
                    <td align="left">
<%
        if (i < subjects.length)
        {
%>
                        <input type=text name="subject_<%= i %>" size="<%= subjectWidth %>"
                            value="<%= subjects[i].value %>">&nbsp;<input type=submit name="submit_subject_remove_<%= i %>" value="Remove">
<%
        }
        else
        {
%>
                        <input type=text name="subject_<%= i %>" size="<%= subjectWidth %>">
<%
        }
%>
                    </td>
                    <td align="left">&nbsp;&nbsp;
<%
        if (i + 1 < subjects.length)
        {
%>
                        <input type=text name="subject_<%= i + 1 %>" size="<%= subjectWidth %>"
                            value="<%= subjects[i + 1].value %>">&nbsp;<input type=submit name="submit_subject_remove_<%= i + 1 %>" value="Remove">
<%
        }
        else if (i + 2 >= subjectFieldCount)
        {
%>
                        <input type=text name="subject_<%= i + 1 %>" size="<%= subjectWidth %>">&nbsp;<input type=submit name=submit_subject_more value="Add More">
<%
        }
        else
        {
%>
                        <input type=text name="subject_<%= i + 1 %>" size="<%= subjectWidth %>">
<%
        }
%>
                    </td>
                </tr>
<%
    }
%>
                <tr>
                    <td colspan=3>&nbsp;</td>
                </tr>


<%-- ================================================ --%>
<%--                  Abstract                        --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=3 class="submitFormHelp">
                        Enter the abstract of the item below.
                    </td>
                </tr>
<%
    // FIXME: (Maybe) assume one abstract
    DCValue[] abstrArray = item.getDC("description", "abstract", Item.ANY);
    String abstr = (abstrArray.length > 0 ? abstrArray[0].value : "");
%>
                <tr>
                    <td class="submitFormLabel">Abstract</td>
                    <td colspan=2>
                        <textarea rows=8 cols=<%= formWidth %> name=description_abstract wrap=soft><%= abstr %></textarea>
                    </td>
                </tr>
                <tr>
                    <td colspan=3>&nbsp;</td>
                </tr>


<%-- ================================================ --%>
<%--                  Sponsorship                     --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=3 class="submitFormHelp">
                        Enter the names of any sponsors and/or funding codes in the box below.
                    </td>
                </tr>
<%
    DCValue[] sponsorArray = item.getDC("description", "sponsorship", Item.ANY);
    String sponsorship = (sponsorArray.length > 0 ? sponsorArray[0].value : "");
%>
                <tr>
                    <td class="submitFormLabel">Sponsors</td>
                    <td colspan=2>
                        <textarea rows=3 cols=<%= formWidth %> name=description_sponsorship wrap=soft><%= sponsorship %></textarea>
                    </td>
                </tr>
                <tr>
                    <td colspan=3>&nbsp;</td>
                </tr>


<%-- ================================================ --%>
<%--                Other Description                 --%>
<%-- ================================================ --%>
                <tr>
                    <td colspan=3 class="submitFormHelp">
                        Enter any other description or comments relating to the item in this box.
                    </td>
                </tr>
<%
    DCValue[] descArray = item.getDC("description", null, Item.ANY);
    String desc = (descArray.length > 0 ? descArray[0].value : "");
%>
                <tr>
                    <td class="submitFormLabel">Description</td>
                    <td colspan=2>
                        <textarea rows=3 cols=<%= formWidth %> name=description wrap=soft><%= desc %></textarea>
                    </td>
                </tr>

            </table>
        </center>
        
        
<%-- HACK:  Need a space - is there a nicer way to do this than <BR> or a --%>
<%--        blank <P>? --%>
        <P>&nbsp;</P>

<%-- Hidden fields needed for submit servlet to know which item to deal with --%>
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type=hidden name=step value=<%= SubmitServlet.EDIT_METADATA_2 %>>
        <center>
            <table border=0 width=80%>
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <input type=submit name=submit_prev value="&lt; Previous">
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

