<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show uploaded file (single-file submission mode)
  -
  - Attributes to pass in
  -    just.uploaded    - Boolean indicating whether the user has just
  -                       uploaded a file OK
  -    show.checksums   - Boolean indicating whether to show checksums
  -
  - FIXME: Merely iterates through bundles, treating all bit-streams as
  -        separate documents.  Shouldn't be a problem for early adopters.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
    
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="java.util.List" %>
<%@ page import="org.apache.commons.lang.time.DateFormatUtils" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.authorize.AuthorizeServiceImpl" %>
<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    boolean justUploaded = ((Boolean) request.getAttribute("just.uploaded")).booleanValue();
    boolean showChecksums = ((Boolean) request.getAttribute("show.checksums")).booleanValue();

    // Get the bitstream
    List<Bitstream> all = ContentServiceFactory.getInstance().getItemService().getNonInternalBitstreams(context, subInfo.getSubmissionItem().getItem());
    Bitstream bitstream = all.get(0);
    BitstreamFormat format = bitstream.getFormat(context);

    boolean withEmbargo = ((Boolean)request.getAttribute("with_embargo")).booleanValue();
%>


<dspace:layout style="submission"
			   locbar="off"
               navbar="off"
               titlekey="jsp.submit.show-uploaded-file.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

<%
    if (justUploaded)
    {
%>
        <%-- <h1>Submit: File Uploaded Successfully</h1> --%>
		<h1><fmt:message key="jsp.submit.show-uploaded-file.heading1"/>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\")+ \"#uploadedfile\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></h1>

        <%-- <p><strong>Your file was successfully uploaded.</strong></p> --%>
		<div class="alert aler-info"><fmt:message key="jsp.submit.show-uploaded-file.info1"/></div>
<%
    }
    else
    {
%>
        <%-- <h1>Submit: Uploaded File</h1> --%>
		<h1><fmt:message key="jsp.submit.show-uploaded-file.heading2"/>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\")+ \"#uploadedfile\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
		</h1>
<%
    }
%>
        <%-- <p>Here are the details of the file you have uploaded.  Please check the
        details before going to the next step.
        &nbsp;&nbsp;&nbsp;<dspace:popup page="/help/index.html#uploadedfile">(More Help...)</dspace:popup></p> --%>

		<div><fmt:message key="jsp.submit.show-uploaded-file.info2"/></div>

        <table class="table">
            <tr>
                <%-- <th class="oddRowOddCol">File</th>
                <th class="oddRowEvenCol">Size</th>
                <th class="oddRowOddCol">File Format</th> --%>
                
				<th id="t1" class="oddRowOddCol"><fmt:message key="jsp.submit.show-uploaded-file.file"/></th>
                <th id="t2" class="oddRowEvenCol"><fmt:message key="jsp.submit.show-uploaded-file.size"/></th>
                <th id="t3" class="oddRowOddCol"><fmt:message key="jsp.submit.show-uploaded-file.format"/></th>

<%
    if (showChecksums)
    {
%>
                <%-- <th class="oddRowEvenCol">Checksum</th> --%>
				<th id="t4" class="oddRowEvenCol"><fmt:message key="jsp.submit.show-uploaded-file.checksum"/></th>
<%
    }
%>
            </tr>
            <tr>
                <td headers="t1" class="evenRowOddCol">
                	<a href="<%= request.getContextPath() %>/retrieve/<%= bitstream.getID() %>/<%= org.dspace.app.webui.util.UIUtil.encodeBitstreamName(bitstream.getName()) %>" target="_blank"><%= bitstream.getName() %></a>
                	<%-- <input type="submit" name="submit_remove_<%= bitstream.getID() %>" value="Click here if this is the wrong file"> --%>
					<input class="btn btn-danger pull-right" type="submit" name="submit_remove_<%= bitstream.getID() %>" value="<fmt:message key="jsp.submit.show-uploaded-file.click2.button"/>" />
                </td>
                <td headers="t2" class="evenRowEvenCol"><fmt:message key="jsp.submit.show-uploaded-file.size-in-bytes">
                    <fmt:param><fmt:formatNumber><%= bitstream.getSize() %></fmt:formatNumber></fmt:param>
                </fmt:message></td>
                <td headers="t3" class="evenRowOddCol">
                    <%= bitstream.getFormatDescription(context) %>
<%    
    if (format.getSupportLevel() == 0)
    { %>
      <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\") +\"#unsupported\"%>">(<fmt:message key="jsp.submit.show-uploaded-file.notSupported"/>)</dspace:popup>
<%  }
    else if (format.getSupportLevel() == 1)
    { %>
      <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\") +\"#known\"%>">(<fmt:message key="jsp.submit.show-uploaded-file.known"/>)</dspace:popup>
<%  }
    else
    { %>
      <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\") +\"#supported\"%>">(<fmt:message key="jsp.submit.show-uploaded-file.supported"/>)</dspace:popup>
<%  } %>
               <%--  <input type="submit" name="submit_format_<%= bitstream.getID() %>" value="Click here if this is the wrong format" /> --%>
			    <input class="btn btn-default pull-right" type="submit" name="submit_format_<%= bitstream.getID() %>" value="<fmt:message key="jsp.submit.show-uploaded-file.click1.button"/>" />

                </td>
<%
    if (showChecksums)
    {
%>
                <td headers="t4" class="evenRowEvenCol">
                    <code><%= bitstream.getChecksum() %> (<%= bitstream.getChecksumAlgorithm() %>)</code>
                </td>
<%
    }
%>
            </tr>
        </table>

<%
    if (withEmbargo)
    {
%>
            <div class="row">
            	<input class="btn btn-primary col-md-2 col-offset-5" type="submit" name="submit_editPolicy_<%= bitstream.getID() %>" value="<fmt:message key="jsp.submit.show-uploaded-file.click3.button"/>" />
            </div>
<%
    }
%>
        <br/>

		<p class="uploadHelp"><fmt:message key="jsp.submit.show-uploaded-file.info3"/></p>
        <ul class="uploadHelp">
			<li class="uploadHelp"><fmt:message key="jsp.submit.show-uploaded-file.info4"/></li>
<%
    if (showChecksums)
    {
%>	
			<li class="uploadHelp"><fmt:message key="jsp.submit.show-uploaded-file.info5"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#checksum\"%>"><fmt:message key="jsp.submit.show-uploaded-file.info6"/></dspace:popup></li>
<%
    }
    else
    {
%>
  		<li class="uploadHelp"><fmt:message key="jsp.submit.show-uploaded-file.info7"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#checksum\"%>"><fmt:message key="jsp.submit.show-uploaded-file.info8"/></dspace:popup>
            <input class="btn btn-info" type="submit" name="submit_show_checksums" value="<fmt:message key="jsp.submit.show-uploaded-file.show.button"/>" /></li>
<%
    }
%>
        </ul>
        <br />

		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

<%-- HACK: Center used to align table; CSS and align="center" ignored by some browsers --%>

				<%  //if not first step, show "Previous" button
					if(!SubmissionController.isFirstStep(request, subInfo))
					{ %>
					<div class="col-md-6 pull-right btn-group">
						<input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
						<input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
						<input class="btn btn-primary col-md-4" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
						
				<%  } else { %>
					<div class="col-md-4 pull-right btn-group">
						<input class="btn btn-default col-md-6" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
					    <input class="btn btn-primary col-md-6" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
				<%  }  %>
					</div>
    </form>

</dspace:layout>
