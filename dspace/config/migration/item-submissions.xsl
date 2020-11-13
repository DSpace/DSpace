<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsm="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" doctype-system="submission-forms.dtd" indent="yes"/>

  <xsl:template match="/">
    <item-submission>
      <submission-map>
        <xsl:copy-of select="/item-submission/submission-map/name-map"/>
      </submission-map>
      <step-definitions>
        <xsl:call-template name="transformSteps"/>
      </step-definitions>
      <submission-definitions></submission-definitions>
    </item-submission>
  </xsl:template>

  <xsl:template name="transformSteps">
    <xsl:for-each select="/item-submission/step-definitions/step">
      <step>
        <xsl:attribute name="id">
          <xsl:value-of select="@id"/>
        </xsl:attribute>
        <heading>
          <xsm:value-of select="./heading"/>
        </heading>
        <processing-class>
          <xsm:value-of select="./processing-class"/>
        </processing-class>
        <type>
          <xsl:choose>
            <xsl:when test="@id='collection' or @id='upload' or @id='licence' or @id='sample'">
              <xsl:value-of select="@id"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>submission-form</xsl:text>
            </xsl:otherwise>
          </xsl:choose>
        </type>
        <xsl:choose>
          <xsl:when test="@id='collection'">
            <scope visibility="hidden" visibilityOutside="hidden">submission</scope>
          </xsl:when>
          <xsl:when test="@id='license'">
            <scope visibilityOutside="read-only">submission</scope>
          </xsl:when>
        </xsl:choose>
      </step>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
