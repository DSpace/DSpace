<?xml version="1.0" encoding="UTF-8" ?>
<!-- https://datacite-metadata-schema.readthedocs.io/ -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	        version="2.0">
    <xsl:import href="dim.xsl" />
    <xsl:import href="../../DIM2DataCite.xsl" />
    <xsl:output method="xml" indent="yes" />
    <xsl:template match="/">
        <xsl:call-template name="datacite-root">
            <xsl:with-param name="dim-root">
                <xsl:call-template name="dim-root">
                    <xsl:with-param name="xoai-root" select="." />
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
</xsl:stylesheet>
