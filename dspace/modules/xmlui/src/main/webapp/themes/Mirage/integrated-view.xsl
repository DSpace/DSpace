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


    <xsl:variable name="token"
                  select="//dri:field[@id='aspect.submission.DryadReviewTransformer.field.token']/dri:value"/>


    <xsl:template match="dri:referenceSet[@type = 'embeddedView']" priority="2">
      <h2 class="ds-list-head">Files in this package</h2>
        <div class="file-list">
	   <!-- TODO: need to test if we have one of the specially-licensed items -->
	   <p class="license-badges" style="font-size: 0.9em;">
	     <i18n:text>xmlui.dri2xhtml.METS-1.0.license-cc0</i18n:text>
	     <xsl:text> </xsl:text>
	     <a href="http://creativecommons.org/publicdomain/zero/1.0/" target="_blank" class="single-image-link"><img src="/themes/Dryad/images/cc-zero.png"/></a>
	     <a href="http://opendefinition.org/" target="_blank" class="single-image-link"><img src="/themes/Dryad/images/opendata.png"/></a>
	   </p>
	   
          <xsl:apply-templates select="*[not(name()='head')]" mode="embeddedView"/>
        </div>
    </xsl:template>


  <!-- ################################ Pull out METS metadata reference and render it ################################ -->
    <xsl:template match="dri:reference" mode="embeddedView">
        <xsl:variable name="externalMetadataURL">
            <xsl:text>cocoon:/</xsl:text>
            <xsl:value-of select="@url"/>
            <!-- No options selected, render the full METS document -->
        </xsl:variable>
        <xsl:comment>External Metadata URL:
            <xsl:value-of select="$externalMetadataURL"/>
        </xsl:comment>
        <xsl:apply-templates select="document($externalMetadataURL)" mode="embeddedView"/>
        <xsl:apply-templates/>
    </xsl:template>

  <!-- ################################ Data File blurb for use on a Data Package page ################################ -->
    <xsl:template match="mets:METS[mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']]" mode="embeddedView">

        <xsl:variable name="my_doi"
                      select=".//dim:field[@element='identifier'][not(@qualifier)][starts-with(., 'doi:')]"/>
	
        <table class="package-file-description">
          <tbody>
          <tr>
            <th>Title</th>
            <th><xsl:copy-of select=".//dim:field[@element='title']"/></th>        
	    <!-- Download count -->
	    <xsl:variable name="downloads" select=".//dim:field[@element='dryad'][@qualifier='downloads']"/>
	    <xsl:if test="$downloads > 0">
	      <tr>
		<th><i18n:text>xmlui.DryadItemSummary.downloads</i18n:text></th>
		<td>
		  <xsl:copy-of select="$downloads" />
		  <xsl:choose>
		    <xsl:when test="$downloads='1'"> time</xsl:when>
		    <xsl:otherwise> times</xsl:otherwise>
		  </xsl:choose>
		</td>
	      </tr>
	    </xsl:if>
	    <xsl:variable name="my_description" select=".//dim:field[@element='description'][@mdschema='dc'][not(@qualifier)]" />
	    <xsl:if test="$my_description!=''">
	      <tr>
		<th>Description</th>
		<td>
		  <xsl:copy-of select="$my_description" />
		</td>
	      </tr>
	    </xsl:if>
          </tr>
          <xsl:for-each select="/mets:METS/mets:fileSec/mets:fileGrp[@USE='CONTENT']/mets:file">
            <tr>
            <th>Download</th>
            <td>
              <a>
                <!-- Download Link -->
                <xsl:attribute name="href">
                    <xsl:value-of select="mets:FLocat/@xlink:href"/>
                </xsl:attribute>

                <xsl:value-of select="mets:FLocat/@xlink:title"/>
                <!-- File Size -->
                <span class="bitstream-filesize">
		  <xsl:text> (</xsl:text>
                    <xsl:choose>
                        <xsl:when test="@SIZE &lt; 1000">
                            <xsl:value-of select="@SIZE"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-bytes</i18n:text>
                        </xsl:when>
                        <xsl:when test="@SIZE &lt; 1000000">
                            <xsl:value-of select="substring(string(@SIZE div 1000),1,5)"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-kilobytes</i18n:text>
                        </xsl:when>
                        <xsl:when test="@SIZE &lt; 1000000000">
                            <xsl:value-of select="substring(string(@SIZE div 1000000),1,5)"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-megabytes</i18n:text>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="substring(string(@SIZE div 1000000000),1,5)"/>
                            <i18n:text>xmlui.dri2xhtml.METS-1.0.size-gigabytes</i18n:text>
                        </xsl:otherwise>
                    </xsl:choose>
		    <xsl:text>)</xsl:text></span></a>
              <!-- View File Details -->
              <xsl:if test="$token!=''">
                <a>
                    <xsl:attribute name="href">
                        <xsl:text>/review?doi=</xsl:text>
                        <xsl:copy-of select="$my_doi"/>
                        <xsl:text>&amp;token=</xsl:text>
                        <xsl:copy-of select="$token"/>
                    </xsl:attribute>
                    <xsl:text>View File Details</xsl:text>
                </a>
            </xsl:if>

            <xsl:if test="not($token!='')">
                <xsl:variable name="my_doi"
                              select="//dim:field[@element='identifier'][not(@qualifier)][starts-with(., 'doi:')]"/>
                <xsl:variable name="my_uri"
                              select="//dim:field[@element='identifier'][@qualifier='uri'][not(starts-with(., 'doi'))]"/>
		<xsl:text>&#160;</xsl:text>                
		<a>
                    <!-- link -->
                    <xsl:attribute name="href">
                        <xsl:choose>
                            <xsl:when test="$my_doi">
                                <xsl:call-template name="checkURL">
                                    <xsl:with-param name="doiIdentifier" select="$my_doi"/>
                                </xsl:call-template>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="$my_uri"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>

		    <xsl:text>View&#160;File&#160;Details</xsl:text>
                </a>
              </xsl:if>
            </td>
          </tr>

          </xsl:for-each>

        <!-- Embargo Notice -->
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
	      <th> </th> <!-- Don't use a label on the embargo text -->
	      <td>
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
                        <div class="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.oneyear</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when
                            test="$embargoedDate='9999-01-01' and ($embargoType='untilArticleAppears' or $embargoType='unknown')">
                        <!-- The item is under embargo, but the end date is not yet known -->
                        <div class="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.publication</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when test="$embargoedDate='9999-01-01' and $embargoType='custom'">
                        <!-- The item is under embargo, but the end date is not yet known. The editor has approved a custom length. -->
                        <div class="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource.custom</i18n:text>
                        </div>
                    </xsl:when>
                    <xsl:when test="not(starts-with($dateDiff, '-'))">
                        <!-- The item is under embargo, and the end date of the embargo is known. -->
                        <div class="embargo_notice">
                            <i18n:text>xmlui.ArtifactBrowser.RestrictedItem.head_resource</i18n:text>
                            <xsl:value-of select="$embargoedDate"/>
                        </div>
                    </xsl:when>
                </xsl:choose>
	      </td>
            </xsl:when>
        </xsl:choose>

      </tbody>
    </table>

    </xsl:template>


</xsl:stylesheet>
