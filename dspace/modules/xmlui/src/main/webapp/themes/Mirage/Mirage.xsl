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


            <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']">
                <div id="ds-system-wide-alert">
                    <p>
                        <xsl:copy-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='alert'][@qualifier='message']/node()"/>
                    </p>
                </div>
            </xsl:if>

            <!-- START NEWS -->

            <xsl:apply-templates select="dri:div[@id='file.news.div.news']"/>

            <!-- START DEPOSIT -->

            <h1 class="ds-div-head">Deposit your data in dryad</h1>
            <div id="file_news_div_news" class="ds-static-div primary">
                <p class="ds-paragraph">
                    <a href="/handle/10255/3/submit" class="submitnowbutton">Submit Data Now!</a>
                    <a href="http://www.youtube.com/watch?v=RP33cl8tL28">See how to submit</a>

                </p>
            </div>

            <!-- START SEARCH -->

            <h1 class="ds-div-head">Search DSpace</h1>
            <form xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" id="aspect_discovery_SiteViewer_div_front-page-search" class="ds-interactive-div primary" action="/discover" method="get" onsubmit="javascript:tSubmit(this);">
                <p class="ds-paragraph">
                    <p>
                        <label class="ds-form-label" for="aspect_discovery_SiteViewer_field_query">Enter some text in the box below to search DSpace.</label>
                    </p>
                    <input xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" id="aspect_discovery_SiteViewer_field_query" class="ds-text-field" name="query" type="text" value="" />
                    <input xmlns:i18n="http://apache.org/cocoon/i18n/2.1" id="aspect_discovery_SiteViewer_field_submit" class="ds-button-field" name="submit" type="submit" value="Go" />
                </p>
            </form>

            <!-- START CONNECT  -->

            <div id="ds_connect_with_dryad">


                <h1 id="ds_connect_with_dryad_head" class="ds_connect_with_dryad_head">
                    <i18n:text>Connect with Dryad</i18n:text>
                </h1>
                <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>


            </div>



            <!-- START BROWSE -->

            <h1 class="ds-div-head">Browse for Data</h1>
            <div id="aspect_discovery_RecentlyAdded_div_Home" class="ds-static-div primary">

                <xsl:for-each select="dri:div[@n='site-home']">
                    <xsl:apply-templates/>
                </xsl:for-each>

                <h1 xmlns:i18n="http://apache.org/cocoon/i18n/2.1" class="ds-div-head"><i18n:text>Featured</i18n:text></h1>

                <div id="aspect_discovery_SiteFeaturedItems_div_site-featured-item" class="ds-static-div secondary featured-item">
                    <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                </div>

                <h1 xmlns:i18n="http://apache.org/cocoon/i18n/2.1" class="ds-div-head"><i18n:text>Most Viewed</i18n:text></h1>
                <div id="aspect_discovery_SiteFeaturedItems_div_site-most-viewed" class="ds-static-div secondary most-viewed">
                    <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                </div>


            </div>

            <!-- START MAILING LIST-->

            <h1 class="ds-div-head">Dryad Mailing List</h1>
            <div id="file_news_div_mailing_list" class="ds-static-div primary">
                <p class="ds-paragraph">
                    <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                </p>
            </div>

            <!-- START BLOG -->
            <xsl:apply-templates select="dri:div[@id='aspect.dryadinfo.DryadBlogFeed.div.dryad-info-home']"/>


            <!-- START STATISTICS -->

            <div id="aspect_statistics_StatisticsTransformer_div_home" class="ds-static-div primary repository">
                <h1 class="ds-div-head">Dryad Statistics</h1>
                <div xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" id="aspect_statistics_StatisticsTransformer_div_stats" class="ds-static-div secondary stats">
                    <h2 class="ds-table-head">Total Visits</h2>
                    <table xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" id="aspect_statistics_StatisticsTransformer_table_list-table" class="ds-table tableWithTitle">
                        <tr class="ds-table-row odd">
                            <td id="aspect_statistics_StatisticsTransformer_cell_" class="ds-table-cell odd labelcell" />
                            <td id="aspect_statistics_StatisticsTransformer_cell_" class="ds-table-cell even labelcell">Views</td>
                        </tr>
                        <tr xmlns:i18n="http://apache.org/cocoon/i18n/2.1" xmlns="http://di.tamu.edu/DRI/1.0/" class="ds-table-row even">
                            <td id="aspect_statistics_StatisticsTransformer_cell_01" class="ds-table-cell odd labelcell">Data from: IDENTIFIER TEST</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_02" class="ds-table-cell even datacell">23</td>
                        </tr>
                        <tr class="ds-table-row odd">
                            <td id="aspect_statistics_StatisticsTransformer_cell_11" class="ds-table-cell odd labelcell">Data from: CHECK</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_12" class="ds-table-cell even datacell">14</td>
                        </tr>
                        <tr class="ds-table-row even">
                            <td id="aspect_statistics_StatisticsTransformer_cell_21" class="ds-table-cell odd labelcell">Data from: Mini_deletion_revert</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_22" class="ds-table-cell even datacell">11</td>
                        </tr>
                        <tr class="ds-table-row odd">
                            <td id="aspect_statistics_StatisticsTransformer_cell_31" class="ds-table-cell odd labelcell">4651</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_32" class="ds-table-cell even datacell">9</td>
                        </tr>
                        <tr class="ds-table-row even">
                            <td id="aspect_statistics_StatisticsTransformer_cell_41" class="ds-table-cell odd labelcell">Canada</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_42" class="ds-table-cell even datacell">9</td>
                        </tr>
                        <tr class="ds-table-row odd">
                            <td id="aspect_statistics_StatisticsTransformer_cell_51" class="ds-table-cell odd labelcell">South Korea</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_52" class="ds-table-cell even datacell">9</td>
                        </tr>
                        <tr class="ds-table-row even">
                            <td id="aspect_statistics_StatisticsTransformer_cell_61" class="ds-table-cell odd labelcell">Data from: Test  duplicate</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_62" class="ds-table-cell even datacell">9</td>
                        </tr>
                        <tr class="ds-table-row odd">
                            <td id="aspect_statistics_StatisticsTransformer_cell_71" class="ds-table-cell odd labelcell">Data from: Metadata version</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_72" class="ds-table-cell even datacell">8</td>
                        </tr>
                        <tr class="ds-table-row even">
                            <td id="aspect_statistics_StatisticsTransformer_cell_81" class="ds-table-cell odd labelcell">Data from: Test delete version -1</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_82" class="ds-table-cell even datacell">8</td>
                        </tr>
                        <tr class="ds-table-row odd">
                            <td id="aspect_statistics_StatisticsTransformer_cell_91" class="ds-table-cell odd labelcell">Data from: 00005</td>
                            <td id="aspect_statistics_StatisticsTransformer_cell_92" class="ds-table-cell even datacell">7</td>
                        </tr>
                    </table>
                </div>
            </div>


            <!-- START INTEGRATED JOURNAL-->

            <h1 class="ds-div-head">Recently Integrated Journal</h1>
            <div id="file_news_div_recently_integrated_journal" class="ds-static-div primary">
                <p class="ds-paragraph">
                    <xsl:text>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam a nisi sit amet neque vehicula dignissim accumsan non erat. Pellentesque eu ligula a est hendrerit porta a non ligula. Quisque in orci nisl, eu dictum massa. Aenean vitae lorem et risus dapibus fringilla et sit amet nunc. Donec ac sem risus. Cras a magna sapien, vel facilisis lacus. Fusce sed blandit tellus. </xsl:text>

                </p>
            </div>


        </div>

    </xsl:template>

</xsl:stylesheet>
