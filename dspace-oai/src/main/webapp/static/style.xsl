<?xml version="1.0" encoding="utf-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:oai="http://www.openarchives.org/OAI/2.0/"
	xmlns:lyn="http://www.lyncode.com/fakeNamespace" xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
	xmlns:dc="http://purl.org/dc/doc:elements/1.1/"
    xmlns:verb="http://informatik.hu-berlin.de/xmlverbatim"
    xmlns:oai_id="http://www.openarchives.org/OAI/2.0/oai-identifier"
    exclude-result-prefixes="oai lyn oai_dc dc verb oai_id">

	<xsl:output method="html" doctype-public="-//W3C//DTD HTML 4.01//EN" doctype-system="http://www.w3.org/TR/html4/strict.dtd" />

	<xsl:template match="/">
		<html>
			<head>
                <title>DSpace OAI-PMH Data Provider</title>
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                <script src="static/js/jquery.js" type="text/javascript"></script>
                <script src="static/js/bootstrap.min.js" type="text/javascript"></script>

                <link rel="stylesheet" href="static/css/bootstrap.min.css" type="text/css" />
                <link rel="stylesheet" href="static/css/bootstrap-theme.min.css" type="text/css" />
                <link rel="stylesheet" href="static/css/style.css" type="text/css" />
			</head>
			<body>
                <div class="container">
                    <div class="navbar navbar-default" role="navigation">
                        <div class="navbar-header">
                            <a class="navbar-brand" href="#">DSpace OAI-PMH Data Provider</a>
                        </div>
                        <div class="navbar-collapse collapse">
                            <ul class="nav navbar-nav navbar-right">
                                <li>
                                    <a title="Institutional information">
                                        <xsl:if test="/oai:OAI-PMH/oai:request/@verb = 'Identify'">
                                            <xsl:attribute name="class">active</xsl:attribute>
                                        </xsl:if>
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                                    select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=Identify')"></xsl:value-of>
                                        </xsl:attribute>
                                        Identify
                                    </a>
                                </li>
                                <li>
                                    <a title="Listing available sets">
                                        <xsl:if test="/oai:OAI-PMH/oai:request/@verb = 'ListSets'">
                                            <xsl:attribute name="class">active</xsl:attribute>
                                        </xsl:if>
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                                    select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListSets')"></xsl:value-of>
                                        </xsl:attribute>
                                        Sets
                                    </a>
                                </li>
                                <li>
                                    <a title="Listing records (with metadata)">
                                        <xsl:if test="/oai:OAI-PMH/oai:request/@verb = 'ListRecords'">
                                            <xsl:attribute name="class">active</xsl:attribute>
                                        </xsl:if>
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                                    select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc')"></xsl:value-of>
                                        </xsl:attribute>
                                        Records
                                    </a>
                                </li>
                                <li>
                                    <a title="Listing identifiers only">
                                        <xsl:if test="/oai:OAI-PMH/oai:request/@verb = 'ListIdentifiers'">
                                            <xsl:attribute name="class">active</xsl:attribute>
                                        </xsl:if>
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                                    select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;metadataPrefix=oai_dc')"></xsl:value-of>
                                        </xsl:attribute>
                                        Identifiers
                                    </a>
                                </li>
                                <li>
                                    <a title="Metadata Formats available">
                                        <xsl:if
                                                test="/oai:OAI-PMH/oai:request/@verb = 'ListMetadataFormats'">
                                            <xsl:attribute name="class">active</xsl:attribute>
                                        </xsl:if>
                                        <xsl:attribute name="href">
                                            <xsl:value-of
                                                    select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListMetadataFormats')"></xsl:value-of>
                                        </xsl:attribute>
                                        Metadata Formats
                                    </a>
                                </li>
                            </ul>
                        </div><!--/.nav-collapse -->
                    </div>
                    <div class="row">
                        <div class="col-lg-offset-1 col-lg-10">
                            <div class="row">
                                <h5>Response Date <small><xsl:value-of select="translate(oai:OAI-PMH/oai:responseDate/text(), 'TZ', ' ')" /></small></h5>
                            </div>
                            <div class="row">
                                <xsl:apply-templates select="oai:OAI-PMH/oai:error" />
                                <xsl:apply-templates select="oai:OAI-PMH/oai:Identify" />
                                <xsl:apply-templates select="oai:OAI-PMH/oai:ListSets" />
                                <xsl:apply-templates select="oai:OAI-PMH/oai:ListRecords" />
                                <xsl:apply-templates select="oai:OAI-PMH/oai:ListIdentifiers" />
                                <xsl:apply-templates select="oai:OAI-PMH/oai:ListMetadataFormats" />
                                <xsl:apply-templates select="oai:OAI-PMH/oai:GetRecord" />
                            </div>
                        </div>
                    </div>


                    <div class="row-fluid text-center">
                        <div class="vertical-space"></div>
                        <p><small>Design by Lyncode</small></p>
                        <p>
                            <a href="http://www.lyncode.com">
                                <img style="height: 20px;" src="static/img/lyncode.png" alt="Lyncode" />
                            </a>
                        </p>
                    </div>
                </div>
			</body>
		</html>
	</xsl:template>

    <xsl:template match="oai:OAI-PMH/oai:error">
        <div class="alert alert-danger">
            <h4>Error</h4>
            <p>
                <xsl:value-of select="text()"></xsl:value-of>
            </p>
        </div>
    </xsl:template>

    <xsl:template match="oai:OAI-PMH/oai:Identify">
        <h2>Repository Information</h2>
        <hr />
        <table class="table table-striped table-bordered">
            <tr>
                <td><b>Repository Name</b></td>
                <td><xsl:value-of select="oai:repositoryName/text()" /></td>
            </tr>
            <xsl:for-each select="oai:adminEmail">
                <tr>
                    <td><b>E-Mail Contact</b></td>
                    <td>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of select="concat('mailto:', text())" />
                            </xsl:attribute>
                            <xsl:value-of select="text()" />
                        </a>
                    </td>
                </tr>
            </xsl:for-each>
            <tr>
                <td><b>Repository identifier</b></td>
                <td>
                    <xsl:value-of select="oai:description/oai_id:oai-identifier/oai_id:repositoryIdentifier/text()" />
                </td>
            </tr>
            <tr>
                <td><b>Sample identifier</b></td>
                <td>
                    <xsl:value-of
                            select="oai:description/oai_id:oai-identifier/oai_id:sampleIdentifier/text()" />
                </td>
            </tr>
            <tr>
                <td><b>Protocol Version</b></td>
                <td>
                    <xsl:value-of
                            select="oai:protocolVersion/text()" />
                </td>

            </tr>
            <tr>
                <td><b>Earliest Registered Date</b></td>
                <td>
                    <xsl:value-of
                            select="translate(oai:earliestDatestamp/text(), 'TZ' ,' ')" />
                </td>

            </tr>
            <tr>
                <td><b>Date Granularity</b></td>
                <td>
                    <xsl:value-of
                            select="translate(oai:granularity/text(), 'TZ', ' ')" />
                </td>

            </tr>
            <tr>
                <td><b>Deletion Mode</b></td>
                <td>
                    <xsl:value-of
                            select="oai:deletedRecord/text()" />
                </td>

            </tr>
        </table>
    </xsl:template>

    <xsl:template match="oai:OAI-PMH/oai:ListSets">
        <h2>List of Sets</h2>
        <hr />
        <div class="well well-sm">
            <h4>Results fetched
                <small>
                    <xsl:call-template name="result-count">
                        <xsl:with-param name="path" select="oai:set" />
                    </xsl:call-template>
                </small>
            </h4>
        </div>
        <div class="list-group">
            <xsl:for-each select="oai:set">
                <div class="list-group-item">
                    <h5 class="list-group-item-heading">
                        <xsl:choose>
                            <xsl:when test="string-length(oai:setName/text()) &gt; 83">
                                <xsl:value-of select="substring(oai:setName/text(),0, 80 )" />
                                ...
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="oai:setName/text()" />
                            </xsl:otherwise>
                        </xsl:choose>
                        <small>
                            [<xsl:value-of select="oai:setSpec/text()" />]
                        </small>
                    </h5>
                    <div class="spec">
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of
                                        select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', oai:setSpec/text())" />
                            </xsl:attribute>
                            Records
                        </a>
                        <a>
                            <xsl:attribute name="href">
                                <xsl:value-of
                                        select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;metadataPrefix=oai_dc&amp;set=', oai:setSpec/text())" />
                            </xsl:attribute>
                            Identifiers
                        </a>
                    </div>
                </div>
            </xsl:for-each>
        </div>

        <xsl:apply-templates select="oai:resumptionToken"/>
    </xsl:template>

    <xsl:template match="oai:OAI-PMH/oai:ListRecords">
        <h2>List of Records</h2>
        <hr />
        <div class="well well-sm">
            <h4>Results fetched
                <small>
                    <xsl:call-template name="result-count">
                        <xsl:with-param name="path" select="oai:record" />
                    </xsl:call-template>
                </small>
            </h4>
        </div>
        <xsl:for-each select="oai:record">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <div class="row">
                        <div class="col-lg-6">
                            <h5>Identifier <small><xsl:value-of select="oai:header/oai:identifier/text()"></xsl:value-of></small></h5>
                        </div>
                        <div class="col-lg-6">
                            <h5>Last Modified <small><xsl:value-of select="translate(oai:header/oai:datestamp/text(), 'TZ', ' ')"></xsl:value-of></small></h5>
                        </div>
                    </div>
                </div>
                <div class="panel-body">
                    <!-- If this record has a "status", display it as a warning -->
                    <xsl:if test="oai:header/@status">
                      <div class="alert alert-warning">Record Status: <xsl:value-of select="oai:header/@status"/></div>
                    </xsl:if>
                    <div class="panel panel-success">
                        <a data-toggle="collapse">
                            <xsl:attribute name="href">#sets<xsl:value-of select="translate(oai:header/oai:identifier/text(), ':/.', '')"></xsl:value-of></xsl:attribute>
                            <div class="panel-heading">
                                <h5 class="panel-title">
                                    Sets
                                </h5>
                            </div>
                        </a>
                        <div class="panel-collapse collapse">
                            <xsl:attribute name="id">sets<xsl:value-of select="translate(oai:header/oai:identifier/text(), ':/.', '')"></xsl:value-of></xsl:attribute>
                            <div class="panel-body list-group">
                                <xsl:for-each select="oai:header/oai:setSpec">
                                    <div class="list-group-item">
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                        select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', text())" />
                                            </xsl:attribute>
                                            <xsl:value-of select="text()" />
                                        </a>
                                    </div>
                                </xsl:for-each>
                            </div>
                        </div>
                    </div>
                    <div class="panel panel-info">
                        <a data-toggle="collapse">
                            <xsl:attribute name="href">#<xsl:value-of select="translate(oai:header/oai:identifier/text(), ':/.', '')"></xsl:value-of></xsl:attribute>
                            <div class="panel-heading">
                                <h5 class="panel-title">
                                        Metadata
                                </h5>
                            </div>
                        </a>
                        <div class="panel-collapse collapse">
                            <xsl:attribute name="id"><xsl:value-of select="translate(oai:header/oai:identifier/text(), ':/.', '')"></xsl:value-of></xsl:attribute>
                            <div class="panel-body">
                                <xsl:apply-templates select="oai:metadata/*" />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </xsl:for-each>

        <xsl:apply-templates select="oai:resumptionToken"/>
    </xsl:template>

    <xsl:template match="oai:OAI-PMH/oai:GetRecord">
        <h2>Record Details</h2>
        <hr />
        <xsl:for-each select="oai:record">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <div class="row">
                        <div class="col-lg-6">
                            <h5>Identifier <small><xsl:value-of select="oai:header/oai:identifier/text()"></xsl:value-of></small></h5>
                        </div>
                        <div class="col-lg-6">
                            <h5>Last Modified <small><xsl:value-of select="translate(oai:header/oai:datestamp/text(), 'TZ', ' ')"></xsl:value-of></small></h5>
                        </div>
                    </div>
                </div>
                <div class="panel-body">
                    <!-- If this record has a "status", display it as a warning -->
                    <xsl:if test="oai:header/@status">
                      <div class="alert alert-warning">Record Status: <xsl:value-of select="oai:header/@status"/></div>
                    </xsl:if>
                    <div class="panel panel-success">
                            <div class="panel-heading">
                                <h5 class="panel-title">
                                    Sets
                                </h5>
                            </div>
                            <div class="panel-body list-group">
                                <xsl:for-each select="oai:header/oai:setSpec">
                                    <div class="list-group-item">
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                        select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', text())" />
                                            </xsl:attribute>
                                            <xsl:value-of select="text()" />
                                        </a>
                                    </div>
                                </xsl:for-each>
                            </div>
                    </div>
                    <div class="panel panel-info">
                            <div class="panel-heading">
                                <h5 class="panel-title">
                                    Metadata
                                </h5>
                            </div>
                            <div class="panel-body">
                                <xsl:apply-templates select="oai:metadata/*" />
                            </div>
                    </div>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="oai:OAI-PMH/oai:ListIdentifiers">
        <h2>List of Identifiers</h2>
        <hr />
        <div class="well well-sm">
            <h4>Results fetched
                <small>
                    <xsl:call-template name="result-count">
                        <xsl:with-param name="path" select="oai:header" />
                    </xsl:call-template>
                </small>
            </h4>
        </div>
        <xsl:for-each select="oai:header">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <div class="row">
                        <div class="col-lg-4">
                            <h5>Identifier <small><xsl:value-of select="oai:identifier/text()"></xsl:value-of></small></h5>
                        </div>
                        <div class="col-lg-4">
                            <h5>Last Modified <small><xsl:value-of select="translate(oai:datestamp/text(), 'TZ', ' ')"></xsl:value-of></small></h5>
                        </div>
                        <div class="col-lg-4">
                            <a class="btn btn-default pull-right">
                                <xsl:attribute name="href">
                                    <xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=GetRecord&amp;metadataPrefix=oai_dc&amp;identifier=', oai:identifier/text())" />
                                </xsl:attribute>
                                View Details
                            </a>
                        </div>
                    </div>
                </div>
                <div class="panel-body">
                    <!-- If this record has a "status", display it as a warning -->
                    <xsl:if test="@status">
                      <div class="alert alert-warning">Record Status: <xsl:value-of select="@status"/></div>
                    </xsl:if>
                    <div class="panel panel-success">
                        <a data-toggle="collapse">
                            <xsl:attribute name="href">#sets<xsl:value-of select="translate(oai:identifier/text(), ':/.', '')"></xsl:value-of></xsl:attribute>
                            <div class="panel-heading">
                                <h5 class="panel-title">
                                    Sets
                                </h5>
                            </div>
                        </a>
                        <div class="panel-collapse collapse">
                            <xsl:attribute name="id">sets<xsl:value-of select="translate(oai:identifier/text(), ':/.', '')"></xsl:value-of></xsl:attribute>
                            <div class="panel-body list-group">
                                <xsl:for-each select="oai:setSpec">
                                    <div class="list-group-item">
                                        <a>
                                            <xsl:attribute name="href">
                                                <xsl:value-of
                                                        select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', text())" />
                                            </xsl:attribute>
                                            <xsl:value-of select="text()" />
                                        </a>
                                    </div>
                                </xsl:for-each>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </xsl:for-each>

        <xsl:apply-templates select="oai:resumptionToken"/>
    </xsl:template>

    <xsl:template match="oai:OAI-PMH/oai:ListMetadataFormats">
        <h2>List of Metadata Formats</h2>
        <hr />
        <div class="well well-sm">
            <h4>Results fetched
                <small>
                    <xsl:value-of select="count(oai:OAI-PMH/oai:ListMetadataFormats/oai:metadataFormat)" />
                </small>
            </h4>
        </div>
        <xsl:for-each select="oai:metadataFormat">
            <div class="panel panel-default">
                <div class="panel-heading">
                    <div class="row">
                        <div class="col-lg-9">
                            <h5><xsl:value-of select="oai:metadataPrefix/text()"></xsl:value-of></h5>
                        </div>
                        <div class="col-lg-3">
                            <a class="btn btn-default pull-right">
                                <xsl:attribute name="href">
                                    <xsl:value-of
                                            select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=', oai:metadataPrefix/text())" />
                                </xsl:attribute>
                                List Records
                            </a>
                        </div>
                    </div>
                </div>
                <div class="panel-body">
                    <div class="row">
                        <div class="col-lg-9">
                            <h5>Namespace <small><xsl:value-of select="oai:metadataNamespace/text()"></xsl:value-of></small></h5>
                            <h5>Schema <small><xsl:value-of select="oai:schema/text()"></xsl:value-of></small></h5>
                        </div>
                    </div>
                </div>
            </div>
        </xsl:for-each>
    </xsl:template>

    <xsl:template match="oai:resumptionToken">
        <xsl:if test="text() != ''">
            <div class="text-center">
                <a class="btn btn-primary">
                <xsl:attribute name="href">
                    <xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=',/oai:OAI-PMH/oai:request/@verb,'&amp;resumptionToken=', text())"></xsl:value-of>
                </xsl:attribute>
                    Show More
                </a>
            </div>
        </xsl:if>
    </xsl:template>

	<xsl:template name="result-count">
		<xsl:param name="path" />
		<xsl:variable name="cursor" select="$path/../oai:resumptionToken/@cursor" />
		<xsl:variable name="count" select="count($path)" />
		<xsl:variable name="total" select="$path/../oai:resumptionToken/@completeListSize" />
		<xsl:choose>
			<xsl:when test="$cursor">
				<xsl:choose>
					<xsl:when test="normalize-space($path/../oai:resumptionToken/text()) = ''">
					<!-- on the last page of results we have to assume that @completeListSize is available -->
						<xsl:value-of
							select="$total - $count" />
						-
						<xsl:value-of select="$total" />
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$cursor * $count" />
						-
						<xsl:value-of select="($cursor+1) * $count" />
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$count" />
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="$total">
			of
			<xsl:value-of select="$total" />
		</xsl:if>
	</xsl:template>

    <xsl:template match="oai:metadata/*" priority='-20'>
        <xsl:apply-templates select="." mode='xmlverb' />
    </xsl:template>

    <xsl:param name="indent-elements" select="false()" />

    <xsl:template match="/" mode="xmlverb">
        <xsl:text>&#xA;</xsl:text>
        <xsl:comment>
            <xsl:text> converted by xmlverbatim.xsl 1.1, (c) O. Becker </xsl:text>
        </xsl:comment>
        <xsl:text>&#xA;</xsl:text>
        <div class="xmlverb-default">
            <xsl:apply-templates mode="xmlverb">
                <xsl:with-param name="indent-elements" select="$indent-elements" />
            </xsl:apply-templates>
        </div>
        <xsl:text>&#xA;</xsl:text>
    </xsl:template>

    <!-- wrapper -->
    <xsl:template match="verb:wrapper">
        <xsl:apply-templates mode="xmlverb">
            <xsl:with-param name="indent-elements" select="$indent-elements" />
        </xsl:apply-templates>
    </xsl:template>

    <xsl:template match="verb:wrapper" mode="xmlverb">
        <xsl:apply-templates mode="xmlverb">
            <xsl:with-param name="indent-elements" select="$indent-elements" />
        </xsl:apply-templates>
    </xsl:template>

    <!-- element nodes -->
    <xsl:template match="*" mode="xmlverb">
        <xsl:param name="indent-elements" select="false()" />
        <xsl:param name="indent" select="''" />
        <xsl:param name="indent-increment" select="'&#xA0;&#xA0;&#xA0;'" />
        <xsl:if test="$indent-elements">
            <br/>
            <xsl:value-of select="$indent" />
        </xsl:if>
        <xsl:text>&lt;</xsl:text>
        <xsl:variable name="ns-prefix"
                      select="substring-before(name(),':')" />
        <xsl:if test="$ns-prefix != ''">
            <span class="xmlverb-element-nsprefix">
                <xsl:value-of select="$ns-prefix"/>
            </span>
            <xsl:text>:</xsl:text>
        </xsl:if>
        <span class="xmlverb-element-name">
            <xsl:value-of select="local-name()"/>
        </span>
        <xsl:variable name="pns" select="../namespace::*"/>
        <xsl:if test="$pns[name()=''] and not(namespace::*[name()=''])">
            <span class="xmlverb-ns-name">
                <xsl:text> xmlns</xsl:text>
            </span>
            <xsl:text>=&quot;&quot;</xsl:text>
        </xsl:if>
        <xsl:for-each select="namespace::*">
            <xsl:if test="not($pns[name()=name(current()) and
                           .=current()])">
                <xsl:call-template name="xmlverb-ns" />
            </xsl:if>
        </xsl:for-each>
        <xsl:for-each select="@*">
            <xsl:call-template name="xmlverb-attrs" />
        </xsl:for-each>
        <xsl:choose>
            <xsl:when test="node()">
                <xsl:text>&gt;</xsl:text>
                <xsl:apply-templates mode="xmlverb">
                    <xsl:with-param name="indent-elements"
                                    select="$indent-elements"/>
                    <xsl:with-param name="indent"
                                    select="concat($indent, $indent-increment)"/>
                    <xsl:with-param name="indent-increment"
                                    select="$indent-increment"/>
                </xsl:apply-templates>
                <xsl:if test="* and $indent-elements">
                    <br/>
                    <xsl:value-of select="$indent" />
                </xsl:if>
                <xsl:text>&lt;/</xsl:text>
                <xsl:if test="$ns-prefix != ''">
                    <span class="xmlverb-element-nsprefix">
                        <xsl:value-of select="$ns-prefix"/>
                    </span>
                    <xsl:text>:</xsl:text>
                </xsl:if>
                <span class="xmlverb-element-name">
                    <xsl:value-of select="local-name()"/>
                </span>
                <xsl:text>&gt;</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text> /&gt;</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:if test="not(parent::*)"><br /><xsl:text>&#xA;</xsl:text></xsl:if>
    </xsl:template>

    <!-- attribute nodes -->
    <xsl:template name="xmlverb-attrs">
        <xsl:text> </xsl:text>
        <span class="xmlverb-attr-name">
            <xsl:value-of select="name()"/>
        </span>
        <xsl:text>=&quot;</xsl:text>
        <span class="xmlverb-attr-content">
            <xsl:call-template name="html-replace-entities">
                <xsl:with-param name="text" select="normalize-space(.)" />
                <xsl:with-param name="attrs" select="true()" />
            </xsl:call-template>
        </span>
        <xsl:text>&quot;</xsl:text>
    </xsl:template>

    <!-- namespace nodes -->
    <xsl:template name="xmlverb-ns">
        <xsl:if test="name()!='xml'">
            <span class="xmlverb-ns-name">
                <xsl:text> xmlns</xsl:text>
                <xsl:if test="name()!=''">
                    <xsl:text>:</xsl:text>
                </xsl:if>
                <xsl:value-of select="name()"/>
            </span>
            <xsl:text>=&quot;</xsl:text>
            <span class="xmlverb-ns-uri">
                <xsl:value-of select="."/>
            </span>
            <xsl:text>&quot;</xsl:text>
        </xsl:if>
    </xsl:template>

    <!-- text nodes -->
    <xsl:template match="text()" mode="xmlverb">
        <span class="xmlverb-text">
            <xsl:call-template name="preformatted-output">
                <xsl:with-param name="text">
                    <xsl:call-template name="html-replace-entities">
                        <xsl:with-param name="text" select="." />
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </span>
    </xsl:template>

    <!-- comments -->
    <xsl:template match="comment()" mode="xmlverb">
        <xsl:text>&lt;!--</xsl:text>
        <span class="xmlverb-comment">
            <xsl:call-template name="preformatted-output">
                <xsl:with-param name="text" select="." />
            </xsl:call-template>
        </span>
        <xsl:text>--&gt;</xsl:text>
        <xsl:if test="not(parent::*)"><br /><xsl:text>&#xA;</xsl:text></xsl:if>
    </xsl:template>

    <!-- processing instructions -->
    <xsl:template match="processing-instruction()" mode="xmlverb">
        <xsl:text>&lt;?</xsl:text>
        <span class="xmlverb-pi-name">
            <xsl:value-of select="name()"/>
        </span>
        <xsl:if test=".!=''">
            <xsl:text> </xsl:text>
            <span class="xmlverb-pi-content">
                <xsl:value-of select="."/>
            </span>
        </xsl:if>
        <xsl:text>?&gt;</xsl:text>
        <xsl:if test="not(parent::*)"><br /><xsl:text>&#xA;</xsl:text></xsl:if>
    </xsl:template>


    <!-- =========================================================== -->
    <!--                    Procedures / Functions                   -->
    <!-- =========================================================== -->

    <!-- generate entities by replacing &, ", < and > in $text -->
    <xsl:template name="html-replace-entities">
        <xsl:param name="text" />
        <xsl:param name="attrs" />
        <xsl:variable name="tmp">
            <xsl:call-template name="replace-substring">
                <xsl:with-param name="from" select="'&gt;'" />
                <xsl:with-param name="to" select="'&amp;gt;'" />
                <xsl:with-param name="value">
                    <xsl:call-template name="replace-substring">
                        <xsl:with-param name="from" select="'&lt;'" />
                        <xsl:with-param name="to" select="'&amp;lt;'" />
                        <xsl:with-param name="value">
                            <xsl:call-template name="replace-substring">
                                <xsl:with-param name="from"
                                                select="'&amp;'" />
                                <xsl:with-param name="to"
                                                select="'&amp;amp;'" />
                                <xsl:with-param name="value"
                                                select="$text" />
                            </xsl:call-template>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:with-param>
            </xsl:call-template>
        </xsl:variable>
        <xsl:choose>
            <!-- $text is an attribute value -->
            <xsl:when test="$attrs">
                <xsl:call-template name="replace-substring">
                    <xsl:with-param name="from" select="'&#xA;'" />
                    <xsl:with-param name="to" select="'&amp;#xA;'" />
                    <xsl:with-param name="value">
                        <xsl:call-template name="replace-substring">
                            <xsl:with-param name="from"
                                            select="'&quot;'" />
                            <xsl:with-param name="to"
                                            select="'&amp;quot;'" />
                            <xsl:with-param name="value" select="$tmp" />
                        </xsl:call-template>
                    </xsl:with-param>
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$tmp" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- replace in $value substring $from with $to -->
    <xsl:template name="replace-substring">
        <xsl:param name="value" />
        <xsl:param name="from" />
        <xsl:param name="to" />
        <xsl:choose>
            <xsl:when test="contains($value,$from)">
                <xsl:value-of select="substring-before($value,$from)" />
                <xsl:value-of select="$to" />
                <xsl:call-template name="replace-substring">
                    <xsl:with-param name="value"
                                    select="substring-after($value,$from)" />
                    <xsl:with-param name="from" select="$from" />
                    <xsl:with-param name="to" select="$to" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$value" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- preformatted output: space as &nbsp;, tab as 8 &nbsp;
                              nl as <br> -->
    <xsl:template name="preformatted-output">
        <xsl:param name="text" />
        <xsl:call-template name="output-nl">
            <xsl:with-param name="text">
                <xsl:call-template name="replace-substring">
                    <xsl:with-param name="value"
                                    select="translate($text,' ','&#xA0;')" />
                    <xsl:with-param name="from" select="'&#9;'" />
                    <xsl:with-param name="to"
                                    select="'&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;&#xA0;'" />
                </xsl:call-template>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>

    <!-- output nl as <br> -->
    <xsl:template name="output-nl">
        <xsl:param name="text" />
        <xsl:choose>
            <xsl:when test="contains($text,'&#xA;')">
                <xsl:value-of select="substring-before($text,'&#xA;')" />
                <br />
                <xsl:text>&#xA;</xsl:text>
                <xsl:call-template name="output-nl">
                    <xsl:with-param name="text"
                                    select="substring-after($text,'&#xA;')" />
                </xsl:call-template>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$text" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
