<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    exclude-result-prefixes="xhtml ddw"
    version="1.0">
    
    <xsl:output method="html" indent="no"/>
    
    <xsl:variable name="mime-type" select="/xhtml:xhtml/xhtml:head/xhtml:meta[@property='dc.format']"/>
    <xsl:param name="doi"/>
    <xsl:param name="object-url"/> <!-- DataOne-MN url for bitstream -->
    
    <!-- 
        Lookup table for template to call for handling input based on mime-type
    -->
    <ddw:templates>
        <ddw:template mime-type="text/plain">text-plain</ddw:template>
        <ddw:template mime-type="application/pdf">application-pdf</ddw:template>
    </ddw:templates>
    
    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="not(document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type])">
                <xsl:call-template name="unsupported"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="template-name" select="document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type]"/>
                <xsl:choose>
                    <xsl:when test="$template-name = 'text-plain'"><xsl:call-template name="text-plain"/></xsl:when>
                    <xsl:when test="$template-name = 'application-pdf'"><xsl:call-template name="application-pdf"/></xsl:when>
                    <xsl:otherwise>
                        <xsl:message terminate="yes">Misconfigured lookup or missing named template.</xsl:message>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- suppress this element from result document -->
    <xsl:template match="xhtml:head"/>
    
    <!-- -->
    <xsl:template name="unsupported">
        <html>
            <head></head>
            <body>
                <div>This content type '<xsl:value-of select="$mime-type"/>' is unsupported. 
                    Please visit the <a href="http://dx.doi.org/{$doi}">Dryad site</a>
                    to view the data or download the data from this widget.
                </div>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="text-plain">
        <html>
            <head></head>
            <body>
<pre>
<xsl:value-of select="/xhtml:xhtml/xhtml:body"/>
</pre>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="application-pdf">
        <html>
            <head>
                <script src="http://mozilla.github.io/pdf.js/build/pdf.js"></script>
                <script><![CDATA[
'use strict';]]>
var url = '<xsl:value-of select="$object-url"/>';<![CDATA[
PDFJS.getDocument(url).then(function(pdf) {
  pdf.getPage(1).then(function(page) {
    var scale = 1.5;
    var viewport = page.getViewport(scale);
    var canvas = document.getElementById('PDFcanvas');
    var context = canvas.getContext('2d');
    canvas.height = viewport.height;
    canvas.width = viewport.width;
    var renderContext = {
      canvasContext: context,
      viewport: viewport
    };
    page.render(renderContext);
  });
});
]]>
                </script>
            </head>
            <body>
                <canvas id="PDFcanvas" style="border:0; width: 100%"></canvas>
            </body>
        </html>        
    </xsl:template>
    
    <xsl:template name="code">
        <html>
            <head>
                <link rel="stylesheet" href="/path/to/styles/default.css"></link>
                <script src="/path/to/highlight.pack.js"></script>
                <script>hljs.initHighlightingOnLoad();</script>
            </head>
            <body>
<pre>
<!-- 
    ADD @class value if auto-detect doesn't work
    https://highlightjs.org/usage/    
-->
<code>
<xsl:value-of select="/html/body"/>
</code>
</pre>
            </body>
        </html>
    </xsl:template>
    
</xsl:stylesheet>
