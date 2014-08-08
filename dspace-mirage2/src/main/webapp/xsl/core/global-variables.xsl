<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Global variables accessible from other templates

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
    xmlns:confman="org.dspace.core.ConfigurationManager"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">

    <xsl:output indent="yes"/>

    <xsl:variable name="document" select="/dri:document"/>
    <xsl:variable name="pagemeta" select="/dri:document/dri:meta/dri:pageMeta"/>
    <xsl:variable name="context-path" select="$pagemeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>

    <xsl:variable name="theme-path" select="concat($context-path,'/themes/',$pagemeta/dri:metadata[@element='theme'][@qualifier='path'])"/>

    <xsl:variable name="isModal" select="dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='framing'][@qualifier='modal']/text()='true'"/>

    <!--the max thumbnail height & width from dspace.cfg, needed for item view and item list pages-->
    <xsl:variable name="thumbnail.maxheight" select="confman:getIntProperty('thumbnail.maxheight', 80)"/>
    <xsl:variable name="thumbnail.maxwidth" select="confman:getIntProperty('thumbnail.maxwidth', 80)"/>
    <!-- item details url -->
    <xsl:variable name="ds_item_view_toggle_url" select="//dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]/dri:xref/@target"/>

    <!--
        Full URI of the current page. Composed of scheme, server name and port and request URI.
    -->
    <xsl:variable name="current-uri">
        <xsl:value-of select="$pagemeta/dri:metadata[@element='request'][@qualifier='scheme']"/>
        <xsl:text>://</xsl:text>
        <xsl:value-of select="$pagemeta/dri:metadata[@element='request'][@qualifier='serverName']"/>
        <xsl:text>:</xsl:text>
        <xsl:value-of select="$pagemeta/dri:metadata[@element='request'][@qualifier='serverPort']"/>
        <xsl:value-of select="$pagemeta/dri:metadata[@element='contextPath']"/>
        <xsl:text>/</xsl:text>
        <xsl:value-of select="$pagemeta/dri:metadata[@element='request'][@qualifier='URI']"/>
    </xsl:variable>

    <xsl:variable name="SFXLink">
        <xsl:if test="$pagemeta/dri:metadata[@element='sfx'][@qualifier='server']">
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="$pagemeta/dri:metadata[@element='sfx'][@qualifier='server']"/>
                </xsl:attribute>
                <xsl:choose>
                    <xsl:when test="$pagemeta/dri:metadata[@element='sfx'][@qualifier='image_url']">
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="$pagemeta/dri:metadata[@element='sfx'][@qualifier='image_url']"/>
                            </xsl:attribute>
                            <xsl:attribute name="alt">
                                <xsl:text>Find Full text</xsl:text>
                            </xsl:attribute>
                        </img>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text>Find Full text</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </a>
        </xsl:if>
    </xsl:variable>


</xsl:stylesheet>
