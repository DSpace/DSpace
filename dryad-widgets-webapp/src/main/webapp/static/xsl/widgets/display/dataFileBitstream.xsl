<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    exclude-result-prefixes="xhtml"
    version="1.0">
    
    <xsl:output method="html"/>
    
    <xsl:variable name="mime-type" select="/xhtml:html/xhtml:head/xhtml:meta[@property='dc:format']"/>
    <xsl:param name="doi"/>
    
    <!-- 
        Lookup table for template to call for handling input based on mime-type
    -->
    <ddw:templates>
        <ddw:template mime-type="text/plain">text-plain</ddw:template>
    </ddw:templates>
    
    <xsl:template match="/">
        <xsl:choose>
            <xsl:when test="document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type]">
                <xsl:call-template name="unsupported"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="template-name" select="document('')/xsl:stylesheet/ddw:templates/ddw:template[@mime-type=$mime-type]"/>
                <xsl:choose>
                    <xsl:when test="$template-name = 'text-plain'"><xsl:call-template name="text-plain"/></xsl:when>
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
                <div>This content type '<xsl:value-of select="$mime-type"/>' is unsupported. Please visit the
                    <a href="http://dx.doi.org/{$doi}">Dryad site</a>
                    to view the data.
                </div>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="text-plain">
        <html>
            <head></head>
            <body>
<pre>
<xsl:value-of select="/html/body"/>
</pre>
            </body>
        </html>
    </xsl:template>
    
    <xsl:template name="code">
        <html>
            <head>
                <link rel="stylesheet" href="/path/to/styles/default.css">
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
