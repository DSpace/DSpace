<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:xlink="http://www.w3.org/TR/xlink/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
        xmlns="http://www.w3.org/1999/xhtml"
        xmlns:xalan="http://xml.apache.org/xalan"
        xmlns:encoder="xalan://java.net.URLEncoder"
        xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
        xmlns:jstring="java.lang.String"
        xmlns:rights="http://cosimo.stanford.edu/sdr/metsrights/"
        xmlns:confman="org.dspace.core.ConfigurationManager"
        exclude-result-prefixes="xalan encoder i18n dri mets dim xlink xsl util jstring rights confman">

    <xsl:variable name="embedWidth" select="confman:getProperty('elsevier-sciencedirect', 'embed.display.width')"/>
    <xsl:variable name="embedHeight" select="confman:getProperty('elsevier-sciencedirect', 'embed.display.height')"/>

    <xsl:template match="dri:div[@n='ElsevierEmbed']">
        <iframe frameborder="0" scrolling="no" marginheight="0" marginwidth="0">
            <xsl:attribute name="width">
                <xsl:choose>
                    <xsl:when test="string-length($embedWidth)=0">
                        <xsl:text>700</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$embedWidth"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="height">
                <xsl:choose>
                    <xsl:when test="string-length($embedHeight)=0">
                        <xsl:text>500</xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$embedHeight"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:attribute name="src">
                <xsl:text>//api.elsevier.com/content/article/pii/</xsl:text>
                <xsl:value-of select="descendant::dri:field[@n='pii']/dri:value"/>
                <xsl:text>?httpAccept=application/pdf&amp;apiKey=</xsl:text>
                <xsl:value-of select="confman:getProperty('elsevier-sciencedirect', 'api.key')"/>
                <xsl:text>&amp;cdnRedirect=true</xsl:text>
            </xsl:attribute>
            &#160;
        </iframe>
    </xsl:template>

</xsl:stylesheet>