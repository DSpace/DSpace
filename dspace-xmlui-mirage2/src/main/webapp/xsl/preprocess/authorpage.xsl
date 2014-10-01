<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet
        xmlns="http://di.tamu.edu/DRI/1.0/"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        exclude-result-prefixes="xsl dri i18n">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:body[dri:div[@n='author-profile-wrapper']]">
        <body>
            <!--<div rend="row discovery-facets vertical-slider invisible-xs hidden-sm" n="discovery-facet-wrapper">-->
            <!--<xsl:call-template name="render-scope-as-list">-->
                <!--<xsl:with-param name="scope" select="//dri:field[@id='aspect.discovery.SimpleSearch.field.raw-scope']"/>-->
            <!--</xsl:call-template>-->
            <!--<xsl:apply-templates-->
                    <!--select="/dri:document/dri:options/dri:list[@n='discovery']/dri:list" mode="facet-wrapper"/>-->
        <!--</div>-->
            <div rend="author-page-wrapper">
            <div rend="deceive-discovery"><xsl:text>deceive-discovery</xsl:text></div>

            <xsl:apply-templates select="dri:div[@n='author-profile-wrapper']"/>
            <xsl:apply-templates select="*[not(self::dri:div[@n='author-profile-wrapper'])]"/>
        </div>
        </body>


    </xsl:template>

    <xsl:template match="dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'][text()='author-page']">
        <metadata>
            <xsl:call-template name="copy-attributes"/>
            <xsl:apply-templates/>
        </metadata>
        <metadata element="deceive-discovery">
            <xsl:text>true</xsl:text>
        </metadata>
    </xsl:template>

    <xsl:template match="dri:div[@rend='controls-gear-wrapper' and @n='search-controls-gear']">
        <xsl:if test="//dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'][text()='author-page']">
            <div n='renderRefine'>true</div>
        </xsl:if>
        <div>
            <xsl:call-template name="copy-attributes"/>
            <xsl:apply-templates/>
        </div>
    </xsl:template>

    <xsl:template match="dri:div[@id='aspect.authorprofile.AuthorPage.div.author-profile']">
        <div>
            <xsl:call-template name="copy-attributes"/>
            <xsl:apply-templates/>
        </div>
        <!--<div rend="row discovery-facets vertical-slider invisible-sm" n="discovery-facet-wrapper">-->
            <!--<xsl:call-template name="render-scope-as-list">-->
                <!--<xsl:with-param name="scope" select="//dri:field[@id='aspect.discovery.SimpleSearch.field.raw-scope']"/>-->
            <!--</xsl:call-template>-->
            <!--<xsl:apply-templates-->
                    <!--select="/dri:document/dri:options/dri:list[@n='discovery']/dri:list" mode="second-facet-wrapper"/>-->
        <!--</div>-->
    </xsl:template>

</xsl:stylesheet>
