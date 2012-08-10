<?xml version="1.0" encoding="UTF-8"?>

<!--
  template.xsl

  Version: $Revision: 3705 $
 
  Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 
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

<!--
    TODO: Describe this XSL file    
    Author: Alexey Maslov
    
-->    

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:import href="../dri2xhtml.xsl"/>
    <xsl:output indent="yes"/>

    <xsl:template name="extraHead-top">
        <script type="text/javascript" src="https://www.google.com/jsapi">
            <xsl:text> </xsl:text>
        </script>
    </xsl:template>

    <xsl:template name="extraBody-end">
        <script type="text/javascript" src="/dspace/themes/dashboard/lib/jquery.ui.datepicker-accessible.min.js">
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript" src="/dspace/themes/dashboard/lib/report-generator-mod.js">
            <xsl:text> </xsl:text>
        </script>
        <script type="text/javascript" src="/dspace/themes/dashboard/visualizeData.js">
            <xsl:text> </xsl:text>
        </script>
    </xsl:template>

    <!-- Just a plain old table cell -->
    <xsl:template match="dri:cell" priority="1">
        <td>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                    <xsl:if test="@n='date'"> date</xsl:if>
                    <xsl:if test="@n='items_added'"> items_added</xsl:if>
                    <xsl:if test="@n='items_total'"> items_total</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </td>
    </xsl:template>
    <xsl:template match="dri:field[@rend='slick']">
      <div>
          <xsl:attribute name="class">
              <xsl:text>slick-wrapper</xsl:text>
          </xsl:attribute>
          <xsl:attribute name="id">
              <xsl:value-of select="translate(@id,'.','_')"/>
              <xsl:text>_wrapper</xsl:text>
          </xsl:attribute>
          <label>
              <xsl:attribute name="for">
                  <xsl:value-of select="translate(@id,'.','_')"/>
              </xsl:attribute>
              <xsl:value-of select="dri:label" />
              <xsl:text>:</xsl:text>
          </label>
          <input>
              <xsl:attribute name="id">
                  <xsl:value-of select="translate(@id,'.','_')"/>
              </xsl:attribute>
              <xsl:attribute name="name">
                  <xsl:value-of select="@n" />
              </xsl:attribute>
              <xsl:if test="@type='text'">
                <xsl:attribute name="class">
                  <xsl:text>date-picker</xsl:text>
                </xsl:attribute>
              </xsl:if>
              <xsl:attribute name="type">
                  <xsl:value-of select="@type" />
              </xsl:attribute>
              <xsl:attribute name="value">
                  <xsl:choose>
                      <xsl:when test="@type='checkbox'">
                          <xsl:value-of select="dri:option/@returnValue" />
                      </xsl:when>
                      <xsl:otherwise>
                          <xsl:value-of select="dri:value" />
                      </xsl:otherwise>
                  </xsl:choose>
              </xsl:attribute>
          </input>
          <p>
              <xsl:attribute name="id">
                  <xsl:text>help_</xsl:text>
                  <xsl:value-of select="translate(@id,'.','_')"/>
              </xsl:attribute>
              <xsl:attribute name="class">
                  <xsl:text>help</xsl:text>
              </xsl:attribute>
              <xsl:value-of select="dri:help" />
          </p>
      </div>
    </xsl:template>
    
</xsl:stylesheet>
