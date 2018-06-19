<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show the changes that might be made
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.bulkedit.BulkEditChange" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.app.bulkedit.BulkEditMetadataValue" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    // Get the changes
    ArrayList<BulkEditChange> changes = (ArrayList<BulkEditChange>)request.getAttribute("changes");
    String changeCount = "" + changes.size();
    
    // Are these changes to be made, or that have been made
    boolean changed = ((Boolean)request.getAttribute("changed")).booleanValue();

    // If changes are allowed to be made
    boolean allow = ((Boolean)request.getAttribute("allow")).booleanValue();
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.metadataimport.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin" 
               nocache="true">

    <h1><fmt:message key="jsp.dspace-admin.metadataimport.title"/></h1>
<%
    // Warn the user if they are not allowed to make the changes
    if (!allow)
    {
        %>
            <p><strong><fmt:message key="jsp.dspace-admin.metadataimport.toomany"/></strong></p>
        <%
    }

    // Tell the user the import has finished (if applicable)
    if (changed)
    {
        %>
            <p><strong><fmt:message key="jsp.dspace-admin.metadataimport.finished">
                <fmt:param value="<%= changeCount %>" />
            </fmt:message></strong></p>
        <%
    }
%>

    <table class="table">

        <%
            // Display the changes
            int changeCounter = 0;
            for (BulkEditChange change : changes)
            {
                // Get the changes
                List<BulkEditMetadataValue> adds = change.getAdds();
                List<BulkEditMetadataValue> removes = change.getRemoves();
                List<Collection> newCollections = change.getNewMappedCollections();
                List<Collection> oldCollections = change.getOldMappedCollections();
                boolean first = false;
                if ((adds.size() > 0) || (removes.size() > 0) ||
                    (newCollections.size() > 0) || (oldCollections.size() > 0) ||
                    (change.getNewOwningCollection() != null) || (change.getOldOwningCollection() != null) ||
                    (change.isDeleted()) || (change.isWithdrawn()) || (change.isReinstated()))
                {
                    // Show the item
                    if (!change.isNewItem())
                    {
                        Item i = change.getItem();
                        %><tr><th bgcolor="white"><fmt:message key="jsp.dspace-admin.metadataimport.changesforitem"/>: <%= i.getID() %> (<%= i.getHandle() %>)</th><%
                    }
                    else
                    {
                        %><tr><th bgcolor="white"><fmt:message key="jsp.dspace-admin.metadataimport.newitem"/>:</th><%
                    }
                    changeCounter++;
                    first = true;
                }

                // Show actions
                if (change.isDeleted())
                {
                    if (!first)
                    {
                        %><tr><td bgcolor="white"></td><%
                    }
                    else
                    {
                        first = false;
                    }
                    if (!changed)
                    {
                        %><td bgcolor="#9B30FF" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.delete"/></td><td bgcolor="#9B30FF" style="font-size:10pt"></td></tr><%
                    }
                    else
                    {
                        %><td bgcolor="#9B30FF" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.deleted"/></td><td bgcolor="#9B30FF" style="font-size:10pt"></td></tr><%
                    }
                }
                if (change.isWithdrawn())
                {
                    if (!first)
                    {
                        %><tr><td bgcolor="white"></td><%
                    }
                    else
                    {
                        first = false;
                    }
                    if (!changed)
                    {
                        %><td bgcolor="#9B30FF" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.withdraw"/></td><td bgcolor="#9B30FF" style="font-size:10pt"></td></tr><%
                    }
                    else
                    {
                        %><td bgcolor="#9B30FF" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.withdrawn"/></td><td bgcolor="#9B30FF" style="font-size:10pt"></td></tr><%
                    }
                }
                if (change.isReinstated())
                {
                    if (!first)
                    {
                        %><tr><td bgcolor="white"></td><%
                    }
                    else
                    {
                        first = false;
                    }
                    if (!changed)
                    {
                        %><td bgcolor="#9B30FF" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.reinstate"/></td><td bgcolor="#9B30FF" style="font-size:10pt"></td></tr><%
                    }
                    else
                    {
                        %><td bgcolor="#9B30FF" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.reinstated"/></td><td bgcolor="#9B30FF" style="font-size:10pt"></td></tr><%
                    }
                }

                // Show new owner collection
                if (change.getNewOwningCollection() != null)
                {
                    Collection c = change.getNewOwningCollection();
                    if (c != null)
                    {
                        String cHandle = c.getHandle();
                        String cName = c.getName();
                        if (!first)
                        {
                            %><tr><td bgcolor="white"></td><%
                        }
                        else
                        {
                            first = false;
                        }
                        if (!changed)
                        {
                            %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.addtoownercollection"/></td><td bgcolor="#4E9258" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                        }
                        else
                        {
                            %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.addedtoownercollection"/></td><td bgcolor="#4E9258" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                        }
                    }
                }

                // Show old owner collection
                if (change.getOldOwningCollection() != null)
                {
                    Collection c = change.getOldOwningCollection();
                    if (c != null)
                    {
                        String cHandle = c.getHandle();
                        String cName = c.getName();
                        if (!first)
                        {
                            %><tr><td bgcolor="white"></td><%
                        }
                        else
                        {
                            first = false;
                        }
                        if (!changed)
                        {
                            %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.removefromownercollection"/></td><td bgcolor="#4E9258" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                        }
                        else
                        {
                            %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.removedfromownercollection"/></td><td bgcolor="#4E9258" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                        }
                    }
                }

                // Show new collections
                for (Collection c : newCollections)
                {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!first)
                    {
                        %><tr><td bgcolor="white"></td><%
                    }
                    else
                    {
                        first = false;
                    }
                    if (!changed)
                    {
                        %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.addtocollection"/></td><td bgcolor="#4E9258" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                    }
                    else
                    {
                        %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.addedtocollection"/></td><td bgcolor="#4E9258" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                    }
                }

                // Show old collections
                for (Collection c : oldCollections)
                {
                    String cHandle = c.getHandle();
                    String cName = c.getName();
                    if (!first)
                    {
                        %><tr><td bgcolor="white"></td><%
                    }
                    else
                    {
                        first = false;
                    }
                    if (!changed)
                    {
                        %><td bgcolor="#98AFC7" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.removefromcollection"/></td><td bgcolor="#98AFC7" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                    }
                    else
                    {
                        %><td bgcolor="#98AFC7" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.removedfromcollection"/></td><td bgcolor="#98AFC7" style="font-size:10pt">(<%= cHandle %>): <%= cName %></td></tr><%
                    }
                }

                // Show additions
                for (BulkEditMetadataValue dcv : adds)
                {
                    String md = dcv.getSchema() + "." + dcv.getElement();
                    if (dcv.getQualifier() != null)
                    {
                        md += "." + dcv.getQualifier();
                    }
                    if (dcv.getLanguage() != null)
                    {
                        md += "[" + dcv.getLanguage() + "]";
                    }
                    if (!first)
                    {
                        %><tr><td bgcolor="white"></td><%
                    }
                    else
                    {
                        first = false;
                    }
                    if (!changed)
                    {
                        %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.add"/> (<%= md %>)</td><td bgcolor="#4E9258" style="font-size:10pt"><%= dcv.getValue() %></td></tr><%
                    }
                    else
                    {
                        %><td bgcolor="#4E9258" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.added"/> (<%= md %>)</td><td bgcolor="#4E9258" style="font-size:10pt"><%= dcv.getValue() %></td></tr><%
                    }
                }

                // Show removals
                for (BulkEditMetadataValue dcv : removes)
                {
                    String md = dcv.getSchema() + "." + dcv.getElement();
                    if (dcv.getQualifier() != null)
                    {
                        md += "." + dcv.getQualifier();
                    }
                    if (dcv.getLanguage() != null)
                    {
                        md += "[" + dcv.getLanguage() + "]";
                    }
                    if (!first)
                    {
                        %><tr><td bgcolor="white"></td><%
                    }
                    else
                    {
                        first = false;
                    }
                    if (!changed)
                    {
                        %><td bgcolor="#98AFC7" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.remove"/> (<%= md %>)</td><td bgcolor="#98AFC7" style="font-size:10pt"><%= dcv.getValue() %></td></tr><%
                    }
                    else
                    {
                        %><td bgcolor="#98AFC7" style="font-size:10pt"><fmt:message key="jsp.dspace-admin.metadataimport.removed"/> (<%= md %>)</td><td bgcolor="#98AFC7" style="font-size:10pt"><%= dcv.getValue() %></td></tr><%
                    }
                }
            }
        %>

        </table>
        
        <%
            if ((!changed) && (allow))
            {
        %>
        <p align="center">
            <form method="post" action="">
                <input type="hidden" name="type" value="confirm" />
                <input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.dspace-admin.metadataimport.apply"/>" />
            </form>
            <form method="post" action="">
                <input type="hidden" name="type" value="cancel" />
                <input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
            </form>
        </p>
        <%
            }
        %>
    
    
</dspace:layout>
