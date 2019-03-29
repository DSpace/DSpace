<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - List of uploaded files
  -
  - Attributes to pass in to this page:
  -   just.uploaded     - Boolean indicating if a file has just been uploaded
  -                       so a nice thank you can be displayed.
  -   show.checksums    - Boolean indicating whether to show checksums
  -
  - FIXME: Assumes each bitstream in a separate bundle.
  -        Shouldn't be a problem for early adopters.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="java.util.List" %>
<%@ page import="org.apache.commons.lang.time.DateFormatUtils" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.authorize.factory.AuthorizeServiceFactory" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    boolean justUploaded = ((Boolean) request.getAttribute("just.uploaded")).booleanValue();
    boolean showChecksums = ((Boolean) request.getAttribute("show.checksums")).booleanValue();
    
    request.setAttribute("LanguageSwitch", "hide");
    boolean allowFileEditing = !subInfo.isInWorkflow() || ConfigurationManager.getBooleanProperty("workflow", "reviewer.file-edit");

    boolean withEmbargo = ((Boolean)request.getAttribute("with_embargo")).booleanValue();

    List<ResourcePolicy> policies = null;
    String startDate = "";
    String globalReason = "";
    if (withEmbargo)
    {
        // Policies List
        policies = AuthorizeServiceFactory.getInstance().getAuthorizeService().findPoliciesByDSOAndType(context, subInfo.getSubmissionItem().getItem(), ResourcePolicy.TYPE_CUSTOM);
    
        startDate = "";
        globalReason = "";
        if (policies.size() > 0)
        {
            startDate = (policies.get(0).getStartDate() != null ? DateFormatUtils.format(policies.get(0).getStartDate(), "yyyy-MM-dd") : "");
            globalReason = policies.get(0).getRpDescription();
        }
    }

    boolean isAdvancedForm = ConfigurationManager.getBooleanProperty("webui.submission.restrictstep.enableAdvancedForm", false);

%>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.submit.upload-file-list.title">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

<%--        <h1>Submit: <%= (justUploaded ? "File Uploaded Successfully" : "Uploaded Files") %></h1> --%>
    
<%
    if (justUploaded)
    {
%>
		<h1><fmt:message key="jsp.submit.upload-file-list.heading1"/>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#uploadedfile\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
		</h1>
        <p><fmt:message key="jsp.submit.upload-file-list.info1"/></p>
<%
    }
    else
    {
%>
	    <h1><fmt:message key="jsp.submit.upload-file-list.heading2"/>
	    <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#uploadedfile\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
	    </h1>
<%
    }
%>
        <div><fmt:message key="jsp.submit.upload-file-list.info2"/></div>
        
        <table class="table" align="center" summary="Table dispalying your submitted files">
            <tr>
				<th id="t1" class="oddRowEvenCol"><fmt:message key="jsp.submit.upload-file-list.tableheading1"/></th>
                <th id="t2" class="oddRowOddCol"><fmt:message key="jsp.submit.upload-file-list.tableheading2"/></th>
                <th id="t3" class="oddRowEvenCol"><fmt:message key="jsp.submit.upload-file-list.tableheading3"/></th>
                <th id="t4" class="oddRowOddCol"><fmt:message key="jsp.submit.upload-file-list.tableheading4"/></th>
                <th id="t5" class="oddRowEvenCol"><fmt:message key="jsp.submit.upload-file-list.tableheading5"/></th>
<%
    String headerClass = "oddRowEvenCol";

    if (showChecksums)
    {
        headerClass = (headerClass == "oddRowEvenCol" ? "oddRowOddCol" : "oddRowEvenCol");
%>

                <th id="t6" class="<%= headerClass %>"><fmt:message key="jsp.submit.upload-file-list.tableheading6"/></th>
<%
    }

    if (withEmbargo)
    {
        // Access Setting
        headerClass = (headerClass == "oddRowEvenCol" ? "oddRowOddCol" : "oddRowEvenCol");
%>
                <th id="t7" class="<%= headerClass %>"><fmt:message key="jsp.submit.upload-file-list.tableheading7"/></th>

<%
    }

%>
            </tr>

<%
    String row = "even";

    List<Bitstream> bitstreams = ContentServiceFactory.getInstance().getItemService().getNonInternalBitstreams(context, subInfo.getSubmissionItem().getItem());
    List<Bundle> bundles = null;

    if (bitstreams.get(0) != null) {
        bundles = bitstreams.get(0).getBundles();
    }

    for (int i = 0; i < bitstreams.size(); i++)
    {
        BitstreamFormat format = bitstreams.get(i).getFormat(context);
        String description = bitstreams.get(i).getFormatDescription(context);
        String supportLevel = LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.supportlevel1");

        if(format.getSupportLevel() == 1)
        {
            supportLevel = LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.supportlevel2");
        }

        if(format.getSupportLevel() == 0)
        {
            supportLevel = LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.supportlevel3");
        }

        // Full param to dspace:popup must be single variable
        String supportLevelLink = LocaleSupport.getLocalizedMessage(pageContext, "help.formats") +"#" + supportLevel;
%>
            <tr>
		<td headers="t1" class="<%= row %>RowEvenCol" align="center">
		    <input class="form-control" type="radio" name="primary_bitstream_id" value="<%= bitstreams.get(i).getID() %>"
			   <% if (bundles.get(0) != null) {
				if (bitstreams.get(i).equals(bundles.get(0).getPrimaryBitstream())) { %>
			       	  <%="checked='checked'" %>
			   <%   }
			      } %> />
		</td>
                <td headers="t2" class="<%= row %>RowOddCol break-all">
                	<a href="<%= request.getContextPath() %>/retrieve/<%= bitstreams.get(i).getID() %>/<%= org.dspace.app.webui.util.UIUtil.encodeBitstreamName(bitstreams.get(i).getName()) %>" target="_blank"><%= bitstreams.get(i).getName() %></a>
            <%      // Don't display "remove" button in workflow mode
			        if (allowFileEditing)
			        {
			%>
	                    <button class="btn btn-danger pull-right" type="submit" name="submit_remove_<%= bitstreams.get(i).getID() %>" value="<fmt:message key="jsp.submit.upload-file-list.button2"/>">
	                    <span class="glyphicon glyphicon-trash"></span>&nbsp;&nbsp;<fmt:message key="jsp.submit.upload-file-list.button2"/>
	                    </button>
			<%
			        } %>	
                </td>
                <td headers="t3" class="<%= row %>RowEvenCol"><%= bitstreams.get(i).getSizeBytes() %> bytes</td>
                <td headers="t4" class="<%= row %>RowOddCol break-all">
                    <%= (bitstreams.get(i).getDescription() == null || bitstreams.get(i).getDescription().equals("")
                        ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.upload-file-list.empty1")
                        : bitstreams.get(i).getDescription()) %>
                    <button type="submit" class="btn btn-default pull-right" name="submit_describe_<%= bitstreams.get(i).getID() %>" value="<fmt:message key="jsp.submit.upload-file-list.button1"/>">
                    <span class="glyphicon glyphicon-pencil"></span>&nbsp;&nbsp;<fmt:message key="jsp.submit.upload-file-list.button1"/>
                    </button>
                </td>
                <td headers="t5" class="<%= row %>RowEvenCol">
                    <%= description %> <dspace:popup page="<%= supportLevelLink %>">(<%= supportLevel %>)</dspace:popup>
                    <button type="submit" class="btn btn-default pull-right" name="submit_format_<%= bitstreams.get(i).getID() %>" value="<fmt:message key="jsp.submit.upload-file-list.button1"/>">
                    <span class="glyphicon glyphicon-file"></span>&nbsp;&nbsp;<fmt:message key="jsp.submit.upload-file-list.button1"/>
                    </button>
                </td>
<%
        // Checksum
        if (showChecksums)
        {
%>
                <td headers="t6" class="<%= row %>RowOddCol">
                    <code><%= bitstreams.get(i).getChecksum() %> (<%= bitstreams.get(i).getChecksumAlgorithm() %>)</code>
                </td>
<%
        }

        String column = "";
        if (withEmbargo)
        {
            column = (showChecksums ? "Even" : "Odd");
%>
                <td headers="t6" class="<%= row %>Row<%= column %>Col" style="text-align:center"> 
                    <button class="btn btn-default pull-left" type="submit" name="submit_editPolicy_<%= bitstreams.get(i).getID() %>" value="<fmt:message key="jsp.submit.upload-file-list.button1"/>">
                    <span class="glyphicon glyphicon-lock"></span>&nbsp;&nbsp;<fmt:message key="jsp.submit.upload-file-list.button1"/>
                    </button>
                </td>
<%
        }
%>
            </tr>
<%
        row = (row.equals("even") ? "odd" : "even");
    }
%>
        </table>

<%
    // Don't allow files to be added in workflow mode
    if (allowFileEditing)
    {
%>
            <div class="row"><input class="btn btn-success col-md-2 col-md-offset-5" type="submit" name="submit_more" value="<fmt:message key="jsp.submit.upload-file-list.button4"/>" /></div>
<%
    }
%>
<br/>
<%-- Show information about how to verify correct upload, but not in workflow
     mode! --%>
<%
    if (allowFileEditing)
    {
%>
        <p class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info3"/></p>
        <ul class="uploadHelp">
            <li class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info4"/></li>
<%
        if (showChecksums)
        {
%>
            <li class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info5"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#checksum\"%>"><fmt:message key="jsp.submit.upload-file-list.help1"/></dspace:popup></li>
<%
        }
        else
        {
%>
            <li class="uploadHelp"><fmt:message key="jsp.submit.upload-file-list.info6"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#checksum\"%>"><fmt:message key="jsp.submit.upload-file-list.help2"/></dspace:popup> 
            <input class="btn btn-info" type="submit" name="submit_show_checksums" value="<fmt:message key="jsp.submit.upload-file-list.button3"/>" /></li>
<%
        }
%>
        </ul>
        <br />
<%
    }
%>    

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>


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
