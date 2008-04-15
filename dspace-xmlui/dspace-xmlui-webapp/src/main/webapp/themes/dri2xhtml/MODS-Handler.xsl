<?xml version="1.0" encoding="UTF-8"?>

<!--
  DS-METS-1.0-MODS.xsl

  Version: $Revision: 1.5 $
 
  Date: $Date: 2006/07/27 22:54:52 $
 
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

<xsl:stylesheet 
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
    xmlns:xlink="http://www.w3.org/TR/xlink/"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
    xmlns="http://www.w3.org/1999/xhtml"
    exclude-result-prefixes="i18n dri mets mods dc dcterms dim xlink xsl">
    
    <xsl:output indent="yes"/>
    
    
    
    
    
    
    
    <!-- 
        The summaryList display type; used to generate simple surrogates for the item involved 
    -->
    
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']]" mode="summaryList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryList-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryList-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryList-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <!-- 
        The templates that handle the respective cases of summaryList: item, collection, and community 
    -->
    
    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemSummaryList-MODS">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods"
            mode="itemSummaryList-MODS"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']"/>
    </xsl:template>
    
    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="mods:mods" mode="itemSummaryList-MODS"> 
        <div class="artifact-description">
            <div class="artifact-title">
                <!-- Put down the title -->
                <a href="{ancestor::mets:METS/@OBJID}">
                    <xsl:choose>
                        <xsl:when test="mods:titleInfo/mods:title">
                            <xsl:copy-of select="mods:titleInfo/mods:title[1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </div>
            <div class="artifact-info">
                <span class="author">
                    <xsl:choose>
                        <xsl:when test="mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/node()">
                            <xsl:copy-of select="mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <!-- Put down the date -->
                <span class="date">(<xsl:copy-of select="substring(mods:originInfo/mods:dateIssued[@encoding='iso8601']/node(),1,10)"/>)</span>
            </div>
        </div>
    </xsl:template>
    
    
    <!-- A collection rendered in the summaryList pattern. Encountered on the community-list page -->
    <xsl:template name="collectionSummaryList-MODS">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <a href="{@OBJID}">
            <xsl:value-of select="$data/mods:titleInfo/mods:title"/>
        </a>
    </xsl:template>
    
    <!-- A community rendered in the summaryList pattern. Encountered on the community-list and on 
        on the front page. -->
    <xsl:template name="communitySummaryList-MODS">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <span class="bold">
            <a href="{@OBJID}">
                <xsl:value-of select="$data/mods:titleInfo/mods:title"/>
            </a>
        </span>
    </xsl:template>
    
    
    
    
    
    
    
    
    <!-- 
        The detailList display type; used to generate simple surrogates for the item involved, but with
        a slightly higher level of information provided. Not commonly used. 
    -->
    
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']]" mode="detailList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailList-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailList-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailList-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    <!-- An item rendered in the detailList pattern. Currently Manakin does not have a separate use for 
        detailList on items, so the logic of summaryList is used in its place. --> 
    <xsl:template name="itemDetailList-MODS">
        
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods"
            mode="itemSummaryList-MODS"/>
        
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']"/>
    </xsl:template>
    
    
    <!-- A collection rendered in the summaryList pattern. Encountered on the community-list page -->
    <xsl:template name="collectionDetailList-MODS">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <a href="{@OBJID}">
            <xsl:value-of select="$data/mods:titleInfo/mods:title"/>
        </a>
        <br/>
        <xsl:choose>
            <xsl:when test="$data/mods:abstract">
                <xsl:value-of select="$data/mods:abstract"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$data/mods:note[1]"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
        
    <!-- A community rendered in the summaryList pattern. Encountered on the community-list and on 
        on the front page. -->
    <xsl:template name="communityDetailList-MODS">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <span class="bold">
            <a href="{@OBJID}">
                <xsl:value-of select="$data/mods:titleInfo/mods:title"/>
            </a>
            <br/>
            <xsl:choose>
                <xsl:when test="$data/mods:abstract">
                    <xsl:value-of select="$data/mods:abstract"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$data/mods:note[1]"/>
                </xsl:otherwise>
            </xsl:choose>
        </span>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    <!-- 
        The summaryView display type; used to generate a near-complete view of the item involved. It is currently
        not applicable to communities and collections. 
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']]" mode="summaryView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemSummaryView-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionSummaryView-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communitySummaryView-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- An item rendered in the summaryView pattern. This is the default way to view a DSpace item in Manakin. -->
    <xsl:template name="itemSummaryView-MODS">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods"
            mode="itemSummaryView-MODS"/>
        
        <!-- Generate the bitstream information from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
            <xsl:with-param name="context" select="."/>
            <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
        </xsl:apply-templates>
        
        <!-- Generate the license information from the file section -->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>
        
    </xsl:template>
    
    
    
    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="mods:mods" mode="itemSummaryView-MODS">
        <table class="ds-includeSet-table">
            <!--
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-preview</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                            <a class="image-link">
                                <xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                            mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                    </xsl:attribute>
                                </img>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-preview</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>-->
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="mods:titleInfo/mods:title">
                            <xsl:copy-of select="mods:titleInfo/mods:title[1]/child::node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span></td>
                <td><xsl:copy-of select="mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/child::node()"/></td>
            </tr>
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span></td>
                <td><xsl:copy-of select="mods:abstract/child::node()"/></td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span></td>
                <td><xsl:copy-of select="mods:note[not(@type)]/child::node()"/></td>
            </tr>
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span></td>
                <td>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:copy-of select="mods:identifier[@type='uri'][1]/child::node()"/>
                        </xsl:attribute>
                        <xsl:copy-of select="mods:identifier[@type='uri'][1]/child::node()"/>
                    </a>
                </td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>:</span></td>
                <td><xsl:copy-of select="substring(mods:originInfo/mods:dateIssued[@encoding='iso8601']/child::node(),1,10)"/></td>
            </tr>
        </table>
    </xsl:template>
        
    
    <!-- The detailList of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template name="collectionSummaryView-MODS">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.collection-not-implemented</i18n:text>
    </xsl:template>
    
    <xsl:template name="communitySummaryView-MODS">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.community-not-implemented</i18n:text>
    </xsl:template>
    
    
    
    
    
    
    
    <!-- 
        The detailView display type; used to generate a complete view of the object involved. It is currently
        used with the "full item record" view of items as well as the default views of communities and collections. 
    -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']]" mode="detailView">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="itemDetailView-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Collection'">
                <xsl:call-template name="collectionDetailView-MODS"/>
            </xsl:when>
            <xsl:when test="@LABEL='DSpace Community'">
                <xsl:call-template name="communityDetailView-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    
    
    <!-- An item rendered in the detailView pattern, the "full item record" view of a DSpace item in Manakin. -->
    <xsl:template name="itemDetailView-MODS">
        
        <!-- Output all of the metadata about the item from the metadata section -->
        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods"
            mode="itemDetailView-MODS"/>
        
        <!-- Generate the bitstream information from the file section -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CONTENT']">
            <xsl:with-param name="context" select="."/>
            <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
        </xsl:apply-templates>
        
        <!-- Generate the license information from the file section -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>
        
    </xsl:template>
    
    
    <!-- The block of templates used to render the mods contents of a DRI object -->
    <!-- The first template creates the top level table and sets the order in which the mods elements are
        to be processed. -->
    <xsl:template match="mods:mods" mode="itemDetailView-MODS" priority="2">
		<table class="ds-includeSet-table">
			<xsl:apply-templates mode="itemDetailView-MODS">
			    <xsl:sort data-type="number" order="ascending" select="
			    	  number(name()='mods:titleInfo') * 1
					+ number(name()='mods:abstract') * 2
					+ number(name()='mods:name') *3
					+ number(name()='mods:accessCondition') * 4
					+ number(name()='mods:classification') * 5
					+ number(name()='mods:genre') * 6
					+ number(name()='mods:identifier') * 7 
					+ number(name()='mods:language') * 8
					+ number(name()='mods:location') * 9
					+ number(name()='mods:note') * 10
					+ number(name()='mods:originInfo') * 11 
					+ number(name()='mods:part') * 12
					+ number(name()='mods:physicalDescription') * 13 
					+ number(name()='mods:recordInfo') * 14
					+ number(name()='mods:relatedItem') * 15
					+ number(name()='mods:subject') * 16
					+ number(name()='mods:tableOfContents') * 17 
					+ number(name()='mods:targetAudience') * 18
			    	+ number(name()='mods:typeOfResource') * 19
			        + number(name()='mods:extension') * 20
			        "/>
			</xsl:apply-templates>
		</table>
	</xsl:template>	
	
	<!-- Top level elements, which set the rows and determine coloration -->
	<xsl:template match="mods:mods/mods:*" mode="itemDetailView-MODS">
		<!-- Check to see if the element contains only text or if it has child elements -->
		<xsl:choose>
			<xsl:when test="count(*)=0">
				<tr>
					<xsl:attribute name="class">
		                <xsl:text>ds-table-row </xsl:text>
		                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
		                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
		            </xsl:attribute>
					<td class="header-cell"><xsl:value-of select="local-name()"/></td>
					<td></td>
					<td><xsl:value-of select="text()"/></td>
				</tr>
			</xsl:when>
			<xsl:otherwise>
				<tr>
					<xsl:attribute name="class">
		                <xsl:text>ds-table-row </xsl:text>
		                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
		                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
		            </xsl:attribute>
					<td class="header-cell"><xsl:value-of select="local-name()"/></td>
					<xsl:apply-templates select="*[position()=1]" mode="dependentRow"/>
				</tr>
				<xsl:apply-templates select="*[not(position()=1)]" mode="independentRow">
					<xsl:with-param name="position" select="position()"/>
				</xsl:apply-templates>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- Semi-independent rows created by second level mods elements -->
	<xsl:template match="mods:*" mode="independentRow">
		<xsl:param name="position"/>
		<!-- Check to see if the element contains only text or if it has child elements -->
		<tr>
			<xsl:attribute name="class">
				<xsl:text>ds-table-row </xsl:text>
				<xsl:if test="($position mod 2 = 0)">even </xsl:if>
				<xsl:if test="($position mod 2 = 1)">odd </xsl:if>
			</xsl:attribute>
			<td></td>
			<xsl:choose>
				<xsl:when test="count(*)=0">
					<td><xsl:value-of select="local-name()"/></td>
					<td><xsl:value-of select="text()"/></td>
				</xsl:when>
				<xsl:otherwise>
					<td><xsl:value-of select="local-name()"/>/<xsl:value-of select="local-name(./*)"/></td>
					<td><xsl:value-of select="./*/text()"/></td>
				</xsl:otherwise>
			</xsl:choose>
		</tr>
	</xsl:template>
	
	<!-- Non-independent table cells created inside an already opened row element -->
	<xsl:template match="mods:*" mode="dependentRow">
		<!-- Check to see if the element contains only text or if it has child elements -->
		<xsl:choose>
			<xsl:when test="count(*)=0">
				<td><xsl:value-of select="local-name()"/></td>
				<td><xsl:value-of select="text()"/></td>
			</xsl:when>
			<xsl:otherwise>
				<td><xsl:value-of select="local-name()"/>/<xsl:value-of select="local-name(./*)"/></td>
				<td><xsl:value-of select="./*/text()"/></td>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
		
	<!-- Special case for a dependent row dealing with the DIM extensions and their presentation -->
	<xsl:template match="dim:*" mode="dependentRow">
		<td>
			<xsl:value-of select="@mdschema"/>
			<xsl:text>.</xsl:text>
			<xsl:value-of select="@element"/>
			<xsl:if test="@qualifier">
				<xsl:text>.</xsl:text>
			    <br/>
				<xsl:value-of select="@qualifier"/>
			</xsl:if>
		</td>
		<td><xsl:value-of select="text()"/></td>
	</xsl:template>
    
    
       
    
    <!-- A collection rendered in the detailView pattern; default way of viewing a collection. -->
    <xsl:template name="collectionDetailView-MODS">
        <div class="detail-view">&#160;
            <!-- Generate the logo, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/>
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods"
                mode="collectionDetailView-MODS"/>
        </div>
    </xsl:template>
        
    <!-- The detailView of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template match="mods:mods" mode="collectionDetailView-MODS">
        <xsl:if test="string-length(mods:note[1])&gt;0">
            <p class="intro-text">
                <xsl:value-of select="mods:note[1]"/>
            </p>
        </xsl:if>
        <xsl:if test="string-length(mods:accessCondition[@type='useAndReproducation'])&gt;0">
            <p class="copyright-text">
                <xsl:value-of select="mods:accessCondition[@type='useAndReproducation']"/>
            </p>
        </xsl:if>
        <xsl:if test="string-length(mods:extension/dim:field[@mdschema='dc' and @element='rights.license'])&gt;0">
            <p class="license-text">
                <xsl:value-of select="mods:extension/dim:field[@mdschema='dc' and @element='rights.license']"/>
            </p>
        </xsl:if>
    </xsl:template>
    
    
    <!-- A community rendered in the detailView pattern; default way of viewing a community. -->
    <xsl:template name="communityDetailView-MODS">
        <div class="detail-view">&#160;
            <!-- Generate the logo, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/>
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@MDTYPE='MODS']/mets:xmlData/mods:mods"
                mode="communityDetailView-MODS"/>
        </div>
    </xsl:template>
    
    <!-- The detailView of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template match="mods:mods" mode="communityDetailView-MODS">
        <xsl:if test="string-length(mods:note[1])&gt;0">
            <p class="intro-text">
                <xsl:value-of select="mods:note[1]"/>
            </p>
        </xsl:if>
        <xsl:if test="string-length(mods:accessCondition[@type='useAndReproducation'])&gt;0">
            <p class="copyright-text">
                <xsl:value-of select="mods:accessCondition[@type='useAndReproducation']"/>
            </p>
        </xsl:if>
    </xsl:template>   
    
    
    
</xsl:stylesheet>
