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

    <xsl:template name="itemSummaryView-DIM">
        <xsl:variable name="datafiles"
                      select=".//dim:field[@element='relation'][@qualifier='haspart']"/>

        <!-- my_doi and my_uri go together; there is a my_uri if no my_doi -->
        <xsl:variable name="my_doi"
                      select=".//dim:field[@element='identifier'][not(@qualifier)][starts-with(., 'doi:')]"/>
        <xsl:variable name="my_full_doi"
                      select=".//dim:field[@element='identifier'][not(@qualifier)][contains(., 'doi.org')]"/>
        <xsl:variable name="my_uri"
                      select=".//dim:field[@element='identifier'][@qualifier='uri'][not(starts-with(., 'doi'))][not(contains(., 'doi.org'))]"/>

        <!--<h1>Non doi test: <xsl:value-of select="$doi_redirect"/></h1> -->

        <!-- Obtain an identifier if the item is harvested from KNB. But we have
              to munge the URL to link to LTER instead of the raw XML. -->
        <xsl:variable name="knb_url_raw"
                      select=".//dim:field[@element='identifier'][starts-with(.,'http://metacat')]"/>
        <xsl:variable name="knb_url">
            <xsl:if test="$knb_url_raw!=''">
                <xsl:value-of
                        select="substring($knb_url_raw,0,string-length($knb_url_raw)-2)"/>
                lter
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


        <!-- If in admin view, show title (in Dryad.xsl for non-admin views) -->
        <xsl:if
                test="$meta[@element='request'][@qualifier='URI'][.='admin/item/view_item'] ">
            <h1 class="pagetitle">
                <xsl:choose>
                    <xsl:when
                            test="not(.//dim:field[@element='title']) and not($meta[@element='title'])">
                        <xsl:text> </xsl:text>
                    </xsl:when>
                    <xsl:when test=".//dim:field[@element='title']">
                        <xsl:value-of select=".//dim:field[@element='title']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="$meta[@element='title']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </h1>
        </xsl:if>

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

            <xsl:variable name="article_doi"
                          select="$meta[@element='identifier'][@qualifier='article'][. != '']"/>

            <xsl:variable name="journal"
                          select="$meta[@element='publicationName']"/>

            <div>
                <div class="citation-view">
                    <p class="ds-paragraph">
                        <i18n:text>xmlui.DryadItemSummary.whenUsing</i18n:text>
                    </p>
                    <table>
                        <tr>
                            <td>
                                <blockquote>
                                    <xsl:variable name="citation"
                                                  select="$meta[@element='citation'][@qualifier='article']"/>
                                    <xsl:choose>
                                        <xsl:when test="$citation != ''">
                                            <xsl:choose>
                                                <xsl:when
                                                        test="$article_doi and not(contains($citation, $article_doi))">
                                                    <xsl:copy-of select="$citation"/>
                                                    <a>
                                                        <xsl:attribute name="href">
                                                            <xsl:choose>
                                                                <xsl:when test="starts-with($article_doi, 'http')">
                                                                    <xsl:value-of select="$article_doi"/>
                                                                </xsl:when>
                                                                <xsl:when test="starts-with($article_doi, 'doi:')">
                                                                    <xsl:value-of
                                                                            select="concat('https://doi.org/', substring-after($article_doi, 'doi:'))"/>
                                                                </xsl:when>
                                                            </xsl:choose>
                                                        </xsl:attribute>
                                                        <xsl:value-of select="$article_doi"/>
                                                    </a>
                                                </xsl:when>
                                                <xsl:when test="$article_doi">
                                                    <xsl:copy-of select="substring-before($citation, $article_doi)"/>
                                                    <a>
                                                        <xsl:attribute name="href">
                                                            <xsl:value-of
                                                                    select="concat('https://doi.org/', substring-after($article_doi, 'doi:'))"/>
                                                        </xsl:attribute>
                                                        <xsl:value-of select="$article_doi"/>
                                                    </a>
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
                                                                    <xsl:choose>
                                                                        <xsl:when
                                                                                test="starts-with($article_doi, 'http')">
                                                                            <xsl:value-of select="$article_doi"/>
                                                                        </xsl:when>
                                                                        <xsl:when
                                                                                test="starts-with($article_doi, 'doi:')">
                                                                            <xsl:value-of
                                                                                    select="concat('https://doi.org/', substring-after($article_doi, 'doi:'))"/>
                                                                        </xsl:when>
                                                                    </xsl:choose>
                                                                </xsl:attribute>
                                                                <xsl:value-of select="$article_doi"/>
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
                                </blockquote>
                            </td>
                            <td rowspan="2">
                            </td>
                        </tr>
                    </table>

                    <p class="ds-paragraph">
                        <i18n:text>xmlui.DryadItemSummary.pleaseCite</i18n:text>
                    </p>
                    <blockquote>
                        <xsl:value-of select="$meta[@element='authors'][@qualifier='package']"/>
                        <xsl:choose>
                            <xsl:when test="$meta[@element='date'][@qualifier='issued']">
                                <xsl:value-of select="$meta[@element='date'][@qualifier='issued']"/>
                            </xsl:when>
                            <xsl:when test="$meta[@element='dateIssued'][@qualifier='package']">
                                <xsl:value-of
                                        select="$meta[@element='dateIssued'][@qualifier='package']"/>
                            </xsl:when>
                        </xsl:choose>
                        <xsl:text> </xsl:text>

                        <xsl:variable name="title"
                                      select="$meta[@element='title'][@qualifier='package']"/>
                        <xsl:copy-of select="$title"/>
                        <span>
                            <i18n:text>xmlui.DryadItemSummary.dryadRepo</i18n:text>
                        </span>


                        <!-- if Item not_archived don't add the link. -->
                        <xsl:variable name="id" select="$meta[@element='identifier'][@qualifier='package']"/>
                        <xsl:choose>
                            <xsl:when
                                    test="not(/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='dc'][@element='date' ][@qualifier='accessioned'])">
                                <xsl:choose>
                                    <xsl:when test="starts-with($id, 'doi')">
                                        <xsl:value-of select="$id"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="concat('http://hdl.handle.net/', $id)"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:otherwise>
                                <a>
                                    <!-- link -->
                                    <xsl:attribute name="href">
                                        <xsl:choose>
                                            <xsl:when test="starts-with($id, 'doi')">
                                                <xsl:value-of
                                                        select="concat('https://doi.org/', substring-after($id, 'doi:'))"/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="concat('http://hdl.handle.net/', $id)"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:attribute>

                                    <!-- text -->
                                    <xsl:choose>
                                        <xsl:when test="starts-with($id, 'doi')">
                                            <xsl:value-of select="$id"/>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:value-of select="concat('http://hdl.handle.net/', $id)"/>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </a>
                            </xsl:otherwise>
                        </xsl:choose>
                    </blockquote>
                    <!-- only show citation/share if viewing from public (not admin) page -->
                    <xsl:if
                            test="not($meta[@element='request'][@qualifier='URI'][.='admin/item/view_item'])
					and not($meta[@element='request'][@qualifier='URI'][contains(., 'workflow')])">
                        <xsl:variable name="pkgDOI"
                                      select="$meta[@element='identifier'][@qualifier='package']"/>
                        <!-- Here we give links to expost the citation and share options available
                              on each item record. The citation links link to a servlet in the DOI module
                              (a Dryad/DSpace module in the modules directory). When a DOI is passed as
                              a parameter in the URL to that servlet, the citation metadata is looked up
                              in DSpace and formatted for download from this link. -->
                        <div align="right" style="padding-right: 70px; padding-bottom: 5px;">
                            <a href="/cite" id="cite" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.cite</i18n:text>
                            </a>
                            <xsl:text>  |  </xsl:text>
                            <a href="/share" id="share" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.share</i18n:text>
                            </a>
                            <div id="citemediv">
                                <table style="width: 45%;">
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
                                <table style="width: 50%;">
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
                                                            select="concat($dfirstpart, $apos, 'https://doi.org/', $pkgDOI, $apos, $dsecondpart,
														$thistitle, $dthirdpart)"/>
                                                </xsl:attribute>
                                                <img src="http://www.delicious.com/static/img/delicious.small.gif"
                                                     height="18" width="18" alt="Delicious"
                                                     style="border: 1px solid #ccc;"/>
                                            </xsl:element>
                                            <!-- xsl:text is a workaround for formatting issues -->
                                            <script type="text/javascript" src="/themes/Dryad/lib/delicious.js">
                                                <xsl:text> </xsl:text>
                                            </script>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <!-- Doesn't like DOI, can't resolve against CrossRef -->
                                                    <xsl:variable name="pkgURL">
                                                        <xsl:choose>
                                                            <xsl:when
                                                                    test=".//dim:field[@element='relation'][@qualifier='ispartof']">
                                                                <xsl:value-of
                                                                        select="encoder:encode(.//dim:field[@element='relation'][@qualifier='ispartof'])"/>
                                                            </xsl:when>
                                                            <xsl:otherwise>
                                                                <xsl:value-of select="$thispage"/>
                                                            </xsl:otherwise>
                                                        </xsl:choose>
                                                    </xsl:variable>
                                                    <xsl:value-of
                                                            select="concat('http://www.connotea.org/add?uri=', $pkgURL)"/>
                                                </xsl:attribute>
                                                <img border="0px;" src="/themes/Dryad/images/connotea.png"
                                                     height="21" width="21" alt="Connotea" title="Connotea"/>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="class">DiggThisButton DiggCompact</xsl:attribute>
                                                <xsl:attribute name="href">
                                                    <xsl:value-of
                                                            select="concat('http://digg.com/submit?url=https://doi.org/', $pkgDOI,
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
                                                            select="concat($rfirstpart, $apos, 'https://doi.org/', $pkgDOI, $apos, $rsecondpart,
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
                                                    <xsl:value-of select="concat('https://doi.org/', $pkgDOI)"/>
                                                </xsl:attribute>
                                                <xsl:text>Tweet</xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="iframe">
                                                <xsl:attribute name="src">
                                                    <xsl:value-of
                                                            select="concat('http://www.facebook.com/plugins/like.php?href=',
														encoder:encode(concat('https://doi.org/', $pkgDOI)),
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
                <div class="journal-cover-view">
                    <xsl:call-template name="journal-lookup">
                        <xsl:with-param name="journal-name" select="$journal"/>
                        <xsl:with-param name="article-doi"
                                        select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi:')]"/>
                    </xsl:call-template>
                </div>
            </div>
        </xsl:if>

        <!-- CITATION FOR DATA PACKAGE -->
        <xsl:if
                test="not($meta[@element='xhtml_head_item'][contains(., 'DCTERMS.isPartOf')]) and .//dim:field[@element='relation'][@qualifier='haspart']">
            <div>
                <div class="citation-view">
                    <xsl:variable name="citation"
                                  select=".//dim:field[@element='identifier'][@qualifier='citation'][position() = 1]"/>
                    <xsl:variable name="article_doi"
                                  select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi:')]"/>
                    <xsl:variable name="article_pmid"
                                  select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'PMID:')]"/>
                    <xsl:variable name="article_id"
                                  select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][not(starts-with(., 'doi:')) and not(starts-with(.,'PMID:'))]"/>
                    <p class="ds-paragraph">
                        <i18n:text>xmlui.DryadItemSummary.whenUsing</i18n:text>
                    </p>
                   <blockquote>
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
                                        <xsl:copy-of select="$citation"/>
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                    select="concat('https://doi.org/', substring-after($article_doi, 'doi:'))"/>
                                            </xsl:attribute>
                                            <xsl:value-of select="$article_doi"/>
                                        </a>
                                    </xsl:when>
                                    <xsl:when test="$article_doi">
                                        <xsl:copy-of select="substring-before($citation, $article_doi)"/>
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                    select="concat('https://doi.org/', substring-after($article_doi, 'doi:'))"/>
                                            </xsl:attribute>
                                            <xsl:value-of select="$article_doi"/>
                                        </a>
                                    </xsl:when>
                                    <xsl:when test="$article_pmid">
                                        <xsl:copy-of select="$citation"/>
                                        <xsl:text> </xsl:text>
                                        <xsl:value-of select="$article_pmid"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="$citation"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:variable name="journal"
                                    select=".//dim:field[@element='publicationName']"/>
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
                                                            select="concat('https://doi.org/', substring-after($article_doi, 'doi:'))"/>
                                                    </xsl:attribute>
                                                    <xsl:value-of select="$article_doi"/>
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
                    </blockquote>
                    <table>
                        <tr>
                            <td rowspace="2">
                            </td>
                        </tr>
                    </table>
                    <xsl:if test="$datafiles">
                        <p class="ds-paragraph">
                            <i18n:text>xmlui.DryadItemSummary.pleaseCite</i18n:text>
                        </p>
                        <blockquote>
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
                            <xsl:if test=".//dim:field[@element='date'][@qualifier='issued']">
                                <xsl:text> </xsl:text>
                                <xsl:value-of
                                        select="concat('(', substring(.//dim:field[@element='date'][@qualifier='issued'], 1, 4), ') ')"/>
                            </xsl:if>
                            <xsl:choose>
                                <xsl:when test="not(.//dim:field[@element='title'])">
                                    <xsl:text> </xsl:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:variable name="title"
                                                  select=".//dim:field[@element='title']/node()"/>
                                    <xsl:if test="not(starts-with($title, 'Data from: '))">
                                        <i18n:text>xmlui.DryadItemSummary.dataFrom</i18n:text>
                                    </xsl:if>
                                    <xsl:copy-of select="$title"/>
                                    <xsl:variable name="titleEndChar"
                                                  select="substring($title, string-length($title), 1)"/>
                                    <xsl:choose>
                                        <xsl:when test="$titleEndChar != '.' and $titleEndChar != '?'">
                                            <xsl:text>. </xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:text>&#160;</xsl:text>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:otherwise>
                            </xsl:choose>
                            <span>
                                <i18n:text>xmlui.DryadItemSummary.dryadRepo</i18n:text>
                            </span>

                            <!-- if Item not_archived don't add the link. -->
                            <xsl:choose>
                                <xsl:when
                                        test="not(/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@mdschema='dc'][@element='date' ][@qualifier='accessioned'])">
                                    <xsl:value-of select="$my_doi"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <a>
                                        <!-- href -->
                                        <xsl:attribute name="href">
                                            <xsl:choose>
                                                <xsl:when test="$my_doi">
                                                    <xsl:value-of
                                                            select="concat('https://doi.org/', substring-after($my_doi, 'doi:'))"/>
                                                </xsl:when>
                                                <xsl:otherwise>
                                                    <xsl:value-of select="$my_uri"/>
                                                </xsl:otherwise>
                                            </xsl:choose>
                                        </xsl:attribute>

                                        <!-- text -->
                                        <xsl:choose>
                                            <xsl:when test="$my_doi">
                                                <xsl:value-of select="$my_doi"/>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:value-of select="$my_uri"/>
                                            </xsl:otherwise>
                                        </xsl:choose>

                                    </a>
                                </xsl:otherwise>
                            </xsl:choose>
                        </blockquote>
                    </xsl:if>
                    <!-- only show citation/share if viewing from public (not admin) page -->
                    <xsl:if
                            test="not($meta[@element='request'][@qualifier='URI'][.='admin/item/view_item'])">
                        <div align="right" style="padding-right: 70px; padding-bottom: 5px;">
                            <a href="/cite" id="cite" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.cite</i18n:text>
                            </a>
                            <xsl:text>  |  </xsl:text>
                            <a href="/share" id="share" title="Click to open and close">
                                <i18n:text>xmlui.DryadItemSummary.share</i18n:text>
                            </a>
                            <div id="citemediv">
                                <table style="width: 45%;">
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
                                <table style="width: 50%;">
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
                                                            select="concat($dfirstpart, $apos, 'https://doi.org/', $my_doi, $apos, $dsecondpart)"/>
                                                </xsl:attribute>
                                                <img src="http://www.delicious.com/static/img/delicious.small.gif"
                                                     height="18" width="18" alt="Delicious"
                                                     style="border: 1px solid #ccc;"/>
                                            </xsl:element>
                                            <!-- xsl:text is a workaround for formatting issues -->
                                            <script type="text/javascript" src="/themes/Dryad/lib/delicious.js">
                                                <xsl:text> </xsl:text>
                                            </script>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="href">
                                                    <!-- Doesn't like DOI, can't resolve against CrossRef -->
                                                    <xsl:value-of
                                                            select="concat('http://www.connotea.org/add?uri=', $thispage)"/>
                                                </xsl:attribute>
                                                <img border="0px;" src="/themes/Dryad/images/connotea.png"
                                                     height="21" width="21" alt="Connotea" title="Connotea"/>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="a">
                                                <xsl:attribute name="class">DiggThisButton DiggCompact</xsl:attribute>
                                                <xsl:attribute name="href">
                                                    <xsl:value-of
                                                            select="concat('http://digg.com/submit?url=https://doi.org/', $my_doi,
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
                                                            select="concat($rfirstpart, $apos, 'https://doi.org/', $my_doi, $apos, $rsecondpart,
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
                                                    <xsl:value-of select="concat('https://doi.org/', $my_doi)"/>
                                                </xsl:attribute>
                                                <xsl:text>Tweet</xsl:text>
                                            </xsl:element>
                                        </td>
                                        <td>
                                            <xsl:element name="iframe">
                                                <xsl:attribute name="src">
                                                    <xsl:value-of
                                                            select="concat('http://www.facebook.com/plugins/like.php?href=',
													encoder:encode(concat('https://doi.org/', $my_doi)),
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
                <div class="journal-cover-view">
                    <xsl:call-template name="journal-lookup">
                        <xsl:with-param name="journal-name" select=".//dim:field[@element='publicationName']"/>
                        <xsl:with-param name="article-doi"
                                        select=".//dim:field[@element='relation'][@qualifier='isreferencedby'][starts-with(., 'doi:')]"/>
                    </xsl:call-template>
                </div>
            </div>
        </xsl:if>

        <!-- General, non-citation, metadata display -->


        <table class="ds-includeSet-table">
            <tr class="ds-table-row">
                <td width="15%">
                    <xsl:choose>
                        <xsl:when test="$treebase_url != ''">
                            <span class="bold">
                                <i18n:text>xmlui.DryadItemSummary.viewContentTB</i18n:text>
                            </span>
                        </xsl:when>
                        <xsl:when test="$knb_url!=''">
                            <span class="bold">
                                <i18n:text>xmlui.DryadItemSummary.viewContentKNB</i18n:text>
                            </span>
                        </xsl:when>
                        <xsl:when test="$datafiles!=''">
                            <span class="bold">
                                <i18n:text>xmlui.DryadItemSummary.dryadPkgID</i18n:text>
                            </span>
                        </xsl:when>
                        <xsl:otherwise>
                            <span class="bold">
                                <i18n:text>xmlui.DryadItemSummary.dryadFileID</i18n:text>
                            </span>
                        </xsl:otherwise>
                    </xsl:choose>
                </td>
                <td width="55%">
                    <xsl:choose>
                        <xsl:when test="$treebase_url!=''">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:call-template name="checkURL">
                                        <xsl:with-param name="doiIdentifier" select="$my_doi"/>
                                    </xsl:call-template>
                                </xsl:attribute>
                                <xsl:value-of select="$treebase_url"/>
                            </a>
                        </xsl:when>
                        <xsl:when test="$knb_url!=''">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:call-template name="checkURL">
                                        <xsl:with-param name="doiIdentifier" select="$my_doi"/>
                                    </xsl:call-template>
                                </xsl:attribute>
                                <xsl:value-of select="$knb_url"/>
                            </a>
                        </xsl:when>
                        <xsl:when test="$my_doi">
                            <!--<a>-->
                                <!--<xsl:attribute name="href">-->
                                    <!--<xsl:call-template name="checkURL">-->
                                        <!--<xsl:with-param name="doiIdentifier" select="$my_doi"/>-->
                                    <!--</xsl:call-template>-->
                                <!--</xsl:attribute>-->
                                <xsl:value-of select="$my_doi"/>
                            <!--</a>-->
                        </xsl:when>
                        <xsl:when test="$my_uri">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="$my_uri"/>
                                </xsl:attribute>
                                <xsl:value-of select="$my_uri"/>
                            </a>
                        </xsl:when>
                    </xsl:choose>

                   <xsl:variable name="pageviews"
                                  select="$meta[@element='dryad'][@qualifier='pageviews']"/>
                    <xsl:if test="$pageviews">
                        <xsl:if test="$datafiles!=''">
                            <span style="font-size: smaller; font-weight: bold;">
                                <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
                            <xsl:value-of select="$pageviews"/>
                            <xsl:choose>
                                <xsl:when test="string($pageviews) = '1'">
                                    <xsl:text>&#160;</xsl:text>
                                    <i18n:text>xmlui.DryadItemSummary.view</i18n:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>&#160;</xsl:text>
                                    <i18n:text>xmlui.DryadItemSummary.views</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
                            </span>
                        </xsl:if>
                    </xsl:if>
                    <xsl:variable name="downloads"  select="$meta[@element='dryad'][@qualifier='downloads']"/>
                    <xsl:if test="$downloads">
                        <span style="font-size: smaller; font-weight: bold;">
                            <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
                            <xsl:value-of select="$downloads"/>
                            <xsl:choose>
                                <xsl:when test="string($downloads) = '1'">
                                    <xsl:text>&#160;</xsl:text>
                                    <i18n:text>xmlui.DryadItemSummary.download</i18n:text>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:text>&#160;</xsl:text>
                                    <i18n:text>xmlui.DryadItemSummary.downloads</i18n:text>
                                </xsl:otherwise>
                            </xsl:choose>
                        </span>
                    </xsl:if>

                    <span class="Z3988">
                        <xsl:attribute name="title">
                            <xsl:call-template name="renderCOinS"/>
                        </xsl:attribute>
                        <xsl:text>&#160;</xsl:text>
                    </span>


                    <!-- the message is dynamic. If the message has to be displayed at the center of the page, manage it in a dynamically.-->
                    <!--xsl:if test="$versionNotice">
                        <span style="versionNotice">
                            <xsl:variable name="targetcell" select="$latestDataVersion"/>
                            <xsl:variable name="target" select="string($targetcell/dri:xref[1]/attribute::target)"/>
                            <i18n:text>xmlui.DryadItemSummary.notCurrentVersion</i18n:text>
                            <xsl:text>&#160;</xsl:text>
                            <xsl:element name="a">
                                <xsl:attribute name="class"></xsl:attribute>
                                <xsl:attribute name="href">
                                    <xsl:value-of select="$target"/>
                                </xsl:attribute>
                                <i18n:text>xmlui.DryadItemSummary.mostCurrentVersion</i18n:text>
                            </xsl:element>
                            <xsl:text>&#160;</xsl:text>
                        </span>
                    </xsl:if-->

                </td>
            </tr>

            <xsl:variable name="description">
                <xsl:for-each
                        select=".//dim:field[@element='description'][not(@qualifier='provenance')]">
                    <xsl:copy-of select="node()"/>
                    <br/>
                </xsl:for-each>
            </xsl:variable>


            <xsl:if test="$description!=''">
                <tr class="ds-table-row">
                    <td>
                        <!-- Display "Description" for data files and "Abstract" for data packages. -->
                        <xsl:choose>
                            <xsl:when
                                    test=".//dim:field[@element='relation'][@qualifier='ispartof']">
                                <span class="bold">
                                    <i18n:text>xmlui.DryadItemSummary.description</i18n:text>
                                </span>
                            </xsl:when>
                            <xsl:otherwise>
                                <span class="bold">
                                    <i18n:text>xmlui.DryadItemSummary.abstract</i18n:text>
                                </span>
                            </xsl:otherwise>
                        </xsl:choose>
                    </td>
                    <td width="70%" colspan="2">
                        <xsl:copy-of select="$description"/>
                    </td>
                </tr>
            </xsl:if>

            <!-- Need to add spatial, temporal, taxonomic keywords from file metadata -->
            <xsl:variable name="keywords">
                <xsl:for-each
                        select=".//dim:field[@element='subject'][@mdschema='dc'][not(@qualifier)]">
                    <xsl:copy-of select="node()"/>
                    <xsl:if test="position() != last()">
		      <xsl:text>, </xsl:text>
		    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$keywords!=''">
                <tr class="ds-table-row">
                    <td>
                        <span class="bold">
                            <i18n:text>xmlui.DryadItemSummary.keywords</i18n:text>
                        </span>
                    </td>
                    <td colspan="2">
                        <xsl:copy-of select="$keywords"/>
                    </td>
                    <td>
                    </td>
                </tr>
            </xsl:if>

            <xsl:if
                    test=".//dim:field[@element='identifier'][not(@qualifier)][contains(., 'dryad.')]">
                <tr class="ds-table-row">
                    <td>
                        <span class="bold">
                            <i18n:text>xmlui.DryadItemSummary.depDate</i18n:text>
                        </span>
                    </td>
                    <td>
                        <xsl:copy-of
                                select=".//dim:field[@element='date' and @qualifier='accessioned']"/>
                    </td>
                    <td>
                    </td>
                </tr>
            </xsl:if>

            <xsl:variable name="sciNames">
                <xsl:for-each select=".//dim:field[@element='ScientificName']">
                    <xsl:copy-of select="node()"/>
                    <xsl:if test="position() != last()">
		      <xsl:text>, </xsl:text>
		    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$sciNames!=''">
                <tr class="ds-table-row">
                    <td>
                        <span class="bold">
                            <i18n:text>xmlui.DryadItemSummary.sciNames</i18n:text>
                        </span>
                    </td>
                    <td colspan="2">
                        <xsl:copy-of select="$sciNames"/>
                    </td>
                    <td>
                    </td>
                </tr>
            </xsl:if>

            <xsl:variable name="spatialCoverage">
                <xsl:for-each
                        select=".//dim:field[@element='coverage'][@qualifier='spatial']">
                    <xsl:copy-of select="node()"/>
                    <xsl:if test="position() != last()">
		      <xsl:text>, </xsl:text>
		    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$spatialCoverage!=''">
                <tr class="ds-table-row">
                    <td>
                        <span class="bold">
                            <i18n:text>xmlui.DryadItemSummary.spatialCov</i18n:text>
                        </span>
                    </td>
                    <td colspan="2">
                        <xsl:copy-of select="$spatialCoverage"/>
                    </td>
                    <td>
                    </td>
                </tr>
            </xsl:if>

            <xsl:variable name="temporalCoverage">
                <xsl:for-each
                        select=".//dim:field[@element='coverage'][@qualifier='temporal']">
                    <xsl:copy-of select="node()"/>
                    <xsl:if test="position() != last()">
		      <xsl:text>, </xsl:text>
		    </xsl:if>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$temporalCoverage!=''">
                <tr class="ds-table-row">
                    <td>
                        <span class="bold">
                            <i18n:text>xmlui.DryadItemSummary.temporalCov</i18n:text>
                        </span>
                    </td>
                    <td colspan="2">
                        <xsl:copy-of select="$temporalCoverage"/>
                    </td>
                    <td>
                    </td>
                </tr>
            </xsl:if>

            <xsl:if
                    test=".//dim:field[@element='identifier'][not(@qualifier)][not(contains(., 'dryad.'))]">

                <xsl:variable name="dc-creators"
                              select=".//dim:field[@element='creator'][@mdschema='dc']"/>

                <xsl:if test="$dc-creators != ''">
                    <tr class="ds-table-row">
                        <td>
                            <xsl:choose>
                                <xsl:when test="count($dc-creators) &gt; 1">
                                    <span class="bold">
                                        <i18n:text>xmlui.DryadItemSummary.authors</i18n:text>
                                    </span>
                                </xsl:when>
                                <xsl:otherwise>
                                    <span class="bold">
                                        <i18n:text>xmlui.DryadItemSummary.author</i18n:text>
                                    </span>
                                </xsl:otherwise>
                            </xsl:choose>
                        </td>
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
                    <tr class="ds-table-row">
                        <td>
                            <xsl:choose>
                                <xsl:when test="count($dc-publishers) &gt; 1">
                                    <span class="bold">
                                        <i18n:text>xmlui.DryadItemSummary.publishers</i18n:text>
                                    </span>
                                </xsl:when>
                                <xsl:otherwise>
                                    <span class="bold">
                                        <i18n:text>xmlui.DryadItemSummary.publisher</i18n:text>
                                    </span>
                                </xsl:otherwise>
                            </xsl:choose>
                        </td>
                        <td>
                            <xsl:for-each select="$dc-publishers">
                                <xsl:value-of select="."/>
                                <br/>
                            </xsl:for-each>
                        </td>
                    </tr>
                </xsl:if>

                <xsl:variable name="dc-date"
                              select=".//dim:field[@element='date'][not(@qualifier)][@mdschema='dc']"/>

                <xsl:if test="$dc-date != ''">
                    <tr class="ds-table-row">
                        <td>
                            <span class="bold">
                                <i18n:text>xmlui.DryadItemSummary.published</i18n:text>
                            </span>
                        </td>
                        <td>
                            <xsl:value-of select="$dc-date[1]"/>
                        </td>
                    </tr>
                </xsl:if>
            </xsl:if>

            <xsl:variable name="externalDataSets">
                <xsl:for-each
                        select=".//dim:field[@element='relation' and @qualifier='external'][contains(., 'treebase')]">
                    <span>
                        <i18n:text>xmlui.DryadItemSummary.treebaseLabel</i18n:text>
                    </span>
                    <a>
                        <xsl:attribute name="href">
                            <xsl:copy-of select="."/>
                        </xsl:attribute>
                        <xsl:copy-of select="."/>
                    </a>
                    <br/>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$externalDataSets!=''">
                <tr class="ds-table-row">
                    <td>
                        <span class="bold">
                            <i18n:text>xmlui.DryadItemSummary.otherRepos</i18n:text>
                        </span>
                    </td>
                    <td>
                        <xsl:copy-of select="$externalDataSets"/>
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
                                <xsl:copy-of select="."/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </a>
                    <br/>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$describedBy!=''">
                <tr class="ds-table-row">
                    <td>
                        <span class="bold">
                            <i18n:text>xmlui.DryadItemSummary.containedInPackage</i18n:text>
                        </span>
                    </td>
                    <td colspan="2">
                        <xsl:copy-of select="$describedBy"/>
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

        </table>

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
                               target="_blank">
                                <img src="/themes/Dryad/images/cc-zero.png" alt="CC0"/>
                            </a>
                            <a href="http://opendefinition.org/">
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
                           target="_blank">
                            <img src="/themes/Dryad/images/cc-zero.png" alt="CC0"/>
                        </a>
                        <a href="http://opendefinition.org/">
                            <img src="/themes/Dryad/images/opendata.png" alt="open data"/>
                        </a>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:template>

    <xsl:template name="checkedAndNoEmbargo">
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

    <!-- this generates the linked journal image - should find a way to drive this from the DryadJournalSubmission.properties file -->
    <xsl:template name="journal-lookup">
        <xsl:param name="journal-name"/>
        <xsl:param name="article-doi"/>
        <xsl:choose>
            <xsl:when test='$journal-name = "The American Naturalist"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.asnamnat.org/amnat')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/amNat.png"
                         alt="The American Naturalist cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Biological Journal of the Linnean Society"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.blackwellpublishing.com/BIJ')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/BJLS.png"
                        alt="Biological Journal of the Linnean Society cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Biology Letters"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://rsbl.royalsocietypublishing.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/BiolLett.png"
                        alt="Biology Letters cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "BioRisk"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.pensoft.net/journals/biorisk/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/biorisk.png"
                        alt="BioRisk cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "BMJ Open"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://bmjopen.bmj.com/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/bmjOpen.png"
                        alt="BMJ Open logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Comparative Cytogenetics"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.pensoft.net/journals/compcytogen/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/compcytogen.png"
                        alt="Comparative Cytogenetics cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Ecological Monographs"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                        select="string('http://www.esapubs.org/esapubs/journals/monographs.htm')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/ecoMono.png"
                         alt="Ecological Monographs cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "eLife"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://http://www.elifesciences.org/the-journal/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/elife-full-color-vertical.png"
                        alt="eLife logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Evolution"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.wiley.com/bw/journal.asp?ref=0014-3820')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/evolution.png" alt="Evolution cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Evolutionary Applications"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                        select="string('http://www.blackwellpublishing.com/eva_enhanced/default.asp')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/EvolApp.png"
                         alt="Evolutionary Applications cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Functional Ecology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.functionalecology.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/FEcover.png"
                        alt="Functional Ecology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "German Medical Science"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.egms.de')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/gms_ejournal.png"
                        alt="German Medical Science cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Heredity"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.nature.com/hdy/index.html')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/heredity.png" alt="Heredity cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "International Journal of Myriapodology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.pensoft.net/journals/ijm/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/ijm.png" alt="International Journal of Myriapodology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Journal of Animal Ecology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.journalofanimalecology.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/JAnimalEcol.png"
                         alt="Journal of Animal Ecology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Journal of Evolutionary Biology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.blackwellpublishing.com/jeb_enhanced/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/EvolBiol.png"
                         alt="Journal of Evolutionary Biology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Journal of Fish and Wildlife Management"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.fwspubs.org/loi/fwma')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/JFWM.png"
                        alt="Journal of Fish and Wildlife Management cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Journal of Heredity"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://jhered.oxfordjournals.org/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/jhered.png"
                        alt="Journal of Heredity cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Journal of Open Public Health Data"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://openpublichealthdata.metajnl.com/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/jophdCover.png"
                        alt="Journal pf Open Public Health Data cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Journal of Paleontology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.journalofpaleontology.org/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/JPALEO.png"
                        alt="Journal of Paleontology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Methods in Ecology and Evolution"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://www.methodsinecologyandevolution.org/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/MEECover.jpg"
                         alt="Methods in Ecology and Evolution cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Molecular Biology and Evolution"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://mbe.oxfordjournals.org/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/mbe.png"
                         alt="Molecular Biology and Evolution cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Molecular Ecology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                        select="string('http://onlinelibrary.wiley.com/journal/10.1111/%28ISSN%291365-294X')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/MolEcol.png"
                         alt="Molecular Ecology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Molecular Ecology Resources"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                        select="string('http://onlinelibrary.wiley.com/journal/10.1111/(ISSN)1755-0998')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/MolEcolRes.png"
                         alt="Molecular Ecology Resources cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Molecular Phylogenetics and Evolution"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                        select="string('http://www.elsevier.com/wps/find/journaldescription.cws_home/622921/description')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/mpe.png"
                         alt="Molecular Phylogenetics and Evolution cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "MycoKeys"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.pensoft.net/journals/mycokeys/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/mycokeys.png"
                        alt="MycoKeys cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Nature Conservation"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.pensoft.net/journals/natureconservation/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/natureconservation.png"
                        alt="Nature Conservation cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "NeoBiota"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.pensoft.net/journals/neobiota/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/neobiota.png"
                        alt="NeoBiota cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Paleobiology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://paleobiol.geoscienceworld.org/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/paleobiology.png"
                        alt="Paleobiology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PhytoKeys"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.pensoft.net/journals/phytokeys/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/phytokeys.png"
                        alt="PhytoKeys cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PLoS Biology" or $journal-name = "PLOS Biology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.plosbiology.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/PLOS-Biology.png"
                        alt="PLOS Biology logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PLoS Computational Biology" or $journal-name = "PLOS Computational Biology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.ploscompbiol.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/PLOS-CompBiology.png"
                        alt="PLOS Computational Biology logo"/>
                </a>
            </xsl:when>
            <xsl:when test='starts-with($journal-name,"PLoS Currents") or starts-with($journal-name,"PLOS Currents")'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.plos.org/publications/currents/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/PLOS-Currents_TreeOfLife.png"
                        alt="PLOS Currents logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PLoS Genetics" or $journal-name = "PLOS Genetics"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.plosgenetics.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/PLOS-Genetics.png"
                        alt="PLOS Genetics logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PLoS Medicine" or $journal-name = "PLOS Medicine"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.plosmedicine.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/pmedicine.png"
                        alt="PLOS Medicine logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PLoS Neglected Tropical Diseases" or $journal-name = "PLOS Neglected Tropical Diseases"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.plosntds.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/pntd.png"
                        alt="PLOS Neglected Tropical Diseases logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PLoS ONE" or $journal-name = "PLOS ONE"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.plosone.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/PLOS-ONE.png"
                        alt="PLOS ONE logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "PLoS Pathogens" or $journal-name = "PLOS Pathogens"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.plospathogens.org')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/PLOS-Pathogens.png"
                        alt="PLOS Pathogens logo"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "Systematic Biology"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                        select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="string('http://sysbio.oxfordjournals.org/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/SystBiol.png"
                         alt="Systematic Biology cover"/>
                </a>
            </xsl:when>
            <xsl:when test='$journal-name = "ZooKeys"'>
                <a>
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="contains($article-doi,'doi:')">
                                <xsl:value-of
                                    select="concat('https://doi.org/', substring-after($article-doi, 'doi:'))"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of
                                    select="string('http://www.pensoft.net/journals/zookeys/')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <img id="journal-logo" src="/themes/Dryad/images/coverimages/zookeys.png"
                        alt="ZooKeys cover"/>
                </a>
            </xsl:when>
            <xsl:otherwise>
                <p></p>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
