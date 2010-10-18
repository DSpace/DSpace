<!--
  common.xsl

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
    Parts of the artifactbrowser which are not
    specific to a single listing or page. These will not
    frequently be adapted in a theme

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
        The summaryList display type; used to generate simple surrogates for the item involved
    -->

    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryList-DIM"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        The detailList display type; used to generate simple surrogates for the item involved, but with
        a slightly higher level of information provided. Not commonly used.
    -->

    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="detailList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailList-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailList-DIM"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        The summaryView display type; used to generate a near-complete view of the item involved. It is currently
        not applicable to communities and collections.
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryView-DIM"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!--
        The detailView display type; used to generate a complete view of the object involved. It is currently
        used with the "full item record" view of items as well as the default views of communities and collections.
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="detailView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailView-DIM"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailView-DIM"/>
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    

    <!-- Generate the logo, if present, from the file section -->
    <xsl:template match="mets:fileGrp[@USE='LOGO']">
        <div class="ds-logo-wrapper">
            <img src="{mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href}" class="logo">
                <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.collection-logo-alt</xsl:attribute>
                <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
            </img>
        </div>
    </xsl:template>
    
</xsl:stylesheet>
