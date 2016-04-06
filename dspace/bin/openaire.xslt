<xsl:stylesheet version="1.0"
          xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
          xmlns:str="http://exslt.org/strings"
          exclude-result-prefixes="str"
>
<xsl:output method="xml" encoding="UTF-8" />
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <!--
    the syntax is:
        info:eu-repo/grantAgreement/Funder/FundingProgram/ProjectID/
        [Jurisdiction]/[ProjectName]/[ProjectAcronym]
    here we strip all the non mandatory parts in order to achieve
    at least some level of consistency.
  -->
  <xsl:template match="stored-value">
    <stored-value>
      <xsl:for-each select="str:tokenize(string(.), '/')">
        <xsl:choose>
          <xsl:when test="position() &lt; 5 and not(position()=last())">
            <xsl:value-of select="concat(.,'/')"/>
          </xsl:when>
            <!-- here we have either ProjectID (if pos=5) or last token
            on position =< 5,
            eg. with WT we are getting only .../WT/123456 -->
          <xsl:when test="position() &lt; 6">
            <xsl:value-of select="."/>
          </xsl:when>
        </xsl:choose>
      </xsl:for-each>
    </stored-value>
  </xsl:template>
</xsl:stylesheet>
