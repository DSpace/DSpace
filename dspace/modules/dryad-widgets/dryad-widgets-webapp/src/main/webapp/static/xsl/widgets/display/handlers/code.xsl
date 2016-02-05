<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
    xmlns:ddw="http://datadryad.org/api/v1/widgets/display"
    xmlns:csv="http://apache.org/cocoon/csv/1.0"
    xmlns:math="http://exslt.org/math"
    exclude-result-prefixes="xhtml ddw math csv"
    version="1.0">    
    
    <xsl:template name="code">
        <html>
            <head>
                <link rel="stylesheet" href="http://cdnjs.cloudflare.com/ajax/libs/highlight.js/8.2/styles/default.min.css"></link>
                <script src="http://cdnjs.cloudflare.com/ajax/libs/highlight.js/8.2/highlight.min.js"></script>
                <script>hljs.initHighlightingOnLoad();</script>
                <!-- https://highlightjs.org/usage/ -->
            </head>
            <body>
                <pre>
<code>
<xsl:value-of select="/xhtml:xhtml/xhtml:body"/>
</code>
</pre>
            </body>
        </html>
    </xsl:template>
    
</xsl:stylesheet>