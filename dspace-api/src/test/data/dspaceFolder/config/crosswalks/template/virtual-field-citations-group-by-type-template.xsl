<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format"
    exclude-result-prefixes="fo">
    
    <xsl:template match="person">   
        <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
        
            <fo:layout-master-set>
                <fo:simple-page-master master-name="simpleA4"
                    page-height="29.7cm" page-width="21cm" margin-top="1cm"
                    margin-bottom="1cm" margin-left="1cm" margin-right="1cm">
                    <fo:region-body />
                </fo:simple-page-master>
            </fo:layout-master-set>
            
            <fo:page-sequence master-reference="simpleA4">
                
                <fo:flow flow-name="xsl-region-body">
                    
                    <xsl:call-template name="section-title">
                        <xsl:with-param name="label" select="'Publications'" />
                    </xsl:call-template>
                    
                    <xsl:for-each select="publications/*">
                        <fo:block font-size="10pt" margin-bottom="4mm">
                            <xsl:copy-of select="."/>
                        </fo:block>
                    </xsl:for-each>

                </fo:flow>
                
            </fo:page-sequence>
        </fo:root>
    </xsl:template>
    
    <xsl:template name = "section-title" >
        <xsl:param name = "label" />
        <fo:block font-size="16pt" font-weight="bold" margin-top="7mm" margin-bottom="5mm" >
            <fo:inline color="#00802f"> <xsl:value-of select="$label" /> </fo:inline>
        </fo:block>
    </xsl:template>
    
</xsl:stylesheet>