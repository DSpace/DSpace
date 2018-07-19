<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
%>
<dspace:layout locbar="commLink" title="Contacts" feedData="NONE">

    <table width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">


                <p class="text-primary"><strong><fmt:message key="jsp.library.department.contacts.title"/></strong></p>

                <p><fmt:message key="jsp.library.department.title"/><br/>
                    <fmt:message key="jsp.library.department.name"/><br/>
                    <fmt:message key="jsp.library.department.address"/></p>

                <p>e-mail: <a href="mailto:info@library.sumdu.edu.ua ">info@library.sumdu.edu.ua</a></p>

                <p><fmt:message key="jsp.library.department.phone"/></p>

            </td>
        </tr>
    </table>

    <br/>
</dspace:layout>
