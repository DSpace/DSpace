<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - "No Theses" message
  -
  - This page displays a message informing the user that theses are not
  - presently accepted in DSpace, and that their submission has been removed.
  - FIX-ME: MIT-SPECIFIC
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<dspace:layout titlekey="jsp.submit.no-theses.title">

    <%-- <h1>Theses Not Accepted in DSpace</h1> --%>
	<h1><fmt:message key="jsp.submit.no-theses.title"/></h1>
    <%-- <p>DSpace does not currently accept individually-submitted
    theses, but you are encouraged to use the separate electronic thesis
    submission site supported by the Libraries and by MIT Information Systems to
    submit your thesis to the <a href="http://thesis.mit.edu">Digital Library of
    MIT Theses</a>.  To learn how to submit your thesis to that system, see <a
    href="http://web.mit.edu/etheses/www/etheses-home.html">Submitting
    an Electronic Thesis at MIT</a>.</p> --%>
	<p><fmt:message key="jsp.submit.no-theses.info1"/></p>

    <%-- <p>Because DSpace does not accept individually-submitted theses, your
    submission will not proceed; any files you have uploaded for the current item
    will not be stored.</p>  --%>
	<p><fmt:message key="jsp.submit.no-theses.info2"/></p> 

    <%-- <p>Please note that printed copies of your thesis are still the official
    requirement for your degree.  Due to important legal and record-keeping
    reasons, it is likely that in the future DSpace will work directly with the
    electronic thesis system to load groups of theses which have been officially
    vetted and approved.  Thanks for understanding.</p>  --%>
	<p><fmt:message key="jsp.submit.no-theses.info3"/></p> 

    <%-- <p>For more information please <strong>contact the DSpace site
    administrators</strong>:</p> --%>
	<p><fmt:message key="jsp.submit.no-theses.info4"/></p>

    <dspace:include page="/components/contact-info.jsp" />

    <%-- <p>Thank you for your interest in DSpace!</p> --%>
	<p><fmt:message key="jsp.submit.no-theses.info5"/></p>

</dspace:layout>
