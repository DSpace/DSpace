<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering specific to the navigation (options)

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

    <!-- Generacion del titulo para los envios recientes en las comunidades -->
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CommunityRecentSubmissions.div.community-recent-submission']/dri:head">
      <h2 class="ds-div-head">
      	<i18n:text><xsl:value-of select="."/></i18n:text>&#160;<xsl:value-of select="/dri:document/dri:body/dri:div/dri:head"/>
      </h2>
    </xsl:template>
    
    <!-- Generacion del titulo para los envios recientes en las colecciones -->
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionRecentSubmissions.div.collection-recent-submission']/dri:head">
      <h2 class="ds-div-head">
      	<i18n:text><xsl:value-of select="."/></i18n:text>&#160;<xsl:value-of select="/dri:document/dri:body/dri:div/dri:head"/>
      </h2>
    </xsl:template>

</xsl:stylesheet>
