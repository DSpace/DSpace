<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ taglib prefix="dspace" uri="http://www.dspace.org/dspace-tags.tld" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>

<dspace:layout title="${title}">
<c:if test="${canEdit}">
    <dspace:sidebar>
        <div class="panel panel-warning">
            <div class="panel-heading"><fmt:message key="jsp.admintools"/></div>
            <div class="panel-body">
                <form method="get" action="<%= request.getContextPath() %>/tools/edit-item">
                    <input type="hidden" name="item_id" value="${itemId}" />
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/mydspace">
                    <input type="hidden" name="item_id" value="${itemId}" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.mydspace.request.export.item"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/mydspace">
                    <input type="hidden" name="item_id" value="${itemId}" />
                    <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.mydspace.request.export.migrateitem"/>" />
                </form>
                <form method="post" action="<%= request.getContextPath() %>/dspace-admin/metadataexport">
                    <input type="hidden" name="handle" value="${handle}" />
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
                </form>

            </div>
        </div>
    </dspace:sidebar>
</c:if>



    <div id="fb-root"></div>
    <script async defer crossorigin="anonymous"
            src="https://connect.facebook.net/ru_RU/sdk.js#xfbml=1&version=v4.0"></script>
    <script>!function (d, s, id) {
        var js, fjs = d.getElementsByTagName(s)[0], p = /^http:/.test(d.location) ? 'http' : 'https';
        if (!d.getElementById(id)) {
            js = d.createElement(s);
            js.id = id;
            js.src = p + '://platform.twitter.com/widgets.js';
            fjs.parentNode.insertBefore(js, fjs);
        }
    }(document, 'script', 'twitter-wjs');</script>

    <div class="panel panel-default">
        <div class="panel-body">
            <fmt:message key="jsp.display-item.identifier"/>
            <code>${uri}</code>
            <br/>
            <div class="row" style="margin-top:3px">
                <div class="col-md-11">
                    <fmt:message key="jsp.suggestbar.title"/>:

                    <a href="https://twitter.com/share" class="twitter-share-button" data-hashtags="SumDU" >Tweet</a>
                    <div class="fb-share-button" data-href="https://essuir.sumdu.edu.ua/" data-layout="button"
                         style="vertical-align: top;"
                         data-size="small"><a target="_blank"
                                              href="https://www.facebook.com/sharer/sharer.php?u=https%3A%2F%2Fessuir.sumdu.edu.ua%2F&amp;src=sdkpreparse"
                                              class="fb-xfbml-parse-ignore">Поделиться</a>
                    </div>

                    <a href="/suggest?handle=${handle}" class="btn btn-info btn-xs" style="vertical-align: top;"><fmt:message key="jsp.display-item.suggest"/></a>

                </div>

            </div>

        </div>
    </div>


    <table class="table table-hover">
        <tr>
            <td><fmt:message key="org.dspace.app.webui.jsptag.ItemListTag.title"/></td>
            <td>${title}</td>
        </tr>
        <c:if test="${not empty titlesAlternative}">
            <tr>
                <td><fmt:message key="metadata.dc.title.alternative"/></td>
                    <td>
                        <c:forEach items="${titlesAlternative}" var="titleAlternative">
                            ${titleAlternative}<br/>
                        </c:forEach>
                    </td>
            </tr>
        </c:if>
        <tr>
            <td><fmt:message key="org.dspace.app.webui.jsptag.ItemListTag.authors"/></td>
            <td>
                <c:forEach items="${authors}" var="author">
                    <a href="/browse?type=author&value=${author.key}">${author.key}</a>
                    <c:if test="${not empty author.value}">
                        &nbsp;<a href = "http://orcid.org/${author.value}"><img src = "/static/img/orcid.gif" width="16px"></a>
                    </c:if>
                    <br/>
                </c:forEach>
            </td>
        </tr>
        <tr>
            <td><fmt:message key="metadata.dc.subject"/></td>
            <td>
                <c:forEach items="${keywords}" var="keyword">
                    <a href="/browse?type=subject&value=${keyword}">${keyword}</a><br/>
                </c:forEach>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="metadata.dc.type"/></td>
            <td>${type}</td>
        </tr>

        <tr>
            <td><fmt:message key="org.dspace.app.webui.jsptag.ItemListTag.issueDate"/></td>
            <td>${year}</td>
        </tr>

        <tr>
            <td><fmt:message key="metadata.dc.identifier.uri"/></td>
            <td><a href="${uri}">${uri}</a></td>
        </tr>

        <tr>
            <td><fmt:message key="metadata.dc.publisher"/></td>
            <td>${publisher}</td>
        </tr>

        <tr>
            <td><fmt:message key="jsp.submit.progressbar.license"/></td>
            <td>${rights}</td>
        </tr>
        <tr>
            <td><fmt:message key="metadata.dc.identifier.citation"/></td>
            <td>${citation}</td>
        </tr>
        <tr>
            <td><fmt:message key="metadata.dc.description.abstract"/></td>
            <td>
                <c:forEach items="${abstracts}" var="abstractText">
                    ${abstractText}<br/>
                </c:forEach>
            </td>
        </tr>

        <tr>
            <td><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.appears"/></td>
            <td>
                <c:forEach items="${owningCollections}" var="collection">
                    <a href="/handle/${collection.handle}">${collection.name}</a> <br/>
                </c:forEach>
            </td>
        </tr>
    </table>
    <div class="row">
        <div class="col-md-6">
            <div class="panel panel-info">
                <div class="panel-heading text-center"><h3 class="panel-title">Views</h3></div>
                <div class="panel-body">
                    <c:forEach items="${views}" var="country">
                        <div class="row">
                            <div class="col-md-8">
                                <img src="/flags/${country.countryCode.toLowerCase()}.gif"
                                     alt="${country.countryName}"> ${country.countryName}
                            </div>
                            <div class="col-md-3">
                                    ${country.count}
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>

        <div class="col-md-6">
            <div class="panel panel-info">
                <div class="panel-heading text-center"><h3 class="panel-title">Downloads</h3></div>
                <div class="panel-body">
                    <c:forEach items="${downloads}" var="country">
                        <div class="row">
                            <div class="col-md-8">
                                <img src="/flags/${country.countryCode.toLowerCase()}.gif"
                                     alt="${country.countryName}"> ${country.countryName}
                            </div>
                            <div class="col-md-3">
                                    ${country.count}
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </div>
    </div>
    <div class="row">

    </div>


    <div class="panel panel-info">
        <div class="panel-heading text-center"><h3 class="panel-title">Files</h3></div>
        <div class="panel-body">
            <table class="table">
                <thead>
                <tr>
                    <th><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.file"/></th>
                    <th><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.filesize"/></th>
                    <th><fmt:message key="org.dspace.app.webui.jsptag.ItemTag.fileformat"/></th>
                    <th>Downloads</th>
                        <%--<th></th>--%>
                </tr>
                </thead>
                <tbody>
                <c:forEach items="${bundles}" var="bundle">
                    <tr>
                        <td><a href="${bundle.link}">${bundle.filename}</a></td>
                        <td>${bundle.size}</td>
                        <td>${bundle.format}</td>
                        <td>${bundle.downloadCount}</td>
                            <%--<td><a class="btn btn-primary" target="_blank" href="/bitstream/123456789/29791/1/m3451.pdf">Download</a></td>--%>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
    <p class="submitFormHelp alert alert-info"><fmt:message key="jsp.display-item.copyright"/></p>
</dspace:layout>

<%
    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);
    context.complete();
%>