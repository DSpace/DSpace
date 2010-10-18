<!--
  COinS.xsl

  Version: $Revision: 3705 $

  Date: $Date: 2009-04-11 17:02:24 +0000 (Sat, 11 Apr 2009) $

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
    Rendering of the OpenURL COinS references

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns:atom="http://www.w3.org/2005/Atom"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:oreatom="http://www.openarchives.org/ore/atom/"
    xmlns="http://www.w3.org/1999/xhtml"
    xmlns:xalan="http://xml.apache.org/xalan"
    xmlns:encoder="xalan://java.net.URLEncoder"
    exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">

    <xsl:output indent="yes"/>


       <!--
    *********************************************
    OpenURL COinS Rendering Template
    *********************************************

    COinS Example:

    <span class="Z3988"
    title="ctx_ver=Z39.88-2004&amp;
    rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;
    rfr_id=info%3Asid%2Focoins.info%3Agenerator&amp;
    rft.title=Making+WordPress+Content+Available+to+Zotero&amp;
    rft.aulast=Kraus&amp;
    rft.aufirst=Kari&amp;
    rft.subject=News&amp;
    rft.source=Zotero%3A+The+Next-Generation+Research+Tool&amp;
    rft.date=2007-02-08&amp;
    rft.type=blogPost&amp;
    rft.format=text&amp;
    rft.identifier=http://www.zotero.org/blog/making-wordpress-content-available-to-zotero/&amp;
    rft.language=English"></span>

    This Code does not parse authors names, instead relying on dc.contributor to populate the
    coins
     -->

    <xsl:template name="renderCOinS">
       <xsl:text>ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;</xsl:text>
       <xsl:for-each select=".//dim:field[@element = 'identifier']">
            <xsl:text>rft_id=</xsl:text>
            <xsl:value-of select="encoder:encode(string(.))"/>
            <xsl:text>&amp;</xsl:text>
        </xsl:for-each>
        <xsl:text>rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;</xsl:text>
        <xsl:for-each select=".//dim:field[@element != 'description' and @mdschema !='dc' and @qualifier != 'provenance']">
            <xsl:value-of select="concat('rft.', @element,'=',encoder:encode(string(.))) "/>
            <xsl:if test="position()!=last()">
                <xsl:text>&amp;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
 

</xsl:stylesheet>
