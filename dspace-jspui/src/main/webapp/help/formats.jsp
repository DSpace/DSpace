<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Formats JSP
  -
  - Note that this is a "stand-alone" JSP that is invoked directly, and not
  - via a Servlet.  
  - This page involves no user interaction, but needs to be a JSP so that it
  - can retrieve the bitstream formats from the database.
  -
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.sql.SQLException" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.apache.log4j.Logger" %>

<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.LogManager" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>

<%
    Context context = null;
    List<BitstreamFormat> formats = null;
    
    try
    {
        // Obtain a context so that the location bar can display log in status
        context = UIUtil.obtainContext(request);
      
       // Get the Bitstream formats
        formats = ContentServiceFactory.getInstance().getBitstreamFormatService().findAll(context);
    }
    catch (SQLException se) {
        // Database error occurred.
        Logger log = Logger.getLogger("org.dspace.jsp");
        log.warn(LogManager.getHeader(context,
                "database_error",
                se.toString()), se);

        // Also email an alert
        UIUtil.sendAlert(request, se);

        JSPManager.showInternalError(request, response);
    } 
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>
	<fmt:message key="jsp.help.formats.title"/></title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<link rel="stylesheet" href="../styles.css" type="text/css"/>
</head>
<body class="help">

<%-- <h2 align="center"><a name="top">DSpace Supported Formats</a></h2> --%>
<h2 align="center"><a name="top"><fmt:message key="jsp.help.formats.title"/></a></h2>
<p align="right"><a href="<%= LocaleSupport.getLocalizedMessage(pageContext, "help.index")%>"><fmt:message key="jsp.help.formats.return"/></a></p>

<%-- <h5><a href="#policy">Format Support Policy</a></h5> --%>
<h5><a href="#policy"><fmt:message key="jsp.help.formats.policy"/></a></h5>
<%-- <h5><a href="#formats">Format Support Levels</a></h5> --%>
<h5><a href="#formats"><fmt:message key="jsp.help.formats.support-levels"/></a></h5>
<%-- <h5><a href="#notlisted">What To Do If Your Format Isn't Listed</a></h5> --%>
<h5><a href="#notlisted"><fmt:message key="jsp.help.formats.whattodo"/></a></h5>
<p>&nbsp;</p>
<table>
    <tr>
    <%-- <td class="leftAlign"><a name="policy"></a><strong>FORMAT SUPPORT POLICY</strong></td> --%>
    <td class="leftAlign"><a name="policy"></a><strong><fmt:message key="jsp.help.formats.policy"/></strong></td>
    <%-- <td class="rightAlign"><a href="#top" align="right">top</a></td> --%>
    <td class="rightAlign"><a href="#top"><fmt:message key="jsp.help.formats.top"/></a></td>
    </tr>
</table>
<%-- <p><i>(Your Format Support Policy Here)</i></p> --%>
<p><i><fmt:message key="jsp.help.formats.here"/></i></p> 
<p>&nbsp;</p>
<table>
    <tr>
    <%-- <td class="leftAlign"><a name="formats"></a><strong>FORMAT SUPPORT LEVELS</strong></td> --%>
    <td class="leftAlign"><a name="formats"></a><strong><fmt:message key="jsp.help.formats.support-levels"/></strong></td>
    <%-- <td class="rightAlign"><a href="#top" align="right">top</a></td> --%>
    <td class="rightAlign"><a href="#top"><fmt:message key="jsp.help.formats.top"/></a></td>
    </tr>
</table>

<table class="formats" border="0" summary="List of formats">
    <tr>
        <%-- <th align="left"><strong>Name</strong></th> --%>
        <th id="t1" align="left"><strong><fmt:message key="jsp.help.formats.name"/></strong></th>
        <%-- <th align="left"><strong>Extensions</strong></th> --%>
        <th id="t2" align="left"><strong><fmt:message key="jsp.help.formats.extensions"/></strong></th>        
        <%-- <th align="left"><strong>MIME Type</strong></th> --%>
        <th id="t3" align="left"><strong><fmt:message key="jsp.help.formats.mime"/></strong></th>
        <%-- <th align="left"><strong>Support Level</strong></th> --%>
        <th id="t4" align="left"><strong><fmt:message key="jsp.help.formats.support"/></strong></th>
    </tr>

<%
    for (int i = 0; i < formats.size(); i++)
    {
        if ( formats.get(i).isInternal() )
            continue;

        List<String> extensions = formats.get(i).getExtensions();
        String extValue = "";

        for (int j = 0 ; j < extensions.size(); j++)
        {
            if (j > 0)
            {
                extValue = extValue + ", ";
            }
            extValue = extValue + extensions.get(j);
        }
%>
           <tr>
                <td headers="t1"><%= formats.get(i).getShortDescription() %></td>
                <td headers="t2"><%= extValue %></td>
                <td headers="t3"><%= formats.get(i).getMIMEType() %></td>
                <td headers="t4">
                <%
                    if(formats.get(i).getSupportLevel() == 2)
                    {
                            %><fmt:message key="jsp.help.formats.support.supported" /><%
                        
                    }
                    else if(formats.get(i).getSupportLevel() == 1)
                    {
                            %><fmt:message key="jsp.help.formats.support.known" /><%
                    }
                    else
                    {
                            %><fmt:message key="jsp.help.formats.support.unknown" /><%
                    }
                  %>
                </td>
        </tr>
<%
      }
      if(context != null && context.isValid())
      {
          context.abort();
      }
%>
    </table>
<p>&nbsp;</p>
<table>
    <tr>
    <%-- <td class="leftAlign"><a name="notlisted"></a><strong>WHAT TO DO IF YOUR FORMAT ISN'T LISTED</strong></td> --%>
    <td class="leftAlign"><a name="notlisted"></a><strong><fmt:message key="jsp.help.formats.whattodo"/></strong></td>
    <%-- <td class="rightAlign"><a href="#top" align="right">top</a></td> --%>
    <td class="rightAlign"><a href="#top"><fmt:message key="jsp.help.formats.top"/></a></td>
    </tr>
</table>
<p>
<%-- Please contact your <a href="#" onClick="javascript:window.open('../components/contact-info.jsp', 'contact', 'location=no,menubar=no,height=300,width=550,resizable')">DSpace Administrator</a>
if you have questions about a particular format. --%>
<fmt:message key="jsp.help.formats.contact1"/> <a href="#" onclick="javascript:window.open('../components/contact-info.jsp', 'contact', 'location=no,menubar=no,height=300,width=550,resizable')"><fmt:message key="jsp.help.formats.contact2"/></a>
<fmt:message key="jsp.help.formats.contact3"/>
</p>
<p>&nbsp;</p>
</body>
</html>
