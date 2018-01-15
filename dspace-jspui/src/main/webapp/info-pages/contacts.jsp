<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="java.util.Locale" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout locbar="commLink" title="Contacts" feedData="NONE">

    <%
        Locale sessionLocale = UIUtil.getSessionLocale(request);
        String locale = sessionLocale.toString();
    %>

    <table width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">
                <%
                    if (locale.equals("en")) {
                %>
                <p class="text-primary"><strong>Contact Archive Administrator</strong></p>

                <p>Central Library SSU<br/>
                    Virtual reading room<br/>
                    2nd floor, room. 213</p>

                <p>e-mail: <a href="mailto:info@library.sumdu.edu.ua">library@sumdu.edu.ua</a></p>

                <p>tel. 687-820</p>
                <%
                } else if (locale.equals("ru")) {
                %>
                <p class="text-primary"><strong>Контакты администратора Репозитария</strong></p>

                <p>Центральная библиотека СумГУ<br/>
                    Виртуальный читальный зал<br/>
                    ІІ этаж, ком. 213</p>

                <p>e-mail: <a href="mailto:info@library.sumdu.edu.ua">library@sumdu.edu.ua</a></p>

                <p>тел. 687-820</p>
                <%
                } else {
                %>
                <p class="text-primary"><strong>Контакти адміністратора архіву</strong></p>

                <p>Центральна бібліотека СумДУ<br/>
                    Віртуальна читальна зала<br/>
                    2 поверх, кім. 213</p>

                <p>e-mail: <a href="mailto:info@library.sumdu.edu.ua">library@sumdu.edu.ua</a></p>

                <p>тел. 687-820</p>
                <%
                    }
                %>

            </td>
        </tr>
    </table>

    <br/>
</dspace:layout>
