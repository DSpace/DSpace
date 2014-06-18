<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  -
  - Attributes to pass in:
  -    license_url	- the CC license URL
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    String cc_license_url = (String) request.getParameter("license_url");
%>
<script type="text/javascript">
the_form = parent.document.getElementById("license_form");
the_form.cc_license_url.value = "<%= cc_license_url %>";
parent.document.getElementById("submit_grant").click();
</script>
