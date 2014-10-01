<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:atom="http://www.w3.org/2005/Atom"
                xmlns:ore="http://www.openarchives.org/ore/terms/"
                xmlns:oreatom="http://www.openarchives.org/ore/atom/"
                xmlns="http://www.w3.org/1999/xhtml"
                xmlns:xalan="http://xml.apache.org/xalan"
                xmlns:encoder="xalan://java.net.URLEncoder"
                xmlns:util="org.dspace.app.xmlui.utils.XSLUtils"
                exclude-result-prefixes="xalan encoder i18n dri mets dim  xlink xsl">


    <xsl:template name="authorSummaryView-DIM">

            <div class="details placeholder"><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.hide</i18n:text></div>
            <div id="AuthorProfileHide" style="display:none"><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.show</i18n:text></div>

        <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='biography' and @qualifier='title'] or
                                    (./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)] and
                                    (./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='author' and @qualifier='internal']='true'))">

        <h3>
            <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='biography' and @qualifier='title']" mode="profile"/>

            <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='biography' and @qualifier='title'] and
                                    (./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)] and
                                    (./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='author' and @qualifier='internal']='true'))">
                <xsl:text>, </xsl:text>
            </xsl:if>

            <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='author' and @qualifier='internal']='true'">
                <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)][1]" mode="profile"/>
            </xsl:if>
        </h3>
        </xsl:if>
            <div class="ds-author-wrapper">
                <div class="ds-author-top-wrapper">
                    <div class="ds-author-picture">
                            <xsl:apply-templates mode="author-pic" select="./mets:fileSec/mets:fileGrp/mets:file"/>
                            <xsl:if test="count(./mets:fileSec/mets:fileGrp/mets:file)=0">
                                <xsl:element name="span" namespace="http://www.w3.org/1999/xhtml">
                                    <img class="author-pic">
                                        <xsl:attribute name="src">
                                            <xsl:value-of select="concat($theme-path, '/images/no-profile-photo.png')"/>
                                        </xsl:attribute>
                                        <xsl:attribute name="width">
                                            <xsl:text>90</xsl:text>
                                        </xsl:attribute>
                                    </img>
                                </xsl:element>
                            </xsl:if>
                    </div>


                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='biography' and not(@qualifier)]" mode="profile"/>



                </div>
                <div class="ds-author-bottom-wrapper author-hide">

                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='variant'][1]" mode="profile"/>

                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='specialization' and not(@qualifier)][1]" mode="profile"/>

                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='degree' and not(@qualifier)][1]" mode="profile"/>
                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='OrchidId' and not(@qualifier)][1]" mode="profile"/>

                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='site'  and not(@qualifier)][1]" mode="profile"/>

                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='external' and @qualifier='hostWork']" mode="profile"/>

                    <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='telephone' and not(@qualifier)] or
                                    ./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]">

                        <h2>
                            <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.contactInformation</i18n:text>
                        </h2>
                        <ul>
                            <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='telephone' and not(@qualifier)]">
                                <li>
                                    <xsl:value-of select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='telephone' and not(@qualifier)]"/>
                                </li>
                            </xsl:if>
                            <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]">
                                <li>
                                    <a>
                                        <xsl:attribute name="href">
                                            <xsl:text>mailto:</xsl:text><xsl:value-of
                                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]"/>
                                        </xsl:attribute>
                                        <xsl:value-of select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]"/>
                                    </a>
                                </li>
                            </xsl:if>
                        </ul>
                    </xsl:if>

                    <xsl:apply-templates select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='updateDate' and not(@qualifier)]" mode="profile"/>

                    <xsl:text>&#160;</xsl:text>
                </div>
            </div>

    </xsl:template>

    <xsl:variable name="separator" select="':##:'"/>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='site'  and not(@qualifier)]" mode="profile">
        <div class="ds-author-links">
            <h2><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.externalLinks</i18n:text></h2>
            <ul>
                <li>
                <a>
                    <xsl:attribute name="href">
                        <xsl:call-template name="string-replace-all">
                            <xsl:with-param name="text"
                                            select="substring-after(.,$separator)"/>
                            <xsl:with-param name="replace" select="$separator"/>
                            <xsl:with-param name="by" select="''"/>
                        </xsl:call-template>
                    </xsl:attribute>
                    <xsl:value-of
                            select="substring-before(.,$separator)"/>
                </a>
                </li>

            <xsl:for-each
                    select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='site'  and not(@qualifier)]">

                <li>
                <a>
                        <xsl:attribute name="href">
                            <xsl:call-template name="string-replace-all">
                                <xsl:with-param name="text"
                                                select="substring-after(.,$separator)"/>
                                <xsl:with-param name="replace" select="$separator"/>
                                <xsl:with-param name="by" select="''"/>
                            </xsl:call-template>
                        </xsl:attribute>
                        <xsl:value-of
                                select="substring-before(.,$separator)"/>
                </a>
                </li>
            </xsl:for-each>
            </ul>

        </div>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='biography' and not(@qualifier)]" mode="profile">
        <div class="ds-author-bio">
            <xsl:call-template name="linebreakIntoBr">
                <xsl:with-param name="text" select="."/>
            </xsl:call-template>
        </div>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='biography' and @qualifier='title']" mode="profile">
                <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='variant']" mode="profile">
        <div class="simple-author-view-other">
        <h2><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.nameVariants</i18n:text></h2>
        <span class="author-names">
            <xsl:value-of select="."/>

            <xsl:for-each
                    select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='variant']">
                <xsl:text>; </xsl:text>
                <xsl:value-of select="."/>
            </xsl:for-each>
        </span>
        </div>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)]" mode="profile">

                    <xsl:choose>
                        <xsl:when
                                test="substring-after(.,$separator)!=$separator">
                            <a>
                                <xsl:attribute name="href">
                                    <xsl:call-template name="string-replace-all">
                                        <xsl:with-param name="text"
                                                        select="substring-after(.,$separator)"/>
                                        <xsl:with-param name="replace" select="$separator"/>
                                        <xsl:with-param name="by" select="''"/>
                                    </xsl:call-template>
                                </xsl:attribute>
                                <xsl:value-of
                                        select="substring-before(.,$separator)"/>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of
                                    select="substring-before(.,$separator)"/>
                        </xsl:otherwise>

                    </xsl:choose>
            <xsl:for-each select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)]">
                <xsl:text>, </xsl:text>
                <xsl:choose>
                    <xsl:when
                            test="substring-after(.,$separator)!=$separator">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:call-template name="string-replace-all">
                                    <xsl:with-param name="text"
                                                    select="substring-after(.,$separator)"/>
                                    <xsl:with-param name="replace" select="$separator"/>
                                    <xsl:with-param name="by" select="''"/>
                                </xsl:call-template>
                            </xsl:attribute>
                            <xsl:value-of
                                    select="substring-before(.,$separator)"/>
                        </a>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of
                                select="substring-before(.,$separator)"/>
                    </xsl:otherwise>

                </xsl:choose>
                <xsl:if test="./following-sibling::dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)]">
                    <xsl:text>, </xsl:text>
                </xsl:if>
            </xsl:for-each>

    </xsl:template>

    <xsl:template match="mets:fileSec/mets:fileGrp/mets:file" mode="author-pic">
        <img class="author-pic" alt="author picture">
            <xsl:attribute name="src">
                <xsl:value-of select="mets:FLocat/@xlink:href"/>
            </xsl:attribute>
            <xsl:attribute name="width">
                <xsl:text>90</xsl:text>
            </xsl:attribute>
        </img>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='specialization' and not(@qualifier)]" mode="profile">
        <div class="simple-author-view-other">
        <h2><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.fieldsOfSpecialization</i18n:text></h2>
        <span class="author-specialization">
            <xsl:value-of select="."/>

            <xsl:for-each
                    select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='specialization' and not(@qualifier)]">
                <xsl:text>, </xsl:text>
                <xsl:value-of select="."/>
            </xsl:for-each>
        </span>
        </div>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='degree' and not(@qualifier)]" mode="profile">
        <div class="simple-author-view-other">
        <h2><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.degrees</i18n:text></h2>
        <span>

                <xsl:value-of select="."/>

                <xsl:for-each
                        select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='degree' and not(@qualifier)]">
                <xsl:text>; </xsl:text>
                    <xsl:value-of select="."/>
            </xsl:for-each>   </span>
         </div>

    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='OrchidId' and not(@qualifier)]" mode="profile">
        <div class="simple-author-view-other">
            <h2><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.orchid</i18n:text></h2>
            <span>

                <a>
                    <xsl:attribute name="href">
                        <xsl:value-of
                            select="concat('http://orcid.org/',encoder:encode(text()))"/>
                    </xsl:attribute>
                    <xsl:value-of select="text()"/>
                </a>
            </span>
        </div>

    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='external' and @qualifier='hostWork']" mode="profile">
        <h2><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.externalHostwork</i18n:text></h2>
        <p>
            <xsl:call-template name="linebreakIntoBr">
                <xsl:with-param name="text" select="."/>
            </xsl:call-template>
        </p>

    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='updateDate' and not(@qualifier)]" mode="profile">
        <div id="AuthorProfile-updateDate"><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.updateDate</i18n:text>
            <xsl:text> </xsl:text>
            <xsl:value-of select="util:dcDateFormat(., 'MMMM d, yyyy')" />
        </div>
    </xsl:template>






    <!-- A community rendered in the summaryList pattern. Encountered on the community-list and on
        on the front page. -->
    <xsl:template name="authorSummaryList-DIM">
        <xsl:variable name="data" select="./mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim"/>
        <div class="artifact-description">
            <div class="artifact-title">
                <a>
                    <xsl:attribute name="href">
                        <xsl:text>author-page?name.last=</xsl:text><xsl:value-of select="encoder:encode($data/dim:field[@element='name' and @qualifier='last'])"/><xsl:text>&amp;</xsl:text>
                        <xsl:text>name.first=</xsl:text><xsl:value-of select="encoder:encode($data/dim:field[@element='name' and @qualifier='first'])"/>
                    </xsl:attribute>
                    <span>
                        <xsl:choose>
                            <xsl:when test="string-length($data/dim:field[@element='name' and @qualifier='last'][1]) &gt; 0">
                                <xsl:value-of select="$data/dim:field[@element='name' and @qualifier='last'][1]"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:text>, </xsl:text>
                        <xsl:choose>
                            <xsl:when test="string-length($data/dim:field[@element='name' and @qualifier='first'][1]) &gt; 0">
                                <xsl:value-of select="$data/dim:field[@element='name' and @qualifier='first'][1]"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>

                    </span>
                </a>
                <!--Display community strengths (item counts) if they exist-->
                <xsl:if test="string-length($data/dim:field[@element='format'][@qualifier='extent'][1]) &gt; 0">
                    <xsl:text> [</xsl:text>
                    <xsl:value-of select="$data/dim:field[@element='format'][@qualifier='extent'][1]"/>
                    <xsl:text>]</xsl:text>
                </xsl:if>
            </div>

        </div>
    </xsl:template>


    <xsl:template match="dri:p[@n='author-link-para']">
        <p>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-paragraph</xsl:with-param>
            </xsl:call-template>
            <xsl:choose>
                <!--  does this element have any children -->
                <xsl:when test="child::node()">
                    <xsl:apply-templates />
                </xsl:when>
                <!-- if no children are found we add a space to eliminate self closing tags -->
                <xsl:otherwise>
                    &#160;
                </xsl:otherwise>
            </xsl:choose>

        </p>


    </xsl:template>



</xsl:stylesheet>
