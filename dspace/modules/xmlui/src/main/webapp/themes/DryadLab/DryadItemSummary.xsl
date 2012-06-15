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
        <xsl:variable name="my_uri"
                      select=".//dim:field[@element='identifier'][@qualifier='uri'][not(starts-with(., 'doi'))]"/>



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


        <!-- ################################# General metadata display ############################## -->

	<!-- tempate to render list of files -->
        <xsl:call-template name="bitstreamList"/>
	
        <table class="ds-includeSet-table">


            <!-- Overview -->
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
                      <span class="bold">
                        <xsl:text>Overview:</xsl:text>
                      </span>
                    </td>
                    <td width="70%" colspan="2">
                        <xsl:copy-of select="$description"/>
                    </td>
                </tr>
            </xsl:if>

            <!-- Authors -->
            <xsl:variable name="authors">
                <xsl:for-each
                        select=".//dim:field[@element='contributor']">
                    <xsl:copy-of select="node()"/>
                    <xsl:text>, </xsl:text>
                </xsl:for-each>
            </xsl:variable>
            <xsl:if test="$authors!=''">
                <tr class="ds-table-row">
                    <td>
                      <span class="bold">
                        <xsl:text>Authors:</xsl:text>
                      </span>
                    </td>
                    <td width="70%" colspan="2">
                        <xsl:copy-of select="$authors"/>
                    </td>
                </tr>
            </xsl:if>

	    <!-- Instruction Level -->
            <xsl:variable name="instructionlevel">
              <xsl:value-of select=".//dim:field[@element='audience']" />
            </xsl:variable>
            <xsl:if test="$instructionlevel!=''">
                <tr class="ds-table-row">
                    <td>
                      <span class="bold">
                        <xsl:text>Instruction Level:</xsl:text>
                      </span>
                    </td>
                    <td width="70%" colspan="2">
                        <xsl:copy-of select="$instructionlevel"/>
                    </td>
                </tr>
            </xsl:if>

	    <!-- Duration -->
            <xsl:variable name="duration">
              <xsl:value-of select=".//dim:field[@element='format'][@qualifier='extent']" />
            </xsl:variable>
            <xsl:if test="$duration!=''">
                <tr class="ds-table-row">
                    <td>
                      <span class="bold">
                        <xsl:text>Duration:</xsl:text>
                      </span>
                    </td>
                    <td width="70%" colspan="2">
                        <xsl:copy-of select="$duration"/>
                    </td>
                </tr>
            </xsl:if>

	    <!-- Requirements -->
            <xsl:variable name="requirements">
              <xsl:value-of select=".//dim:field[@element='relation'][@qualifier='requires']" />
            </xsl:variable>
            <xsl:if test="$requirements!=''">
                <tr class="ds-table-row">
                    <td>
                      <span class="bold">
                        <xsl:text>Requirements:</xsl:text>
                      </span>
                    </td>
                    <td width="70%" colspan="2">
                        <xsl:copy-of select="$requirements"/>
                    </td>
                </tr>
            </xsl:if>


            <!-- Learning Outcomes -->
            <tr class="ds-table-row">
              <td>
                <span class="bold">
                  <xsl:text>Learning Outcomes:</xsl:text>
                </span>
              </td>
              <td colspan="2">
		<ul class="text-list">
                  <xsl:for-each
                     select=".//dim:field[@element='learningoutcome']">
                    <li> 
		      <xsl:copy-of select="node()"/>
		    </li>
                  </xsl:for-each>
		</ul>
              </td>
              <td>
              </td>
            </tr>
	    
            <!-- Keywords -->
            <xsl:variable name="keywords">
                <xsl:for-each
                        select=".//dim:field[@element='subject'][@mdschema='dc'][not(@qualifier)]">
                    <xsl:copy-of select="node()"/>
                    <xsl:text>, </xsl:text>
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
                          <xsl:text>Date Published:</xsl:text>
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
	    
	    <!-- Identifier -->
            <tr class="ds-table-row">
              <td width="15%">
		<span class="bold">
		  <xsl:text>Activity Identifier</xsl:text>
		</span>
              </td>
              <td width="55%">
                <xsl:choose>
                  <xsl:when test="$my_doi">
                    <xsl:value-of select="$my_doi"/>
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
                <span class="Z3988">
                  <xsl:attribute name="title">
                    <xsl:call-template name="renderCOinS"/>
                  </xsl:attribute>
                  <xsl:text>&#160;</xsl:text>
                </span>
		
              </td>
            </tr>
	    
            </table>
	<!-- End of main metadata section -->


        <!-- we only want this view from item view - not the administrative pages -->
        <xsl:if test="$meta[@qualifier='URI' and contains(.., 'handle') and not(contains(..,'workflow'))]">
            <div style="padding: 10px; margin-top: 5px; margin-bottom: 5px;">
                <a href="?show=full">
                    <i18n:text>xmlui.DryadItemSummary.showFull</i18n:text>
                </a>
            </div>
        </xsl:if>

        <xsl:apply-templates
                select="./mets:fileSec/mets:fileGrp[@USE='CC-LICENSE' or @USE='LICENSE']"/>

        <xsl:if
                test=".//dim:field[@element='rights'][.='http://creativecommons.org/publicdomain/zero/1.0/']">
                    <div style="padding-top: 10px;">
                        <i18n:text>xmlui.dri2xhtml.METS-1.0.license-cc0</i18n:text>
                        <xsl:text> &#160; </xsl:text>
                        <a href="http://creativecommons.org/publicdomain/zero/1.0/"
                           target="_blank">
                            <img src="/themes/Dryad/images/cc-zero.png"/>
                        </a>
                        <a href="http://opendefinition.org/">
                            <img src="/themes/Dryad/images/opendata.png"/>
                        </a>
                    </div>
        </xsl:if>

    </xsl:template>


	    <!-- ######################### Bitstream List ############################ -->
	    <xsl:template name="bitstreamList">
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
	    

    <!-- ########################### An item rendered in the detailView pattern, the "full item record" ####################
         ########################### view of a DSpace item in Manakin.                                  #################### -->
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

                <xsl:call-template name="bitstreamList"/>
                
            </xsl:when>
            <xsl:when test="./mets:fileSec/mets:fileGrp[@USE='CONTENT']">
              <xsl:call-template name="bitstreamList"/>
            </xsl:when>
        </xsl:choose>

        <!-- Generate the Creative Commons license information from the file section
              (DSpace deposit license hidden by default) -->
        <xsl:apply-templates select="mets:fileSec/mets:fileGrp[@USE='CC-LICENSE']"/>

    </xsl:template>


</xsl:stylesheet>
