<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:frida="http://www.cristin.no/techdoc/xsd/resultater/1.0/resultater.xsd"
                xmlns:brage="http://brage.unit.no"
                xmlns:exslt="http://exslt.org/common"
                exclude-result-prefixes="frida brage">

    <xsl:output omit-xml-declaration="yes" method="xml" indent="no"/>

    <xsl:variable name="brage-institution" select="/brage:brage/brage:institution"/>
    <xsl:variable name="cristin-institution-ids" select="exslt:node-set($brage-institution)/cristin/cristinID"/>

    <xsl:template name="first-person">
        <xsl:param name="field-nodeset"/>
        <xsl:for-each select="$field-nodeset">
            <dim:field mdschema="cristin" element="unitcode">
                <xsl:value-of select="institusjonsnr"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="avdnr"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="undavdnr"/>
                <xsl:text>,</xsl:text>
                <xsl:value-of select="gruppenr"/>
            </dim:field>
            <dim:field mdschema="cristin" element="unitname">
                <xsl:value-of select="navn"/>
            </dim:field>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="document-type">
        <xsl:param name="hovedkategori"/>
        <xsl:param name="underkategori"/>
        <dim:field mdschema="dc" element="type">
            <xsl:choose>
                <xsl:when test="$hovedkategori = 'BOK'">Book</xsl:when>
                <xsl:when test="$hovedkategori = 'FOREDRAG'">Lecture</xsl:when>
                <xsl:when test="$hovedkategori = 'BOKRAPPORTDEL' and $underkategori = 'KAPITTEL'">Chapter</xsl:when>
                <xsl:when test="$hovedkategori = 'RAPPORT' and $underkategori = 'DRGRADAVH'">Doctoral thesis</xsl:when>
                <xsl:when test="$hovedkategori = 'RAPPORT' and $underkategori = 'RAPPORT'">Research report</xsl:when>
                <xsl:when test="$hovedkategori = 'TIDSSKRIFTPUBL' and $underkategori = 'ARTIKKEL'">Journal article</xsl:when>
                <xsl:when test="$hovedkategori = 'TIDSSKRIFTPUBL' and $underkategori = 'OVERSIKTSART'">Journal article</xsl:when>
                <xsl:when test="$hovedkategori = 'TIDSSKRIFTPUBL' and $underkategori = 'ARTIKKEL_POP'">Journal article</xsl:when>
                <xsl:otherwise>Others</xsl:otherwise>
            </xsl:choose>
        </dim:field>
    </xsl:template>

    <xsl:template name="iso-language">
        <xsl:param name="kode"/>
        <dim:field mdschema="dc" element="language" qualifier="iso">
            <xsl:choose>
                <xsl:when test="$kode = 'DA'">dan</xsl:when>
                <xsl:when test="$kode = 'EN'">eng</xsl:when>
                <xsl:when test="$kode = 'FI'">fin</xsl:when>
                <xsl:when test="$kode = 'FR'">fre</xsl:when>
                <xsl:when test="$kode = 'DE'">ger</xsl:when>
                <xsl:when test="$kode = 'IS'">ice</xsl:when>
                <xsl:when test="$kode = 'IT'">ita</xsl:when>
                <xsl:when test="$kode = 'NL'">dut</xsl:when>
                <xsl:when test="$kode = 'NB'">nob</xsl:when>
                <xsl:when test="$kode = 'NN'">nno</xsl:when>
                <xsl:when test="$kode = 'PT'">por</xsl:when>
                <xsl:when test="$kode = 'RU'">rus</xsl:when>
                <xsl:when test="$kode = 'SE'">smi</xsl:when>
                <xsl:when test="$kode = 'ES'">spa</xsl:when>
                <xsl:when test="$kode = 'SV'">swe</xsl:when>
                <xsl:otherwise>mis</xsl:otherwise>
            </xsl:choose>
        </dim:field>
    </xsl:template>

    <xsl:template name="peer-reviewed">
        <xsl:param name="hovedkategori"/>
        <xsl:param name="underkategori"/>
        <xsl:param name="qualitycode"/>
        <xsl:value-of select="string(contains('|1|1A|2|2A|', concat('|', $qualitycode, '|')) and $hovedkategori = 'TIDSSKRIFTPUBL' and ($underkategori = 'ARTIKKEL' or $underkategori = 'OVERSIKTSART'))"/>
    </xsl:template>


    <xsl:template name="subject">
        <xsl:param name="field-node"/>
        <xsl:for-each select="$field-node">
            <xsl:if test="navn">
                <dim:field mdschema="dc" element="subject">
                    <xsl:value-of select="navn"/>
                </dim:field>
            </xsl:if>
            <xsl:if test="navnEngelsk">
                <dim:field mdschema="dc" element="subject">
                    <xsl:value-of select="navnEngelsk"/>
                </dim:field>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>

    <xsl:template name="nsi">
        <xsl:param name="field-node"/>
        <xsl:if test="$field-node/navn">
            <dim:field mdschema="dc" element="subject" qualifier="nsi">
                <xsl:text>VDP::</xsl:text>
                <xsl:value-of select="$field-node/navn"/>
                <xsl:text>: </xsl:text>
                <xsl:value-of select="$field-node/kode"/>
            </dim:field>
        </xsl:if>
        <xsl:if test="$field-node/navnEngelsk">
            <dim:field mdschema="dc" element="subject" qualifier="nsi">
                <xsl:text>VDP::</xsl:text>
                <xsl:value-of select="$field-node/navnEngelsk"/>
                <xsl:text>: </xsl:text>
                <xsl:value-of select="$field-node/kode"/>
            </dim:field>
        </xsl:if>
    </xsl:template>

    <xsl:template name="project-identifier">
        <xsl:param name="project"/>
        <xsl:param name="id"/>
        <dim:field mdschema="dc" element="relation" qualifier="project">
            <xsl:choose>
                <xsl:when test="$project='EU' or starts-with($project, 'EC/FP') or starts-with($project, 'EC/H2020')">
                    <xsl:value-of select="concat($project, '/', $id)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="concat($project, ': ', $id)"/>
                </xsl:otherwise>
            </xsl:choose>
        </dim:field>
    </xsl:template>

    <xsl:template name="publication-version">
        <xsl:param name="fulltextType"/>
        <xsl:if test="$fulltextType='original'">
            <dim:field mdschema="dc" element="description" qualifier="version">publishedVersion</dim:field>
        </xsl:if>
        <xsl:if test="$fulltextType='preprint'">
            <dim:field mdschema="dc" element="description" qualifier="version">submittedVersion</dim:field>
        </xsl:if>
        <xsl:if test="$fulltextType='postprint'">
            <dim:field mdschema="dc" element="description" qualifier="version">acceptedVersion</dim:field>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/">
        <metadata xmlns:dim="http://www.dspace.org/xmlns/dspace/dim">


            <!-- cristin.unitcode -->
            <!-- (/brage:brage/brage:frida/forskningsresultat/fellesdata/person/tilhorighet/sted/) institusjonsnr,avdnr,undavdnr,gruppenr -->
            <!-- "Must be fetched only for the first author (person) -->

            <!-- cristin.unitname -->
            <!-- (/brage:brage/brage:frida/forskningsresultat/fellesdata/person/tilhorighet/sted/) navn	-->
            <!-- Must be fetched only for the authors belonging to the current institution (current top community) (person) -->
            <xsl:call-template name="first-person">
                <xsl:with-param name="field-nodeset" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/person/tilhorighet/sted[institusjonsnr=$cristin-institution-ids]" />
            </xsl:call-template>

            <!-- cristin.ispublished	(/brage:brage/brage:frida/forskningsresultat/fellesdata/) erPublisert -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/erPublisert">
                <dim:field mdschema="cristin" element="ispublished">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/fellesdata/erPublisert"/>
                </dim:field>
            </xsl:if>

            <!-- cristin.fulltext	(/brage:brage/brage:frida/forskningsresultat/fellesdata/fulltekst/) type -->
            <!-- dc.description.version	(/brage:brage/brage:frida/forskningsresultat/fellesdata/fulltekst/) type -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/fulltekst/type">
                <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata/fulltekst/type">
                    <dim:field mdschema="cristin" element="fulltext">
                        <xsl:value-of select="."/>
                    </dim:field>
                    <xsl:call-template name="publication-version">
                        <xsl:with-param name="fulltextType" select="."/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>

            <!-- dc.relation.project      "(/brage:brage/brage:frida/forskningsresultat/fellesdata/eksternprosjekt) finansieringskilde/navn/id (EC-funded)"-->
            <!-- dc.relation.project      "(/brage:brage/brage:frida/forskningsresultat/fellesdata/eksternprosjekt) finansieringskilde/navn: id" (others)-->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/eksternprosjekt">
                <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata/eksternprosjekt">
                    <xsl:call-template name="project-identifier">
                        <xsl:with-param name="project" select="finansieringskilde/navn"/>
                        <xsl:with-param name="id" select="id"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>

            <!-- cristin.qualitycode (/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/kvalitetsniva/) kode -->
            <!-- dc.type = Peer reviewed if cristin.qualitycode > 0 and TIDSSKRIFTPUBL and (ARTIKKEL or OVERSIKTSART) -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/kvalitetsniva/kode">
                <xsl:variable name="hovedkategori" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/kategori/hovedkategori/kode"/>
                <xsl:variable name="underkategori" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/kategori/underkategori/kode"/>
                <xsl:variable name="qualitycode" select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/kvalitetsniva/kode"/>
                <xsl:variable name="PeerReviewed">
                    <xsl:call-template name="peer-reviewed">
                        <xsl:with-param name="hovedkategori" select="$hovedkategori"/>
                        <xsl:with-param name="underkategori" select="$underkategori"/>
                        <xsl:with-param name="qualitycode" select="$qualitycode"/>
                    </xsl:call-template>
                </xsl:variable>

                <dim:field mdschema="cristin" element="qualitycode"><xsl:value-of select="$qualitycode"/></dim:field>

                <xsl:if test="$PeerReviewed = 'true'">
                    <dim:field mdschema="dc" element="type">Peer reviewed</dim:field>
                </xsl:if>
            </xsl:if>

            <!-- cristin.qualitycode (/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/forlag/kvalitetsniva/) kode -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/forlag/kvalitetsniva/kode">
                <dim:field mdschema="cristin" element="qualitycode">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/forlag/kvalitetsniva/kode"/>
                </dim:field>
            </xsl:if>

            <!-- cristin.qualitycode (/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/forlag/kvalitetsniva/) kode -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/forlag/kvalitetsniva/kode">
                <dim:field mdschema="cristin" element="qualitycode">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/forlag/kvalitetsniva/kode"/>
                </dim:field>
            </xsl:if>

            <!-- dc.contributor.author	(/brage:brage/brage:frida/forskningsresultat/fellesdata/person/) etternavn, fornavn -->
            <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata/person">
                <xsl:choose>
                    <xsl:when test="tilhorighet/sted/rolle/kode = 'REDAKTØR'">
                        <dim:field mdschema="dc" element="contributor" qualifier="editor">
                            <xsl:value-of select="concat(etternavn, ', ',  fornavn)"/>
                        </dim:field>
                    </xsl:when>
                    <xsl:otherwise>
                        <dim:field mdschema="dc" element="contributor" qualifier="author">
                            <xsl:value-of select="concat(etternavn, ', ',  fornavn)"/>
                        </dim:field>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>

            <!-- dc.date.created	(/brage:brage/brage:frida/forskningsresultat/fellesdata/registrert/) dato -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/registrert/dato">
                <dim:field mdschema="dc" element="date" qualifier="created">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/fellesdata/registrert/dato"/>
                </dim:field>
            </xsl:if>

            <!-- dc.date.issued	(/brage:brage/brage:frida/forskningsresultat/fellesdata/) ar -->
            <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata">
                <dim:field mdschema="dc" element="date" qualifier="issued">
                    <xsl:value-of select="ar"/>
                </dim:field>
            </xsl:for-each>

            <!-- dc.description.abstract	(/brage:brage/brage:frida/forskningsresultat/fellesdata/sammendrag/) tekst -->
            <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata/sammendrag/tekst">
                <dim:field mdschema="dc" element="description" qualifier="abstract">
                    <xsl:value-of select="."/>
                </dim:field>
            </xsl:for-each>

            <!-- dc.identifier.cristin	(/brage:brage/brage:frida/forskningsresultat/fellesdata/) id -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/id">
                <dim:field mdschema="dc" element="identifier" qualifier="cristin">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/fellesdata/id"/>
                </dim:field>
            </xsl:if>

            <!-- dc.source.journal	(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/) navn -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/navn">
                <dim:field mdschema="dc" element="source" qualifier="journal">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/navn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.source.volume	(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/) volum -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/volum">
                <dim:field mdschema="dc" element="source" qualifier="volume">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/volum"/>
                </dim:field>
            </xsl:if>

            <!-- dc.source.volume	(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/) volum -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/volum">
                <dim:field mdschema="dc" element="source" qualifier="volume">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/volum"/>
                </dim:field>
            </xsl:if>

            <!-- dc.source.issue	(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/) hefte -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/hefte">
                <dim:field mdschema="dc" element="source" qualifier="issue">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/hefte"/>
                </dim:field>
            </xsl:if>

            <!-- dc.source.issue	(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/) hefte -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/hefte">
                <dim:field mdschema="dc" element="source" qualifier="issue">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/hefte"/>
                </dim:field>
            </xsl:if>

            <!-- dc.source.pagenumber
            (/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/) sideFra
            -
            (/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/) sideTil
            -->

            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/sideFra
             or /brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/sideTil">
                <dim:field mdschema="dc" element="source" qualifier="pagenumber">
                    <xsl:choose>
                        <xsl:when test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/sideFra">
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/sideFra"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>?</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>

                    <xsl:value-of select="'-'"/>

                    <xsl:choose>
                        <xsl:when test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/sideTil">
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/sideTil"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>?</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </dim:field>
            </xsl:if>

            <!-- dc.source.pagenumber
            (/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/) sideFra
            -
            (/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/) sideTil
            -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/sideFra
             or /brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/sideTil">
                <dim:field mdschema="dc" element="source" qualifier="pagenumber">
                    <xsl:choose>
                        <xsl:when test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/sideFra">
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/sideFra"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>?</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>

                    <xsl:value-of select="'-'"/>

                    <xsl:choose>
                        <xsl:when test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/sideTil">
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/sideangivelse/sideTil"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:text>?</xsl:text>
                        </xsl:otherwise>
                    </xsl:choose>
                </dim:field>
            </xsl:if>

            <!-- dc.source.pagenumber
            (/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/) antallSider
            (/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/) antallSider
            (/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/) antallSider
            -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/antallSider">
                <dim:field mdschema="dc" element="source" qualifier="pagenumber">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse/antallSider"/>
                </dim:field>
            </xsl:if>
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/antallSider">
                <dim:field mdschema="dc" element="source" qualifier="pagenumber">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/antallSider"/>
                </dim:field>
            </xsl:if>
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/antallSider">
                <dim:field mdschema="dc" element="source" qualifier="pagenumber">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/antallSider"/>
                </dim:field>
            </xsl:if>

            <!-- dc.identifier.isbn	(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/) isbn -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/isbn">
                <dim:field mdschema="dc" element="identifier" qualifier="isbn">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/isbn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.identifier.isbn	(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/) isbn -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/isbn">
                <dim:field mdschema="dc" element="identifier" qualifier="isbn">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/isbn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.identifier.issn	(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/) issn -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/issn">
                <dim:field mdschema="dc" element="identifier" qualifier="issn">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/tidsskrift/issn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.identifier.doi	(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/) doi -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/doi">
                <dim:field mdschema="dc" element="identifier" qualifier="doi">
                    <xsl:choose>
                        <xsl:when test='starts-with(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/doi, "doi:")
            					or starts-with(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/doi, "DOI:")'>
                            <xsl:value-of
                                    select="substring(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/doi, 5)"/>
                        </xsl:when>
                        <xsl:when
                                test='starts-with(/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/doi, "10")'>
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/doi"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/doi"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </dim:field>
            </xsl:if>

            <!-- dc.language	(/brage:brage/brage:frida/forskningsresultat/fellesdata/sprak/) kode -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/sprak/kode">
                <xsl:call-template name="iso-language">
                    <xsl:with-param name="kode" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/sprak/kode"/>
                </xsl:call-template>
            </xsl:if>

            <!-- berget fra DUO -->
            <xsl:if test="/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/forlag/navn">
                <dim:field mdschema="dc" element="publisher">
                    <xsl:value-of select="/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/forlag/navn"/>
                </dim:field>
            </xsl:if>
            <xsl:if test="/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/utgiver/navn">
                <dim:field mdschema="dc" element="publisher">
                    <xsl:value-of select="/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/utgiver/navn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.publisher	"(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/forlag/) navn" -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/forlag/navn">
                <dim:field mdschema="dc" element="publisher">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/forlag/navn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.publisher	"(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/utgiver/) navn" -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/utgiver/navn">
                <dim:field mdschema="dc" element="publisher">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/utgiver/navn"/>
                </dim:field>
            </xsl:if>


            <!-- dc.relation.ispartof	(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/tittel) tittel -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/tittel">
                <dim:field mdschema="dc" element="relation" qualifier="ispartof">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/tittel"/>
                </dim:field>
            </xsl:if>

            <!-- berget fra DUO -->
            <!-- dc.relation.ispartof	(/frida/forskningsresultat/kategoridata/bokRapport/serie/) (/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/serie/) navn -->
            <!-- dc.relation.ispartofseries	(/frida/forskningsresultat/kategoridata/bokRapport/serie/) (/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/serie/) navn -->
            <xsl:if test="/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/serie/navn">
                <dim:field mdschema="dc" element="relation" qualifier="ispartof">
                    <xsl:value-of select="/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/serie/navn"/>
                </dim:field>
                <dim:field mdschema="dc" element="relation" qualifier="ispartofseries">
                    <xsl:value-of select="/frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/kategoridata/bokRapport/serie/navn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.relation.ispartof	(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/serie/) navn -->
            <!-- dc.relation.ispartofseries	(/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/serie/) navn -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/serie/navn">
                <dim:field mdschema="dc" element="relation" qualifier="ispartof">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/serie/navn"/>
                </dim:field>
                <dim:field mdschema="dc" element="relation" qualifier="ispartofseries">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapport/serie/navn"/>
                </dim:field>
            </xsl:if>

            <!-- dc.subject.nsi	(/brage:brage/brage:frida/forskningsresultat/fellesdata/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/) vitenskapsdisiplin -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/vitenskapsdisiplin">
                <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/vitenskapsdisiplin">
                    <xsl:call-template name="nsi">
                        <xsl:with-param name="field-node" select="/brage:brage/brage:frida/forskningsresultat/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/vitenskapsdisiplin"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>

            <!-- dc.subject.nsi	(/brage:brage/brage:frida/forskningsresultat/fellesdata/) vitenskapsdisiplin -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/vitenskapsdisiplin">
                <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata/vitenskapsdisiplin">
                    <xsl:call-template name="nsi">
                        <xsl:with-param name="field-node" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/vitenskapsdisiplin"/>
                    </xsl:call-template>
                </xsl:for-each>
            </xsl:if>

            <!-- dc.title.alternative	(/brage:brage/brage:frida/forskningsresultat/fellesdata/alternativTittel) tittel -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/alternativTittel/tittel">
                <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata/alternativTittel/tittel">
                    <dim:field mdschema="dc" element="title" qualifier="alternative">
                        <xsl:value-of select="."/>
                    </dim:field>
                </xsl:for-each>
            </xsl:if>

            <!-- dc.title	(/brage:brage/brage:frida/forskningsresultat/fellesdata/) tittel -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/tittel">
                <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/fellesdata/tittel">
                    <dim:field mdschema="dc" element="title">
                        <xsl:value-of select="."/>
                    </dim:field>
                </xsl:for-each>
            </xsl:if>


            <!-- dc.type
            (/brage:brage/brage:frida/forskningsresultat/fellesdata/kategori/hovedkategori) kode
            (/brage:brage/brage:frida/forskningsresultat/fellesdata/kategori/underkategori) kode
             -->
            <xsl:call-template name="document-type">
                <xsl:with-param name="hovedkategori" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/kategori/hovedkategori/kode"/>
                <xsl:with-param name="underkategori" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/kategori/underkategori/kode"/>
            </xsl:call-template>

            <!-- dc.subject (/brage:brage/brage:frida/forskningsresultat/fellesdata/) emneord -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/emneord">
                <xsl:call-template name="subject">
                    <xsl:with-param name="field-node" select="/brage:brage/brage:frida/forskningsresultat/fellesdata/emneord"/>
                </xsl:call-template>
            </xsl:if>

            <!-- dc.subject (/brage:brage/brage:frida/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/) emneord -->
            <xsl:if test="/brage:brage/brage:frida/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/emneord">
                <xsl:call-template name="subject">
                    <xsl:with-param name="field-node" select="/brage:brage/brage:frida/kategoridata/bokRapportDel/delAv/forskningsresultat/fellesdata/emneord"/>
                </xsl:call-template>
            </xsl:if>

            <!-- dc.subject (/brage:brage/brage:frida/kategoridata/bokRapport/deler/) forskningsresultat[*]/emneord -->
            <xsl:if test="/brage:brage/brage:frida/kategoridata/bokRapport/deler">
                <xsl:for-each select="/brage:brage/brage:frida/kategoridata/bokRapport/deler">
                    <xsl:if test="forskningsresultat/emneord">
                        <xsl:call-template name="subject">
                            <xsl:with-param name="field-node" select="forskningsresultat/emneord"/>
                        </xsl:call-template>
                    </xsl:if>
                </xsl:for-each>
            </xsl:if>




            <!-- Citation (explicitly built) -->
            <!-- format of the citation is: <title of journal>. <year>, <vol> (<nr>), <startpage>:<endpage>. -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/sideangivelse">
                <xsl:for-each select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel">

                    <dim:field mdschema="dc" element="identifier" qualifier="citation">

                        <xsl:value-of select="tidsskrift/navn"/>
                        <xsl:text>. </xsl:text>
                        <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/ar">
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/fellesdata/ar"/>
                            <xsl:text>, </xsl:text>
                        </xsl:if>

                        <xsl:value-of select="volum"/>
                        <xsl:value-of select="' '"/>
                        <!--
                        <xsl:value-of select="string('(')" />
                        <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/fellesdata/ar" />
                        <xsl:value-of select="string(')')" />
                        -->
                        <xsl:if test="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/hefte">
                            <xsl:value-of select="'('"/>
                            <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/kategoridata/tidsskriftsartikkel/hefte"/>
                            <xsl:value-of select="')'"/>
                            <xsl:text>, </xsl:text>
                        </xsl:if>

                        <xsl:if test="sideangivelse/sideFra or sideangivelse/sideTil">
                            <xsl:choose>
                                <xsl:when test="sideangivelse/sideFra">
                                    <xsl:value-of select="sideangivelse/sideFra"/>
                                </xsl:when>
                                <xsl:otherwise><xsl:text>?</xsl:text></xsl:otherwise>
                            </xsl:choose>

                            <xsl:value-of select="'-'"/>

                            <xsl:choose>
                                <xsl:when test="sideangivelse/sideTil">
                                    <xsl:value-of select="sideangivelse/sideTil"/>
                                </xsl:when>
                                <xsl:otherwise><xsl:text>?</xsl:text></xsl:otherwise>
                            </xsl:choose>
                        </xsl:if>
                        <xsl:text>.</xsl:text>
                    </dim:field>
                </xsl:for-each>
            </xsl:if>

            <!-- dc.relation.uri (/brage:brage/brage:frida/forskningsresultat/fellesdata/ressurs/) url -->
            <xsl:if test="/brage:brage/brage:frida/forskningsresultat/fellesdata/ressurs/type/kode = 'FULLTEKST'">
                <dim:field mdschema="dc" element="relation" qualifier="uri">
                    <xsl:value-of select="/brage:brage/brage:frida/forskningsresultat/fellesdata/ressurs/url"/>
                </dim:field>
            </xsl:if>
        </metadata>
    </xsl:template>
</xsl:stylesheet>