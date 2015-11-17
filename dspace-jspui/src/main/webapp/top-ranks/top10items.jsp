<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="java.sql.*" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<% 
	org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request); 
%>

<dspace:layout locbar="commLink" titlekey="jsp.top50items" feedData="NONE">

<h2><fmt:message key="jsp.top50items"/></h2>

<table align="center" width="95%" border="0">
		            <tr>
		            	<th></th>
		            	<th style = "text-align:center;"><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.downloads"/></th>
		            </tr>
		            <tr>
		            	<td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
		            </tr>

<%

		try {
		    Connection c = null;
		    try {
		        Class.forName(ConfigurationManager.getProperty("db.driver"));
		    
		        c = DriverManager.getConnection(ConfigurationManager.getProperty("db.url"),
		                                        ConfigurationManager.getProperty("db.username"),
		                                        ConfigurationManager.getProperty("db.password"));
		
		        Statement s = c.createStatement();
		
		        ResultSet res = s.executeQuery("SELECT metadatavalue.resource_id, metadata_field_id, text_value, downloads" +
											    "	FROM metadatavalue" +
											    "	RIGHT JOIN (" +
											    "		SELECT item_id, SUM(view_cnt) AS downloads" +
											    "			FROM statistics" +
											    " 			WHERE sequence_id > 0 " +
											    "			GROUP BY item_id" + 
											    "			ORDER BY downloads DESC" +
											    "			LIMIT 10" +
											    "	) AS stat ON stat.item_id = metadatavalue.resource_id" +
											    "	WHERE metadata_field_id IN (25, 64) AND resource_type_id = 2" +
											    "	ORDER BY downloads DESC, resource_id, metadata_field_id");
		        int item_id;
		        long downloads;
		        String url;
		        String title;
                int i = 1;
		        while (res.next()) {
		            item_id = res.getInt("resource_id");
		            url = res.getString("text_value");
		            downloads = res.getLong("downloads");
		            title = "";
		            
		            if (res.next()) {
		            	title = res.getString("text_value");
		            }
		            
		            %>
		            <tr height="30">
		            	<td><a href="<%= url %>"><%= i %>. <%= title %></a></td>
		            	<td align="center"><%= downloads %></td>
		            </tr>
		            
		            <tr>
		            	<td colspan="2"><span style="width: 100%; display: block; border-bottom: 1px dotted #777;" /></td>
		            </tr>
		            
		            <%
                    i++;
		        }
		
		        s.close();
		    } finally {
		        if (c != null) 
		            c.close();
		    }
		} catch (Exception e) {
			e.printStackTrace();		
		}


%>
</table>
</dspace:layout>
