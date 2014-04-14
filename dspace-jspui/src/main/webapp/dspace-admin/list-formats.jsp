<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of bitstream formats
  -
  - Attributes:
  -
  -   formats - the bitstream formats in the system (BitstreamFormat[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.core.Context"%>
<%@ page import="org.dspace.app.webui.util.UIUtil"%>



<%
    BitstreamFormat[] formats =
        (BitstreamFormat[]) request.getAttribute("formats");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.list-formats.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.list-formats.title"/>
    <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#bitstream\"%>"><fmt:message key="jsp.help"/></dspace:popup>
    </h1>

    <p class="alert alert-info"><fmt:message key="jsp.dspace-admin.list-formats.text1"/></p>
    <p class="alert alert-info"><fmt:message key="jsp.dspace-admin.list-formats.text2"/></p>

<%
    Context context = UIUtil.obtainContext(request);

%>

        <table class="table" summary="Bitstream Format Registry data table">
            <tr>
                <th class="oddRowOddCol">
                <span class="col-md-offset-3">
                    <strong>
                            <fmt:message key="jsp.general.id" />
                            / <fmt:message key="jsp.dspace-admin.list-formats.mime"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.name"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.description"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.support"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.internal"/>
                            / <fmt:message key="jsp.dspace-admin.list-formats.extensions"/>
                    </strong>
                 </span>
                 </th>
            </tr>
<%

    String row = "even";
    for (int i = 0; i < formats.length; i++)
    {
        String[] extensions = formats[i].getExtensions();
        String extValue = "";

        for (int j = 0 ; j < extensions.length; j++)
        {
            if (j > 0)
            {
                extValue = extValue + ", ";
            }
            extValue = extValue + extensions[j];
        }
%>
             <tr>
                 <td>
				<form class="form-inline" method="post" action="">
  					
					<span class="col-md-1"><%= formats[i].getID() %></span>
                    <div class="form-group">
                    	<label class="sr-only" for="mimetype"><fmt:message key="jsp.dspace-admin.list-formats.mime"/></label>
                    	<input class="form-control" type="text" name="mimetype" value="<%= formats[i].getMIMEType()!=null?formats[i].getMIMEType():"" %>" size="14" placeholder="<fmt:message key="jsp.dspace-admin.list-formats.mime"/>"/>
                    </div>
                    <div class="form-group">
                    	  <label class="sr-only" for="short_description"><fmt:message key="jsp.dspace-admin.list-formats.name"/></label>
                    <%
                      if (BitstreamFormat.findUnknown(context).getID() == formats[i].getID()) {
                    %>
                      		  <span class="form-control"><i><%= formats[i].getShortDescription() %></i></span>
                    <% } else { %>                    	
                              <input class="form-control" type="text" name="short_description" value="<%= formats[i].getShortDescription()!=null?formats[i].getShortDescription():"" %>" size="10" placeholder="<fmt:message key="jsp.dspace-admin.list-formats.name"/>"/>
                    <% } %>
                     </div>     
                     <div class="form-group">
                     		<label class="sr-only" for="description"><fmt:message key="jsp.dspace-admin.list-formats.description"/></label>     
                              <input class="form-control" type="text" name="description" value="<%= formats[i].getDescription()!=null?formats[i].getDescription():"" %>" size="20" placeholder="<fmt:message key="jsp.dspace-admin.list-formats.description"/>"/>
                     </div>
                     <div class="form-group">                     		
                              <select class="form-control" name="support_level">
                                  <option value="0" <%= formats[i].getSupportLevel() == 0 ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.dspace-admin.list-formats.unknown"/></option>
	    	                  <option value="1" <%= formats[i].getSupportLevel() == 1 ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.dspace-admin.list-formats.known"/></option>
                                  <option value="2" <%= formats[i].getSupportLevel() == 2 ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.dspace-admin.list-formats.supported"/></option>
                              </select>
                     </div>
                     <div class="form-group">     
                              <input class="form-control" type="checkbox" name="internal" value="true"<%= formats[i].isInternal() ? " checked=\"checked\"" : "" %>/>
                          </div>
                          <div class="form-group">
                          	  <label class="sr-only" for="extensions"><fmt:message key="jsp.dspace-admin.list-formats.extensions"/></label>
                              <input class="form-control" type="text" name="extensions" value="<%= extValue %>" size="10" placeholder="<fmt:message key="jsp.dspace-admin.list-formats.extensions"/>"/>
                          </div>
                     <div class="btn-group pull-right">
                              <input type="hidden" name="format_id" value="<%= formats[i].getID() %>" />
                              <input class="btn btn-primary" type="submit" name="submit_update" value="<fmt:message key="jsp.dspace-admin.general.update"/>"/>
                          
                    <%
                      if (BitstreamFormat.findUnknown(context).getID() != formats[i].getID()) {
                    %>
                             <input class="btn btn-danger" type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>" />
                     <% 
                      } 
                    %>
                    </div>
		       
		                  
                 </form>
             </td>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>

  </table>

  <form method="post" action="">
    
    	<input class="btn btn-success col-md-offset-5" type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.general.addnew"/>" />
    
  </form>
</dspace:layout>
