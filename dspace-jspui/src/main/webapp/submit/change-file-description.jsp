<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
%>

<dspace:layout style="submission" locbar="off"
               navbar="off"
               titlekey="jsp.submit.change-file-description.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

        <%-- <h1>Submit: Change File Description</h1> --%>
		<h1><fmt:message key="jsp.submit.change-file-description.heading"/>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#filedescription\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
		</h1>

        <%-- <p>Here are the details of the file.  </p> --%>
		<div><fmt:message key="jsp.submit.change-file-description.info1"/></div>

        <table class="table" align="center" summary="Change file descripton details">
            <tr>
           <%-- <th class="oddRowOddCol">File</th>
                <th class="oddRowEvenCol">Size</th>
                <th class="oddRowOddCol">File Format</th> --%>
				<th id="t1" class="oddRowOddCol"><fmt:message key="jsp.submit.change-file-description.file"/></th>
                <th id="t2" class="oddRowEvenCol"><fmt:message key="jsp.submit.change-file-description.size"/></th>
                <th id="t3" class="oddRowOddCol"><fmt:message key="jsp.submit.change-file-description.format"/></th>
            </tr>
            <tr>
                <td headers="t1" class="evenRowOddCol"><%= subInfo.getBitstream().getName() %></td>
                <td headers="t2" class="evenRowEvenCol"><%= subInfo.getBitstream().getSize() %> bytes</td>
                <td headers="t3" class="evenRowOddCol"><%= subInfo.getBitstream().getFormatDescription(context) %></td>
            </tr>
        </table>

        <p class="alert alert-info"><fmt:message key="jsp.submit.change-file-description.info2"/></p>
<%
    String currentDesc = subInfo.getBitstream().getDescription();
    if (currentDesc == null)
    {
        currentDesc="";
    }
%>
		<div class="row">
			<label for="tdescription" class="col-md-2"><fmt:message key="jsp.submit.change-file-description.filedescr"/></label>
            <span class="col-md-10"><input class="form-control" type="text" name="description" id="tdescription" size="50" value="<%= currentDesc %>" /></span>
		</div><br/>

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
     
		<input class="btn btn-primary col-md-2 col-md-offset-5" type="submit" name="submit" value="<fmt:message key="jsp.submit.general.submit"/>" />
    </form>

</dspace:layout>
