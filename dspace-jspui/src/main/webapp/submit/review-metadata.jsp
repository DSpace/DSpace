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
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.DCInputsReaderException" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInput" %>
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
    
    // Fetch the document type (dc.type)
    String documentType = "";
    if( (ContentServiceFactory.getInstance().getItemService()
            .getMetadataByMetadataString(item, "dc.type") != null)
            && (ContentServiceFactory.getInstance().getItemService()
                    .getMetadataByMetadataString(item, "dc.type").size() >0) )
    {
        documentType = ContentServiceFactory.getInstance().getItemService()
                .getMetadataByMetadataString(item, "dc.type").get(0).getValue();
    }

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
        String documentType,
        int pageNum,
        PageContext pageContext)
        throws ServletException, IOException
    {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        InProgressSubmission ip = subInfo.getSubmissionItem();
 
        //need to actually get the rows for pageNum-1 (since first page is index 0)
        DCInput[] inputs = inputSet.getPageRows(pageNum-1,
                ip.hasMultipleTitles(),
                ip.isPublishedBefore());
            
        MetadataAuthorityService mam = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();

        for (int z = 0; z < inputs.length; z++)
        {
            // Omit fields not allowed for this document type
            if(!inputs[z].isAllowedFor(documentType))
            {
                continue;
            }
            String scope = subInfo.isInWorkflow() ? DCInput.WORKFLOW_SCOPE : DCInput.SUBMISSION_SCOPE;
            if (!inputs[z].isVisible(scope) && !inputs[z].isReadOnly(scope))
            {
                continue;
            }
            String inputType = inputs[z].getInputType();
            String pairsName = inputs[z].getPairsType();
            String value;
            List<MetadataValue> values;
            StringBuffer row = new StringBuffer();
            
            row.append("<div class=\"row\">");
            row.append("<span class=\"metadataFieldLabel col-md-4 \">");
            row.append(inputs[z].getLabel());
            row.append("</span>");
            row.append("<span class=\"metadataFieldValue col-md-8\">");
  
            if (inputType.equals("qualdrop_value"))
            {
                values = itemService.getMetadata(item, inputs[z].getSchema(), inputs[z].getElement(), Item.ANY, Item.ANY);
            }
            else
            {
                values = itemService.getMetadata(item, inputs[z].getSchema(), inputs[z].getElement(), inputs[z].getQualifier(), Item.ANY);
            }
            if (values.size() == 0)
            {
                row.append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.submit.review.no_md"));
            }
            else
            {
                boolean isAuthorityControlled = mam.isAuthorityControlled(inputs[z].getSchema()+ "." +
                        inputs[z].getElement() + "." + inputs[z].getQualifier());
  
                for (int i = 0; i < values.size(); i++)
                {
                    boolean newline = true;
                    if (inputType.equals("date"))
                    {
                        DCDate date = new DCDate(values.get(i).getValue());
                        row.append(UIUtil.displayDate(date, false, true, request));
                    }
                    else if (inputType.equals("dropdown") || inputType.equals("list"))
                    {
                        String storedVal = values.get(i).getValue();
                        String displayVal = inputs[z].getDisplayString(pairsName, storedVal);
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
                        String qual = values.get(i).getMetadataField().getQualifier();
                        if(qual==null)
                        {
                            qual = "";
                            newline = false;
                        }
                        else
                        {
                            String displayQual = inputs[z].getDisplayString(pairsName,qual);
                            String displayValue = Utils.addEntities(values.get(i).getValue());
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
                        row.append(Utils.addEntities(values.get(i).getValue()));
                    }
                    if (isAuthorityControlled)
                    {
                        row.append("<span class=\"ds-authority-confidence cf-")
                            .append(values.get(i).getConfidence()).append("\">")
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
            
<%@page import="org.dspace.workflowbasic.BasicWorkflowItem"%>
<%@ page import="org.dspace.authority.factory.AuthorityServiceFactory" %>
<%@ page import="org.dspace.content.authority.factory.ContentAuthorityServiceFactory" %>
<%@ page import="org.dspace.content.authority.service.MetadataAuthorityService" %>
<%@ page import="org.dspace.content.service.ItemService" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>
<%@ page import="org.dspace.content.*" %>
<%@ page import="java.util.List" %>
<div class="col-md-10">

<%
    layoutSection(request, out, inputSet, subInfo, item, documentType, pageNum, pageContext);
%>
</div>
<div class="col-md-2">
    <input class="btn btn-default" type="submit" name="submit_jump_<%=stepJump%>" value="<fmt:message key="jsp.submit.review.button.correct"/>" />
</div>
