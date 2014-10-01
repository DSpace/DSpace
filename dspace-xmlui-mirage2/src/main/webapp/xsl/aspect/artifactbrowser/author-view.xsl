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


        <div class="row">

            <div class="col-sm-12 col-xs-9 col-xxs-12 col-xs-push-3 col-xxs-push-0">

                <h2>
                    <xsl:value-of
                            select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='last']"/>
                    <xsl:text>, </xsl:text><xsl:value-of
                        select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='first']"/>
                </h2>

                <xsl:if test="(./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)] and
        (./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='author' and @qualifier='internal']='true'))">
                    <blockquote>
                        <p>
                            <xsl:apply-templates
                                    select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)][1]"
                                    mode="profile"/>
                        </p>
                    </blockquote>

                </xsl:if>
            </div>
            <div class="col-xs-3 hidden-sm hidden-md hidden-lg col-xxs-12 col-xs-pull-9 col-xxs-push-0">
                <div class="ds-author-picture small hidden-xxs">
                    <xsl:apply-templates mode="author-pic-responsive" select="./mets:fileSec/mets:fileGrp/mets:file"/>
                    <xsl:if test="count(./mets:fileSec/mets:fileGrp/mets:file)=0">
                    <xsl:element name="span" namespace="http://www.w3.org/1999/xhtml">
                        <div class="author-pic small icon">
                            <i class="glyphicon glyphicon-user"/>
                        </div>
                    </xsl:element>
                    </xsl:if>
                </div>
            </div>
        </div>
        <div class="row author-profile">

            <div class="col-xs-12 ">
            <div class="author-details-wrapper">
                <div class="ds-author-picture hidden-xs visible-xxs">
                    <xsl:attribute name="class">
                        <xsl:text>ds-author-picture hidden-xs</xsl:text>
                        <xsl:choose>
                            <xsl:when test="count(./mets:fileSec/mets:fileGrp/mets:file)=0">
                                <xsl:text> hidden-xxs</xsl:text>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:text> visible-xxs</xsl:text>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
                    <xsl:apply-templates mode="author-pic" select="./mets:fileSec/mets:fileGrp/mets:file"/>
                    <xsl:if test="count(./mets:fileSec/mets:fileGrp/mets:file)=0">
                    <xsl:element name="span" namespace="http://www.w3.org/1999/xhtml">
                        <div class="author-pic icon">
                            <i class="glyphicon glyphicon-user"/>
                        </div>
                    </xsl:element>
                    </xsl:if>
                </div>

                <div class="author-details clearfix">

                    <div class="author-details-minimal">
                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='biography' and not(@qualifier)]"
                                mode="profile-minimal"/>

                    </div>
                    <div class="author-details-full hidden">

                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='biography' and not(@qualifier)]"
                                mode="profile"/>


                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='variant'][1]"
                                mode="profile"/>

                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='specialization' and not(@qualifier)][1]"
                                mode="profile"/>

                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='degree' and not(@qualifier)][1]"
                                mode="profile"/>
                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='OrchidId' and not(@qualifier)][1]"
                                mode="profile"/>

                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='site'  and not(@qualifier)][1]"
                                mode="profile"/>

                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='external' and @qualifier='hostWork']"
                                mode="profile"/>

                        <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='telephone' and not(@qualifier)] or
                                    ./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]">
                            <dl>
                                <dt>
                                    <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.contactInformation</i18n:text>
                                </dt>
                                <dd>
                                    <ul class="list-unstyled">
                                        <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='telephone' and not(@qualifier)]">
                                            <li>
                                                <xsl:value-of
                                                        select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='telephone' and not(@qualifier)]"/>
                                            </li>
                                        </xsl:if>
                                        <xsl:if test="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]">
                                            <li>
                                                <a>
                                                    <xsl:attribute name="href">
                                                        <xsl:text>mailto:</xsl:text><xsl:value-of
                                                            select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]"/>
                                                    </xsl:attribute>
                                                    <xsl:value-of
                                                            select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='email' and not(@qualifier)]"/>
                                                </a>
                                            </li>
                                        </xsl:if>
                                    </ul>
                                </dd>
                            </dl>
                        </xsl:if>
                        <xsl:apply-templates
                                select="./mets:dmdSec/mets:mdWrap[@OTHERMDTYPE='DIM']/mets:xmlData/dim:dim/dim:field[@mdschema='authorProfile' and @element='updateDate' and not(@qualifier)]"
                                mode="profile"/>

                    </div>
                </div>
                </div>

                <div class="row author-details-button-wrapper">
                    <div class="col-sm-10">
                        <button id="author-details-hide-button" class="ds-button-field btn btn-default author-details-button hidden" name="submit"><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.hide</i18n:text></button>
                        <button id="author-details-show-button" class="ds-button-field btn btn-default author-details-button" name="submit"><i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.show</i18n:text></button>
                    </div>
                </div>
                <span>

                </span>
            </div>
        </div>
    </xsl:template>

    <xsl:template name="author-profile-stats-button">
        <div class='additional-button-wrapper'>
            <div class='pull-right'>
                <button class="btn btn-default show-stats-filter">
                    <i18n:text catalogue="default">xmlui.statistics.show-filter</i18n:text>
                </button>
            </div>
        </div>
    </xsl:template>

    <xsl:variable name="separator" select="':##:'"/>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='site'  and not(@qualifier)]" mode="profile">
        <dl>
            <dt>
                <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.externalLinks</i18n:text>
            </dt>
            <dd>
                <ul class="list-unstyled">
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
            </dd>
        </dl>

    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='biography' and not(@qualifier)]" mode="profile">
        <h3>
            <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.biography</i18n:text>
        </h3>
        <div class="ds-author-bio">
            <p>
                <xsl:value-of select="." disable-output-escaping="yes"/>
            </p>
        </div>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='biography' and not(@qualifier)]" mode="profile-minimal">
        <h3>
            <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.biography</i18n:text>
        </h3>
        <div class="ds-author-bio">
            <p>
                <xsl:variable name="bio" select="."/>
                <xsl:value-of select="util:shortenString($bio, 530, 5)" disable-output-escaping="yes"/>
            </p>
        </div>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='biography' and @qualifier='title']" mode="profile">
        <xsl:value-of select="."/>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='variant']" mode="profile">
        <dl>
            <dt>
                <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.nameVariants</i18n:text>
            </dt>
            <dd>
                <span class="author-names">
                    <xsl:value-of select="."/>

                    <xsl:for-each
                            select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='name' and @qualifier='variant']">
                        <xsl:text>; </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </span>
            </dd>
        </dl>
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
        <xsl:for-each
                select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='department' and not(@qualifier)]">
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

        </img>
    </xsl:template>

    <xsl:template match="mets:fileSec/mets:fileGrp/mets:file" mode="author-pic-responsive">
        <img class="author-pic img-responsive" alt="author picture">
            <xsl:attribute name="src">
                <xsl:value-of select="mets:FLocat/@xlink:href"/>
            </xsl:attribute>

        </img>
    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='specialization' and not(@qualifier)]" mode="profile">
        <dl>
            <dt>
                <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.fieldsOfSpecialization</i18n:text>
            </dt>
            <dd>
                <span class="author-specialization">
                    <xsl:value-of select="."/>

                    <xsl:for-each
                            select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='specialization' and not(@qualifier)]">
                        <xsl:text>, </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </span>
            </dd>
        </dl>

    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='degree' and not(@qualifier)]" mode="profile">
        <dl>
            <dt>
                <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.degrees</i18n:text>
            </dt>
            <dd>
                <span>

                    <xsl:value-of select="."/>

                    <xsl:for-each
                            select="./following-sibling::dim:field[@mdschema='authorProfile' and @element='degree' and not(@qualifier)]">
                        <xsl:text>; </xsl:text>
                        <xsl:value-of select="."/>
                    </xsl:for-each>
                </span>
            </dd>
        </dl>


    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='OrchidId' and not(@qualifier)]" mode="profile">

        <dl>
            <dt>
                <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.orchid</i18n:text>
            </dt>
            <dd>
                <span>

                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of
                                    select="concat('http://orcid.org/',encoder:encode(text()))"/>
                        </xsl:attribute>
                        <xsl:value-of select="text()"/>
                    </a>
                </span>
            </dd>
        </dl>


    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='external' and @qualifier='hostWork']" mode="profile">
        <dl>
            <dt>
                <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.externalHostwork</i18n:text>
            </dt>
            <dd>
                <p>
                    <xsl:value-of select="." disable-output-escaping="yes"/>
                </p>
            </dd>
        </dl>

    </xsl:template>

    <xsl:template match="dim:field[@mdschema='authorProfile' and @element='updateDate' and not(@qualifier)]" mode="profile">
        <div id="AuthorProfile-updateDate">
            <i18n:text>xmlui.authorprofile.artifactbrowser.AuthorProfile.updateDate</i18n:text>
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
                        <xsl:text>author-page?name.last=</xsl:text><xsl:value-of
                            select="encoder:encode($data/dim:field[@element='name' and @qualifier='last'])"/><xsl:text>&amp;</xsl:text>
                        <xsl:text>name.first=</xsl:text><xsl:value-of
                            select="encoder:encode($data/dim:field[@element='name' and @qualifier='first'])"/>
                    </xsl:attribute>
                    <span>
                        <xsl:choose>
                            <xsl:when
                                    test="string-length($data/dim:field[@element='name' and @qualifier='last'][1]) &gt; 0">
                                <xsl:value-of select="$data/dim:field[@element='name' and @qualifier='last'][1]"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <i18n:text>xmlui.dri2xhtml.METS-1.0.no-title</i18n:text>
                            </xsl:otherwise>
                        </xsl:choose>
                        <xsl:text>, </xsl:text>
                        <xsl:choose>
                            <xsl:when
                                    test="string-length($data/dim:field[@element='name' and @qualifier='first'][1]) &gt; 0">
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


</xsl:stylesheet>
