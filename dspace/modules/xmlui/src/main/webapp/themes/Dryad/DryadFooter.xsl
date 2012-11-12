<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml" xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:mods="http://www.loc.gov/mods/v3" xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xalan strings encoder"
    version="1.0" xmlns:strings="http://exslt.org/strings">

	<xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes"/>

    <xsl:variable name="dryadrelease" select="document('meta/version.xml')"/>

    <xsl:template name="buildFooter">
        <div id="ds-footer">
            <div id="ds-footer-links">
                <p>
                    <i18n:text>xmlui.dri2xhtml.structural.feedback-text</i18n:text>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/feedback</xsl:text>
                        </xsl:attribute>
                        <i18n:text>xmlui.dri2xhtml.structural.feedback-form</i18n:text>.
                    </a>
                </p>
            </div>
            <div id="ds-footer-credit">
                <p style="font-size: smaller;">
                    <i18n:text>xmlui.dri2xhtml.structural.footer-promotional1</i18n:text>
                </p>
                <p style="font-size: smaller; text-align:right; padding-top: 15px;">
                    <i18n:text>xmlui.dri2xhtml.structural.footer-promotional2</i18n:text>
                    <xsl:value-of select="$dryadrelease/release/date"/> 
                    <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dryad'][@qualifier='node']">
                    	<i18n:text>xmlui.dri2xhtml.structureal.footer-node</i18n:text>
                    	<xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dryad'][@qualifier='node']"/>
                    </xsl:if>
                    <!--Commit hash in comment-->
                    <xsl:comment>Commit Hash: 
                        <xsl:value-of select="$dryadrelease/release/version"/>
                    </xsl:comment>

                    <!--Invisible link to HTML sitemap (for search engines) -->
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                            <xsl:text>/htmlmap</xsl:text>
                        </xsl:attribute>
                    </a>
                </p>
            </div>
        </div>
    </xsl:template>

</xsl:stylesheet>
