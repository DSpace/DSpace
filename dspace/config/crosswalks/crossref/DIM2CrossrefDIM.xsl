<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : DIM2CrossrefDIM.xsl
    Created on : October 4, 2020, 1:26 PM
    Author     : jdamerow
    Description: This XSLT simply returns the input XML (the DIM XML). It is useful during
              the development of new XSLTs.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns="http://datacite.org/schema/kernel-2.2"
                version="1.0">

    <xsl:output method="xml" indent="yes" encoding="utf-8" />

    <!-- Don't copy everything by default! -->
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@*|node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
