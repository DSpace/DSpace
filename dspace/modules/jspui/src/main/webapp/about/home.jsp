<%--
  - home.jsp
  -
  - Version: $Revision: 3705 $
  -
  - Date: $Date: 2009-04-11 17:02:24 +0000 (Sat, 11 Apr 2009) $
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
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
%>

<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="">

    <table  width="95%" align="center">
      <tr align="right">
        <td align="right">
<% if (supportedLocales != null && supportedLocales.length > 1){%>
        <form method="get" name="repost" action="">
          <input type ="hidden" name ="locale"/>
        </form>
<%
for (int i = supportedLocales.length-1; i >= 0; i--)
{
%>
        <a class ="langChangeOn"

                  onclick="javascript:document.repost.locale.value='<%=supportedLocales[i].toString()%>';
                  document.repost.submit();">
                 <%= supportedLocales[i].getDisplayLanguage(supportedLocales[i])%>
        </a> &nbsp;
<%
}
}
%>
        </td>
      </tr>
      <tr>
            <h2>Purpose of the DataSpace Repository</h2>
<p>
DataSpace is a digital repository meant for both archiving and publicly disseminating digital data that are the result of research, academic, or administrative work performed by members of the Princeton University community. DataSpace will promote awareness of the contents of the repository and help to ensure their long-term accessibility.
</p>

<p>
Benefits of using DataSpace to publish and archive your data include:
</p>

<ul>
<li>One-time charge for long-term storage of digital data</li>
<li>Persistent URLs pointing to each submission will continue to work for years to come, even if the underlying data is migrated to another system</li>
<li>Increases the visibility of your work</li>
<li>Makes worldwide dissemination of your work easy</li>
</ul>

<p>
Types of data that are well suited for storage in DataSpace include:

<ul>
<li>Research datasets that are to be independently or to which other print publications make reference</li>
<li>Student projects and reports</li>
<li>Conference and workshop proceedings</li>
<li>Technical reports</li>
<li>Digital collections of images, videos, or other digital assets created by members of the University</li>
</ul>
</p>

            <h2>Open Access to Content</h2>
<p>Content stored in DataSpace will be made openly available to the public via the DataSpace website.  DataSpace content will be organized into “Communities” which ty
pically represent collections of content from academic and administrative departments at the University.  Users can browse the collections within these communities
 by title, author, subject, or date of content submission.  Users can also search the content within and across collections.
</p>
<p>
Users can subscribe to news feeds that deliver notifications of new submissions to Communities within DataSpace.  The feeds are delivered using the popular RSS pro
tocol.  Internet Explorer, FireFox, or other popular RSS readers can then be used to review the list of recent submissions.
</p>
<p>
In the future we plan to register the repository with various online directories to which help promote awareness.  We also plan to make the repository metadata ava
ilable to services such as OAIster.org which can be used to search the records of repositories around the world.
</p>

            <h2>Submitting Your Content</h2>

<p>
Any member of the University may submit appropriate content to DataSpace.  The content must:

<ul>
<li>Be intended for public access</li>
<li>Be a completed work</li>
<li>Have potential long-term value (e.g. beyond 10 years)</li>
<li>Be the result of work performed at or related to Princeton University</li>
</ul>
</p>

<p>Before submitting your content, you must be a member of a Community defined in DataSpace.  Communities correspond roughly to administrative departments, academic departments, or other long-lived organizational units at Princeton.  The department or organizational unit must identify an administrator who will be responsible for reviewing content submissions for the Community.  Once the Community is established, you must request a DataSpace user account in order to submit content.  Your account will then enable you to submit content at any time for review and acceptance to the repository.
</p>

<p>
You must accept a licensing agreement which will allow Princeton University the non-exclusive right to preserve and disseminate your submitted work.
</p>

<p>
A one-time charge will be assessed for each submission.  The cost is calculated based on the amount of storage consumed by the submission.  Current (as of 7/1/2009) charges are $0.006 per Megabyte with a $0.60 minimum charge per submission.  As part of the submission process you must supply a valid project grant code to which the charges will be applied.
</p>

            <h2>Policies &amp; Guidelines</h2>

<p>A more complete description of policies and guidelines governing the DataSpace service are available as a PDF document:  <a href="DataSpacePnG.pdf">DataSpace Policies and Guidelines</a>.</p>

        </tr>
    </table>

    <dspace:sidebar>

    </dspace:sidebar>
</dspace:layout>
