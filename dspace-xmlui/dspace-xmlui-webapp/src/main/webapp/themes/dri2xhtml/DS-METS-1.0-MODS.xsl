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
    
    
    <!-- Creates an index across the object store using the METS profile as a distinguishing characteristic. 
        At the time of this writing the current profile for DSpace and DRI is METS SIP Profile 1.0, with DRI
        making use of an extended profile for communities and collections. Since the change is unofficial, we
        have to tag the two profiles differently, but treat them the same in the code (since at some point in 
        the future the two profiles should merge). 
        
        The index allows two modes of access to the store: by means of the 'all' constant to grab all objects
        that user METS 1.0 profile, or by means of specifying an objectIdentifier to grab a specific object. 
        In the near future I will add a third mode to grab an object by both its objectIdentifier and 
        repositoryIdentifier attributes. -->
    
    <xsl:key name="DSMets1.0-all" match="dri:object[substring-before(mets:METS/@PROFILE,';')='DSPACE METS SIP Profile 1.0'] |
        dri:object[mets:METS/@PROFILE='DSPACE METS SIP Profile 1.0'] |
        dri:object[substring-before(mets:METS/@PROFILE,';')='DSPACE METS SIP Profile 1.0 (DRI extensions for communities/collections)'] |
        dri:object[mets:METS/@PROFILE='DSPACE METS SIP Profile 1.0 (DRI extensions for communities/collections)']"
        use="'all'"/>
    <xsl:key name="DSMets1.0" match="dri:object[substring-before(mets:METS/@PROFILE,';')='DSPACE METS SIP Profile 1.0'] |
        dri:object[mets:METS/@PROFILE='DSPACE METS SIP Profile 1.0'] |
        dri:object[substring-before(mets:METS/@PROFILE,';')='DSPACE METS SIP Profile 1.0 (DRI extensions for communities/collections)'] |
        dri:object[mets:METS/@PROFILE='DSPACE METS SIP Profile 1.0 (DRI extensions for communities/collections)']"
        use="@objectIdentifier"/>
    
        
        
    
    
    
    <!-- The fallback cases for collections/communities as well as items that for whatever reason did not fit
        the table case, but are still using the DSpace METS 1.0 profile. They are broken up into separate 
        templates for simpler overriding. -->
    <xsl:template match="dri:objectInclude[key('DSMets1.0', @objectSource)]" mode="summaryList">
        <li>
            <xsl:apply-templates select="key('DSMets1.0', @objectSource)" mode="summaryList">
                <xsl:with-param name="position" select="position()"/>
            </xsl:apply-templates>
            <xsl:apply-templates />
        </li>
    </xsl:template>
    
    <xsl:template match="key('DSMets1.0-all', 'all')" mode="summaryList">
        <xsl:param name="position"/>
        <xsl:choose>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Item']">
                <xsl:call-template name="itemSummaryList_DS-METS-1.0-MODS">
                    <xsl:with-param name="position" select="$position"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Collection']">
                <xsl:call-template name="collectionSummaryList_DS-METS-1.0-MODS"/>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Community']">
                <xsl:call-template name="communitySummaryList_DS-METS-1.0-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="itemSummaryList_DS-METS-1.0-MODS">
        <xsl:param name="position"/>
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <xsl:attribute name="class">
            <xsl:if test="($position mod 2 = 0)">even </xsl:if>
            <xsl:if test="($position mod 2 = 1)">odd </xsl:if>
            <xsl:text>ds-artifact-item </xsl:text>
        </xsl:attribute>
        <div class="artifact-title">
            <!-- Put down the title -->
            <a>
                <xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
                <xsl:choose>
                    <xsl:when test="$data/mods:titleInfo/mods:title">
                        <xsl:copy-of select="$data/mods:titleInfo/mods:title[1]/child::node()"/>
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
                    <xsl:when test="$data/mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/child::node()">
                        <xsl:copy-of select="$data/mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/child::node()"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </span>
            <!-- Put down the date -->
            <span class="date">(<xsl:copy-of select="substring($data/mods:originInfo/mods:dateIssued[@encoding='iso8601']/child::node(),1,10)"/>)</span>
        </div>
    </xsl:template>
    
    <xsl:template name="collectionSummaryList_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="@url"/>
            </xsl:attribute>
            <xsl:value-of select="$data/mods:titleInfo/mods:title"/>
        </a>
    </xsl:template>
    
    <xsl:template name="communitySummaryList_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <span class="bold">
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="@url"/>
                </xsl:attribute>
                <xsl:value-of select="$data/mods:titleInfo/mods:title"/>
            </a>
        </span>
    </xsl:template>
    
    <!-- And that does it for the summaryList case. Currently it is the only case that make use of the
        "pioneer" paradigm, since in all the other cases there is either only one object being included or
        they are all of the same type. Those cases are:
            - detailList: only exists for communities/collections since there is no current need for a detailed
                list of items. If such a case is added through Aspects, however, the theme will need to 
                handle it, mostly likely in way similar to summaryList.
            - summaryView: while it might be possible to have several objects inside an includeSet of this
                type, it is not the way it is currently used. Right now, communities and collections do not
                make use of this type, while items use it for the default item display page.
            - detailView: only meant to display one object at a time.
    -->
    
    
    
    
    
    <!-- Note to self and others: the purpose for using the seemlingly worthless intermediate step of the
        objectInlude template is two-fold. First, it allows processing of the objects in order that they
        appear in the includeSet, rather than the object store. Second, it makes handling recursive include
        structures much more straightforward. -->
    
    
    
    <!-- Note that despite not using the "pioneer" paradigm, the template that matches the detailList case 
        still check for the proper DSpace profile. -->
    <xsl:template match="dri:objectInclude[key('DSMets1.0', @objectSource)]" mode="detailList">
        <li>
            <xsl:apply-templates select="key('DSMets1.0', @objectSource)" mode="detailList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>    
    
    <!-- The actual handling of the objects, spread across three differnt templates. -->
    <xsl:template match="key('DSMets1.0-all', 'all')" mode="detailList">
        <xsl:choose>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Item']">
                <xsl:call-template name="itemDetailList_DS-METS-1.0-MODS"/>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Collection']">
                <xsl:call-template name="collectionDetailList_DS-METS-1.0-MODS"/>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Community']">
                <xsl:call-template name="communityDetailList_DS-METS-1.0-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- The templates that handle the respective cases: item, collection, and community. In the case of items
        current Manakin build does really have a special use for detailList so the logic of summaryList is 
        basically used in its place. --> 
    <xsl:template name="itemDetailList_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <!-- Put down the author -->
        <xsl:choose>
            <xsl:when test="$data/mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/child::node()">
                <xsl:copy-of select="$data/mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/child::node()"/>.
            </xsl:when>
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-author</i18n:text>
                <xsl:text>. </xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <!-- Put down the title -->
        <a>
            <xsl:attribute name="href"><xsl:value-of select="@url"/></xsl:attribute>
            <xsl:choose>
                <xsl:when test="$data/mods:titleInfo/mods:title">
                    <xsl:copy-of select="$data/mods:titleInfo/mods:title[1]/child::node()"/>.
                </xsl:when>
                <xsl:otherwise>
                    <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                </xsl:otherwise>
            </xsl:choose>
        </a>
        <xsl:text>. </xsl:text>
        <!-- Put down the date -->
        <xsl:copy-of select="substring($data/mods:originInfo/mods:dateIssued[@encoding='iso8601']/child::node(),1,10)"/>
    </xsl:template>
    
    <!-- The detailList of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template name="collectionDetailList_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="@url"/>
            </xsl:attribute>
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
    
    <xsl:template name="communityDetailList_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <span class="bold">
            <a>
                <xsl:attribute name="href">
                    <xsl:value-of select="@url"/>
                </xsl:attribute>
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
    
    
    
    
    
    
    
    
    
    <!-- Note that despite not using the "pioneer" paradigm, the template that matches the detailList case 
        still check for the proper DSpace profile. -->
    <xsl:template match="dri:objectInclude[key('DSMets1.0', @objectSource)]" mode="summaryView">
       <xsl:apply-templates select="key('DSMets1.0', @objectSource)" mode="summaryView"/>
       <xsl:apply-templates />
    </xsl:template>    
    
    <!-- The actual handling of the objects, spread across three differnt templates. -->
    <xsl:template match="key('DSMets1.0-all', 'all')" mode="summaryView">
        <xsl:choose>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Item']">
                <xsl:call-template name="itemSummaryView_DS-METS-1.0-MODS"/>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Collection']">
                <xsl:call-template name="collectionSummaryView_DS-METS-1.0-MODS"/>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Community']">
                <xsl:call-template name="communitySummaryView_DS-METS-1.0-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- The templates that handle the respective cases: item, collection, and community. In the case of items
        current Manakin build does really have a special use for detailList so the logic of summaryList is 
        basically used in its place. --> 
    <xsl:template name="itemSummaryView_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <xsl:variable name="context" select="."/>
        <table class="ds-includeSet-table">
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
            </tr>
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-title</i18n:text>:</span></td>
                <td>
                    <xsl:choose>
                        <xsl:when test="$data/mods:titleInfo/mods:title">
                            <xsl:copy-of select="$data/mods:titleInfo/mods:title[1]/child::node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-author</i18n:text>:</span></td>
                <td><xsl:copy-of select="$data/mods:name[mods:role/mods:roleTerm/text()='author']/mods:namePart/child::node()"/></td>
            </tr>
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-abstract</i18n:text>:</span></td>
                <td><xsl:copy-of select="$data/mods:abstract/child::node()"/></td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-description</i18n:text>:</span></td>
                <td><xsl:copy-of select="$data/mods:note[not(@type)]/child::node()"/></td>
            </tr>
            <tr class="ds-table-row even">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-uri</i18n:text>:</span></td>
                <td>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:copy-of select="$data/mods:identifier[@type='uri'][1]/child::node()"/>
                        </xsl:attribute>
                        <xsl:copy-of select="$data/mods:identifier[@type='uri'][1]/child::node()"/>
                    </a>
                </td>
            </tr>
            <tr class="ds-table-row odd">
                <td><span class="bold"><i18n:text>xmlui.dri2xhtml.METS-1.0.item-date</i18n:text>:</span></td>
                <td><xsl:copy-of select="substring($data/mods:originInfo/mods:dateIssued[@encoding='iso8601']/child::node(),1,10)"/></td>
            </tr>
        </table>
        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <table class="ds-table file-list">
            <tr class="ds-table-header-row">
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
            </tr>
            <!-- First, figure out if there is a primary bitstream -->
            <xsl:variable name="primary" select="mets:METS/mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
            <xsl:choose>
                <!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
                <xsl:when test="mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@ID=$primary]/@MIMETYPE='text/html'">
                    <xsl:call-template name="buildBitstreamRow">
                        <xsl:with-param name="context" select="$context"/>
                        <xsl:with-param name="file" select="mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@ID=$primary]"/>
                    </xsl:call-template>
                </xsl:when>
                <!-- Otherwise, iterate over and display all of them -->
                <xsl:otherwise>
                    <xsl:for-each select="mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file">
                        <xsl:sort select="./mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        <xsl:call-template name="buildBitstreamRow">
                            <xsl:with-param name="context" select="$context"/>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
        </table>
        <xsl:if test="mets:METS/mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']">
            <div class="license-info">
                <p><i18n:text>xmlui.dri2xhtml.METS-1.0.license-text</i18n:text></p>
                <ul>
                    <xsl:if test="mets:METS/mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']">
                        <li><a href="{mets:METS/mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']/mets:file/
                            mets:FLocat[@xlink:title='license_text']/@xlink:href}">Creative Commons</a></li>
                    </xsl:if>
                    <xsl:if test="mets:METS/mets:fileSec/mets:fileGrp[@USE='LICENSE']">
                        <li><a href="{mets:METS/mets:fileSec/mets:fileGrp[@USE='LICENSE']/mets:file/
                            mets:FLocat[@xlink:title='license.txt']/@xlink:href}">Original License</a></li>
                    </xsl:if>
                </ul>
            </div>
        </xsl:if>
    </xsl:template>
    
    <xsl:template name="buildBitstreamRow">
        <xsl:param name="context" select="."/>
        <xsl:param name="file" select="."/>
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
            </xsl:attribute>
            <td>
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                    </xsl:attribute>
                    <xsl:attribute name="title">
                        <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                    </xsl:attribute>
                    <xsl:choose>
                        <xsl:when test="string-length($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title) > 50">
                            <xsl:variable name="title_length" select="string-length($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title)"/>
                            <xsl:value-of select="substring($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title,1,15)"/>
                            <xsl:text> ... </xsl:text>
                            <xsl:value-of select="substring($file/mets:FLocat[@LOCTYPE='URL']/@xlink:title,$title_length - 25,$title_length)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </a>
            </td>
            <!-- File size always comes in bytes and thus needs conversion --> 
            <td>
                <xsl:choose>
                    <xsl:when test="$file/@SIZE &lt; 1000">
                        <xsl:value-of select="$file/@SIZE"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="$file/@SIZE &lt; 1000000">
                        <xsl:value-of select="substring(string($file/@SIZE div 1000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                    </xsl:when>
                    <xsl:when test="$file/@SIZE &lt; 1000000000">
                        <xsl:value-of select="substring(string($file/@SIZE div 1000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="substring(string($file/@SIZE div 1000000000),1,5)"/>
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                    </xsl:otherwise>
                </xsl:choose>
            </td>
            <!-- Currently format carries forward the mime type. In the original DSpace, this 
                would get resolved to an application via the Bitstream Registry, but we are
                constrained by the capabilities of METS and can't really pass that info through. -->
            <td><xsl:value-of select="substring-before($file/@MIMETYPE,'/')"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="substring-after($file/@MIMETYPE,'/')"/>
            </td>
            <td>
                <xsl:choose>
                    <xsl:when test="$context/mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                        mets:file[@GROUP_ID=current()/@GROUP_ID]">
                        <a class="image-link">
                            <xsl:attribute name="href">
                                <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <img alt="Thumbnail">
                                <xsl:attribute name="src">
                                    <xsl:value-of select="$context/mets:METS/mets:fileSec/mets:fileGrp[@USE='THUMBNAIL']/
                                        mets:file[@GROUP_ID=current()/@GROUP_ID]/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                                </xsl:attribute>
                            </img>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="$file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-viewOpen</i18n:text>
                        </a>
                    </xsl:otherwise>
                </xsl:choose>                        
            </td>
        </tr>
    </xsl:template>
    
    <!-- The detailList of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template name="collectionSummaryView_DS-METS-1.0-MODS">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.collection-not-implemented</i18n:text>
    </xsl:template>
    
    <xsl:template name="communitySummaryView_DS-METS-1.0-MODS">
        <i18n:text>xmlui.dri2xhtml.METS-1.0.community-not-implemented</i18n:text>
    </xsl:template>
    
    
    
    
    
    
    
    
    
    
    
    
    <!-- Note that despite not using the "pioneer" paradigm, the template that matches the detailList case 
        still check for the proper DSpace profile. -->
    <xsl:template match="dri:objectInclude[key('DSMets1.0', @objectSource)]" mode="detailView">
       <xsl:apply-templates select="key('DSMets1.0', @objectSource)" mode="detailView"/>
       <xsl:apply-templates />
    </xsl:template>    
    
    <!-- The actual handling of the objects, spread across three differnt templates. -->
    <xsl:template match="key('DSMets1.0-all', 'all')" mode="detailView">
        <xsl:choose>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Item']">
                <xsl:call-template name="itemDetailView_DS-METS-1.0-MODS"/>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Collection']">
                <xsl:call-template name="collectionDetailView_DS-METS-1.0-MODS"/>
            </xsl:when>
            <xsl:when test="mets:METS/mets:structMap/mets:div[@TYPE='DSpace Community']">
                <xsl:call-template name="communityDetailView_DS-METS-1.0-MODS"/>
            </xsl:when>                
            <xsl:otherwise>
                <i18n:text>xmlui.dri2xhtml.METS-1.0.non-conformant</i18n:text>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- The templates that handle the respective cases: item, collection, and community. In the case of items
        current Manakin build does really have a special use for detailList so the logic of summaryList is 
        basically used in its place. --> 
    <xsl:template name="itemDetailView_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <xsl:variable name="context" select="."/>
        <xsl:apply-templates select="$data" mode="detailView"/>
        <h2><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-head</i18n:text></h2>
        <table class="ds-table file-list">
            <tr class="ds-table-header-row">
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-file</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-size</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-format</i18n:text></th>
                <th><i18n:text>xmlui.dri2xhtml.METS-1.0.item-files-view</i18n:text></th>
            </tr>
            <!-- First, figure out if there is a primary bitstream -->
            <xsl:variable name="primary" select="mets:METS/mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
            <xsl:choose>
                <!-- If one exists and it's of text/html MIME type, only display the primary bitstream -->
                <xsl:when test="mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@ID=$primary]/@MIMETYPE='text/html'">
                    <xsl:call-template name="buildBitstreamRow">
                        <xsl:with-param name="context" select="$context"/>
                        <xsl:with-param name="file" select="mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file[@ID=$primary]"/>
                    </xsl:call-template>
                </xsl:when>
                <!-- Otherwise, iterate over and display all of them -->
                <xsl:otherwise>
                    <xsl:for-each select="mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file">
                        <xsl:sort select="./mets:FLocat[@LOCTYPE='URL']/@xlink:title"/>
                        <xsl:call-template name="buildBitstreamRow">
                            <xsl:with-param name="context" select="$context"/>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:otherwise>
            </xsl:choose>
        </table>
        <xsl:if test="mets:METS/mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']">
            <div class="license-info">
                <p>xmlui.dri2xhtml.METS-1.0.license-text</p>
                <ul>
                    <xsl:if test="mets:METS/mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']">
                        <li><a href="{mets:METS/mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']/mets:file/
                            mets:FLocat[@xlink:title='license_text']/@xlink:href}">Creative Commons</a></li>
                    </xsl:if>
                    <xsl:if test="mets:METS/mets:fileSec/mets:fileGrp[@USE='LICENSE']">
                        <li><a href="{mets:METS/mets:fileSec/mets:fileGrp[@USE='LICENSE']/mets:file/
                            mets:FLocat[@xlink:title='license.txt']/@xlink:href}">Original License</a></li>
                    </xsl:if>
                </ul>
            </div>
        </xsl:if>
    </xsl:template>
    
    <!-- The block of templates used to render the mods contents of a DRI object -->
    <!-- The first template creates the top level table and sets the order in which the mods elements are
        to be processed. -->
    <xsl:template match="mods:mods" mode="detailView" priority="2">
		<table class="ds-includeSet-table">
			<xsl:apply-templates>
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
	<xsl:template match="mods:mods/mods:*">
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
    
    <!-- The detailView of communities and collections basically adds a paragraph of info to the mix. -->
    <xsl:template name="collectionDetailView_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <div class="detail-view">
            <div class="ds-logo-wrapper">
                <xsl:choose>
                    <xsl:when test="mets:METS/mets:fileSec/mets:fileGrp[@USE='LOGO']">
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="mets:METS/mets:fileSec/mets:fileGrp[@USE='LOGO']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.collection-logo-alt</xsl:attribute>
                            <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
                            <xsl:attribute name="class">logo</xsl:attribute>
                        </img>
                    </xsl:when>
                    <xsl:otherwise>
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                                <xsl:text>/themes/</xsl:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                                <xsl:text>/no_logo.jpg</xsl:text>
                            </xsl:attribute>
                            <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.no-logo-alt</xsl:attribute>
                            <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
                            <xsl:attribute name="class">logo</xsl:attribute>
                        </img>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
            <xsl:if test="string-length($data/mods:note[1])&gt;0">
	            <p class="intro-text">
	                <xsl:value-of select="$data/mods:note[1]"/>
	            </p>
            </xsl:if>
            <xsl:if test="string-length($data/mods:accessCondition[@type='useAndReproducation'])&gt;0">
	            <p class="copyright-text">
	                <xsl:value-of select="$data/mods:accessCondition[@type='useAndReproducation']"/>
	            </p>
            </xsl:if>
            <xsl:if test="string-length($data/mods:extension/dim:field[@mdschema='dc' and @element='rights.license'])&gt;0">
	            <p class="license-text">
	                <xsl:value-of select="$data/mods:extension/dim:field[@mdschema='dc' and @element='rights.license']"/>
  	            </p>
            </xsl:if>
        </div>
    </xsl:template>
    
    <xsl:template name="communityDetailView_DS-METS-1.0-MODS">
        <xsl:variable name="data" select="./mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/mods:mods"/>
        <div class="detail-view">
            <div class="ds-logo-wrapper">
                <xsl:choose>
                    <xsl:when test="mets:METS/mets:fileSec/mets:fileGrp[@USE='LOGO']">
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="mets:METS/mets:fileSec/mets:fileGrp[@USE='LOGO']/mets:file/mets:FLocat[@LOCTYPE='URL']/@xlink:href"/>
                            </xsl:attribute>
                            <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.community-logo-alt</xsl:attribute>
                            <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
                            <xsl:attribute name="class">logo</xsl:attribute>
                        </img>
                    </xsl:when>
                    <xsl:otherwise>
                        <img>
                            <xsl:attribute name="src">
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath'][not(@qualifier)]"/>
                                <xsl:text>/themes/</xsl:text>
                                <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='theme'][@qualifier='path']"/>
                                <xsl:text>/no_logo.jpg</xsl:text>
                            </xsl:attribute>
                            <xsl:attribute name="alt">xmlui.dri2xhtml.METS-1.0.no-logo-alt</xsl:attribute>
                            <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">alt</xsl:attribute>
                            <xsl:attribute name="class">logo</xsl:attribute>
                        </img>
                    </xsl:otherwise>
                </xsl:choose>
            </div>
            <xsl:if test="string-length($data/mods:note[1])&gt;0">
	            <p class="intro-text">
	                <xsl:value-of select="$data/mods:note[1]"/>
	            </p>
            </xsl:if>
            <xsl:if test="string-length($data/mods:accessCondition[@type='useAndReproducation'])&gt;0">
	            <p class="copyright-text">
	                <xsl:value-of select="$data/mods:accessCondition[@type='useAndReproducation']"/>
	            </p>
            </xsl:if>
        </div>
    </xsl:template>
    
    
    
    
</xsl:stylesheet>
