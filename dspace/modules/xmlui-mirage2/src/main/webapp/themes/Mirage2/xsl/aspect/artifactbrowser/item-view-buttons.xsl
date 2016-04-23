<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Rendering specific to the item display page.

    Author: Adan Roman
    Author Sergio Nieto

-->

<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:oreatom="http://www.openarchives.org/ore/atom/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
    xmlns:jstring="java.lang.String"
    xmlns:rights="http://cosimo.stanford.edu/sdr/metsrights/"
    xmlns:confman="org.dspace.core.ConfigurationManager"
    exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util jstring rights">

    <xsl:output indent="yes"/>


	
		<xsl:template name="itemSummaryView-DIM-juicio-revision">
		<xsl:param name="link" select="//@OBJID" />
			<div id="juicio_buttons">
				<a>
					<xsl:attribute name="href">
						<xsl:value-of
							select="concat(substring-before($link,'handle'),'juzgarRequest',substring-after($link,'handle'))" />
					</xsl:attribute>
					<i18n:text>xmlui.openaire.juicio.juzgar</i18n:text>
				</a>
			</div>
	</xsl:template>
	
	<xsl:template name="itemSummaryView-DIM-revision-request">
		<xsl:param name="link" select="//@OBJID" />
			<div id="revision_buttons">
				<a>
					<xsl:attribute name="href">
						<xsl:value-of
							select="concat(substring-before($link,'handle'),'revisionRequest',substring-after($link,'handle'))" />
					</xsl:attribute>
					<i18n:text>xmlui.openaire.revision.solicitar</i18n:text>
				</a>
			</div>
	</xsl:template>
	
</xsl:stylesheet>
