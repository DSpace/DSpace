<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Templates to cover the attribute calls.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <!-- The last thing in the structural elements section are the templates to cover the attribute calls.
        Although, by default, XSL only parses elements and text, an explicit call to apply the attributes
        of children tags can still be made. This, in turn, requires templates that handle specific attributes,
        like the kind you see below. The chief amongst them is the pagination attribute contained by divs,
        which creates a new div element to display pagination information. -->


    <!-- A quick helper function used by the @pagination template for repetitive tasks -->
    <!-- checkbox and radio fields type uses this attribute -->
    <xsl:template match="@returnValue">
        <xsl:attribute name="value"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- used for image buttons -->
    <xsl:template match="@source">
        <xsl:attribute name="src"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- size and maxlength used by text, password, and textarea inputs -->
    <xsl:template match="@size">
        <xsl:attribute name="size"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- used by select element -->
    <xsl:template match="@evtbehavior">
        <xsl:param name="behavior" select="."/>
        <xsl:if test="normalize-space($behavior)='submitOnChange'">
            <xsl:attribute name="onchange">this.form.submit();</xsl:attribute>
        </xsl:if>
    </xsl:template>

    <xsl:template match="@maxlength">
        <xsl:attribute name="maxlength"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- "multiple" attribute is used by the <select> input method -->
    <xsl:template match="@multiple[.='yes']">
        <xsl:attribute name="multiple">multiple</xsl:attribute>
    </xsl:template>

    <!-- rows and cols attributes are used by textarea input -->
    <xsl:template match="@rows">
        <xsl:attribute name="rows"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <xsl:template match="@cols">
        <xsl:attribute name="cols"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- Add the HTML5 autofocus attribute to the input field -->
    <xsl:template match="@autofocus">
        <xsl:attribute name="autofocus"><xsl:value-of select="."/></xsl:attribute>
    </xsl:template>

    <!-- The general "catch-all" template for attributes matched, but not handled above -->
    <xsl:template match="@*"></xsl:template>

    <xsl:template match="dri:div[@n = 'masked-page-control']">
        <!--Do not render this division, this is handled by the xsl-->
    </xsl:template>

    <xsl:template match="dri:div[@n ='search-controls-gear']">
        <xsl:param name="position"/>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class"><xsl:value-of select="$position"/></xsl:with-param>
            </xsl:call-template>

            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <xsl:template match="@pagination">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test=". = 'simple'">
                <div class="pagination-simple clearfix {$position}">
                    <xsl:variable name="gear"
                                  select="//dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-controls'
                                  or @id='aspect.administrative.WithdrawnItems.div.browse-controls'
                                  or @id='aspect.administrative.PrivateItems.div.browse-controls'
                                  or @id='aspect.discovery.SearchFacetFilter.div.browse-controls']"/>
                    <xsl:choose>
                        <xsl:when test="$position = 'top' and $gear">
                            <div class="row">
                                <div class="col-xs-10">
                                    <p class="pagination-info">
                                        <i18n:translate>
                                            <xsl:choose>
                                                <xsl:when test="parent::node()/@itemsTotal = -1">
                                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info.nototal
                                                    </i18n:text>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                            <i18n:param>
                                                <xsl:value-of select="parent::node()/@firstItemIndex"/>
                                            </i18n:param>
                                            <i18n:param>
                                                <xsl:value-of select="parent::node()/@lastItemIndex"/>
                                            </i18n:param>
                                            <i18n:param>
                                                <xsl:value-of select="parent::node()/@itemsTotal"/>
                                            </i18n:param>
                                        </i18n:translate>
                                    </p>
                                </div>
                                <div class="col-xs-2">
                                    <xsl:call-template name="renderSortOptionsMenu"/>
                                </div>
                            </div>
                        </xsl:when>
                        <xsl:when test="$position = 'top'">
                            <p class="pagination-info">
                                <i18n:translate>
                                    <xsl:choose>
                                        <xsl:when test="parent::node()/@itemsTotal = -1">
                                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info.nototal</i18n:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <i18n:param>
                                        <xsl:value-of select="parent::node()/@firstItemIndex"/>
                                    </i18n:param>
                                    <i18n:param>
                                        <xsl:value-of select="parent::node()/@lastItemIndex"/>
                                    </i18n:param>
                                    <i18n:param>
                                        <xsl:value-of select="parent::node()/@itemsTotal"/>
                                    </i18n:param>
                                </i18n:translate>
                            </p>
                        </xsl:when>
                    </xsl:choose>

                    <xsl:variable name="prev-page" select="parent::node()/@previousPage"/>
                    <xsl:variable name="next-page" select="parent::node()/@nextPage"/>
                    <xsl:if test="not($position = 'top') and ($prev-page or $next-page)">
                        <ul class="pagination">
                            <li>
                                <xsl:attribute name="class">
                                    <xsl:text>previous</xsl:text>
                                    <xsl:if test="not($prev-page)">
                                        <xsl:text> disabled</xsl:text>
                                    </xsl:if>
                                </xsl:attribute>

                                <a class="previous-page-link">
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="$prev-page"/>
                                    </xsl:attribute>
                                    <span class="glyphicon glyphicon-arrow-left"></span>
                                </a>
                            </li>
                            <li>
                                <xsl:attribute name="class">
                                    <xsl:text>next pull-right</xsl:text>
                                    <xsl:if test="not($next-page)">
                                        <xsl:text> disabled</xsl:text>
                                    </xsl:if>
                                </xsl:attribute>

                                <a class="next-page-link">
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="$next-page"/>
                                    </xsl:attribute>
                                    <span class="glyphicon glyphicon-arrow-right"></span>
                                </a>
                            </li>
                        </ul>
                    </xsl:if>

                </div>
                <ul class="ds-artifact-list list-unstyled"></ul>
            </xsl:when>
            <xsl:when test=". = 'masked'">
                <div class="pagination-masked clearfix {$position}">
                    <xsl:variable name="gear"
                                  select="parent::node()/dri:div[@n = 'masked-page-control']/dri:div[@rend='controls-gear-wrapper' and @n='search-controls-gear']"/>
                    <xsl:choose>
                        <xsl:when test="$position = 'top' and $gear">
                            <div class="row">
                                <div class="col-xs-9">
                                    <p class="pagination-info">
                                        <i18n:translate>
                                            <xsl:choose>
                                                <xsl:when test="parent::node()/@itemsTotal = -1">
                                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info.nototal
                                                    </i18n:text>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                            <i18n:param>
                                                <xsl:value-of select="parent::node()/@firstItemIndex"/>
                                            </i18n:param>
                                            <i18n:param>
                                                <xsl:value-of select="parent::node()/@lastItemIndex"/>
                                            </i18n:param>
                                            <i18n:param>
                                                <xsl:value-of select="parent::node()/@itemsTotal"/>
                                            </i18n:param>
                                        </i18n:translate>
                                    </p>
                                </div>
                                <div class="col-xs-3">
                                    <xsl:apply-templates select="$gear"/>
                                </div>
                            </div>
                        </xsl:when>
                        <xsl:when test="$position = 'top'">
                            <p class="pagination-info">
                                <i18n:translate>
                                    <xsl:choose>
                                        <xsl:when test="parent::node()/@itemsTotal = -1">
                                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info.nototal</i18n:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <i18n:text>xmlui.dri2xhtml.structural.pagination-info</i18n:text>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <i18n:param>
                                        <xsl:value-of select="parent::node()/@firstItemIndex"/>
                                    </i18n:param>
                                    <i18n:param>
                                        <xsl:value-of select="parent::node()/@lastItemIndex"/>
                                    </i18n:param>
                                    <i18n:param>
                                        <xsl:value-of select="parent::node()/@itemsTotal"/>
                                    </i18n:param>
                                </i18n:translate>
                            </p>
                        </xsl:when>
                    </xsl:choose>

                    <xsl:variable name="is-first-page"
                                  select="parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1"/>
                    <xsl:variable name="has-next-page"
                                  select="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)"/>
                    <xsl:if test="not($position = 'top') and (not($is-first-page) or $has-next-page)">
                        <div class="centered-pagination">
                            <ul class="pagination">
                                <li>
                                    <xsl:if test="$is-first-page">
                                        <xsl:attribute name="class">disabled</xsl:attribute>
                                    </xsl:if>
                                    <a class="previous-page-link">
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                                    select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                            <xsl:value-of select="parent::node()/@currentPage - 1"/>
                                            <xsl:value-of
                                                    select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        </xsl:attribute>
                                        <span class="glyphicon glyphicon-arrow-left"></span>
                                    </a>
                                </li>
                                <xsl:if test="(parent::node()/@currentPage - 4) &gt; 0">
                                    <li class="first-page-link">
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                        select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                                <xsl:text>1</xsl:text>
                                                <xsl:value-of
                                                        select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                            </xsl:attribute>
                                            <xsl:text>1</xsl:text>
                                        </a>
                                    </li>
                                    <li>
                                        <span>
                                            <xsl:text> . . . </xsl:text>
                                        </span>

                                    </li>
                                </xsl:if>
                                <xsl:call-template name="offset-link">
                                    <xsl:with-param name="pageOffset">-3</xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="offset-link">
                                    <xsl:with-param name="pageOffset">-2</xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="offset-link">
                                    <xsl:with-param name="pageOffset">-1</xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="offset-link">
                                    <xsl:with-param name="pageOffset">0</xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="offset-link">
                                    <xsl:with-param name="pageOffset">1</xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="offset-link">
                                    <xsl:with-param name="pageOffset">2</xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="offset-link">
                                    <xsl:with-param name="pageOffset">3</xsl:with-param>
                                </xsl:call-template>
                                <xsl:if test="(parent::node()/@currentPage + 4) &lt;= (parent::node()/@pagesTotal)">
                                    <li>
                                        <span>
                                            <xsl:text>. . .</xsl:text>
                                        </span>

                                    </li>
                                    <li class="last-page-link">
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                        select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                                <xsl:value-of select="parent::node()/@pagesTotal"/>
                                                <xsl:value-of
                                                        select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                            </xsl:attribute>
                                            <xsl:value-of select="parent::node()/@pagesTotal"/>
                                        </a>
                                    </li>
                                </xsl:if>
                                <xsl:if test="$has-next-page">
                                    <li>
                                        <a class="next-page-link">
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                        select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                                <xsl:value-of select="parent::node()/@currentPage + 1"/>
                                                <xsl:value-of
                                                        select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                            </xsl:attribute>
                                            <span class="glyphicon glyphicon-arrow-right"></span>
                                        </a>
                                    </li>
                                </xsl:if>

                            </ul>
                        </div>
                    </xsl:if>


                    <xsl:if test="parent::node()/dri:div[@n = 'masked-page-control']">
                        <xsl:apply-templates
                                select="parent::node()/dri:div[@n='masked-page-control']/dri:div[not(@rend='controls-gear-wrapper' and @n='search-controls-gear')]">
                            <xsl:with-param name="position" select="$position"/>
                        </xsl:apply-templates>
                    </xsl:if>
                </div>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="offset-link">
        <xsl:param name="pageOffset"/>
        <xsl:variable name="distance" select="$pageOffset*($pageOffset >=0) - $pageOffset*($pageOffset &lt; 0)"/>
        <xsl:if test="((parent::node()/@currentPage + $pageOffset) &gt; 0) and
            ((parent::node()/@currentPage + $pageOffset) &lt;= (parent::node()/@pagesTotal))">
            <li class="page-link page-link-offset-{$distance}">
                <xsl:if test="$pageOffset = 0">
                    <xsl:attribute name="class">active</xsl:attribute>
                </xsl:if>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                        <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                    </xsl:attribute>
                    <xsl:value-of select="parent::node()/@currentPage + $pageOffset"/>
                </a>
            </li>
        </xsl:if>
    </xsl:template>

    <xsl:template name="renderSortOptionsMenu">
        <div class="btn-group sort-options-menu pull-right">
            <xsl:call-template name="renderGearButton"/>
            <ul class="dropdown-menu pull-right" role="menu">
                <xsl:for-each
                        select="//dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-controls'
                        or @id='aspect.administrative.WithdrawnItems.div.browse-controls'
                        or @id='aspect.administrative.PrivateItems.div.browse-controls'
                        or @id='aspect.discovery.SearchFacetFilter.div.browse-controls']//dri:field[@type='select']">
                    <xsl:if test="position() > 1">
                        <li class="divider"/>
                    </xsl:if>
                    <li class="dropdown-header">
                        <xsl:apply-templates select="preceding-sibling::i18n:text[1]"/>
                    </li>
                    <xsl:for-each select="dri:option">
                        <li>
                            <a href="#" data-returnvalue="{@returnValue}" data-name="{../@n}">
                                <span aria-hidden="true">
                                    <xsl:attribute name="class">
                                        <xsl:text>glyphicon glyphicon-ok btn-xs</xsl:text>
                                        <xsl:choose>
                                            <xsl:when test="@returnValue = ../dri:value/@option">
                                                <xsl:text> active</xsl:text>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:text> invisible</xsl:text>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:attribute>
                                </span>
                                <xsl:choose>
                                    <xsl:when test="i18n:text">
                                        <xsl:apply-templates select="i18n:text"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="."/>
                                    </xsl:otherwise>
                                </xsl:choose>


                            </a>
                        </li>
                    </xsl:for-each>
                </xsl:for-each>
            </ul>
        </div>
    </xsl:template>

    <xsl:template name="renderGearButton">
        <button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
            <span class="glyphicon glyphicon-cog" aria-hidden="true"/>
        </button>
    </xsl:template>

</xsl:stylesheet>
