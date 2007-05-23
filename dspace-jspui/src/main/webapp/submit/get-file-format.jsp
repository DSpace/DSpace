<%--
  - get-file-format.jsp
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
  - Select type of uploaded file
  -
  - Attributes to pass in to this page:
  -    submission.info    - the SubmissionInfo object
  -    guessed.format     - the system's guess as to the format - null if it
  -                         doesn't know (BitstreamFormat)
  -    bitstream.formats  - the (non-internal) formats known by the system
  -                         (BitstreamFormat[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.SubmitServlet" %>
<%@ page import="org.dspace.app.webui.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.content.Item" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    SubmissionInfo si =
        (SubmissionInfo) request.getAttribute("submission.info");
    BitstreamFormat guess =
        (BitstreamFormat) request.getAttribute("guessed.format");
    BitstreamFormat[] formats =
        (BitstreamFormat[]) request.getAttribute("bitstream.formats");    

    Item item = si.submission.getItem();
%>

<dspace:layout locbar="off" navbar="off" titlekey="jsp.submit.get-file-format.title" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post">

        <jsp:include page="/submit/progressbar.jsp">
            <jsp:param name="current_stage" value="<%= SubmitServlet.UPLOAD_FILES %>"/>
            <jsp:param name="stage_reached" value="<%= SubmitServlet.getStepReached(si) %>"/>
            <jsp:param name="md_pages" value="<%= si.numMetadataPages %>"/>
        </jsp:include>

        <%-- <h1>Submit: Select File Format</h1> --%>
		<h1><fmt:message key="jsp.submit.get-file-format.heading"/></h1>

        <%-- <p>Uploaded file: <code><%= si.bitstream.getName() %></code> (<%= si.bitstream.getSize() %> bytes)</p> --%>
		<p><fmt:message key="jsp.submit.get-file-format.info1">
            <fmt:param><%= si.bitstream.getName() %></fmt:param>
            <fmt:param><%= String.valueOf(si.bitstream.getSize()) %></fmt:param>
        </fmt:message></p>

<%
    if (guess == null)
    {
%>
        <%-- <p>DSpace could not identify the format of this file.</p> --%>
		<p><fmt:message key="jsp.submit.get-file-format.info2"/></p>
<%
    }
    else
    {
%>
        <%-- <p>DSpace recognized the file format as <%= guess.getShortDescription() %>.
        <strong>Please be sure before you change this!</strong></p> --%>
		<p><fmt:message key="jsp.submit.get-file-format.info3">
            <fmt:param><%= guess.getShortDescription() %></fmt:param>
        </fmt:message></p>   
        <input type="hidden" name="format" value="<%= guess.getID() %>" />
        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type="hidden" name="step" value="<%= SubmitServlet.GET_FILE_FORMAT %>" />
        <%-- <p align="center"><input type="submit" name="submit" value="Choose automatically-recognized type"></p> --%>
		<p align="center"><input type="submit" name="submit" value="<fmt:message key="jsp.submit.get-file-format.choose.button"/>" /></p>
    </form>

<%-- Option list put in a separate form --%>
    <form action="<%= request.getContextPath() %>/submit" method="post">
<%
    }
%>

        <%-- <p>Select the format of the file from the list below, for example "Adobe
        PDF" or "Microsoft Word", <strong>OR</strong> if the format is not in the list, please describe
        the format file in the input box below the list.
        <dspace:popup page="/help/index.html#formats">(More Help...)</dspace:popup></p> --%>

		<div><fmt:message key="jsp.submit.get-file-format.info5"/>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#formats\" %>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>
    
        <center>
            <select name="format" size="8">
                <option value="-1" <%= si.bitstream.getFormat().getShortDescription().equals("Unknown") ? "selected=\"selected\"" : "" %>>
                    <%-- Format Not in List --%>
					<fmt:message key="jsp.submit.get-file-format.info6"/>
                </option>
<%
    for (int i = 0; i < formats.length; i++)
    {
%>
                <option
                    <%= si.bitstream.getFormat().getID() == formats[i].getID() ? "selected=\"selected\"" : "" %>
                    value="<%= formats[i].getID() %>">
                   <%= formats[i].getShortDescription() %>
<%-- <%
        if (formats[i].getSupportLevel() == 1) { %>(known)<% }
        if (formats[i].getSupportLevel() == 2) { %>(supported)<% } 
      %> --%>
<%
        if (formats[i].getSupportLevel() == 1) { %><fmt:message key="jsp.submit.get-file-format.known"/><% }
        if (formats[i].getSupportLevel() == 2) { %><fmt:message key="jsp.submit.get-file-format.supported"/><% }
%>
                </option>
<%
    }
%>
            </select>
        </center>
    
       <%--  <p class=submitFormHelp><strong>If the format is not in the above list</strong>, describe
        it in the format below.  Enter the name of the application you used to create
        the file, and the version number of the application (for example,
        "ACMESoft SuperApp version 1.5").</p> --%>
		 <p class="submitFormHelp"><fmt:message key="jsp.submit.get-file-format.info7"/></p>

        <table border="0" align="center">
            <tr>
                <td class="submitFormLabel">
                    <%-- File Format: --%>
					<label for="tformat_description"><fmt:message key="jsp.submit.get-file-format.format"/></label>
                </td>
                <td>
<%
    String desc = si.bitstream.getUserFormatDescription();
    if (desc == null)
    {
        desc = "";
    }
%>
                   <input type="text" name="format_description" id="tformat_description" size="40" value="<%= desc %>" />
                </td>
            </tr>
        </table>

        <%= SubmitServlet.getSubmissionParameters(si) %>
        <input type="hidden" name="step" value="<%= SubmitServlet.GET_FILE_FORMAT %>" />

        <%-- <center><p><input type="submit" name="submit" value="Set File Format"></p></center> --%>
		<center><p><input type="submit" name="submit" value="<fmt:message key="jsp.submit.general.submit"/>" /></p></center>
    </form>
</dspace:layout>
