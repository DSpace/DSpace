<?xml version="1.0" encoding="utf-8"?>
<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<xsl:stylesheet  version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:oai="http://www.openarchives.org/OAI/2.0/"
    xmlns:lyn="http://www.lyncode.com/fakeNamespace"
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:dc="http://purl.org/dc/doc:elements/1.1/"
    exclude-result-prefixes="oai lyn oai_dc dc"
>

	<xsl:output method="html"/>
	
	<xsl:template name="write-doctype">
		<xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd"&gt;
</xsl:text>
	</xsl:template>
	
	<xsl:template match="/">
		<xsl:call-template name="write-doctype"/>
		<html>
			<head>
				<title>DSpace OAI-PMH Data Provider</title>
				<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
				<script src="http://code.jquery.com/jquery-1.7.2.min.js" type="text/javascript"></script>
				<style type="text/css">
				a {
					color: #00b2d0;
					text-decoration: none;
				}
				body {
					text-align: center;
					font-family: "adelle-1","adelle-2", Georgia, Times, serif;
					color: #444751;
					margin: 0px;
					padding: 0px;
				}
				div.wrapper {
				}
				
				div.footer div.right {
					float: right;
					padding: 10px;
				}
				div.footer div.right {
					position: fixed;
					bottom: 0px;
					right: 20px;
					padding: 10px;
					text-align: center;
				}
				div.footer div.right p.image img {
					width: 150px;
				}
				div.header {
					margin-top: 20px;
					text-align: center;
					margin-bottom: 30px;
				}
				div.header div.contexts {
					background-color: #444751;
					-moz-box-shadow: 3px 3px 4px #999;
					-webkit-box-shadow: 3px 3px 4px #999;
					box-shadow: 3px 3px 4px #999;
				}
				
				div.header div.contexts ul {
					list-style-type: none;
				}
				
				div.header div.contexts ul li a:hover {
					background-color: #00b2d0;
					cursor: pointer;
				}
				
				div.header div.contexts ul li {
					float: left;
					padding-top: 10px;
					padding-bottom: 10px;
				}
				div.header div.contexts ul li a {
					color: white;
					text-decoration: none;
					padding: 10px;
					background-color: #444751;
					color: white;
					text-transform: uppercase;
				}
				div.header div.contexts ul li a.active {
					color: white;
					text-decoration: none;
					padding: 10px;
					background-color: #00b2d0;
					color: white;
					text-transform: uppercase;
				}
				
				div.header div.date {
				
				  color:#FFFFFF;
				  float:right;
				  margin-right:15px;
				  margin-top:10px;
				}
				
				div.header div.date label {
					margin-right: 10px;
					
				}
				
				table.identify {
					text-align: left;
					margin-left: auto;
					margin-right: auto;
				}
				
				tr.identify td.separator {
					border-bottom: solid 1px #CCC;
				}
				
				tr.info td.name {
					font-size: 20px;
					font-weight: bold;
					padding: 10px;
				}
				
				tr.info td.value {
					font-size: 20px;
					padding: 10px;
				}
				
				div.sets {
					display: inline-block;
					text-align: center;
					text-align: left;
				}
				
				div.set {
					border-left: solid 1px #ccc;
					color: #444751;
					padding: 10px;
					width: 100%;
					display: inline-block;
					margin-bottom: 2px;
					-moz-box-shadow: 1px 1px 1px #999;
					-webkit-box-shadow: 1px 1px 1px #999;
					box-shadow: 1px 1px 1px #999;
				}
				div.set div.spec {
					display: inline-block;
				}
				
				div.set div.name {
					font-size: 15px;
					display: inline-block;
				}
				
				div.setalternate {
					background-color: #00b2d0;
					border-left: solid 1px #00b2d0;
					padding: 10px;
					width: 100%;
					color: #FFF;
					margin-bottom: 2px;
					display: inline-block;
					-moz-box-shadow: 1px 1px 1px #999;
					-webkit-box-shadow: 1px 1px 1px #999;
					box-shadow: 1px 1px 1px #999;
				}
				div.setalternate div.spec {
					display: inline-block;
				}
				
				div.setalternate div.name {
					font-size: 15px;
					display: inline-block;
				}
				
				
				div.sets div.setalternate div.spec a {
					color: #FFF;
				}
				
				div.sets div div.spec a:hover {
					color: #FFF;
					background-color: #444751;
				}
				
				div.sets div div.spec a {
					border: solid 1px #CCC;
					margin-left: 5px;
					padding: 5px;
				}
				div.sets div div.spec {
					float: right;
				}
				.error {
					color: #D8000C;
					background-color: #FFBABA;
					border: solid 1px #D8000C;
					display: inline-block;
					padding: 20px;
					width: 60%;
				}
				
				.error div {
					display: inline-block;
					text-align: justify;
				}
				
				a.next {
					display: block;
					padding: 30px;
					text-align: center;
					font-size: 25px;
				}
				
				div.formats {
					display: inline-block;
				}
				
				a.format {
					color: #FFF;
					display: inline-block;
					padding: 20px;
					background-color: #00B2D0;
					border: solid 1px #00B2D0;
					margin-bottom: 15px;
					width: 100%;
				}
				a.format:hover {
					color: #444751;
					background-color: #FFF;
				}
				a.format div {
					text-align: left;
				}
				
				
				a.format div.prefix {
					text-align: center;
					font-size: 30px;
					margin-bottom: 15px;
				}
				
				div.identifiers {
					text-align: center;
				}
				div.identifiers div div.id {
					display: inline-block;
				}
				
				div.identifiers div div.datestamp {
					display: inline-block;
					margin-left: 20px;
				}
				
				
				div.identifiers div div.datestamp label {
					font-weight: bold;
					margin-right: 10px;
				}
				div.identifiers div div.id label {
					font-weight: bold;
					margin-right: 10px;
					
				}
				
				div.identifiers div.identifieralternate div.setspecs span {
					display: block;
					text-align: left;
					font-weight: bold;
					padding: 5px;
					margin-top: 10px;
					background-color: rgba(0,0,0,0.1);
				}
				div.identifiers div.setspecalternate {
					text-align: left;
					padding: 5px;
					padding-left: 15px;
					background-color: rgba(0,0,0,0.3);
				}
				div.identifiers div.setspec {
					text-align: left;
					padding: 5px;
					padding-left: 15px;
					background-color: rgba(0,0,0,0.2);
				}
				
				div.identifiers div.identifier div.setspecs span {
					display: block;
					text-align: left;
					font-weight: bold;
					margin-top: 10px;
					background-color: rgba(255,255,255,0.3);
					padding: 5px;
				}
				
				
				div.identifiers div div.setspecs span {
					cursor: pointer;
				}
				
				div.identifiers div.setspecs div.list {
					margin-left: 10px;
					margin-right: 10px;
					margin-top: 10px;
				}
				
				div.identifiers div.identifier {
					color: #FFF;
					display: inline-block;
					padding: 20px;
					background-color: #00B2D0;
					margin-bottom: 15px;
					-moz-box-shadow: 1px 1px 1px #EAEAEA;
					-webkit-box-shadow: 1px 1px 1px #EAEAEA;
					box-shadow: 1px 1px 1px #EAEAEA;
				}
				
				div.identifiers div.list div label {
					margin-top: 7px;
					display: block;
					float: left;
				}
				
				div.identifiers div.identifieralternate {
					color: #444751;
					display: inline-block;
					padding: 20px;
					background-color: #FFF;
					margin-bottom: 15px;
					-moz-box-shadow: 1px 1px 1px #999;
					-webkit-box-shadow: 1px 1px 1px #999;
					box-shadow: 1px 1px 1px #999;
				}
				div.identifiers div.innerspec {
					float: right;
				}
				div.identifiers div.innerspec a {
					border: solid 1px #FFF;
					color: #FFF;
					padding: 5px;
					margin: 5px;
					font-size: 10px;
					display: block;
					float: left;
				}
				div.identifiers div.innerspec a:hover {
					color: #444751;
					background-color: #FFF;
					border-color: #444751;
				}
				div.identifiers div.identifieralternate div.setspecs span:hover {
					background-color: #CCC;
				}
				
				div.identifiers div.identifier div.setspecs span:hover {
					background-color: rgba(255,255,255,0.6);
				}
				
				
				
				a.getrecord {
					display: block;
					margin-top: 5px;
					padding: 5px;
					padding-bottom: 0px;
				}
				
				a.getrecord:hover {
					color: #444751;
				}
				div.identifiers div.identifier a {
					color: #FFF;
				}
				div.identifiers div.identifier a:hover {
					color: #444751;
				}
				
				div.metadataPrefix {
					text-align: left;
					padding-left: 40px;
				}
				
				div.metadataPrefix label {
					margin-right: 5px;
					font-size: 20px;
				}
				
				div.metadataPrefix p {
					display: inline;
					margin-left: 10px;
				}
				div.metadataPrefix p span.mdset {
					margin-left: 10px;
					font-weight: bold;
					margin-right: 10px;
				}
				
				div.getRecord {
					text-align:center; color: #FFF;
				}
				
				div.getRecord div.getcontent {
					display: inline-block; padding: 20px; width: 900px; background-color: #00B2D0;
				}
				
				div.getRecord div.getcontent div.in {
					display: inline-block;
				}
				
				div.getRecord div.getcontent div.in label {
					font-weight: bold; margin: 5px;
				}
				
				
				div.getRecordAlternate {
					text-align:center; color: #444751;
				}
				
				div.getRecordAlternate div.getcontent {
					display: inline-block; padding: 20px; width: 900px; background-color: #FFF;
					-moz-box-shadow: 1px 1px 1px #999;
					-webkit-box-shadow: 1px 1px 1px #999;
					box-shadow: 1px 1px 1px #999;
				}
				
				
				div.getRecordAlternate div.getcontent div.in {
					display: inline-block;
				}
				
				div.getRecordAlternate div.getcontent div.in label {
					font-weight: bold;
					margin: 5px;
				}
				
				div.recSets label.lab {
					display: block;
					text-align: left;
					font-weight: bold;
					margin-top: 10px;
					background-color: rgba(255,255,255,0.3);
					padding: 5px;
				}
				
				div.getRecordAlternate label.lab {
					background-color: #EAEAEA;
				}
				div.recSets label.hiddable:hover {
					cursor: pointer;
					background-color: rgba(255,255,255,0.1);
				}
				div.getRecordAlternate label.lab:hover {
					cursor: pointer;
					background-color: rgba(0,0,0,0.3);
				}
				
				
				div.recSets div.list {
					margin-left: 10px;
					margin-right: 10px;
					margin-top: 10px;
				}
				
				div.recSets div.recSet {
					text-align: left;
					padding: 5px;
					padding-left: 15px;
					background-color: rgba(0,0,0,0.3);
				}
				
				div.recSets div.recSetAlternate {
					text-align: left;
					padding: 5px;
					padding-left: 15px;
					background-color: rgba(0,0,0,0.2);
				}
				div.recSets div.recSetInner {
					float: right;
				}
				div.recSets div.recSetInner a {
					border: solid 1px #FFF;
					color: #FFF;
					padding: 5px;
					margin: 5px;
					font-size: 12px;
					display: block;
					float: left;
				}
				div.recSets div.recSetInner a:hover {
					color: #444751;
					background-color: #FFF;
					border-color: #444751;
				}
				
				div.recSets label.getrecordsets {
					display: block;
					float: left;
					margin-top: 8px;
				}
				
				div.metadataTitle {
					display: block;
					text-align: left;
					font-weight: bold;
					margin-top: 10px;
					background-color: rgba(255, 255, 255, 0.3);
					padding: 5px;
				}
				
				div.divMetadata {
					text-align: left;
					margin: 10px;
					background-color: rgba(255,255,255,0.6);
					padding: 10px;
					color: #444751;
				}
				
				div.divXML { display: inline; }
				div.tagName { display: inline; }
				div.divText { display: inline; text-align: justify; }
				div.divAttribute { display: inline; color: #9C1313; }
				div.divAttributeValue { display: inline; color: #0A0E80; }
				
				.quote { color: #9C1313; }
				.equal { color: #9C1313; }
				
				.level1 { display: inline-block; margin-left: 10px; }
				.level2 { display: inline-block; margin-left: 20px; }
				.level3 { display: inline-block; margin-left: 30px; }
				.level4 { display: inline-block; margin-left: 40px; }
				.level5 { display: inline-block; margin-left: 50px; }
				.level6 { display: inline-block; margin-left: 60px; }
				.level7 { display: inline-block; margin-left: 70px; }
				.level8 { display: inline-block; margin-left: 80px; }
				.level9 { display: inline-block; margin-left: 90px; }
				.level10 { display: inline-block; margin-left: 100px; }
				
				.hiddable:hover {
					background-color: rgba(255,255,255,0.2);
					cursor: pointer;
				}
				div.getRecordAlternate .hiddable {
					background-color: #EAEAEA;
				}
				
				div.getRecordAlternate .hiddable:hover {
					cursor: pointer;
					background-color: rgba(0,0,0,0.3);
				}
				
				.result-count {
					text-align: left;
					padding: 20px;
					margin-left: 40px;
					margin-bottom: 20px;
				}
				.result-count label {
					font-weight: bold;
					margin-right: 10px;
				}
				
				.tag {
					color: #FFF;
					font-weight: bold;
				}
				
				div.getRecordAlternate .tag {
					color: #BBB;
				}
				
				.tagname {
					color: #2364D6;
				}
				
				.multiple {
					margin-bottom: 10px;
				}
				
				.tiny {
					font-size: 10px;
					margin-left: 10px;
				}
				
				.clear {
					clear: both;
				}
				</style>
			</head>
			<body>
				<div class="wrapper">
					<div class="header">
						<div>
							<h1>DSpace OAI-PMH Data Provider</h1>
							<div class="contexts">
								<ul>
									<li>
										<a title="Institutional information">
										<xsl:if test="/oai:OAI-PMH/oai:request/@verb = 'Identify'">
										<xsl:attribute name="class">active</xsl:attribute> 
										</xsl:if>
											<xsl:attribute name="href">
												<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=Identify')"></xsl:value-of>
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
												<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListSets')"></xsl:value-of>
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
												<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc')"></xsl:value-of>
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
												<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;metadataPrefix=oai_dc')"></xsl:value-of>
											</xsl:attribute>
											Identifiers
										</a>
									</li>
									<li>
										<a title="Metadata Formats available">
										<xsl:if test="/oai:OAI-PMH/oai:request/@verb = 'ListMetadataFormats'">
										<xsl:attribute name="class">active</xsl:attribute> 
										</xsl:if>
											<xsl:attribute name="href">
												<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListMetadataFormats')"></xsl:value-of>
											</xsl:attribute>
											Metadata Formats
										</a>
									</li>
								</ul>
								
								<div class="date">
									<label>Response Date</label>
									<span><xsl:value-of select="translate(oai:OAI-PMH/oai:responseDate/text(), 'TZ', ' ')" /></span>
								</div>
								<div class="clear"></div>
							</div>
						</div>
					</div>
					<div class="content">
						<xsl:if test="oai:OAI-PMH/oai:Identify">
						<table class="identify">
							<tr class="info">
								<td class="name">Repository Name</td>
								<td class="value"><xsl:value-of select="oai:OAI-PMH/oai:Identify/oai:repositoryName/text()" /></td>
								<td class="clear"></td>
							</tr>
							<tr class="info">
								<td class="name">E-Mail Contact</td>
								<td class="value"><a>
									<xsl:attribute name="href">
										<xsl:value-of select="concat('mailto:', oai:OAI-PMH/oai:Identify/oai:adminEmail/text())" />
									</xsl:attribute>
									<xsl:value-of select="oai:OAI-PMH/oai:Identify/oai:adminEmail/text()" /></a>
								</td>
								<td class="clear"></td>
							</tr>
							<tr class="info">
								<td class="name">Description</td>
								<td class="value"><xsl:value-of select="oai:OAI-PMH/oai:Identify/oai:description/node()/text()" /></td>
								<td class="clear"></td>
							</tr>
							<tr>
								<td class="separator"></td>
							</tr>
							<tr class="info">
								<td class="name">Protocol Version</td>
								<td class="value"><xsl:value-of select="oai:OAI-PMH/oai:Identify/oai:protocolVersion/text()" /></td>
								<td class="clear"></td>
							</tr>
							<tr class="info">
								<td class="name">Earliest Registered Date</td>
								<td class="value"><xsl:value-of select="translate(oai:OAI-PMH/oai:Identify/oai:earliestDatestamp/text(), 'TZ' ,' ')" /></td>
								<td class="clear"></td>
							</tr>
							<tr class="info">
								<td class="name">Date Granularity</td>
								<td class="value"><xsl:value-of select="translate(oai:OAI-PMH/oai:Identify/oai:granularity/text(), 'TZ', ' ')" /></td>
								<td class="clear"></td>
							</tr>
							<tr class="info">
								<td class="name">Deletion Mode</td>
								<td class="value"><xsl:value-of select="oai:OAI-PMH/oai:Identify/oai:deletedRecord/text()" /></td>
								<td class="clear"></td>
							</tr>
						</table>
						</xsl:if>
						<xsl:if test="oai:OAI-PMH/oai:ListSets">
							<div class="result-count"><label>Results fetched:</label>
								<span>
									<xsl:call-template name="result-count">
										<xsl:with-param name="path" select="oai:OAI-PMH/oai:ListSets/oai:set"/>
									</xsl:call-template>
								</span>
							</div>
							<div class="sets">
								<xsl:for-each select="oai:OAI-PMH/oai:ListSets/oai:set">
									<div class="set">
										<xsl:if test="position() mod 2 = 1">
											<xsl:attribute  name="class">setalternate</xsl:attribute>
										</xsl:if>
										<div class="name">
											<xsl:choose>
												<xsl:when test="string-length(oai:setName/text()) &gt; 83">
													<xsl:value-of select="substring(oai:setName/text(),0, 80 )"/>...
												</xsl:when>
												<xsl:otherwise>
													<xsl:value-of select="oai:setName/text()"/>
												</xsl:otherwise>
											</xsl:choose>
											<span class="tiny">(<xsl:value-of select="oai:setSpec/text()" />)</span>
										</div>
										<div class="spec">
											<a>
												<xsl:attribute name="href">
													<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', oai:setSpec/text())" />
												</xsl:attribute>
												Records
											</a>
											<a>
												<xsl:attribute name="href">
													<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;metadataPrefix=oai_dc&amp;set=', oai:setSpec/text())" />
												</xsl:attribute>
												Identifiers
											</a>
										</div>
										<div class="clear"></div>
									</div>
									<div class="clear"></div>
								</xsl:for-each>
								
								<xsl:if test="oai:OAI-PMH/oai:ListSets/oai:resumptionToken/text() != ''">
								<a class="next">
										<xsl:attribute name="href">
											<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListSets&amp;resumptionToken=', oai:OAI-PMH/oai:ListSets/oai:resumptionToken/text())"></xsl:value-of>
										</xsl:attribute>
										Show More
								</a> 
								</xsl:if>
							</div> 
							<script type="text/javascript">
							$(document).ready(function (){
								$('.sets div').mouseover(function() {
									$(this).find('a').css("visibility", "visible");
								});
								$('.sets div').mouseout(function () {
									$(this).find('a').css("visibility", "hidden");
								});
								$('.sets div').find('a').css("visibility", "hidden");
							});
							</script>
						</xsl:if>
						<xsl:if test="oai:OAI-PMH/oai:error">
							<div class="error">
								<div>
								<xsl:value-of select="oai:OAI-PMH/oai:error/text()"></xsl:value-of>
								</div>
							</div>
						</xsl:if>
						<xsl:if test="oai:OAI-PMH/oai:ListMetadataFormats">
							<div class="result-count"><label>Results fetched</label> <span><xsl:value-of select="count(oai:OAI-PMH/oai:ListMetadataFormats/oai:metadataFormat)"/></span></div>
							<div class="formats">
								<xsl:for-each select="oai:OAI-PMH/oai:ListMetadataFormats/oai:metadataFormat">
									<a class="format">
										<xsl:attribute name="href">
											<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=', oai:metadataPrefix/text())" />
										</xsl:attribute>
										<div class="prefix">
											<xsl:value-of select="oai:metadataPrefix/text()"></xsl:value-of>
										</div>
										<div class="namespace">
											Namespace: 
											<xsl:value-of select="oai:metadataNamespace/text()"></xsl:value-of>
										</div>
										<div class="schema">
											Schema: 
											<xsl:value-of select="oai:schema/text()"></xsl:value-of>
										</div>
										<div class="clear"></div>
									</a>
									<div class="clear"></div>
								</xsl:for-each>
							</div>
						</xsl:if>
						<xsl:if test="oai:OAI-PMH/oai:ListIdentifiers">
						<div class="identifiers">
							<div class="result-count"><label>Results fetched:</label>
								<span>
									<xsl:call-template name="result-count">
										<xsl:with-param name="path" select="oai:OAI-PMH/oai:ListIdentifiers/oai:header"/>
									</xsl:call-template>
								</span>
							</div>
							<xsl:for-each select="oai:OAI-PMH/oai:ListIdentifiers/oai:header">
							<div class="identifier">
								<xsl:if test="position() mod 2 = 0">
									<xsl:attribute  name="class">identifieralternate</xsl:attribute>
								</xsl:if>
								<div class="id">
									<label>Identifier</label>
									<span>
										<xsl:value-of select="oai:identifier" />
									</span>
								</div>
								<div class="datestamp">
									<label>Last Modified</label>
									<span>
									<xsl:value-of select="translate(oai:datestamp, 'TZ', ' ')" />
									</span>
								</div>
								<div class="setspecs">
									<span>Sets</span>
									<div class="list">
									<xsl:for-each select="oai:setSpec">
										<div class="setspec">
											<xsl:if test="position() mod 2 = 1">
												<xsl:attribute name="class">
													setspecalternate
												</xsl:attribute>
											</xsl:if>
											<label><xsl:value-of select="text()" /></label>
											<div class="innerspec">
												<a>
													<xsl:attribute name="href">
														<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', text())" />
													</xsl:attribute>
													Records
												</a>
												<a>
													<xsl:attribute name="href">
														<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;metadataPrefix=oai_dc&amp;set=', text())" />
													</xsl:attribute>
													Identifiers
												</a>
												<div class="clear"></div>
											</div>
											<div class="clear"></div>
										</div>
									</xsl:for-each>
									</div>
									<a class="getrecord">
										<xsl:attribute name="href">
											<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=GetRecord&amp;metadataPrefix=oai_dc&amp;identifier=', oai:identifier/text())" />
										</xsl:attribute>
										Get Record
									</a>
								</div>
							</div>
							<div class="clear"></div>
							</xsl:for-each>
							
							<xsl:if test="oai:OAI-PMH/oai:ListIdentifiers/oai:resumptionToken/text() != ''">
							<a class="next">
									<xsl:attribute name="href">
										<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;resumptionToken=', oai:OAI-PMH/oai:ListIdentifiers/oai:resumptionToken/text())"></xsl:value-of>
									</xsl:attribute>
									Show More
							</a> 
							</xsl:if>
						</div>
						<script type="text/javascript">
						$(document).ready(function(){
							$('div.identifiers div div.setspecs span').click(function(){
								$(this).parent().children('.list').toggle();
							});
							$('div.identifiers div div.setspecs div.list').hide();
						});
						$(document).ready(function() {
							$('div.identifiers div.list div').mouseover(function(){
								$(this).find('a').css('visibility','visible');
							});
							$('div.identifiers div.list div').mouseout(function(){
								$(this).find('a').css('visibility','hidden');
							});
							$('div.identifiers div.list div a').css('visibility','hidden');
						});
						</script>
						</xsl:if>
						<xsl:if test="oai:OAI-PMH/oai:GetRecord">
							<div class="getRecord">
								<div class="getcontent">
									<div class="in">
										<label>Identifier</label>
										<span><xsl:value-of select="oai:OAI-PMH/oai:GetRecord/oai:record/oai:header/oai:identifier/text()"></xsl:value-of></span>
									</div>
									<div class="in">
										<label>Last Modified</label>
										<span><xsl:value-of select="translate(oai:OAI-PMH/oai:GetRecord/oai:record/oai:header/oai:datestamp/text(), 'TZ', ' ')"></xsl:value-of></span>
									</div>
									<div class="clear"></div>
									<div class="recSets">
										<label class="lab">Sets</label>
										<div class="list">
										<xsl:for-each select="oai:OAI-PMH/oai:GetRecord/oai:record/oai:header/oai:setSpec">
										
											<div class="recSet">
												<xsl:if test="position() mod 2 = 1">
													<xsl:attribute name="class">recSetAlternate</xsl:attribute>
												</xsl:if>
												<label class="getrecordsets"><xsl:value-of select="text()" /></label>
												<div class="recSetInner">
													<a>
														<xsl:attribute name="href">
															<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', text())" />
														</xsl:attribute>
														Records
													</a>
													<a>
														<xsl:attribute name="href">
															<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;metadataPrefix=oai_dc&amp;set=', text())" />
														</xsl:attribute>
														Identifiers
													</a>
													<div class="clear"></div>
												</div>
												<div class="clear"></div>
											</div>
											<div class="clear"></div>
										</xsl:for-each>
											<div class="clear"></div>
										</div>
									</div>
									<div class="metadata">
										<div class="metadataTitle hiddable">Metadata</div>
										<xsl:apply-templates select="oai:OAI-PMH/oai:GetRecord/oai:record/oai:metadata/*" />
									</div>
								</div>
							</div>
							<script type="text/javascript">
							$(document).ready(function(){
								$('div.recSetInner a').css('visibility','hidden');
								$('div.recSetInner').parent().mouseover(function(){
									$(this).find('a').css('visibility', 'visible');
								});
								$('div.recSetInner').parent().mouseout(function(){
									$(this).find('a').css('visibility', 'hidden');
								});
							});
							</script>
						</xsl:if>
						<xsl:if test="oai:OAI-PMH/oai:ListRecords">
							<div class="result-count"><label>Results fetched:</label>
								<span>
									<xsl:call-template name="result-count">
										<xsl:with-param name="path" select="oai:OAI-PMH/oai:ListRecords/oai:record"/>
									</xsl:call-template>
<!--
									<xsl:choose>
										<xsl:when test="oai:OAI-PMH/oai:ListRecords/oai:resumptionToken/@cursor">
											<xsl:variable name="cursor" select="oai:OAI-PMH/oai:ListRecords/oai:resumptionToken/@cursor"/>
											<xsl:variable name="per-page" select="count(oai:OAI-PMH/oai:ListRecords/oai:record)"/>
											<xsl:value-of select="$cursor * $per-page"/>-<xsl:value-of select="($cursor+1) * $per-page"/>
										</xsl:when>
										<xsl:otherwise>
											<xsl:value-of select="count(oai:OAI-PMH/oai:ListRecords/oai:record)"/>
										</xsl:otherwise>
									</xsl:choose>
									<xsl:if test="oai:OAI-PMH/oai:ListRecords/oai:resumptionToken/@completeListSize">
										of <xsl:value-of select="oai:OAI-PMH/oai:ListRecords/oai:resumptionToken/@completeListSize"/>
									</xsl:if>
-->
								</span>
							</div>
							<xsl:for-each select="oai:OAI-PMH/oai:ListRecords/oai:record">
							<div class="getRecord multiple">
								<xsl:if test="position() mod 2 = 0">
								<xsl:attribute name="class">
									getRecordAlternate multiple
								</xsl:attribute>
								</xsl:if>
								<div class="getcontent">
									<div class="in">
										<label>Identifier</label>
										<span><xsl:value-of select="oai:header/oai:identifier/text()"></xsl:value-of></span>
									</div>
									<div class="in">
										<label>Last Modified</label>
										<span><xsl:value-of select="translate(oai:header/oai:datestamp/text(), 'TZ', ' ')"></xsl:value-of></span>
									</div>
									<div class="clear"></div>
									<div class="recSets">
										<label class="lab hiddable">Sets</label>
										<div class="list">
										<xsl:for-each select="oai:header/oai:setSpec">
											<div class="recSet">
												<xsl:if test="position() mod 2 = 1">
													<xsl:attribute name="class">recSetAlternate</xsl:attribute>
												</xsl:if>
												<label class="getrecordsets"><xsl:value-of select="text()" /></label>
												<div class="recSetInner">
													<a>
														<xsl:attribute name="href">
															<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;metadataPrefix=oai_dc&amp;set=', text())" />
														</xsl:attribute>
														Records
													</a>
													<a>
														<xsl:attribute name="href">
															<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListIdentifiers&amp;metadataPrefix=oai_dc&amp;set=', text())" />
														</xsl:attribute>
														Identifiers
													</a>
													<div class="clear"></div>
												</div>
												<div class="clear"></div>
											</div>
											<div class="clear"></div>
										</xsl:for-each>
											<div class="clear"></div>
										</div>
									</div>
									<div class="metadata">
										<div class="metadataTitle hiddable">Metadata</div>
										<xsl:apply-templates select="oai:metadata/*" />
									</div>
								</div>
							</div>
							</xsl:for-each>
							
							<xsl:if test="oai:OAI-PMH/oai:ListRecords/oai:resumptionToken/text() != ''">
							<a class="next">
									<xsl:attribute name="href">
										<xsl:value-of select="concat(/oai:OAI-PMH/oai:request/text(), '?verb=ListRecords&amp;resumptionToken=', oai:OAI-PMH/oai:ListRecords/oai:resumptionToken/text())"></xsl:value-of>
									</xsl:attribute>
									Show More
							</a> 
							</xsl:if>
							<script type="text/javascript">
							$(document).ready(function(){
								$('div.recSetInner a').css('visibility','hidden');
								$('div.recSetInner').parent().mouseover(function(){
									$(this).find('a').css('visibility', 'visible');
								});
								$('div.recSetInner').parent().mouseout(function(){
									$(this).find('a').css('visibility', 'hidden');
								});
								$('div.list').hide();
								$('label.hiddable').click(function(){
									$(this).parent().children('div.list').toggle();
								});
								$('div.divMetadata').hide();
								$('div.hiddable').click(function(){
									$(this).parent().children('div.divMetadata').toggle();
								});
							});
							</script>
						</xsl:if>
					</div>
					<div class="footer">
						<div class="right">
							<p class="text">Stylesheet provided by</p>
							<p class="image"><a href="http://www.lyncode.com"><img src="static/lyncode.png" alt="Lyncode logo" /></a></p>
						</div>
					</div>
				</div>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="oai:metadata/*" priority='-20'>
		<div class="divMetadata">
			<xsl:apply-templates select="." mode='prettyXML' />
		</div>
	</xsl:template>
	
	<xsl:template match="node()" mode='prettyXML'>
		<div class="divXML">
			<div class="clear"></div>
			<span>
				<xsl:attribute name="class">level<xsl:value-of select="count(ancestor::*)-4" /></xsl:attribute>
			</span>
			<span class="tag">&lt;</span><span class="tagname"><xsl:value-of select='name(.)' /></span><xsl:apply-templates select="@*" mode='prettyXML'/><span class="tag">&gt;</span>
			<xsl:apply-templates select="node()" mode='prettyXML' />
			<div class="clear"></div>
			<span>
				<xsl:attribute name="class">level<xsl:value-of select="count(ancestor::*)-4" /></xsl:attribute>
			</span>
			<span class="tag">&lt;/</span><span class="tagname"><xsl:value-of select='name(.)' /></span><span class="tag">&gt;</span>
		</div>
	</xsl:template>
	
	<xsl:template match="text()" mode='prettyXML'>
		<div class="clear"></div>
		<div class="divText">
			<xsl:attribute name="class">level<xsl:value-of select="count(ancestor::*)-3" /> divText</xsl:attribute>
			<xsl:value-of select='.' />
		</div>
	</xsl:template>
	
	<xsl:template match="@*" mode='prettyXML'>
	  <xsl:text> </xsl:text><div class="divAttribute"><xsl:value-of select='name()' /></div><span class="equal">=</span><span class="quote">"</span><div class="divAttributeValue"><xsl:value-of select='.' /></div><span class="quote">"</span>
	</xsl:template>
	
	
	<xsl:template name="result-count">
		<xsl:param name="path" />
		<xsl:choose>
			<xsl:when test="$path/../oai:resumptionToken/@cursor">
				<xsl:variable name="cursor" select="$path/../oai:resumptionToken/@cursor"/>
				<xsl:variable name="per-page" select="count($path)"/>
				<xsl:choose>
					<xsl:when test="$per-page &lt; 100"> <!-- on the last page of results we have to assume that there were 100 results per page and that @completeListSize is available -->
						<xsl:value-of select="$path/../oai:resumptionToken/@completeListSize - $per-page"/>-<xsl:value-of select="$path/../oai:resumptionToken/@completeListSize"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$cursor * $per-page"/>-<xsl:value-of select="($cursor+1) * $per-page"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="count($path)"/>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="$path/../oai:resumptionToken/@completeListSize">
			of <xsl:value-of select="$path/../oai:resumptionToken/@completeListSize"/>
		</xsl:if>
	</xsl:template>
</xsl:stylesheet>
