<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet 
        xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
        xmlns:dri="http://di.tamu.edu/DRI/1.0/"
        xmlns:mets="http://www.loc.gov/METS/"
        xmlns:dc="http://purl.org/dc/elements/1.1/"
        xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
        xmlns:mods="http://www.loc.gov/mods/v3"
        xmlns:xlink="http://www.w3.org/TR/xlink/"
        xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:tdl="http://www.tdl.org/NS/tdl" version="1.0">

        <xsl:import href="../shared.xsl"/>
        <xsl:output indent="yes"/>
        
    <!-- Set up the key for the Muenchian grouping -->
    <xsl:key name="issues-by-vol" match="tdl:issue" use="@vol" />
    
    <!--
        The document variable is a reference to the top of the original DRI 
        document. This can be usefull in situations where the XSL has left
        the original document's context such as after a document() call and 
        would like to retrieve information back from the base DRI document.
    -->
    <xsl:variable name="document" select="/dri:document"/>
    
    <xsl:variable name="hidesearch" select="contains(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='queryString']/text(),'hidesearch')"/>
    <xsl:variable name="discoveryUrl" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='search' and @qualifier='simpleURL']"/>

     <!-- A collection rendered in the detailView pattern; default way of viewing a collection. -->
    <xsl:template name="collectionDetailView-DIM">
        <div class="detail-view">&#160;
            <!-- Generate the logo, if present, from the file section -->
            <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='LOGO']"/>
            <!-- Generate the info about the collections from the metadata section -->
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                mode="collectionDetailView-DIM"/>
        </div>
        
        <xsl:apply-templates select="//tdl:issue[generate-id(.) = generate-id(key('issues-by-vol', @vol)[1])]" />
         <xsl:variable name="collection_handle" select="substring-after($document/dri:meta/dri:pageMeta/dri:metadata[@element='focus' and @qualifier='container'], ':')" />

        <p style="padding-top: 50px;"> </p>
        <p>
            <a href="{$context-path}/handle/{$collection_handle}/advanced-search">Search within this collection</a>
        </p>
    </xsl:template>

    <xsl:template name="string-replace-all">
      <xsl:param name="text" />
      <xsl:param name="replace" />
      <xsl:param name="by" />
      <xsl:choose>
        <xsl:when test="contains($text, $replace)">
          <xsl:value-of select="substring-before($text,$replace)" />
          <xsl:value-of select="$by" />
          <xsl:call-template name="string-replace-all">
            <xsl:with-param name="text" select="substring-after($text,$replace)" />
            <xsl:with-param name="replace" select="$replace" />
            <xsl:with-param name="by" select="$by" />
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="$text" />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:template>

    <!-- Iterate over the <tdl:issue> tags and group using the Muenchian method -->
    <xsl:template match="tdl:issue">
        <xsl:variable name="search_path" select="$document/dri:meta/dri:pageMeta/dri:metadata[@element='search' and @qualifier='simpleURL']" />
        <xsl:variable name="query_string" select="$document/dri:meta/dri:pageMeta/dri:metadata[@element='search' and @qualifier='queryField']" />
        <xsl:variable name="context_path" select="$document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']" />
        <xsl:variable name="collection_handle" select="substring-after($document/dri:meta/dri:pageMeta/dri:metadata[@element='focus' and @qualifier='container'], ':')" />
        <xsl:variable name="collection_title" select="$document/dri:meta/dri:pageMeta/dri:metadata[@element='title']" />
        
        <div class="journal-volume-group">
        
            <h2>
                <xsl:text>Volume </xsl:text>
                <xsl:value-of select="@vol" />
            </h2>
            <xsl:for-each select="key('issues-by-vol', @vol)">
                <xsl:variable name="numEncoded">
                  <xsl:call-template name="string-replace-all">
                    <xsl:with-param name="text" select="@num" />
                    <xsl:with-param name="replace" select="'&amp;'" />
                    <xsl:with-param name="by" select="'%26'" />
                  </xsl:call-template>
                </xsl:variable>

                <p>
                    <strong>
                        <xsl:text>Issues </xsl:text>
                        <xsl:value-of select="@num" />
                        <xsl:text> (</xsl:text>
                        <xsl:value-of select="@year" />
                        <xsl:text>)</xsl:text>
                        <xsl:if test="@name != ''">
                            <xsl:text> :: </xsl:text>
                            <xsl:value-of select="@name" />
                        </xsl:if>
                    </strong> <br />
                    <xsl:variable name="index"><xsl:if test="@index"><xsl:value-of select="@index"/></xsl:if>
                    <xsl:if test="not(@index)"><xsl:text>series</xsl:text></xsl:if></xsl:variable>
                    <xsl:element name="a">
                        <xsl:attribute name="href">
                            <xsl:value-of select="$context_path" />
                            <xsl:text>/handle/</xsl:text>
                            <xsl:value-of select="$collection_handle" />
                            <xsl:value-of select="$discoveryUrl" />
                            <xsl:text>?filtertype_1=Series&amp;filter_relational_operator_1=equals&amp;filter_1=</xsl:text>
                            <xsl:value-of select="$collection_title" />
                            <xsl:text>%3A+Vol.+</xsl:text>
                            <xsl:value-of select="@vol" />
                            <xsl:text>%2C+Nos.+</xsl:text>
                            <xsl:value-of select="$numEncoded" />
                        </xsl:attribute>
                        <xsl:text>Browse Issue</xsl:text>
                    </xsl:element>    
                     | <a href="{$context_path}/handle/{@handle}">Download Complete Issue</a>
                </p>
            </xsl:for-each>
        
        </div>
            
    </xsl:template>

    <!-- Hide the search box -->
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.CollectionViewer.div.collection-search-browse']" >
    </xsl:template>

    <!-- Hide the recent submissions list -->
                                     
    <xsl:template match="dri:div[@id='aspect.discovery.CollectionRecentSubmissions.div.collection-recent-submission']" >
    </xsl:template>

    <!-- Group of templates to hide the search forms when appropriate (if the "hidesearch" parameter is in the contextualized-search URL) -->
    <xsl:template match="dri:div[@n='general-query'][$hidesearch]" >
    </xsl:template>
    <xsl:template match="dri:p[@n='result-query'][$hidesearch]" >
    </xsl:template>
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.SimpleSearch.div.search'][$hidesearch]/dri:head" >
        <h1>
            <span class="header-insert">Browse Issue</span>
        </h1>
    </xsl:template>
    <xsl:template match="dri:div[@id='aspect.artifactbrowser.SimpleSearch.div.search-results'][$hidesearch]/dri:head">
    </xsl:template>

    <!-- We are overriding Mirage2/xsl/aspect/artifactbrowser/item-view.xsl to add the ispartofseries transform -->
    <xsl:template match="dim:dim" mode="itemSummaryView-DIM">
        <div class="item-summary-view-metadata">
            <xsl:call-template name="itemSummaryView-DIM-title"/>
            <div class="row">
                <div class="col-sm-4">
                    <div class="row">
                        <div class="col-xs-6 col-sm-12">
                            <xsl:call-template name="itemSummaryView-DIM-thumbnail"/>
                        </div>
                        <div class="col-xs-6 col-sm-12">
                            <xsl:call-template name="itemSummaryView-DIM-file-section"/>
                        </div>
                    </div>
                    <xsl:call-template name="itemSummaryView-DIM-date"/>
                    <xsl:call-template name="itemSummaryView-DIM-authors"/>
                    <xsl:if test="$ds_item_view_toggle_url != ''">
                        <xsl:call-template name="itemSummaryView-show-full"/>
                    </xsl:if>
                </div>
                <div class="col-sm-8">
                    <xsl:call-template name="itemSummaryView-DIM-abstract"/>
                    <xsl:call-template name="itemSummaryView-DIM-URI"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-ispartofseries" />
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-description" />
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-subject" />
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-department" />
                    <xsl:call-template name="itemSummaryView-collections"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-dwc"/>
                    <!-- TAMU Customization -->
                    <xsl:call-template name="itemSummaryView-DIM-citation"/>
                </div>
            </div>
        </div>
    </xsl:template>

    <!-- TAMU Customization -->
    <xsl:template name="itemSummaryView-DIM-ispartofseries">
        <xsl:if test="dim:field[@element='relation' and @qualifier='ispartofseries']">
            <h5><i18n:text>Issue</i18n:text></h5>
            <div class="simple-item-view-ispartofseries word-break item-page-field-wrapper table">
                <xsl:for-each select="dim:field[@element='relation' and @qualifier='ispartofseries']">
                    <xsl:copy-of select="./node()"/>
                    <xsl:if test="count(following-sibling::dim:field[@element='date' and @qualifier='issued']) != 0">
                    <br/>
                    </xsl:if>
                </xsl:for-each>
            </div>
        </xsl:if>
    </xsl:template>
</xsl:stylesheet>