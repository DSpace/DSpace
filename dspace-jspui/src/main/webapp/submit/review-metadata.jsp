<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Review metadata page(s)
  -
  - Parameters to pass in to this page (from review.jsp)
  -    submission.jump - the step and page number (e.g. stepNum.pageNum) to create a "jump-to" link
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.io.IOException" %>

<%@ page import="org.dspace.submit.step.DescribeStep" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.content.InProgressSubmission" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.DCInputsReaderException" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInput" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCLanguage" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ page import="org.dspace.content.authority.MetadataAuthorityManager" %>

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

        //get the step number (for jump-to link and to determine page)
        String stepJump = (String) request.getParameter("submission.jump");

        //extract out the step & page numbers from the stepJump (format: stepNum.pageNum)
        //(since there are multiple pages, we need to know which page we are reviewing!)
    String[] fields = stepJump.split("\\.");  //split on period
    int stepNum = Integer.parseInt(fields[0]);
        int pageNum = Integer.parseInt(fields[1]);

    Item item = subInfo.getSubmissionItem().getItem();
    
    // determine collection
    Collection c = subInfo.getSubmissionItem().getCollection();

    DCInputSet inputSet = null;

    try
    {
        //get the inputs reader
        DCInputsReader inputsReader = DescribeStep.getInputsReader();

        //load the input set for the current collection
        inputSet = inputsReader.getInputs(c.getHandle());
    }
    catch (DCInputsReaderException e)
    {
        throw new ServletException(e);
    }
%>

<%!void layoutSection(HttpServletRequest request,
                       javax.servlet.jsp.JspWriter out,
                       DCInputSet inputSet,
                       SubmissionInfo subInfo,
                       Item item,
                       int pageNum,
                       PageContext pageContext)
        throws ServletException, IOException
    {
       InProgressSubmission ip = subInfo.getSubmissionItem();

           //need to actually get the rows for pageNum-1 (since first page is index 0)
           DCInput[] inputs = inputSet.getPageRows(pageNum-1,
                                                   ip.hasMultipleTitles(),
                                                   ip.isPublishedBefore());

        MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();


       for (int z = 0; z < inputs.length; z++)
       {
          String scope = subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
          if (!inputs[z].isVisible(scope) && !inputs[z].isReadOnly(scope))
          {
              continue;
          }
          String inputType = inputs[z].getInputType();
          String pairsName = inputs[z].getPairsType();
          String value;
          DCValue[] values;
          StringBuffer row = new StringBuffer();
          
          row.append("<div class=\"row\">");
          row.append("<span class=\"metadataFieldLabel col-md-4 \">");
          row.append(inputs[z].getLabel());
          row.append("</span>");
          row.append("<span class=\"metadataFieldValue col-md-8\">");

          if (inputType.equals("qualdrop_value"))
          {
             values = item.getMetadata(inputs[z].getSchema(), inputs[z].getElement(), Item.ANY, Item.ANY);
          }
          else
          {
             values = item.getMetadata(inputs[z].getSchema(), inputs[z].getElement(), inputs[z].getQualifier(), Item.ANY);
          }
          if (values.length == 0)
          {
             row.append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.no_md"));
          }
          else
          {
             boolean isAuthorityControlled = mam.isAuthorityControlled(inputs[z].getSchema(),
                                                    inputs[z].getElement(),inputs[z].getQualifier());

             for (int i = 0; i < values.length; i++)
             {
                boolean newline = true;
                if (inputType.equals("date"))
                {
                   DCDate date = new DCDate(values[i].value);
                   row.append(UIUtil.displayDate(date, false, true, request));
                }
                else if (inputType.equals("dropdown") || inputType.equals("list"))
                {
                   String storedVal = values[i].value;
                   String displayVal = inputs[z].getDisplayString(pairsName,
                                                                storedVal);
                   if (displayVal != null && !displayVal.equals(""))
                   {
                       row.append(Utils.addEntities(displayVal));
                   }
                   else if (storedVal != null && !storedVal.equals(""))
                   {
                       // use the stored value as label rather than null
                       row.append(Utils.addEntities(storedVal));
                   }
                }
                else if (inputType.equals("qualdrop_value"))
                {
                   String qual = values[i].qualifier;
                   if(qual==null)
                   {
                       qual = "";
                       newline = false;
                   }
                   else
                   {
                        String displayQual = inputs[z].getDisplayString(pairsName,qual);
                        String displayValue = Utils.addEntities(values[i].value);
                        if (displayQual != null)
                        {
                            row.append(displayQual + ":" + displayValue);
                        }
                        else
                        {
                            newline = false;
                        }
                   }
                }
                else
                {
                   row.append(Utils.addEntities(values[i].value));
                }
                                if (isAuthorityControlled)
                {
                    row.append("<span class=\"ds-authority-confidence cf-")
                       .append(values[i].confidence).append("\">")
                       .append(" </span>");
                }
                if (newline)
                {
                    row.append("<br />");
                }
             }
          }
          row.append("</span>");
          row.append("</div>");
   
          out.write(row.toString());
       }
    }%>


<%-- ====================================================== --%>
<%--             DESCRIBE ITEM ELEMENTS                     --%>
<%-- ====================================================== --%>
            
<%@page import="org.dspace.workflow.WorkflowItem"%>
<div class="col-md-10">

<%
            layoutSection(request, out, inputSet, subInfo, item, pageNum, pageContext);
%>
</div>
<div class="col-md-2">
     <input class="btn btn-default" type="submit" name="submit_jump_<%=stepJump%>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
</div>
