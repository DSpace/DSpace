<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!--
  Copyright (c) 2006, Doeke Zanstra
  All rights reserved.

  Redistribution and use in source and binary forms, with or without modification, 
  are permitted provided that the following conditions are met:

  Redistributions of source code must retain the above copyright notice, this 
  list of conditions and the following disclaimer. Redistributions in binary 
  form must reproduce the above copyright notice, this list of conditions and the 
  following disclaimer in the documentation and/or other materials provided with 
  the distribution.

  Neither the name of the dzLib nor the names of its contributors may be used to 
  endorse or promote products derived from this software without specific prior 
  written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
  THE POSSIBILITY OF SUCH DAMAGE.
-->

  <xsl:output indent="no" omit-xml-declaration="yes" method="text" encoding="UTF-8" media-type="text/x-json"/>
	<xsl:strip-space elements="*"/>
  <!--contant-->
  <xsl:variable name="d">0123456789</xsl:variable>

    <!-- convert root element to an anonymous container -->
    <xsl:template match="root">
        <xsl:text>{</xsl:text>
        <xsl:apply-templates select="child::node()"/>
        <xsl:text>}</xsl:text>
    </xsl:template>

  <!-- ignore document text -->
  <xsl:template match="text()[preceding-sibling::node() or following-sibling::node()]"/>

  <!-- string -->
  <xsl:template match="text()">
    <xsl:call-template name="escape-string">
      <xsl:with-param name="s" select="."/>
    </xsl:call-template>
  </xsl:template>
  
  <!-- Main template for escaping strings; used by above template and for object-properties 
       Responsibilities: placed quotes around string, and chain up to next filter, escape-bs-string -->
  <xsl:template name="escape-string">
    <xsl:param name="s"/>
    <xsl:text>"</xsl:text>
    <xsl:call-template name="escape-bs-string">
      <xsl:with-param name="s" select="$s"/>
    </xsl:call-template>
    <xsl:text>"</xsl:text>
  </xsl:template>
  
  <!-- Escape the backslash (\) before everything else. -->
  <xsl:template name="escape-bs-string">
    <xsl:param name="s"/>
    <xsl:choose>
      <xsl:when test="contains($s,'\')">
        <xsl:call-template name="escape-quot-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'\'),'\\')"/>
        </xsl:call-template>
        <xsl:call-template name="escape-bs-string">
          <xsl:with-param name="s" select="substring-after($s,'\')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="escape-quot-string">
          <xsl:with-param name="s" select="$s"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Escape the double quote ("). -->
  <xsl:template name="escape-quot-string">
    <xsl:param name="s"/>
    <xsl:choose>
      <xsl:when test="contains($s,'&quot;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&quot;'),'\&quot;')"/>
        </xsl:call-template>
        <xsl:call-template name="escape-quot-string">
          <xsl:with-param name="s" select="substring-after($s,'&quot;')"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="$s"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <!-- Replace tab, line feed and/or carriage return by its matching escape code. Can't escape backslash
       or double quote here, because they don't replace characters (&#x0; becomes \t), but they prefix 
       characters (\ becomes \\). Besides, backslash should be seperate anyway, because it should be 
       processed first. This function can't do that. -->
  <xsl:template name="encode-string">
    <xsl:param name="s"/>
    <xsl:choose>
      <!-- tab -->
      <xsl:when test="contains($s,'&#x9;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&#x9;'),'\t',substring-after($s,'&#x9;'))"/>
        </xsl:call-template>
      </xsl:when>
      <!-- line feed -->
      <xsl:when test="contains($s,'&#xA;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&#xA;'),'\n',substring-after($s,'&#xA;'))"/>
        </xsl:call-template>
      </xsl:when>
      <!-- carriage return -->
      <xsl:when test="contains($s,'&#xD;')">
        <xsl:call-template name="encode-string">
          <xsl:with-param name="s" select="concat(substring-before($s,'&#xD;'),'\r',substring-after($s,'&#xD;'))"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise><xsl:value-of select="$s"/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- number (no support for javascript mantise) -->
  <xsl:template match="text()[not(string(number())='NaN')]">
    <xsl:value-of select="."/>
  </xsl:template>

  <!-- boolean, case-insensitive -->
  <xsl:template match="text()[translate(.,'TRUE','true')='true']">true</xsl:template>
  <xsl:template match="text()[translate(.,'FALSE','false')='false']">false</xsl:template>

  <!-- item:null -->
  <xsl:template match="*[count(child::node())=0]">
    <xsl:call-template name="escape-string">
      <xsl:with-param name="s" select="local-name()"/>
    </xsl:call-template>
    <xsl:text>:null</xsl:text>
    <xsl:if test="following-sibling::*">,</xsl:if>
  </xsl:template>

  <!-- object -->
  <xsl:template match="*" name="base">
    <!--<xsl:if test="not(preceding-sibling::*)">{</xsl:if>-->
    <xsl:call-template name="escape-string">
      <xsl:with-param name="s" select="name()"/>
    </xsl:call-template>
    <xsl:text>:</xsl:text>
    <xsl:apply-templates select="child::node()"/>
    <xsl:if test="following-sibling::*">,</xsl:if>
    <!--<xsl:if test="not(following-sibling::*)">}</xsl:if>-->
  </xsl:template>

  <xsl:template match="*" mode="array">
        <xsl:apply-templates select="child::node()"/>
    <xsl:if test="following-sibling::*">,</xsl:if>
  </xsl:template>
  
  <!-- array -->
    <xsl:template match="*[@array = 'true']">
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="name()"/>
        </xsl:call-template>
        <xsl:text>:[</xsl:text>
        <xsl:apply-templates select="child::node()" mode="array"/>
        <xsl:text>]</xsl:text>
        <xsl:if test="following-sibling::*">,</xsl:if>
    </xsl:template>

    <xsl:template match="*[@array = 'true']" mode="array">
        <xsl:text>[</xsl:text>
        <xsl:apply-templates select="child::node()" mode="array"/>
        <xsl:text>]</xsl:text>
        <xsl:if test="following-sibling::*">,</xsl:if>
    </xsl:template>

    <!--Object array-->
    <xsl:template match="*[@object = 'true']" mode="array" priority="2">
        <xsl:text>{</xsl:text>
        <xsl:apply-templates select="child::node()"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="following-sibling::*">,</xsl:if>
    </xsl:template>

    <xsl:template match="*[@object = 'true']">
        <xsl:text>{</xsl:text>
        <xsl:apply-templates select="child::node()"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="following-sibling::*">,</xsl:if>
    </xsl:template>

    <!--Object array-->
    <xsl:template match="*[@namedObject = 'true']" mode="array" priority="2">
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="name()"/>
        </xsl:call-template>
        <xsl:text>:</xsl:text>
        <xsl:text>{</xsl:text>
        <xsl:apply-templates select="child::node()"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="following-sibling::*">,</xsl:if>
    </xsl:template>

    <xsl:template match="*[@namedObject = 'true']">
        <xsl:call-template name="escape-string">
            <xsl:with-param name="s" select="name()"/>
        </xsl:call-template>
        <xsl:text>:</xsl:text>
        <xsl:text>{</xsl:text>
        <xsl:apply-templates select="child::node()"/>
        <xsl:text>}</xsl:text>
        <xsl:if test="following-sibling::*">,</xsl:if>
    </xsl:template>

</xsl:stylesheet>
