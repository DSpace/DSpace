<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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

    <xsl:import href="../dri2xhtml-alt/dri2xhtml.xsl"/>
    <xsl:output indent="yes"/>
    <xsl:template match="dri:document">
        <xsl:apply-templates select="//dri:body"/>
    </xsl:template>
    <xsl:template match="dri:body">
        <script type="text/javascript">
            <xsl:text disable-output-escaping="yes">var JsHost = (("https:" == document.location.protocol) ? "https://" : "http://");
            document.write(unescape("%3Cscript src='" + JsHost + "ajax.googleapis.com/ajax/libs/jquery/1.9.1/jquery.min.js' type='text/javascript'%3E%3C/script%3E"));</xsl:text>
        </script>
        <script type="text/javascript">
            <xsl:attribute name="src">
                <xsl:value-of
                        select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                <xsl:text>/themes/Paypal/lib/js/paypal.js</xsl:text>
            </xsl:attribute>
            &#160;
        </script>
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="dri:options"/>
    <xsl:template match="dri:meta"/>
</xsl:stylesheet>
