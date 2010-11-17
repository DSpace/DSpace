<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    This stylesheet contains helper templates for things like i18n and standard attributes.

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

    <!-- The standard attributes template -->
    <!-- TODO: should probably be moved up some, since it is commonly called -->
    <xsl:template name="standardAttributes">
        <xsl:param name="class"/>
        <xsl:if test="@id">
            <xsl:attribute name="id"><xsl:value-of select="translate(@id,'.','_')"/></xsl:attribute>
        </xsl:if>
        <xsl:attribute name="class">
            <xsl:value-of select="normalize-space($class)"/>
            <xsl:if test="@rend">
                <xsl:text> </xsl:text>
                <xsl:value-of select="@rend"/>
            </xsl:if>
        </xsl:attribute>

    </xsl:template>

    <!-- templates for required textarea attributes used if not found in DRI document -->
    <xsl:template name="textAreaCols">
      <xsl:attribute name="cols">20</xsl:attribute>
    </xsl:template>

    <xsl:template name="textAreaRows">
      <xsl:attribute name="rows">5</xsl:attribute>
    </xsl:template>



    <!-- This does it for all the DRI elements. The only thing left to do is to handle Cocoon's i18n
        transformer tags that are used for text translation. The templates below simply push through
        the i18n elements so that they can translated after the XSL step. -->
    <xsl:template match="i18n:text">
       <xsl:param name="text" select="."/>
       <xsl:choose>
         <xsl:when test="contains($text, '&#xa;')">
           <xsl:value-of select="substring-before($text, '&#xa;')"/>
           <ul>
                <xsl:attribute name="style">float:left; list-style-type:none; text-align:left;</xsl:attribute>
                <xsl:call-template name="linebreak">
                  <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
                </xsl:call-template>
           </ul>
         </xsl:when>
         <xsl:otherwise>
           <xsl:copy-of select="$text"/>
         </xsl:otherwise>
       </xsl:choose>
    </xsl:template>

    <!-- Function to replace \n -->
    <xsl:template name="linebreak">
       <xsl:param name="text" select="."/>
       <xsl:choose>
         <xsl:when test="contains($text, '&#xa;')">
           <li>
           <xsl:value-of select="substring-before($text, '&#xa;')"/>
           </li>
           <xsl:call-template name="linebreak">
             <xsl:with-param name="text" select="substring-after($text,'&#xa;')"/>
           </xsl:call-template>
         </xsl:when>
         <xsl:otherwise>
           <xsl:value-of select="$text"/>
         </xsl:otherwise>
       </xsl:choose>
    </xsl:template>

    <xsl:template match="i18n:translate">
        <xsl:copy-of select="."/>
    </xsl:template>

    <xsl:template match="i18n:param">
        <xsl:copy-of select="."/>
    </xsl:template>

</xsl:stylesheet>
