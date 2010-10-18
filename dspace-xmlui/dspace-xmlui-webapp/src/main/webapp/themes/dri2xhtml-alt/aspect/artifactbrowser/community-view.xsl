<!--
  community-view.xsl

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
    Rendering specific to the community home page.

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


    <xsl:template name="communitySummaryView-DIM">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.community-not-implemented</i18n:text>
    </xsl:template>


    <!-- A community rendered in the detailView pattern; default way of viewing a community. -->
    <xsl:template name="communityDetailView-DIM">
        <div class="detail-view">&#160;
            <!-- Generate the logo, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/>
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                mode="communityDetailView-DIM"/>
        </div>
    </xsl:template>

    <!-- Generate the info about the community from the metadata section -->
    <xsl:template match="dim:dim" mode="communityDetailView-DIM">
        <xsl:if test="string-length(dim:field[@element='description'][not(@qualifier)])&gt;0">
            <p class="intro-text">
                <xsl:copy-of select="dim:field[@element='description'][not(@qualifier)]/node()"/>
            </p>
        </xsl:if>

        <xsl:if test="string-length(dim:field[@element='description'][@qualifier='tableofcontents'])&gt;0">
        	<div class="detail-view-news">
        		<h3><i18n:text>xmlui.dri2xhtml.METS-1.0.news</i18n:text></h3>
        		<p class="news-text">
        			<xsl:copy-of select="dim:field[@element='description'][@qualifier='tableofcontents']/node()"/>
        		</p>
        	</div>
        </xsl:if>

        <xsl:if test="string-length(dim:field[@element='rights'][not(@qualifier)])&gt;0">
        	<div class="detail-view-rights-and-license">
	            <p class="copyright-text">
	                <xsl:copy-of select="dim:field[@element='rights'][not(@qualifier)]/node()"/>
	            </p>
            </div>
        </xsl:if>
    </xsl:template>
 

</xsl:stylesheet>
