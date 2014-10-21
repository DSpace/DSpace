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

    <!--the max thumbnail height & width from dspace.cfg, needed for item view and item list pages-->
    <xsl:variable name="thumbnail.maxheight" select="confman:getIntProperty('thumbnail.maxheight', 80)"/>
    <xsl:variable name="thumbnail.maxwidth" select="confman:getIntProperty('thumbnail.maxwidth', 80)"/>
    <!-- item details url -->
    <xsl:variable name="ds_item_view_toggle_url" select="//dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]/dri:xref/@target"/>

    <!-- render linked resources using the http:// or https:// scheme depending on dspace.baseUrl -->
    <xsl:variable name="scheme">
        <xsl:choose>
            <xsl:when test="starts-with(confman:getProperty('dspace.baseUrl'), 'https://')">
                <xsl:text>https://</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>http://</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <!-- item metadata reference -->
    <xsl:variable name='identifier_doi'
                  select='//dri:meta/dri:pageMeta/dri:metadata[@element="identifier" and @qualifier="doi"]'/>
    <xsl:variable name='identifier_handle'
                  select='//dri:meta/dri:pageMeta/dri:metadata[@element="identifier" and @qualifier="handle"]'/>

</xsl:stylesheet>
