<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of DC types
  -
  - Attributes:
  -
  -   formats - the DC formats in the system (MetadataValue[])
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.lang.String" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.MetadataField" %>
<%@ page import="org.dspace.content.MetadataSchema" %>
<%@ page import="java.util.List" %>


<%
    List<MetadataField> types =
        (List<MetadataField>) request.getAttribute("types");
    MetadataSchema schema =
        (MetadataSchema) request.getAttribute("schema");
    List<MetadataSchema> schemas =
        (List<MetadataSchema>) request.getAttribute("schemas");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.list-metadata-fields.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

  
       <h1><fmt:message key="jsp.dspace-admin.list-metadata-fields.title"/>
       	 <a href="<%=request.getContextPath()%>/dspace-admin/metadata-schema-registry">
        	<fmt:message key="jsp.dspace-admin.list-metadata-fields.schemas"/>
        </a> |
         <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#dublincore\"%>"><fmt:message key="jsp.help"/></dspace:popup>
       </h1>
      



<%
String error = (String)request.getAttribute("error");
if (error!=null) {
%>
    <p class="alert alert-danger">
    	<%=error%>
    </p>
<% } %>

    <p class="alert alert-info">
        <fmt:message key="jsp.dspace-admin.list-metadata-fields.note"/>
    </p>

        <table class="table" summary="Dublic Core Type Registry data table">
           <tr>
              <th class="oddRowOddCol">
                 <strong>
                            <fmt:message key="jsp.general.id" /> 
                            / <fmt:message key="jsp.dspace-admin.list-metadata-fields.element"/> 
                            / <fmt:message key="jsp.dspace-admin.list-metadata-fields.qualifier"/> 
                            / <fmt:message key="jsp.dspace-admin.list-metadata-fields.scope"/>
                 </strong>
              </th>
           </tr>
           
<%
    String row = "even";
    for (int i = 0; i < types.size(); i++)
    {
%>
      <tr>
         <td>
             <form class="form-inline" method="post" action="">
                 <span class="col-md-1"><%= types.get(i).getID() %></span>

                    <div class="form-group">
                    	<label class="sr-only" for="element"><fmt:message key="jsp.dspace-admin.list-metadata-fields.element"/></label>
                		<input class="form-control" type="text" name="element" value="<%= types.get(i).getElement() %>" size="12" placeholder="<fmt:message key="jsp.dspace-admin.list-metadata-fields.element"/>"/>
                	</div>
                    <div class="form-group">
                    	<label class="sr-only" for="qualifier"><fmt:message key="jsp.dspace-admin.list-metadata-fields.qualifier"/></label>
                		<input class="form-control" type="text" name="qualifier" value="<%= (types.get(i).getQualifier() == null ? "" : types.get(i).getQualifier()) %>" size="12" placeholder="<fmt:message key="jsp.dspace-admin.list-metadata-fields.qualifier"/>"/>
                	</div>                         
                    <div class="form-group">
                    	<label class="sr-only" for="scope_note"><fmt:message key="jsp.dspace-admin.list-metadata-fields.scope"/></label>
                		<textarea class="form-control" name="scope_note" rows="3" cols="40"><%= (types.get(i).getScopeNote() == null ? "" : types.get(i).getScopeNote()) %></textarea>
                	</div>                             
                         
					<div class="btn-group pull-right">                             
                         
                            <input type="hidden" name="dc_type_id" value="<%= types.get(i).getID() %>"/>
                            <input class="btn btn-primary" type="submit" name="submit_update" value="<fmt:message key="jsp.dspace-admin.general.update"/>"/>             
                         
                            <input class="btn btn-danger" type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>"/>
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
        <input type="hidden" name="dc_schema_id" value="<%= schema.getID() %>"/>
        	 <h2><fmt:message key="jsp.dspace-admin.list-metadata-fields.addfield"/></h2>
              <p class="alert alert-info"><fmt:message key="jsp.dspace-admin.list-metadata-fields.addfieldnote"/></p>
                      
			<p><fmt:message key="jsp.dspace-admin.list-metadata-fields.element"/>:</p>
                      <input class="form-control" type="text" name="element"/>

                      <p><fmt:message key="jsp.dspace-admin.list-metadata-fields.qualifier"/>:</p>
                      <input class="form-control" type="text" name="qualifier"/>
              
                      <p><fmt:message key="jsp.dspace-admin.list-metadata-fields.scope"/>:</p>
                      <textarea class="form-control" name="scope_note" rows="3" cols="40"></textarea>
              
            <input class="btn btn-primary" type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.general.addnew"/>"/>
        
      </form>


    <form method="post" action="">
      
      <h2><fmt:message key="jsp.dspace-admin.list-metadata-fields.move"/></h2>
<% if (schemas.size() > 1) { %>
        <p class="alert alert-info">
        <fmt:message key="jsp.dspace-admin.list-metadata-fields.movenote"/></p>
      
      
       <p><fmt:message key="jsp.dspace-admin.list-metadata-fields.element"/>:</p>
      <select class="form-control" name="dc_field_id" multiple="multiple" size="5">
<%
    for (int i = 0; i < types.size(); i++)
    {
      String qualifier = (types.get(i).getQualifier() == null ? "" : "."+types.get(i).getQualifier());
%>     <option value="<%= types.get(i).getID() %>"><%= types.get(i).getElement()+qualifier %></option>
<%  }
%>
      </select>
      <p><fmt:message key="jsp.dspace-admin.list-metadata-fields.schema"/>: </p>
      <select class="form-control" name="dc_dest_schema_id">
<%
    for (int i = 0; i < schemas.size(); i++)
    {
              if (schemas.get(i).getID() != schema.getID())
              {
%>      <option value="<%= schemas.get(i).getID() %>"><%= schemas.get(i).getNamespace() %></option>
<%            }
    }
%>
      </select>
        <p><input class="btn btn-primary" type="submit" name="submit_move" value="<fmt:message key="jsp.dspace-admin.list-metadata-fields.movesubmit"/>"/></p>
<% } else { %>
      
              <p class="alert alert-info"><fmt:message key="jsp.dspace-admin.list-metadata-fields.moveformnote"/><br/><br/>
              </p>
      
<% } %>
      
   </form>

</dspace:layout>
