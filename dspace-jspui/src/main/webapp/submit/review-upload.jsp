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

<%@page import="org.dspace.core.ConfigurationManager"%>
<%@page import="org.dspace.authorize.AuthorizeServiceImpl"%>
<%@page import="org.dspace.authorize.ResourcePolicy"%>
<%@page import="java.util.List"%>
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
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>
<%@ page import="org.dspace.authorize.factory.AuthorizeServiceFactory" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
	
    boolean advanced = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

	//get the step number (for jump-to link)
	String stepJump = (String) request.getParameter("submission.jump");

    Item item = subInfo.getSubmissionItem().getItem();
	        
	//is advanced upload embargo step?
	Object isUploadWithEmbargoB = request.getAttribute("submission.step.uploadwithembargo");
	boolean isUploadWithEmbargo = false;
	if(isUploadWithEmbargoB!=null) {
	    isUploadWithEmbargo = (Boolean)isUploadWithEmbargoB;
	}
%>


<%-- ====================================================== --%>
<%--                    UPLOADED_FILES                      --%>
<%-- ====================================================== --%>
<div class="col-md-10">
                                    <div class="row">
                                        <span class="metadataFieldLabel col-md-4"><%= (subInfo.getSubmissionItem().hasMultipleFiles() ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload1") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.upload2")) %></span>
                                        <span class="metadataFieldValue col-md-8 break-all">
<%
    List<Bitstream> bitstreams = ContentServiceFactory.getInstance().getItemService().getNonInternalBitstreams(context, item);

	if(bitstreams.size() > 0)
	{
	    for (int i = 0; i < bitstreams.size() ; i++)
	    {
	        // Work out whether to use /retrieve link for simple downloading,
	        // or /html link for HTML files
	        BitstreamFormat format = bitstreams.get(i).getFormat(context);
	        String downloadLink = "retrieve/" + bitstreams.get(i).getID();
	        if (format != null && format.getMIMEType().equals("text/html"))
	        {
	            downloadLink = "html/db-id/" + item.getID();
	        }
%>
	                                            <a href="<%= request.getContextPath() %>/<%= downloadLink %>/<%= UIUtil.encodeBitstreamName(bitstreams.get(i).getName()) %>" target="_blank"><%= bitstreams.get(i).getName() %></a> - <%= bitstreams.get(i).getFormatDescription(context) %>
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
<%
if(isUploadWithEmbargo) {
List<ResourcePolicy> rpolicies = AuthorizeServiceFactory.getInstance().getAuthorizeService().findPoliciesByDSOAndType(context, bitstreams.get(i), ResourcePolicy.TYPE_CUSTOM); %>
<% if(rpolicies!=null && !rpolicies.isEmpty()) { %>
		<% int countPolicies = 0;
		   //show information about policies setting only in the case of advanced embargo form
		   if(advanced) {  
		       countPolicies = rpolicies.size();
		%>
			<% if(countPolicies>0) { %>		
				<i class="label label-info"><fmt:message key="jsp.submit.review.policies.founded"><fmt:param><%= countPolicies %></fmt:param></fmt:message></i>
			<% } %>
		<% } else { %>
				<% for(ResourcePolicy rpolicy : rpolicies) { 
						if(rpolicy.getStartDate()!=null) {
						%>
							<i class="label label-info"><fmt:message key="jsp.submit.review.policies.embargoed"><fmt:param><%= rpolicy.getStartDate() %></fmt:param></fmt:message></i>				    
						<%
						}
						else { 
						%>
							<i class="label label-success"><fmt:message key="jsp.submit.review.policies.openaccess"/></i>														    
					    <%
						}
					}
				%>
				
				
		<% } %>
<% } 
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
