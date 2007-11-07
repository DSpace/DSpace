<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.1"
   xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
   xmlns:cc="http://creativecommons.org/ns#"
   xmlns:old-cc="http://web.resource.org/cc/"
   xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
   exclude-result-prefixes="old-cc">
   
   <!-- 
      CreativeCommons.xsl
      
      Version: $Revision: 2312 $
      
      Date: $Date: 2007-11-06 07:43:56 -0500 (Tue, 06 Nov 2007) $
      
      Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
      Institute of Technology.  All rights reserved.
      
      Redistribution and use in source and binary forms, with or without
      modification, are permitted provided that the following conditions are
      met:
      
      - Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
      
      - Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
      
      - Neither the name of the Hewlett-Packard Company nor the name of the
      Massachusetts Institute of Technology nor the names of their
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.
      
      THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
      ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
      LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
      A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
      HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
      OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
      ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
      TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
      USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
      DAMAGE.
   -->

   <xsl:output method="xml" indent="yes"/>
   
   <!--  process incoming RDF, copy everything add our own statements for cc:Work -->
   <xsl:template match="/rdf:RDF">
      <rdf:RDF>
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates select="cc:License"/>
      </rdf:RDF>
   </xsl:template>

   <!--  handle License element -->
   <xsl:template match="cc:License">
      <cc:Work rdf:about="">
         <cc:license rdf:resource="{@rdf:about}"/>
      </cc:Work>
      <cc:License>
         <xsl:copy-of select="@*"/>
         <xsl:apply-templates select="node()"/>
      </cc:License>
   </xsl:template>

   <!-- 
      Identity transform 
   -->
   <xsl:template match="node()|@*">
      <xsl:copy>
         <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
   </xsl:template>
   
</xsl:stylesheet>