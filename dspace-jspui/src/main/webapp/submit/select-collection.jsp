<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - UI page for selection of collection.
  -
  - Required attributes:
  -    collections - Array of collection objects to show in the drop-down.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    //get collections to choose from
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");

	//check if we need to display the "no collection selected" error
    Boolean noCollection = (Boolean) request.getAttribute("no.collection");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
%>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.select-collection.title"
               nocache="true">

    <h1><fmt:message key="jsp.submit.select-collection.heading"/></h1>

	
<%  if (collections.length > 0)
    {
%>
	<div><fmt:message key="jsp.submit.select-collection.info1"/>
      <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#choosecollection\"%>"><fmt:message key="jsp.morehelp"/> </dspace:popup> 
	</div>

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">
<%-- HACK: a <center> tag seems to be the only way to convince certain --%>
<%--       browsers to center the table. --%>
        <center>
            <table summary="Select collection table">
<%
		//if no collection was selected, display an error
		if((noCollection != null) && (noCollection.booleanValue()==true))
		{
%>
                <tr>
					<td colspan="2" class="submitFormWarn"><fmt:message key="jsp.submit.select-collection.no-collection"/></td>
				</tr>
<%
		}
%>            
            
                <tr>
                    <%-- <td class="submitFormLabel"><label for="tcollection">Collection</label></td> --%>
					<td class="submitFormLabel"><label for="tcollection"><fmt:message key="jsp.submit.select-collection.collection"/></label></td>
                    <td>
                        <select name="collection" id="tcollection">
                        	<option value="-1"></option>
<%
        for (int i = 0; i < collections.length; i++)
        {
%>
                            <option value="<%= collections[i].getID() %>"><%= collections[i].getMetadata("name") %></option>
<%
        }
%>
                        </select>
                    </td>
                </tr>
            </table>
            <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
            <%= SubmissionController.getSubmissionParameters(context, request) %>
            <br />

            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
                    <td>
                        <%-- <input type="submit" name="submit_next" value="Next &gt;"> --%>
						<input type="submit" name="<%=AbstractProcessingStep.NEXT_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td>
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel/Save"> --%>
						<input type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.select-collection.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
<%  } else { %>
	<p class="submitFormWarn"><fmt:message key="jsp.submit.select-collection.none-authorized"/></p>
<%  } %>	
	   <p><fmt:message key="jsp.general.goto"/><br />
	   <a href="<%= request.getContextPath() %>"><fmt:message key="jsp.general.home"/></a><br />
	   <a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.general.mydspace" /></a>
	   </p>	
</dspace:layout>
