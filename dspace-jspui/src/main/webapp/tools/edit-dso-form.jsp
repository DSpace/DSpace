<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show form allowing edit of collection metadata
  -
  - Attributes:
  -    item        - item to edit
  -    collections - collections the item is in, if any
  -    handle      - item's Handle, if any (String)
  -    dc.types    - MetadataField[] - all metadata fields in the registry
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"
    prefix="c" %>

<%@ page import="java.util.Date" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>

<%@ page import="org.dspace.content.MetadataField" %>
<%@ page import="org.dspace.app.webui.servlet.admin.AuthorizeAdminServlet" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DSpaceObject" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.content.authority.MetadataAuthorityManager" %>
<%@ page import="org.dspace.content.authority.ChoiceAuthorityManager" %>
<%@ page import="org.dspace.content.authority.Choices" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="java.util.ArrayList" %>

<%
    DSpaceObject item = (DSpaceObject) request.getAttribute("item");
    String handle = (String) request.getAttribute("handle");
    DSpaceObject parent = (DSpaceObject) request.getAttribute("parent");
    MetadataField[] dcTypes = (MetadataField[])  request.getAttribute("dc.types");
    HashMap metadataFields = (HashMap) request.getAttribute("metadataFields");
    request.setAttribute("LanguageSwitch", "hide");

    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    
    // owning Collection ID for choice authority calls
    int collectionID = -1;
%>
<%!
     StringBuffer doAuthority(MetadataAuthorityManager mam, ChoiceAuthorityManager cam,
            PageContext pageContext,
            String contextPath, String fieldName, String idx,
            Metadatum dcv, int collectionID)
    {
        StringBuffer sb = new StringBuffer();
        if (cam.isChoicesConfigured(fieldName))
        {
            boolean authority = mam.isAuthorityControlled(fieldName);
            boolean required = authority && mam.isAuthorityRequired(fieldName);
           
            String fieldNameIdx = "value_" + fieldName + "_" + idx;
            String authorityName = "choice_" + fieldName + "_authority_" + idx;
            String confidenceName = "choice_" + fieldName + "_confidence_" + idx;

            // put up a SELECT element containing all choices
            if ("select".equals(cam.getPresentation(fieldName)))
            {
                sb.append("<select class=\"form-control\" id=\"").append(fieldNameIdx)
                   .append("\" name=\"").append(fieldNameIdx)
                   .append("\" size=\"1\">");
                Choices cs = cam.getMatches(fieldName, dcv.value, collectionID, 0, 0, null);
                if (cs.defaultSelected < 0)
                    sb.append("<option value=\"").append(dcv.value).append("\" selected>")
                      .append(dcv.value).append("</option>\n");

                for (int i = 0; i < cs.values.length; ++i)
                {
                    sb.append("<option value=\"").append(cs.values[i].value).append("\"")
                      .append(i == cs.defaultSelected ? " selected>":">")
                      .append(cs.values[i].label).append("</option>\n");
                }
                sb.append("</select>\n");
            }

              // use lookup for any other presentation style (i.e "select")
            else
            {
                String confidenceIndicator = "indicator_"+confidenceName;
                sb.append("<textarea class=\"form-control\" id=\"").append(fieldNameIdx).append("\" name=\"").append(fieldNameIdx)
                   .append("\" rows=\"3\" cols=\"50\">")
                   .append(dcv.value).append("</textarea>\n<br/>\n");

                if (authority)
                {
                    String confidenceSymbol = Choices.getConfidenceText(dcv.confidence).toLowerCase();
                    sb.append("<span class=\"col-md-1\">")
                      .append("<img id=\""+confidenceIndicator+"\"  title=\"")
                      .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.authority.confidence.description."+confidenceSymbol))
                      .append("\" class=\"ds-authority-confidence cf-"+ confidenceSymbol)
                      .append("\" src=\"").append(contextPath).append("/image/confidence/invisible.gif\" />")
                      .append("</span>");
                	sb.append("<span class=\"col-md-5\">")
                      .append("<input class=\"form-control\" type=\"text\" readonly value=\"")
                      .append(dcv.authority != null ? dcv.authority : "")
                      .append("\" id=\"").append(authorityName)
                      .append("\" onChange=\"javascript: return DSpaceAuthorityOnChange(this, '")
                      .append(confidenceName).append("','").append(confidenceIndicator)
                      .append("');\" name=\"").append(authorityName).append("\" class=\"ds-authority-value ds-authority-visible \"/>")
                      .append("<input type=\"image\" class=\"ds-authority-lock is-locked \" ")
                      .append(" src=\"").append(contextPath).append("/image/confidence/invisible.gif\" ")
                      .append(" onClick=\"javascript: return DSpaceToggleAuthorityLock(this, '").append(authorityName).append("');\" ")
                      .append(" title=\"")
                      .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-item-form.unlock"))
                      .append("\" >")
                      .append("<input type=\"hidden\" value=\"").append(confidenceSymbol).append("\" id=\"").append(confidenceName)
                      .append("\" name=\"").append(confidenceName)
                      .append("\" class=\"ds-authority-confidence-input\"/>")
                      .append("</span>");
                }
                 
               sb.append("<span class=\"col-md-1\">")
               	 .append("<button class=\"form-control\" name=\"").append(fieldNameIdx).append("_lookup\" ")
                 .append("onclick=\"javascript: return DSpaceChoiceLookup('")
                 .append(contextPath).append("/tools/lookup.jsp','")
                 .append(fieldName).append("','edit_metadata','")
                 .append(fieldNameIdx).append("','").append(authorityName).append("','")
                 .append(confidenceIndicator).append("',")
                 .append(String.valueOf(collectionID)).append(",")
                 .append("false").append(",false);\"")
                 .append(" title=\"")
                 .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.lookup.lookup"))
                 .append("\"><span class=\"glyphicon glyphicon-search\"></span></button></span>");
            }
        }
        return sb;
    }
%>

<c:set var="dspace.layout.head.last" scope="request">
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/prototype.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/builder.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/effects.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/scriptaculous/controls.js"></script>		
    <script type="text/javascript" src="<%= request.getContextPath() %>/dspace-admin/js/bitstream-ordering.js"></script>
</c:set>

<dspace:layout style="submission" titlekey="jsp.tools.edit-dso-form.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">


    <%-- <h1>Edit Item</h1> --%>
        <h1><fmt:message key="jsp.tools.edit-dso-form.title"/>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\") + \"#editmetadata\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
        </h1>
    
    <%-- <p><strong>PLEASE NOTE: These changes are not validated in any way.
    You are responsible for entering the data in the correct format.
    If you are not sure what the format is, please do NOT make changes.</strong></p> --%>
    <p class="alert alert-danger"><strong><fmt:message key="jsp.tools.edit-item-form.note"/></strong></p>

	<div class="row">
	<div class="col-md-12">
		<div class="panel panel-primary">
			<div class="panel-heading"><fmt:message key="jsp.tools.edit-dso-form.details" /></div>

			<div class="panel-body">
				<table class="table">
					<tr>
						<td><fmt:message key="jsp.tools.edit-dso-form.resourcetype" />
						</td>
						<td><%= item.getTypeText() %></td>
					</tr>
					<tr>
						<td><fmt:message key="jsp.tools.edit-dso-form.resourceID" />
						</td>
						<td><%= item.getID() %></td>
					</tr>

					<tr>
						<td><fmt:message key="jsp.tools.edit-item-form.handle" />
						</td>
						<td><%= (handle == null ? "None" : handle) %></td>
					</tr>
<%--					<tr>
						<td><fmt:message key="jsp.tools.edit-item-form.modified" />
						</td>
						<td><dspace:date
								date="<%= new DCDate(item.getLastModified()) %>" />
						</td>
					</tr> --%>


					<%-- <td class="submitFormLabel">In Collections:</td> --%>
					<tr>
						<td><fmt:message key="jsp.tools.edit-dso-form.parents" />
						</td>
						<td>
							<% if (parent != null) { %>
							<%= parent.getName() %> - <%= parent.getTypeText() %> ID: <%= parent.getID() %> 
							<a class="btn btn-warning"
    							href="<%= request.getContextPath() %>/tools/edit-dso?resource_type=<%= parent.getType() %>&resource_id=<%= parent.getID() %>"><fmt:message key="jsp.tools.general.edit"/></a></h3>	
							<% } else { %>
								<fmt:message key="jsp.tools.edit-dso-form.na" />
							<% } %>
						</td>
					</tr>
					<tr>
						<%-- <td class="submitFormLabel">Item page:</td> --%>
						<td><fmt:message key="jsp.tools.edit-dso-form.dsopage" />
						</td>
						<td>
							<%  if (handle == null) { %> <em><fmt:message
									key="jsp.tools.edit-dso-form.na" />
						</em> <%  } else {
    				String url = ConfigurationManager.getProperty("dspace.url") + "/handle/" + handle; %>
							<a target="_blank" href="<%= url %>"><%= url %></a> <%  } %>
						</td>
					</tr>


				</table>
			</div>
		</div>
	</div>

    </div>

    <form id="edit_metadata" name="edit_metadata" method="post" action="<%= request.getContextPath() %>/tools/edit-dso">
    <div class="table-responsive">
        <table class="table" summary="Edit item withdrawn table">
            <tr>
                <%-- <th class="oddRowOddCol"><strong>Element</strong></th>
                <th id="t1" class="oddRowEvenCol"><strong>Qualifier</strong></th>
                <th id="t2" class="oddRowOddCol"><strong>Value</strong></th>
                <th id="t3" class="oddRowEvenCol"><strong>Language</strong></th> --%>
                
                <th id="t0" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem0"/></strong></th>
                <th id="t1" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem1"/></strong></th>
                <th id="t2" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem2"/></strong></th>
                <th id="t3" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem3"/></strong></th>
                <th id="t4" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem4"/></strong></th>
                <th id="t5" class="oddRowEvenCol">&nbsp;</th>
            </tr>
<%
    MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
    ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
    Metadatum[] dcv = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
    String row = "even";
    
    // Keep a count of the number of values of each element+qualifier
    // key is "element" or "element_qualifier" (String)
    // values are Integers - number of values that element/qualifier so far
    Map<String, Integer> dcCounter = new HashMap<String, Integer>();
    
    for (int i = 0; i < dcv.length; i++)
    {
        // Find out how many values with this element/qualifier we've found

        String key = ChoiceAuthorityManager.makeFieldKey(dcv[i].schema, dcv[i].element, dcv[i].qualifier);

        Integer count = dcCounter.get(key);
        if (count == null)
        {
            count = new Integer(0);
        }
        
        // Increment counter in map
        dcCounter.put(key, new Integer(count.intValue() + 1));

        // We will use two digits to represent the counter number in the parameter names.
        // This means a string sort can be used to put things in the correct order even
        // if there are >= 10 values for a particular element/qualifier.  Increase this to
        // 3 digits if there are ever >= 100 for a single element/qualifer! :)
        String sequenceNumber = count.toString();
        
        while (sequenceNumber.length() < 2)
        {
            sequenceNumber = "0" + sequenceNumber;
        }
 %>
            <tr>
                <td headers="t0" class="<%= row %>RowOddCol"><%=dcv[i].schema %></td>
                <td headers="t1" class="<%= row %>RowEvenCol"><%= dcv[i].element %>&nbsp;&nbsp;</td>
                <td headers="t2" class="<%= row %>RowOddCol"><%= (dcv[i].qualifier == null ? "" : dcv[i].qualifier) %></td>
                <td headers="t3" class="<%= row %>RowEvenCol">
                    <%
                        if (cam.isChoicesConfigured(key))
                        {
                    %>
                    <%=
                        doAuthority(mam, cam, pageContext, request.getContextPath(), key, sequenceNumber,
                                dcv[i], collectionID).toString()
                    %>
                    <% } else { %>
                        <textarea class="form-control" id="value_<%= key %>_<%= sequenceNumber %>" name="value_<%= key %>_<%= sequenceNumber %>" rows="3" cols="50"><%= dcv[i].value %></textarea>
                    <% } %>
                </td>
                <td headers="t4" class="<%= row %>RowOddCol">
                    <input class="form-control" type="text" name="language_<%= key %>_<%= sequenceNumber %>" value="<%= (dcv[i].language == null ? "" : dcv[i].language.trim()) %>" size="5"/>
                </td>
                <td headers="t5" class="<%= row %>RowEvenCol">
                    <%-- <input type="submit" name="submit_remove_<%= key %>_<%= sequenceNumber %>" value="Remove" /> --%>
                    <button class="btn btn-danger" name="submit_remove_<%= key %>_<%= sequenceNumber %>" value="<fmt:message key="jsp.tools.general.remove"/>">
                    	<span class="glyphicon glyphicon-trash"></span>
                    </button>
                </td>
            </tr>
<%      row = (row.equals("odd") ? "even" : "odd");
    } %>

            <tr>
        
                <td headers="t1" colspan="3" class="<%= row %>RowEvenCol">
                    <select  class="form-control" name="addfield_dctype">
<%  for (int i = 0; i < dcTypes.length; i++)
    {
        Integer fieldID = new Integer(dcTypes[i].getFieldID());
        String displayName = (String)metadataFields.get(fieldID);
%>
                        <option value="<%= fieldID.intValue() %>"><%= displayName %></option>
<%  } %>
                    </select>
                </td>
                <td headers="t3" class="<%= row %>RowOddCol">
                    <textarea class="form-control" name="addfield_value" rows="3" cols="50"></textarea>
                </td>
                <td headers="t4" class="<%= row %>RowEvenCol">
                    <input class="form-control" type="text" name="addfield_language" size="5"/>
                </td>
                <td headers="t5" class="<%= row %>RowOddCol">
                    <%-- <input type="submit" name="submit_addfield" value="Add"> --%>
					<button class="btn btn-default" name="submit_addfield" value="<fmt:message key="jsp.tools.general.add"/>">
						<span class="glyphicon glyphicon-plus"></span> 
					</button>
                </td>
            </tr>
        </table>
        
	</div>
        
        <%-- <p align="center"><input type="submit" name="submit_addbitstream" value="Add Bitstream"></p> --%>
	<div class="btn-group col-md-12">
		<input type="hidden" name="resource_type" value="<%= item.getType() %>"/>
        <input type="hidden" name="resource_id" value="<%= item.getID() %>"/>
        <input type="hidden" name="action" value="<%= EditItemServlet.UPDATE_ITEM %>"/>
					
                        <%-- <input type="submit" name="submit" value="Update" /> --%>
                        <input class="btn btn-primary pull-right col-md-3" type="submit" name="submit" value="<fmt:message key="jsp.tools.general.update"/>" />
                        <%-- <input type="submit" name="submit_cancel" value="Cancel" /> --%>
						<input class="btn btn-default pull-right col-md-3" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
					</div>
    </form>
</dspace:layout>
