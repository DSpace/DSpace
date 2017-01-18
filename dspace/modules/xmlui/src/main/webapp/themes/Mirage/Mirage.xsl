<?xml version="1.0" encoding="UTF-8"?>
<!--
    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at
    http://www.dspace.org/license/
-->
<!--
    Original author: Alexey Maslov
    Extensively modified by many others....
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:confman="org.dspace.core.ConfigurationManager"
                exclude-result-prefixes="confman dc dim dri i18n mets mods xhtml xlink xsl">

    <xsl:import href="../dri2xhtml-alt/dri2xhtml.xsl"/>
    <xsl:import href="lib/xsl/core/global-variables.xsl"/>
    <xsl:import href="lib/xsl/core/page-structure.xsl"/>
    <xsl:import href="lib/xsl/core/navigation.xsl"/>
    <xsl:import href="lib/xsl/core/elements.xsl"/>
    <xsl:import href="lib/xsl/core/forms.xsl"/>
    <xsl:import href="lib/xsl/core/attribute-handlers.xsl"/>
    <xsl:import href="lib/xsl/core/utils.xsl"/>
    <xsl:import href="lib/xsl/aspect/general/choice-authority-control.xsl"/>
    <xsl:import href="lib/xsl/aspect/administrative/administrative.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/item-list.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/item-view.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/community-list.xsl"/>
    <xsl:import href="lib/xsl/aspect/artifactbrowser/collection-list.xsl"/>
    <xsl:import href="lib/xsl/aspect/JournalLandingPage/main.xsl"/>
    <xsl:import href="integrated-view.xsl"/>
    <xsl:import href="DryadItemSummary.xsl"/>
    <xsl:import href="DryadUtils.xsl"/>
    <xsl:import href="DryadSearch.xsl"/>
    
    <xsl:output indent="yes"/>
    <xsl:variable name="iframe.maxheight" select="confman:getIntProperty('iframe.maxheight', 300)"/>
    <xsl:variable name="iframe.maxwidth" select="confman:getIntProperty('iframe.maxwidth', 600)"/>

    <xsl:template match="dri:body[//dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='URI'] = '' ]">
        <!-- add special style just for the homepage -->
        <style type="text/css">
            /* special style for Dryad homepage only */
            #ds-body {
                width: 100%;
            }

            .labelcell {
                font-weight: bold;
            }

            .datacell {
                text-align: right;
            }

            .ds-div-head a {
                font-size: 0.7em;
                font-weight: normal;
                position: relative;
                top: -0.1em;
            }

            .ds-artifact-list {
                /* font-size: 100%; */
                line-height: 1.4em;
            }

            .artifact-title {
                font-size: 100%;
            }

            .ds-artifact-list .artifact-info {
                display: none;
            }

            /* implied 3 columns @300px width, 25px gutters */
            .home-col-1 {
                float: left;
                width: 625px;
                padding: 0;
                /* margin-right: 25px;*/
            }

            .home-col-2 {
                float: right;
                width: 300px;
                margin-left: 0;
                margin-right: 0;
            }

            .home-top-row {
                height: 220px;
            }

            .home-bottom-row {
                height: 420px;
            }

            #recently_integrated_journals,
            #aspect_statistics_StatisticsTransformer_div_stats,
            #aspect_dryadinfo_DryadBlogFeed_div_blog-hook {
                height: 300px;
                overflow-x: visible;
                overflow-y: scroll;
            }

        #aspect_statistics_StatisticsTransformer_div_stats table {
            width: 100%;
            margin-top: 10px;
        }
        #aspect_statistics_StatisticsTransformer_div_stats .ds-table-row {
	        height: 40px;
	    }
        #aspect_statistics_StatisticsTransformer_div_stats tr.odd td {
	        background-color: #eee;
	    }
        #aspect_statistics_StatisticsTransformer_div_stats th,
        #aspect_statistics_StatisticsTransformer_div_stats td {
            padding: 0 8px;
            text-align: right
        }
        #aspect_statistics_StatisticsTransformer_div_stats td:first-child {
            text-align: left;
        }

        #recently_integrated_journals img.pub-cover {
	        margin: 7px 10px;
	    }

	    #recently_integrated_journals .container {
	        text-align: center;
	    }

            #dryad-home-carousel {
                font-size: 23px;
                font-weight: bold;
                background-color: rgb(255, 255, 255);
                height: 216px;
                padding: 0px;
                overflow: hidden;
            }

            #dryad-home-carousel .bx-viewport {
                height: 194px;
                width: 627px;
            }

            #dryad-home-carousel div.bxslider {
                overflow: visible;
            }

            #dryad-home-carousel div.bxslider div {
                height: 190px;
                padding: 0;
                margin: 0;
                position: relative;
                display: none;
            }

            #dryad-home-carousel div.bxslider div > a > img,
            #dryad-home-carousel div.bxslider div > img {
                display: block;
                height: 194px;
                width: 627px;
            }

            #dryad-home-carousel div.bxslider div .publication-date {
                display: none;
            }

            #dryad-home-carousel div.bxslider div p {
                width: 550px;
                margin: auto;
                margin-top: 1em;
            }

            #dryad-home-carousel .bx-pager {
            }
            #dryad-home-carousel .bx-pager-item {
            }

            #dryad-home-carousel .bx-controls-auto {
                bottom: -16px;
            }
            #dryad-home-carousel .bx-controls-auto-item {
                float: right;
                padding-right: 8px;
            }

            .blog-box ul {
                list-style: none;
                margin-left: 0;
            }

            .blog-box li {
                margin: 0.5em 0 1.2em;
            }
            .home-col-2 #connect-illustrated-prose p {
                line-height: 1.3em;
            }

            #connect-illustrated-prose img {
                width: auto;
                margin: 4px;		
            }

            #aspect_discovery_SiteViewer_field_query {
                width: 85%;
            }

        </style>


        <div id="ds-body">

            <!-- SYSTEM-WIDE ALERT BOX -->
            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>

            <!-- CAROUSEL -->
            <div class="home-col-1">
                <div id="dryad-home-carousel" class="ds-static-div primary">
                    <!-- REMINDER: slide publication dates are in the format YEAR-MONTH-DAY, eg, 2013-12-28 -->
                    <div class="bxslider" style="">
                        <div><span class="publication-date">2015-04-14</span>
                            <a href="/pages/submissionIntegration">
                                <img src="/themes/Mirage/images/integration-slide.jpg" alt="Publishers: Simplify data submission. Strengthen links between articles and data. For free. Integrate your journal with Dryad now" />
                            </a>
                        </div>
                        <div><span class="publication-date">2015-02-15</span>
                            <a href="/pages/dryadlab">
                                <img alt="" src="/themes/Mirage/images/dryadlab-promo.png" />
                                <p style="width: 580px; color: #444; font-size: 80%; top: 75px; right: 10px; line-height: 1.2em; position: absolute; text-shadow: 1px 2px 2px rgba(33, 33, 33, 0.25);"> 
                                    DryadLab is a collection of free, openly-licensed, high-quality, hands-on, educational modules for students to engage in scientific inquiry using real data.
                                </p>
                                <p style="drop-shadow: 4px 4px; position: absolute; right: 40px; bottom: 6px; font-size: 70%; text-align: right; text-shadow: 1px 2px 2px rgba(33, 33, 33, 0.25);">Learn More &#187;</p>
                            </a>
                        </div>
                        <!--><div><span class="publication-date">2015-03-23</span>
                            <a href="/pages/membershipOverview">
                                <img alt="" src="/themes/Mirage/images/watering-can.png" />
                                <p style="width: 450px; color: #363; font-size: 90%; top: 0px; right: 10px; line-height: 1.2em; position: absolute; text-shadow: 1px 2px 2px rgba(33, 33, 33, 0.25);">Help grow open data at Dryad:<br />Become an organizational member</p>
                                <p style="drop-shadow: 4px 4px; position: absolute; right: 40px; bottom: 80px; font-size: 70%; text-align: right; text-shadow: 1px 2px 2px rgba(33, 33, 33, 0.25);">Learn more &#187;</p>
                            </a>
                        </div>-->
                        <div><span class="publication-date">2013-02-01</span>
                            <p Xid="ds-dryad-is" style="font-size: 88%; line-height: 1.35em;"
                               xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/">
                                <span style="color: #595;">DataDryad.org</span>
                                is a
                                <span style="color: #363;">curated general-purpose repository</span>
                                that makes the
                                <span style="color: #242;">data underlying scientific publications</span>
                                discoverable, freely reusable, and citable. Dryad has
                                <span style="color: #595;">integrated data submission</span>
                                for a growing list of journals; submission of data from other publications is also welcome.
                            </p>
                        </div>
                        <div><span class="publication-date">2013-01-01</span>
                            <a href="/pages/repository#keyFeatures">
                                <img src="/themes/Mirage/images/bookmarkSubmissionProcess.png" alt="Deposit data. Get permanent identifier. Watch your citations grow! Relax, your data are discoverable and secure." />
                            </a>
                        </div>
                    </div>
                </div>
            </div>


            <!-- START NEWS -->
            <!--<div class="home-col-2">-->
            <!--<xsl:apply-templates select="dri:div[@id='file.news.div.news']"/>-->
            <!--</div>-->

            <!-- START DEPOSIT -->
            <div id="submit-data-sidebar-box" class="home-col-2 simple-box" style="padding: 8px 34px; width: 230px; margin: 8px 0 12px;">
                <div class="ds-static-div primary" id="file_news_div_news" style="height: 75px;">
                    <p class="ds-paragraph">
		      <!-- The next line should remain as one piece (without linebreaks) to allow it to be matched and replaced with mod_substitute on read-only servers -->
                        <a class="submitnowbutton"><xsl:attribute name="href"><xsl:value-of select="/dri:document/dri:options/dri:list[@n='submitNow']/dri:item[@n='submitnowitem']/dri:xref[@rend='submitnowbutton']/@target"/></xsl:attribute><xsl:text>Submit data now</xsl:text></a>
                    </p>
                    <p style="margin: 14px 0 4px;">
                        <a href="/pages/faq#deposit">How and why?</a>
                    </p>
                </div>
            </div>

            <!-- START SEARCH -->
            <div class="home-col-2">
                <h1 class="ds-div-head">Search for data</h1>

                <form xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                      id="aspect_discovery_SiteViewer_div_front-page-search" class="ds-interactive-div primary" style="overflow: hidden;"
                      action="/discover" method="get" onsubmit="javascript:tSubmit(this);">
                    <p class="ds-paragraph" style="overflow; hidden; margin-bottom: 0px;">
                        <label for="aspect_discovery_SiteViewer_field_query" class="accessibly-hidden">Enter keyword, author, title, DOI</label>
                        <input xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                               id="aspect_discovery_SiteViewer_field_query" class="ds-text-field" name="query"
                               placeholder="Enter keyword, author, title, DOI, etc. Example: herbivory"
                               title="Enter keyword, author, title, DOI, etc. Example: herbivory"
                               type="text" value="" style="width: 224px;"/><!-- no whitespace between these!
                     --><input xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                               id="aspect_discovery_SiteViewer_field_submit" class="ds-button-field" name="submit"
                               type="submit" value="Go" style="margin-right: -4px;"/>
                        <a style="float:left; font-size: 95%;" href="/discover?query=&amp;submit=Search#advanced">Advanced search</a>
                    </p>
                </form>
            </div>

            <!-- START CONNECT  -->
            <div class="home-col-2" style="clear: right;">
                <h1 class="ds-div-head ds_connect_with_dryad_head" id="ds_connect_with_dryad_head">Latest from @datadryad
                </h1>

                <div id="ds_connect_with_dryad" class="ds-static-div primary" style="height: 475px; font-size: 14px;">
                    <div id="connect-illustrated-prose">
		      <a class="twitter-timeline" href="https://twitter.com/datadryad" data-widget-id="572434627277901824">Latest from @datadryad</a>
		      <script>!function(d,s,id){var js,fjs=d.getElementsByTagName(s)[0],p=/^http:/.test(d.location)?'http':'https';if(!d.getElementById(id)){js=d.createElement(s);js.id=id;js.src=p+"://platform.twitter.com/widgets.js";fjs.parentNode.insertBefore(js,fjs);}}(document,"script","twitter-wjs");</script>	
                    </div>
                </div>
            </div>

            <!-- START BROWSE -->
            <div class="home-col-1">
                <h1 class="ds-div-head">Browse for data</h1>
                <div id="browse-data-buttons" class="tab-buttons">
                    <a href="#recently-published-data"><span>Recently published</span></a>

                    <a href="#most-viewed-data"><span>Popular</span></a>
                    <a id="by_author" href="#by-author"><span>By author</span></a>
                    <a id="by_journal" href="#by-journal"><span>By journal</span></a>

                </div>
                <div id="aspect_discovery_RecentlyAdded_div_Home" class="ds-static-div primary" style="height: 649px; overflow: auto;">
                    <div id="recently-published-data" class="browse-data-panel">
                        <xsl:for-each select="dri:div[@n='site-home']">
                            <xsl:apply-templates/>
                        </xsl:for-each>
                    </div>
                    <div id="most-viewed-data" class="browse-data-panel">

                        <xsl:apply-templates select="//dri:document/dri:body/dri:div[@id='aspect.discovery.MostDownloadedBitstream.div.home']"/>

                    </div>

                    <div id="by-author" class="browse-data-panel">
                        <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.discovery.SearchFilterTransformer.div.browse-by-dc.contributor.author_filter']"/>
                        <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.discovery.SearchFilterTransformer.div.browse-by-dc.contributor.author_filter-results']"/>

                    </div>
                    <div id="by-journal" class="browse-data-panel">
                        <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.discovery.SearchFilterTransformer.div.browse-by-prism.publicationName_filter']"/>
                        <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.discovery.SearchFilterTransformer.div.browse-by-prism.publicationName_filter-results']"/>

                    </div>

                </div>
            </div>

            <!-- START MAILING LIST-->
            <div class="home-col-2">
                <h1 class="ds-div-head">Mailing list</h1>
                <div id="file_news_div_mailing_list" class="ds-static-div primary" style="height: 100px;">
                    <!--This form is modified from the iContact sign-up form for Announcements -->
                    <form id="ic_signupform" method="POST" action="https://app.icontact.com/icp/core/mycontacts/signup/designer/form/?id=96&amp;cid=1548100&amp;lid=23049">
                        <p style="margin-bottom: 0px;">Sign up for announcements:</p>
                        <div class="formEl fieldtype-input required" data-validation-type="1" data-label="Email" style="display: inline-block; width: 100%;">
                            <input type="text" placeholder="Your e-mail" title="Your e-mail" name="data[email]" class="ds-text-field" style="width: 240px; margin-top: 8px;" id="file_news_div_mailing_list_input_email"/>
                        </div>
                        <div class="formEl fieldtype-checkbox required" dataname="listGroups" data-validation-type="1" data-label="Lists" style="display: none;">
                            <label class="checkbox"><input type="checkbox" alt="" name="data[listGroups][]" value="42588" checked="checked"/>
                                Dryad-announce
                            </label>
                        </div>
                        <input value="Subscribe" type="submit" name="submit" class="ds-button-field" id="file_news_div_mailing_list_input_subscribe" />
                    </form>
                    <img src="//app.icontact.com/icp/core/signup/tracking.gif?id=96&amp;cid=1548100&amp;lid=23049"/>
                </div>
            </div>

            <!-- START INTEGRATED JOURNAL-->
            <div class="home-col-2" style="clear: both; margin-left: 25px;">
                <h1 class="ds-div-head">Recently integrated journals</h1>
                <div id="recently_integrated_journals" class="ds-static-div primary">
                    <div class="container">
                        <!-- Annals of the Entomological Society of America -->
                        <a class="single-image-link" href="/discover?field=prism.publicationName_filter&amp;query=&amp;fq=prism.publicationName_filter%3Aannals%5C+of%5C+the%5C+entomological%5C+society%5C+of%5C+america%5C%7C%5C%7C%5C%7CAnnals%5C+of%5C+the%5C+Entomological%5C+Society%5C+of%5C+America"><img class="pub-cover" src="/themes/Mirage/images/recentlyIntegrated-AESAME.png" alt="Annals of the Entomological Society of America" /></a>

                        <!-- Environmental Entomology -->
                        <a class="single-image-link" href="/discover?field=prism.publicationName_filter&amp;query=&amp;fq=prism.publicationName_filter%3Aenvironmental%5C+entomology%5C%7C%5C%7C%5C%7CEnvironmental%5C+Entomology"><img class="pub-cover" src="/themes/Mirage/images/recentlyIntegrated-ENVENT.png" alt="Environmental Entomology" /></a>

                        <!-- Journal of Economic Entomology -->
                        <a class="single-image-link" href="/discover?field=prism.publicationName_filter&amp;query=&amp;fq=prism.publicationName_filter%3Ajournal%5C+of%5C+economic%5C+entomology%5C%7C%5C%7C%5C%7CJournal%5C+of%5C+Economic%5C+Entomology"><img class="pub-cover" src="/themes/Mirage/images/recentlyIntegrated-JEENTO.png" alt="Journal of Economic Entomology" /></a>

                        <!-- Journal of Insect Science -->
                        <a class="single-image-link" href="/discover?field=prism.publicationName_filter&amp;query=&amp;fq=prism.publicationName_filter%3Ajournal%5C+of%5C+insect%5C+science%5C%7C%5C%7C%5C%7CJournal%5C+of%5C+Insect%5C+Science"><img class="pub-cover" src="/themes/Mirage/images/recentlyIntegrated-JISESA.png" alt="Journal of Insect Science" /></a>

                    </div>
                </div>
            </div>
            <!-- START STATISTICS -->
            <div class="home-col-2" style="margin-left: 25px;">
                <div id="aspect_statistics_StatisticsTransformer_div_home" class="repository">
                    <h1 class="ds-div-head">Stats</h1>
                    <div xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                         id="aspect_statistics_StatisticsTransformer_div_stats" class="ds-static-div secondary stats">
                        <!--remove old static information and add real data-->
                        <xsl:apply-templates select="/dri:document/dri:body/dri:div[@n='front-page-stats']"/>
                    </div>
                </div>
            </div>
            <!-- START BLOG -->
            <div class="home-col-2">
                <xsl:apply-templates select="dri:div[@id='aspect.dryadinfo.DryadBlogFeed.div.dryad-info-home']"/> 
            </div>
            <div id="SpiderTrap">
                <p>
                    <a href="/spider">Spider.
                    </a>
                </p>
            </div>
        </div>

        <xsl:apply-templates select="dri:div[@id='aspect.eperson.TermsOfService.div.background']"/>
        <xsl:apply-templates select="dri:div[@id='aspect.eperson.TermsOfService.div.modal-content']"/>
    </xsl:template>


    <!--
        The template to handle dri:options. Since it contains only dri:list tags (which carry the actual
        information), the only things than need to be done is creating the ds-options div and applying
        the templates inside it.
        In fact, the only bit of real work this template does is add the search box, which has to be
        handled specially in that it is not actually included in the options div, and is instead built
        from metadata available under pageMeta.
-->

    <xsl:template match="dri:options/dri:list[@n='administrative']"/>
    <xsl:template match="dri:options/dri:list[@n='browse']"/>
    <xsl:template match="dri:options/dri:list[@n='context']"/>
    <xsl:template match="dri:options/dri:list[@n='search']"/>
    <xsl:template match="dri:options/dri:list[@n='account']"/>
    <xsl:template match="dri:options/dri:list[@n='DryadBrowse']"/>
    <!--- Static Navigation Override -->
    <!-- TODO: figure out why i18n tags break the go button -->
    <xsl:template match="dri:options">
        <div id="ds-options-wrapper">
            <div id="ds-options">
                <xsl:variable name="uri" select="string(/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='request'][@qualifier='URI'])"/>
                <xsl:choose>
                    <!-- on the "My Submissions" page, have the "Submit data now" button at top of sidebar -->
                    <xsl:when test="$uri = 'submissions'">
                        <xsl:apply-templates select="dri:list[@n='DryadSubmitData']"/>
                        <xsl:apply-templates select="dri:list[@n='discovery']|dri:list[@n='DryadSearch']|dri:list[@n='DryadConnect']"/>                        
                    </xsl:when>
                    <!-- on the "My Tasks" page, suppress "Submit data now" -->
                    <xsl:when test="$uri = 'my-tasks'">
                        <xsl:apply-templates select="dri:list[@n='discovery']|dri:list[@n='DryadSearch']|dri:list[@n='DryadConnect']"/>                        
                    </xsl:when>
                    <!-- Once the search box is built, the other parts of the options are added -->
                    <xsl:otherwise>
                        <xsl:apply-templates select="dri:list[@n='discovery']|dri:list[@n='DryadSubmitData']|dri:list[@n='DryadSearch']|dri:list[@n='DryadConnect']"/>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:apply-templates select="dri:list[@n='Payment']"/>
                <xsl:apply-templates select="dri:list[@n='need-help']"/>
                <xsl:apply-templates select="dri:list[@n='human-subjects']"/>
                <xsl:apply-templates select="dri:list[@n='large-data-packages']"/>
            </div>
        </div>
    </xsl:template>
    <!--
    <xsl:template match="dri:options/dri:list[@n='DryadInfo']" priority="3">
        <div id="main-menu">
            <ul class="sf-menu">
                <xsl:apply-templates select="dri:list" mode="nested"/>
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </div>
    </xsl:template>
    -->

    <xsl:template match="dri:list" mode="menu">

        <li>
            <a href="#TODO-MenuList">
                <i18n:text>
                    <xsl:value-of select="dri:head"/>
                </i18n:text>
            </a>
            <ul>
                <xsl:apply-templates select="dri:list|dri:item" mode="menu"/>
            </ul>
        </li>

    </xsl:template>


    <xsl:template match="dri:item" mode="menu">

        <li>
            <a href="#TODO-MenuItem">
                <xsl:attribute name="href">
                    <xsl:value-of select="dri:xref/@target"/>
                </xsl:attribute>
                <xsl:apply-templates select="dri:xref/*|dri:xref/text()"/>
            </a>

        </li>

    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='DryadSearch']" priority="3">
      <div class="NOT-simple-box">
        <!-- START SEARCH -->
        <div class="home-col-1">
            <h1 class="ds-div-head">Search for data
            </h1>

            <form xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                  id="aspect_discovery_SiteViewer_div_front-page-search" class="ds-interactive-div primary"
                  action="/discover" method="get" onsubmit="javascript:tSubmit(this);" style="overflow: hidden;">
                <p class="ds-paragraph">
                    <input xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                           id="aspect_discovery_SiteViewer_field_query" class="ds-text-field" name="query"
                           placeholder="Enter keyword, DOI, etc."
                           title="Enter keyword, author, title, DOI, etc. Example: herbivory"
                           type="text" value="" style="width: 175px;"/><!-- no whitespace between these!
                     --><input xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                               id="aspect_discovery_SiteViewer_field_submit" class="ds-button-field" name="submit"
                               type="submit" value="Go" style="margin-right: -4px;"/>
                        <a style="float:left; font-size: 95%;" href="/discover?query=&amp;submit=Search">Advanced search</a>
                </p>
            </form>
        </div>
      </div>
    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='DryadConnect']" priority="3">
      <div class="NOT-simple-box">
        <!-- START CONNECT  -->
        <h1 class="ds-div-head ds_connect_with_dryad_head" id="ds_connect_with_dryad_head">Be part of Dryad
        </h1>
        <div id="ds_connect_with_dryad" class="ds-static-div primary" style="font-size: 14px;">
            <p style="margin-bottom: 0;">
                We encourage organizations to:</p>
				<ul style="list-style: none; margin-left: 1em;">
				<li><a href="/pages/membershipOverview">Become a member</a></li>
				<li><a href="/pages/payment">Sponsor data publishing fees</a></li> 
				<li><a href="/pages/submissionIntegration">Integrate your journal(s)</a>, or</li>
				<li>All of the above</li>
			</ul>
        </div>      
	  </div>
    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='large-data-packages']" priority="3">
        <div class="NOT-simple-box">
            <h1 class="ds-div-head ds_large_data_package_head" id="ds_large_data_package_head">Large data packages</h1>
            <div id="ds_large_data_package" class="ds-static-div primary" style="font-size: 14px;">
                <p style="margin-bottom: 0;">
                    Note that for data packages over 20GB, submitters will
                    be asked to pay $50 for each additional 10GB, or part thereof.
                </p>
            </div>      
        </div>
    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='human-subjects']" priority="3">
        <!-- note margin space added to top here -->
        <div class="NOT-simple-box ds-margin-top-20">
            <h1 class="ds-div-head ds_human_subjects_head" id="ds_human_subjects_head">Got human subject data?</h1>
            <div id="ds_human_subjects" class="ds-static-div primary" style="font-size: 14px;">
                <p style="margin-bottom: 0;">
                    Dryad does not accept submissions that contain personally identifiable 
                    human subject information. Human subject data must be properly anonymized. 
                    <a href="/pages/faq#depositing-acceptable-data">Read more about the kinds of data Dryad accepts</a>.
                </p> 
            </div>      
        </div>
    </xsl:template>


    <xsl:template match="dri:options/dri:list[@n='DryadSubmitData']" priority="3">
      <div id="submit-data-sidebar-box" class="simple-box">
        <!-- START DEPOSIT -->
        <div class="ds-static-div primary" id="file_news_div_news">
            <p class="ds-paragraph">
	      <!-- The next line should remain as one piece (without linebreaks) to allow it to be matched and replaced with mod_substitute on read-only servers -->
                <a class="submitnowbutton"><xsl:attribute name="href"><xsl:value-of select="/dri:document/dri:options/dri:list[@n='submitNow']/dri:item[@n='submitnowitem']/dri:xref[@rend='submitnowbutton']/@target"/></xsl:attribute>Submit data now</a>
            </p>
            <p style="margin: 1em 0 4px;">
                <a href="/pages/faq#deposit">How and why?</a>
            </p>
        </div>
      </div>
    </xsl:template>

    <xsl:template match="dri:options/dri:list[@n='DryadMail']" priority="3">
        <!-- START MAILING LIST-->
        <div class="home-col-2">
            <h1 class="ds-div-head">Dryad mailing list</h1>
            <div id="file_news_div_mailing_list" class="ds-static-div primary">
                <p class="ds-paragraph">
                    <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                </p>
            </div>
        </div>
    </xsl:template>

    <xsl:variable name="meta" select="/dri:document/dri:meta/dri:pageMeta/dri:metadata"/>
    <xsl:variable name="pageName" select="$meta[@element='request'][@qualifier='URI']"/>
    <!--xsl:variable name="doc" select="document(concat('pages/', $pageName, '.xhtml'))"/-->

    <xsl:template match="dri:xref[@rend='embed']">
               
        <xsl:variable name="url" select="concat('pages/',@target)"/>
               
        <xsl:copy-of select="document(string($url))/html/*"/>
           
    </xsl:template>

    <!-- description of dataset for 'Submission overview' page -->
    <xsl:template match="dri:hi[@rend='dataset-description']">
        <p>
            <xsl:value-of select="."/>
        </p>
    </xsl:template>

    <xsl:template match="dri:body/dri:div/dri:list[@id='aspect.submission.StepTransformer.list.submit-progress']"/>


    <!-- First submission form: added and rewrote some templates to manage the form using jquery, to lead the user through the submission -->

    <!-- First submission form: Article Status Radios -->
    <xsl:template match="dri:body/dri:div/dri:list/dri:item[@n='jquery_radios']/dri:field">

        <br/>
        <span>
            <i18n:text>
                <xsl:value-of select="dri:help"/>
            </i18n:text>
        </span>
        <br/>
        <br/>
        <div class="radios">
            <xsl:for-each select="dri:option">
                <input type="radio">
                    <xsl:attribute name="id">
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                    <xsl:attribute name="name">
                        <xsl:value-of select="../@n"/>
                    </xsl:attribute>
                    <xsl:attribute name="value">
                        <xsl:value-of select="@returnValue"/>
                    </xsl:attribute>
                    <xsl:if test="../dri:value[@type='option'][@option = current()/@returnValue]">
                        <xsl:attribute name="checked">checked</xsl:attribute>
                    </xsl:if>
                </input>
                <label>
                    <xsl:attribute name="for">
                        <xsl:value-of select="."/>
                    </xsl:attribute>
                    <i18n:text>
                        <xsl:value-of select="."/>
                    </i18n:text>
                </label>
                <br/>
            </xsl:for-each>
        </div>
    </xsl:template>



    <!-- First submission form: STATUS: PUBLISHED - journalID Select + Manuscript Number Edit Box -->
    <xsl:template match="dri:list[@n='doi']">
        <li id="aspect_submission_StepTransformer_list_doi">
            <table>
                <tr>
                    <td>
                    <xsl:for-each select="dri:item/dri:field">
                        <xsl:variable name="currentId"><xsl:value-of select="@id"/></xsl:variable>
                        <xsl:variable name="currentName"><xsl:value-of select="@n"/></xsl:variable>
                        <xsl:attribute name="id"><xsl:value-of select="$currentName"/></xsl:attribute>

                        <xsl:if test="$currentName!='unknown_doi'">
                            <div style='padding: 0 8px 8px;'>
                                <label class="ds-form-label-select-publication">
                                    <xsl:attribute name="for">
                                        <xsl:value-of select="translate($currentId,'.','_')"/>
                                    </xsl:attribute>
                                    <i18n:text>
                                        <xsl:value-of select="dri:label"/>
                                    </i18n:text>
                                    <xsl:text>: </xsl:text>
                                </label>

                                <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                                <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
                            </div>
                        </xsl:if>

                        <xsl:if test="$currentName='unknown_doi'">
                            <div style="font-weight:bold; border-top: 2px dotted #ccc; border-bottom: 2px dotted #ccc; padding: 3px 0 1px; text-align: center;">
                                OR
                            </div>
                            <div style="padding: 8px;" id="unknown-doi-panel">
                                <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                                <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
                            </div>
                        </xsl:if>

                    </xsl:for-each>
                    </td>
                </tr>
            </table>
        </li>
    </xsl:template>

    <!-- First submission form: STATUS: ACCEPTED/IN REVIEW/NOT_YET_SUBMITTED -->
    <xsl:template match="dri:list/dri:item[@n='select_publication_new' or @n='select_publication_exist']">
        <li>
            <table id="status_other_than_published">
                    <!--xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">
                        <xsl:text>ds-form-item </xsl:text>
                        <xsl:choose>
                        <xsl:when test="position() mod 2 = 0 and not(@rend = 'odd')">even</xsl:when>
                        <xsl:otherwise>odd</xsl:otherwise>
                        </xsl:choose>
                    </xsl:with-param>
                    </xsl:call-template>
                    <div class="ds-form-content">
                    <xsl:if test="dri:field[@type='radio']">
                        <xsl:apply-templates select="dri:field[@type='radio']"/>
                        <br/>
                    </xsl:if-->

                    <!-- RENDER:
                        - JournalID_status_not_yet_submitted
                        - journalID_status_in_review
                        - journalID
                        - MANUSCRIPT NUMBER
                    -->
                    <xsl:for-each select="dri:field[@type='composite']/dri:field">
                        <tr class="selectPubSubmitTable"><td>

                            <xsl:variable name="currentId"><xsl:value-of select="@id"/></xsl:variable>
                            <xsl:variable name="currentName"><xsl:value-of select="@n"/></xsl:variable>
                            <xsl:attribute name="id"><xsl:value-of select="$currentName"/></xsl:attribute>


                            <label class="ds-form-label-select-publication">
                                <xsl:attribute name="for"><xsl:value-of select="translate($currentId,'.','_')"/></xsl:attribute>
                                <i18n:text><xsl:value-of select="dri:label"/></i18n:text>
                                <xsl:text>: </xsl:text>
                            </label>
                            <br/>


                            <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                            <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>


                        </td></tr>
                    </xsl:for-each>

                    <xsl:for-each select="dri:field[@type!='composite']">
                        <xsl:variable name="currentId"><xsl:value-of select="@id"/></xsl:variable>
                        <xsl:variable name="currentName"><xsl:value-of select="@n"/></xsl:variable>

                        <!-- MANUSCRIPT NUMBER STATUS ACCEPTED-->
                        <xsl:if test="$currentName='manu-number-status-accepted'">
                            <tr id="aspect_submission_StepTransformer_item_manu-number-status-accepted">
                                <td>
                                    <label class="ds-form-label-manu-number-status-accepted">
                                        <xsl:attribute name="for">
                                            <xsl:value-of select="translate($currentId,'.','_')"/>
                                        </xsl:attribute>
                                        <i18n:text>
                                            <xsl:value-of select="dri:label"/>
                                        </i18n:text>
                                        <xsl:text>: </xsl:text>
                                    </label>
                                    <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                                    <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
                                </td>
                            </tr>
                        </xsl:if>
                        <xsl:if test="$currentName='publication_select'">
                            <tr id="aspect_submission_StepTransformer_item_manu-number-publication_select">
                                <td>
                                    <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                                    <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
                                </td>
                            </tr>
                        </xsl:if>

                        <!-- CHECKBOX ACCEPTEANCE STATUS ACCEPTED -->
                        <xsl:if test="$currentName='manu_accepted-cb'">
                            <tr id="aspect_submission_StepTransformer_item_manu_accepted-cb">
                                <td>
                                    <xsl:apply-templates select="../dri:field[@id=$currentId]"/>
                                    <xsl:apply-templates select="../dri:field[@id=$currentId]/dri:error"/>
                                </td>
                            </tr>
                        </xsl:if>



                    </xsl:for-each>
            </table>
        </li>
    </xsl:template>
    <!-- END First submission form: added and rewrote some templates to manage the form using jquery, to lead the user through the submission -->
    <!-- Here we construct Dryad's search results tabs; externally harvested
collections are each given a tab. Collection values of these collections
(l3 for instance... this is just a code assigned by DSpace) are hard-coded
so we need to make sure a collection has the same code across different
Dryad installs (dev, demo, staging, production, etc.) -->
    <xsl:template match="dri:referenceSet[@type = 'summaryList']"
                  priority="2">
        <xsl:apply-templates select="dri:head" />
        <!-- Here we decide whether we have a hierarchical list or a flat one -->
        <xsl:choose>
            <xsl:when
                    test="descendant-or-self::dri:referenceSet/@rend='hierarchy' or ancestor::dri:referenceSet/@rend='hierarchy'">
                <ul>
                    <xsl:apply-templates select="*[not(name()='head')]"
                                         mode="summaryList" />
                </ul>
            </xsl:when>
            <xsl:otherwise>
                <xsl:if test="$meta[@element='request'][@qualifier='URI'][.='discover']">


                    <!-- The tabs display a selected tab based on the location
parameter that is being used (see variable defined above) -->
                </xsl:if>
                <ul class="ds-artifact-list">
                    <xsl:choose>
                        <xsl:when test="$meta[@element='request'][@qualifier='URI'][.='submissions']">
                            <xsl:apply-templates select="*[not(name()='head')]"
                                                 mode="summaryNonArchivedList" />
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:apply-templates select="*[not(name()='head')]"
                                                 mode="summaryList" />
                        </xsl:otherwise>
                    </xsl:choose>
                </ul>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="buildTabs">

        <xsl:for-each select="/dri:document/dri:body/dri:div/dri:div/dri:list[@n='tabs']/dri:item">

            <xsl:element name="li">

                <xsl:if test="dri:field[@n='selected']">
                    <xsl:attribute name="id">selected</xsl:attribute>

                </xsl:if>
                <xsl:element name="a">
                    <xsl:attribute name="href">
                        <xsl:value-of select="dri:xref/@target"/>
                    </xsl:attribute>


                    <xsl:value-of select="dri:xref/text()"/>

                </xsl:element>
            </xsl:element>

        </xsl:for-each>

    </xsl:template>




    <!--
<xsl:template match="/dri:document/dri:body/dri:div/dri:div[@id='aspect.discovery.SimpleSearch.div.search-results']/dri:list">
</xsl:template>
-->
    <xsl:template match="/dri:document/dri:body/dri:div/dri:div/dri:list[@n='tabs']">
        <div id="searchTabs">
            <ul>
                <xsl:call-template name="buildTabs"/>


            </ul>
        </div>
    </xsl:template>


    <xsl:template match="/dri:document/dri:body/dri:div/dri:div/dri:list[@n='search-query']/dri:item[position()=1]">
        <li class="ds-form-item">
            <label class="ds-form-label" for="aspect_discovery_SimpleSearch_field_query"><i18n:text><xsl:value-of select="dri:field/dri:label"/></i18n:text></label>
            <div class="ds-form-content">
                <xsl:apply-templates/>
                <!-- Place the 'Go' button beside the search field -->
                <input class="ds-button-field " name="submit" type="submit" i18n:attr="value"
                       value="xmlui.general.go">
                </input>
            </div>
        </li>
        <li class="ds-form-item">
            <a id="advanced-search" href="#">Advanced search</a>
        </li>
    </xsl:template>


    <xsl:template match="/dri:document/dri:body/dri:div/dri:list[@id='aspect.submission.StepTransformer.list.submit-select-publication']/dri:head">
        <legend>
            <i18n:text><xsl:value-of select="."/></i18n:text>
        </legend>
    </xsl:template>
    <xsl:template match="/dri:document/dri:body/dri:div/dri:list[@id='aspect.submission.StepTransformer.list.submit-upload-file']/dri:head">
        <legend>
            <i18n:text><xsl:value-of select="."/></i18n:text>
        </legend>
    </xsl:template>

    <xsl:template match="/dri:document/dri:body/dri:div/dri:list[@id='aspect.submission.StepTransformer.list.submit-describe-dataset']/dri:head">
        <legend>
            <i18n:text><xsl:value-of select="."/></i18n:text>
        </legend>
    </xsl:template>

    <xsl:template match="/dri:document/dri:body/dri:div/dri:list[@id='aspect.submission.StepTransformer.list.submit-overview-file']/dri:head">
        <legend>
            <i18n:text><xsl:value-of select="."/></i18n:text>
        </legend>
    </xsl:template>

    <xsl:template match="dri:list[@id='aspect.submission.StepTransformer.list.submit-upload-file']">
        <fieldset>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <!-- Provision for the sub list -->
                    <xsl:text>ds-form-</xsl:text>
                    <xsl:if test="ancestor::dri:list[@type='form']">
                        <xsl:text>sub</xsl:text>
                    </xsl:if>
                    <xsl:text>list </xsl:text>
                    <xsl:if test="count(dri:item) > 3">
                        <xsl:text>thick </xsl:text>
                    </xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:head"/>

            <xsl:apply-templates select="dri:item[@id='aspect.submission.StepTransformer.item.data-upload-details']"/>

            <table class="datafiletable">
                <tr>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-item']/dri:field[@type='radio']"
                                />
                    </td>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-item']/*[not(@type='radio')]"
                                />
                    </td>
                </tr>
                <tr>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-identifier']/dri:field[@type='radio']"
                                />
                    </td>
                    <td>
                        <xsl:apply-templates
                                select="dri:item[@id='aspect.submission.StepTransformer.item.dataset-identifier']/*[not(@type='radio')]"
                                />
                    </td>
                </tr>
            </table>
        </fieldset>
    </xsl:template>
    <xsl:template match="dri:item[@id='aspect.submission.StepTransformer.item.data-upload-details']">
        <div class="ds-form-content">
            <i18n:text>
                <xsl:value-of select="."/>
            </i18n:text>
        </div>
    </xsl:template>
    <!-- remove old dryad tooltip style help text-->
    <!--xsl:template match="dri:help" mode="compositeComponent">
        <xsl:if
                test="not(ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset'])">
            <span class="composite-help">
                <xsl:if
                        test="ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset']">
                    <xsl:variable name="translatedParentId">
                        <xsl:value-of select="translate(../@id, '.', '_')"/>
                    </xsl:variable>
                    <xsl:attribute name="connectId">
                        <xsl:value-of select="$translatedParentId"/>
                    </xsl:attribute>
                    <xsl:attribute name="id"><xsl:value-of select="$translatedParentId"
                            />_tooltip
                    </xsl:attribute>
                </xsl:if>
                <xsl:apply-templates/>
            </span>
        </xsl:if>
    </xsl:template-->
    <!--add hidden class to help text-->
    <xsl:template match="dri:help" mode="compositeComponent">
        <xsl:choose>
            <xsl:when test="ancestor::dri:div[@id='aspect.dryadfeedback.MembershipApplicationForm.div.membership-form']"/>
            <xsl:otherwise>
                <span class="composite-help">
                    <xsl:if test="ancestor::dri:field[@rend='hidden']">
                        <xsl:attribute name="class">
                            <xsl:text>hidden</xsl:text>
                        </xsl:attribute>
                    </xsl:if>
                    <xsl:apply-templates />
                </span>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="dri:help">
        <xsl:choose>
            <!-- only display <help> in tooltip for feedback form -->
            <xsl:when test="ancestor::dri:div[@id='aspect.artifactbrowser.FeedbackForm.div.feedback-form']"/>
            
            <xsl:when test="not(ancestor::dri:div[@id='aspect.submission.StepTransformer.div.submit-describe-publication' or @id= 'aspect.submission.StepTransformer.div.submit-describe-dataset' or @id= 'aspect.submission.StepTransformer.div.submit-select-publication' or @id= 'aspect.dryadfeedback.MembershipApplicationForm.div.membership-form' or @id= 'aspect.artifactbrowser.FeedbackForm.div.feedback-form'])">
                <!--Only create the <span> if there is content in the <dri:help> node-->
                <xsl:if test="./text() or ./node()">
                    <span>
                        <xsl:attribute name="class">
                            <xsl:text>field-help</xsl:text>
                        </xsl:attribute>
                        <xsl:if test="ancestor::dri:field[@rend='hidden']">
                            <xsl:attribute name="class">
                                <xsl:text>hidden</xsl:text>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:apply-templates/>
                    </span>
                </xsl:if>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template match="/dri:document/dri:body/dri:div/dri:div/dri:list[@n='most_recent' or @n='link-to-button']">
        <div class="link-to-button">
            <xsl:apply-templates select="dri:item"/>
        </div>
    </xsl:template>

    <xsl:template match="//dri:document/dri:body/dri:div[@id='aspect.discovery.MostDownloadedBitstream.div.home']">
        <div id="aspect_discovery_MostDownloadedBitstream_table_most-downloaded">
            <xsl:apply-templates select="./dri:div/dri:head"/>
            <table>
                <tr>
                    <th><xsl:apply-templates select="./dri:div/dri:div[@n='items']/dri:head"/></th>
                    <th><xsl:apply-templates select="./dri:div/dri:div[@n='count']/dri:head"/></th>
                </tr>
                <xsl:for-each select="./dri:div/dri:div[@n='items']/dri:referenceSet/dri:reference">
                    <xsl:variable name="position">
                        <xsl:value-of select="position()"/>
                    </xsl:variable>
                    <tr>
                        <td><xsl:apply-templates select="." mode="summaryList"/></td>
                        <td><xsl:apply-templates select="//dri:document/dri:body/dri:div[@id='aspect.discovery.MostDownloadedBitstream.div.home']/dri:div/dri:div[@n='count']/dri:list/dri:item[position()=$position]"/></td>
                    </tr>
                </xsl:for-each>

            </table>
        </div>
    </xsl:template>

    <!--add table for updated file information-->
    <xsl:template match="/dri:document/dri:body/dri:div/dri:list/dri:item[@id='aspect.submission.StepTransformer.item.bitstream-item']">
        <table><tr>
            <xsl:for-each select="./dri:hi[@rend='head']">
                <th>
                    <xsl:apply-templates/>
                </th>
            </xsl:for-each>
        </tr>
            <tr>
                <xsl:for-each select="./dri:hi[@rend='content']">
                    <td>
                        <xsl:apply-templates/>
                    </td>
                </xsl:for-each>
            </tr>
        </table>
        <xsl:apply-templates select="./dri:field"/>
    </xsl:template>

    <!--add table for updated readme file information-->
    <xsl:template match="/dri:document/dri:body/dri:div/dri:list/dri:item[@id='aspect.submission.StepTransformer.item.submission-file-dc_readme']">
        <li class="ds-form-item odd">
            <span class="ds-form-label"><xsl:value-of select="../dri:label[position()=1]"/></span>
            <table style="clear:both"><tr>
                <xsl:for-each select="./dri:hi[@rend='head']">
                    <th>
                        <xsl:apply-templates/>
                    </th>
                </xsl:for-each>
            </tr>
                <tr>
                    <xsl:for-each select="./dri:hi[@rend='content']">
                        <td>
                            <xsl:apply-templates/>
                        </td>
                    </xsl:for-each>
                </tr>
            </table>
            <xsl:apply-templates select="./dri:field"/>
        </li>
    </xsl:template>

    <!-- Add Empty select option if no authors listed.  Prevents Subject Keywords from breaking -->
    <xsl:template match="/dri:document/dri:body/dri:div/dri:list/dri:item/dri:field[@id='aspect.submission.StepTransformer.field.dc_contributor_correspondingAuthor' and @type='select']">
        <select class="ds-select-field">
            <xsl:apply-templates/>
            <xsl:if test="not(dri:option)">
                <option value=""/>
            </xsl:if>
        </select>
    </xsl:template>

     <!--add attribute placeholder and title-->
    <xsl:template match="/dri:document/dri:body/dri:div/dri:list/dri:item/dri:field/dri:field[@id='aspect.submission.StepTransformer.field.datafile_identifier']" mode="normalField">
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="placeholder">
                <xsl:text>External file identifier</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="title">
                <xsl:text>External file identifier</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </input>
    </xsl:template>

     <!--add attribute placeholder and title for other repository-->
    <xsl:template match="/dri:document/dri:body/dri:div/dri:list/dri:item/dri:field/dri:field[@id='aspect.submission.StepTransformer.field.other_repo_name']" mode="normalField">
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:attribute name="placeholder">
                <xsl:text>Repository name</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="title">
                <xsl:text>Repository name</xsl:text>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </input>
    </xsl:template>

    <!--payment-->

    <xsl:template match="//dri:item[@id='aspect.paymentsystem.ShoppingCartTransformer.item.country-list' or @id='aspect.paymentsystem.ShoppingCartTransformer.item.voucher-list']">

        <li>
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:attribute name="class">
                <xsl:value-of select="@rend"/>
            </xsl:attribute>
            <div class="label">
                <xsl:if test="string-length(dri:field/dri:label)>0">
                    <i18n:text><xsl:value-of select="dri:field/dri:label"/></i18n:text>
                </xsl:if>
            </div>
            <div class="help-title">
                <xsl:if test="string-length(dri:field/dri:help)>0">
                    <img class="label-mark" src="/themes/Mirage/images/help.jpg">
                        <xsl:attribute name="title">
                            <xsl:value-of select="dri:field/dri:help"/>
                        </xsl:attribute>
                    </img>
                </xsl:if>
            </div>
            <xsl:apply-templates select="*"/>
        </li>
    </xsl:template>



    <xsl:template match="//dri:field[@id='aspect.paymentsystem.ShoppingCartTransformer.field.voucher']">
        <input>
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:attribute name="value">
                <xsl:value-of select="@value"/>
            </xsl:attribute>
            <xsl:attribute name="id">
                <xsl:value-of select="translate(@id,'.','_')"/>
            </xsl:attribute>
        </input>
    </xsl:template>



    <xsl:template match="//dri:field[@id='aspect.paymentsystem.ShoppingCartTransformer.field.currency' or @id='aspect.paymentsystem.ShoppingCartTransformer.field.country']">
    <select onchange="javascript:updateOrder()">
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:apply-templates select="*"/>
        </select>
    </xsl:template>
    <xsl:template match="//dri:field[@id='aspect.submission.StepTransformer.field.country']">
        <select onchange="javascript:updateCountry()">
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:apply-templates select="*"/>
        </select>
    </xsl:template>
    <xsl:template match="//dri:field[@id='aspect.paymentsystem.ShoppingCartTransformer.field.apply']">
        <button onclick="javascript:updateOrder()" class="ds-button-field">
            <xsl:attribute name="name">
                <xsl:value-of select="@n"/>
            </xsl:attribute>
            <xsl:value-of select="@n"/>
        </button>
    </xsl:template>


    <xsl:template match="//dri:list[@id='aspect.paymentsystem.PayPalConfirmationTransformer.list.paypal-form']">
        <form action="https://pilot-payflowpro.paypal.com/" method="post">
            <xsl:apply-templates select="*"/>
        </form>
    </xsl:template>


    <xsl:template match="//dri:div[@n='paypal-iframe']">
        <iframe name="paypal-iframe" scrolling="no" id="paypal-iframe">
            <xsl:attribute name="src">
                <xsl:value-of select="dri:list/dri:item[@n='link']" />
                <xsl:text disable-output-escaping="yes">?MODE=</xsl:text>
                <xsl:value-of select="dri:list/dri:item[@n='testMode']" />
                <xsl:text>&amp;SECURETOKENID=</xsl:text>
                <xsl:value-of select="dri:list/dri:item[@n='secureTokenId']" />
                <xsl:text disable-output-escaping="yes">&amp;SECURETOKEN=</xsl:text>
                <xsl:value-of select="dri:list/dri:item[@n='secureToken']" />
            </xsl:attribute>
            <xsl:attribute name="width">
                <xsl:value-of select="$iframe.maxwidth"/>
            </xsl:attribute>
            <xsl:attribute name="height">
                <xsl:value-of select="$iframe.maxheight"/>
            </xsl:attribute>
              error when load payment form
        </iframe>
    </xsl:template>

    <xsl:template match="//dri:list[@n='voucher-list']">
                 <xsl:apply-templates/>
    </xsl:template>
    
    <!-- make sure search labels appear -->
    <xsl:template name="search_labels">
        <xsl:variable name="currentId">
          <xsl:value-of select="./@id" />
        </xsl:variable>
        <label style="font-weight: normal;">
          <xsl:attribute name="for">
              <xsl:value-of select="translate($currentId,'.','_')"/>
          </xsl:attribute>
          <i18n:text>
              <xsl:value-of select="dri:label"/>
          </i18n:text>
        </label>
        <xsl:apply-templates select="." mode="normalField"/>
    </xsl:template>


    <xsl:template match="dri:table[@id='aspect.discovery.SimpleSearch.table.search-controls']/dri:row/dri:cell/dri:field[@type='select']">
      <xsl:call-template name="search_labels" />
    </xsl:template>

    <xsl:template match="dri:field[@rend='starts_with' and @type='text']">
      <xsl:call-template name="search_labels" />
    </xsl:template>

    <xsl:template match="dri:div[@id='full-stacktrace']">
        <xsl:comment>
            <xsl:value-of select="."/>
        </xsl:comment>
    </xsl:template>
        

    <!-- remove voucher link -->
    <xsl:template match="//dri:item[@id='aspect.paymentsystem.ShoppingCartTransformer.item.remove-voucher']/dri:xref">
        <a id="remove-voucher" href="#">
            <xsl:attribute name="onclick">
                <xsl:text>javascript:removeVoucher()</xsl:text>
            </xsl:attribute>
            <xsl:value-of select="."/>&#160;
        </a>
    </xsl:template>



    <!-- remove country link -->
    <xsl:template match="//dri:item[@id='aspect.paymentsystem.ShoppingCartTransformer.item.remove-country']/dri:xref">
        <a id="remove-country" href="#">
            <xsl:attribute name="onclick">
                <xsl:text>javascript:removeCountry()</xsl:text>
            </xsl:attribute>
            <xsl:value-of select="."/>&#160;
        </a>
    </xsl:template>

    <!-- Add 'return' link to propagate-metadata form-->
    <xsl:template match="dri:field[@id='aspect.administrative.item.PropagateItemMetadataForm.field.submit_return']" mode="normalField">
        <a href="#">
            <xsl:attribute name="onclick">
                <xsl:text>javascript:DryadClosePropagate()</xsl:text>
            </xsl:attribute>
                <i18n:text>
                    <xsl:value-of select="."/>
                </i18n:text>
        </a>
    </xsl:template>

  <xsl:template match="dri:p[@rend='edit-metadata-actions bottom']">
    <xsl:apply-templates />
    <!-- Propagate metadata buttons can be clicked from either admin or curator edit metdata interfaces -->
    <!-- The DRI structure is similar but the elements have different IDs -->
    <xsl:variable name="propagateShowPopupAdmin" select="//dri:field[@id='aspect.administrative.item.EditItemMetadataForm.field.propagate_show_popup']/dri:value[@type='raw']"></xsl:variable>
    <xsl:variable name="propagateShowPopupCurator" select="//dri:field[@id='aspect.submission.submit.CuratorEditMetadataForm.field.propagate_show_popup']/dri:value[@type='raw']"></xsl:variable>
    <xsl:choose>
      <xsl:when test="$propagateShowPopupAdmin = '1'">
        <xsl:call-template name="popupPropagateMetadata">
          <xsl:with-param name="packageDoi" select="//dri:row[@id='aspect.administrative.item.EditItemMetadataForm.row.dc_identifier']/dri:cell/dri:field[@type='textarea']/dri:value[@type='raw']"></xsl:with-param>
          <xsl:with-param name="fileDois" select="//dri:row[@id='aspect.administrative.item.EditItemMetadataForm.row.dc_relation_haspart']/dri:cell/dri:field[@type='textarea']/dri:value[@type='raw']"></xsl:with-param>
          <xsl:with-param name="metadataFieldName" select="//dri:field[@id='aspect.administrative.item.EditItemMetadataForm.field.propagate_md_field']/dri:value[@type='raw']"></xsl:with-param>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$propagateShowPopupCurator = '1'">
        <xsl:call-template name="popupPropagateMetadata">
          <xsl:with-param name="packageDoi" select="//dri:row[@id='aspect.submission.submit.CuratorEditMetadataForm.row.dc_identifier']/dri:cell/dri:field[@type='textarea']/dri:value[@type='raw']"></xsl:with-param>
          <xsl:with-param name="fileDois" select="//dri:row[@id='aspect.submission.submit.CuratorEditMetadataForm.row.dc_relation_haspart']/dri:cell/dri:field[@type='textarea']/dri:value[@type='raw']"></xsl:with-param>
          <xsl:with-param name="metadataFieldName" select="//dri:field[@id='aspect.submission.submit.CuratorEditMetadataForm.field.propagate_md_field']/dri:value[@type='raw']"></xsl:with-param>
        </xsl:call-template>
      </xsl:when>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template name="popupPropagateMetadata">
    <xsl:param name="fileDois"/>
    <xsl:param name="metadataFieldName"/>
    <xsl:param name="packageDoi"/>
      <xsl:if test="count($fileDois) > 0">
        <script>
          <xsl:attribute name="type"><xsl:text>text/javascript</xsl:text></xsl:attribute>
          <xsl:text>runAfterJSImports.add( function() { DryadShowPropagateMetadata('</xsl:text>
          <xsl:value-of select="concat($context-path,'/admin/item/propagate-metadata')" />
          <xsl:text>','</xsl:text>
          <xsl:value-of select="$metadataFieldName" />
          <xsl:text>','</xsl:text>
          <xsl:value-of select="$packageDoi" />
          <xsl:text>'); } );</xsl:text>
        </script>
      </xsl:if>
  
  </xsl:template>


    <xsl:template match="//dri:item[@rend='total']">
        <li xmlns:i18n="http://apache.org/cocoon/i18n/2.1" class="ds-form-item odd total">
            <xsl:attribute name="id">
                <xsl:value-of select="translate(@id,'.','_')"/>
            </xsl:attribute>
            <span class="ds-form-label">Your total
                <img src="/themes/Mirage/images/help.jpg" class="label-mark">
                    <xsl:attribute name="title">xmlui.PaymentSystem.shoppingcart.order.help.title</xsl:attribute>
                    <xsl:attribute name="attr" namespace="http://apache.org/cocoon/i18n/2.1">title</xsl:attribute>
                </img>
                :
            </span>
            <div class="ds-form-content"><xsl:value-of select="."/></div>
        </li>
    </xsl:template>

    <!-- Confirmations for destructive buttons -->
    <xsl:template name="destructiveSubmitButton">
      <xsl:param name="confirmationText" select="'Are you sure?'" />
        <!-- Adapted from normalField in dri2xhtml-alt/core/forms.xsl -->
        <xsl:variable name="submitButtonId" select="translate(@id,'.','_')"/>
        <input>
            <xsl:call-template name="fieldAttributes"/>
            <xsl:if test="@type='button'">
                <xsl:attribute name="type">submit</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="value">
                <xsl:choose>
                    <xsl:when test="./dri:value[@type='raw']">
                        <xsl:value-of select="./dri:value[@type='raw']"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="./dri:value[@type='default']"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:attribute>
            <xsl:if test="dri:value/i18n:text">
                <xsl:attribute name="i18n:attr">value</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="onclick">
                <xsl:text>if(confirm('</xsl:text><!--
                --><xsl:value-of select="$confirmationText" /><!--
                --><xsl:text>')){ </xsl:text>
                <xsl:text>  jQuery('#</xsl:text><!--
                --><xsl:value-of select="$submitButtonId" /><!--
                --><xsl:text>').submit(); } else {</xsl:text>
                <xsl:text>return false;</xsl:text>
                <xsl:text>}</xsl:text>
            </xsl:attribute>
            <xsl:apply-templates />
        </input>
    </xsl:template>

    <!-- Confirm before lifting embargo -->
    <xsl:template match="//dri:field[@id='aspect.administrative.item.EditItemEmbargoForm.field.submit_lift_embargo']">
        <xsl:call-template name="destructiveSubmitButton">
            <xsl:with-param name="confirmationText" select="'Are you sure you would like to lift this embargo now?'" />
        </xsl:call-template>
    </xsl:template>

    <!-- Confirm before deleting data files in submission overview -->
    <xsl:template match="//dri:field[starts-with(@id,'aspect.submission.submit.OverviewStep.field.submit_delete_dataset')]">
        <xsl:call-template name="destructiveSubmitButton">
            <xsl:with-param name="confirmationText" select="'Are you sure you would like to delete this Data file?'" />
        </xsl:call-template>
    </xsl:template>

</xsl:stylesheet>
