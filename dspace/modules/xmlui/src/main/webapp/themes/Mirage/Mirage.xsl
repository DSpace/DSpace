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
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

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
    <xsl:output indent="yes"/>


    <xsl:template match="dri:body[//dri:meta/dri:pageMeta/dri:metadata[@element='request' and @qualifier='URI'] = '' ]">

        <div id="ds-body">

            <!-- CAROUSEL -->
            <div class="home-col-1">
                <div id="dryad-home-carousel" class="ds-static-div primary">
                    <div class="bxslider" style="">
                        <div>
                            <p style="margin-top: 44px;" Xid="ds-dryad-is" xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/">
                                <span style="color: #595;">Dryad</span> is a <span style="color: #363;">nonprofit organization</span> and an <span style="color: #242;">international repository</span> of data underlying scientific and medical publications.</p>
                        </div>
                        <div>
                            <p>The scientific, educational, and charitable mission of Dryad is to promote the availability of data underlying findings in
                                the scientific literature for research and educational reuse. </p>
                        </div>
                        <div style="font-size: 0.85em; margin-top: 12px;">
                            <p>The vision of Dryad is a scholarly communication system in which learned societies, publishers, institutions of research
                                and education, funding bodies and other stakeholders collaboratively sustain and promote the preservation and reuse of data
                                underlying the scholarly literature.</p>
                        </div>
                    </div>
                </div>
            </div>


            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of
                                select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>

            <!-- START NEWS -->
            <!--<div class="home-col-2">-->
                <!--<xsl:apply-templates select="dri:div[@id='file.news.div.news']"/>-->
            <!--</div>-->

            <!-- START DEPOSIT -->
            <div class="home-col-2">
                <h1 class="ds-div-head " style="border-bottom: none; text-align: center; padding: 50px 45px 0; height: 50px;">Deposit your data in dryad</h1>
                <div class="ds-static-div primary" id="file_news_div_news" style="height: 100px;">
                    <p class="ds-paragraph" style="text-align: center; font-size: 1.2em; margin: 0.5em 0 1.5em;">
                        <a class="submitnowbutton" href="/handle/10255/3/submit">Submit Data Now!</a>
                    </p>
                    <a style="float: right; margin-right: 18px;" href="http://www.youtube.com/watch?v=RP33cl8tL28">See how to submit</a>
                </div>
            </div>

            <!-- START SEARCH -->
            <div class="home-col-1">
                <h1 class="ds-div-head">Search DSpace</h1>
                <form xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                      id="aspect_discovery_SiteViewer_div_front-page-search" class="ds-interactive-div primary"
                      action="/discover" method="get" onsubmit="javascript:tSubmit(this);">
                    <p class="ds-paragraph">
                        <p>
                            <label class="ds-form-label" for="aspect_discovery_SiteViewer_field_query">Enter some text
                                in the box below to search DSpace.
                            </label>
                        </p>
                        <input xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                               id="aspect_discovery_SiteViewer_field_query" class="ds-text-field" name="query"
                               type="text" value=""/>
                        <input xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                               id="aspect_discovery_SiteViewer_field_submit" class="ds-button-field" name="submit"
                               type="submit" value="Go"/>
                    </p>
                </form>
            </div>

            <!-- START CONNECT  -->
            <div class="home-col-2">
                <h1 class="ds-div-head ds_connect_with_dryad_head" id="ds_connect_with_dryad_head">Connect with Dryad</h1>
                <div id="ds_connect_with_dryad" class="ds-static-div primary" style="height: 490px;">
                    <div id="TEMP-connect-alternatives">

                        <div id="connect-illustrated-prose">
                            <p>
                                <img style="float: right;margin-left: 8px;" src="files/images/connect-1.png"/>
                                This text would describe the program in broad, readable terms,
                                including clickable links for terms that lead us deeper,
                                <img style="float: left;margin-right: 8px;" src="files/images/connect-2.png"/>
                                like <a href="#">membership</a> and <a href="#">journal integration</a>. Lorem ipsum
                                dolor sit amet, consectetur adipiscing elit.
                            </p>
                            <p>
                                Nam a nisi sit
                                amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est
                                <img style="float: right;margin-left: 8px" src="files/images/connect-3.png"/>
                                hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa.
                            </p>
                            <p>
                                Aenean
                                vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus.
                                <img style="float: left;margin-right: 10px;" src="files/images/connect-4.png"/>
                                Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus.
                            </p>
                            <em>[1. illustrated prose]</em>
                        </div>

                        <div id="connect-keyword-cloud">
                            <div class="wordcloud resizeable vertical horizontal saved" id="newWordCloud" style="display: none; background-color: rgb(255, 255, 255); width: 274px; height: 450px;"><a class="cloudword Yiggivoo" title="institution" id="cp0" style="font-size: 23px; color: rgb(21, 108, 155); left: 18px; top: 208px; display: inline;">institution</a><a class="cloudword Yiggivoo" title="join" id="cp1" style="font-size: 35px; color: rgb(165, 29, 90); left: 82px; top: 230px; display: inline;">join</a><a class="cloudword Yiggivoo" title="academic" id="cp2" style="font-size: 26px; color: rgb(41, 72, 171); left: 69px; top: 182px; display: inline;">academic</a><a class="cloudword Yiggivoo" title="integrate" id="cp3" style="font-size: 31px; color: rgb(55, 173, 55); left: 66px; top: 265px; display: inline;">integrate</a><a class="cloudword Yiggivoo" title="preserve" id="cp4" style="font-size: 26px; color: rgb(72, 41, 171); left: 91px; top: 156px; display: inline;">preserve</a><a class="cloudword Yiggivoo" title="member" id="cp5" style="font-size: 33px; color: rgb(29, 165, 90); left: 8px; top: 122px; display: inline; text-decoration: underline;" href="#">member</a><a class="cloudword Yiggivoo" title="archive" id="cp6" style="font-size: 30px; color: rgb(29, 165, 90); left: 36px; top: 296px; display: inline;">archive</a><a class="cloudword Yiggivoo" title="publisher" id="cp7" style="font-size: 19px; color: rgb(173, 55, 55); left: 159px; top: 210px; display: inline; text-decoration: underline;" href="#">publisher</a><a class="cloudword Yiggivoo" title="govern" id="cp8" style="font-size: 15px; color: rgb(21, 108, 155); left: 158px; top: 228px; display: inline;">govern</a><a class="cloudword Yiggivoo" title="organization" id="cp9" style="font-size: 31px; color: rgb(21, 108, 155); left: 44px; top: 325px; display: inline;">organization</a><a class="cloudword Yiggivoo" title="partner" id="cp10" style="font-size: 14px; color: rgb(141, 17, 126); left: 157px; top: 244px; display: inline;">partner</a><a class="cloudword Yiggivoo" title="analyze" id="cp11" style="font-size: 26px; color: rgb(173, 55, 55); left: 118px; top: 96px; display: inline;">analyze</a><a class="cloudword Yiggivoo" title="scientist" id="cp12" style="font-size: 20px; color: rgb(165, 29, 90); left: 173px; top: 136px; display: inline;">scientist</a><a class="cloudword Yiggivoo" title="data" id="cp13" style="font-size: 28px; color: rgb(17, 141, 126); left: 185px; top: 296px; display: inline;">data</a><a class="cloudword Yiggivoo" title="educate" id="cp14" style="font-size: 35px; color: rgb(165, 29, 90); left: 60px; top: 60px; display: inline;">educate</a><a class="cloudword Yiggivoo" title="journal" id="cp15" style="font-size: 38px; color: rgb(21, 108, 155); left: 1px; top: 356px; display: inline; text-decoration: underline;" href="#">journal</a><a class="cloudword Yiggivoo" title="help" id="cp16" style="font-size: 32px; color: rgb(108, 21, 155); left: 180px; top: 356px; display: inline;">help</a><a class="cloudword Yiggivoo" title="share" id="cp17" style="font-size: 13px; color: rgb(41, 72, 171); left: 30px; top: 230px; display: inline;">share</a><a class="cloudword Yiggivoo" title="society" id="cp18" style="font-size: 39px; color: rgb(41, 72, 171); left: 0px; top: 20px; display: inline; text-decoration: underline;" href="#">society</a><a class="cloudword Yiggivoo" title="collaborate" id="cp19" style="font-size: 26px; color: rgb(21, 108, 155); left: 102px; top: 394px; display: inline;">collaborate</a><a class="cloudword Yiggivoo" title="connect" id="cp20" style="font-size: 27px; color: rgb(141, 17, 126); left: 110px; top: 419px; display: inline;">connect</a><a class="cloudword Yiggivoo" title="study" id="cp21" style="font-size: 21px; color: rgb(21, 108, 155); left: 2px; top: 242px; display: inline;">study</a><a class="cloudword Yiggivoo" title="sustain" id="cp22" style="font-size: 21px; color: rgb(141, 17, 126); left: 18px; top: 100px; display: inline;">sustain</a><a class="cloudword Yiggivoo" title="synthesize" id="cp23" style="font-size: 20px; color: rgb(72, 41, 171); left: 75px; top: 0px; display: inline;">synthesize</a></div>

                            <em>[2. keyword cloud]</em>
                        </div>

                        <div id="connect-legible-cloud">
                            The
                            <span style="font-size: 25px; color: #c99;">
                                <a href="#">vision</a> of
                                <span style="font-weight: 36px; color: #494; font-weight: bold;">Dryad</span></span> is a
                            <span style="font-size: 22px; color: #99b;">scholarly
                                <a href="#">communication</a> system</span> in which
                            <span style="font-size: 22px;">learned
                                <a style="font-size: 24px; color: #494;" href="#TODO">societies</a>,
                                <a style="font-size: 27px; color: #9c9; font-weight: bold;" href="#TODO">publishers</a>,
                                <a style="font-size: 30px; color: #494;" href="#TODO">institutions</a>
                                of research and education</span>, funding bodies and other
                            <span style="font-size: 22px;">
                                <a style="font-size: 28px; color: #449;" href="#TODO">stakeholders</a>
                            </span>
                            collaboratively
                            <a style="font-size: 24px; color: #c99; font-weight: bold;" href="#TODO">sustain</a>
                            and promote the
                            <span style="font-size: 24px; color: #bb6;">
                                <a href="#" style="color: #bb6;">preservation</a>
                                and reuse
                            </span>
                            of data.

                            <br/>
                            <br/>
                            <em style="font-size: 13px;">[3. readable word cloud]</em>
                        </div>

                    </div><!-- END of #TEMP-connect-alternatives -->
                </div>
            </div>

            <!-- START BROWSE -->
            <div class="home-col-1">
                <h1 class="ds-div-head">Browse for Data</h1>
                <div id="aspect_discovery_RecentlyAdded_div_Home" class="ds-static-div primary">

                    <xsl:for-each select="dri:div[@n='site-home']">
                        <xsl:apply-templates/>
                    </xsl:for-each>

                    <h1 xmlns:i18n="http://apache.org/cocoon/i18n/2.1" class="ds-div-head">
                        <i18n:text>Featured</i18n:text>
                    </h1>

                    <div id="aspect_discovery_SiteFeaturedItems_div_site-featured-item"
                         class="ds-static-div secondary featured-item">
                        <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                    </div>

                    <h1 xmlns:i18n="http://apache.org/cocoon/i18n/2.1" class="ds-div-head">
                        <i18n:text>Most Viewed</i18n:text>
                    </h1>
                    <div id="aspect_discovery_SiteFeaturedItems_div_site-most-viewed"
                         class="ds-static-div secondary most-viewed">
                        <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>
                    </div>
                </div>
            </div>

            <!-- START MAILING LIST-->
            <div class="home-col-2">
                <h1 class="ds-div-head">Dryad Mailing List</h1>
                <div id="file_news_div_mailing_list" class="ds-static-div primary">
                    <p class="ds-paragraph">
                        <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                    </p>
                </div>
            </div>

            <!-- START INTEGRATED JOURNAL-->
            <div class="home-col-2" style="clear: both; margin-left: 25px;">
                <h1 class="ds-div-head">Recently Integrated Journal</h1>
                <div id="file_news_div_recently_integrated_journal" class="ds-static-div primary">
                    <p class="ds-paragraph">
                        <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                    </p>
                </div>
            </div>

            <!-- START BLOG -->
            <div class="home-col-2">
                <xsl:apply-templates select="dri:div[@id='aspect.dryadinfo.DryadBlogFeed.div.dryad-info-home']"/>
            </div>


            <!-- START STATISTICS -->
            <div class="home-col-2" style="margin-left: 25px;">
                <div id="aspect_statistics_StatisticsTransformer_div_home" class="ds-static-div primary repository">
                    <h1 class="ds-div-head">Dryad Statistics</h1>
                    <div xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                         id="aspect_statistics_StatisticsTransformer_div_stats" class="ds-static-div secondary stats">
                        <h2 class="ds-table-head">Total Visits</h2>
                        <table xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                               id="aspect_statistics_StatisticsTransformer_table_list-table"
                               class="ds-table tableWithTitle">
                            <tr class="ds-table-row odd">
                                <td id="aspect_statistics_StatisticsTransformer_cell_"
                                    class="ds-table-cell odd labelcell"/>
                                <td id="aspect_statistics_StatisticsTransformer_cell_"
                                    class="ds-table-cell even labelcell">Views
                                </td>
                            </tr>
                            <tr xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/"
                                class="ds-table-row even">
                                <td id="aspect_statistics_StatisticsTransformer_cell_01"
                                    class="ds-table-cell odd labelcell">Data from: IDENTIFIER TEST
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_02"
                                    class="ds-table-cell even datacell">23
                                </td>
                            </tr>
                            <tr class="ds-table-row odd">
                                <td id="aspect_statistics_StatisticsTransformer_cell_11"
                                    class="ds-table-cell odd labelcell">Data from: CHECK
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_12"
                                    class="ds-table-cell even datacell">14
                                </td>
                            </tr>
                            <tr class="ds-table-row even">
                                <td id="aspect_statistics_StatisticsTransformer_cell_21"
                                    class="ds-table-cell odd labelcell">Data from: Mini_deletion_revert
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_22"
                                    class="ds-table-cell even datacell">11
                                </td>
                            </tr>
                            <tr class="ds-table-row odd">
                                <td id="aspect_statistics_StatisticsTransformer_cell_31"
                                    class="ds-table-cell odd labelcell">4651
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_32"
                                    class="ds-table-cell even datacell">9
                                </td>
                            </tr>
                            <tr class="ds-table-row even">
                                <td id="aspect_statistics_StatisticsTransformer_cell_41"
                                    class="ds-table-cell odd labelcell">Canada
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_42"
                                    class="ds-table-cell even datacell">9
                                </td>
                            </tr>
                            <tr class="ds-table-row odd">
                                <td id="aspect_statistics_StatisticsTransformer_cell_51"
                                    class="ds-table-cell odd labelcell">South Korea
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_52"
                                    class="ds-table-cell even datacell">9
                                </td>
                            </tr>
                            <tr class="ds-table-row even">
                                <td id="aspect_statistics_StatisticsTransformer_cell_61"
                                    class="ds-table-cell odd labelcell">Data from: Test duplicate
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_62"
                                    class="ds-table-cell even datacell">9
                                </td>
                            </tr>
                            <tr class="ds-table-row odd">
                                <td id="aspect_statistics_StatisticsTransformer_cell_71"
                                    class="ds-table-cell odd labelcell">Data from: Metadata version
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_72"
                                    class="ds-table-cell even datacell">8
                                </td>
                            </tr>
                            <tr class="ds-table-row even">
                                <td id="aspect_statistics_StatisticsTransformer_cell_81"
                                    class="ds-table-cell odd labelcell">Data from: Test delete version -1
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_82"
                                    class="ds-table-cell even datacell">8
                                </td>
                            </tr>
                            <tr class="ds-table-row odd">
                                <td id="aspect_statistics_StatisticsTransformer_cell_91"
                                    class="ds-table-cell odd labelcell">Data from: 00005
                                </td>
                                <td id="aspect_statistics_StatisticsTransformer_cell_92"
                                    class="ds-table-cell even datacell">7
                                </td>
                            </tr>
                        </table>
                    </div>
                </div>
            </div>
        </div>

    </xsl:template>

</xsl:stylesheet>
