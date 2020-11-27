<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsm="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" doctype-system="item-submission.dtd" indent="yes"/>
  <xsl:param name="inputFormsPath"/>
  <xsl:variable name="inputForms" select="document($inputFormsPath)"/>

  <xsl:template match="/">
    <item-submission>
      <submission-map>
        <xsl:copy-of select="/item-submission/submission-map/name-map"/>
      </submission-map>
      <step-definitions>
        <xsl:call-template name="transformSteps"/>
      </step-definitions>
      <submission-definitions>
        <xsl:call-template name="transformSubmissions"/>
      </submission-definitions>
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

  <xsl:template name="transformSubmissions">
    <xsl:for-each select="$inputForms/input-forms/form-definitions/form">
      <xsl:variable name="formName" select="@name"/>
      <submission-process>
        <xsl:attribute name="name">
          <xsl:value-of select="$formName"/>
        </xsl:attribute>

        <step id="collection"/>

        <!-- The describe step pages -->
        <xsl:for-each select="page">
          <step>
            <xsl:attribute name="id">
              <xsl:value-of select="concat($formName,'page',@number)"/>
            </xsl:attribute>
          </step>
        </xsl:for-each>

        <step id="license"/>
        <step id="upload"/>
      </submission-process>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
