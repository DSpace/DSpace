<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - User profile editing form.
  -
  - This isn't a full page, just the fields for entering a user's profile.
  -
  - Attributes to pass in:
  -   eperson       - the EPerson to edit the profile for.  Can be null,
  -                   in which case blank fields are displayed.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.util.UIUtil"%>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.I18nUtil" %>

<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.storage.rdbms.DatabaseManager" %>
<%@ page import="org.dspace.storage.rdbms.TableRow" %>
<%@ page import="org.dspace.storage.rdbms.TableRowIterator" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.Locale" %>

<%
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    EPerson epersonForm = (EPerson) request.getAttribute("eperson");
//    System.out.println(epersonForm.getClass());
    String lastName = "";
    String firstName = "";
    String phone = "";
    String language = "";
    int chairid = -1;
    String position = "";

    if (epersonForm != null)
    {
        // Get non-null values
        lastName = epersonForm.getLastName();
        if (lastName == null) lastName = "";

        firstName = epersonForm.getFirstName();
        if (firstName == null) firstName = "";

        phone = epersonForm.getMetadata("phone");
        if (phone == null) phone = "";

        language = epersonForm.getMetadata("language");
        if (language == null) language = "";

        chairid = epersonForm.getChair();
//        System.out.println(chairid);
        position = epersonForm.getPosition();
        if (position == null) position = "";
    }
%>
	<div class="form-group">
		<label class="col-md-offset-3 col-md-2 control-label" for="first_name"><fmt:message key="jsp.register.profile-form.fname.field"/></label>
        <div class="col-md-3"><input class="form-control" type="text" name="first_name" id="tfirst_name" size="40" value="<%= Utils.addEntities(firstName) %>"/></div>
	</div>
	<div class="form-group">
        <%-- <td align="right" class="standard"><label for="tlast_name"><strong>Last name*:</strong></label></td> --%>
		<label class="col-md-offset-3 col-md-2 control-label" for="tlast_name"><fmt:message key="jsp.register.profile-form.lname.field"/></label>
        <div class="col-md-3"><input class="form-control" type="text" name="last_name" id="tlast_name" size="40" value="<%= Utils.addEntities(lastName) %>" /></div>
    </div>
	<div class="form-group">
		<label class="col-md-offset-3 col-md-2 control-label" for="tphone"><fmt:message key="jsp.register.profile-form.phone.field"/></label>
        <div class="col-md-3"><input class="form-control" type="text" name="phone" id="tphone" size="40" maxlength="32" value="<%= Utils.addEntities(phone) %>"/></div>
    </div>
    <div class="form-group">
		<label class="col-md-offset-3 col-md-2 control-label" for="tlanguage"><strong><fmt:message key="jsp.register.profile-form.language.field"/></strong></label>
 		<div class="col-md-3">
        <select class="form-control" name="language" id="tlanguage">
<%
        for (int i = supportedLocales.length-1; i >= 0; i--)
        {
        	String lang = supportedLocales[i].toString();
        	String selected = "";

        	if (language.equals(""))
        	{ if(lang.equals(I18nUtil.getSupportedLocale(request.getLocale()).getLanguage()))
        		{
        			selected = "selected=\"selected\"";
        		}
        	}
        	else if (lang.equals(language))
        	{ selected = "selected=\"selected\"";}
%>
           <option <%= selected %>
                value="<%= lang %>"><%= supportedLocales[i].getDisplayName(UIUtil.getSessionLocale(request)) %></option>
<%
        }
%>
        </select>
        </div>
     </div>

<script type="text/javascript" src="../static/js/linkedselect.js"></script>

<%
    Context context = UIUtil.obtainContext(request);

    int faculty = -1;
    int startIndex = 0;
    int count;

    StringBuilder sb = new StringBuilder();

    StringBuilder sb1 = new StringBuilder();
    StringBuilder sb2 = new StringBuilder();
    try {


        TableRowIterator tri = null;

        int faculty_id;
        try {
            tri = DatabaseManager.query(context, "SELECT faculty_id, chair_id, chair_name " +
                    "  FROM chair " +
                    "  ORDER BY faculty_id, chair_id ");

            faculty_id = 0;
            count = 0;
            sb.setLength(0);

            while (tri.hasNext()) {
                TableRow row = tri.next();

                if (row.getIntColumn("faculty_id") != faculty_id && faculty_id > 0) {

                    sb1.append("'faculty_" + faculty_id + "':{");
                    sb1.append(sb.substring(0, sb.length() - 1));
                    sb1.append("},\n");

                    sb.setLength(0);
                    count = 0;
                }


                faculty_id = row.getIntColumn("faculty_id");

                if (row.getIntColumn("chair_id") == chairid) {
                    startIndex = count;
                    faculty = faculty_id;
                }

                sb.append("'" + row.getIntColumn("chair_id") + "':");
                sb.append("'" + row.getStringColumn("chair_name") + "',");

                sb2.append("'" + row.getIntColumn("chair_id") + "':{},");

                count++;
            }


            if (faculty_id > 0) {
                sb1.append("'faculty_" + faculty_id + "':{");
                sb1.append(sb.substring(0, sb.length() - 1));
                sb1.append("},\n");
            }

        } finally {
            if (tri != null)
                tri.close();
        }
    } catch (SQLException e) {
%><%=e.toString()%><%
    }

    sb.setLength(0);

    sb.append("<select class=\"form-control\" name=\"faculty\" id=\"faculty\"><option value=\"0\"></option>");
    try {
        TableRowIterator tri = null;
        int i = 1;
        try {
            tri = DatabaseManager.query(context, "SELECT faculty_id, faculty_name FROM faculty ORDER BY faculty_id ");

            while (tri.hasNext()) {
                TableRow row = tri.next();

                i = row.getIntColumn("faculty_id");

                sb.append("<option value=\"faculty_" + i)
                        .append(faculty == i ? "\" selected=\"selected\"" : "" )
                        .append("\">" + row.getStringColumn("faculty_name") + "</option>");
            }
        } finally {
            if (tri != null)
                tri.close();
        }
    } catch (SQLException e) {
%><%=e.toString()%><%
    }

    sb.append("</select>");
%>

<div class="form-group">
    <%-- <td>Faculty*:</td> --%>
    <label class="col-md-offset-3 col-md-2 control-label" for="faculty"><fmt:message key="jsp.dspace-admin.eperson.general.faculty"/></label>
    <div class="col-md-3"><%=sb.toString()%></div>
</div>
<div class="form-group">
    <%-- <td>Chair*:</td> --%>
    <label class="col-md-offset-3 col-md-2 control-label" for="chair_id"><fmt:message key="jsp.dspace-admin.eperson.general.chair"/></label>
    <div class="col-md-3">
        <select class="form-control" name="chair_id" id="chair_id"></select>

        <script type="text/javascript">
            var syncList1 = new syncList;

            startIndex = <%=startIndex %>;

            syncList1.dataList = {
                <%=sb1.toString()%>
                <%=sb2.substring(0, sb2.length() - 1)%>
            };

            syncList1.sync("faculty","chair_id");
        </script>
    </div>
</div>

<div class="form-group">
    <%-- <td>Position*:</td> --%>
    <label class="col-md-offset-3 col-md-2 control-label" for="tposition"><fmt:message key="jsp.dspace-admin.eperson.general.position"/></label>
    <div class="col-md-3">
        <input class="form-control" name="position" id="tposition" size="24" value="<%=position == null ? "" : Utils.addEntities(position) %>"/>
    </div>
</div>