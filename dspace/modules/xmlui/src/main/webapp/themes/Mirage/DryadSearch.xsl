<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/" xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan" xmlns:datetime="http://exslt.org/dates-and-times"
                xmlns:encoder="xalan://java.net.URLEncoder" xmlns:decoder="xalan://java.net.URLDecoder"
                xmlns:strings="http://exslt.org/strings" xmlns:set="http://exslt.org/sets"
                exclude-result-prefixes="xalan set strings decoder encoder datetime"
                version="1.0">



    <xsl:variable name="meta"
                  select="/dri:document/dri:meta/dri:pageMeta/dri:metadata" />



    <!-- Change the fields shown in search results -->
    <xsl:template match="dim:dim" mode="itemSummaryList-DIM-dryad">
        <xsl:variable name="itemWithdrawn" select="@withdrawn" />
        <xsl:variable name="doiIdentifier" select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)][1]" />

        <xsl:call-template name="itemSummaryTemplate">
            <xsl:with-param name="itemUrl" select="false"/>
            <xsl:with-param name="itemWithdrawn" select="$itemWithdrawn"/>
            <xsl:with-param name="doiIdentifier" select="$doiIdentifier"/>
            <xsl:with-param name="isWorkflow" select="boolean(.//dim:field[@element='workflow'][@mdschema='internal'][@qualifier='submitted'])"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="dim:dim" mode="summaryNonArchivedList-DIM">
        <xsl:variable name="itemUrl">
            <xsl:text>internal-item?</xsl:text>
            <xsl:value-of select="substring-after(ancestor::mets:METS/@OBJEDIT, '?')"/>
        </xsl:variable>

        <xsl:call-template name="itemSummaryTemplate">
            <xsl:with-param name="itemUrl" select="$itemUrl"/>
            <xsl:with-param name="itemWithdrawn" select="false"/>
            <xsl:with-param name="doiIdentifier" select="false"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="itemSummaryTemplate">
        <xsl:param name="itemUrl"/>
        <xsl:param name="itemWithdrawn"/>
        <xsl:param name="doiIdentifier"/>
        <xsl:param name="isWorkflow" select="false()"/>

        <div class="artifact-description" style="padding: 6px;">
            <xsl:element name="a">
                <xsl:attribute name="href">
                    <xsl:choose>
                        <xsl:when test="$itemUrl">
                            <xsl:value-of select="$itemUrl"/>
                        </xsl:when>
                        <xsl:when test="$itemWithdrawn">
                            <xsl:value-of select="/mets:METS/@OBJEDIT"/>
                        </xsl:when>
                        <xsl:when test="$doiIdentifier">
                            <xsl:choose>
                                <xsl:when test=".//dim:field[@element='identifier'][not(@qualifier)][starts-with(., 'doi:')]">
                                    <xsl:text>/resource/</xsl:text>
                                    <xsl:copy-of select=".//dim:field[@element='identifier'][@mdschema='dc'][not(@qualifier)][1]"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>/resource/</xsl:text>
                                    <xsl:value-of select="/mets:METS/@ID"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="/mets:METS/@OBJID"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <span class="author">
                    <xsl:choose>
                        <xsl:when
                                test=".//dim:field[@element='contributor'][@qualifier='author']">
                            <xsl:for-each
                                    select=".//dim:field[@element='contributor'][@qualifier='author']">
                                <xsl:choose>
                                    <xsl:when test="contains(., ',')">
                                        <xsl:call-template name="name-parse-reverse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="name-parse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:if
                                        test="count(following-sibling::dim:field[@element='contributor'][@qualifier='author']) != 0">
                                    <xsl:text>, </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test=".//dim:field[@element='creator']">
                            <xsl:for-each select=".//dim:field[@element='creator']">
                                <xsl:choose>
                                    <xsl:when test="contains(., ',')">
                                        <xsl:copy-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="name-parse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:if
                                        test="count(following-sibling::dim:field[@element='creator']) != 0">
                                    <xsl:text>, </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                        <xsl:when test=".//dim:field[@element='contributor']">
                            <xsl:for-each select=".//dim:field[@element='contributor']">
                                <xsl:choose>
                                    <xsl:when test="contains(., ',')">
                                        <xsl:copy-of select="."/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:call-template name="name-parse">
                                            <xsl:with-param name="name" select="node()"/>
                                        </xsl:call-template>
                                    </xsl:otherwise>
                                </xsl:choose>
                                <xsl:if
                                        test="count(following-sibling::dim:field[@element='contributor']) != 0">
                                    <xsl:text>, </xsl:text>
                                </xsl:if>
                            </xsl:for-each>
                        </xsl:when>
                    </xsl:choose>
                </span>
                <xsl:if test="dim:field[@element='date' and @qualifier='issued']">
                    <span class="pub-date">
                        <xsl:text> (</xsl:text>
                        <xsl:value-of
                                select="substring(dim:field[@element='date' and @qualifier='issued']/node(),1,4)"/>
                        <xsl:text>) </xsl:text>
                    </span>
                </xsl:if>
                <span class="artifact-title">
                    <xsl:choose>
                        <xsl:when test="dim:field[@element='title']">
                            <xsl:variable name="title" select="dim:field[@element='title'][1]"/>
                            <xsl:variable name="titleEndChar"
                                          select="substring($title, string-length($title), 1)"/>
                            <xsl:value-of select="$title"/>
                            <xsl:choose>
                                <xsl:when test="$titleEndChar != '.' and $titleEndChar != '?'">
                                    <xsl:text>. </xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>&#160;</xsl:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:when>
                        <xsl:otherwise>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </span>
                <xsl:if
                        test="dim:field[@element='publicationName' and @mdschema='prism']">
                    <span class="italics">
                        <xsl:value-of
                                select="dim:field[@element='publicationName' and @mdschema='prism']"/>
                    </span>
                    <xsl:text> </xsl:text>
                </xsl:if>
                <xsl:if test="not($isWorkflow)">
                    <xsl:if test="$doiIdentifier">
                    <span class="doi">
                        <xsl:variable name="id"
                                      select="dim:field[@element='identifier'][not(@qualifier)][@mdschema='dc'][1]"/>
                         <xsl:choose>
                           <xsl:when test="$id[starts-with(., 'doi')]">
                            <xsl:value-of select="concat('http://dx.doi.org/',substring-after($id,'doi:'))"/>
                           </xsl:when>
                           <xsl:when test="$id[starts-with(.,'http')]">
                             <xsl:value-of select="$id"/>
                           </xsl:when>
                         </xsl:choose>
                    </span>
                    </xsl:if>
                </xsl:if>
            </xsl:element>
            <span class="Z3988">
                <xsl:attribute name="title">
                    <xsl:call-template name="renderCOinS"/>
                </xsl:attribute>
                &#160;
            </span>
        </div>
    </xsl:template>
    <xsl:template match="dri:reference" mode="summaryNonArchivedList">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- Since this is a summary only grab the descriptive metadata, and the thumbnails -->
            <xsl:text>?sections=dmdSec,fileSec&amp;fileGrpTypes=THUMBNAIL</xsl:text>
            <!-- An example of requesting a specific metadata standard (MODS and QDC crosswalks only work for items)->
            <xsl:if test="@type='DSpace Item'">
                <xsl:text>&amp;dmdTypes=DC</xsl:text>
            </xsl:if>-->
        </xsl:variable>
        <xsl:variable name="inner_id"
                      select="dim:field[@element='identifier'][not(@qualifier)][@mdschema='dc'][1]"/>
        <xsl:comment> External Metadata URL: <xsl:value-of select="$externalMetadataURL"/> </xsl:comment>
        <li>
            <xsl:attribute name="class">
                <xsl:text>ds-artifact-item </xsl:text>
                <xsl:if test="$inner_id[starts-with(., 'doi')]">doi-item </xsl:if>
                <xsl:choose>
                    <xsl:when test="position() mod 2 = 0">even</xsl:when>
                    <xsl:otherwise>odd</xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:apply-templates select="document($externalMetadataURL)" mode="summaryNonArchivedList"/>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="summaryNonArchivedList">
        <xsl:choose>
            <xsl:when test="@LABEL='DSpace Item'">
                <xsl:call-template name="summaryNonArchivedList-DIM"/>
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

    <xsl:template name="summaryNonArchivedList-DIM">
        <!-- Generate the info about the item from the metadata section -->
        <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                             mode="summaryNonArchivedList-DIM"/>
        <!-- Generate the thunbnail, if present, from the file section -->
        <xsl:apply-templates select="./mets:fileSec" mode="artifact-preview"/>
    </xsl:template>



</xsl:stylesheet>
