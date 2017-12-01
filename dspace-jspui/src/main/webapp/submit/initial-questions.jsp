<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Initial questions for keeping UI as simple as possible.
  -
  - Attributes to pass in:
  -    submission.info    - the SubmissionInfo object
  -    submission.inputs  - the DCInputSet object
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

        DCInputSet inputSet =
        (DCInputSet) request.getAttribute("submission.inputs");

	// Obtain DSpace context
    Context context = UIUtil.obtainContext(request);

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
%>

<dspace:layout style="submission"
			   locbar="off"
               navbar="off"
               titlekey="jsp.submit.initial-questions.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp" />

        <%-- <h1>Submit: Describe Your Item</h1> --%>
		<h1><fmt:message key="jsp.submit.initial-questions.heading"/>
		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#describe1\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
		</h1>
    
        <%-- <p>Please check the boxes next to the statements that apply to your
        submission.
        <object><dspace:popup page="/help/index.html#describe1">(More Help...)</dspace:popup></object></p> --%>

        <p><fmt:message key="jsp.submit.initial-questions.info" /></p>

<%
	// Don't display MultipleTitles if no such form box defined
    if (inputSet.isDefinedMultTitles())
    {
%>			
			<div class="input-group">
				<span class="input-group-addon">
					<input type="checkbox" name="multiple_titles" value="true" <%= (subInfo.getSubmissionItem().hasMultipleTitles() ? "checked='checked'" : "") %> /></td>
				</span>
				<label class="form-control" for="multiple_titles"><fmt:message key="jsp.submit.initial-questions.elem1"/></label>
			</div>
<%
    }
    // Don't display PublishedBefore if no form boxes defined
    if (inputSet.isDefinedPubBefore())
    {
%>
			<div class="input-group">
                <span class="input-group-addon">
					<input type="checkbox" type="checkbox" name="published_before" value="true" <%= (subInfo.getSubmissionItem().isPublishedBefore() ? "checked='checked'" : "") %> /></td>
				</span>
				<label class="form-control" for="published_before"><fmt:message key="jsp.submit.initial-questions.elem2"/></label>
			</div>
<%
    }
    // Don't display file or thesis questions in workflow mode
    if (!subInfo.isInWorkflow())
    {
%>
			<div class="input-group">
                <span class="input-group-addon">
					<input type="checkbox" name="multiple_files" value="true" <%= (subInfo.getSubmissionItem().hasMultipleFiles() ? "checked='checked'" : "") %> />
				</span>
				<label class="form-control" for="multiple_files">
					<fmt:message key="jsp.submit.initial-questions.elem3"/>
				</label>
			</div>		
<%
        if (ConfigurationManager.getBooleanProperty("webui.submit.blocktheses"))
        {
%>
			<div class="input-group">
                <span class="input-group-addon">
					<input type="checkbox" name="is_thesis" value="true">
				</span>	
				<label class="form-control" for="is_thesis">
					<fmt:message key="jsp.submit.initial-questions.elem4"/>
				</label>
			</div>		
<%
        }
    }
%>
<br/>
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
				<%  //if not first step, show "Previous" button
					if(!SubmissionController.isFirstStep(request, subInfo))
					{ %>
					<div class="row">
						<div class="col-md-6 pull-right btn-group">
							<input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
							<input class="btn btn-default col-md-4" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
							<input class="btn btn-primary col-md-4" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
						</div>
					</div>		
		                       
				<%  } else { %>
    			<div class="row">
					<div class="col-md-4 pull-right btn-group">
						<input class="btn btn-default col-md-6" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
						<input class="btn btn-primary col-md-6" type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
					</div>
				</div>		
    			<%  }  %>
    </form>

</dspace:layout>
