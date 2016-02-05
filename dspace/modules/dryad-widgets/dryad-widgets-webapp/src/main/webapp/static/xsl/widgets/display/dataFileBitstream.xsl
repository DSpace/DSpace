<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    xmlns:csv="http://apache.org/cocoon/csv/1.0"
    xmlns:math="http://exslt.org/math"
    exclude-result-prefixes="xhtml ddw math csv"
    version="1.0">

    <xsl:include href="handlers/csv.xsl"/>
    <xsl:include href="handlers/text.xsl"/>
    <xsl:include href="handlers/image.xsl"/>
    <xsl:include href="handlers/code.xsl"/>
    <!--<xsl:include href="handlers/pdf.xsl"/>-->

    <xsl:output method="html" indent="yes"/>
    <xsl:preserve-space elements="script"/>

    <xsl:variable name="mime-type" select="/xhtml:xhtml/xhtml:head/xhtml:meta[@property='dc.format']"/>
    <xsl:variable name="extent"    select="/xhtml:xhtml/xhtml:head/xhtml:meta[@property='dc.extent']"/>
    <xsl:variable name="source"    select="/xhtml:xhtml/xhtml:head/xhtml:meta[@property='dc.source']"/>

    <xsl:param name="doi"/>
    <xsl:param name="bitstream"/>   <!-- DataOne-MN url for bitstream -->
    <xsl:param name="ddwcss"/>      <!-- url of the widget css stylesheet -->

    <!--
        Lookup table for template to call for handling input based on mime-type
    -->
    <ddw:templates>
        <!--<ddw:template mime-type="application/pdf"     >application-pdf</ddw:template>-->
        <ddw:template mime-type="application/x-python">code</ddw:template>
        <ddw:template mime-type="image/png"           >image-native</ddw:template>
        <ddw:template mime-type="image/jpeg"          >image-native</ddw:template>
        <ddw:template mime-type="text/csv"            >text-csv</ddw:template>
        <ddw:template mime-type="text/plain"          >text-plain</ddw:template>
    </ddw:templates>

    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="not(document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type])">
                <xsl:call-template name="unsupported"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="template-name" select="document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type]"/>
                <xsl:choose>
                    <!--<xsl:when test="$template-name = 'application-pdf'" ><xsl:call-template name="application-pdf"   /></xsl:when>-->
                    <xsl:when test="$template-name = 'code'"            ><xsl:call-template name="code"              /></xsl:when>
                    <xsl:when test="$template-name = 'image-native'"    ><xsl:call-template name="image-native"      /></xsl:when>
                    <xsl:when test="$template-name = 'text-csv'"        ><xsl:call-template name="text-csv"          /></xsl:when>
                    <xsl:when test="$template-name = 'text-plain'"      ><xsl:call-template name="text-plain"        /></xsl:when>
                    <xsl:otherwise>
                        <xsl:message terminate="yes">Misconfigured lookup or missing named template.</xsl:message>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- suppress this element from result document -->
    <xsl:template match="xhtml:head"/>

    <!-- template called for unsupported mime-types -->
    <xsl:template name="unsupported">
        <html>
            <head>
                <link type="text/css" rel="stylesheet" href="{$ddwcss}"></link>
            </head>
            <body>
                <div class="dryad-ddw-unhandled">
                    <p class="dryad-ddw-unhandled">
                        The content for this data is unsupported for viewing.
                        Please visit the <a href="http://dx.doi.org/{$doi}" target="_blank">Dryad site</a>
                        or download the data from this widget.
                    </p>
                </div>
            </body>
        </html>
    </xsl:template>

</xsl:stylesheet>
