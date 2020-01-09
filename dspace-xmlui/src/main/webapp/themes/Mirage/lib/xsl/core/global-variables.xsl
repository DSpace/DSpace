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
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="3.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <xsl:variable name="pagemeta" select="/dri:document/dri:meta/dri:pageMeta"/>

    <!--the max thumbnail height & width from dspace.cfg, needed for item view and item list pages-->
    <xsl:variable name="thumbnail.maxheight" select="$pagemeta/dri:metadata[@element='thumbnail'][@qualifier='maxheight']"/>
    <xsl:variable name="thumbnail.maxwidth" select="$pagemeta/dri:metadata[@element='thumbnail'][@qualifier='maxwidth']"/>

    <!-- item details url -->
    <xsl:variable name="ds_item_view_toggle_url" select="//dri:p[contains(@rend , 'item-view-toggle') and
        (preceding-sibling::dri:referenceSet[@type = 'summaryView'] or following-sibling::dri:referenceSet[@type = 'summaryView'])]/dri:xref/@target"/>

    <!-- render linked resources using the http:// or https:// scheme depending on dspace.baseUrl -->
    <xsl:variable name="scheme" select="$pagemeta/dri:metadata[@element='scheme'][not(@qualifier)]"/>

    <!-- item metadata reference -->
    <xsl:variable name='identifier_doi'
                  select='$pagemeta/dri:metadata[@element="identifier" and @qualifier="doi"]'/>
    <xsl:variable name='identifier_handle'
                  select='$pagemeta/dri:metadata[@element="identifier" and @qualifier="handle"]'/>

    <!-- altmetric -->

    <xsl:variable name='altmetric.enabled'
                  select='$pagemeta/dri:metadata[@element="altmetric" and @qualifier="enabled"]'/>

    <!--<xsl:if test="not(contains($altmetric.enabled), 'false')">-->
        <xsl:variable name='altmetric.badgeType'
                      select='$pagemeta/dri:metadata[@element="altmetric" and @qualifier="badgeType"]'/>
        <xsl:variable name='altmetric.popover'
                      select='$pagemeta/dri:metadata[@element="altmetric" and @qualifier="popover"]'/>
        <xsl:variable name='altmetric.details'
                      select='$pagemeta/dri:metadata[@element="altmetric" and @qualifier="details"]'/>
        <xsl:variable name='altmetric.noScore'
                      select='$pagemeta/dri:metadata[@element="altmetric" and @qualifier="noScore"]'/>
        <xsl:variable name='altmetric.hideNoMentions'
                      select='$pagemeta/dri:metadata[@element="altmetric" and @qualifier="hideNoMentions"]'/>
        <xsl:variable name='altmetric.linkTarget'
                      select='$pagemeta/dri:metadata[@element="altmetric" and @qualifier="linkTarget"]'/>

    <!--</xsl:if>-->

    <!-- plumx -->

    <xsl:variable name='plumx.enabled'
                  select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="enabled"]'/>

    <!--<xsl:if test="not(contains($plumx.enabled), 'false')">-->
        <xsl:variable name='plumx.widget-type'
                      select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="widget-type"]'/>
        <xsl:variable name='plumx.data-popup'
                      select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="data-popup"]'/>
        <xsl:variable name='plumx.data-hide-when-empty'
                      select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="data-hide-when-empty"]'/>
        <xsl:variable name='plumx.data-hide-print'
                      select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="data-hide-print"]'/>
        <xsl:variable name='plumx.data-orientation'
                      select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="data-orientation"]'/>
        <xsl:variable name='plumx.data-width'
                      select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="data-width"]'/>
        <xsl:variable name='plumx.data-border'
                      select='$pagemeta/dri:metadata[@element="plumx" and @qualifier="data-border"]'/>

    <!--</xsl:if>-->

</xsl:stylesheet>
