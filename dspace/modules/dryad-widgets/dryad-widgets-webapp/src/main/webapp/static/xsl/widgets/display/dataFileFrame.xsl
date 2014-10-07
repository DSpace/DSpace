<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
    xmlns:bibo="http://purl.org/dryad/schema/dryad-bibo/v3.1"
    xmlns:d1="http://ns.dataone.org/service/types/v1" xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:ddf="http://purl.org/dryad/schema/terms/v3.1" xmlns:dri="http://di.tamu.edu/DRI/1.0/"
    xmlns:dwc="http://rs.tdwg.org/dwc/terms/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:java="http://xml.apache.org/xalan/java"
    xmlns:sc="http://xml.apache.org/xalan/java/java.util.Scanner"
    xmlns:u="http://xml.apache.org/xalan/java/java.net.URL"
    exclude-result-prefixes="bibo d1 dcterms ddf dri dwc xsi java"
    version="1.0">

    <xsl:output method="html" indent="yes"/>

    <!--
        <map:parameter name="bitstream-url"     value="{request:scheme}://{request:serverName}:{request:serverPort}/api/v1/widgets/display/dataFileBitstream?referrer={request-param:referrer}&amp;doi={url-encode:{request-param:doi}}"/>
        <map:parameter name="datapackage-url"   value="{request:scheme}://{request:serverName}:{request:serverPort}/resource/{request-param:doi}"/>
        <map:parameter name="datapackage-img"   value="{request:scheme}://{request:serverName}:{request:serverPort}/static/img/bannerForPub.png"/>
        <map:parameter name="ddwcss"            value="{request:scheme}://{request:serverName}:{request:serverPort}/static/css/widgets/display/dryad-ddw.min.css"/>
        <map:parameter name="link1"             value="http://maxcdn.bootstrapcdn.com/font-awesome/4.1.0/css/font-awesome.min.css"/>
        <map:parameter name="link2"             value="http://fonts.googleapis.com/css?family=Source+Sans+Pro:200,300italic,300,400italic,400,600italic,600,700italic,700,900italic,900"/>
        <map:parameter name="referrer"          value="{request-param:referrer}"/>
        <map:parameter name="request-origin"    value="{request-param:origin}"/>
    -->
    <xsl:param name="bitstream-url"/>
    <xsl:param name="download-url"/>
    <xsl:param name="datapackage-url"/>
    <xsl:param name="datapackage-img"/>
    <xsl:param name="ddwcss"/>
    <xsl:param name="link1"/>
    <xsl:param name="link2"/>
    <xsl:param name="request-origin"/>

    <xsl:variable name="view-count"
        select="/parts/dri/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dryad'][@qualifier='pageviews']"/>
    <xsl:variable name="download-count"
        select="/parts/dri/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='dryad'][@qualifier='downloads']"/>
    <xsl:variable name="publication-doi-url" select="/parts/object/ddf:DryadDataFile/ddf:isPartOf"/>
    <xsl:variable name="publication-doi-doi"
        select="substring-after($publication-doi-url,'http://dx.doi.org/')"/>
    <xsl:variable name="format-id" select="/parts/meta-bitstream/d1:systemMetadata/formatId"/>
    <xsl:variable name="title" select="/parts/object/ddf:DryadDataFile/dcterms:title"/>
    <xsl:variable name="description" select="/parts/object/ddf:DryadDataFile/dcterms:description"/>

    <xsl:variable name="doi-prefix" select="'http://dx.doi.org/'"/>
    <xsl:variable name="article-doi"
        select="/parts/dri/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='identifier'][@qualifier='article']"/>
    <xsl:variable name="article-doi-url" select="concat($doi-prefix, $article-doi)"/>
    <xsl:variable name="package-doi"
        select="/parts/dri/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='identifier'][@qualifier='package']"/>
    <xsl:variable name="package-doi-url" select="concat($doi-prefix,$package-doi)"/>

    <xsl:variable name="article-citation"
        select="normalize-space(/parts/dri/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='citation'][@qualifier='article'])"/>
    <xsl:variable name="package-title"
        select="normalize-space(/parts/dri/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='title'][@qualifier='package'])"/>
    <xsl:variable name="article-title" select="substring-after($package-title, 'Data from: ')"/>
    <xsl:variable name="post-title" select="substring-after($article-citation,$article-title)"/>
    <xsl:variable name="author-date" select="substring-before($article-citation, $article-title)"/>
    <xsl:variable name="package-citation"
        select="concat($author-date, $package-title, ' Dryad Digital Repository.')"/>

    <xsl:template match="/">
        <html>
            <head>
                <xsl:call-template name="head-content"/>
            </head>
            <body id="dryad-ddw-body">
                <xsl:call-template name="body-content"/>
                <xsl:call-template name="make-meta"/>
                <xsl:call-template name="script-content"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template name="head-content">
        <link href="{$link1}" rel="stylesheet" type="text/css"/>
        <link href="{$link2}" rel="stylesheet" type="text/css"/>
        <link href="{$ddwcss}" rel="stylesheet" type="text/css"/>
    </xsl:template>

    <xsl:template name="body-content">
        <div class="dryad-ddw">
            <div class="dryad-ddw-header" id="ddw-header">
                <div class="dryad-ddw-banner">
                    <ul>
                        <li>
                            <a target="_blank" href="{$datapackage-url}">
                                <img src="{$datapackage-img}" alt="Data in Dryad"/>
                            </a>
                        </li>
                        <li>
                            <b>
                                <xsl:value-of select="$publication-doi-doi"/>
                            </b>
                        </li>
                    </ul>
                </div>
                <div class="dryad-ddw-title">
                    <h1>
                        <xsl:value-of select="$title"/>
                    </h1>
                    <ul>
                        <li><b><xsl:value-of select="$view-count"/></b> views</li>
                        <li><b><xsl:value-of select="$download-count"/></b> downloads</li>
                    </ul>
                </div>
            </div>
            <div class="dryad-ddw-body" id="ddw-body">
                <div class="dryad-ddw-frame" id="ddw-body-frame">
                    <xsl:call-template name="data-content"/>
                </div>
                <div class="dryad-ddw-control">
                    <ul>
                        <li>
                            <a class="dryad-ddw-zoom" title="Zoom">
                                <i class="fa fa-expand"/>
                            </a>
                        </li>
                        <li>
                            <a class="dryad-ddw-share" title="Share">
                                <i class="fa fa-share-alt"/>
                            </a>
                        </li>
                        <li>
                            <a class="dryad-ddw-download" title="Download" href="{$download-url}">
                                <i class="fa fa-download"/>
                            </a>
                        </li>
                        <li>
                            <a class="dryad-ddw-cite" title="Cite">
                                <i class="fa fa-quote-left"/>
                            </a>
                        </li>
                    </ul>
                </div>
            </div>
            <div class="dryad-ddw-footer" id="ddw-footer">
                <div class="dryad-ddw-control">
                    <ul>
                        <li>
                            <a class="dryad-ddw-zoom" title="Zoom">
                                <i class="fa fa-expand"/>
                            </a>
                        </li>
                        <li>
                            <a class="dryad-ddw-share" title="Share">
                                <i class="fa fa-share-alt"/>
                            </a>
                        </li>
                        <li>
                            <a class="dryad-ddw-download" title="Download" href="{$download-url}">
                                <i class="fa fa-download"/>
                            </a>
                        </li>
                        <li>
                            <a class="dryad-ddw-cite" title="Cite">
                                <i class="fa fa-quote-left"/>
                            </a>
                        </li>
                    </ul>
                </div>
                <p>
                    <xsl:value-of select="$description"/>
                </p>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="make-meta">
        <div id="dryad-ddw-meta" class="dryad-ddw-hide" style="display:none !important;">
            <div id="dryad-ddw-citation" class="dryad-popup dryad-ddw dryad-ddw-citation">
                <div class="dryad-ddw-citation">
                    <div><img src="{$datapackage-img}" alt="Data in Dryad"/></div>
                    <!--<i18n:text>xmlui.DryadItemSummary.whenUsing</i18n:text>-->
                    <div>
                    <xsl:text>When using this data, please cite the original publication:</xsl:text>
                    </div>
                    <p class="shade">
                        <xsl:value-of select="$article-citation"/>
                        <xsl:text> </xsl:text>
                        <a href="{$article-doi-url}">
                            <xsl:value-of select="$article-doi-url"/>
                        </a>
                    </p>
                    <!--<i18n:text>xmlui.DryadItemSummary.pleaseCite</i18n:text>-->
                    <div><xsl:text>Additionally, please cite the Dryad data package: </xsl:text></div>
                    <p class="shade">
                        <xsl:value-of select="$package-citation"/>
                        <xsl:text> </xsl:text>
                        <a href="{$package-doi-url}">
                            <xsl:value-of select="$package-doi-url"/>
                        </a>
                    </p>
                    <p>
                        <!--<i18n:text>xmlui.DryadItemSummary.downloadFormats</i18n:text>-->
                        <xsl:text>Download the data package citation in the following formats:</xsl:text>
                    </p>
                    <ul class="dryad-ddw-citation">
                        <li>
                            <a
                                href="http://datadryad.org/resource/doi:10.5061/dryad.87ht85rs/citation/ris"
                                >RIS</a>
                            <span>
                                <!--<i18n:text>xmlui.DryadItemSummary.risCompatible</i18n:text>-->
                                <xsl:text> (compatible with EndNote, Reference Manager, ProCite, RefWorks)</xsl:text>
                            </span>
                        </li>
                        <li>
                            <a
                                href="http://datadryad.org/resource/doi:10.5061/dryad.87ht85rs/citation/bib"
                                >BibTex</a>
                            <span>
                                <!--<i18n:text>xmlui.DryadItemSummary.bibtexCompatible</i18n:text>-->
                                <xsl:text> (compatible with BibDesk, LaTeX)</xsl:text>
                            </span>
                        </li>
                    </ul>
                </div>
            </div>
            <div id="dryad-ddw-share" class="dryad-popup dryad-ddw dryad-ddw-share">
                <div class="dryad-ddw-share">
                    <img src="{$datapackage-img}" alt="Data in Dryad"/>
                    <ul class="dryad-ddw-share">
                        <li>
                            <xsl:call-template name="reddit-item"/>
                        </li>
                        <li>
                            <xsl:call-template name="twitter-item"/>
                        </li>
                        <li>
                            <xsl:call-template name="facebook-item"/>
                        </li>
                        <li>
                            <xsl:call-template name="mendeley-item"/>
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="reddit-item">
        <a href="http://reddit.com/submit"
            onclick="window.open('http://reddit.com/submit?url='+encodeURIComponent('http://dx.doi.org/doi:10.5061/dryad.87ht85rs')+'&amp;title=Data+from%3A+Robustness+of+compound+Dirichlet+priors+for+Bayesian+inference+of+branch+lengths.+','reddit','toolbar=no,width=550,height=550'); return false">
            <img alt="Reddit" src="http://reddit.com/static/spreddit7.gif" border="0px;"/>
        </a>
    </xsl:template>

    <xsl:template name="twitter-item">
        <iframe id="twitter-widget-0" scrolling="no" frameborder="0" allowtransparency="true"
            src="http://platform.twitter.com/widgets/tweet_button.1406859257.html#_=1407626732783&amp;count=none&amp;id=twitter-widget-0&amp;lang=en&amp;original_referer=http%3A%2F%2Fdatadryad.org%2Fresource%2Fdoi%3A10.5061%2Fdryad.87ht85rs%2F1&amp;size=m&amp;text=USYB-2011-142.SupplData%20from%3A%20Robustness%20of%20compound%20Dirichlet%20priors%20for%20Bayesian%20inference%20of%20branch%20lengths.%20-%20Dryad&amp;url=http%3A%2F%2Fdx.doi.org%2Fdoi%3A10.5061%2Fdryad.87ht85rs&amp;via=datadryad"
            class="twitter-share-button twitter-tweet-button twitter-share-button twitter-count-none"
            title="Twitter Tweet Button" data-twttr-rendered="true"/>
    </xsl:template>

    <xsl:template name="facebook-item">
        <iframe
            src="http://www.facebook.com/plugins/like.php?href=http%3A%2F%2Fdx.doi.org%2Fdoi%3A10.5061%2Fdryad.87ht85rs&amp;layout=button_count&amp;show_faces=false&amp;width=100&amp;action=like&amp;colorscheme=light"
            scrolling="no" frameborder="0"
            style="border:none; overflow:hidden; width:80px;height:21px;" allowtransparency="true"/>
    </xsl:template>

    <xsl:template name="mendeley-item">
        <a
            href="http://www.mendeley.com/import/?url=http://datadryad.org/resource/doi:10.5061/dryad.87ht85rs">
            <img alt="Mendeley" src="http://www.mendeley.com/graphics/mendeley.png" border="0px;"/>
        </a>
    </xsl:template>

    <xsl:template name="script-content">

        <!-- set widget body size to expand to all space between header and footer -->
        <script type="text/javascript"><![CDATA[
            var wh = window.innerHeight;
            var h = document.getElementById('ddw-header');
            var hh = h.clientHeight + parseInt(window.getComputedStyle(h, null).paddingTop);
            var f = document.getElementById('ddw-footer');
            var fh = f.clientHeight + parseInt(window.getComputedStyle(f, null).borderTopWidth) + parseInt(window.getComputedStyle(f, null).borderBottomWidth);
            var b = document.getElementById('ddw-body');
            var bf = document.getElementById('ddw-body-frame');
            b.style.height = (wh - hh - fh).toString() + 'px';
            bf.style.height = '100%';//]]></script>

        <!-- click handlers for buttons -->
        <script type="text/javascript"><![CDATA[
            (function (w, d) {
                'use strict';//]]>
var origin = '<xsl:value-of select="$request-origin"/>'            
<![CDATA[
, downloads = d.getElementsByClassName('dryad-ddw-download'), cites = d.getElementsByClassName('dryad-ddw-cite'), shares = d.getElementsByClassName('dryad-ddw-share'), zooms = d.getElementsByClassName('dryad-ddw-zoom'), elt, i;
            function set_onclick(elts, data) {
                for (i = 0; i < elts.length; i++) {
                    elts[i].onclick = function (evt) {
                        w.parent.postMessage(data, origin);
                        evt.preventDefault();
                    };
                }
            };
            set_onclick(cites, {
                "action": "cite", "data": d.getElementById("dryad-ddw-citation").cloneNode(true).outerHTML
            });
            set_onclick(shares, {
                "action": "share", "data": d.getElementById("dryad-ddw-share").cloneNode(true).outerHTML
            });
            set_onclick(downloads, {
                "action": "download", "data": downloads[0].getAttribute('href')
            });
            var zoomc = d.getElementsByClassName("dryad-ddw")[0].cloneNode(true);
            var controls = zoomc.getElementsByClassName('dryad-ddw-control');
            for (i = 0; i < controls.length; i++) {
                controls[i].parentNode.removeChild(controls[i]);
            }
            zoomc.getElementsByClassName('dryad-ddw-frame')[0].classList.add('dryad-ddw-frame-full');
            set_onclick(zooms, {
                "action": "zoom", "data": zoomc.outerHTML
            });
        })(window, document);//]]></script>
    </xsl:template>

    <xsl:template name="data-content">
        <iframe class="dryad-ddw-data" src="{$bitstream-url}" height="100%" width="100%"/>
    </xsl:template>

</xsl:stylesheet>
