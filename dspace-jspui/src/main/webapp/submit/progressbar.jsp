<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Progress bar for submission form.  Note this must be included within
  - the FORM element in the submission form, since it contains navigation
  - buttons.
  -
  - Parameters: None
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Iterator" %>

<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.webui.submit.JSPStepManager" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.util.SubmissionConfig" %>
<%@ page import="org.dspace.app.util.SubmissionStepConfig" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.apache.log4j.Logger" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    /** log4j logger */
    Logger log = Logger.getLogger("progressbar.jsp");

	// Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
    
    //get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    //get configuration for our current Submission process
    SubmissionConfig subConfig = subInfo.getSubmissionConfig();

    //get configuration for our current step & page in submission process
    SubmissionStepConfig currentStepConfig = SubmissionController.getCurrentStepConfig(request, subInfo);
	int currentPage = AbstractProcessingStep.getCurrentPage(request);

	//get last step & page reached
	int stepReached = SubmissionController.getStepReached(subInfo);
	int pageReached = JSPStepManager.getPageReached(subInfo);

    // Are we in workflow mode?
    boolean workflowMode = false;
    if (stepReached == -1)
    {
        workflowMode = true;
    }
%>

<!--Progress Bar-->
<div class="row container btn-group">
<%    
    //get progress bar info, used to build progress bar
	HashMap progressBarInfo = (HashMap) subInfo.getProgressBarInfo();

    if((progressBarInfo!=null) && (progressBarInfo.keySet()!=null))
    {
	   //get iterator
	   Set keys = progressBarInfo.keySet();
	   Iterator barIterator = keys.iterator();

	   //loop through all steps & print out info     
       while(barIterator.hasNext())
        {
		  //this is a string of the form: stepNum.pageNum
	      String stepAndPage = (String) barIterator.next();
		
		  //get heading from hashmap
		  String heading = (String) progressBarInfo.get(stepAndPage);

		  //if the heading contains a period (.), then assume
          //it is referencing a key in Messages.properties
          if(heading.indexOf(".") >= 0)
          {
             //prepend the existing key with "jsp." since we are using JSP-UI
             heading = LocaleSupport.getLocalizedMessage(pageContext, "jsp." + heading);
          }

		  //split into stepNum and pageNum
		  String[] fields = stepAndPage.split("\\.");  //split on period
          int stepNum = Integer.parseInt(fields[0]);
		  int pageNum = Integer.parseInt(fields[1]);
          
		  //if anywhere in last step (i.e. submission is completed), disable EVERYTHING (not allowed to jump back)
		  if(stepReached >= subConfig.getNumberOfSteps())
          {
			 if(stepNum==subConfig.getNumberOfSteps())
			 {
			   // Show "Complete" step as the current step
    %>
               <input class="submitProgressButtonCurrent btn btn-primary" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" />
    <%
             }
        	 else
        	 {
			   // submission is completed, so cannot jump back to any steps
    %>
               <input class="submitProgressButtonDone btn btn-success" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" />
    <%
        	 }
           }
		   //if this is the current step & page, highlight it as "current"
		   else if((stepNum == currentStepConfig.getStepNumber()) && (pageNum == currentPage))
           {
	         %>
		     <input class="submitProgressButtonCurrent btn btn-primary" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" />
        	 <%
           }
		   else if(workflowMode) //if in workflow mode, can jump to any step/page
    	   {
		     %>
            <input class="submitProgressButtonDone btn btn-success" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" />
			 <%
    	   }
		  //else if this step & page has been completed
		  else if( (stepNum < stepReached) || ((stepNum == stepReached) && (pageNum <= pageReached)) )
    	  {
%>
            <input class="submitProgressButtonDone btn btn-info" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" />
<%
          }
		  else //else this is a step that has not been done, yet
          {
            // Stage hasn't been completed yet (can't be jumped to)
%>
		    <input class="submitProgressButtonNotDone btn btn-default" disabled="disabled" type="submit" name="<%=AbstractProcessingStep.PROGRESS_BAR_PREFIX + stepAndPage%>" value="<%=heading%>" />
<%
          }
	   }//end while
   }
%>
        </div>
