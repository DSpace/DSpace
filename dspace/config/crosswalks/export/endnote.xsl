<?xml version="1.0" encoding="UTF-8" ?>
<!--
    Description: Converts metadata from DSpace DataCite Schema 
                 in the RIS file format. If you want to extend this file,
                 please take a look which metadatas are provided by the method
                 org.dspace.util.ExportItemUtils.retrieveMetadata(...). 
-->
<xsl:stylesheet 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai"
    version="1.0">
    <xsl:output method="text" />

    <!-- Newline as a variable -->
    <xsl:variable name='newline'>
        <xsl:text>&#xa;</xsl:text>
    </xsl:variable>
    
    <xsl:template match="/">
        
        <!-- Provider description --> 
        <xsl:text>Provider: DSpace RIS Export</xsl:text>
        <xsl:value-of select="$newline"></xsl:value-of> 
        <xsl:text>Repository: </xsl:text>
        <xsl:value-of select="translate(doc:metadata/doc:element[@name='repository']/doc:field[@name='name']/text(), '/', '_')"></xsl:value-of>      
        <xsl:value-of select="$newline"></xsl:value-of> 
        <xsl:value-of select="$newline"></xsl:value-of> 
        
        <!-- dc.type -->
        <xsl:text>TY  - </xsl:text>
        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
	 <xsl:value-of select="$newline"></xsl:value-of>
         
        <!-- dc.contributor.author -->      		
        <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
            <xsl:text>AU  - </xsl:text>
            <xsl:value-of select="."></xsl:value-of>
            <xsl:value-of select="$newline"></xsl:value-of>
        </xsl:for-each>	
        	
        <!-- dc.date.issued --> 
        <xsl:if test="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']">
            <xsl:text>PY  - </xsl:text>
            <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
            <xsl:value-of select="$newline"></xsl:value-of>
        </xsl:if>
        
        <!-- dc.title -->      		
        <xsl:text>TI  - </xsl:text>
        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
        <xsl:value-of select="$newline"></xsl:value-of>
        	
        <!-- dc.identifier.uri --> 
         <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
            <xsl:text>UR  - </xsl:text>
            <xsl:value-of select="."></xsl:value-of>
            <xsl:value-of select="$newline"></xsl:value-of>
        </xsl:for-each>
        
        <!-- dc.publisher --> 
        <xsl:text>PB  - </xsl:text>
        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
	<xsl:value-of select="$newline"></xsl:value-of>
        
        <!-- dc.language.iso -->     
        <xsl:text>LA  - </xsl:text> 
        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']/text()"></xsl:value-of>
	<xsl:value-of select="$newline"></xsl:value-of>
        
        <!-- dc.description.abstract -->      	 
        <xsl:text>AB  - </xsl:text>
        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']"></xsl:value-of>
	<xsl:value-of select="$newline"></xsl:value-of>
        
        <!-- End of Reference (must be empty and the last tag)-->	
        <xsl:text>ER  - </xsl:text> 
        		
    </xsl:template>
    
</xsl:stylesheet>