<%--
  - formats.jsp
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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

<%
    Context context = null;
    BitstreamFormat[] formats = null;
    
    try
    {
        // Obtain a context so that the location bar can display log in status
        context = UIUtil.obtainContext(request);
      
       // Get the Bitstream formats
        formats = BitstreamFormat.findAll(context);
    }
    catch (SQLException se)
    {
        // Database error occurred.
        Logger log = Logger.getLogger("org.dspace.jsp");
        log.warn(LogManager.getHeader(context,
            "database_error",
            se.toString()), se);

        // Also email an alert
        UIUtil.sendAlert(request, se);

        JSPManager.showInternalError(request, response);
    }
    finally {
        context.abort();
    }
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
<title>
	<fmt:message key="jsp.help.formats.title"/></title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
<link rel="stylesheet" href="../styles.css.jsp" type="text/css"/>
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
    for (int i = 0; i < formats.length; i++)
    {
        if ( formats[i].isInternal() )
            continue;

        String[] extensions = formats[i].getExtensions();
        String extValue = "";

        for (int j = 0 ; j < extensions.length; j++)
        {
            if (j > 0)
            {
                extValue = extValue + ", ";
            }
            extValue = extValue + extensions[j];
        }
%>
           <tr>
                <td headers="t1"><%= formats[i].getShortDescription() %></td>
                <td headers="t2"><%= extValue %></td>
                <td headers="t3"><%= formats[i].getMIMEType() %></td>
                <td headers="t4">
                <%
                    if(formats[i].getSupportLevel() == 2)
                    {
                            %><fmt:message key="jsp.help.formats.support.supported" /><%
                        
                    }
                    else if(formats[i].getSupportLevel() == 1)
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
