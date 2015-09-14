<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Select type of uploaded file
  -
  - Attributes to pass in to this page:
  -    guessed.format     - the system's guess as to the format - null if it
  -                         doesn't know (BitstreamFormat)
  -    bitstream.formats  - the (non-internal) formats known by the system
  -                         (BitstreamFormat[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="java.util.List" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

	//retrieve attributes from request
    BitstreamFormat guess =
        (BitstreamFormat) request.getAttribute("guessed.format");
    List<BitstreamFormat> formats =
        (List<BitstreamFormat>) request.getAttribute("bitstream.formats");    

    Item item = subInfo.getSubmissionItem().getItem();
%>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.submit.get-file-format.title" nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

        <%-- <h1>Submit: Select File Format</h1> --%>
		<h1><fmt:message key="jsp.submit.get-file-format.heading"/>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#formats\" %>"><fmt:message key="jsp.morehelp"/></dspace:popup>
		</h1>

        <%-- <p>Uploaded file: <code><%= si.bitstream.getName() %></code> (<%= si.bitstream.getSize() %> bytes)</p> --%>
		<p><fmt:message key="jsp.submit.get-file-format.info1">
            <fmt:param><%= subInfo.getBitstream().getName() %></fmt:param>
            <fmt:param><%= String.valueOf(subInfo.getBitstream().getSize()) %></fmt:param>
        </fmt:message></p>

<%
    if (guess == null)
    {
%>
        <%-- <p>DSpace could not identify the format of this file.</p> --%>
		<p class="alert alert-info"><fmt:message key="jsp.submit.get-file-format.info2"/></p>
<%
    }
    else
    {
%>
        <%-- <p>DSpace recognized the file format as <%= guess.getShortDescription() %>.
        <strong>Please be sure before you change this!</strong></p> --%>
		<p class="alert alert-info"><fmt:message key="jsp.submit.get-file-format.info3">
            <fmt:param><%= guess.getShortDescription() %></fmt:param>
        </fmt:message></p>   
        <input type="hidden" name="format" value="<%= guess.getID() %>" />

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <%-- <p align="center"><input type="submit" name="submit" value="Choose automatically-recognized type"></p> --%>
		<p align="center"><input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.submit.get-file-format.choose.button"/>" /></p>
    </form>

<%-- Option list put in a separate form --%>
    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">
<%
    }
%>

        <%-- <p>Select the format of the file from the list below, for example "Adobe
        PDF" or "Microsoft Word", <strong>OR</strong> if the format is not in the list, please describe
        the format file in the input box below the list.</p> --%>

		<div><fmt:message key="jsp.submit.get-file-format.info5"/></div>
    	<div class="row">
    	<span class="col-md-6">
            <select class="form-control" name="format" size="8">
                <option value="-1" <%= subInfo.getBitstream().getFormat(context).getShortDescription().equals("Unknown") ? "selected=\"selected\"" : "" %>>
                    <%-- Format Not in List --%>
					<fmt:message key="jsp.submit.get-file-format.info6"/>
                </option>
<%
    for (int i = 0; i < formats.size(); i++)
    {
%>
                <option
                    <%= subInfo.getBitstream().getFormat(context).getID() == formats.get(i).getID() ? "selected=\"selected\"" : "" %>
                    value="<%= formats.get(i).getID() %>">
                   <%= formats.get(i).getShortDescription() %>
<%-- <%
        if (formats.get(i).getSupportLevel() == 1) { %>(known)<% }
        if (formats.get(i).getSupportLevel() == 2) { %>(supported)<% } 
      %> --%>
<%
        if (formats.get(i).getSupportLevel() == 1) { %><fmt:message key="jsp.submit.get-file-format.known"/><% }
        if (formats.get(i).getSupportLevel() == 2) { %><fmt:message key="jsp.submit.get-file-format.supported"/><% }
%>
                </option>
<%
    }
%>
            </select>
    </span>
       <%--  <p class=submitFormHelp><strong>If the format is not in the above list</strong>, describe
        it in the format below.  Enter the name of the application you used to create
        the file, and the version number of the application (for example,
        "ACMESoft SuperApp version 1.5").</p> --%>
		 <div class="col-md-6"><p class="submitFormHelp alert alert-warning"><fmt:message key="jsp.submit.get-file-format.info7"/></p>

		
                    <%-- File Format: --%>
					<label for="tformat_description" class="col-md-3"><fmt:message key="jsp.submit.get-file-format.format"/></label>
<%
    String desc = subInfo.getBitstream().getUserFormatDescription();
    if (desc == null)
    {
        desc = "";
    }
%>
                   <span class="col-md-9"><input class="form-control" type="text" name="format_description" id="tformat_description" size="40" value="<%= desc %>" /></span>
		</div>
	</div><br/>
        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <%-- <center><p><input type="submit" name="submit" value="Set File Format"></p></center> --%>
		<input class="btn btn-primary col-md-2 col-md-offset-5" type="submit" name="submit" value="<fmt:message key="jsp.submit.general.submit"/>" />
    </form>
</dspace:layout>
