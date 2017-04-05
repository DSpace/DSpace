<?xml version="1.0" encoding="UTF-8"?>

<!-- This stylesheet controls the view/display of the item pages (package
	and file). -->

<!-- If you use an XML editor to reformat this page make sure that the i18n:text
	elements do not break across separate lines; the text will fail to be internationalized
	if this happens and the i18n text is what will be displayed on the Web page. -->

<xsl:stylesheet xmlns="http://www.w3.org/1999/xhtml"
                xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/" xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xalan="http://xml.apache.org/xalan" xmlns:datetime="http://exslt.org/dates-and-times"
                xmlns:encoder="xalan://java.net.URLEncoder" exclude-result-prefixes="xalan strings encoder datetime"
                version="1.0" xmlns:strings="http://exslt.org/strings"
                xmlns:confman="org.dspace.core.ConfigurationManager">

    <xsl:import href="DryadUtils.xsl"/>
    <xsl:output method="xml" version="1.0" encoding="utf-8" indent="yes"/>

    <xsl:variable name="meta"
                  select="/dri:document/dri:meta/dri:pageMeta/dri:metadata"/>
    <xsl:variable name="localize"
                  select="$meta[@element='dryad'][@qualifier='localize'][.='true']"/>

    <xsl:variable name="versionNotice"
                  select="/dri:document/dri:body//dri:div[@id='org.datadryad.dspace.xmlui.aspect.browse.ItemViewer.div.notice'][@rend='notice']"/>
    <xsl:variable name="latestDataVersion"
                  select="/dri:document/dri:body/dri:div[@id='aspect.versioning.VersionHistoryForm.div.view-verion-history']/dri:table/dri:row[2]/dri:cell[1]"/>
    <xsl:variable name="embeddedViewReferenceSet" 
                  select="//dri:referenceSet[@type='embeddedView']"/>

    <xsl:template name="itemSummaryView-DIM">
        <xsl:variable name="datafiles"
                      select=".//dim:field[@element='relation'][@qualifier='haspart']"/>

        <!-- my_doi and my_uri go together; there is a my_uri if no my_doi -->
        <xsl:variable name="my_doi"
                      select=".//dim:field[@element='identifier'][not(@qualifier)][starts-with(., 'doi:')]"/>
        <xsl:variable name="my_full_doi"
                      select=".//dim:field[@element='identifier'][not(@qualifier)][starts-with(., 'http://dx.doi')]"/>
        <xsl:variable name="my_uri"
                      select=".//dim:field[@element='identifier'][@qualifier='uri'][not(starts-with(., 'doi'))][not(starts-with(., 'http://dx.doi'))]"/>

        <!--<h1>Non doi test: <xsl:value-of select="$doi_redirect"/></h1> -->

        <!-- Obtain an identifier if the item is harvested from KNB. But we have
              to munge the URL to link to LTER instead of the raw XML. -->
        <xsl:variable name="knb_url_raw"
                      select=".//dim:field[@element='identifier'][starts-with(.,'http://metacat')]"/>
        <xsl:variable name="knb_url">
            <xsl:if test="$knb_url_raw!=''">
                <xsl:value-of
                        select="substring($knb_url_raw,0,string-length($knb_url_raw)-2)"/>
                <xsl:text>lter</xsl:text>
            </xsl:if>
        </xsl:variable>

        <!-- Obtain an identifier if the item is harvested from TreeBASE. But we
              have to munge the URL to link to TreeBASE instead of raw XML. -->
        <xsl:variable name="treebase_url_raw"
                      select=".//dim:field[@element='identifier'][contains(., 'purl.org/phylo/treebase/')]"/>
        <xsl:variable name="treebase_url">
            <xsl:if test="$treebase_url_raw != ''">
                <xsl:choose>
                    <xsl:when test="starts-with(., 'http:')">
                        <xsl:value-of select="concat($treebase_url_raw, '?format=html')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of
                                select="concat('http://', $treebase_url_raw, '?format=html')"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>
        </xsl:variable>


        <xsl:variable name="title"
                      select=".//dim:field[@element='title']/node()"/>

        <!-- publication header -->
        <div class="publication-header">
            <xsl:call-template name="journal-lookup">
                <xsl:with-param name="fullname" select=".//dim:field[@element='publicationName']"/>
                <xsl:with-param name="article-doi"
                                select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi:')]"/>
                <xsl:with-param name="cover" select="$meta[@element='journal'][@qualifier='cover']"/>
                <xsl:with-param name="website" select="$meta[@element='journal'][@qualifier='website']"/>
            </xsl:call-template>
            <p class="pub-title">
                <xsl:value-of select="$title"/>
            </p>
            <p class="pub-authors">
                <xsl:call-template name="make-author-string"/>
            </p>
            <xsl:if test=".//dim:field[@element='date' and @qualifier='accessioned']">
                <p>
                    <i18n:text>xmlui.DryadItemSummary.depDate</i18n:text><xsl:text>: </xsl:text>
                    <xsl:call-template name="format-date">
                        <xsl:with-param name="datetime"><xsl:value-of select=".//dim:field[@element='date' and @qualifier='accessioned']"/></xsl:with-param>
                    </xsl:call-template>
                </p>
            </xsl:if>
            <xsl:if test="$my_doi != ''">
                <p>
                    <i18n:text>xmlui.DryadItemSummary.dryadPkgID</i18n:text>
                    <xsl:text>: </xsl:text>
                    <xsl:value-of select="concat('http://dx.doi.org/', substring-after($my_doi, 'doi:'))"/>
                </p>
            </xsl:if>
            <xsl:if test="$treebase_url != ''">
                <p>
                    <i18n:text>xmlui.DryadItemSummary.viewContentTB</i18n:text>
                    <xsl:text>: </xsl:text>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$treebase_url"/>
                        </xsl:attribute>
                        <xsl:value-of select="$treebase_url"/>
                    </a>
                </p>
            </xsl:if>
            <xsl:if test="$knb_url != ''">
                <p>
                    <i18n:text>xmlui.DryadItemSummary.viewContentKNB</i18n:text>
                    <xsl:text>: </xsl:text>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="$knb_url"/>
                        </xsl:attribute>
                        <xsl:value-of select="$knb_url"/>
                    </a>
                </p>
            </xsl:if>

        </div>
        <xsl:if test=".//dim:field[@element='curatorNotePublic']">
            <div class="ds-static-div primary">
                <p class="ds-paragraph">
                    <xsl:value-of select=".//dim:field[@element='curatorNotePublic']"/>
                </p>
            </div>
        </xsl:if>
        <!-- Data Files in package -->
        <xsl:if test="$datafiles">
            <div class="ds-static-div primary">&#160;
                <xsl:apply-templates select="$embeddedViewReferenceSet"/>
            </div>
        </xsl:if>
        <!-- citing -->
        <!-- CITATION FOR DATA FILE -->
        <!-- Citation for the data file is different from the citation for the
              data package because for the file we pull metadata elements from the metadata
              we've put into the DSpace page metadata; this is put there by the ItemViewer
              java class in the XMLUI tree. If something breaks in the display of the data
              file, it may be a problem with the information not being put into the proper
              page metadata slots in the org.datadryad.dspace.xmlui.aspect.browse.ItemViewer
              class -->
        <xsl:if
                test="$meta[@element='xhtml_head_item'][contains(., 'DCTERMS.isPartOf')]
				and $meta[@element='request'][@qualifier='queryString'][not(contains(., 'show=full'))]
				and $meta[@element='authors'][@qualifier='package']">

            <div class="ds-static-div primary">
                    <div class="secondary">
                    <p class="ds-paragraph">
                        <i18n:text>xmlui.DryadItemSummary.whenUsing</i18n:text>
                    </p>
                    <div class="citation-sample">
                        <xsl:call-template name="publication-citation">
                            <xsl:with-param name="citation" select="$meta[@element='citation'][@qualifier='article']"/>
                            <xsl:with-param name="article_doi" select="$meta[@element='identifier'][@qualifier='article'][. != '']"/>
                            <xsl:with-param name="article_pmid"/>
                            <xsl:with-param name="article_id"/>
                            <xsl:with-param name="journal" select="$meta[@element='publicationName']"/>
                        </xsl:call-template>
                    </div>
                    <p class="ds-paragraph">
                        <i18n:text>xmlui.DryadItemSummary.pleaseCite</i18n:text>
                    </p>
                    <div class="citation-sample">
                        <xsl:call-template name="make-author-string"/>
                        <xsl:call-template name="package-citation">
                            <xsl:with-param name="package_doi">
                                <xsl:call-template name="package-doi">
                                    <xsl:with-param name="id" select="$meta[@element='identifier'][@qualifier='package']"/>
                                </xsl:call-template>
                            </xsl:with-param>
                            <xsl:with-param name="date">
                                <xsl:choose>
                                    <xsl:when test="$meta[@element='date'][@qualifier='issued']">
                                        <xsl:value-of select="$meta[@element='date'][@qualifier='issued']"/>
                                    </xsl:when>
                                    <xsl:when test="$meta[@element='dateIssued'][@qualifier='package']">
                                        <xsl:value-of select="$meta[@element='dateIssued'][@qualifier='package']"/>
                                    </xsl:when>
                                </xsl:choose>
                            </xsl:with-param>
                            <xsl:with-param name="title" select="$meta[@element='title'][@qualifier='package']"/>
                            <xsl:with-param name="date_accessioned" select="/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='dc'][@element='date' ][@qualifier='accessioned']"/>
                        </xsl:call-template>
                    </div>
                    <!-- only show citation/share if viewing from page with real handle (not in process) -->
                    <xsl:if
                            test="$meta[@element='request'][@qualifier='URI'][contains(.,'handle') or contains(.,'resource')]">
                        <xsl:variable name="pkgDOI"
                                      select="$meta[@element='identifier'][@qualifier='package']"/>
                        <!-- Here we give links to expost the citation and share options available
                              on each item record. The citation links link to a servlet in the DOI module
                              (a Dryad/DSpace module in the modules directory). When a DOI is passed as
                              a parameter in the URL to that servlet, the citation metadata is looked up
                              in DSpace and formatted for download from this link. -->
                        <div align="right" style="padding-right: 20px; padding-bottom: 5px;">
                            <a href="/cite" id="cite" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.cite</i18n:text>
                            </a>
                            <xsl:text>  |  </xsl:text>
                            <a href="/share" id="share" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.share</i18n:text>
                            </a>
                            <div id="citemediv">
                                <table style="width: 100%;">
                                    <tr>
                                        <td align="left" style="text-decoration: underline;">
                                            <i18n:text>xmlui.DryadItemSummary.downloadFormats</i18n:text>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            &#xa0;&#xa0;
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <xsl:value-of select="concat('/resource/', $pkgDOI, '/citation/ris')"/>
                                                </xsl:attribute>
                                                <xsl:text>RIS </xsl:text>
                                            </xsl:element>
                                            <span class="italics">
                                                <i18n:text>xmlui.DryadItemSummary.risCompatible</i18n:text>
                                            </span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            &#xa0;&#xa0;
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <xsl:value-of select="concat('/resource/', $pkgDOI, '/citation/bib')"/>
                                                </xsl:attribute>
                                                <xsl:text>BibTex </xsl:text>
                                            </xsl:element>
                                            <span class="italics">
                                                <i18n:text>xmlui.DryadItemSummary.bibtexCompatible</i18n:text>
                                            </span>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                            <!-- The sharemediv has the links that allow sharing a Dryad data package
                                   on the various social software sites. We've used a per-site approach, using
                                   the method suggested on each of these sites, but there are all-in-one services
                                   that will provide a common way to share that might be worth looking into.
                                   One of the things we've seen with some of these is that some of them take
                                   awhile to load (causing the page to take a while to complete loading, though
                                   the display of the page is pretty instantaneous). An all-in-one approach
                                   might be quicker(?) in this respect. Where possible, we use the DOI as what
                                   we pass to these other services. -->
                            <div id="sharemediv">
                                <xsl:variable name="thispage"
                                              select="concat($meta[@element='request'][@qualifier='scheme'],
									'%3A%2F%2F',
									$meta[@element='request'][@qualifier='serverName'], ':',
									$meta[@element='request'][@qualifier='serverPort'], '%2F',
									encoder:encode($meta[@element='request'][@qualifier='URI']))"/>
                                <xsl:variable name="thistitle"
                                              select="encoder:encode($meta[@element='title'][@qualifier='package'])"/>
                                <xsl:variable name="apos">
                                    '
                                </xsl:variable>
                                <!-- for building JavaScript -->
                                <table style="width: 100%;">
                                    <tr>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:variable name="dfirstpart">
                                                    window.open('http://www.delicious.com/save?v=5&amp;noui&amp;jump=close&amp;url='+encodeURIComponent(
                                                </xsl:variable>
                                                <xsl:variable name="dsecondpart">)+'&amp;title=
                                                </xsl:variable>
                                                <xsl:variable name="dthirdpart">
                                                    ',
                                                    'delicious','toolbar=no,width=550,height=550'); return
                                                    false;
                                                </xsl:variable>
                                                <xsl:attribute name="href">http://www.delicious.com/save</xsl:attribute>
                                                <xsl:attribute name="onclick">
                                                    <xsl:value-of
                                                            select="concat($dfirstpart, $apos, 'http://dx.doi.org/', $pkgDOI, $apos, $dsecondpart,
														$thistitle, $dthirdpart)"/>
                                                </xsl:attribute>
                                                <img src="//delicious.com/img/logo.png"
                                                     height="16" width="16" alt="Delicious"
                                                     style="border: 1px solid #ccc;"/>
                                            </xsl:element>
                                            <!-- xsl:text is a workaround for formatting issues -->
                                            <script type="text/javascript" src="/themes/Dryad/lib/delicious.js">
                                                <xsl:text> </xsl:text>
                                            </script>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="class">DiggThisButton DiggCompact</xsl:attribute>
                                                <xsl:attribute name="href">
                                                    <xsl:value-of
                                                            select="concat('http://digg.com/submit?url=http://dx.doi.org/', $pkgDOI,
														'&amp;title=', $thistitle)"/>
                                                </xsl:attribute>
                                                <!-- xsl:text is a work around for formatting issues -->
                                                <xsl:text> </xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:variable name="rfirstpart">
                                                    window.open('http://reddit.com/submit?url='+encodeURIComponent(
                                                </xsl:variable>
                                                <xsl:variable name="rsecondpart">)+'&amp;title=
                                                </xsl:variable>
                                                <xsl:variable name="rthirdpart">
                                                    ',
                                                    'reddit','toolbar=no,width=550,height=550'); return false
                                                </xsl:variable>
                                                <xsl:attribute name="href">http://reddit.com/submit</xsl:attribute>
                                                <xsl:attribute name="onclick">
                                                    <xsl:value-of
                                                            select="concat($rfirstpart, $apos, 'http://dx.doi.org/', $pkgDOI, $apos, $rsecondpart,
														$thistitle, $rthirdpart)"/>
                                                </xsl:attribute>
                                                <img border="0px;" src="http://reddit.com/static/spreddit7.gif"
                                                     alt="Reddit"/>
                                                <!-- xsl:text is a work around for formatting issues -->
                                                <xsl:text> </xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">http://twitter.com/share</xsl:attribute>
                                                <xsl:attribute name="class">twitter-share-button</xsl:attribute>
                                                <xsl:attribute name="data-count">none</xsl:attribute>
                                                <xsl:attribute name="data-via">datadryad</xsl:attribute>
                                                <xsl:attribute name="data-url">
                                                    <xsl:value-of select="concat('http://dx.doi.org/', $pkgDOI)"/>
                                                </xsl:attribute>
                                                <xsl:text>Tweet</xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="iframe">
                                                <xsl:attribute name="src">
                                                    <xsl:value-of
                                                            select="concat('http://www.facebook.com/plugins/like.php?href=',
														encoder:encode(concat('http://dx.doi.org/', $pkgDOI)),
														'&amp;layout=button_count&amp;show_faces=false&amp;width=100&amp;action=like&amp;colorscheme=light&amp;height=21')"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="scrolling">no</xsl:attribute>
                                                <xsl:attribute name="frameborder">0</xsl:attribute>
                                                <xsl:attribute name="style">border:none; overflow:hidden; width:70px;
                                                    height:21px;
                                                </xsl:attribute>
                                                <xsl:attribute name="allowTransparency">true</xsl:attribute>
                                                <xsl:text> </xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <xsl:value-of
                                                            select="concat('http://www.mendeley.com/import/?url=http://datadryad.org/resource/',
                                                                       $pkgDOI)" />
                                                </xsl:attribute>
                                                <img border="0px;" src="http://www.mendeley.com/graphics/mendeley.png" alt="Mendeley"/>
                                            </xsl:element>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                        </div>
                    </xsl:if>
                </div>
            </div>
        </xsl:if>        
        <!-- CITATION FOR DATA PACKAGE -->
        <xsl:if
                test="not($meta[@element='xhtml_head_item'][contains(., 'DCTERMS.isPartOf')]) and .//dim:field[@element='relation'][@qualifier='haspart']">
            <div class="ds-static-div primary">
                    <div class="secondary">
                    <p class="ds-paragraph">
                        <i18n:text>xmlui.DryadItemSummary.whenUsing</i18n:text>
                    </p>
                    <div class="citation-sample">
                        <xsl:call-template name="publication-citation">
                            <xsl:with-param name="citation" select=".//dim:field[@element='identifier'][@qualifier='citation'][position() = 1]"/>
                            <xsl:with-param name="article_doi" select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi:')]"/>
                            <xsl:with-param name="article_pmid" select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'PMID:')]"/>
                            <xsl:with-param name="article_id" select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][not(starts-with(., 'doi:')) and not(starts-with(.,'PMID:'))]"/>
                            <xsl:with-param name="journal" select=".//dim:field[@element='publicationName']"/>
                        </xsl:call-template>
                    </div>
                    <xsl:if test="$datafiles">
                        <p class="ds-paragraph">
                            <i18n:text>xmlui.DryadItemSummary.pleaseCite</i18n:text>
                        </p>
                        <div class="citation-sample">
                            <xsl:call-template name="make-author-string"/>
                            <xsl:call-template name="package-citation">
                                <xsl:with-param name="date" select=".//dim:field[@element='date'][@qualifier='issued']"/>
                                <xsl:with-param name="title" select=".//dim:field[@element='title']"/>
                                <xsl:with-param name="date_accessioned" select="/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='dc'][@element='date' ][@qualifier='accessioned']"/>
                                <xsl:with-param name="package_doi">
                                    <xsl:choose>
                                        <xsl:when test="$my_doi">
                                            <xsl:value-of
                                                    select="concat('http://dx.doi.org/', substring-after($my_doi, 'doi:'))"/>
                                        </xsl:when>
                                        <xsl:when test="$my_full_doi">
                                            <xsl:value-of select="$my_full_doi"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="$my_uri"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:with-param>
                            </xsl:call-template>
                       </div>
                    </xsl:if>
                    <!-- only show citation/share if viewing from page with real handle (not in process) -->
                    <xsl:if
                            test="$meta[@element='request'][@qualifier='URI'][contains(.,'handle') or contains(.,'resource')]">
                        <div align="right" style="padding-right: 20px; padding-bottom: 5px;">
                            <a href="/cite" id="cite" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.cite</i18n:text>
                            </a>
                            <xsl:text>  |  </xsl:text>
                            <a href="/share" id="share" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.share</i18n:text>
                            </a>
                            <div id="citemediv">
                                <table style="width: 100%;">
                                    <tr>
                                        <td align="left" style="text-decoration: underline;">
                                            <i18n:text>xmlui.DryadItemSummary.downloadFormats</i18n:text>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            &#xa0;&#xa0;
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <xsl:value-of select="concat('/resource/', $my_doi, '/citation/ris')"/>
                                                </xsl:attribute>
                                                <xsl:text>RIS </xsl:text>
                                            </xsl:element>
                                            <span class="italics">
                                                <i18n:text>xmlui.DryadItemSummary.risCompatible</i18n:text>
                                            </span>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td>
                                            &#xa0;&#xa0;
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <xsl:value-of select="concat('/resource/', $my_doi, '/citation/bib')"/>
                                                </xsl:attribute>
                                                <xsl:text>BibTex </xsl:text>
                                            </xsl:element>
                                            <span class="italics">
                                                <i18n:text>xmlui.DryadItemSummary.bibtexCompatible</i18n:text>
                                            </span>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                            <div id="sharemediv">
                                <xsl:variable name="thispage"
                                              select="concat($meta[@element='request'][@qualifier='scheme'],
							'%3A%2F%2F',
							$meta[@element='request'][@qualifier='serverName'], ':',
							$meta[@element='request'][@qualifier='serverPort'], '%2F',
							encoder:encode($meta[@element='request'][@qualifier='URI']))"/>
                                <xsl:variable name="thistitle"
                                              select="encoder:encode($meta[@element='title'])"/>
                                <xsl:variable name="apos">
                                    '
                                </xsl:variable>
                                <!-- for building JavaScript -->
                                <table style="width: 100%;">
                                    <tr>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:variable name="dfirstpart">
                                                    window.open('http://www.delicious.com/save?v=5&amp;noui&amp;jump=close&amp;url='+encodeURIComponent(
                                                </xsl:variable>
                                                <xsl:variable name="dsecondpart">)+'&amp;title='+encodeURIComponent(document.title),
                                                    'delicious','toolbar=no,width=550,height=550'); return
                                                    false;
                                                </xsl:variable>
                                                <xsl:attribute name="href">http://www.delicious.com/save</xsl:attribute>
                                                <xsl:attribute name="onclick">
                                                    <xsl:value-of
                                                            select="concat($dfirstpart, $apos, 'http://dx.doi.org/', $my_doi, $apos, $dsecondpart)"/>
                                                </xsl:attribute>
                                                <img src="//delicious.com/img/logo.png"
                                                     height="16" width="16" alt="Delicious"
                                                     style="border: 1px solid #ccc;"/>
                                            </xsl:element>
                                            <!-- xsl:text is a workaround for formatting issues -->
                                            <script type="text/javascript" src="/themes/Dryad/lib/delicious.js">
                                                <xsl:text> </xsl:text>
                                            </script>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="class">DiggThisButton DiggCompact</xsl:attribute>
                                                <xsl:attribute name="href">
                                                    <xsl:value-of
                                                            select="concat('http://digg.com/submit?url=http://dx.doi.org/', $my_doi,
												'&amp;title=', $thistitle)"/>
                                                </xsl:attribute>
                                                <!-- xsl:text is a work around for formatting issues -->
                                                <xsl:text> </xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:variable name="rfirstpart">
                                                    window.open('http://reddit.com/submit?url='+encodeURIComponent(
                                                </xsl:variable>
                                                <xsl:variable name="rsecondpart">)+'&amp;title=
                                                </xsl:variable>
                                                <xsl:variable name="rthirdpart">
                                                    ',
                                                    'reddit','toolbar=no,width=550,height=550'); return false
                                                </xsl:variable>
                                                <xsl:attribute name="href">http://reddit.com/submit</xsl:attribute>
                                                <xsl:attribute name="onclick">
                                                    <xsl:value-of
                                                            select="concat($rfirstpart, $apos, 'http://dx.doi.org/', $my_doi, $apos, $rsecondpart,
												$thistitle, $rthirdpart)"/>
                                                </xsl:attribute>
                                                <img border="0px;" src="http://reddit.com/static/spreddit7.gif"
                                                     alt="Reddit"/>
                                                <!-- xsl:text is a work around for formatting issues -->
                                                <xsl:text> </xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">http://twitter.com/share</xsl:attribute>
                                                <xsl:attribute name="class">twitter-share-button</xsl:attribute>
                                                <xsl:attribute name="data-count">none</xsl:attribute>
                                                <xsl:attribute name="data-via">datadryad</xsl:attribute>
                                                <xsl:attribute name="data-url">
                                                    <xsl:value-of select="concat('http://dx.doi.org/', $my_doi)"/>
                                                </xsl:attribute>
                                                <xsl:text>Tweet</xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="iframe">
                                                <xsl:attribute name="src">
                                                    <xsl:value-of
                                                            select="concat('http://www.facebook.com/plugins/like.php?href=',
													encoder:encode(concat('http://dx.doi.org/', $my_doi)),
												'&amp;layout=button_count&amp;show_faces=false&amp;width=100&amp;action=like&amp;colorscheme=light&amp;height=21')"/>
                                                </xsl:attribute>
                                                <xsl:attribute name="scrolling">no</xsl:attribute>
                                                <xsl:attribute name="frameborder">0</xsl:attribute>
                                                <xsl:attribute name="style">border:none; overflow:hidden; width:70px;
                                                    height:21px;
                                                </xsl:attribute>
                                                <xsl:attribute name="allowTransparency">true</xsl:attribute>
                                                <xsl:text> </xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <xsl:value-of
                                                            select="concat('http://www.mendeley.com/import/?url=http://datadryad.org/resource/',
                                                            $my_doi)" />
                                                </xsl:attribute>
                                                <img border="0px;" src="http://www.mendeley.com/graphics/mendeley.png" alt="Mendeley"/>
                                            </xsl:element>
                                        </td>
                                    </tr>
                                </table>
                            </div>
                        </div>
                    </xsl:if>
                </div>
            </div>
        </xsl:if>
        <!-- package metadata -->
        <div class="ds-static-div primary">
            <div class="item-summary-view-metadata">
                <table class="package-metadata">
                    <tbody>
                        <xsl:variable name="funder" select="$meta[@element='dryad'][@qualifier='fundingEntity']"/>
                        <xsl:if test="$funder">
                            <tr>
                                <xsl:call-template name="make-funding-string">
                                    <xsl:with-param name="InputString" select="$funder"/>
                                </xsl:call-template>
                            </tr>
                        </xsl:if>
                    <xsl:variable name="pageviews"
                                  select="$meta[@element='dryad'][@qualifier='pageviews']"/>
                    <xsl:if test="$pageviews > 0">
		      <tr>
			<th><i18n:text>xmlui.DryadItemSummary.views</i18n:text></th>
			<td>
			  <xsl:value-of select="$pageviews" />
			</td>
		      </tr>
                    </xsl:if>
                    <xsl:variable name="downloads"  select="$meta[@element='dryad'][@qualifier='downloads']"/>
                    <xsl:if test="$downloads > 0">
		      <tr>
			<th><i18n:text>xmlui.DryadItemSummary.downloads</i18n:text></th>
			<td>
			  <xsl:value-of select="$downloads" />
			  <xsl:choose>
			    <xsl:when test="$downloads='1'"> time</xsl:when>
			    <xsl:otherwise> times</xsl:otherwise>
			  </xsl:choose>
			</td>
		      </tr>
                    </xsl:if>

                        <span class="Z3988" style="display:none;">
                            <xsl:attribute name="title">
                                <xsl:call-template name="renderCOinS"/>
                            </xsl:attribute>
                            <xsl:text>&#160;</xsl:text>
                        </span>

                        <!-- End of identifier -->

            <!-- Need to add spatial, temporal, taxonomic keywords from file metadata -->
            <xsl:if test=".//dim:field[@element='subject'][@mdschema='dc'][not(@qualifier)]">
                <tr>
                    <th>
                        <i18n:text>xmlui.DryadItemSummary.keywords</i18n:text>
                    </th>
                    <td>
                        <xsl:for-each select=".//dim:field[@element='subject'][@mdschema='dc'][not(@qualifier)]">
                            <xsl:variable name="keyword" select="."/>
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="concat('/discover?query=&amp;submit=Go&amp;fq=dc.subject%3A', translate($keyword,' ','+'), '&amp;filtertype=*&amp;filter=&amp;rpp=20&amp;sort_by=score&amp;order=DESC')"/>
                                </xsl:attribute>
                                <xsl:value-of select="$keyword"/>
                            </a>
                            <xsl:if test="position() != last()">
                                <xsl:text>, </xsl:text>
                            </xsl:if>                            
                        </xsl:for-each>
                    </td>
                    <td></td>
                </tr>
            </xsl:if>

            <xsl:variable name="sciNames">
                <xsl:for-each select=".//dim:field[@element='ScientificName']">
                    <xsl:value-of select="node()"/>
                    <xsl:if test="position() != last()">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$sciNames!=''">
                <tr>
                    <th>
                        <i18n:text>xmlui.DryadItemSummary.sciNames</i18n:text>
                    </th>
                    <td>
                        <xsl:value-of select="$sciNames"/>
                    </td>
                </tr>
            </xsl:if>

            <xsl:variable name="spatialCoverage">
                <xsl:for-each
                        select=".//dim:field[@element='coverage'][@qualifier='spatial']">
                    <xsl:value-of select="node()"/>
                    <xsl:if test="position() != last()">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$spatialCoverage!=''">
                <tr>
                    <th>
                        <i18n:text>xmlui.DryadItemSummary.spatialCov</i18n:text>
                    </th>
                    <td>
                        <xsl:value-of select="$spatialCoverage"/>
                    </td>
                </tr>
            </xsl:if>

            <xsl:variable name="temporalCoverage">
                <xsl:for-each
                        select=".//dim:field[@element='coverage'][@qualifier='temporal']">
                    <xsl:value-of select="node()"/>
                    <xsl:if test="position() != last()">
                        <xsl:text>, </xsl:text>
                    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$temporalCoverage!=''">
                <tr>
                    <th>
                        <i18n:text>xmlui.DryadItemSummary.temporalCov</i18n:text>
                    </th>
                    <td>
                        <xsl:value-of select="$temporalCoverage"/>
                    </td>
                </tr>
            </xsl:if>

            <xsl:if
                    test=".//dim:field[@element='identifier'][not(@qualifier)][not(contains(., 'dryad.'))]">

                <xsl:variable name="dc-creators"
                              select=".//dim:field[@element='creator'][@mdschema='dc']"/>

                <xsl:if test="$dc-creators != ''">
                    <tr>
                        <th>
                            <xsl:choose>
                                <xsl:when test="count($dc-creators) &gt; 1">
                                    <i18n:text>xmlui.DryadItemSummary.authors</i18n:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <i18n:text>xmlui.DryadItemSummary.author</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </th>
                        <td>
                            <xsl:for-each select="$dc-creators">
                                <xsl:value-of select="."/>
                                <br/>
                            </xsl:for-each>
                        </td>
                    </tr>
                </xsl:if>

                <xsl:variable name="dc-publishers"
                              select=".//dim:field[@element='publisher'][@mdschema='dc']"/>

                <xsl:if test="$dc-publishers != ''">
                    <tr>
                        <th>
                            <xsl:choose>
                                <xsl:when test="count($dc-publishers) &gt; 1">
                                    <i18n:text>xmlui.DryadItemSummary.publishers</i18n:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <i18n:text>xmlui.DryadItemSummary.publisher</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </th>
                        <td>
                            <xsl:for-each select="$dc-publishers">
                                <xsl:value-of select="."/>
                                <br/>
                            </xsl:for-each>
                        </td>
                    </tr>
                </xsl:if>
            </xsl:if>

            <xsl:variable name="dc-date"
                          select=".//dim:field[@element='date'][not(@qualifier)][@mdschema='dc']"/>

            <xsl:if test="$dc-date != ''">
                <tr>
                    <th>
                        <i18n:text>xmlui.DryadItemSummary.published</i18n:text>
                    </th>
                    <td>
                        <xsl:value-of select="$dc-date[1]"/>
                    </td>
                </tr>
            </xsl:if>

            <xsl:variable name="externalDataSets">
                <xsl:for-each
                        select=".//dim:field[@element='relation' and @qualifier='external'][contains(., 'treebase')]">
                    <span>
                        <i18n:text>xmlui.DryadItemSummary.treebaseLabel</i18n:text>
                    </span>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="."/>
                        </xsl:attribute>
                        <xsl:value-of select="."/>
                    </a>
                    <br/>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$externalDataSets!=''">
                <tr>
                    <th>
                        <i18n:text>xmlui.DryadItemSummary.otherRepos</i18n:text>
                    </th>
                    <td>
                        <xsl:value-of select="$externalDataSets"/>
                    </td>
                    <td>
                    </td>
                </tr>
            </xsl:if>

            <xsl:variable name="describedBy">
                <xsl:for-each
                        select=".//dim:field[@element='relation' and @qualifier='ispartof']">
                    <a>
                        <xsl:attribute name="href">
                            <xsl:call-template name="checkURL">
                                <xsl:with-param name="doiIdentifier" select="$my_doi"/>
                            </xsl:call-template>
                        </xsl:attribute>
                        <xsl:choose>
                            <xsl:when test="$meta[@element='title'][@qualifier='package']">
                                <xsl:value-of select="$meta[@element='title'][@qualifier='package']"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="."/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </a>
                    <br/>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$describedBy!=''">
                <tr>
                    <th>
                        <i18n:text>xmlui.DryadItemSummary.containedInPackage</i18n:text>
                    </th>
                    <td>
                        <xsl:value-of select="$describedBy"/>
                    </td>
                </tr>
            </xsl:if>


            <!--		<xsl:if
                   test=".//dim:field[@element='identifier'][not(@qualifier)][contains(., 'dryad.')]">
                   <tr class="ds-table-row">
                       <td>
                           <span class="bold">
                               <i18n:text>xmlui.DryadItemSummary.depDate</i18n:text>
                           </span>
                       </td>
                       <td>
                           <xsl:copy-of
                               select=".//dim:field[@element='date' and @qualifier='accessioned']" />
                       </td>
                       <td>
                       </td>
                   </tr>
               </xsl:if>  -->
        <!-- abstract goes here -->
            <xsl:variable name="description">
                <xsl:for-each
                        select=".//dim:field[@element='description'][not(@qualifier='provenance')]">
                    <xsl:value-of select="node()"/>
                    <br/>
                </xsl:for-each>
            </xsl:variable>


            <xsl:if test="$description!=''">
                <tr>
                    <td colspan="2">
                        <!-- Display "Description" for data files and "Abstract" for data packages. -->
                        <xsl:choose>
                            <xsl:when
                                    test=".//dim:field[@element='relation'][@qualifier='ispartof']">
                                <div class="article-abstract"><b><i18n:text>xmlui.DryadItemSummary.description</i18n:text></b><br/>
                                  <xsl:value-of select="$description"/>
                                </div>
                            </xsl:when>
                            <xsl:otherwise>
                                <div class="article-abstract"><b><i18n:text>xmlui.DryadItemSummary.abstract</i18n:text></b><br/>
                                  <xsl:value-of select="$description"/>
                                </div>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                </tr>
            </xsl:if>


                <!-- payment goes here -->
                <xsl:variable name="payment">
                    <xsl:for-each
                            select=".//dim:field[@element='payment'][@qualifier='charge'][@mdschema='internal']">
                        <xsl:value-of select="node()"/>
                        <br/>
                    </xsl:for-each>
                </xsl:variable>

                <xsl:if test="$payment!=''">
                    <tr>
                        <th><i18n:text>xmlui.DryadItemSummary.charge</i18n:text></th>
                        <td colspan="2">
                            <!-- Display "Description" for data files and "Abstract" for data packages. -->
                            <xsl:value-of select="$payment"/>
                        </td>
                    </tr>
                </xsl:if>

                <xsl:variable name="transaction">
                    <xsl:for-each
                            select=".//dim:field[@element='payment'][@qualifier='transactionID'][@mdschema='internal']">
                        <xsl:value-of select="node()"/>
                        <br/>
                    </xsl:for-each>
                </xsl:variable>

                <xsl:if test="$transaction!=''">
                    <tr>
                        <th><i18n:text>xmlui.DryadItemSummary.transactionId</i18n:text></th>
                        <td colspan="2">
                            <!-- Display "Description" for data files and "Abstract" for data packages. -->
                            <xsl:value-of select="$transaction"/>
                        </td>
                    </tr>
                </xsl:if>
        </tbody>
        </table>
        

          </div>
        </div>
  
        <!-- we only want this view from item view - not the administrative pages -->
        <xsl:if test="$meta[@qualifier='URI' and contains(.., 'handle') and not(contains(..,'workflow'))]">
            <div style="padding: 10px; margin-top: 5px; margin-bottom: 5px;">
                <a href="?show=full">
                    <i18n:text>xmlui.DryadItemSummary.showFull</i18n:text>
                </a>
            </div>
        </xsl:if>

        <xsl:variable name="embargoedDate"
                      select=".//dim:field[@element='date' and @qualifier='embargoedUntil']"/>
        <xsl:variable name="embargoType">
            <xsl:choose>
                <xsl:when test=".//dim:field[@element='type' and @qualifier='embargo']">
                    <xsl:value-of
                            select=".//dim:field[@element='type' and @qualifier='embargo']"/>
                </xsl:when>
                <xsl:otherwise>
                    <i18n:text>xmlui.DryadItemSummary.unknown</i18n:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
                <xsl:call-template name="checkedAndNoEmbargo"/>
            </xsl:when>
            <xsl:when test="$embargoedDate!=''">
                <!-- this all might be overkill, need to confirm embargoedDate element
                        disappears after time expires -->
                <xsl:variable name="dateDiff">
                    <xsl:call-template name="datetime:difference">
                        <xsl:with-param name="start" select="datetime:date()"/>
                        <xsl:with-param name="end"
                                        select="datetime:date($embargoedDate)"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$embargoedDate='9999-01-01' and $embargoType='oneyear'">
                        <!-- The item is under one-year embargo, but the article has not been published yet,
                                   so we don't have an end date. -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.oneyear</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when
                            test="$embargoedDate='9999-01-01' and ($embargoType='untilArticleAppears' or $embargoType='unknown')">
                        <!-- The item is under embargo, but the end date is not yet known -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.publication</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when test="$embargoedDate='9999-01-01' and $embargoType='custom'">
                        <!-- The item is under embargo, but the end date is not yet known. The editor has approved a custom length. -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.custom</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when test="not(starts-with($dateDiff, '-'))">
                        <!-- The item is under embargo, and the end date of the embargo is known. -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource</i18n:text>
                            <xsl:value-of select="$embargoedDate"/>
                        </div>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- The item is not currently under embargo -->
                        <xsl:call-template name="checkedAndNoEmbargo"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
        </xsl:choose>

        <xsl:apply-templates
                select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>

        <xsl:if
                test=".//dim:field[@element='rights'][.='http://creativecommons.org/publicdomain/zero/1.0/']">
            <xsl:choose>
                <!-- this all might be overkill, need to confirm embargoedDate element
                        disappears after time expires -->
                <xsl:when test="$embargoedDate!=''">
                    <xsl:variable name="dateDiff">
                        <xsl:call-template name="datetime:difference">
                            <xsl:with-param name="start" select="datetime:date()"/>
                            <xsl:with-param name="end"
                                            select="datetime:date($embargoedDate)"/>
                        </xsl:call-template>
                    </xsl:variable>
                    <xsl:if test="starts-with($dateDiff, '-')">
                        <div style="padding-top: 10px;">
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.license-cc0</i18n:text>
                            <xsl:text> &#160; </xsl:text>
                            <a href="http://creativecommons.org/publicdomain/zero/1.0/"
                               target="_blank" class="single-image-link">
                                <img src="/themes/Dryad/images/cc-zero.png" alt="CC0"/>
                            </a>
                            <a href="http://opendefinition.org/" class="single-image-link">
                                <img src="/themes/Dryad/images/opendata.png" alt="open data"/>
                            </a>
                        </div>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <div style="padding-top: 10px;">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.license-cc0</i18n:text>
                        <xsl:text> &#160; </xsl:text>
                        <a href="http://creativecommons.org/publicdomain/zero/1.0/"
                           target="_blank" class="single-image-link">
                            <img src="/themes/Dryad/images/cc-zero.png" alt="CC0"/>
                        </a>
                        <a href="http://opendefinition.org/" class="single-image-link">
                            <img src="/themes/Dryad/images/opendata.png" alt="open data"/>
                        </a>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <xsl:template name="checkedAndNoEmbargo">
        <div class="ds-static-div primary">
            <table class="ds-table file-list">
                <tr class="ds-table-header-row">
                </tr>
                <tr>
                    <xsl:apply-templates select="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
                        <xsl:with-param name="context" select="."/>
                        <xsl:with-param name="primaryBitstream"
                                        select="./mets:structMap[@TYPE='LOGICAL']/mets:div[@TYPE='DSpace Item']/mets:fptr/@FILEID"/>
                    </xsl:apply-templates>
                </tr>
            </table>
        </div>
    </xsl:template>


    <!-- An item rendered in the detailView pattern, the "full item record"
         view of a DSpace item in Manakin. -->
    <xsl:template name="itemDetailView-DIM">

        <!-- Output all of the metadata about the item from the metadata section -->
        <xsl:apply-templates
                select="mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim"
                mode="itemDetailView-DIM"/>

        <!-- Generate the bitstream information from the file section -->
        <xsl:variable name="embargoedDate"
                      select=".//dim:field[@element='date' and @qualifier='embargoedUntil']"/>
        <xsl:variable name="embargoType">
            <xsl:choose>
                <xsl:when test=".//dim:field[@element='type' and @qualifier='embargo']">
                    <xsl:value-of
                            select=".//dim:field[@element='type' and @qualifier='embargo']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:text>unknown</xsl:text>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$embargoedDate!=''">
                <!-- this all might be overkill, need to confirm embargoedDate element
                        disappears after time expires -->
                <xsl:variable name="dateDiff">
                    <xsl:call-template name="datetime:difference">
                        <xsl:with-param name="start" select="datetime:date()"/>
                        <xsl:with-param name="end"
                                        select="datetime:date($embargoedDate)"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:choose>
                    <xsl:when test="$embargoedDate='9999-01-01' and $embargoType='oneyear'">
                        <!-- The item is under one-year embargo, but the article has not been published yet,
                                   so we don't have an end date. -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.oneyear</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when
                            test="$embargoedDate='9999-01-01' and ($embargoType='untilArticleAppears' or $embargoType='unknown')">
                        <!-- The item is under embargo, but the end date is not yet known -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.publication</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when test="$embargoedDate='9999-01-01' and $embargoType='custom'">
                        <!-- The item is under embargo, but the end date is not yet known. The editor has approved a custom length. -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.custom</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when test="not(starts-with($dateDiff, '-'))">
                        <!-- The item is under embargo, and the end date of the embargo is known. -->
                        <div id="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource</i18n:text>
                            <xsl:value-of select="$embargoedDate"/>
                        </div>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- The item is not currently under embargo -->
                        <xsl:call-template name="checkedAndNoEmbargo"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
                <xsl:call-template name="checkedAndNoEmbargo"/>
            </xsl:when>
        </xsl:choose>

        <!-- Generate the Creative Commons license information from the file section
              (DSpace deposit license hidden by default) -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>

    </xsl:template>

    <xsl:template name="package-citation">
        <xsl:param name="date"/>
        <xsl:param name="title"/>
        <xsl:param name="date_accessioned"/>
        <xsl:param name="package_doi"/>

        <xsl:choose>
            <xsl:when test="string-length($date) > 0">
                <xsl:text> </xsl:text>
                <xsl:choose>
                    <xsl:when test="not(starts-with($date, '('))">
                        <xsl:value-of select="concat('(', substring($date, 1, 4), ')')"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$date"/>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:text> </xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>. </xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:choose>
            <xsl:when test="not($title)">
                <xsl:text> </xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="not(starts-with($title, 'Data from: '))">
                    <i18n:text>xmlui.DryadItemSummary.dataFrom</i18n:text>
                </xsl:if>
                <xsl:variable name="clean-title" select="normalize-space($title)"/>
                <xsl:value-of select="$clean-title"/>
                <xsl:variable name="titleEndChar" select="substring($clean-title, string-length($clean-title), 1)"/>
                <xsl:choose>
                    <xsl:when test="$titleEndChar != '.' and $titleEndChar != '?'">
                        <xsl:text>. </xsl:text>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:text> </xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
        <span>
            <i18n:text>xmlui.DryadItemSummary.dryadRepo</i18n:text>
        </span>

        <!-- if Item not_archived don't add the link. -->
        <xsl:choose>
            <xsl:when test="not($date_accessioned)">
                <xsl:value-of select="$package_doi"/>
            </xsl:when>
            <xsl:otherwise>
                <a>
                    <!-- href -->
                    <xsl:attribute name="href">
                        <xsl:value-of select="$package_doi"/>
                    </xsl:attribute>

                    <!-- text -->
                    <xsl:value-of select="$package_doi"/>
                </a>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="journal-lookup">
        <xsl:param name="fullname"/>
        <xsl:param name="cover"/>
        <xsl:param name="website"/>
        <xsl:param name="article-doi"/>
        <xsl:if test="$cover">
            <a target="_blank">
                <xsl:attribute name="href">
                    <xsl:choose>
                        <xsl:when test="contains($article-doi,'doi:')">
                            <xsl:value-of
                                    select="concat('http://dx.doi.org/', substring-after($article-doi, 'doi:'))"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of
                                    select="$website"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:attribute>
                <xsl:if test="$cover">
                    <img class="pub-cover" id="journal-logo">
                        <xsl:attribute name="src"><xsl:value-of select="$cover"/></xsl:attribute>
                        <xsl:attribute name="alt"><xsl:value-of select="$fullname"/></xsl:attribute>
                    </img>
                </xsl:if>
            </a>
        </xsl:if>
    </xsl:template>
    <!-- Override metadata field rendering to hide manuscript number -->
    <xsl:template match="dim:field" mode="itemDetailView-DIM">
        <xsl:if test="not(./@qualifier = 'manuscriptNumber')">
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

    <xsl:template name="format-author-names">
        <!--@Huang D@#0000-0002-1497-1284#, @Lapp H@#0000-0001-9107-0714#, @Smith J@, @Smith J@, @Doe J@-->
        <xsl:param name="InputString"/>
        <xsl:choose>
            <xsl:when test="contains($InputString, ',')">
                <xsl:choose>
                    <!-- There is only one name, but there's still a comma after-->
                    <xsl:when test="substring-after($InputString,',') = ''">
                        <xsl:call-template name="format-author-names">
                            <xsl:with-param name="InputString" select="substring-before($InputString,',')"/>
                        </xsl:call-template>
                    </xsl:when>
                    <xsl:otherwise>
                        <!-- There are multiple names -->
                        <xsl:call-template name="format-single-name">
                            <xsl:with-param name="nameString" select="substring-before($InputString,',')"/>
                        </xsl:call-template>
                        <xsl:text>, </xsl:text>
                        <xsl:call-template name="format-author-names">
                            <xsl:with-param name="InputString" select="substring-after($InputString,',')"/>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="format-single-name">
                    <xsl:with-param name="nameString" select="$InputString"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="format-single-name">
        <xsl:param name="nameString"/>
        <xsl:variable name="author" select="substring-before(substring-after($nameString,'@'),'@')"/>
        <xsl:choose>
            <xsl:when test="contains($nameString,'#')">
                <!-- name contains an Orcid: make it a hyperlink. -->
                <xsl:variable name="orcid" select="substring-before(substring-after($nameString,'#'),'#')"/>
                <!--https://dev.datadryad.org/discover?query=0000-0002-1497-1284&submit=Go-->
                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of select="concat($context-path,'/discover?query=',$orcid,'&amp;submit=Go')"/>
                    </xsl:attribute>
                    <xsl:value-of select="$author"/>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$author"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="make-funding-string">
        <!-- Funding strings are in the format 'grantnumber@FunderName (country)'-->
        <xsl:param name="InputString"/>
        <xsl:choose>
            <!-- This test is for pre-DryadFundingConcept packages: they don't have the funder packed into the metadata; we assumed that US NSF was the only funder.-->
            <xsl:when test="contains($InputString, '@')">
                <xsl:call-template name="format-single-funder">
                    <xsl:with-param name="nameString" select="$InputString"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="format-single-funder">
                    <xsl:with-param name="nameString" select="concat($InputString,'@National Science Foundation (United States)')"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="format-single-funder">
        <xsl:param name="nameString"/>
        <xsl:variable name="grantNumber" select="substring-before($nameString,'@')"/>
        <xsl:variable name="funder" select="substring-after($nameString,'@')"/>
        <tr>
            <th>Funding</th>
            <td>
                <xsl:value-of select="$funder"/><xsl:text>, grant number </xsl:text><xsl:value-of select="$grantNumber"/>
            </td>
        </tr>
    </xsl:template>


    <xsl:template name="make-author-string">
        <xsl:variable name="authors">
            <!-- the authors@packages node is packed by DryadWorkflowUtils.getAuthors()-->
            <!-- format is @Doe J@#0000-0000-0000-0000#,@Smith L@#0000-0000-0000-0000#,-->
            <!-- comma-delimited, with each name offset by @ and orcid offset by #, tailing comma-->
            <!-- if this metadata is not available, construct it from the component dc metadata fields.-->
            <xsl:choose>
                <xsl:when test="$meta[@element='authors'][@qualifier='package']">
                    <xsl:value-of select="$meta[@element='authors'][@qualifier='package']"/>
                </xsl:when>
                <xsl:when test=".//dim:field[@element='contributor'][@qualifier='author']">
                    <xsl:for-each select=".//dim:field[@element='contributor'][@qualifier='author']">
                        <xsl:call-template name="parse-author-node">
                            <xsl:with-param name="author" select="."/>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test=".//dim:field[@element='creator']">
                    <xsl:for-each select=".//dim:field[@element='creator']">
                        <xsl:call-template name="parse-author-node">
                            <xsl:with-param name="author" select="."/>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:when>
                <xsl:when test=".//dim:field[@element='contributor']">
                    <xsl:for-each select=".//dim:field[@element='contributor']">
                        <xsl:call-template name="parse-author-node">
                            <xsl:with-param name="author" select="."/>
                        </xsl:call-template>
                    </xsl:for-each>
                </xsl:when>
            </xsl:choose>
        </xsl:variable>
        <xsl:call-template name="format-author-names">
            <xsl:with-param name="InputString" select="$authors"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template name="parse-author-node">
        <xsl:param name="author"/>
        <xsl:text>@</xsl:text>
        <xsl:choose>
            <xsl:when test="contains($author, ',')">
                <xsl:call-template name="name-parse-reverse">
                    <xsl:with-param name="name" select="$author"/>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="name-parse">
                    <xsl:with-param name="name" select="$author"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:text>@</xsl:text>
        <xsl:if test="$author/@authority">
            <!-- will be generated::orcid::0000-0002-1497-1284-->
            <xsl:text>#</xsl:text>
            <xsl:value-of select="substring-after($author/@authority,'orcid:')"/>
            <xsl:text>#</xsl:text>
        </xsl:if>
        <xsl:text>,</xsl:text>
    </xsl:template>

    <xsl:template name="publication-citation">
        <xsl:param name="article_doi"/>
        <xsl:param name="citation"/>
        <xsl:param name="article_id"/>
        <xsl:param name="article_pmid"/>
        <xsl:param name="journal"/>

        <xsl:choose>
            <xsl:when test="$citation!=''">
                <xsl:choose>
                    <xsl:when test="$article_id">
                        <xsl:value-of select="$citation"/>
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="$article_id"/>
                    </xsl:when>
                    <xsl:when
                            test="$article_doi and not(contains($citation, $article_doi))">
                        <xsl:value-of select="$citation"/>
                        <xsl:text> </xsl:text>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of
                                        select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"/>
                            </xsl:attribute>
                            <xsl:value-of
                                    select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"/>
                        </a>
                    </xsl:when>
                    <xsl:when test="$article_doi">
                        <xsl:value-of select="substring-before($citation, $article_doi)"/>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of
                                        select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"/>
                            </xsl:attribute>
                            <xsl:value-of
                                    select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"/>
                        </a>
                    </xsl:when>
                    <xsl:when test="$article_pmid">
                        <xsl:value-of select="$citation"/>
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="$article_pmid"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$citation"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="$journal">
                        <span style="font-style: italic;">
                            <i18n:text>xmlui.DryadItemSummary.citationNotYet1</i18n:text>
                            <xsl:value-of select="$journal"/>
                            <xsl:text>. </xsl:text>
                            <i18n:text>xmlui.DryadItemSummary.citationNotYet2</i18n:text>
                            <xsl:if test="$article_doi">
                                <a>
                                    <xsl:attribute name="href">
                                        <xsl:value-of
                                                select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"/>
                                    </xsl:attribute>
                                    <xsl:value-of
                                            select="concat('http://dx.doi.org/', substring-after($article_doi, 'doi:'))"/>
                                </a>
                            </xsl:if>
                        </span>
                    </xsl:when>
                    <xsl:otherwise>
                        <span style="font-style: italic;">
                            <i18n:text>xmlui.DryadItemSummary.citationNotYet</i18n:text>
                        </span>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
