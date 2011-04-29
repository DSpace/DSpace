<?xml version='1.0' encoding='UTF-8'?>
<!--
Rudimentary Case - By - Case DocBook to Confluence transformation.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                  version="2.0"
                xmlns:str="http://exslt.org/strings">


    <xsl:output indent="no" omit-xml-declaration="yes" method="text" encoding='UTF8'/>


    <xsl:strip-space elements="*"/>

    <xsl:template match="/">
         <xsl:apply-templates select="*"/>
    </xsl:template>

    <xsl:template match="chapter">
        <xsl:text>&#x0D;</xsl:text>
        <xsl:value-of select="concat(@remap,'. ')"/>
        <xsl:apply-templates select="title"/>
        <xsl:text>&#x0D;</xsl:text>
         <xsl:apply-templates select="*[name(.) != 'title']|text()"/>
        <xsl:text>&#x0D;</xsl:text>
    </xsl:template>

    <xsl:template match="section">
        <xsl:text>&#x0D;</xsl:text>
        <xsl:value-of select="concat(@remap,'. ')"/>
        <xsl:apply-templates select="title"/>
        <xsl:text>&#x0D;</xsl:text>
        <xsl:apply-templates select="*[name(.) != 'title']|text()"/>
        <xsl:text>&#x0D;</xsl:text>
    </xsl:template>

    <xsl:template match="informaltable|table">

        <xsl:for-each select="tgroup/thead/row">
            <xsl:for-each select="entry">
                <xsl:text>||</xsl:text>
                <xsl:apply-templates select="*|text()"/>
                <xsl:if test="not(*)">
                    <xsl:text>&#160;</xsl:text>
                </xsl:if>
                <xsl:if test="position() = last()">
                    <xsl:text>||</xsl:text>
                    <xsl:text>&#x0D;</xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:for-each>
        <xsl:for-each select="tgroup/tbody/row">
            <xsl:for-each select="entry">
                <xsl:text>|</xsl:text>
                <xsl:apply-templates select="*|text()"/>
                <xsl:if test="not(*)">
                    <xsl:text>&#160;</xsl:text>
                </xsl:if>
                <xsl:if test="position() = last()">
                    <xsl:text>|</xsl:text>
                    <xsl:text>&#x0D;</xsl:text>
                </xsl:if>
            </xsl:for-each>
        </xsl:for-each>

    </xsl:template>

    <xsl:template match="varlistentry">
        <xsl:text>&#x0D;</xsl:text>
        <xsl:call-template name="renderList"/>
        <xsl:text>*</xsl:text>
        <xsl:apply-templates select="term"/>
        <xsl:text>*: </xsl:text>
        <xsl:for-each select="listitem">
            <xsl:apply-templates select="*"/>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="listitem">
        <xsl:text>&#x0D;</xsl:text>
       <xsl:call-template name="renderList"/>
       <xsl:apply-templates select="*"/>
    </xsl:template>


    <xsl:template match="screen">
        <xsl:text>{code}</xsl:text>
        <xsl:value-of select="."/>
        <xsl:text>{code}</xsl:text>
    </xsl:template>

    <xsl:template match="para">
          <xsl:text>&#x0D;</xsl:text>
        <xsl:apply-templates select="*|text()"/>
          <xsl:text>&#x0D;</xsl:text>
    </xsl:template>


    <xsl:template match="para[ancestor::listitem]|para[ancestor::tbody]">
        <xsl:apply-templates select="*|text()"/>
        <xsl:if test="not(*)">
            <xsl:text>&#160;</xsl:text>
        </xsl:if>
    </xsl:template>


    <xsl:template match="inlinemediaobject/imageobject/imagedata">
        <xsl:value-of select="concat('!',substring-after(@fileref,'image/'),'!')"/>
    </xsl:template>
                                                         <!--
    <inlinemediaobject>
          <imageobject>
            <imagedata fileref="image/architecture-600x450.gif" format="GIF" width="6.5in" scalefit="1"/>
          </imageobject>
        </inlinemediaobject>
                                                             -->
    <xsl:template match="literal">
        <xsl:text>_</xsl:text>
        <xsl:apply-templates select="*|text()"/>
        <xsl:text>_</xsl:text>
    </xsl:template>
    
    <xsl:template match="ulink">
        <xsl:value-of select="concat('[',.,'|',@url,'|',.,']')"/>
    </xsl:template>

    <xsl:template match="emphasis">
        <xsl:text>_</xsl:text><xsl:apply-templates select="*|text()"/><xsl:text>_</xsl:text>
    </xsl:template>

    <xsl:template match="emphasis[@role='bold']">
        <xsl:text>*</xsl:text><xsl:apply-templates select="*|text()"/><xsl:text>*</xsl:text>
    </xsl:template>

    <xsl:template match="text()">
        <xsl:variable name="filtered">
            <xsl:value-of select="replace( replace(.,'\[','\\[') ,'\]','\\]')"/>
        </xsl:variable>
        <xsl:value-of select="replace( replace($filtered,'\{','\\{') ,'\}','\\}')"/>
    </xsl:template>

    <xsl:template match="*">
        <xsl:apply-templates select="*|text()"/>
    </xsl:template>

    <xsl:template name="renderList">
        <xsl:for-each select="ancestor-or-self::itemizedlist|ancestor-or-self::orderedlist|ancestor-or-self::variablelist">
            <xsl:choose>
                <xsl:when test="name(.) = 'orderedlist'"><xsl:text>#</xsl:text></xsl:when>
                <xsl:otherwise><xsl:text>*</xsl:text></xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
        <xsl:text> </xsl:text>
    </xsl:template>
</xsl:stylesheet>