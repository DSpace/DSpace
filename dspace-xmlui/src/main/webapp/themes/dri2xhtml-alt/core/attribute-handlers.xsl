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

    <xsl:template match="@pagination">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test=". = 'simple'">
                <div class="pagination {$position}">
                    <xsl:if test="parent::node()/@previousPage">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@previousPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                        </a>
                    </xsl:if>
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
                            <i18n:param><xsl:value-of select="parent::node()/@firstItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@lastItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@itemsTotal"/></i18n:param>
                        </i18n:translate>
                        <!--
                        <xsl:text>Now showing items </xsl:text>
                        <xsl:value-of select="parent::node()/@firstItemIndex"/>
                        <xsl:text>-</xsl:text>
                        <xsl:value-of select="parent::node()/@lastItemIndex"/>
                        <xsl:text> of </xsl:text>
                        <xsl:value-of select="parent::node()/@itemsTotal"/>
                            -->
                    </p>
                    <xsl:if test="parent::node()/@nextPage">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="parent::node()/@nextPage"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                        </a>
                    </xsl:if>
                </div>
            </xsl:when>
            <xsl:when test=". = 'masked'">
                <div class="pagination-masked {$position}">
                    <xsl:if test="not(parent::node()/@firstItemIndex = 0 or parent::node()/@firstItemIndex = 1)">
                        <a class="previous-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage - 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-previous</i18n:text>
                        </a>
                    </xsl:if>
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
                            <i18n:param><xsl:value-of select="parent::node()/@firstItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@lastItemIndex"/></i18n:param>
                            <i18n:param><xsl:value-of select="parent::node()/@itemsTotal"/></i18n:param>
                        </i18n:translate>
                    </p>
                    <ul class="pagination-links">
                        <xsl:if test="(parent::node()/@currentPage - 4) &gt; 0">
                            <li class="first-page-link">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:text>1</xsl:text>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:text>1</xsl:text>
                                </a>
                                <xsl:text> . . . </xsl:text>
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
                            <li class="last-page-link">
                                <xsl:text> . . . </xsl:text>
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                        <xsl:value-of select="parent::node()/@pagesTotal"/>
                                        <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                                    </xsl:attribute>
                                    <xsl:value-of select="parent::node()/@pagesTotal"/>
                                </a>
                            </li>
                        </xsl:if>
                    </ul>
                    <xsl:if test="not(parent::node()/@lastItemIndex = parent::node()/@itemsTotal)">
                        <a class="next-page-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="substring-before(parent::node()/@pageURLMask,'{pageNum}')"/>
                                <xsl:value-of select="parent::node()/@currentPage + 1"/>
                                <xsl:value-of select="substring-after(parent::node()/@pageURLMask,'{pageNum}')"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.structural.pagination-next</i18n:text>
                        </a>
                    </xsl:if>
                    <xsl:if test="parent::node()/dri:div[@n = 'masked-page-control']">
                        <xsl:apply-templates select="parent::node()/dri:div[@n='masked-page-control']/dri:div">
                            <xsl:with-param name="position" select="$position"/>
                        </xsl:apply-templates>
                    </xsl:if>
                </div>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <!-- A quick helper function used by the @pagination template for repetitive tasks -->
    <xsl:template name="offset-link">
        <xsl:param name="pageOffset"/>
        <xsl:if test="((parent::node()/@currentPage + $pageOffset) &gt; 0) and
            ((parent::node()/@currentPage + $pageOffset) &lt;= (parent::node()/@pagesTotal))">
            <li class="page-link">
                <xsl:if test="$pageOffset = 0">
                    <xsl:attribute name="class">current-page-link</xsl:attribute>
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

</xsl:stylesheet>
