<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet version="1.1"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:cc="http://creativecommons.org/ns#"
   xmlns:old-cc="http://web.resource.org/cc/"
   xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
   exclude-result-prefixes="old-cc">

   <!-- 
      LicenseCleanup.xsl
      
      Version: $Revision$
      
      Date: $Date$
   -->

   <xsl:output method="xml" indent="yes" />

   <!--  process incoming RDF, copy everything add our own statements for work -->
   <xsl:template match="/rdf:RDF">
      <rdf:RDF>
         <xsl:copy-of select="@*" />
         <xsl:apply-templates select="cc:License|old-cc:License" />
      </rdf:RDF>
   </xsl:template>

   <!-- 
      handle new format 
   -->

   <!--  handle License element -->
   <xsl:template match="cc:License|old-cc:License">
      <cc:Work rdf:about="">
         <cc:license rdf:resource="{@rdf:about}" />
      </cc:Work>
      <cc:License>
         <xsl:copy-of select="@*" />
         <xsl:apply-templates select="node()" />
      </cc:License>
   </xsl:template>

   <!-- 
      Cleanup for Older copy
   -->

   <!-- transform old namespace elements -->
   <xsl:template match="old-cc:*">
      <xsl:element name="cc:{name()}"
         namespace="http://creativecommons.org/ns#">
         <xsl:copy-of select="@*" />
         <xsl:apply-templates select="node()" />
      </xsl:element>
   </xsl:template>

   <!-- 
      Identity transform 
   -->
   <xsl:template match="node()|@*">
      <xsl:copy>
         <xsl:apply-templates select="node()|@*" />
      </xsl:copy>
   </xsl:template>

</xsl:stylesheet>