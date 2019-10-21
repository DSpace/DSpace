<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri='http://java.sun.com/jsp/jstl/core' prefix='c' %>
<%@taglib prefix="essuir" tagdir="/WEB-INF/tags/essuir"%>

<dspace:layout locbar="commLink" titlekey="browse.page-author">

    <c:if test="${not isExtended}">
        <c:set var="divClass" value="col-md-offset-2 col-md-8"/>
    </c:if>

    <div class="${divClass}">
        <div class="panel panel-primary">
            <div class="panel-heading text-center">
                <fmt:message key="browse.full.range">
                    <fmt:param value="${startIndex}"/>
                    <fmt:param value="${finishIndex}"/>
                    <fmt:param value="${totalItems}"/>
                </fmt:message>
                <a href="#" class="pull-right glyphicon glyphicon-filter" aria-hidden="true"  data-toggle="modal" data-target="#searchModal"></a>
            </div>

            <c:choose>
                <c:when test="${isExtended}">
                    <essuir:browseExtendedTable items="${items}" />
                </c:when>
                <c:otherwise>
                    <essuir:browseSimpleTable items="${items}" type="${type}"/>
                </c:otherwise>
            </c:choose>

            <div class="panel-footer text-center">
                <essuir:pagination links="${links}" prevPageUrl="${prevPageUrl}" prevPageDisabled="${prevPageDisabled}" nextPageUrl="${nextPageUrl}" nextPageDisabled="${nextPageDisabled}"/>
            </div>
        </div>
    </div>

    <div class="modal fade" id="searchModal" tabindex="-1" role="dialog" aria-labelledby="searchModalLabel"
         aria-hidden="true">
        <div class="modal-dialog" role="document">
            <div class="modal-content">
                <form action="" method="get" autocomplete="off">
                    <input type="hidden" name="type" value="${type}"/>
                    <div class="modal-header">

                        <h4 class="modal-title" id="searchModalLabel"><fmt:message key="jsp.search.filter.applied"/>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </h4>
                    </div>
                    <div class="modal-body">
                        <essuir:sortOptionField sortOptions="${sortOptions}" sortedBy="${sortedBy}"/>
                        <essuir:sortOrderField sortOrder="${sortOrder}" />
                        <essuir:resultsPerPageField rpp="${rpp}" />

                        <c:choose>
                            <c:when test="${\"dateissued\" == type}">
                                <div class="form-group row">
                                    <label for="year" class="col-sm-6 col-form-label"><fmt:message key="browse.nav.date.jump"/></label>
                                    <div class="col-sm-6">
                                        <input type="text" class="yearpicker form-control" value="${selectedYear}" name="year" id="year"/>
                                    </div>
                                </div>

                                <script>
                                    $(document).ready(function(){
                                        $('.yearpicker').yearpicker();
                                        $('.yearpicker').val("${selectedYear}");
                                    });
                                </script>
                            </c:when>
                            <c:otherwise>
                                <div class="form-group row">
                                    <label for="starts_with" class="col-sm-6 col-form-label"><fmt:message key="browse.nav.date.jump"/></label>
                                    <div class="col-sm-6">
                                        <input type="text" class="form-control" value="${selectedYear}" name="starts_with" id="starts_with"/>
                                    </div>
                                </div>
                            </c:otherwise>
                        </c:choose>



                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-dismiss="modal"><fmt:message key="jsp.tools.group-select-list.close.button"/></button>
                        <button type="submit" class="btn btn-primary"><fmt:message key="browse.nav.go"/></button>
                    </div>
                </form>
            </div>
        </div>
    </div>

</dspace:layout>