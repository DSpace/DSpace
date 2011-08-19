<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - eperson editor - for new or existing epeople
  -
  - Attributes:
  -   eperson - eperson to be edited
  -   email_exists - if non-null, user has attempted to enter a duplicate email
  -                  address, so an error should be displayed
  -
  - Returns:
  -   submit_save   - admin wants to save edits
  -   submit_delete - admin wants to delete edits
  -   submit_cancel - admin wants to cancel
  -
  -   eperson_id
  -   email
  -   firstname
  -   lastname
  -   phone
  -   language
  -   can_log_in          - (boolean)
  -   require_certificate - (boolean)
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.Locale"%>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.eperson.EPerson, org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.eperson.Group"   %>
<%@ page import="org.dspace.core.Utils" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");

	Group [] groupMemberships = null;
	if(request.getAttribute("group.memberships") != null)
	{
		groupMemberships = (Group []) request.getAttribute("group.memberships");
	}

    String email     = eperson.getEmail();
    String firstName = eperson.getFirstName();
    String lastName  = eperson.getLastName();
    String phone     = eperson.getMetadata("phone");
    String netid = eperson.getNetid();
    String language     = eperson.getMetadata("language");
    boolean emailExists = (request.getAttribute("email_exists") != null);

    boolean ldap_enabled = ConfigurationManager.getBooleanProperty("authentication-ldap", "enable");
%>

<dspace:layout titlekey="jsp.dspace-admin.eperson-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">


  <table width="95%">
    <tr>
      <td align="left">
        <%-- <h1>Edit EPerson <%= eperson.getEmail() %>:</h1> --%>
        <h1><fmt:message key="jsp.dspace-admin.eperson-edit.heading">
            <fmt:param><%= eperson.getEmail() %></fmt:param>
        </fmt:message></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#epeople\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

<% if (emailExists)
	{ %><p><strong>
	     <fmt:message key="jsp.dspace-admin.eperson-edit.emailexists"/>
	   </strong></p>
<%  } %>

    <form method="post" action="">

    <table class="miscTable" align="center">
        <tr>     
            <%-- <td>Email:</td> --%>         
            <td><label for="temail"><fmt:message key="jsp.dspace-admin.eperson-edit.email"/></label></td>
            <td>
                <input type="hidden" name="eperson_id" value="<%=eperson.getID()%>"/>
                <input name="email" id="temail" size="24" value="<%=email == null ? "" : email%>"/>
            </td>
        </tr>

        <tr>
            <%-- <td>Last Name:</td> --%>
            <td><label for="tlastname"><fmt:message key="jsp.dspace-admin.eperson.general.lastname"/></label></td>
            <td>
                <input name="lastname" id="tlastname" size="24" value="<%=lastName == null ? "" : Utils.addEntities(lastName) %>"/>
            </td>
        </tr>

        <tr>           
            <%-- <td>First Name:</td> --%>
            <td><label for="tfirstname"><fmt:message key="jsp.dspace-admin.eperson.general.firstname"/></label></td>
            <td>
                <input name="firstname" id="tfirstname" size="24" value="<%=firstName == null ? "" : Utils.addEntities(firstName) %>"/>
            </td>
        </tr>

        <% if (ldap_enabled) { %>
	<tr>
            <td>LDAP NetID:</td>
            <td>
                <input name="netid" size="24" value="<%=netid == null ? "" : Utils.addEntities(netid) %>" />
            </td>
        </tr>
        <% } %>

        <tr>
            <%-- <td>Phone:</td> --%>
            <td><label for="tphone"><fmt:message key="jsp.dspace-admin.eperson-edit.phone"/></label></td>
            <td>
                <input name="phone" id="tphone" size="24" value="<%=phone == null ? "" : Utils.addEntities(phone) %>"/>
            </td>
        </tr>
        <tr>
            <td><label for="tlanguage"><fmt:message key="jsp.register.profile-form.language.field"/></label></td>
            <td class="standard">
        		<select name="language" id="tlanguage">
<%
		Locale[] supportedLocales = I18nUtil.getSupportedLocales();

        for (int i = supportedLocales.length-1; i >= 0; i--)
        {
        	String lang = supportedLocales[i].toString();
        	String selected = "";
        	
        	if (language == null || language.equals(""))
        	{ if(lang.equals(I18nUtil.getSupportedLocale(request.getLocale()).getLanguage()))
        		{
        			selected = "selected=\"selected\"";
        		}
        	}
        	else if (lang.equals(language))
        	{ selected = "selected=\"selected\"";}
%>
          	 <option <%= selected %>
                value="<%= lang %>"><%= supportedLocales[i].getDisplayName(I18nUtil.getSupportedLocale(request.getLocale())) %></option>
<%
        }
%>
        	</select>
   		     </td>
        </tr>
   
        <tr>
            <%-- <td>Can Log In:</td> --%>
            <td><label for="tcan_log_in"><fmt:message key="jsp.dspace-admin.eperson-edit.can"/></label></td>
            <td>
                <input type="checkbox" name="can_log_in" id="tcan_log_in" value="true"<%= eperson.canLogIn() ? " checked=\"checked\"" : "" %> />
            </td>
        </tr>

        <tr>
            <%-- <td>Require Certificate:</td> --%>
            <td><label for="trequire_certificate"><fmt:message key="jsp.dspace-admin.eperson-edit.require"/></label></td>
            <td>
                <input type="checkbox" name="require_certificate" id="trequire_certificate" value="true"<%= eperson.getRequireCertificate() ? " checked=\"checked\"" : "" %> />
            </td>
        </tr>
    </table>

    <center>
        <table width="70%">
            <tr>
                <td align="left">
                    <%-- <input type="submit" name="submit_save" value="Save Edits"> --%>
                    <input type="submit" name="submit_save" value="<fmt:message key="jsp.dspace-admin.general.save"/>" />
                </td>
                <td align="right">
                    <%-- <input type="submit" name="submit_delete" value="Delete EPerson..."> --%>
                    <input type="submit" name="submit_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                </td>
            </tr>
        </table>
    </center>        

    </form>

<%
  if((groupMemberships != null) && (groupMemberships.length>0))
  {
%>
    <h3><fmt:message key="jsp.dspace-admin.eperson-edit.groups"/></h3>
    <ul>
	<%  for(int i=0; i<groupMemberships.length; i++)
     	{
        String myLink = groupMemberships[i].getName();
        String args   = "submit_edit&amp;group_id="+groupMemberships[i].getID();
        
        myLink = "<a href=\""
        +request.getContextPath()
        +"/tools/group-edit?"+args+"\">" + myLink + "</a>";
	%>
    	<li><%=myLink%></li>
	<%  } %>
    </ul>
<% } %>  

</dspace:layout>
