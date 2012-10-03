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
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
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
    Item item = (Item) request.getAttribute("item");
    String handle = (String) request.getAttribute("handle");
    Collection[] collections = (Collection[]) request.getAttribute("collections");
    MetadataField[] dcTypes = (MetadataField[])  request.getAttribute("dc.types");
    HashMap metadataFields = (HashMap) request.getAttribute("metadataFields");
    request.setAttribute("LanguageSwitch", "hide");

    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    
    // Is the logged in user an admin of the item
    Boolean itemAdmin = (Boolean)request.getAttribute("admin_button");
    boolean isItemAdmin = (itemAdmin == null ? false : itemAdmin.booleanValue());
    
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

    Boolean reOrderBitstreams = (Boolean)request.getAttribute("reorder_bitstreams_button");
    boolean breOrderBitstreams = (reOrderBitstreams != null && reOrderBitstreams);

    // owning Collection ID for choice authority calls
    int collectionID = -1;
    if (collections.length > 0)
        collectionID = collections[0].getID();
%>
<%!
     StringBuffer doAuthority(MetadataAuthorityManager mam, ChoiceAuthorityManager cam,
            PageContext pageContext,
            String contextPath, String fieldName, String idx,
            DCValue dcv, int collectionID)
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
                sb.append("<select id=\"").append(fieldNameIdx)
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
                sb.append("<textarea id=\"").append(fieldNameIdx).append("\" name=\"").append(fieldNameIdx)
                   .append("\" rows=\"3\" cols=\"50\">")
                   .append(dcv.value).append("</textarea>\n<br/>\n");

                if (authority)
                {
                    String confidenceSymbol = Choices.getConfidenceText(dcv.confidence).toLowerCase();
                    sb.append("<img id=\""+confidenceIndicator+"\"  title=\"")
                      .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.authority.confidence.description."+confidenceSymbol))
                      .append("\" class=\"ds-authority-confidence cf-"+ confidenceSymbol)
                      .append("\" src=\"").append(contextPath).append("/image/confidence/invisible.gif\" />")
                      .append("<input type=\"text\" readonly value=\"")
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
                      .append("\" class=\"ds-authority-confidence-input\"/>");
                }
                 
               sb.append("<input type=\"image\" name=\"").append(fieldNameIdx).append("_lookup\" ")
                 .append("onclick=\"javascript: return DSpaceChoiceLookup('")
                 .append(contextPath).append("/tools/lookup.jsp','")
                 .append(fieldName).append("','edit_metadata','")
                 .append(fieldNameIdx).append("','").append(authorityName).append("','")
                 .append(confidenceIndicator).append("',")
                 .append(String.valueOf(collectionID)).append(",")
                 .append("false").append(",false);\"")
                 .append(" title=\"")
                 .append(LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.lookup.lookup"))
                 .append("\" width=\"16px\" height=\"16px\" src=\""+contextPath+"/image/authority/zoom.png\" />");
            }
        }
        return sb;
    }
%>

<dspace:layout titlekey="jsp.tools.edit-item-form.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

    <script type="text/javascript" src="<%= request.getContextPath() %>/dspace-admin/js/jquery-1.4.2.min.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/dspace-admin/js/bitstream-ordering.js"></script>


    <%-- <h1>Edit Item</h1> --%>
        <h1><fmt:message key="jsp.tools.edit-item-form.title"/></h1>
    
    <%-- <p><strong>PLEASE NOTE: These changes are not validated in any way.
    You are responsible for entering the data in the correct format.
    If you are not sure what the format is, please do NOT make changes.</strong></p> --%>
        <p><strong><fmt:message key="jsp.tools.edit-item-form.note"/></strong></p>
    
    <%-- <p><dspace:popup page="/help/collection-admin.html#editmetadata">More help...</dspace:popup></p>  --%>
        <div><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\") + \"#editmetadata\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></div>

    <center>
        <table width="70%" summary="Edit item table">
            <tr>
                <%-- <td class="submitFormLabel">Item&nbsp;internal&nbsp;ID:</td> --%>
                                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.itemID"/></td>
                <td class="standard"><%= item.getID() %></td>
                <td class="standard" width="100%" align="right" rowspan="5">
<%
    if (!item.isWithdrawn() && bWithdraw)
    {
%>
                    <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_WITHDRAW %>" />
                        <%-- <input type="submit" name="submit" value="Withdraw..."> --%>
                                                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.withdraw-w-confirm.button"/>"/>
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
                                                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.reinstate.button"/>"/>
                    </form>
<%
    }
%>

                    <br/>
<%
  if (bDelete)
  {
%>
                    <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_DELETE %>" />
                        <%-- <input type="submit" name="submit" value="Delete (Expunge)..."> --%>
                                                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.delete-w-confirm.button"/>"/>
                    </form>
<%
  }

  if (isItemAdmin)
  {
%>                     <form method="post" action="<%= request.getContextPath() %>/tools/edit-item">
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditItemServlet.START_MOVE_ITEM %>" />
                                                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-item-form.move-item.button"/>"/>
                    </form>
<%
  }
%>
                </td>
            </tr>
            <tr>
                <%-- <td class="submitFormLabel">Handle:</td> --%>
                                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.handle"/></td>
                <td class="standard"><%= (handle == null ? "None" : handle) %></td>
            </tr>
            <tr>
                <%-- <td class="submitFormLabel">Last modified:</td> --%>
                                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.modified"/></td>
                <td class="standard"><dspace:date date="<%= new DCDate(item.getLastModified()) %>" /></td>
            </tr>
            <tr>
                <%-- <td class="submitFormLabel">In Collections:</td> --%>
                                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.collections"/></td>
                <td class="standard">
<%  for (int i = 0; i < collections.length; i++) { %>
                    <%= collections[i].getMetadata("name") %><br/>
<%  } %>
                </td>
            </tr>

            <tr>
                <%-- <td class="submitFormLabel">Item page:</td> --%>
                                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.itempage"/></td>
                <td class="standard">
<%  if (handle == null) { %>
                    <em><fmt:message key="jsp.tools.edit-item-form.na"/></em>
<%  } else {
    String url = ConfigurationManager.getProperty("dspace.url") + "/handle/" + handle; %>
                    <a target="_blank" href="<%= url %>"><%= url %></a>
<%  } %>
                </td>
            </tr>
<%
  if (bPolicy)
  {
%>
<%-- ===========================================================
     Edit item's policies
     =========================================================== --%>
            <tr>
                <%-- <td class="submitFormLabel">Item's Authorizations:</td> --%>
                                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-item-form.item"/></td>
                <td>
                    <form method="post" action="<%= request.getContextPath() %>/tools/authorize">
                        <input type="hidden" name="handle" value="<%= ConfigurationManager.getProperty("handle.prefix") %>" />
                        <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                        <%-- <input type="submit" name="submit_item_select" value="Edit..."> --%>
                                                <input type="submit" name="submit_item_select" value="<fmt:message key="jsp.tools.general.edit"/>"/>
                    </form>
                </td>
            </tr>
<%
  }
%>
        </table>
    </center>

<%

    if (item.isWithdrawn())
    {
%>
    <%-- <p align="center"><strong>This item was withdrawn from DSpace</strong></p> --%>
        <p align="center"><strong><fmt:message key="jsp.tools.edit-item-form.msg"/></strong></p>
<%
    }
%>
    <p>&nbsp;</p>


    <form id="edit_metadata" name="edit_metadata" method="post" action="<%= request.getContextPath() %>/tools/edit-item">
        <table class="miscTable" summary="Edit item withdrawn table">
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
    DCValue[] dcv = item.getMetadata(Item.ANY, Item.ANY, Item.ANY, Item.ANY);
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
                        <textarea id="value_<%= key %>_<%= sequenceNumber %>" name="value_<%= key %>_<%= sequenceNumber %>" rows="3" cols="50"><%= dcv[i].value %></textarea>
                    <% } %>
                </td>
                <td headers="t4" class="<%= row %>RowOddCol">
                    <input type="text" name="language_<%= key %>_<%= sequenceNumber %>" value="<%= (dcv[i].language == null ? "" : dcv[i].language.trim()) %>" size="5"/>
                </td>
                <td headers="t5" class="<%= row %>RowEvenCol">
                    <%-- <input type="submit" name="submit_remove_<%= key %>_<%= sequenceNumber %>" value="Remove" /> --%>
                                        <input type="submit" name="submit_remove_<%= key %>_<%= sequenceNumber %>" value="<fmt:message key="jsp.tools.general.remove"/>"/>
                </td>
            </tr>
<%      row = (row.equals("odd") ? "even" : "odd");
    } %>

            <tr><td>&nbsp;</td></tr>

            <tr>
        
                <td headers="t1" colspan="3" class="<%= row %>RowEvenCol">
                    <select name="addfield_dctype">
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
                    <textarea name="addfield_value" rows="3" cols="50"></textarea>
                </td>
                <td headers="t4" class="<%= row %>RowEvenCol">
                    <input type="text" name="addfield_language" size="5"/>
                </td>
                <td headers="t5" class="<%= row %>RowOddCol">
                    <%-- <input type="submit" name="submit_addfield" value="Add"> --%>
                                        <input type="submit" name="submit_addfield" value="<fmt:message key="jsp.tools.general.add"/>"/>
                </td>
            </tr>
        </table>
        
        <p>&nbsp;</p>

        <%-- <h2>Bitstreams</h2> --%>
                <h2><fmt:message key="jsp.tools.edit-item-form.heading"/></h2>

        <%-- <p>Note that if the "user format description" field isn't empty, the format will
        always be set to "Unknown", so clear the user format description before changing the
        format field.</p> --%>
                <p><fmt:message key="jsp.tools.edit-item-form.note3"/></p>

        <table id="bitstream-edit-form-table" class="miscTable" summary="Bitstream data table">
            <tr>
          <%-- <th class="oddRowEvenCol"><strong>Primary<br>Bitstream</strong></th>
                <th class="oddRowOddCol"><strong>Name</strong></th>
                <th class="oddRowEvenCol"><strong>Source</strong></th>
                <th class="oddRowOddCol"><strong>Description</strong></th>
                <th class="oddRowEvenCol"><strong>Format</strong></th>
                <th class="oddRowOddCol"><strong>User&nbsp;Format&nbsp;Description</strong></th> --%>
                
                        <th id="t11" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem5"/></strong></th>
                <th id="t12" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem7"/></strong></th>
                <th id="t13" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem8"/></strong></th>
                <th id="t14" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem9"/></strong></th>
                <th id="t15" class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem10"/></strong></th>
                <th id="t16" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem11"/></strong></th>
                <th id="t17" class="oddRowOddCol"><strong><fmt:message key="jsp.tools.edit-item-form.elem12"/></strong></th>
                <th id="t18" class="oddRowEvenCol">&nbsp;</th>
            </tr>
<%
    Bundle[] bundles = item.getBundles();
    row = "even";

    for (int i = 0; i < bundles.length; i++)
    {
        Bitstream[] bitstreams = bundles[i].getBitstreams();
        for (int j = 0; j < bitstreams.length; j++)
        {
            ArrayList<Integer> bitstreamIdOrder = new ArrayList<Integer>();
            for (Bitstream bitstream : bitstreams) {
                bitstreamIdOrder.add(bitstream.getID());
            }

            // Parameter names will include the bundle and bitstream ID
            // e.g. "bitstream_14_18_desc" is the description of bitstream 18 in bundle 14
            String key = bundles[i].getID() + "_" + bitstreams[j].getID();
            BitstreamFormat bf = bitstreams[j].getFormat();
%>
            <tr id="<%="row_" + bundles[i].getName() + "_" + bitstreams[j].getID()%>">
                <% if (bundles[i].getName().equals("ORIGINAL"))
                   { %>
                     <td headers="t11" class="<%= row %>RowEvenCol" align="center">
                       <input type="radio" name="<%= bundles[i].getID() %>_primary_bitstream_id" value="<%= bitstreams[j].getID() %>"
                           <% if (bundles[i].getPrimaryBitstreamID() == bitstreams[j].getID()) { %>
                                  checked="<%="checked" %>"
                           <% } %> />
                   </td>
                <% } else { %>
                     <td headers="t11"> </td>
                <% } %>
                <td headers="t12" class="<%= row %>RowOddCol">
                    <input type="text" name="bitstream_name_<%= key %>" value="<%= (bitstreams[j].getName() == null ? "" : Utils.addEntities(bitstreams[j].getName())) %>"/>
                </td>
                <td headers="t13" class="<%= row %>RowEvenCol">
                    <input type="text" name="bitstream_source_<%= key %>" value="<%= (bitstreams[j].getSource() == null ? "" : bitstreams[j].getSource()) %>"/>
                </td>
                <td headers="t14" class="<%= row %>RowOddCol">
                    <input type="text" name="bitstream_description_<%= key %>" value="<%= (bitstreams[j].getDescription() == null ? "" : Utils.addEntities(bitstreams[j].getDescription())) %>"/>
                </td>
                <td headers="t15" class="<%= row %>RowEvenCol">
                    <input type="text" name="bitstream_format_id_<%= key %>" value="<%= bf.getID() %>" size="4"/> (<%= Utils.addEntities(bf.getShortDescription()) %>)
                </td>
                <td headers="t16" class="<%= row %>RowOddCol">
                    <input type="text" name="bitstream_user_format_description_<%= key %>" value="<%= (bitstreams[j].getUserFormatDescription() == null ? "" : Utils.addEntities(bitstreams[j].getUserFormatDescription())) %>"/>
                </td>
<%
                   if (bundles[i].getName().equals("ORIGINAL") && breOrderBitstreams)
                   {
                       //This strings are only used in case the user has javascript disabled
                       String upButtonValue = null;
                       String downButtonValue = null;
                       if(0 != j){
                           ArrayList<Integer> temp = (ArrayList<Integer>) bitstreamIdOrder.clone();
                           //We don't have the first button, so create a value where the current bitstreamId moves one up
                           Integer tempInt = temp.get(j);
                           temp.set(j, temp.get(j - 1));
                           temp.set(j - 1, tempInt);
                           upButtonValue = StringUtils.join(temp.toArray(new Integer[temp.size()]), ",");
                       }
                       if(j < (bitstreams.length -1)){
                           //We don't have the first button, so create a value where the current bitstreamId moves one up
                           ArrayList<Integer> temp = (ArrayList<Integer>) bitstreamIdOrder.clone();
                           Integer tempInt = temp.get(j);
                           temp.set(j, temp.get(j + 1));
                           temp.set(j + 1, tempInt);
                           downButtonValue = StringUtils.join(temp.toArray(new Integer[temp.size()]), ",");
                       }



%>
                <td headers="t17" class="<%= row %>RowEvenCol">
                    <input type="hidden" value="<%=j+1%>" name="order_<%=bitstreams[j].getID()%>">
                    <input type="hidden" value="<%=upButtonValue%>" name="<%=bundles[i].getID()%>_<%=bitstreams[j].getID()%>_up_value">
                    <input type="hidden" value="<%=downButtonValue%>" name="<%=bundles[i].getID()%>_<%=bitstreams[j].getID()%>_down_value">
                    <div>
                        <input name="submit_order_<%=key%>_up" type="submit" value="<fmt:message key="jsp.tools.edit-item-form.move-up"/> " <%=j==0 ? "disabled=\"disabled\"" : ""%>/>
                    </div>
                    <div>
                        <input name="submit_order_<%=key%>_down" type="submit" value="<fmt:message key="jsp.tools.edit-item-form.move-down"/> " <%=j==(bitstreams.length-1) ? "disabled=\"disabled\"" : ""%>/>
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
                    <%-- <a target="_blank" href="<%= request.getContextPath() %>/retrieve/<%= bitstreams[j].getID() %>">View</a>&nbsp;<input type="submit" name="submit_delete_bitstream_<%= key %>" value="Remove"> --%>
                                        <a target="_blank" href="<%= request.getContextPath() %>/retrieve/<%= bitstreams[j].getID() %>"><fmt:message key="jsp.tools.general.view"/></a>&nbsp;
                                        <% if (bRemoveBits) { %>
                                        <input type="submit" name="submit_delete_bitstream_<%= key %>" value="<fmt:message key="jsp.tools.general.remove"/>" />
                                        <% } %>
                </td>
            </tr>
<%
            row = (row.equals("odd") ? "even" : "odd");
        }
    }
%>
        </table>

        <p>&nbsp;</p>

        <%-- <p align="center"><input type="submit" name="submit_addbitstream" value="Add Bitstream"></p> --%>
        <center>
            <table width="70%" align="center">
                <tr>
                  <td>
                <%
                        if (bCreateBits) {
                %>
                                                <input type="submit" name="submit_addbitstream" value="<fmt:message key="jsp.tools.edit-item-form.addbit.button"/>"/>
                <%  }
                    if(breOrderBitstreams){
                %>
                    <input class="hidden" type="submit" value="<fmt:message key="jsp.tools.edit-item-form.order-update"/>" name="submit_update_order" style="visibility: hidden;">
                <%
                    }

                        if (ConfigurationManager.getBooleanProperty("webui.submit.enable-cc") && bccLicense)
                        {
                                String s;
                                Bundle[] ccBundle = item.getBundles("CC-LICENSE");
                                s = ccBundle.length > 0 ? LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-item-form.replacecc.button") : LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-item-form.addcc.button");
                %>
                    <input type="submit" name="submit_addcc" value="<%= s %>" />
                    <input type="hidden" name="handle" value="<%= ConfigurationManager.getProperty("handle.prefix") %>"/>
                    <input type="hidden" name="item_id" value="<%= item.getID() %>"/>
       <%
                        }
%>
                  </td>
                </tr>
            </table>
        </center>

        <p>&nbsp;</p>

        <input type="hidden" name="item_id" value="<%= item.getID() %>"/>
        <input type="hidden" name="action" value="<%= EditItemServlet.UPDATE_ITEM %>"/>
        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <%-- <input type="submit" name="submit" value="Update" /> --%>
                                                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.general.update"/>" />
                    </td>
                    <td align="right">

                        <%-- <input type="submit" name="submit_cancel" value="Cancel" /> --%>
                                                <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
