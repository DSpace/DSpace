<?xml version="1.0" encoding="UTF-8"?>
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
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    
    <xsl:import href="../dri2xhtml.xsl"/>
    <xsl:output indent="yes"/>
    
         
    <!-- 
        This xsl is provided only as an example of over ridding selected templates. 
        Uncomment the following to add ">" to the end of all trail links, currently 
        only browsers which support the pseudo element of ":after" are able to 
        render the ">" trail postfix.
        
        Remember to remove the pseudo element from the CSS when uncommenting!
    -->
         
    <!--
    <xsl:template match="dri:trail">
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-trail-link </xsl:text>
                <xsl:if test="position()=1">
                    <xsl:text>first-link</xsl:text>
                </xsl:if>
                <xsl:if test="position()=last()">
                    <xsl:text>last-link</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:choose>
                <xsl:when test="./@target">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="./@target"/>
                        </xsl:attribute>
                        <xsl:apply-templates />
                    </a>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates />
                </xsl:otherwise>
            </xsl:choose>
            &gt;
        </li>
    </xsl:template>
    -->
         
         
            
</xsl:stylesheet>
