<?xml version="1.0" encoding="UTF-8" ?>
<!--
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://www.lyncode.com/xoai"
                xmlns:ms="http://w3id.org/meta-share/meta-share/"
                xmlns:fn="http://custom.crosswalk.functions"
                xmlns:exsl="http://exslt.org/common"
                exclude-result-prefixes="doc fn"
                version="1.0">

    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>

    <!-- VARIABLES BEGIN -->
    <xsl:variable name="UPPER_CHARS" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ'"/>
    <xsl:variable name="LOWER_CHARS" select="'abcdefghijklmnopqrstuvwxyz'"/>
    <xsl:variable name="identifier_uri" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/>

    <xsl:variable name="handle" select="/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()"/>

    <xsl:variable name="type"
                  select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']/text()"/>

    <xsl:variable name="upperType">
        <xsl:value-of select="translate(substring($type,1,1), $LOWER_CHARS, $UPPER_CHARS)"/>
        <xsl:value-of select="substring($type,2)"/>
    </xsl:variable>

    <xsl:variable name="mediaType">
        <!-- No media type for toolService -->
        <xsl:if test="not($type='toolService')">
            <xsl:choose>
                <xsl:when test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='mediaType']/doc:element/doc:field[@name='value']">
                    <xsl:value-of select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='mediaType']/doc:element/doc:field[@name='value']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="fn:logMissing('mediaType',$handle)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:variable>

    <xsl:variable name="upperMediaType">
        <xsl:value-of select="translate(substring($mediaType,1,1), $LOWER_CHARS, $UPPER_CHARS)"/>
        <xsl:value-of select="substring($mediaType,2)"/>
    </xsl:variable>

    <xsl:variable name="detailedType">
        <!-- No detailed type for corpus -->
        <xsl:if test="not($type='corpus')">
            <xsl:choose>
                <xsl:when
                        test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='detailedType']/doc:element/doc:field[@name='value'] = 'wordList' ">
                    <xsl:value-of select="'wordlist'"/>
                </xsl:when>
                <xsl:when
                        test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='detailedType']/doc:element/doc:field[@name='value'] ">
                    <xsl:value-of select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='detailedType']/doc:element/doc:field[@name='value']"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="fn:logMissing('detailedType',$handle)"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
    </xsl:variable>

    <xsl:variable name="files">
        <xsl:copy-of
                select="//doc:element[@name='bundle']/doc:field[@name='name'][text()='ORIGINAL']/..//doc:element[@name='bitstream']"/>
    </xsl:variable>

    <xsl:variable name="lr.download.all.limit.max.file.size" select="fn:getProperty('download.all.limit.max.file.size')"/>
    <xsl:variable name="lr.elg.download-location.exposed" select="fn:getProperty('elg.downloadLocation.exposed')"/>
    <!-- VARIABLES END -->

    <xsl:template match="/">
        <xsl:call-template name="MetadataRecord"/>
    </xsl:template>

    <xsl:template name="MetadataRecord">
        <ms:MetadataRecord xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://w3id.org/meta-share/meta-share/ ../Schema/ELG-SHARE.xsd">
            <ms:MetadataRecordIdentifier ms:MetadataRecordIdentifierScheme="http://w3id.org/meta-share/meta-share/elg">value automatically assigned - leave as is</ms:MetadataRecordIdentifier>
            <ms:metadataCreationDate>
                <xsl:call-template name="formatDate">
                    <xsl:with-param name="date"
                                    select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='available']/doc:element/doc:field[@name='value']"/>
                </xsl:call-template>
            </ms:metadataCreationDate>
            <ms:metadataLastDateUpdated>
                <xsl:call-template name="formatDate">
                    <xsl:with-param name="date" select="doc:metadata/doc:element[@name='others']/doc:field[@name='lastModifyDate']"/>
                </xsl:call-template>
            </ms:metadataLastDateUpdated>
            <ms:compliesWith>http://w3id.org/meta-share/meta-share/ELG-SHARE</ms:compliesWith>
            <ms:sourceOfMetadataRecord>
                <ms:repositoryName xml:lang="en">LINDAT/CLARIAH-CZ</ms:repositoryName>
            </ms:sourceOfMetadataRecord>
            <ms:sourceMetadataRecord>
                <ms:MetadataRecordIdentifier>
                    <xsl:attribute name="ms:MetadataRecordIdentifierScheme">http://purl.org/spar/datacite/handle</xsl:attribute>
                    <xsl:value-of select="$identifier_uri"/>
                </ms:MetadataRecordIdentifier>
            </ms:sourceMetadataRecord>
            <ms:DescribedEntity>
                <xsl:call-template name="LanguageResource"/>
            </ms:DescribedEntity>
        </ms:MetadataRecord>
    </xsl:template>

    <xsl:template name="LanguageResource">
        <ms:LanguageResource>
            <ms:entityType>LanguageResource</ms:entityType>
            <xsl:call-template name="resourceName"/>
            <xsl:call-template name="description"/>
            <ms:version>unspecified</ms:version>
            <ms:additionalInfo>
                <ms:landingPage><xsl:value-of select="$identifier_uri"/></ms:landingPage>
            </ms:additionalInfo>
            <xsl:call-template name="keyword"/>
            <xsl:call-template name="resourceProvider"/>
            <ms:publicationDate>
                <xsl:call-template name="formatDate">
                    <xsl:with-param name="date" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']"/>
                </xsl:call-template>
            </ms:publicationDate>
            <xsl:call-template name="resourceCreator"/>
            <xsl:call-template name="fundingProject"/>
            <!-- TODO replaces need a title; should add that to the xoai format -->
            <xsl:call-template name="LRSubclass"/>
        </ms:LanguageResource>
    </xsl:template>

    <xsl:template name="resourceName">
        <ms:resourceName xml:lang="en"><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']"/></ms:resourceName>
    </xsl:template>

    <xsl:template name="description">
        <ms:description xml:lang="en"><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']"/></ms:description>
    </xsl:template>

    <xsl:template name="keyword">
        <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
            <ms:keyword xml:lang='en'><xsl:value-of select="."/></ms:keyword>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="resourceProvider">
        <ms:resourceProvider>
            <ms:Organization>
                <ms:actorType>Organization</ms:actorType>
                <ms:organizationName xml:lang="en"><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']"/></ms:organizationName>
            </ms:Organization>
        </ms:resourceProvider>
    </xsl:template>

    <xsl:template name="resourceCreator">
        <xsl:for-each select="doc:metadata/doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']">
            <xsl:if test="not(. = 'et al.')">
                <ms:resourceCreator>
                    <xsl:choose>
                        <!-- assume names stored in 'last, first, any, other' fashion -->
                        <xsl:when test="contains(., ', ')">
                            <xsl:variable name="surname" select="tokenize(., ', ')[1]"/>
                            <xsl:variable name="given">
                                <xsl:for-each select="tokenize(., ', ')">
                                    <xsl:if test="position() &gt; 1">
                                        <xsl:value-of select="."/>
                                        <xsl:if test="position() != last()">
                                            <xsl:text>, </xsl:text>
                                        </xsl:if>
                                    </xsl:if>
                                </xsl:for-each>
                            </xsl:variable>
                            <ms:Person>
                                <ms:actorType>Person</ms:actorType>
                                <!--  xml:lang doesn't make much sense for surnames and givenName; it should be "script", en mandatory -->
                                <ms:surname xml:lang="en"><xsl:value-of select="$surname"/></ms:surname>
                                <ms:givenName xml:lang="en"><xsl:value-of select="$given"/></ms:givenName>
                            </ms:Person>
                        </xsl:when>
                        <!-- no comma assume it's an org -->
                        <xsl:otherwise>
                            <ms:Organization>
                                <ms:actorType>Organization</ms:actorType>
                                <ms:organizationName xml:lang="en"><xsl:value-of select="."/></ms:organizationName>
                            </ms:Organization>
                        </xsl:otherwise>
                    </xsl:choose>
                </ms:resourceCreator>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="fundingProject">
        <xsl:for-each select="doc:metadata/doc:element[@name='local']/doc:element[@name='sponsor']/doc:element/doc:field[@name='value']">
            <xsl:variable name="proj_arr" select="tokenize(., ';')"/>
            <xsl:if test="count($proj_arr) &gt;= 4">
                <xsl:if
                        test="$proj_arr[1] != '' and $proj_arr[2] != '' and $proj_arr[3] != '' and $proj_arr[4] != ''">
                    <ms:fundingProject>
                        <ms:projectName xml:lang="en">
                            <xsl:value-of select="$proj_arr[4]"/>
                        </ms:projectName>
                        <xsl:choose>
                            <xsl:when test="starts-with($proj_arr[5], 'info:')">
                                <ms:ProjectIdentifier>
                                    <xsl:attribute name="ms:ProjectIdentifierScheme">http://w3id.org/meta-share/meta-share/OpenAIRE</xsl:attribute>
                                    <xsl:value-of select="$proj_arr[5]"/>
                                </ms:ProjectIdentifier>
                            </xsl:when>
                            <xsl:otherwise>
                                <ms:grantNumber>
                                    <xsl:value-of select="$proj_arr[2]"/>
                                </ms:grantNumber>
                            </xsl:otherwise>
                        </xsl:choose>
                        <ms:fundingType>
                            <xsl:choose>
                                <xsl:when test="$proj_arr[1] = 'Other'">http://w3id.org/meta-share/meta-share/other</xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="concat('http://w3id.org/meta-share/meta-share/', $proj_arr[1])"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </ms:fundingType>
                        <ms:funder>
                            <ms:Organization>
                                <ms:actorType>Organization</ms:actorType>
                                <ms:organizationName xml:lang="en"><xsl:value-of select="$proj_arr[3]"/></ms:organizationName>
                            </ms:Organization>
                        </ms:funder>
                    </ms:fundingProject>
                </xsl:if>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="LRSubclass">
        <ms:LRSubclass>
            <xsl:choose>
                <xsl:when test="$type = 'corpus'">
                    <xsl:call-template name="corpus"/>
                </xsl:when>
                <xsl:when test="$type = 'toolService'">
                    <xsl:call-template name="toolService"/>
                </xsl:when>
                <xsl:when test="$type = 'languageDescription'">
                    <xsl:call-template name="languageDescription"/>
                </xsl:when>
                <xsl:when test="$type = 'lexicalConceptualResource'">
                    <xsl:call-template name="lexicalConceptualResource"/>
                </xsl:when>
            </xsl:choose>
        </ms:LRSubclass>
    </xsl:template>

    <xsl:template name="corpus">
        <ms:Corpus>
            <ms:lrType>Corpus</ms:lrType>
            <ms:corpusSubclass>http://w3id.org/meta-share/meta-share/unspecified</ms:corpusSubclass>
            <xsl:call-template name="CommonMediaPart"/>
            <xsl:call-template name="Distribution"/>
            <xsl:call-template name="personalSensitiveAnon"/>
        </ms:Corpus>
    </xsl:template>

    <xsl:template name="personalSensitiveAnon">
        <ms:personalDataIncluded>http://w3id.org/meta-share/meta-share/noP</ms:personalDataIncluded>
        <ms:sensitiveDataIncluded>http://w3id.org/meta-share/meta-share/noS</ms:sensitiveDataIncluded>
    </xsl:template>

    <xsl:template name="CommonMediaPart">
        <xsl:param name="noMediaPart" select="false()"/>
        <xsl:choose>
            <xsl:when test="$noMediaPart">
                <ms:unspecifiedPart>
                    <xsl:call-template name="lingualityAndLanguges"/>
                </ms:unspecifiedPart>
            </xsl:when>
            <xsl:otherwise>
                <xsl:variable name="name" select="concat($upperType, 'MediaPart')"/>
                <xsl:element name="ms:{$name}">
                    <xsl:call-template name="commonMediaElements"/>
                </xsl:element>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="commonMediaElements">
        <xsl:variable name="name" select="concat($upperType, $upperMediaType, 'Part')"/>
        <xsl:variable name="name2" >
            <xsl:choose>
                <xsl:when test="$type = 'lexicalConceptualResource'">lcrMediaType</xsl:when>
                <xsl:when test="$type = 'languageDescription'">ldMediaType</xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($type, 'MediaType')"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:element name="ms:{$name}">
            <xsl:element name="ms:{$name2}"><xsl:value-of select="$name"/></xsl:element>
            <ms:mediaType><xsl:value-of select="concat('http://w3id.org/meta-share/meta-share/', $mediaType)"/></ms:mediaType>
            <xsl:call-template name="lingualityAndLanguges"/>
            <xsl:choose>
                <xsl:when test="$mediaType = 'audio'">
                    <xsl:call-template name="audio"/>
                </xsl:when>
                <xsl:when test="$mediaType = 'video'">
                    <xsl:call-template name="video"/>
                </xsl:when>
                <xsl:when test="$mediaType = 'text'">
                    <xsl:call-template name="text"/>
                </xsl:when>
                <xsl:when test="$mediaType = 'image'">
                    <xsl:call-template name="image"/>
                </xsl:when>
            </xsl:choose>
        </xsl:element>
    </xsl:template>

    <xsl:template name="lingualityAndLanguges">
        <ms:lingualityType>
            <xsl:variable name="langCount" select="count(/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value'])"/>
            <xsl:text>http://w3id.org/meta-share/meta-share/</xsl:text>
            <xsl:choose>
                <xsl:when test="$langCount=1">monolingual</xsl:when>
                <xsl:when test="$langCount=2">bilingual</xsl:when>
                <xsl:otherwise>multilingual</xsl:otherwise>
            </xsl:choose>
        </ms:lingualityType>
        <ms:multilingualityType>http://w3id.org/meta-share/meta-share/unspecified</ms:multilingualityType>
        <xsl:call-template name="Languages"/>
    </xsl:template>

    <xsl:template name="commonCorpusMediaElements">
        <xsl:variable name="name" select="concat('Corpus', $upperMediaType, 'Part')"/>
        <xsl:element name="ms:{$name}">
            <ms:corpusMediaType><xsl:value-of select="$name"/></ms:corpusMediaType>
            <ms:mediaType><xsl:value-of select="concat('http://w3id.org/meta-share/meta-share/', $mediaType)"/></ms:mediaType>
            <ms:lingualityType>
                <xsl:variable name="langCount" select="count(/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value'])"/>
                <xsl:text>http://w3id.org/meta-share/meta-share/</xsl:text>
                <xsl:choose>
                    <xsl:when test="$langCount=1">monolingual</xsl:when>
                    <xsl:when test="$langCount=2">bilingual</xsl:when>
                    <xsl:otherwise>multilingual</xsl:otherwise>
                </xsl:choose>
            </ms:lingualityType>
            <xsl:call-template name="Languages"/>
        </xsl:element>
    </xsl:template>

    <xsl:template name="text"></xsl:template>
    <xsl:template name="audio"></xsl:template>
    <!-- XXX
     elg.xml:53: element CorpusVideoPart: Schemas validity error : Element '{http://w3id.org/meta-share/meta-share/}CorpusVideoPart': Missing child element(s). Expected is one of ( {http://w3id.org/meta-share/meta-share/}language, {http://w3id.org/meta-share/meta-share/}languageVariety, {http://w3id.org/meta-share/meta-share/}modalityType, {http://w3id.org/meta-share/meta-share/}VideoGenre, {http://w3id.org/meta-share/meta-share/}typeOfVideoContent ).
elg.xml:62: element typeOfVideoContent: Schemas validity error : Element '{http://w3id.org/meta-share/meta-share/}typeOfVideoContent': This element is not expected.
     -->
    <xsl:template name="video">
        <ms:typeOfVideoContent xml:lang="en">unspecified</ms:typeOfVideoContent>
    </xsl:template>
    <xsl:template name="image">
        <ms:typeOfImageContent xml:lang="en">unspecified</ms:typeOfImageContent>
    </xsl:template>

    <xsl:template name="Language">
        <xsl:param name="isoCode"/>
        <xsl:choose>
            <xsl:when test="$isoCode = 'und' or $isoCode = 'mis'">
                <xsl:call-template name="uncoded_languages"/>
            </xsl:when>
            <xsl:otherwise>
                <ms:language>
                    <xsl:call-template name="ms_language_inside">
                        <xsl:with-param name="isoCode" select="$isoCode"/>
                    </xsl:call-template>
                </ms:language>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="ms_language_inside">
        <xsl:param name="isoCode"/>
        <xsl:variable name="modifiedIsoCode">
            <xsl:choose>
                <xsl:when test="$isoCode = 'eng'">en</xsl:when>
                <xsl:when test="$isoCode = 'ces'">cs</xsl:when>
                <xsl:when test="$isoCode = 'lit'">lt</xsl:when>
                <xsl:when test="$isoCode = 'urd'">ur</xsl:when>
                <xsl:when test="$isoCode = 'sme'">se</xsl:when>
                <xsl:when test="$isoCode = 'kor'">ko</xsl:when>
                <xsl:when test="$isoCode = 'uig'">ug</xsl:when>
                <xsl:when test="$isoCode = 'vie'">vi</xsl:when>
                <xsl:when test="$isoCode = 'bel'">be</xsl:when>
                <xsl:when test="$isoCode = 'tur'">tr</xsl:when>
                <xsl:when test="$isoCode = 'san'">sa</xsl:when>
                <xsl:when test="$isoCode = 'slk'">sk</xsl:when>
                <xsl:when test="$isoCode = 'ukr'">uk</xsl:when>
                <xsl:when test="$isoCode = 'rus'">ru</xsl:when>
                <xsl:when test="$isoCode = 'ara'">ar</xsl:when>
                <xsl:when test="$isoCode = 'fas'">fa</xsl:when>
                <xsl:when test="$isoCode = 'deu'">de</xsl:when>
                <xsl:when test="$isoCode = 'its'">it</xsl:when>
                <xsl:when test="$isoCode = 'hin'">hi</xsl:when>
                <xsl:when test="$isoCode = 'som'">so</xsl:when>
                <xsl:when test="$isoCode = 'ell'">el</xsl:when>
                <xsl:when test="$isoCode = 'pol'">pl</xsl:when>
                <xsl:when test="$isoCode = 'por'">pt</xsl:when>
                <xsl:when test="$isoCode = 'fra'">fr</xsl:when>
                <xsl:when test="$isoCode = 'afr'">af</xsl:when>
                <xsl:when test="$isoCode = 'bul'">bg</xsl:when>
                <xsl:when test="$isoCode = 'cat'">ca</xsl:when>
                <xsl:when test="$isoCode = 'chu'">cu</xsl:when>
                <xsl:when test="$isoCode = 'cym'">cy</xsl:when>
                <xsl:when test="$isoCode = 'dan'">da</xsl:when>
                <xsl:when test="$isoCode = 'est'">et</xsl:when>
                <xsl:when test="$isoCode = 'eus'">eu</xsl:when>
                <xsl:when test="$isoCode = 'fao'">fo</xsl:when>
                <xsl:when test="$isoCode = 'fas'">fa</xsl:when>
                <xsl:when test="$isoCode = 'fin'">fi</xsl:when>
                <xsl:when test="$isoCode = 'gle'">ga</xsl:when>
                <xsl:when test="$isoCode = 'heb'">he</xsl:when>
                <xsl:when test="$isoCode = 'hrv'">hr</xsl:when>
                <xsl:when test="$isoCode = 'hun'">hu</xsl:when>
                <xsl:when test="$isoCode = 'hye'">hy</xsl:when>
                <xsl:when test="$isoCode = 'ind'">id</xsl:when>
                <xsl:when test="$isoCode = 'isl'">is</xsl:when>
                <xsl:when test="$isoCode = 'ita'">it</xsl:when>
                <xsl:when test="$isoCode = 'jpn'">ja</xsl:when>
                <xsl:when test="$isoCode = 'lat'">la</xsl:when>
                <xsl:when test="$isoCode = 'lav'">lv</xsl:when>
                <xsl:when test="$isoCode = 'mar'">mr</xsl:when>
                <xsl:when test="$isoCode = 'mlt'">mt</xsl:when>
                <xsl:when test="$isoCode = 'nld'">nl</xsl:when>
                <xsl:when test="$isoCode = 'nno'">nn</xsl:when>
                <xsl:when test="$isoCode = 'nob'">nb</xsl:when>
                <xsl:when test="$isoCode = 'ron'">ro</xsl:when>
                <xsl:when test="$isoCode = 'slv'">sl</xsl:when>
                <xsl:when test="$isoCode = 'srp'">sr</xsl:when>
                <xsl:when test="$isoCode = 'tam'">ta</xsl:when>
                <xsl:when test="$isoCode = 'tel'">te</xsl:when>
                <xsl:when test="$isoCode = 'wol'">wo</xsl:when>
                <xsl:when test="$isoCode = 'zho'">zh</xsl:when>
                <xsl:when test="$isoCode = 'nor'">no</xsl:when>
                <xsl:when test="$isoCode = 'glv'">gv</xsl:when>
                <xsl:when test="$isoCode = 'gla'">gd</xsl:when>
                <xsl:when test="$isoCode = 'swe'">sv</xsl:when>
                <xsl:when test="$isoCode = 'glg'">gl</xsl:when>
                <xsl:when test="$isoCode = 'kaz'">kk</xsl:when>
                <xsl:when test="$isoCode = 'amh'">am</xsl:when>
                <xsl:when test="$isoCode = 'bre'">br</xsl:when>
                <xsl:when test="$isoCode = 'tha'">th</xsl:when>
                <xsl:when test="$isoCode = 'ben'">bn</xsl:when>
                <xsl:when test="$isoCode = 'guj'">gu</xsl:when>
                <xsl:when test="$isoCode = 'kan'">kn</xsl:when>
                <xsl:when test="$isoCode = 'mal'">ml</xsl:when>
                <xsl:when test="$isoCode = 'mkd'">mk</xsl:when>
                <xsl:when test="$isoCode = 'nep'">ne</xsl:when>
                <xsl:when test="$isoCode = 'sqi'">sq</xsl:when>
                <xsl:when test="$isoCode = 'swa'">sw</xsl:when>
                <xsl:when test="$isoCode = 'jav'">jv</xsl:when>
                <xsl:when test="$isoCode = 'tat'">tt</xsl:when>
                <xsl:when test="$isoCode = 'fry'">fy</xsl:when>
                <xsl:when test="$isoCode = 'spa'">es</xsl:when>
                <xsl:when test="$isoCode = 'hbs'">sh</xsl:when>
                <xsl:when test="$isoCode = 'tgl'">tl</xsl:when>
                <xsl:when test="$isoCode = 'pan'">pa</xsl:when>
                <xsl:when test="$isoCode = 'abk'">ab</xsl:when>
                <xsl:when test="$isoCode = 'aka'">ak</xsl:when>
                <xsl:when test="$isoCode = 'arg'">an</xsl:when>
                <xsl:when test="$isoCode = 'asm'">as</xsl:when>
                <xsl:when test="$isoCode = 'ava'">av</xsl:when>
                <xsl:when test="$isoCode = 'aym'">ay</xsl:when>
                <xsl:when test="$isoCode = 'aze'">az</xsl:when>
                <xsl:when test="$isoCode = 'bak'">ba</xsl:when>
                <xsl:when test="$isoCode = 'bam'">bm</xsl:when>
                <xsl:when test="$isoCode = 'bis'">bi</xsl:when>
                <xsl:when test="$isoCode = 'bod'">bo</xsl:when>
                <xsl:when test="$isoCode = 'cha'">ch</xsl:when>
                <xsl:when test="$isoCode = 'che'">ce</xsl:when>
                <xsl:when test="$isoCode = 'chv'">cv</xsl:when>
                <xsl:when test="$isoCode = 'cor'">kw</xsl:when>
                <xsl:when test="$isoCode = 'cre'">cr</xsl:when>
                <xsl:when test="$isoCode = 'div'">dv</xsl:when>
                <xsl:when test="$isoCode = 'dzo'">dz</xsl:when>
                <xsl:when test="$isoCode = 'eop'">ep</xsl:when>
                <xsl:when test="$isoCode = 'ewe'">ee</xsl:when>
                <xsl:when test="$isoCode = 'fij'">fj</xsl:when>
                <xsl:when test="$isoCode = 'ful'">ff</xsl:when>
                <xsl:when test="$isoCode = 'hat'">ht</xsl:when>
                <xsl:when test="$isoCode = 'hau'">ha</xsl:when>
                <xsl:when test="$isoCode = 'her'">hz</xsl:when>
                <xsl:when test="$isoCode = 'hmo'">ho</xsl:when>
                <xsl:when test="$isoCode = 'ibo'">ig</xsl:when>
                <xsl:when test="$isoCode = 'iku'">iu</xsl:when>
                <xsl:when test="$isoCode = 'ile'">ie</xsl:when>
                <xsl:when test="$isoCode = 'ina'">ia</xsl:when>
                <xsl:when test="$isoCode = 'ipk'">ik</xsl:when>
                <xsl:when test="$isoCode = 'kal'">kl</xsl:when>
                <xsl:when test="$isoCode = 'kas'">ks</xsl:when>
                <xsl:when test="$isoCode = 'kat'">ka</xsl:when>
                <xsl:when test="$isoCode = 'kau'">kr</xsl:when>
                <xsl:when test="$isoCode = 'khm'">km</xsl:when>
                <xsl:when test="$isoCode = 'kik'">ki</xsl:when>
                <xsl:when test="$isoCode = 'kir'">ky</xsl:when>
                <xsl:when test="$isoCode = 'kom'">kv</xsl:when>
                <xsl:when test="$isoCode = 'kon'">kg</xsl:when>
                <xsl:when test="$isoCode = 'kur'">ku</xsl:when>
                <xsl:when test="$isoCode = 'lao'">lo</xsl:when>
                <xsl:when test="$isoCode = 'lin'">ln</xsl:when>
                <xsl:when test="$isoCode = 'ltz'">lb</xsl:when>
                <xsl:when test="$isoCode = 'lug'">lg</xsl:when>
                <xsl:when test="$isoCode = 'mah'">mh</xsl:when>
                <xsl:when test="$isoCode = 'mlg'">mg</xsl:when>
                <xsl:when test="$isoCode = 'mob'">mn</xsl:when>
                <xsl:when test="$isoCode = 'mri'">mi</xsl:when>
                <xsl:when test="$isoCode = 'msa'">ms</xsl:when>
                <xsl:when test="$isoCode = 'mya'">my</xsl:when>
                <xsl:when test="$isoCode = 'nau'">na</xsl:when>
                <xsl:when test="$isoCode = 'nav'">nv</xsl:when>
                <xsl:when test="$isoCode = 'ndo'">ng</xsl:when>
                <xsl:when test="$isoCode = 'nya'">ny</xsl:when>
                <xsl:when test="$isoCode = 'oci'">oc</xsl:when>
                <xsl:when test="$isoCode = 'ori'">or</xsl:when>
                <xsl:when test="$isoCode = 'orm'">om</xsl:when>
                <xsl:when test="$isoCode = 'oss'">os</xsl:when>
                <xsl:when test="$isoCode = 'pli'">pi</xsl:when>
                <xsl:when test="$isoCode = 'pus'">ps</xsl:when>
                <xsl:when test="$isoCode = 'que'">qu</xsl:when>
                <xsl:when test="$isoCode = 'roh'">rm</xsl:when>
                <xsl:when test="$isoCode = 'run'">rn</xsl:when>
                <xsl:when test="$isoCode = 'sag'">sg</xsl:when>
                <xsl:when test="$isoCode = 'sin'">si</xsl:when>
                <xsl:when test="$isoCode = 'smo'">sm</xsl:when>
                <xsl:when test="$isoCode = 'sna'">sn</xsl:when>
                <xsl:when test="$isoCode = 'snd'">sd</xsl:when>
                <xsl:when test="$isoCode = 'sot'">st</xsl:when>
                <xsl:when test="$isoCode = 'srd'">sc</xsl:when>
                <xsl:when test="$isoCode = 'ssw'">ss</xsl:when>
                <xsl:when test="$isoCode = 'sun'">su</xsl:when>
                <xsl:when test="$isoCode = 'tah'">ty</xsl:when>
                <xsl:when test="$isoCode = 'tgk'">tg</xsl:when>
                <xsl:when test="$isoCode = 'tir'">ti</xsl:when>
                <xsl:when test="$isoCode = 'ton'">to</xsl:when>
                <xsl:when test="$isoCode = 'tsn'">tn</xsl:when>
                <xsl:when test="$isoCode = 'tso'">ts</xsl:when>
                <xsl:when test="$isoCode = 'tuk'">tk</xsl:when>
                <xsl:when test="$isoCode = 'twi'">tw</xsl:when>
                <xsl:when test="$isoCode = 'uzb'">uz</xsl:when>
                <xsl:when test="$isoCode = 'ven'">ve</xsl:when>
                <xsl:when test="$isoCode = 'vol'">vo</xsl:when>
                <xsl:when test="$isoCode = 'wln'">wa</xsl:when>
                <xsl:when test="$isoCode = 'xho'">xh</xsl:when>
                <xsl:when test="$isoCode = 'yid'">yi</xsl:when>
                <xsl:when test="$isoCode = 'yor'">yo</xsl:when>
                <xsl:when test="$isoCode = 'zha'">za</xsl:when>
                <xsl:when test="$isoCode = 'zul'">zu</xsl:when>
                <xsl:when test="$isoCode = 'bos'">bs</xsl:when>
                <xsl:when test="$isoCode = 'cos'">co</xsl:when>
                <xsl:when test="$isoCode = 'epo'">eo</xsl:when>
                <xsl:when test="$isoCode = 'grn'">gn</xsl:when>
                <xsl:when test="$isoCode = 'zul'">zu</xsl:when>
                <xsl:when test="$isoCode = 'ido'">io</xsl:when>
                <xsl:when test="$isoCode = 'lim'">li</xsl:when>
                <xsl:when test="$isoCode = 'mon'">mn</xsl:when>
                <xsl:when test="$isoCode = 'bos'">bs</xsl:when>
                <xsl:when test="$isoCode = 'kin'">rw</xsl:when>
                <xsl:otherwise><xsl:value-of select="$isoCode"/></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <ms:languageTag>
            <xsl:value-of select="$modifiedIsoCode"/>
        </ms:languageTag>
        <ms:languageId>
            <xsl:value-of select="$modifiedIsoCode"/>
        </ms:languageId>
    </xsl:template>

    <xsl:template name="uncoded_languages">
        <!-- This is expected in ELG:
          Languages without ISO 639-3 but with a glottolog code: use “mis” for ISO and add the glottolog code
          Languages with neither ISO 639-3 nor a glottolog code: use “und” for ISO and add the free text name at “languageVarietyName”

          At the moment we don't have/know glottolog codes, so behave as 'und' (Undetermined) even if the iso is 'mis'
          (Uncoded languages).
        -->
        <xsl:choose>
            <!-- Assume that if we have 'und' there are language names in dc.language -->
            <xsl:when test="/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:field[@name='value']">
                <xsl:for-each
                        select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element/doc:field[@name='value']">
                    <ms:language>
                        <xsl:call-template name="ms_language_inside">
                            <xsl:with-param name="isoCode" select="'und'"/>
                        </xsl:call-template>
                        <ms:languageVarietyName xml:lang="en"><xsl:value-of select="."/></ms:languageVarietyName>
                    </ms:language>
                </xsl:for-each>
            </xsl:when>
            <!-- if not just produce the und/und tag/id; though, elg will complain -->
            <xsl:otherwise>
                <ms:language>
                    <xsl:call-template name="ms_language_inside">
                        <xsl:with-param name="isoCode" select="'und'"/>
                    </xsl:call-template>
                </ms:language>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="Distribution">
        <xsl:param name="distributionType" select="'Dataset'"/>
        <xsl:param name="mediaFeature" select="$upperMediaType"/>
        <xsl:variable name="form">
            <xsl:choose>
                <xsl:when test="$distributionType = 'Dataset'">downloadable</xsl:when>
                <xsl:otherwise>sourceCode</xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:element name="ms:{$distributionType}Distribution">
            <xsl:element name="ms:{$distributionType}DistributionForm">
                <xsl:value-of select="concat('http://w3id.org/meta-share/meta-share/', $form)"/>
            </xsl:element>

            <xsl:if test="$lr.elg.download-location.exposed">
                <!-- downloadLocation if there are files -->
                <xsl:if test="exsl:node-set($files)/doc:element[@name='bitstream']">
                    <xsl:choose>
                        <!-- one file -> direct link -->
                        <xsl:when test="count(exsl:node-set($files)/doc:element[@name='bitstream']) = 1">
                            <ms:downloadLocation><xsl:value-of
                                    select="exsl:node-set($files)[1]/doc:element[@name='bitstream']/doc:field[@name='url']/text()" /></ms:downloadLocation>
                        </xsl:when>
                        <!-- multiple files within allzip limit -->
                        <xsl:when
                                test="sum(exsl:node-set($files)/doc:element[@name='bitstream']/doc:field[@name='size']/text()) &lt; $lr.download.all.limit.max.file.size ">
                            <ms:downloadLocation><xsl:value-of
                                    select="concat(tokenize(exsl:node-set($files)[1]/doc:element[@name='bitstream']/doc:field[@name='url']/text(), 'bitstream/')[1], $handle, '/allzip')" /></ms:downloadLocation>
                        </xsl:when>
                    </xsl:choose>
                </xsl:if>
            </xsl:if>
            <ms:accessLocation><xsl:value-of select="$identifier_uri"/></ms:accessLocation>
            <xsl:if test="doc:metadata/doc:element[@name='local']/doc:element[@name='demo']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
                <xsl:variable name="locType">
                    <xsl:choose>
                        <xsl:when test="$type = 'toolService'">demo</xsl:when>
                        <xsl:otherwise>samples</xsl:otherwise>
                    </xsl:choose>
                </xsl:variable>
                <xsl:element name="ms:{$locType}Location">
                    <xsl:value-of select="doc:metadata/doc:element[@name='local']/doc:element[@name='demo']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/>
                </xsl:element>
            </xsl:if>

            <!-- distributionXfeature -->
            <xsl:if test="$distributionType = 'Dataset'">
                <xsl:element name="ms:distribution{$mediaFeature}Feature">
                    <xsl:call-template name="Sizes"/>
                    <xsl:call-template name="dataFormat"/>
                </xsl:element>
            </xsl:if>


            <ms:licenceTerms>
                <ms:licenceTermsName xml:lang="en"><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element/doc:field[@name='value']" /></ms:licenceTermsName>
                <ms:licenceTermsURL><xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name='value']" /></ms:licenceTermsURL>
                <ms:conditionOfUse>http://w3id.org/meta-share/meta-share/unspecified</ms:conditionOfUse>
            </ms:licenceTerms>
        </xsl:element>
    </xsl:template>

    <xsl:template name="toolService">
        <ms:ToolService>
            <ms:lrType>ToolService</ms:lrType>
            <ms:function>
                <ms:LTClassOther>undefined</ms:LTClassOther>
            </ms:function>
            <xsl:call-template name="Distribution">
                <xsl:with-param name="distributionType" select="'Software'"/>
            </xsl:call-template>
            <xsl:variable name="languageDependent">
                <xsl:choose>
                    <xsl:when test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceComponentType#ToolServiceInfo']/doc:element[@name='languageDependent']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceComponentType#ToolServiceInfo']/doc:element[@name='languageDependent']/doc:element/doc:field[@name='value']"/>
                    </xsl:when>
                    <!-- XXX taking a default of non language dependent -->
                    <xsl:otherwise>false</xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <ms:languageDependent>
                <xsl:value-of select="$languageDependent"/>
            </ms:languageDependent>
            <ms:inputContentResource>
                <ms:processingResourceType>http://w3id.org/meta-share/meta-share/unspecified</ms:processingResourceType>
                <xsl:if test="$languageDependent='true'">
                    <xsl:call-template name="Languages"/>
                </xsl:if>
            </ms:inputContentResource>
            <xsl:if test="$languageDependent='true'">
                <ms:outputResource>
                    <ms:processingResourceType>http://w3id.org/meta-share/meta-share/unspecified</ms:processingResourceType>
                    <xsl:call-template name="Languages"/>
                </ms:outputResource>
            </xsl:if>
            <!--
             The element can be used for adding evaluation/quality-related information,
             e.g. BLEU scores for machine translation tools,
             or links to evaluation reports if it has been formally evaluated by someone.
             -->
            <ms:evaluated>false</ms:evaluated>
        </ms:ToolService>
    </xsl:template>

    <xsl:template name="languageDescription">
        <xsl:variable name="isModel" select="contains($detailedType, 'model')"/>
        <ms:LanguageDescription>
            <ms:lrType>LanguageDescription</ms:lrType>
            <ms:ldSubclass>
                <xsl:choose>
                    <xsl:when test="$detailedType='grammar'">http://w3id.org/meta-share/meta-share/grammar</xsl:when>
                    <xsl:when test="$isModel">http://w3id.org/meta-share/meta-share/model</xsl:when>
                    <xsl:otherwise>http://w3id.org/meta-share/meta-share/other</xsl:otherwise>
                </xsl:choose>
            </ms:ldSubclass>
            <xsl:if test="$detailedType='grammar' or $isModel">
                <ms:LanguageDescriptionSubclass>
                    <xsl:choose>
                        <xsl:when test="$detailedType='grammar'">
                            <ms:Grammar>
                                <ms:ldSubclassType>Grammar</ms:ldSubclassType>
                                <ms:encodingLevel>http://w3id.org/meta-share/meta-share/unspecified</ms:encodingLevel>
                            </ms:Grammar>
                        </xsl:when>
                        <xsl:when test="$detailedType='mlmodel'">
                            <ms:Model>
                                <xsl:call-template name="languageDescriptionMsModel"/>
                            </ms:Model>
                        </xsl:when>
                        <xsl:when test="$detailedType='ngrammodel'">
                            <ms:Model>
                                <xsl:call-template name="languageDescriptionMsModel"/>
                                <ms:NGramModel>
                                    <ms:baseItem>http://w3id.org/meta-share/meta-share/unspecified</ms:baseItem>
                                    <!-- XXX this is supposed to mean unspecified -->
                                    <ms:order>-1</ms:order>
                                </ms:NGramModel>
                            </ms:Model>
                        </xsl:when>
                    </xsl:choose>
                </ms:LanguageDescriptionSubclass>
            </xsl:if>
            <xsl:call-template name="CommonMediaPart">
                <xsl:with-param name="noMediaPart" select="boolean($isModel)"/>
            </xsl:call-template>
            <xsl:call-template name="Distribution">
                <xsl:with-param name="mediaFeature">
                    <xsl:choose>
                        <xsl:when test="$isModel">Unspecified</xsl:when>
                        <xsl:otherwise><xsl:value-of select="$upperMediaType"/></xsl:otherwise>
                    </xsl:choose>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="personalSensitiveAnon"/>
        </ms:LanguageDescription>
    </xsl:template>

    <xsl:template name="lexicalConceptualResource">
        <ms:LexicalConceptualResource>
            <ms:lrType>LexicalConceptualResource</ms:lrType>
            <ms:lcrSubclass>
                <xsl:choose>
                    <xsl:when test="$detailedType = 'wordnet'">http://w3id.org/meta-share/meta-share/wordNet</xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="concat('http://w3id.org/meta-share/meta-share/', $detailedType)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </ms:lcrSubclass>
            <ms:encodingLevel>http://w3id.org/meta-share/meta-share/unspecified</ms:encodingLevel>
            <xsl:call-template name="CommonMediaPart"/>
            <xsl:call-template name="Distribution"/>
            <xsl:call-template name="personalSensitiveAnon"/>
        </ms:LexicalConceptualResource>
    </xsl:template>

    <xsl:template name="formatDate">
        <xsl:param name="date"/>
        <xsl:value-of select="tokenize($date, 'T')[1]"/>
    </xsl:template>

    <xsl:template name="Sizes">
        <xsl:choose>
            <xsl:when
                    test="doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value']">
                <xsl:for-each
                        select="doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value']">
                    <xsl:variable name="size_arr" select="tokenize(., ';')"/>
                    <xsl:call-template name="size">
                        <xsl:with-param name="amount" select="$size_arr[1]"/>
                        <xsl:with-param name="unit" select="$size_arr[2]"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="size">
                    <xsl:with-param name="amount" select="format-number(sum(exsl:node-set($files)/doc:element[@name='bitstream']/doc:field[@name='size']/text()), '0')"/>
                    <xsl:with-param name="unit" select="'bytes'"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="size">
        <xsl:param name="amount"/>
        <xsl:param name="unit"/>

        <xsl:variable name="unit_lc" select="translate($unit, $UPPER_CHARS, $LOWER_CHARS)"/>
        <ms:size>
            <ms:amount><xsl:value-of select="$amount"/></ms:amount>
            <!-- sizeUnit -->
            <!-- adapted from https://gitlab.com/european-language-grid/platform/ELG-SHARE-schema/-/blob/master/Support%20tools/META-SHARE_3.1_into_ELG/elg-conversion-tools-master/rules/elra-to-elg-body.xsl -->
            <!-- DO NOT CHANGER ORDER DECLARATION -->
            <xsl:choose>
                <xsl:when test="$unit_lc = '4-grams'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/four-gram</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = '5-grams'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/five-gram</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 't-hpairs'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/T-HPair</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'articles'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/article</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'bigrams'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/bigram</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'bytes'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/byte</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'classes'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/class</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'concepts'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/concept</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'diphones'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/diphone1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'elements'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/element</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'entries'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/entry</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'expressions'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/expression</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'files'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/file</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'frames'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/frame1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'gb'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/gb</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'hours'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/hour1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'idiomaticExpressions'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/idiomaticExpression</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'images'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/image2</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'items'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/item</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'kb'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/kb</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'keywords'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/keyword1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'lexicalTypes'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/lexicalType</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'mb'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/mb</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'minutes'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/minute</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'multiWordUnits'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/multiWordUnit</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'neologisms'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/neologism</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'other'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/other</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'phonemes'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/phoneme2</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'phoneticUnits'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/phoneticUnit</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'predicates'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/predicate</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'rules'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/rule</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'seconds'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/second</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'semanticUnits'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/semanticUnit1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'sentences'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/sentence1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'shots'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/shot1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'syllables'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/syllable2</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'synsets'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/synset</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'syntacticUnits'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/syntacticUnit1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'terms'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/term</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'texts'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/text1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'tokens'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/token</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'trigrams'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/trigram</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'turns'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/turn</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'unigrams'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/unigram</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'units'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/unit</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'utterances'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/utterance1</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:when test="$unit_lc = 'words'">
                    <ms:sizeUnit>
                        <ms:sizeUnitRecommended>http://w3id.org/meta-share/meta-share/word3</ms:sizeUnitRecommended>
                    </ms:sizeUnit>
                </xsl:when>
                <xsl:otherwise>
                    <ms:sizeUnit>
                        <ms:sizeUnitOther>
                            <xsl:value-of select="$unit_lc"/>
                        </ms:sizeUnitOther>
                    </ms:sizeUnit>
                </xsl:otherwise>
            </xsl:choose>
        </ms:size>
    </xsl:template>

    <xsl:template name="dataFormat">
        <ms:dataFormat>
            <ms:dataFormatRecommended>
                <xsl:choose>
                    <xsl:when test="false()"></xsl:when>
                    <xsl:otherwise><xsl:value-of select="'http://w3id.org/meta-share/omtd-share/BinaryFormat'"/></xsl:otherwise>
                </xsl:choose>
            </ms:dataFormatRecommended>
        </ms:dataFormat>
    </xsl:template>

    <xsl:template name="languageDescriptionMsModel">
        <ms:ldSubclassType>Model</ms:ldSubclassType>
        <ms:modelType>
            <ms:modelTypeRecommended>http://w3id.org/meta-share/meta-share/unspecified</ms:modelTypeRecommended>
        </ms:modelType>
        <ms:modelFunction>
            <ms:modelFunctionRecommended>http://w3id.org/meta-share/meta-share/unspecified</ms:modelFunctionRecommended>
        </ms:modelFunction>
    </xsl:template>

    <xsl:template name="Languages">
        <xsl:for-each
                select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']">
            <xsl:call-template name="Language">
                <xsl:with-param name="isoCode" select="fn:shortestIdFn(.)"/>
            </xsl:call-template>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>