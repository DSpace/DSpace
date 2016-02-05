<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    version="1.0">
    
    <xsl:output method="html"/>
    
    <xsl:template match="/">
        <html>
            <head></head>
            <body>
                <xsl:apply-templates select="ex:exception-report"/>
            </body>
        </html>
        <xsl:apply-templates/>
    </xsl:template>
    
    <xsl:template match="ex:exception-report">
        <xsl:apply-templates/>
    </xsl:template>
    
</xsl:stylesheet>