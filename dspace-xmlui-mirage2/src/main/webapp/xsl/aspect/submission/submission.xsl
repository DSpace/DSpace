<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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

    <xsl:template match="dri:list[@rend='sherpaList' and not(@n='sherpaListEnd')]" priority="2">
        <div>
            <xsl:attribute name="class">
                <xsl:text>col-md-12 col-xs-12</xsl:text>
            </xsl:attribute>
            <div>
                <xsl:attribute name="class">
                    <xsl:text>col-md-2 hidden-xs</xsl:text>
                </xsl:attribute>
                <xsl:apply-templates select="dri:item/dri:figure"/>
            </div>
            <div>
                <xsl:attribute name="class">
                    <xsl:text>col-md-10 col-xs-12</xsl:text>
                </xsl:attribute>
                <!--<xsl:apply-templates select="dri:item[not(./dri:figure)]/*"/>-->

                <xsl:for-each select="dri:item">
                    <xsl:if test="not(./dri:figure)">
                        <xsl:if test="count(./*) = 0">
                            <xsl:value-of select="."/>
                            <br/>

                        </xsl:if>
                        <xsl:if test="count(./*) &gt; 0">
                            <xsl:choose>
                                <xsl:when test="./dri:xref[@rend='sherpaMoreInfo']">
                                    <small>
                                        <xsl:apply-templates select="./*"/>
                                    </small>
                                </xsl:when>
                                <xsl:when test="./dri:xref">
                                    <xsl:apply-templates select="./*"/>
                                    <br/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:apply-templates select="./*"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                    </xsl:if>
                </xsl:for-each>
                <br/>
            </div>
        </div>
    </xsl:template>

    <xsl:template match="dri:list[@n='sherpaListEnd']" priority="2">
        <div>
            <xsl:attribute name="class">
                <xsl:text>col-md-12</xsl:text>
            </xsl:attribute>
            <br/>
            <i18n:text>
                <xsl:value-of select="dri:item"/></i18n:text>
        </div>
    </xsl:template>

    <xsl:template match="dri:list[@rend='sherpaList']/dri:item/dri:hi[@rend='sherpaBold']" priority="2" >
        <strong>
            <xsl:call-template name="standardAttributes">
            </xsl:call-template>
            <xsl:apply-templates/>
        </strong>
    </xsl:template>

    <xsl:template match="dri:list[@rend='sherpaList']/dri:item/dri:hi[contains(@rend,'sherpaStyle')]" priority="2" >
        <span>
            <xsl:attribute name="class">

                <xsl:if test="contains(@rend,'white')">
                    <xsl:text>label label-white</xsl:text>
                </xsl:if>
                <xsl:if test="contains(@rend,'blue')">
                    <xsl:text>label label-info</xsl:text>
                </xsl:if>
                <xsl:if test="contains(@rend,'yellow')">
                    <xsl:text>label label-warning</xsl:text>
                </xsl:if>
                <xsl:if test="contains(@rend,'green')">
                    <xsl:text>label label-success</xsl:text>
                </xsl:if>
                <xsl:if test="contains(@rend,'gray')">
                    <xsl:text>label label-default</xsl:text>
                </xsl:if>

            </xsl:attribute>

            <i18n:text>
                <xsl:value-of select="."/>
            </i18n:text>
        </span>
        <span><xsl:text> </xsl:text></span>
    </xsl:template>

    <xsl:template match="dri:list[@rend='sherpaList']/dri:item/dri:figure" priority="2" >
        <a>
            <xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
            <xsl:if test="@title">
                <xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute>
            </xsl:if>
            <xsl:if test="@rend">
                <xsl:attribute name="class">
                    <xsl:value-of select="@rend"/>
                </xsl:attribute>
            </xsl:if>

            <img>
                <xsl:attribute name="src"><xsl:value-of select="@source"/></xsl:attribute>
                <xsl:attribute name="alt"><xsl:apply-templates /></xsl:attribute>
                <xsl:attribute name="border"><xsl:text>none</xsl:text></xsl:attribute>
            </img>
        </a>
    </xsl:template>

    <xsl:template match="dri:list[@rend='sherpaList']/dri:item/dri:data" priority="2" >
        <xsl:call-template name="standardAttributes">
            <!--<xsl:with-param name="class">pull-left</xsl:with-param>-->
        </xsl:call-template>
        <xsl:apply-templates/>
    </xsl:template>

</xsl:stylesheet>
