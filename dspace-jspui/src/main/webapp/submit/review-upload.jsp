<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Review file upload info
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

	//get the step number (for jump-to link)
	String stepJump = (String) request.getParameter("submission.jump");

    Item item = subInfo.getSubmissionItem().getItem();
%>


<%-- ====================================================== --%>
<%--                    UPLOADED_FILES                      --%>
<%-- ====================================================== --%>
<div class="col-md-10">
                                    <div class="row">
                                        <span class="metadataFieldLabel col-md-4"><%= (subInfo.getSubmissionItem().hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload2")) %></span>
                                        <span class="metadataFieldValue col-md-8">
<%
    Bitstream[] bitstreams = item.getNonInternalBitstreams();

	if(bitstreams.length > 0)
	{
	    for (int i = 0; i < bitstreams.length ; i++)
	    {
	        // Work out whether to use /retrieve link for simple downloading,
	        // or /html link for HTML files
	        BitstreamFormat format = bitstreams[i].getFormat();
	        String downloadLink = "retrieve/" + bitstreams[i].getID();
	        if (format != null && format.getMIMEType().equals("text/html"))
	        {
	            downloadLink = "html/db-id/" + item.getID();
	        }
%>
	                                            <a href="<%= request.getContextPath() %>/<%= downloadLink %>/<%= UIUtil.encodeBitstreamName(bitstreams[i].getName()) %>" target="_blank"><%= bitstreams[i].getName() %></a> - <%= bitstreams[i].getFormatDescription() %>
<%
	        switch (format.getSupportLevel())
	        {
	        case 0:
	            %><fmt:message key="jsp.submit.review.unknown"/><%
	            break;
	        case 1:
	            %><fmt:message key="jsp.submit.review.known"/><%
	            break;
	        case 2:
	            %><fmt:message key="jsp.submit.review.supported"/><%
	        }
%>        
	                                            <br />
<%
	    }
	}
	else { //otherwise, no files uploaded
%>
		<fmt:message key="jsp.submit.review.no_md"/>
<%		
	}
%>
                                        </span>
                                    </div>
                                </div>    
                            <div class="col-md-2">
<%
    // Can't edit files in workflow mode
    if(!subInfo.isInWorkflow())
    {
%>
                                    <input class="btn btn-default" type="submit" name="submit_jump_<%=stepJump%>"
                                     value="<%= (subInfo.getSubmissionItem().hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.button.upload1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.button.upload2")) %>" />
<%
    }
    else
    {
%>

                                    <input class="btn btn-default" type="submit" name="submit_jump_<%=stepJump%>"
                                     value="<fmt:message key="jsp.submit.review.button.edit"/>" />
<%
    }
%>
                  </div>