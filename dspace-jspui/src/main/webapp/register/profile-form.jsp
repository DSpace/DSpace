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

<%@ page import="java.util.Locale"%>

<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>

<%
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    EPerson epersonForm = (EPerson) request.getAttribute("eperson");

    String lastName = "";
    String firstName = "";
    String phone = "";
    String language = "";

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
    }
%>

<table border="0" align="center" cellpadding="5">
    <tr>
        <%-- <td align="right" class="standard"><strong>First name*:</strong></td> --%>
		<td align="right" class="standard"><strong><fmt:message key="jsp.register.profile-form.fname.field"/></strong></td>
        <td class="standard"><input type="text" name="first_name" id="tfirst_name" size="40" value="<%= Utils.addEntities(firstName) %>"/></td>
    </tr>
    <tr>
        <%-- <td align="right" class="standard"><label for="tlast_name"><strong>Last name*:</strong></label></td> --%>
		<td align="right" class="standard"><label for="tlast_name"><strong><fmt:message key="jsp.register.profile-form.lname.field"/></strong></label></td>
        <td class="standard"><input type="text" name="last_name" id="tlast_name" size="40" value="<%= Utils.addEntities(lastName) %>" /></td>
    </tr>
    <tr>
        <%-- <td align="right" class="standard"><strong>Contact telephone:</strong></td> --%>
		<td align="right" class="standard"><label for="tphone"><strong><fmt:message key="jsp.register.profile-form.phone.field"/></strong></label></td>
        <td class="standard"><input type="text" name="phone" id="tphone" size="40" maxlength="32" value="<%= Utils.addEntities(phone) %>"/></td>
    </tr>
        <tr>
 		<td align="right" class="standard"><label for="tlanguage"><strong><fmt:message key="jsp.register.profile-form.language.field"/></strong></label></td>
 		<td class="standard">
        <select name="language" id="tlanguage">
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
        </td>
     </tr>
</table>
