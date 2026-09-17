<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <!--
        Recursive function searching for a DDC in a string.
        Example string 1: '153 Kognitive Prozesse, Intelligenz'
        Example string 2: 'DDC::300 Sozialwissenschaften::330 Wirtschaft::336 Öffentliche Finanzen'
        In the last case, we want the last DDC.
    -->
    <xsl:template name="find-ddc-recursively">
        <xsl:param name="text"/>
        <xsl:choose>
            <xsl:when test="contains($text, '::')">
                <xsl:call-template name="find-ddc-recursively">
                    <xsl:with-param name="text" select="substring-after($text, '::')"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="number(substring($text,1,3)) + 1"><!-- The +1 is a trick to make it accept the DDC 000 -->
                    <xsl:value-of select="substring($text,1,3)"/>
                </xsl:if>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
