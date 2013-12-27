<?xml version="1.0" encoding="UTF-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    TODO: Describe this XSL file    
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
    <!--  
    the above should be replaced with if Saxon is going to be used.
    
     -->
    <xsl:output indent="yes"/>
    
       
    <!-- Some issues:
        - The named templates that are used to break up the monolithic top-level cases (like detailList, for
            example) could potentially conflict with named templates in other metadata handlers. So if, for
            example, I have a MODS and a DIM handler, they will match their respective object templates 
            correctly, since those check for the profile. However, if those templates then break the processing
            up between named templates, and those named templates happen to have the same name between the two
            handlers, a conflict will occur. You will have called a template that is expecting a different 
            profile, which will in turn lead to it not finding the metadata it is expecting. 
        
          The solution to this issue (which would be a pain to debug if it were to happen) is to make sure that
            if you do use named templates, you make their names unique. It would have been a clean and simple 
            solution to just place the name of the profile into the name template's mode, but alas XSL does not
            allow that. 
    -->
    
    
    
    
    
    
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
        The templates that handle the respective cases of summaryList: item, collection, and community 
    -->
    
    <!-- An item rendered in the summaryList pattern. Commonly encountered in various browse-by pages
        and search results. -->
    <xsl:template name="itemSummaryList-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="itemSummaryList-DIM"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
    </xsl:template>
    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemSummaryList-DIM"> 
        <xsl:variable name="itemWithdrawn" select="@withdrawn" />

        <div class="artifact-description">
		   <div class="artifact-title">
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="$itemWithdrawn">
                                <xsl:value-of select="ancestor::mets:METS/@OBJEDIT" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="ancestor::mets:METS/@OBJID" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="dim:field[@mdschema='dc' and @element='title'] and (string-length(dim:field[@element='title']) &gt; 0)">
                            <xsl:value-of select="dim:field[@mdschema='dc' and @element='title'][1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:element>
               <!-- Generate COinS with empty content per spec but force Cocoon to not create a minified tag  -->
               <span class="Z3988">
                   <xsl:attribute name="title">
                       <xsl:call-template name="renderCOinS"/>
                   </xsl:attribute>
                   &#xFEFF; <!-- non-breaking space to force separating the end tag -->
               </span>
           </div>
            <div class="artifact-info">
                <span class="author">
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
                            <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                <span>
                                  <xsl:if test="@authority">
                                    <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                                  </xsl:if>
                                  <xsl:copy-of select="node()"/>
                                </span>
                                <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dim:field[@element='creator']">
                            <xsl:for-each select="dim:field[@element='creator']">
                                <xsl:copy-of select="node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="dim:field[@element='contributor']">
                            <xsl:for-each select="dim:field[@element='contributor']">
                                <xsl:copy-of select="node()"/>
                                <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                    <xsl:text>; </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <xsl:text> </xsl:text>
                <xsl:if test="dim:field[@element='date' and @qualifier='issued'] or dim:field[@element='publisher']">
	                <span class="publisher-date">
	                    <xsl:text>(</xsl:text>
	                    <xsl:if test="dim:field[@element='publisher']">
	                        <span class="publisher">
	                            <xsl:copy-of select="dim:field[@element='publisher']/node()"/>
	                        </span>
	                        <xsl:text>, </xsl:text>
	                    </xsl:if>
	                    <span class="date">
	                        <xsl:value-of select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,10)"/>
	                    </span>
	                    <xsl:text>)</xsl:text>
	                </span>
                </xsl:if>
            </div>
        </div>
    </xsl:template>
    

    <!-- A collection rendered in the summaryList pattern. Encountered on the community-list page -->
    <xsl:template name="collectionSummaryList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <a href="{@OBJID}">
            <xsl:choose>
	            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
	                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
	            </xsl:when>
	            <xsl:otherwise>
	                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
	            </xsl:otherwise>
            </xsl:choose>
        </a>
        <!--Display collection strengths (item counts) if they exist-->
        <span class="ds-strength">
            <xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                <xsl:text> [</xsl:text>
                <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>]</xsl:text>
            </xsl:if>
        </span>
    </xsl:template>

    <!-- A community rendered in the summaryList pattern. Encountered on the community-list and on 
        on the front page. -->
    <xsl:template name="communitySummaryList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <span class="bold">
            <a href="{@OBJID}">
	            <xsl:choose>
		            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
		                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
		            </xsl:when>
		            <xsl:otherwise>
		                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
		            </xsl:otherwise>
	            </xsl:choose>
            </a>
            <!--Display community strengths (item counts) if they exist-->
            <span class="ds-strength">
                <xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                    <xsl:text> [</xsl:text>
                    <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
                    <xsl:text>]</xsl:text>
                </xsl:if>
            </span>
        </span>
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
        
    <!-- An item rendered in the detailList pattern. Currently Manakin does not have a separate use for 
        detailList on items, so the logic of summaryList is used in its place. --> 
    <xsl:template name="itemDetailList-DIM">
        
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
        mode="itemSummaryList-DIM"/>
        
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
    </xsl:template>
    
    
    <!-- A collection rendered in the detailList pattern. Encountered on the item view page as 
        the "this item is part of these collections" list -->
    <xsl:template name="collectionDetailList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <a href="{@OBJID}">
            <xsl:choose>
	            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
	                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
	            </xsl:when>
	            <xsl:otherwise>
	                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
	            </xsl:otherwise>
            </xsl:choose>
        </a>
		<!--Display collection strengths (item counts) if they exist-->
		<xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
            <xsl:text> [</xsl:text>
            <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
            <xsl:text>]</xsl:text>
        </xsl:if>
        <br/>
        <xsl:choose>
            <xsl:when test="$data/dim:field[@element='description' and @qualifier='abstract']">
                <xsl:copy-of select="$data/dim:field[@element='description' and @qualifier='abstract']/node()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:copy-of select="$data/dim:field[@element='description'][1]/node()"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- A community rendered in the detailList pattern. Not currently used. -->
    <xsl:template name="communityDetailList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <span class="bold">
            <a href="{@OBJID}">
                <xsl:choose>
		            <xsl:when test="string-length($data/dim:field[@element='title'][1]) &gt; 0">
		                <xsl:value-of select="$data/dim:field[@element='title'][1]"/>
		            </xsl:when>
		            <xsl:otherwise>
		                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
		            </xsl:otherwise>
           		</xsl:choose>
            </a>
			<!--Display community strengths (item counts) if they exist-->
			<xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                <xsl:text> [</xsl:text>
                <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
                <xsl:text>]</xsl:text>
            </xsl:if>
            <br/>
            <xsl:choose>
                <xsl:when test="$data/dim:field[@element='description' and @qualifier='abstract']">
                    <xsl:copy-of select="$data/dim:field[@element='description' and @qualifier='abstract']/node()"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:copy-of select="$data/dim:field[@element='description'][1]/node()"/>
                </xsl:otherwise>
            </xsl:choose>
        </span>
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
    
    <!-- An item rendered in the summaryView pattern. This is the default way to view a DSpace item in Manakin. -->
    <xsl:template name="itemSummaryView-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
        mode="itemSummaryView-DIM"/>
        
        <!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                    <xsl:with-param name="context" select="."/>
                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                </xsl:apply-templates>
            </xsl:when>
            <!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']"/>
            </xsl:when>
            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2> 
                <table class="ds-table file-list">
                    <tr class="ds-table-header-row">
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                    </tr>
                    <tr>
                        <td colspan="4">
                            <p><i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text></p>
                        </td>
                    </tr>
                </table>
            </xsl:otherwise>
        </xsl:choose>

        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default)-->
        <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>

    </xsl:template>
    
    
    <!-- Generate the info about the item from the metadata section -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <table class="ds-includeSet-table">
         <xsl:call-template name="itemSummaryView-DIM-fields">
         </xsl:call-template>
        </table>
        <!--  Generate COinS  -->
        <span class="Z3988">
            <xsl:attribute name="title">
                <xsl:call-template name="renderCOinS"/>
            </xsl:attribute>
	    &#xFEFF; <!-- non-breaking space to force separating the end tag -->
        </span>
    </xsl:template>

    <!-- render each field on a row, alternating phase between odd and even -->
    <!-- recursion needed since not every row appears for each Item. -->
    <xsl:template name="itemSummaryView-DIM-fields">
      <xsl:param name="clause" select="'1'"/>
      <xsl:param name="phase" select="'even'"/>
      <xsl:variable name="otherPhase">
            <xsl:choose>
              <xsl:when test="$phase = 'even'">
                <xsl:text>odd</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                <xsl:text>even</xsl:text>
              </xsl:otherwise>
            </xsl:choose>
      </xsl:variable>

      <xsl:choose>

            <!--  artifact?
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-preview</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']">
                            <a class="image-link">
                                <xsl:attribute name="href"><xsl:value-of select="@OBJID"/></xsl:attribute>
                                <img alt="Thumbnail">
                                    <xsl:attribute name="src">
                                        <xsl:value-of select="mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
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
            
          <!-- Title row -->
          <xsl:when test="$clause = 1">
            <tr class="ds-table-row {$phase}">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>: </span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) &gt; 1">
                            <xsl:for-each select="dim:field[@element='title'][not(@qualifier)]">
                        	   <xsl:value-of select="./node()"/>
                        	   <xsl:if test="count(following-sibling::dim:field[@element='title'][not(@qualifier)]) != 0">
                                    <xsl:text>; </xsl:text><br/>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test="count(dim:field[@element='title'][not(@qualifier)]) = 1">
                            <xsl:value-of select="dim:field[@element='title'][not(@qualifier)][1]/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>
            <xsl:call-template name="itemSummaryView-DIM-fields">
              <xsl:with-param name="clause" select="($clause + 1)"/>
              <xsl:with-param name="phase" select="$otherPhase"/>
            </xsl:call-template>
          </xsl:when>

          <!-- Author(s) row -->
          <xsl:when test="$clause = 2 and (dim:field[@element='contributor'][@qualifier='author'] or dim:field[@element='creator'] or dim:field[@element='contributor'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span></td>
	                <td>
	                    <xsl:choose>
	                        <xsl:when test="dim:field[@element='contributor'][@qualifier='author']">
	                            <xsl:for-each select="dim:field[@element='contributor'][@qualifier='author']">
                                        <span>
                                          <xsl:if test="@authority">
                                            <xsl:attribute name="class"><xsl:text>ds-dc_contributor_author-authority</xsl:text></xsl:attribute>
                                          </xsl:if>
	                                <xsl:copy-of select="node()"/>
                                        </span>
	                                <xsl:if test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:when test="dim:field[@element='creator']">
	                            <xsl:for-each select="dim:field[@element='creator']">
	                                <xsl:copy-of select="node()"/>
	                                <xsl:if test="count(following-sibling::dim:field[@element='creator']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:when test="dim:field[@element='contributor']">
	                            <xsl:for-each select="dim:field[@element='contributor']">
	                                <xsl:copy-of select="node()"/>
	                                <xsl:if test="count(following-sibling::dim:field[@element='contributor']) != 0">
	                                    <xsl:text>; </xsl:text>
	                                </xsl:if>
	                            </xsl:for-each>
	                        </xsl:when>
	                        <xsl:otherwise>
	                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
	                        </xsl:otherwise>
	                    </xsl:choose>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- Abstract row -->
          <xsl:when test="$clause = 3 and (dim:field[@element='description' and @qualifier='abstract'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span></td>
	                <td>
	                <xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
	                	<hr class="metadata-separator"/>
	                </xsl:if>
	                <xsl:for-each select="dim:field[@element='description' and @qualifier='abstract']">
		                <xsl:copy-of select="./node()"/>
		                <xsl:if test="count(following-sibling::dim:field[@element='description' and @qualifier='abstract']) != 0">
	                    	<hr class="metadata-separator"/>
	                    </xsl:if>
	              	</xsl:for-each>
	              	<xsl:if test="count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1">
	                	<hr class="metadata-separator"/>
	                </xsl:if>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- Description row -->
          <xsl:when test="$clause = 4 and (dim:field[@element='description' and not(@qualifier)])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span></td>
	                <td>
	                <xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1 and not(count(dim:field[@element='description' and @qualifier='abstract']) &gt; 1)">
	                	<hr class="metadata-separator"/>
	                </xsl:if>
	                <xsl:for-each select="dim:field[@element='description' and not(@qualifier)]">
		                <xsl:copy-of select="./node()"/>
		                <xsl:if test="count(following-sibling::dim:field[@element='description' and not(@qualifier)]) != 0">
	                    	<hr class="metadata-separator"/>
	                    </xsl:if>
	               	</xsl:for-each>
	               	<xsl:if test="count(dim:field[@element='description' and not(@qualifier)]) &gt; 1">
	                	<hr class="metadata-separator"/>
	                </xsl:if>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- identifier.uri row -->
          <xsl:when test="$clause = 5 and (dim:field[@element='identifier' and @qualifier='uri'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span></td>
	                <td>
	                	<xsl:for-each select="dim:field[@element='identifier' and @qualifier='uri']">
		                    <a>
		                        <xsl:attribute name="href">
		                            <xsl:copy-of select="./node()"/>
		                        </xsl:attribute>
		                        <xsl:copy-of select="./node()"/>
		                    </a>
		                    <xsl:if test="count(following-sibling::dim:field[@element='identifier' and @qualifier='uri']) != 0">
		                    	<br/>
		                    </xsl:if>
	                    </xsl:for-each>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- date.issued row -->
          <xsl:when test="$clause = 6 and (dim:field[@element='date' and @qualifier='issued'])">
                    <tr class="ds-table-row {$phase}">
	                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>:</span></td>
	                <td>
		                <xsl:for-each select="dim:field[@element='date' and @qualifier='issued']">
		                	<xsl:copy-of select="substring(./node(),1,10)"/>
		                	 <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='issued']) != 0">
	                    	<br/>
	                    </xsl:if>
		                </xsl:for-each>
	                </td>
	            </tr>
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$otherPhase"/>
              </xsl:call-template>
          </xsl:when>

          <!-- recurse without changing phase if we didn't output anything -->
          <xsl:otherwise>
            <!-- IMPORTANT: This test should be updated if clauses are added! -->
            <xsl:if test="$clause &lt; 7">
              <xsl:call-template name="itemSummaryView-DIM-fields">
                <xsl:with-param name="clause" select="($clause + 1)"/>
                <xsl:with-param name="phase" select="$phase"/>
              </xsl:call-template>
            </xsl:if>
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    
    <!-- The summaryView of communities and collections is undefined. -->
    <xsl:template name="collectionSummaryView-DIM">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.collection-not-implemented</i18n:text>
    </xsl:template>
    
    <xsl:template name="communitySummaryView-DIM">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.community-not-implemented</i18n:text>
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
    
    
    <!-- An item rendered in the detailView pattern, the "full item record" view of a DSpace item in Manakin. -->
    <xsl:template name="itemDetailView-DIM">
        
        <!-- Output all of the metadata about the item from the metadata section -->
        <xsl:apply-templates select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
            mode="itemDetailView-DIM"/>
        
		<!-- Generate the bitstream information from the file section -->
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT' or @USE='ORIGINAL']">
                    <xsl:with-param name="context" select="."/>
                    <xsl:with-param name="primaryBitstream" select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                </xsl:apply-templates>
            </xsl:when>
            <!-- Special case for handling ORE resource maps stored as DSpace bitstreams -->
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='ORE']">
                <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='ORE']"/>
            </xsl:when>
            <xsl:otherwise>
                <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2> 
                <table class="ds-table file-list">
                    <tr class="ds-table-header-row">
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                        <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                    </tr>
                    <tr>
                        <td colspan="4">
                            <p><i18n:text>xmlui.dri2xhtml.METS-1.0.item-no-files</i18n:text></p>
                        </td>
                    </tr>
                </table>
            </xsl:otherwise>
        </xsl:choose>

        
        <!-- Generate the Creative Commons license information from the file section (DSpace deposit license hidden by default) -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>
        
    </xsl:template>
    
    
    <!-- The block of templates used to render the complete DIM contents of a DRI object -->
    <xsl:template match="dim:dim" mode="itemDetailView-DIM">
     	<table class="ds-includeSet-table">
		    <xsl:apply-templates mode="itemDetailView-DIM"/>
		</table>
        <span class="Z3988">
            <xsl:attribute name="title">
                 <xsl:call-template name="renderCOinS"/>
            </xsl:attribute>
            &#xFEFF; <!-- non-breaking space to force separating the end tag -->
        </span>
    </xsl:template>
            
    <xsl:template match="dim:field" mode="itemDetailView-DIM">
        <xsl:if test="not(@element='description' and @qualifier='provenance')">
            <tr>
                <xsl:attribute name="class">
                    <xsl:text>ds-table-row </xsl:text>
                    <xsl:if test="(position() div 2 mod 2 = 0)">even </xsl:if>
                    <xsl:if test="(position() div 2 mod 2 = 1)">odd </xsl:if>
                </xsl:attribute>
                <td>
                    <xsl:value-of select="./@mdschema"/>
                    <xsl:text>.</xsl:text>
                    <xsl:value-of select="./@element"/>
                    <xsl:if test="./@qualifier">
                        <xsl:text>.</xsl:text>
                        <xsl:value-of select="./@qualifier"/>
                    </xsl:if>
                </td>
            <td>
              <xsl:copy-of select="./node()"/>
              <xsl:if test="./@authority and ./@confidence">
                <xsl:call-template name="authorityConfidenceIcon">
                  <xsl:with-param name="confidence" select="./@confidence"/>
                </xsl:call-template>
              </xsl:if>
            </td>
                <td><xsl:value-of select="./@language"/></td>
            </tr>
        </xsl:if>
    </xsl:template>

	
	
	
    <!-- A collection rendered in the detailView pattern; default way of viewing a collection. -->
    <xsl:template name="collectionDetailView-DIM">
        <div class="detail-view">&#160;
            <!-- Generate the logo, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/>
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                mode="collectionDetailView-DIM"/>
        </div>
    </xsl:template>
    
    <!-- Generate the info about the collection from the metadata section -->
    <xsl:template match="dim:dim" mode="collectionDetailView-DIM"> 
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
    
    
	
    <!-- Rendering the file list from an Atom ReM bitstream stored in the ORE bundle -->
    <xsl:template match="mets:fileGrp[@USE='ORE']">
        <xsl:variable name="AtomMapURL" select="concat('cocoon:/',substring-after(mets:file/mets:FLocat[@LOCTYPE='URL']//@*[local-name(.)='href'],$context-path))"/>
        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <table class="ds-table file-list">
            <thead>
                <tr class="ds-table-header-row">
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                    <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
                </tr>
            </thead>
            <tbody>
                <xsl:apply-templates select="document($AtomMapURL)/atom:entry/atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
                    <xsl:sort select="@title"/>
                </xsl:apply-templates>
            </tbody>
        </table>
    </xsl:template>    
	
	
    <!-- Iterate over the links in the ORE resource maps and make them into bitstream references in the file section -->
    <xsl:template match="atom:link[@rel='http://www.openarchives.org/ore/terms/aggregates']">
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="@href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:choose>
                            <xsl:when test="@title">
                                <xsl:value-of select="@title"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="@href"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length(@title) > 50">
                            <xsl:variable name="title_length" select="string-length(@title)"/>
                            <xsl:value-of select="substring(@title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring(@title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:when test="@title">
                            <xsl:value-of select="@title"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="@href"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </td>
            <!-- File size always comes in bytes and thus needs conversion --> 
            <td>
                <xsl:choose>
                    <xsl:when test="@length &lt; 1000">
                        <xsl:value-of select="@length"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@length &lt; 1000000">
                        <xsl:value-of select="substring(string(@length div 1000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@length &lt; 1000000001">
                        <xsl:value-of select="substring(string(@length div 1000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="@length &gt; 1000000000">
                        <xsl:value-of select="substring(string(@length div 1000000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:when>
                    <!-- When one isn't available -->
                    <xsl:otherwise><xsl:text>n/a</xsl:text></xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Currently format carries forward the mime type. In the original DSpace, this 
                would get resolved to an application via the Bitstream Registry, but we are
                constrained by the capabilities of METS and can't really pass that info through. -->
            <td>
                <xsl:value-of select="substring-before(@type,'/')"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="substring-after(@type,'/')"/>
            </td>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="@href"/>
                    </xsl:attribute>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                </a>
            </td>
        </tr>
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

    <!-- If you are using SFX, uncomment the template below
         and comment out the default renderCOinS template -->

        <!-- SFX renderCOinS

	        <xsl:template name="renderCOinS">
	        <xsl:text>ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;</xsl:text>
	        <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='sfx'][@qualifier='server']"/>
	        <xsl:text>&amp;</xsl:text>
	        <xsl:text>rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;</xsl:text>
	        </xsl:template>
        -->

    <xsl:template name="renderCOinS">
       <xsl:text>ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;</xsl:text>
       <xsl:for-each select=".//dim:field[@element = 'identifier']">
            <xsl:text>rft_id=</xsl:text>
            <xsl:value-of select="encoder:encode(string(.), 'UTF-8')"/>
            <xsl:text>&amp;</xsl:text>
        </xsl:for-each>
        <xsl:text>rfr_id=info%3Asid%2Fdspace.org%3Arepository&amp;</xsl:text>
        <xsl:for-each select=".//dim:field[@mdschema='dc' and @element != 'description' and @element != 'embargo' and @qualifier != 'provenance']">

            <!-- We do need a simple DC crosswalk in place for this, but for now at least fix author
                 - most other fields will be ok -->
            
            <xsl:choose>
            <xsl:when test="@element = 'contributor' and @qualifier='author'">
                <xsl:value-of select="concat('rft.', 'creator','=',encoder:encode(string(.), 'UTF-8')) "/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat('rft.', @element,'=',encoder:encode(string(.), 'UTF-8')) "/>
            </xsl:otherwise>
            </xsl:choose>

            <xsl:if test="position()!=last()">
                <xsl:text>&amp;</xsl:text>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    
</xsl:stylesheet>
