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
   <!--                     <xsl:for-each select="dri:div[@n='site-home']">
                            <xsl:apply-templates/>
                        </xsl:for-each>
-->
<!-- Start of temp Recently Added -->

<h1 class="ds-div-head">Recently published
data  <a xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" href="/feed/atom_1.0/10255/3" class="single-image-link" title="Web feed of data packages recently added to Dryad">
<img src="/themes/Dryad/images/rss.jpg" style="border: 0px;" alt="RSS feed - Recently published data" />
</a>
</h1>

<div id="aspect_discovery_SiteRecentSubmissions_div_site-recent-submission" class="ds-static-div secondary recent-submission">
<ul class="ds-artifact-list">

<li class="ds-artifact-item odd">
<div xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:mets="http://www.loc.gov/METS/" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" style="padding: 6px;" class="artifact-description">
<a href="/resource/doi:10.5061/dryad.bn1gf">
<span class="author">Correia PA, Lottem E, Banerjee D, Machado AS, Carey MR, Mainen ZF</span>
<span class="artifact-title">Data from: Transient inhibition and long-term facilitation of locomotion by phasic optogenetic activation of serotonin neurons. </span>
<span class="italics">eLife</span> <span class="doi">http://dx.doi.org/10.5061/dryad.bn1gf</span>
</a>
<span class="Z3988" title="ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;rft_id=doi%3A10.5061%2Fdryad.bn1gf&amp;rft_id=2050-084X&amp;rft_id=eLife-20975&amp;rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;rft.contributor=Correia%2C+Patr%C3%ADcia+A.&amp;rft.contributor=Lottem%2C+Eran&amp;rft.contributor=Banerjee%2C+Dhruba&amp;rft.contributor=Machado%2C+Ana+S.&amp;rft.contributor=Carey%2C+Megan+R.&amp;rft.contributor=Mainen%2C+Zachary+F.&amp;rft.coverage=Portugal&amp;rft.identifier=doi%3A10.5061%2Fdryad.bn1gf&amp;rft.identifier=2050-084X&amp;rft.description=Serotonin+%285-HT%29+is+associated+with+mood+and+motivation+but+the+function+of+endogenous+5-HT+remains+controversial.+Here%2C+we+studied+the+impact+of+phasic+optogenetic+activation+of+5-HT+neurons+in+mice+over+time+scales+from+seconds+to+weeks.+We+found+that+activating+dorsal+raphe+nucleus+%28DRN%29+5-HT+neurons+induced+a+strong+suppression+of+spontaneous+locomotor+behavior+in+the+open+field+with+rapid+kinetics+%28onset+%7Bless+than+or+equal+to%7D+1+s%29.+Inhibition+of+locomotion+was+independent+of+measures+of+anxiety+or+motor+impairment+and+could+be+overcome+by+strong+motivational+drive.+Repetitive+place-contingent+pairing+of+activation+caused+neither+place+preference+nor+aversion.+However%2C+repeated+15+min+daily+stimulation+caused+a+persistent+increase+in+spontaneous+locomotion+to+emerge+over+three+weeks.+These+results+show+that+5-HT+transients+have+strong+and+opposing+short+and+long-term+effects+on+motor+behavior+that+appear+to+arise+from+effects+on+the+underlying+factors+that+motivate+actions.&amp;rft.relation=doi%3A10.5061%2Fdryad.bn1gf%2F1&amp;rft.subject=serotonin&amp;rft.subject=dorsal+raphe+nucleus&amp;rft.subject=locomotion&amp;rft.subject=optogenetics&amp;rft.subject=mouse&amp;rft.title=Data+from%3A+Transient+inhibition+and+long-term+facilitation+of+locomotion+by+phasic+optogenetic+activation+of+serotonin+neurons&amp;rft.type=Article&amp;rft.contributor=Correia%2C+Patr%C3%ADcia+A.&amp;rft.identifier=eLife-20975&amp;rft.publicationName=eLife&amp;rft.archive=editorial%40elifesciences.org&amp;rft.archive=automated-messages%40datadryad.org&amp;rft.submit=false&amp;rft.review=editorial%40elifesciences.org&amp;rft.review=automated-messages%40datadryad.org">
                 
</span>
</div>
</li>

<li class="ds-artifact-item even">
<div xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:mets="http://www.loc.gov/METS/" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" style="padding: 6px;" class="artifact-description">
<a href="/resource/doi:10.5061/dryad.869f2">
<span class="author">Garrido E, Díaz MF, Bernal H, Ñústez CE, Thaler J, Jander G, Poveda K</span>
<span class="pub-date"> (2017) </span>
<span class="artifact-title">Data from: Costs and tradeoffs of resistance and tolerance to belowground herbivory in potato. </span>
<span class="italics">PLOS ONE</span> <span class="doi">http://dx.doi.org/10.5061/dryad.869f2</span>
</a>
<span class="Z3988" title="ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;rft_id=doi%3A10.5061%2Fdryad.869f2&amp;rft_id=Garrido+E%2C+D%C3%ADaz+MF%2C+Bernal+H%2C+%C3%91ustez+CE%2C+Thaler+J%2C+Jander+G%2C+Poveda+K+%282017%29+Costs+and+Tradeoffs+of+Resistance+and+Tolerance+to+Belowground+Herbivory+in+Potato.+PLOS+ONE+12%281%29%3A+e0169083.&amp;rft_id=1932-6203&amp;rft_id=PONE-D-16-19771&amp;rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;rft.contributor=Garrido%2C+Etzel&amp;rft.contributor=D%C3%ADaz%2C+Mar%C3%ADa+F.&amp;rft.contributor=Bernal%2C+Hugo&amp;rft.contributor=%C3%91%C3%BAstez%2C+Carlos+E.&amp;rft.contributor=Thaler%2C+Jennifer&amp;rft.contributor=Jander%2C+Georg&amp;rft.contributor=Poveda%2C+Katja&amp;rft.coverage=Colombia&amp;rft.date=2017-01-17&amp;rft.identifier=doi%3A10.5061%2Fdryad.869f2&amp;rft.identifier=Garrido+E%2C+D%C3%ADaz+MF%2C+Bernal+H%2C+%C3%91ustez+CE%2C+Thaler+J%2C+Jander+G%2C+Poveda+K+%282017%29+Costs+and+Tradeoffs+of+Resistance+and+Tolerance+to+Belowground+Herbivory+in+Potato.+PLOS+ONE+12%281%29%3A+e0169083.&amp;rft.identifier=1932-6203&amp;rft.description=1.+The+success+of+sustainable+crop+production+depends+on+our+ability+to+select+or+create+varieties+that+can+allocate+resources+to+both+growth+and+defence.+However%2C+breeding+efforts+have+emphasized+increases+in+yields+but+have+partially+neglected+defence+traits+against+pests.+Estimating+the+costs+of+multiple+defences+against+tuber+herbivores+and+the+tradeoffs+among+them%2C+as+well+as+understanding+the+relationship+between+yield+and+multiple+defences+is+still+unknown+but+relevant+to+both+basic+and+applied+ecology.+2.+Using+twenty+commercial+potato+varieties+available+in+Colombia+and+the+tuber+herbivore+Tecia+solanivora%2C+we+tested+whether+high+yielding+varieties+show+a+reduction+in+three+types+of+defence%3A+constitutive+and+induced+resistance%2C+as+well+as+tolerance.+Specifically%2C+we+determined+%281%29+the+costs+in+terms+of+yield+of+all+three+defences%2C+%282%29+the+possible+tradeoffs+among+them%2C+and+%283%29+if+oviposition+preference+was+related+to+the+expression+of+these+defences.+3.+We+detected+no+costs+in+terms+of+yield+of+constitutive+and+induced+resistance+to+tuber+damage.+We+did%2C+however%2C+find+evidence+of+costs+of+being+able+to+tolerate+tuber+herbivory.+While+we+found+no+tradeoffs+among+any+of+the+estimated+defences%2C+there+was+a+positive+correlation+between+aboveground+compensatory+growth+and+tolerance+in+terms+of+tuber+production%2C+suggesting+that+after+damage+there+are+no+shifts+in+the+allocation+of+resources+from+aboveground+to+belowground+biomass.+Finally%2C+we+found+that+females+laid+more+eggs+on+those+varieties+with+the+lowest+level+of+constitutive+resistance.+4.+Synthesis+and+applications.+Our+findings+suggest+that+in+potatoes%2C+breeding+for+higher+yields+has+not+caused+any+reduction+in+constitutive+or+induced+resistance+to+tuber+damage.+This+is+not+the+case+for+tolerance+where+those+varieties+with+higher+yields+are+also+less+likely+to+tolerate+tuber+damage.+Given+the+high+incidence+of+tuber+pests+in+Colombia%2C+selecting+for+higher+tolerance+could+allow+for+high+productivity+in+the+presence+of+herbivores.+Finding+mechanisms+to+decouple+the+tolerance+response+from+yield+should+be+a+new+priority+in+potato+breeding+in+Colombia+to+guarantee+a+higher+yield+in+both+the+presence+and+absence+of+herbivores.&amp;rft.relation=doi%3A10.5061%2Fdryad.869f2%2F1&amp;rft.relation=doi%3A10.1371%2Fjournal.pone.0169083&amp;rft.subject=belowground+herbivory&amp;rft.subject=costs&amp;rft.subject=defence&amp;rft.subject=induced+defences&amp;rft.subject=resistance&amp;rft.subject=tolerance&amp;rft.subject=tradeoffs&amp;rft.subject=tuber-herbivores&amp;rft.title=Data+from%3A+Costs+and+tradeoffs+of+resistance+and+tolerance+to+belowground+herbivory+in+potato&amp;rft.type=Article&amp;rft.ScientificName=Solanum+tuberosum&amp;rft.ScientificName=Tecia+solanivora&amp;rft.contributor=Katja+Poveda&amp;rft.identifier=PONE-D-16-19771&amp;rft.publicationName=PLOS+ONE&amp;rft.archive=plosone%40plos.org&amp;rft.archive=automated-messages%40datadryad.org&amp;rft.submit=false&amp;rft.review=plosone%40plos.org&amp;rft.review=automated-messages%40datadryad.org&amp;rft.citationInProgress=true">
                 
</span>
</div>
</li>

<li class="ds-artifact-item odd">
<div xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:mets="http://www.loc.gov/METS/" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" style="padding: 6px;" class="artifact-description">
<a href="/resource/doi:10.5061/dryad.34m6j">
<span class="author">Li Y, Geary D</span>
<span class="artifact-title">Data from: Children's visuospatial memory predicts mathematics achievement through early adolescence. </span>
<span class="italics">PLOS ONE</span> <span class="doi">http://dx.doi.org/10.5061/dryad.34m6j</span>
</a>
<span class="Z3988" title="ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;rft_id=doi%3A10.5061%2Fdryad.34m6j&amp;rft_id=1932-6203&amp;rft_id=PONE-D-16-36630&amp;rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;rft.contributor=Li%2C+Yaoran&amp;rft.contributor=Geary%2C+David&amp;rft.identifier=doi%3A10.5061%2Fdryad.34m6j&amp;rft.identifier=1932-6203&amp;rft.description=A+previous+study+showed+that+gains+in+visuospatial+memory+from+first+to+fifth+grade+predicted+end-of-fifth+grade+mathematics+but+not+reading+achievement%2C+controlling+other+factors.+In+this+follow+up+study%2C+these+relations+were+assessed+from+sixth+to+ninth+grade%2C+inclusive+%28n+%3D+145%29.+The+results+showed+that+growth+in+visuospatial+memory+across+the+elementary+school+years+was+related+to+growth+in+mathematics+achievement+after+fifth+grade%2C+controlling+intelligence%2C+the+central+executive+and+phonological+memory+components+of+working+memory%2C+in-class+attentive+behavior%2C+parental+education%2C+and+fifth+grade+mathematics+achievement.+As+found+for+fifth+grade%2C+this+relation+was+not+found+for+reading+achievement+after+fifth+grade.+In+total%2C+the+results+suggest+that+visuospatial+memory+has+a+unique+influence+on+ease+of+learning+some+types+of+mathematics+and+that+this+influence+becomes+more+important+across+successive+grades.&amp;rft.relation=doi%3A10.5061%2Fdryad.34m6j%2F1&amp;rft.relation=doi%3A10.1371%2Fjournal.pone.0172046&amp;rft.subject=visuospatial+ability&amp;rft.subject=working+memory&amp;rft.subject=mathematics+achievement&amp;rft.title=Data+from%3A+Children%27s+visuospatial+memory+predicts+mathematics+achievement+through+early+adolescence&amp;rft.type=Article&amp;rft.contributor=Geary%2C+David&amp;rft.identifier=PONE-D-16-36630&amp;rft.publicationName=PLOS+ONE&amp;rft.archive=plosone%40plos.org&amp;rft.archive=automated-messages%40datadryad.org&amp;rft.submit=false&amp;rft.review=plosone%40plos.org&amp;rft.review=automated-messages%40datadryad.org&amp;rft.fundingEntity=R01+HD38283%2C+R37+HD045914+US+National+Institute+of+Child+Health+and+Human+Development%3B+DRL-1250359%2C+US+National+Science+Foundation%40National+Science+Foundation+%28United+States%29&amp;rft.citationMismatchedDOI=doi%3A10.1371%2Fjournal.pone.0070160">
                 
</span>
</div>
</li>

<li class="ds-artifact-item even">
<div xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:mets="http://www.loc.gov/METS/" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" style="padding: 6px;" class="artifact-description">
<a href="/resource/doi:10.5061/dryad.p170v">
<span class="author">Corry J, Abdullah H, Omar S, Clarke R, Power U, Brady C, McGarvey L, Winter H, Cosby S, Lundy F, Touzelet O</span>
<span class="artifact-title">Data from: Respiratory virus infection up-regulates TRPV1, TRPA1 and ASICS3 receptors on airway cells. </span>
<span class="italics">PLOS ONE</span> <span class="doi">http://dx.doi.org/10.5061/dryad.p170v</span>
</a>
<span class="Z3988" title="ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;rft_id=doi%3A10.5061%2Fdryad.p170v&amp;rft_id=1932-6203&amp;rft_id=PONE-D-16-42709&amp;rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;rft.contributor=Corry%2C+John&amp;rft.contributor=Abdullah%2C+Haniah&amp;rft.contributor=Omar%2C+Shadia&amp;rft.contributor=Clarke%2C+Rebecca&amp;rft.contributor=Power%2C+Ultan&amp;rft.contributor=Brady%2C+Clare&amp;rft.contributor=McGarvey%2C+Lorcan&amp;rft.contributor=Winter%2C+Hanagh&amp;rft.contributor=Cosby%2C+Sara&amp;rft.contributor=Lundy%2C+Fionnuala&amp;rft.contributor=Touzelet%2C+Olivier&amp;rft.identifier=doi%3A10.5061%2Fdryad.p170v&amp;rft.identifier=1932-6203&amp;rft.description=Receptors+implicated+in+cough+hypersensitivity+are+transient+receptor+potential+vanilloid+1+%28TRPV1%29%2C+transient+receptor+potential+cation+channel%2C+Subfamily+A%2C+Member+1+%28TRPA1%29+and+acid+sensing+ion+channel+receptor+3+%28ASIC3%29.+Respiratory+viruses%2C+such+as+respiratory+syncytial+virus+%28RSV%29+and+measles+virus+%28MV%29+may+interact+directly+and%2For+indirectly+with+these+receptors+on+sensory+nerves+and+epithelial+cells+in+the+airways.+We+used+in+vitro+models+of+sensory+neurones+%28SHSY5Y+or+differentiated+IMR-32+cells%29+and+human+bronchial+epithelium+%28BEAS-2B+cells%29+as+well+as+primary+human+bronchial+epithelial+cells+%28PBEC%29+to+study+the+effect+of+MV+and+RSV+infection+on+receptor+expression.+Receptor+mRNA+and+protein+levels+were+examined+by+qPCR+and+flow+cytometry%2C+respectively%2C+following+infection+or+treatment+with+UV+inactivated+virus%2C+virus-induced+soluble+factors+or+pelleted+virus.+Concentrations+of+a+range+of+cytokines+in+resultant+BEAS-2B+and+PBEC+supernatants+were+determined+by+ELISA.%0D%0AUp-regulation+of+TRPV1%2C+TRPA1+and+ASICS3+expression+occurred+by+12+hours+post-infection+in+each+cell+type.+This+was+independent+of+replicating+virus%2C+within+the+same+cell%2C+as+virus-induced+soluble+factors+alone+were+sufficient+to+increase+channel+expression.+IL-8+and+IL-6+increased+in+infected+cell+supernatants.+Antibodies+against+these+factors+inhibited+TRP+receptor+up-regulation.+Capsazepine+treatment+inhibited+virus+induced+up-regulation+of+TRPV1+indicating+that+these+receptors+are+targets+for+treating+virus-induced+cough.&amp;rft.relation=doi%3A10.5061%2Fdryad.p170v%2F1&amp;rft.relation=doi%3A10.1371%2Fjournal.pone.0171681&amp;rft.subject=Respiratory+viruses&amp;rft.subject=TRPV1&amp;rft.subject=TRPA1&amp;rft.subject=ASICS+3&amp;rft.subject=Cough&amp;rft.subject=asthma&amp;rft.subject=COPD&amp;rft.subject=Measles+virus&amp;rft.subject=Respiratory+syncytial+virus&amp;rft.title=Data+from%3A+Respiratory+virus+infection+up-regulates+TRPV1%2C+TRPA1+and+ASICS3+receptors+on+airway+cells&amp;rft.type=Article&amp;rft.contributor=Cosby%2C+Sara&amp;rft.identifier=PONE-D-16-42709&amp;rft.publicationName=PLOS+ONE&amp;rft.archive=plosone%40plos.org&amp;rft.archive=automated-messages%40datadryad.org&amp;rft.submit=false&amp;rft.review=plosone%40plos.org&amp;rft.review=automated-messages%40datadryad.org">
                 
</span>
</div>
</li>

<li class="ds-artifact-item odd">
<div xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:mets="http://www.loc.gov/METS/" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" style="padding: 6px;" class="artifact-description">
<a href="/resource/doi:10.5061/dryad.t7sq1">
<span class="author">Su W, Wang L, Lei J, Chai S, Liu Y, Yang Y, Yang X, Jiao C</span>
<span class="artifact-title">Data from: Genome-wide assessment of population structure and genetic diversity and development of a core germplasm set for sweet potato based on specific length amplified fragment (SLAF) sequencing. </span>
<span class="italics">PLOS ONE</span> <span class="doi">http://dx.doi.org/10.5061/dryad.t7sq1</span>
</a>
<span class="Z3988" title="ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;rft_id=doi%3A10.5061%2Fdryad.t7sq1&amp;rft_id=1932-6203&amp;rft_id=PONE-D-16-41461&amp;rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;rft.contributor=Su%2C+Wenjin&amp;rft.contributor=Wang%2C+Lianjun&amp;rft.contributor=Lei%2C+Jian&amp;rft.contributor=Chai%2C+Shasha&amp;rft.contributor=Liu%2C+Yi&amp;rft.contributor=Yang%2C+Yuanyuan&amp;rft.contributor=Yang%2C+Xinsun&amp;rft.contributor=Jiao%2C+Chunhai&amp;rft.coverage=China&amp;rft.coverage=Africa&amp;rft.coverage=Japan&amp;rft.coverage=South+Korea&amp;rft.coverage=Thailand&amp;rft.coverage=United+States+of+America&amp;rft.identifier=doi%3A10.5061%2Fdryad.t7sq1&amp;rft.identifier=1932-6203&amp;rft.description=Sweet+potato%2C+Ipomoea+batatas+%28L.%29+Lam.%2C+is+an+important+food+crop+that+is+cultivated+worldwide.+However%2C+no+genome-wide+assessment+of+the+genetic+diversity+of+sweet+potato+has+been+reported+to+date.+In+the+present+study%2C+the+population+structure+and+genetic+diversity+of+197+sweet+potato+accessions+most+of+which+were+from+China+were+assessed+using+62%2C363+SNPs.+A+model-based+structure+analysis+divided+the+accessions+into+three+groups%3A+group+1%2C+group+2+and+group+3.+The+genetic+relationships+among+the+accessions+were+evaluated+using+a+phylogenetic+tree%2C+which+clustered+all+the+accessions+into+three+major+groups.+A+principal+component+analysis+%28PCA%29+showed+that+the+accessions+were+distributed+according+to+their+population+structure.+The+mean+genetic+distance+among+accessions+ranged+from+0.290+for+group+1+to+0.311+for+group+3%2C+and+the+mean+polymorphic+information+content+%28PIC%29+ranged+from+0.232+for+group+1+to+0.251+for+group+3.+The+mean+minor+allele+frequency+%28MAF%29+ranged+from+0.207+for+group+1+to+0.222+for+group+3.+Analysis+of+molecular+variance+%28AMOVA%29+showed+that+the+maximum+diversity+was+within+accessions+%2889.569%25%29.+Using+CoreHunter+software%2C+a+core+set+of+39+accessions+was+obtained%2C+which+accounted+for+approximately+19.8%25+of+the+total+collection.+The+core+germplasm+set+of+sweet+potato+developed+will+be+a+valuable+resource+for+future+sweet+potato+improvement+strategies.&amp;rft.relation=doi%3A10.5061%2Fdryad.t7sq1%2F1&amp;rft.relation=doi%3A10.1371%2Fjournal.pone.0172066&amp;rft.subject=sweet+potato&amp;rft.subject=genetic+diversity&amp;rft.subject=a+core+germplasm+set&amp;rft.title=Data+from%3A+Genome-wide+assessment+of+population+structure+and+genetic+diversity+and+development+of+a+core+germplasm+set+for+sweet+potato+based+on+specific+length+amplified+fragment+%28SLAF%29+sequencing&amp;rft.type=Article&amp;rft.contributor=Su%2C+Wenjin&amp;rft.identifier=PONE-D-16-41461&amp;rft.publicationName=PLOS+ONE&amp;rft.archive=plosone%40plos.org&amp;rft.archive=automated-messages%40datadryad.org&amp;rft.submit=false&amp;rft.review=plosone%40plos.org&amp;rft.review=automated-messages%40datadryad.org">
                 
</span>
</div>
</li>

<li class="ds-artifact-item even">
<div xmlns:xlink="http://www.w3.org/TR/xlink/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:mets="http://www.loc.gov/METS/" xmlns:dri="http://di.tamu.edu/DRI/1.0/" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" style="padding: 6px;" class="artifact-description">
<a href="/resource/doi:10.5061/dryad.nq67q">
<span class="author">Riesch R, Muschick M, Lindtke D, Villoutreix R, Comeault AA, Farkas TE, Lucek K, Hellen E, Soria-Carrasco V, Dennis SR, de Carvalho CF, Safran RJ, Sandoval CP, Feder J, Gries R, Crespi BJ, Gries G, Gompert Z, Nosil P</span>
<span class="artifact-title">Data from: Transitions between phases of genomic differentiation during stick-insect speciation. </span>
<span class="italics">Nature Ecology &amp; Evolution</span> <span class="doi">http://dx.doi.org/10.5061/dryad.nq67q</span>
</a>
<span class="Z3988" title="ctx_ver=Z39.88-2004&amp;rft_val_fmt=info%3Aofi%2Ffmt%3Akev%3Amtx%3Adc&amp;rft_id=doi%3A10.5061%2Fdryad.nq67q&amp;rft_id=2397-334X&amp;rfr_id=info%3Asid%2Fdatadryad.org%3Arepo&amp;rft.contributor=Riesch%2C+R%C3%BCdiger&amp;rft.contributor=Muschick%2C+Moritz&amp;rft.contributor=Lindtke%2C+Dorothea&amp;rft.contributor=Villoutreix%2C+Romain&amp;rft.contributor=Comeault%2C+Aaron+A.&amp;rft.contributor=Farkas%2C+Timothy+E.&amp;rft.contributor=Lucek%2C+Kay&amp;rft.contributor=Hellen%2C+Elizabeth&amp;rft.contributor=Soria-Carrasco%2C+V%C3%ADctor&amp;rft.contributor=Dennis%2C+Stuart+R.&amp;rft.contributor=de+Carvalho%2C+Clarissa+F.&amp;rft.contributor=Safran%2C+Rebecca+J.&amp;rft.contributor=Sandoval%2C+Cristina+P.&amp;rft.contributor=Feder%2C+Jeff&amp;rft.contributor=Gries%2C+Regine&amp;rft.contributor=Crespi%2C+Bernard+J.&amp;rft.contributor=Gries%2C+Gerhard&amp;rft.contributor=Gompert%2C+Zach&amp;rft.contributor=Nosil%2C+Patrik&amp;rft.coverage=California&amp;rft.coverage=Oligocene&amp;rft.coverage=Miocene&amp;rft.coverage=Pliocene&amp;rft.coverage=Pleistocene&amp;rft.coverage=Holocene&amp;rft.identifier=doi%3A10.5061%2Fdryad.nq67q&amp;rft.identifier=2397-334X&amp;rft.description=Speciation+can+involve+a+transition+from+a+few+genetic+loci+that+are+resistant+to+gene+flow+to+genome-wide+differentiation.+However%2C+only+limited+data+exist+concerning+this+transition+and+the+factors+promoting+it.+We+study+phases+of+speciation+using+data+from+%3E100+populations+of+11+species+of+Timema+stick+insects.+Consistent+with+early+phases+of+genic+speciation%2C+adaptive+colour-pattern+loci+reside+in+localised+genetic+regions+of+accentuated+differentiation+between+populations+experiencing+gene+flow.+Transitions+to+genome-wide+differentiation+are+also+observed+with+gene+flow%2C+in+association+with+differentiation+in+polygenic+chemical+traits+affecting+mate+choice.+Our+results+support+that+intermediate+phases+of+speciation+are+associated+with+genome-wide+differentiation+and+mate+choice%2C+but+not+growth+of+a+few+genomic+islands.+We+also+find+a+gap+in+genomic+differentiation+between+sympatric+taxa+that+still+exchange+genes+and+those+that+do+not%2C+highlighting+the+association+between+differentiation+and+complete+reproductive+isolation.+Our+results+suggest+that+substantial+progress+towards+speciation+may+involve+the+alignment+of+multi-faceted+aspects+of+differentiation.&amp;rft.relation=1%3B0082%3B2017&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F1&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F2&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F3&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F4&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F5&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F6&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F7&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F8&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F9&amp;rft.relation=doi%3A10.5061%2Fdryad.nq67q%2F10&amp;rft.relation=doi%3A10.1038%2Fs41559-016-0082&amp;rft.subject=evolution&amp;rft.subject=genomics&amp;rft.subject=adaptation&amp;rft.subject=speciation&amp;rft.subject=mate+choice&amp;rft.title=Data+from%3A+Transitions+between+phases+of+genomic+differentiation+during+stick-insect+speciation&amp;rft.type=Article&amp;rft.ScientificName=Timema+bartmani&amp;rft.ScientificName=Timema+boharti&amp;rft.ScientificName=Timema+californicum&amp;rft.ScientificName=Timema+cristinae&amp;rft.ScientificName=Timema+chumash&amp;rft.ScientificName=Timema+knulli&amp;rft.ScientificName=Timema+landelsensis&amp;rft.ScientificName=Timema+petita&amp;rft.ScientificName=Timema+podura&amp;rft.ScientificName=Timema+poppensis&amp;rft.ScientificName=Timema+sp+nov.+%22cuesta+ridge%22&amp;rft.contributor=Patrik+Nosil&amp;rft.publicationName=Nature+Ecology+%26+Evolution">
                 
</span>
</div>
</li>

</ul>
</div>
<!-- End of temp Recently Added -->

                    </div>
                    <div id="most-viewed-data" class="browse-data-panel">
		      This display is currently unavailable.
                        <xsl:apply-templates select="//dri:document/dri:body/dri:div[@id='aspect.discovery.MostDownloadedBitstream.div.home']"/>

                    </div>

                    <div id="by-author" class="browse-data-panel">
		      This display is currently unavailable.
                        <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.discovery.SearchFilterTransformer.div.browse-by-dc.contributor.author_filter']"/>
                        <xsl:apply-templates select="/dri:document/dri:body/dri:div[@id='aspect.discovery.SearchFilterTransformer.div.browse-by-dc.contributor.author_filter-results']"/>

                    </div>
                    <div id="by-journal" class="browse-data-panel">
		      This display is currently unavailable.
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
                        <!-- <xsl:apply-templates select="/dri:document/dri:body/dri:div[@n='front-page-stats']"/> -->
			<!-- Begin temporary stats -->
			<div xmlns="http://www.w3.org/1999/xhtml" id="org_datadryad_dspace_statistics_SiteOverview_div_front-page-stats" class="ds-static-div">
<table id="org_datadryad_dspace_statistics_SiteOverview_table_list-table" class="ds-table">
<tr class="ds-table-header-row">
<th class="ds-table-header-cell odd">Type</th>
<th xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-header-cell even">Total</th>
<th xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-header-cell odd">30 days</th>
</tr>
<tr xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-row even">
<td class="ds-table-cell odd">Data packages</td>
<td xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-cell even">15905</td>
<td class="ds-table-cell odd">305</td>
</tr>
<tr class="ds-table-row odd">
<td class="ds-table-cell odd">Data files</td>
<td xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-cell even">50441</td>
<td class="ds-table-cell odd">749</td>
</tr>
<tr class="ds-table-row even">
<td class="ds-table-cell odd">Journals</td>
<td xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-cell even">563</td>
<td class="ds-table-cell odd">134</td>
</tr>
<tr class="ds-table-row odd">
<td class="ds-table-cell odd">Authors</td>
<td xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-cell even">56647</td>
<td class="ds-table-cell odd">5466</td>
</tr>
<tr class="ds-table-row even">
<td class="ds-table-cell odd">Downloads</td>
<td xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-cell even">9951</td>
<td class="ds-table-cell odd">893</td>
</tr>
</table>
			</div>
<!-- End temporary stats -->
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
