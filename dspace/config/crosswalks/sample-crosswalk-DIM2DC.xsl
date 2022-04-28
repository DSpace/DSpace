<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dspace="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dcterms="http://purl.org/dc/terms/"
                version="1.0">
<!--
        Incomplete proof-of-concept Example of
        XSLT crosswalk from DIM (DSpace Intermediate Metadata) to
        Qualified Dublin Core.
         by William Reilly, aug. 05; mutilated by Larry Stone.

        This is only fit for a simple smoke test of the XSLT-based
        crosswalk plugin, do not use it for anthing more serious.
 -->

        <xsl:template match="@* | node()">
        <!--  XXX don't copy everything by default.
                <xsl:copy>
                        <xsl:apply-templates select="@* | node()"/>
                </xsl:copy>
         -->
        </xsl:template>

        <!-- http://wiki.dspace.org/DspaceIntermediateMetadata -->
        
                <xsl:template match="dspace:dim">
                <!-- http://dublincore.org/schemas/xmls/qdc/2003/04/02/qualifieddc.xsd -->
                <xsl:element name="dcterms:qualifieddc">
                        <xsl:apply-templates/>
                </xsl:element>
        </xsl:template>
        
        <xsl:template match="dspace:field[@element ='title']">
                <!--  http://dublincore.org/schemas/xmls/qdc/2003/04/02/dcterms.xsd  -->
                <xsl:element name="dc:title">
                        <xsl:value-of select="text()"/>
                </xsl:element>
        </xsl:template>
        
        <xsl:template match="dspace:field[@element ='contributor' and @qualifier='author']">
                <xsl:element name="dc:author">
                        <xsl:value-of select="text()"/>
                </xsl:element>
        </xsl:template>
        
        <xsl:template match="dspace:field[@element ='contributor' and @qualifier='illustrator']">
                <xsl:element name="dc:author">
                        <xsl:value-of select="text()"/>
                </xsl:element>
        </xsl:template>
</xsl:stylesheet>
