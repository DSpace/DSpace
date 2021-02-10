<?xml version="1.0"?>
<!-- This XSLT is used by `./dspace submission-forms-migrate` to transform a DSpace 6.x (or below) item-submission.xml
configuration file into a DSpace 7.x (or above) item-submission.xml -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xsm="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" doctype-system="item-submission.dtd" indent="yes"/>
  <xsl:param name="inputFormsPath"/>
  <xsl:variable name="inputForms" select="document($inputFormsPath)"/>

  <xsl:template match="/">
    <item-submission>
      <submission-map>
        <xsl:for-each select="$inputForms/input-forms/form-map/name-map">
          <name-map>
            <xsl:attribute name="collection-handle">
              <xsl:value-of select="@collection-handle"/>
            </xsl:attribute>
            <xsl:attribute name="submission-name">
              <xsl:value-of select="@form-name"/>
            </xsl:attribute>
          </name-map>
        </xsl:for-each>
      </submission-map>
      <step-definitions>
        <xsl:call-template name="transformSteps"/>
        <xsl:call-template name="addFormPageSteps"/>
        <xsl:call-template name="addStaticSteps"/>
      </step-definitions>
      <submission-definitions>
        <xsl:call-template name="transformInputFormsToSubmissions"/>
      </submission-definitions>
    </item-submission>
  </xsl:template>

  <xsl:template name="transformSteps">
    <xsl:for-each select="/item-submission/step-definitions/step">
      <step-definition>
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
      </step-definition>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="addFormPageSteps">
    <xsl:for-each select="$inputForms/input-forms/form-definitions/form/page">
      <xsl:variable name="formName" select="../@name"/>
      <step-definition mandatory="true">
        <xsl:attribute name="id">
          <xsl:value-of select="concat($formName,'page',@number)"/>
        </xsl:attribute>
        <heading>
          <xsm:value-of select="concat('submit.progressbar.describe.step',@number)"/>
        </heading>
        <processing-class>org.dspace.app.rest.submit.step.DescribeStep</processing-class>
        <type>submission-form</type>
      </step-definition>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="addStaticSteps">
    <step-definition id="upload">
      <heading>submit.progressbar.upload</heading>
      <processing-class>org.dspace.app.rest.submit.step.UploadStep</processing-class>
      <type>upload</type>
    </step-definition>
    <step-definition id="license">
      <heading>submit.progressbar.license</heading>
      <processing-class>org.dspace.app.rest.submit.step.LicenseStep</processing-class>
      <type>license</type>
      <scope visibilityOutside="read-only">submission</scope>
    </step-definition>
  </xsl:template>

  <xsl:template name="transformInputFormsToSubmissions">
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

        <step id="upload"/>
        <step id="license"/>
      </submission-process>
    </xsl:for-each>
  </xsl:template>
</xsl:stylesheet>
