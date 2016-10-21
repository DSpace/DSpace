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

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>

<%@ page import="org.dspace.app.webui.servlet.admin.AuthorizeAdminServlet" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.content.authority.service.MetadataAuthorityService" %>
<%@ page import="org.dspace.content.authority.service.ChoiceAuthorityService" %>
<%@ page import="org.dspace.content.authority.Choices" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="java.util.*" %>
<%@ page import="org.dspace.content.authority.service.MetadataAuthorityService" %>
<%@ page import="org.dspace.content.*" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.authority.factory.ContentAuthorityServiceFactory" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%
    Item item = (Item) request.getAttribute("item");
    String handle = (String) request.getAttribute("handle");
    List<Collection> collections = (List<Collection>) request.getAttribute("collections");
    List<MetadataField> dcTypes = (List<MetadataField>)  request.getAttribute("dc.types");
    HashMap metadataFields = (HashMap) request.getAttribute("metadataFields");
    request.setAttribute("LanguageSwitch", "hide");

    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    
    // Is the logged in user an admin of the item
    Boolean itemAdmin = (Boolean)request.getAttribute("admin_button");
    boolean isItemAdmin = (itemAdmin == null ? false : itemAdmin.booleanValue());

    // Is the logged in user an admin or community admin or collection admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    Boolean communityAdmin = (Boolean)request.getAttribute("is.communityAdmin");
    boolean isCommunityAdmin = (communityAdmin == null ? false : communityAdmin.booleanValue());
    
    Boolean collectionAdmin = (Boolean)request.getAttribute("is.collectionAdmin");
    boolean isCollectionAdmin = (collectionAdmin == null ? false : collectionAdmin.booleanValue());
    
    String naviAdmin = "admin";
    String link = "/dspace-admin";
    
    if(!isAdmin && (isCommunityAdmin || isCollectionAdmin))
    {
        naviAdmin = "community-or-collection-admin";
        link = "/tools";
    }
    
    Boolean policy = (Boolean)request.getAttribute("policy_button");
    boolean bPolicy = (policy == null ? false : policy.booleanValue());
    
    Boolean delete = (Boolean)request.getAttribute("delete_button");
    boolean bDelete = (delete == null ? false : delete.booleanValue());

    Boolean createBits = (Boolean)request.getAttribute("create_bitstream_button");
    boolean bCreateBits = (createBits == null ? false : createBits.booleanValue());

    Boolean removeBits = (Boolean)request.getAttribute("remove_bitstream_button");
    boolean bRemoveBits = (removeBits == null ? false : removeBits.booleanValue());

    Boolean ccLicense = (Boolean)request.getAttribute("cclicense_button");
    boolean bccLicense = (ccLicense == null ? false : ccLicense.booleanValue());
    
    Boolean withdraw = (Boolean)request.getAttribute("withdraw_button");
    boolean bWithdraw = (withdraw == null ? false : withdraw.booleanValue());
    
    Boolean reinstate = (Boolean)request.getAttribute("reinstate_button");
    boolean bReinstate = (reinstate == null ? false : reinstate.booleanValue());

    Boolean privating = (Boolean)request.getAttribute("privating_button");
    boolean bPrivating = (privating == null ? false : privating.booleanValue());
    
    Boolean publicize = (Boolean)request.getAttribute("publicize_button");
    boolean bPublicize = (publicize == null ? false : publicize.booleanValue());

    Boolean reOrderBitstreams = (Boolean)request.getAttribute("reorder_bitstreams_button");
    boolean breOrderBitstreams = (reOrderBitstreams != null && reOrderBitstreams);

    // owning Collection ID for choice authority calls
    Collection collection = null;
    if (collections.size() > 0)
        collection = collections.get(0);
%>
<%!
     StringBuffer doAuthority(MetadataAuthorityService mam, ChoiceAuthorityService cam,
            PageContext pageContext,
            String contextPath, String fieldName, String idx,
            MetadataValue dcv, Collection collection)
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
                Choices cs = cam.getMatches(fieldName, dcv.getValue(), collection, 0, 0, null);
                if (cs.defaultSelected < 0)
                    sb.append("<option value=\"").append(dcv.getValue()).append("\" selected>")
                      .append(dcv.getValue()).append("</option>\n");

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
                   .append(dcv.getValue()).append("</textarea>\n<br/>\n");

                if (authority)
                {
                    String confidenceSymbol = Choices.getConfidenceText(dcv.getConfidence()).toLowerCase();
                    sb.append("<span class=\"col-md-1\">")
                      .append("<img id=\""+confidenceIndicator+"\"  title=\"")
                      .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.authority.confidence.description."+confidenceSymbol))
                      .append("\" class=\"ds-authority-confidence cf-"+ confidenceSymbol)
                      .append("\" src=\"").append(contextPath).append("/image/confidence/invisible.gif\" />")
                      .append("</span>");
                	sb.append("<span class=\"col-md-5\">")
                      .append("<input class=\"form-control\" type=\"text\" readonly value=\"")
                      .append(dcv.getAuthority() != null ? dcv.getAuthority() : "")
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
                 .append(String.valueOf(collection.getID())).append(",")
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

<dspace:layout style="submission" titlekey="jsp.tools.edit-item-form.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>"
               nocache="true">


    <%-- <h1>Edit Item</h1> --%>
        <h1><fmt:message key="jsp.tools.edit-item-form.title"/>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\") + \"#editmetadata\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
        </h1>
    
    <%-- <p><strong>PLEASE NOTE: These changes are not validated in any way.
    You are responsible for entering the data in the correct format.
    If you are not sure what the format is, please do NOT make changes.</strong></p> --%>
    <p class="alert alert-danger"><strong><fmt:message key="jsp.tools.edit-item-form.note"/></strong></p>

	<div class="row">
	<div class="col-md-9">
		<div class="panel panel-primary">
			<div class="panel-heading"><fmt:message key="jsp.tools.edit-item-form.details" /></div>

			<div class="panel-body">
				<table class="table">
					<tr>
						<td><fmt:message key="jsp.tools.edit-item-form.itemID" />
						</td>
						<td><%= item.getID() %></td>
					</tr>

					<tr>
						<td><fmt:message key="jsp.tools.edit-item-form.handle" />
						</td>
						<td><%= (handle == null ? "None" : handle) %></td>
					</tr>
					<tr>
						<td><fmt:message key="jsp.tools.edit-item-form.modified" />
						</td>
						<td><dspace:date
								date="<%= new DCDate(item.getLastModified()) %>" />
						</td>
					</tr>


					<%-- <td class="submitFormLabel">In Collections:</td> --%>
					<tr>
						<td><fmt:message key="jsp.tools.edit-item-form.collections" />
						</td>
						<td>
							<%  for (int i = 0; i < collections.size(); i++) { %> <%= collections.get(i).getName() %>
							<br /> <%  } %>
						</td>
					</tr>
					<tr>
						<%-- <td class="submitFormLabel">Item page:</td> --%>
						<td><fmt:message key="jsp.tools.edit-item-form.itempage" />
						</td>
						<td>
							<%  if (handle == null) { %> <em><fmt:message
									key="jsp.tools.edit-item-form.na" />
						</em> <%  } else {
    				String url = ConfigurationManager.getProperty("dspace.url") + "/handle/" + handle; %>
							<a target="_blank" href="<%= url %>"><%= url %></a> <%  } %>
						</td>
					</tr>


				</table>
			</div>
		</div>
	</div>

	<div class="col-md-3">
		<div class="panel panel-default">
			<div class="panel-heading"><fmt:message key="jsp.actiontools"/></div>
        	<div class="panel-body">
        	<%
    if (!item.isWithdrawn() && bWithdraw)
    {
%>
                    <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_WITHDRAW %>" />
                        <%-- <input type="submit" name="submit" value="Withdraw..."> --%>
						<input class="btn btn-warning col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.withdraw-w-confirm.button"/>"/>
                    </form>
<%
    }
    else if (item.isWithdrawn() && bReinstate)
    {
%>
                    <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.REINSTATE %>" />
                        <%-- <input type="submit" name="submit" value="Reinstate"> --%>
						<input class="btn btn-warning col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.reinstate.button"/>"/>
                    </form>
<%
    }
%>
<%
  if (bDelete)
  {
%>
                    <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_DELETE %>" />
                        <%-- <input type="submit" name="submit" value="Delete (Expunge)..."> --%>
                        <input class="btn btn-danger col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.delete-w-confirm.button"/>"/>
                    </form>
<%
  }
%>
<%
  if (isItemAdmin)
  {
%>                     
					<form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_MOVE_ITEM %>" />
						<input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.move-item.button"/>"/>
                    </form>
<%
  }
%>
<%
    if (item.isDiscoverable() && bPrivating)
    {
%>
                    <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_PRIVATING %>" />
                        <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.privating-w-confirm.button"/>"/>
                    </form>
<%
    }
    else if (!item.isDiscoverable() && bPublicize)
    {
%>
                    <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.PUBLICIZE %>" />
                        <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.publicize.button"/>"/>
                    </form>
<%
    }
%>

<%
  if (bPolicy)
  {
%>
	<%-- ===========================================================
     Edit item's policies
     =========================================================== --%>
							<form method="post"
								action="<%= request.getContextPath() %>/tools/authorize">
								<input type="hidden" name="handle"
									value="<%= ConfigurationManager.getProperty("handle.prefix") %>" />
								<input type="hidden" name="item_id" value="<%= item.getID() %>" />
								<%-- <input type="submit" name="submit_item_select" value="Edit..."> --%>
								<input class="btn btn-default col-md-12" type="submit"
									name="submit_item_select"
									value="<fmt:message key="jsp.tools.edit-item-form.item" />" />
							</form>
<%
  }
%>
<%
  if (isItemAdmin)
  {
%>
<%-- ===========================================================
     Curate Item
     =========================================================== --%>
							<form method="post"
								action="<%= request.getContextPath() %>/tools/curate">
								<input type="hidden" name="item_id" value="<%= item.getID() %>" />
								<input class="btn btn-default col-md-12" type="submit"
									name="submit_item_select"
									value="<fmt:message key="jsp.tools.edit-item-form.form.button.curate"/>" />
							</form>
					<%
						}
					%>
    	    </div>
        </div>
	</div>
    </div>


	
<%

    if (item.isWithdrawn())
    {
%>
    <%-- <p align="center"><strong>This item was withdrawn from DSpace</strong></p> --%>
        <p class="alert alert-warning"><fmt:message key="jsp.tools.edit-item-form.msg"/></p>
<%
    }
%>
    <form id="edit_metadata" name="edit_metadata" method="post" action="<%= request.getContextPath() %>/tools/edit-item">
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
    MetadataAuthorityService mam = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
    ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
    List<MetadataValue> dcv = ContentServiceFactory.getInstance().getItemService().getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
    String row = "even";
    
    // Keep a count of the number of values of each element+qualifier
    // key is "element" or "element_qualifier" (String)
    // values are Integers - number of values that element/qualifier so far
    Map<String, Integer> dcCounter = new HashMap<String, Integer>();
    
    for (int i = 0; i < dcv.size(); i++)
    {
        // Find out how many values with this element/qualifier we've found

        String key = dcv.get(i).getMetadataField().toString();

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
                <td headers="t0" class="<%= row %>RowOddCol"><%=dcv.get(i).getMetadataField().getMetadataSchema().getName() %></td>
                <td headers="t1" class="<%= row %>RowEvenCol"><%= dcv.get(i).getMetadataField().getElement() %>&nbsp;&nbsp;</td>
                <td headers="t2" class="<%= row %>RowOddCol"><%= (dcv.get(i).getMetadataField().getQualifier() == null ? "" : dcv.get(i).getMetadataField().getQualifier()) %></td>
                <td headers="t3" class="<%= row %>RowEvenCol">
                    <%
                        if (cam.isChoicesConfigured(key))
                        {
                    %>
                    <%=
                        doAuthority(mam, cam, pageContext, request.getContextPath(), key, sequenceNumber,
                                dcv.get(i), collection).toString()
                    %>
                    <% } else { %>
                        <textarea class="form-control" id="value_<%= key %>_<%= sequenceNumber %>" name="value_<%= key %>_<%= sequenceNumber %>" rows="3" cols="50"><%= dcv.get(i).getValue() %></textarea>
                    <% } %>
                </td>
                <td headers="t4" class="<%= row %>RowOddCol">
                    <input class="form-control" type="text" name="language_<%= key %>_<%= sequenceNumber %>" value="<%= (dcv.get(i).getLanguage() == null ? "" : dcv.get(i).getLanguage().trim()) %>" size="5"/>
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
<%  for (int i = 0; i < dcTypes.size(); i++)
    {
        Integer fieldID = new Integer(dcTypes.get(i).getID());
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
        
        <br/>

        <%-- <h2>Bitstreams</h2> --%>
                <h2><fmt:message key="jsp.tools.edit-item-form.heading"/></h2>

        <%-- <p>Note that if the "user format description" field isn't empty, the format will
        always be set to "Unknown", so clear the user format description before changing the
        format field.</p> --%>
                <p class="alert alert-warning"><fmt:message key="jsp.tools.edit-item-form.note3"/></p>
	<div class="table-responsive">
        <table id="bitstream-edit-form-table" class="table" summary="Bitstream data table">
            <tr>
          <%-- <th class="oddRowEvenCol"><strong>Primary<br>Bitstream</strong></th>
                <th class="oddRowOddCol"><strong>Name</strong></th>
                <th class="oddRowEvenCol"><strong>Source</strong></th>
                <th class="oddRowOddCol"><strong>Description</strong></th>
                <th class="oddRowEvenCol"><strong>Format</strong></th>
                <th class="oddRowOddCol"><strong>User&nbsp;Format&nbsp;Description</strong></th> --%>
                <th id="t10" class="oddRowEvenCol">&nbsp;</th>
                <th id="t11" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem5"/></strong></th>        
                <th id="t12" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem7"/></strong></th>
                <th id="t13" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem8"/></strong></th>
                <th id="t14" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem9"/></strong></th>
                <th id="t15" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem10"/></strong></th>
                <th id="t16" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem11"/></strong></th>
                <th id="t17" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem12"/></strong></th>
                <th id="t18" class="oddRowEvenCol">&nbsp;</th>
            </tr>
<%
    List<Bundle> bundles = item.getBundles();
    row = "even";

    for (int i = 0; i < bundles.size(); i++)
    {
        List<Bitstream> bitstreams = bundles.get(i).getBitstreams();
        for (int j = 0; j < bitstreams.size(); j++)
        {
            ArrayList<UUID> bitstreamIdOrder = new ArrayList<UUID>();
            for (Bitstream bitstream : bitstreams) {
                bitstreamIdOrder.add(bitstream.getID());
            }

            // Parameter names will include the bundle and bitstream ID
            // e.g. "bitstream_14_18_desc" is the description of bitstream 18 in bundle 14
            Bitstream bitstream = bitstreams.get(j);
            String key = bundles.get(i).getID() + "_" + (bitstream).getID();
            BitstreamFormat bf = (bitstream).getFormat(UIUtil.obtainContext(request));
%>
            <tr id="<%="row_" + bundles.get(i).getName() + "_" + bitstream.getID()%>">
            	<td headers="t10" class="<%= row %>RowEvenCol" align="center">
                	<%-- <a target="_blank" href="<%= request.getContextPath() %>/retrieve/<%= bitstream.getID() %>">View</a>&nbsp;<input type="submit" name="submit_delete_bitstream_<%= key %>" value="Remove"> --%>
					<a class="btn btn-info" target="_blank" href="<%= request.getContextPath() %>/retrieve/<%= bitstream.getID() %>"><fmt:message key="jsp.tools.general.view"/></a>&nbsp;
				</td>
                <% if (bundles.get(i).getName().equals("ORIGINAL"))
                   { %>
                     <td headers="t11" class="<%= row %>RowEvenCol" align="center">
                       <span class="form-control">
                       <input type="radio" name="<%= bundles.get(i).getID() %>_primary_bitstream_id" value="<%= bitstream.getID() %>"
                           <% if (bitstream.equals(bundles.get(i).getPrimaryBitstream())) { %>
                                  checked="<%="checked" %>"
                           <% } %> /></span>
                   </td>
                <% } else { %>
                     <td headers="t11"> </td>
                <% } %>
                <td headers="t12" class="<%= row %>RowOddCol">
                    <input class="form-control" type="text" name="bitstream_name_<%= key %>" value="<%= ((bitstream).getName() == null ? "" : Utils.addEntities(bitstream.getName())) %>"/>
                </td>
                <td headers="t13" class="<%= row %>RowEvenCol">
                    <input class="form-control" type="text" name="bitstream_source_<%= key %>" value="<%= ((bitstream).getSource() == null ? "" : bitstream.getSource()) %>"/>
                </td>
                <td headers="t14" class="<%= row %>RowOddCol">
                    <input class="form-control" type="text" name="bitstream_description_<%= key %>" value="<%= ((bitstream).getDescription() == null ? "" : Utils.addEntities(bitstream.getDescription())) %>"/>
                </td>
                <td headers="t15" class="<%= row %>RowEvenCol">
                    <input class="form-control" type="text" name="bitstream_format_id_<%= key %>" value="<%= bf.getID() %>" size="4"/> (<%= Utils.addEntities(bf.getShortDescription()) %>)
                </td>
                <td headers="t16" class="<%= row %>RowOddCol">
                    <input class="form-control" type="text" name="bitstream_user_format_description_<%= key %>" value="<%= ((bitstream).getUserFormatDescription() == null ? "" : Utils.addEntities(bitstream.getUserFormatDescription())) %>"/>
                </td>
<%
                   if (bundles.get(i).getName().equals("ORIGINAL") && breOrderBitstreams)
                   {
                       //This strings are only used in case the user has javascript disabled
                       String upButtonValue = null;
                       String downButtonValue = null;
                       if(0 != j){
                           ArrayList<UUID> temp = (ArrayList<UUID>) bitstreamIdOrder.clone();
                           //We don't have the first button, so create a value where the current bitstreamId moves one up
                           UUID tempInt = temp.get(j);
                           temp.set(j, temp.get(j - 1));
                           temp.set(j - 1, tempInt);
                           upButtonValue = StringUtils.join(temp.toArray(new UUID[temp.size()]), ",");
                       }
                       if(j < (bitstreams.size() -1)){
                           //We don't have the first button, so create a value where the current bitstreamId moves one up
                           ArrayList<UUID> temp = (ArrayList<UUID>) bitstreamIdOrder.clone();
                           UUID tempInt = temp.get(j);
                           temp.set(j, temp.get(j + 1));
                           temp.set(j + 1, tempInt);
                           downButtonValue = StringUtils.join(temp.toArray(new UUID[temp.size()]), ",");
                       }



%>
                <td headers="t17" class="<%= row %>RowEvenCol">
                    <input type="hidden" value="<%=j+1%>" name="order_<%=bitstream.getID()%>">
                    <input type="hidden" value="<%=upButtonValue%>" name="<%=bundles.get(i).getID()%>_<%=bitstream.getID()%>_up_value">
                    <input type="hidden" value="<%=downButtonValue%>" name="<%=bundles.get(i).getID()%>_<%=bitstream.getID()%>_down_value">
                    <div>
                        <button class="btn btn-default" name="submit_order_<%=key%>_up" value="<fmt:message key="jsp.tools.edit-item-form.move-up"/> " <%=j==0 ? "disabled=\"disabled\"" : ""%>>
                        	<span class="glyphicon glyphicon-arrow-up"></span>
                        </button>
                    </div>
                    <div>
                        <button class="btn btn-default" name="submit_order_<%=key%>_down" value="<fmt:message key="jsp.tools.edit-item-form.move-down"/> " <%=j==(bitstreams.size()-1) ? "disabled=\"disabled\"" : ""%>>
                        	<span class="glyphicon glyphicon-arrow-down"></span>
                        </button>
                    </div>
                </td>

<%
                   }else{
%>
                <td>
                    <%=j+1%>
                </td>
<%
                   }
%>
                <td headers="t18" class="<%= row %>RowEvenCol">

                                        <% if (bRemoveBits) { %>
                                        <button class="btn btn-danger" name="submit_delete_bitstream_<%= key %>" value="<fmt:message key="jsp.tools.general.remove"/>">
                                        	<span class="glyphicon glyphicon-trash"></span>
                                        </button>
                                        <% } %>
                </td>
            </tr>
<%
            row = (row.equals("odd") ? "even" : "odd");
        }
    }
%>
        </table>
	</div>
        

        <%-- <p align="center"><input type="submit" name="submit_addbitstream" value="Add Bitstream"></p> --%>
	<div class="btn-group col-md-12">
                <%
					if (bCreateBits) {
                %>                
					<input class="btn btn-success col-md-2" type="submit" name="submit_addbitstream" value="<fmt:message key="jsp.tools.edit-item-form.addbit.button"/>"/>
                <%  }
                    if(breOrderBitstreams){
                %>
                    <input class="hidden" type="submit" value="<fmt:message key="jsp.tools.edit-item-form.order-update"/>" name="submit_update_order" style="visibility: hidden;">
                <%
                    }

                        if (ConfigurationManager.getBooleanProperty("webui.submit.enable-cc") && bccLicense)
                        {
                                String s;
                                List<Bundle> ccBundle = ContentServiceFactory.getInstance().getItemService().getBundles(item, "CC-LICENSE");
                                s = ccBundle.size() > 0 ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-item-form.replacecc.button") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-item-form.addcc.button");
                %>
                    <input class="btn btn-success col-md-3" type="submit" name="submit_addcc" value="<%= s %>" />
                    <input type="hidden" name="handle" value="<%= ConfigurationManager.getProperty("handle.prefix") %>"/>
                    <input type="hidden" name="item_id" value="<%= item.getID() %>"/>
                    
       			<%
              		}
				%>
	


        <input type="hidden" name="item_id" value="<%= item.getID() %>"/>
        <input type="hidden" name="action" value="<%= EditItemServlet.UPDATE_ITEM %>"/>
					
                        <%-- <input type="submit" name="submit" value="Update" /> --%>
                        <input class="btn btn-primary pull-right col-md-3" type="submit" name="submit" value="<fmt:message key="jsp.tools.general.update"/>" />
                        <%-- <input type="submit" name="submit_cancel" value="Cancel" /> --%>
						<input class="btn btn-default pull-right col-md-3" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
					</div>
    </form>
</dspace:layout>
