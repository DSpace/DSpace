<?xml version="1.0" encoding="UTF-8" ?>
<!--

  The contents of this file are subject to the license and copyright
  detailed in the LICENSE and NOTICE files at the root of the source
  tree and available online at

  http://www.dspace.org/license/
  Developed by Keiji Suzuki

 -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://www.lyncode.com/xoai"
                xmlns:Locale="http://xml.apache.org/xalan-j/java.util.Locale"
                exclude-result-prefixes="doc Locale">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

    <xsl:template match="/">
        <junii2 xmlns="http://irdb.nii.ac.jp/oai"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://irdb.nii.ac.jp/oai http://irdb.nii.ac.jp/oai/junii2-3-1.xsd"
                version="3.1">
            <!-- title:none = title -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <title><xsl:value-of select="." /></title>
                </xsl:if>
            </xsl:for-each>
            <!-- title:alternative,transcription = alternative -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative' or @name='transcription']/doc:element/doc:field">
                <alternative><xsl:value-of select="." /></alternative>
            </xsl:for-each>
            <!-- contributor:author = creator -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field">
                <xsl:if test="@name='value'">
                    <creator>
                        <xsl:if test="following-sibling::*[2][@name='confidence'] and following-sibling::*[2]/text()&gt;=600">
                            <xsl:choose>
                                <!-- when using researcher number as an authority code -->
                                <xsl:when test="following-sibling::*[1][@name='authority'] and string-length(following-sibling::*[1]/text())=8">
                                    <xsl:attribute name="id">
                                        <xsl:value-of select="concat('http://rns.nii.ac.jp/nr/10000', following-sibling::*[1]/text())"/>
                                    </xsl:attribute>
                                </xsl:when>
                                <!-- when using researcher name resolver id as an authority code -->
                                <xsl:when test="following-sibling::*[1][@name='authority'] and string-length(following-sibling::*[1]/text())=13">
                                    <xsl:attribute name="id">
                                        <xsl:value-of select="concat('http://rns.nii.ac.jp/nr/', following-sibling::*[1]/text())"/>
                                    </xsl:attribute>
                                </xsl:when>
                            </xsl:choose>
                        </xsl:if>
                        <xsl:value-of select="."/>
                    </creator>
                </xsl:if>
            </xsl:for-each>
            <!-- subject -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element">
                <xsl:choose>
                    <!-- subject:none = subject -->
                    <xsl:when test="./doc:field">
                        <subject><xsl:value-of select="./doc:field/text()"/></subject>
                    </xsl:when>
                    <!-- subject:nii = NIIsubject -->
                    <xsl:when test="@name='nii'">
                        <NIIsubject><xsl:value-of select="./doc:element/doc:field/text()"/></NIIsubject>
                    </xsl:when>
                    <!-- subject:ndc = NDC -->
                    <xsl:when test="@name='ndc'">
                        <NDC><xsl:value-of select="./doc:element/doc:field/text()"/></NDC>
                    </xsl:when>
                    <!-- subject:ndlc = NDLC -->
                    <xsl:when test="@name='ndlc'">
                        <NDLC><xsl:value-of select="./doc:element/doc:field/text()"/></NDLC>
                    </xsl:when>
                    <!-- subject:bsh = BSH -->
                    <xsl:when test="@name='bsh'">
                        <BSH><xsl:value-of select="./doc:element/doc:field/text()"/></BSH>
                    </xsl:when>
                    <!-- subject:ndlsh = NDLSH -->
                    <xsl:when test="@name='ndlsh'">
                        <NDLSH><xsl:value-of select="./doc:element/doc:field/text()"/></NDLSH>
                    </xsl:when>
                    <!-- subject:mesh = MeSH -->
                    <xsl:when test="@name='mesh'">
                        <MeSH><xsl:value-of select="./doc:element/doc:field/text()"/></MeSH>
                    </xsl:when>
                    <!-- subject:ddc = DDC -->
                    <xsl:when test="@name='ddc'">
                        <DDC><xsl:value-of select="./doc:element/doc:field/text()"/></DDC>
                    </xsl:when>
                    <!-- subject:lcc = LCC -->
                    <xsl:when test="@name='lcc'">
                        <LCC><xsl:value-of select="./doc:element/doc:field/text()"/></LCC>
                    </xsl:when>
                    <!-- subject:udc = UDC -->
                    <xsl:when test="@name='udc'">
                        <UDC><xsl:value-of select="./doc:element/doc:field/text()"/></UDC>
                    </xsl:when>
                    <!-- subject:lcsh = LCSH -->
                    <xsl:when test="@name='lcsh'">
                        <LCSH><xsl:value-of select="./doc:element/doc:field/text()"/></LCSH>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            <!-- description:abstract = description -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field">
                <description><xsl:value-of select="." /></description>
            </xsl:for-each>
            <!-- publisher:none = publisher -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field">
                <publisher><xsl:value-of select="." /></publisher>
            </xsl:for-each>
            <!-- contributor:any!author,alternative,transcription -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element">
                <xsl:choose>
                    <xsl:when test="./doc:field">
                        <contributor><xsl:value-of select="./doc:field/text()"/></contributor>
                    </xsl:when>
                    <xsl:when test="@name!='author' and @name!='alternative' and @name!='transcription'">
                        <contributor><xsl:value-of select="./doc:element/doc:field/text()"/></contributor>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            <!-- date -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element">
                <xsl:choose>
                    <!-- date:none = date -->
                    <xsl:when test="./doc:field">
                        <date>
                            <xsl:call-template name="formDate">
                                <xsl:with-param name="datestr" select="./doc:field/text()" />
                            </xsl:call-template>
                        </date>
                    </xsl:when>
                    <!-- date:copyright = date -->
                    <!-- date:created   = date -->
                    <!-- date:submitted = date -->
                    <xsl:when test="@name='copyright' or @name='created' or @name='submitted'">
                        <date>
                            <xsl:call-template name="formDate">
                                <xsl:with-param name="datestr" select="./doc:element/doc:field/text()" />
                            </xsl:call-template>
                        </date>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            <!-- type:nii = NIItype -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element[@name='nii']/doc:element/doc:field">
                <NIItype><xsl:value-of select="." /></NIItype>
            </xsl:for-each>
            <!-- format:none = format -->
            <xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:field[text()='ORIGINAL']">
                <xsl:for-each select="following-sibling::doc:element[@name='bitstreams']/doc:element[@name='bitstream']/doc:field">
                    <xsl:if test="@name='format'">
                        <format><xsl:value-of select="." /></format>
                    </xsl:if>
                </xsl:for-each>
            </xsl:for-each>
            <!-- identifier:any!issn,uri,isbn,selfdoi,ichushi,naid,ncid,doi,pmid,scpjid,grantid = identifider -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element">
                <xsl:choose>
                    <xsl:when test="./doc:field">
                        <identifier><xsl:value-of select="./doc:field/text()"/></identifier>
                    </xsl:when>
                    <xsl:when test="@name!='issn' and @name!='uri' and @name!='isbn' and @name!='selfdoi' and @name!='ichushi' and @name!='naid' and @name!='ncid' and @name!='doi' and @name!='pmid' and @name!='scpjid' and @name!='grantid'">
                        <identifier><xsl:value-of select="./doc:element/doc:field/text()"/></identifier>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            <!-- identifier:uri = URI -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <URI><xsl:value-of select="." /></URI>
                </xsl:if>
            </xsl:for-each>
            <!-- fulltext:none = fullTextURL -->
            <xsl:for-each select="doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:field[text()='ORIGINAL']">
                <xsl:for-each select="following-sibling::doc:element[@name='bitstreams']/doc:element[@name='bitstream']/doc:field">
                    <xsl:if test="@name='url'">
                        <fullTextURL><xsl:value-of select="." /></fullTextURL>
                    </xsl:if>
                </xsl:for-each>
            </xsl:for-each>
            <!-- identifier:selfdoi = selfDOI:JaLC -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='selfdoi']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <selfDOI>
                        <xsl:attribute name="ra">
                            <xsl:value-of select="'JaLC'"/>
                        </xsl:attribute>
                        <xsl:value-of select="."/>
                    </selfDOI>
                </xsl:if>
            </xsl:for-each>
            <!-- identifier:isbn = isbn -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:field[@name='isbn']/doc:element/doc:field">
                <isbn><xsl:value-of select="." /></isbn>
            </xsl:for-each>
            <!-- identifier:issn = issn -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='issn' or @name='eissn']/doc:element/doc:field">
                <issn><xsl:value-of select="." /></issn>
            </xsl:for-each>
            <!-- identifier:ncid = NCID -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='ncid']/doc:field">
                <NCID><xsl:value-of select="." /></NCID>
            </xsl:for-each>
            <!-- source:jtitle = jtitle -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='jtitle']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <jtitle><xsl:value-of select="." /></jtitle>
                </xsl:if>
            </xsl:for-each>
            <!-- source:volume = volume -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='volume']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <volume><xsl:value-of select="." /></volume>
                </xsl:if>
            </xsl:for-each>
            <!-- source:issue = issue -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='issue']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <issue><xsl:value-of select="." /></issue>
                </xsl:if>
            </xsl:for-each>
            <!-- source:spage = spage -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='spage']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <spage><xsl:value-of select="." /></spage>
                </xsl:if>
            </xsl:for-each>
            <!-- source.epage = epage -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element[@name='epage']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <epage><xsl:value-of select="." /></epage>
                </xsl:if>
            </xsl:for-each>
            <!-- date:issued -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <dateofissued>
                        <xsl:call-template name="formDate">
                            <xsl:with-param name="datestr" select="." />
                        </xsl:call-template>
                    </dateofissued>
                </xsl:if>
            </xsl:for-each>
            <!-- source:any!jtitle,volume,issue,spage,epage = source -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='source']/doc:element">
                <xsl:choose>
                    <xsl:when test="./doc:field">
                        <source><xsl:value-of select="./doc:field/text()"/></source>
                    </xsl:when>
                    <xsl:when test="@name!='jtitle' and @name!='volume' and @name!='issue' and @name!='spage' and @name!='epage'">
                        <source><xsl:value-of select="./doc:element/doc:field/text()"/></source>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            <!-- language:iso = language -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field">
                <xsl:choose>
                    <xsl:when test="string-length(.)=2">
                        <language><xsl:value-of select="Locale:getISO3Language(Locale:new(.))"/></language>
                    </xsl:when>
                    <xsl:otherwise>
                        <language><xsl:value-of select="." /></language>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
            <!-- relation:none = relation -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field">
                <relation><xsl:value-of select="." /></relation>
            </xsl:for-each>
            <!-- identifier:pmid = pmid -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='pmid']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <pmid>
                        <xsl:call-template name="addPrefix">
                            <xsl:with-param name="value" select="." />
                            <xsl:with-param name="prefix" select="'info:pmid/'"/>
                        </xsl:call-template>
                    </pmid>
                </xsl:if>
            </xsl:for-each>
            <!-- identifier:doi = doi -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <doi>
                        <xsl:call-template name="transformDOI">
                            <xsl:with-param name="value" select="." />
                            <xsl:with-param name="prefix1" select="'info:doi/'"/>
                            <xsl:with-param name="prefix2" select="'http://dx.doi.org/'"/>
                        </xsl:call-template>
                    </doi>
                </xsl:if>
            </xsl:for-each>
            <!-- identifier:naid = NAID -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='naid']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <NAID>
                        <xsl:call-template name="addPrefix">
                            <xsl:with-param name="value" select="." />
                            <xsl:with-param name="prefix" select="'http://ci.nii.ac.jp/naid/'"/>
                        </xsl:call-template>
                    </NAID>
                </xsl:if>
            </xsl:for-each>
            <!-- identifier:ichushi = ichushi -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='ichushi']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <ichushi>
                        <xsl:call-template name="addPrefix">
                            <xsl:with-param name="value" select="." />
                            <xsl:with-param name="prefix" select="'http://search.jamas.or.jp/link/ui/'"/>
                        </xsl:call-template>
                    </ichushi>
                </xsl:if>
            </xsl:for-each>
            <!-- relation -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='relation']/doc:element">
                <xsl:choose>
                    <!-- relation:isversionof = isVersionOf -->
                    <xsl:when test="@name='isversionof'">
                        <isVersionOf><xsl:value-of select="./doc:element/doc:field/text()"/></isVersionOf>
                    </xsl:when>
                    <!-- relation:hasversion = hasVersion -->
                    <xsl:when test="@name='hasversion'">
                        <hasVersion><xsl:value-of select="./doc:element/doc:field/text()"/></hasVersion>
                    </xsl:when>
                    <!-- relation:isreplacedby = isReplacedBy -->
                    <xsl:when test="@name='isreplacedby'">
                        <isReplacedBy><xsl:value-of select="./doc:element/doc:field/text()"/></isReplacedBy>
                    </xsl:when>
                    <!-- relation:replaces = replaces -->
                    <xsl:when test="@name='replaces'">
                        <replaces><xsl:value-of select="./doc:element/doc:field/text()"/></replaces>
                    </xsl:when>
                    <!-- relation:isrequiredby = isRequiredBy -->
                    <xsl:when test="@name='isrequiredby'">
                        <isRequiredBy><xsl:value-of select="./doc:element/doc:field/text()"/></isRequiredBy>
                    </xsl:when>
                    <!-- relation:requires = requires -->
                    <xsl:when test="@name='requires'">
                        <requires><xsl:value-of select="./doc:element/doc:field/text()"/></requires>
                    </xsl:when>
                    <!-- relation:ispartof = isPartOf -->
                    <xsl:when test="@name='ispartof'">
                        <isPartOf><xsl:value-of select="./doc:element/doc:field/text()"/></isPartOf>
                    </xsl:when>
                    <!-- relation:haspart = hasPart -->
                    <xsl:when test="@name='haspart'">
                        <hasPart><xsl:value-of select="./doc:element/doc:field/text()"/></hasPart>
                    </xsl:when>
                    <!-- relation:isreferencedby = isReferencedBy -->
                    <xsl:when test="@name='isreferencedby'">
                        <isReferencedBy><xsl:value-of select="./doc:element/doc:field/text()"/></isReferencedBy>
                    </xsl:when>
                    <!-- relation:references = references -->
                    <xsl:when test="@name='references'">
                        <references><xsl:value-of select="./doc:element/doc:field/text()"/></references>
                    </xsl:when>
                    <!-- relation:isformatof = isFormatOf -->
                    <xsl:when test="@name='isformatof'">
                        <isFormatOf><xsl:value-of select="./doc:element/doc:field/text()"/></isFormatOf>
                    </xsl:when>
                    <!-- relation:hasformat = hasFormat -->
                    <xsl:when test="@name='hasformat'">
                        <hasFormat><xsl:value-of select="./doc:element/doc:field/text()"/></hasFormat>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            <!-- coverage -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='coverage']/doc:element">
                <xsl:choose>
                    <!-- coverage:none = coverage -->
                    <xsl:when test="./doc:field">
                        <coverage><xsl:value-of select="./doc:field/text()"/></coverage>
                    </xsl:when>
                    <!-- coverage:spatial = spatial -->
                    <xsl:when test="@name='spatial'">
                        <spatial><xsl:value-of select="./doc:element/doc:field/text()"/></spatial>
                    </xsl:when>
                    <!-- coverage:niispatial = NIIspatial -->
                    <xsl:when test="@name='niispatial'">
                        <NIIspatial><xsl:value-of select="./doc:element/doc:field/text()"/></NIIspatial>
                    </xsl:when>
                    <!-- coverage:temporal = temporal -->
                    <xsl:when test="@name='temporal'">
                        <temporal><xsl:value-of select="./doc:element/doc:field/text()"/></temporal>
                    </xsl:when>
                    <!-- coverage:niitemporal = NIItemporal -->
                    <xsl:when test="@name='niitemporal'">
                        <NIItemporal><xsl:value-of select="./doc:element/doc:field/text()"/></NIItemporal>
                    </xsl:when>
                </xsl:choose>
            </xsl:for-each>
            <!-- rights:none = rights -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:elment/doc:field">
                <rights><xsl:value-of select="." /></rights>
            </xsl:for-each>
            <!-- textversion:none = textversion -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='textversion']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <textversion><xsl:value-of select="." /></textversion>
                </xsl:if>
            </xsl:for-each>
            <!--  description:DegreeNumber = grantid-->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='grantid']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <grantid><xsl:value-of select="." /></grantid>
                </xsl:if>
            </xsl:for-each>
            <!-- date:dateofgranted = dateofgranted -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='granted']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <dateofgranted><xsl:value-of select="." /></dateofgranted>
                </xsl:if>
            </xsl:for-each>
            <!-- description:DegreeDiscipline = degreename -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='degreename']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <degreename><xsl:value-of select="." /></degreename>
                </xsl:if>
            </xsl:for-each>
            <!-- description:DegreeGrantor = grantor -->
            <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='grantor']/doc:element/doc:field">
                <xsl:if test="position() = 1">
                    <grantor><xsl:value-of select="." /></grantor>
                </xsl:if>
            </xsl:for-each>
        </junii2>
    </xsl:template>

    <!-- dc.identifier.doi transforming -->
    <xsl:template name="transformDOI">
        <xsl:param name="value" />
        <xsl:param name="prefix1" />
        <xsl:param name="prefix2" />
        <xsl:choose>
            <xsl:when test="starts-with($value, $prefix1)">
                <xsl:value-of select="$value" />
            </xsl:when>
            <xsl:when test="starts-with($value, $prefix2)">
                <xsl:value-of select="concat($prefix1, substring-after($value, $prefix2))" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($prefix1, $value)" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- dc.identifier.pmid,naid,ichushi transforming -->
    <xsl:template name="addPrefix">
        <xsl:param name="value" />
        <xsl:param name="prefix" />
        <xsl:choose>
            <xsl:when test="starts-with($value, $prefix)">
                <xsl:value-of select="$value" />
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($prefix, $value)" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Date format -->
    <xsl:template name="formDate">
        <xsl:param name="datestr" />
        <xsl:variable name="sub">
            <xsl:choose>
                <xsl:when test="string-length($datestr)&gt;=10">
                    <xsl:value-of select="substring($datestr,1,10)" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$datestr" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="date">
            <xsl:choose>
                <xsl:when test="contains($sub, '-00-00')">
                    <xsl:value-of select="substring-before($sub, '-00-00')" />
                </xsl:when>
                <xsl:when test="contains($sub, '-00')">
                    <xsl:value-of select="substring-before($sub, '-00')" />
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$sub" />
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:value-of select="$date" />
    </xsl:template>

</xsl:stylesheet>
