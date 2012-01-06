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

    <xsl:template match="dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-by-dateissued']/dri:div[@id='aspect.artifactbrowser.ConfigurableBrowse.div.browse-navigation']">
            <xsl:apply-templates select="dri:p[1]"/>
            <xsl:apply-templates select="dri:p[2]" mode='dateIssued'/>
    </xsl:template>
    
    <xsl:template match='dri:p' mode='dateIssued'>
      <p class="ds-paragraph">
              <xsl:apply-templates/>
              <xsl:apply-templates select="../dri:p[3]/dri:field[@n='submit']"/>      
      </p>
    </xsl:template>

    

</xsl:stylesheet>
