
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:confman="org.dspace.core.ConfigurationManager"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc confman">	

    <xsl:output indent="yes"/>
    
        
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.FeedbackForm.div.feedback-form']/dri:p[1]">
        <p class="ds-paragraph">
           <i18n:text><xsl:value-of select="."/></i18n:text>
        </p>
        <p class="ds-paragraph">
           <i18n:text>xmlui.ArtifactBrowser.FeedbackForm.para2</i18n:text>
           <a>
            <xsl:attribute name="href">mailto: info@sedici.unlp.edu.ar</xsl:attribute>
            info@sedici.unlp.edu.ar
           </a>
        </p>
    </xsl:template>
   
</xsl:stylesheet>