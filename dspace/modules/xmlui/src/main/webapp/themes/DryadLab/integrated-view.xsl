<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet controls the view/display of the item pages (package
	and file). -->

<!-- If you use an XML editor to reformat this page make sure that the i18n:text
	elements do not break across separate lines; the text will fail to be internationalized
	if this happens and the i18n text is what will be displayed on the Web page. -->

<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/" xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan" xmlns:datetime="http://exslt.org/dates-and-times"
                xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xalan strings encoder datetime"
                version="1.0" xmlns:strings="http://exslt.org/strings"
                xmlns:confman="org.dspace.core.ConfigurationManager">



  <!-- ################################ Pull out METS metadata reference and render it ################################ -->
    <xsl:template match="dri:referenceSet[@type = 'embeddedView']" priority="2">
        <xsl:apply-templates select="*[not(name()='head')]" mode="embeddedView"/>
    </xsl:template>

    <xsl:template match="dri:reference" mode="embeddedView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment>External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
        </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="embeddedView"/>
        <xsl:apply-templates/>
    </xsl:template>

</xsl:stylesheet>
