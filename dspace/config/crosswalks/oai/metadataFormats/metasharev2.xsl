<?xml version="1.0" encoding="UTF-8" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai"
    xmlns:ms="http://www.ilsp.gr/META-XMLSchema"
    xmlns:fn="http://custom.crosswalk.functions"
    exclude-result-prefixes="doc fn"
    version="2.0">

    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes"/>
    
    <xsl:variable name="handle" select="/doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()"/>
    <xsl:variable name="rightsUri" select="doc:metadata/doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='uri']/doc:element/doc:field[@name='value']"/>
    <xsl:variable name="type">
        <xsl:call-template name="Type"/>
    </xsl:variable>
    <xsl:variable name="mediaType">
        <xsl:call-template name="MediaType"/>
    </xsl:variable>
    <xsl:variable name="detailedType">
        <xsl:call-template name="DetailedType"/>
    </xsl:variable>
    
    
    <xsl:template match="/">
	   <xsl:call-template name="ResourceInfo">
		<!-- ugly hack to change the namespace of some elements in ResourceComponentType, the rest changed using aliasing -->
		<xsl:with-param name="ns" select='"http://www.ilsp.gr/META-XMLSchema"'/>
	   </xsl:call-template>
    </xsl:template>

    <xsl:template name="ResourceInfo">
    <xsl:param name="ns"/>
           <ms:resourceInfo>
		<xsl:if test="$ns='http://www.ilsp.gr/META-XMLSchema'">
			<xsl:attribute name="xsi:schemaLocation" namespace="http://www.w3.org/2001/XMLSchema-instance">http://www.ilsp.gr/META-XMLSchema http://metashare.ilsp.gr/META-XMLSchema/v2.0/META-SHARE-Resource.xsd</xsl:attribute>
		</xsl:if>
                <xsl:call-template name="IdentificationInfo"/>
                <xsl:call-template name="DistributionInfo"/>
                <xsl:call-template name="ContactInfo"/>        
                <xsl:call-template name="MetadataInfo"/>
                <!-- <xsl:call-template name="VersionInfo"/> -->
                <xsl:call-template name="ValidationInfo"/>
                <!-- <xsl:call-template name="UsageInfo"/> -->
                <xsl:call-template name="ResourceDocumentationInfo"/>
                <xsl:call-template name="ResourceCreationInfo"/>      
                <xsl:call-template name="ResourceComponentType">
			<xsl:with-param name="ns" select="$ns"/>
                </xsl:call-template>
        </ms:resourceInfo>
    </xsl:template>
    
    <xsl:template name="IdentificationInfo">
        <ms:identificationInfo>
            <ms:resourceName>
                <xsl:choose>
                    <xsl:when test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#IdentificationInfo']/doc:element[@name='resourceName']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#IdentificationInfo']/doc:element[@name='resourceName']/doc:element/doc:field[@name='value']" />
                    </xsl:when>
                    <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="fn:logMissing('resourceName',$handle)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </ms:resourceName>
            <ms:description>
                <xsl:choose>
                    <xsl:when test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='description']/doc:element/doc:field[@name='value']" />
                    </xsl:when>
                    <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="fn:logMissing('description',$handle)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </ms:description>
            <ms:metaShareId>NOT_DEFINED_FOR_V2</ms:metaShareId>
            <ms:identifier>
                <xsl:choose>
                    <xsl:when test="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="doc:metadata/doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']" />
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="fn:logMissing('identifier',$handle)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </ms:identifier>
    </ms:identificationInfo>
    </xsl:template>
    
    <xsl:template name="DistributionInfo">
    <ms:distributionInfo>
           <ms:availability><xsl:value-of select="fn:uriToAvailability($rightsUri)"/></ms:availability>
            <ms:licenceInfo>
                <ms:licence><xsl:value-of select="fn:uriToMetashare($rightsUri)"/></ms:licence>
                <xsl:for-each select="fn:uriToRestrictions($rightsUri)">
                        <ms:restrictionsOfUse><xsl:value-of select="."/></ms:restrictionsOfUse>
                </xsl:for-each>
                    <xsl:choose>
                        <xsl:when test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#DistributionInfo#LicenseInfo']/doc:element[@name='distributionAccessMedium']/doc:element/doc:field[@name='value']">
                                <xsl:for-each select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#DistributionInfo#LicenseInfo']/doc:element[@name='distributionAccessMedium']/doc:element/doc:field[@name='value']">
                                <ms:distributionAccessMedium>
                                <xsl:variable name="dam" select="." />
                                <xsl:choose>
                                        <xsl:when test="$dam='download'">
                                                <xsl:text>downloadable</xsl:text>
                                        </xsl:when>
                                        <xsl:when test="$dam='internetBrowsing'">
                                                <xsl:text>webExecutable</xsl:text>
                                        </xsl:when>
                                        <xsl:otherwise>
                                                <xsl:value-of select="$dam" />
                                        </xsl:otherwise>
                                </xsl:choose>
                                </ms:distributionAccessMedium>
                                </xsl:for-each>
                        </xsl:when>
                        <xsl:otherwise>
                        <ms:distributionAccessMedium>
                            <xsl:value-of select="fn:logMissing('distributionAccessMedium',$handle)"/>
                        </ms:distributionAccessMedium>
                        </xsl:otherwise>
                    </xsl:choose>
            </ms:licenceInfo>
            </ms:distributionInfo>
        </xsl:template>

    <xsl:template name="ContactInfo">
	<ms:contactPerson>
		<ms:surname>
			<xsl:choose>
				<xsl:when
					test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo']/doc:element[@name='surname']/doc:element/doc:field[@name='value']">
					<xsl:value-of
						select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo']/doc:element[@name='surname']/doc:element/doc:field[@name='value']" />
				</xsl:when>
                <xsl:when
                    test="doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value']">
                    <xsl:value-of select="tokenize(doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value'], ';')[2]"/>
                </xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="fn:logMissing('surname',$handle)" />
				</xsl:otherwise>
			</xsl:choose>
		</ms:surname>
		<ms:givenName>
            <xsl:choose>
                <xsl:when
                    test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo']/doc:element[@name='givenName']/doc:element/doc:field[@name='value']">
                    <xsl:value-of
                        select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo']/doc:element[@name='givenName']/doc:element/doc:field[@name='value']" />
                </xsl:when>
                <xsl:when
                    test="doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value']">
                    <xsl:value-of select="tokenize(doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value'], ';')[1]"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="fn:logMissing('givenName',$handle)" />
                </xsl:otherwise>
            </xsl:choose>		
		</ms:givenName>
        <xsl:call-template name="CommunicationInfo" />
		<xsl:choose>
			<xsl:when 
                test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo#OrganizationInfo']/doc:element[@name='organizationName']/doc:element/doc:field[@name='value']">
				<ms:affiliation>
				    <ms:organizationName>
					   <xsl:value-of select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo#OrganizationInfo']/doc:element[@name='organizationName']/doc:element/doc:field[@name='value']" />
					</ms:organizationName>
					<!--another communicationInfo needed -->
					<xsl:call-template name="CommunicationInfo" />
				</ms:affiliation>
			</xsl:when>
			<xsl:when
                test="doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value']">
				<ms:affiliation>
				    <ms:organizationName>
					   <xsl:value-of select="tokenize(doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value'], ';')[4]" />
					</ms:organizationName>
					<!--another communicationInfo needed -->
					<xsl:call-template name="CommunicationInfo" />
				</ms:affiliation>
			</xsl:when>
		</xsl:choose>
	</ms:contactPerson>
    </xsl:template>
    
    <xsl:template name="CommunicationInfo">
        <ms:communicationInfo>
            <ms:email>
                    <xsl:choose>
                        <xsl:when
                            test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo#OrganizationInfo#CommunicationInfo']/doc:element[@name='email']/doc:element/doc:field[@name='value']">
                            <xsl:value-of
                                select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContactInfo#PersonInfo#OrganizationInfo#CommunicationInfo']/doc:element[@name='email']/doc:element/doc:field[@name='value']" />
                        </xsl:when>
                        <xsl:when
                            test="doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value']">
                            <xsl:value-of select="tokenize(doc:metadata/doc:element[@name='local']/doc:element[@name='contact']/doc:element[@name='person']/doc:element/doc:field[@name='value'], ';')[3]"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="fn:logMissing('email',$handle)" />
                        </xsl:otherwise>
                    </xsl:choose>        
            </ms:email>   
        </ms:communicationInfo>    
    </xsl:template>

    <xsl:template name="MetadataInfo">
        <ms:metadataInfo>
            <ms:metadataCreationDate>
                    <xsl:value-of select="substring(/doc:metadata/doc:element[@name='others']/doc:field[@name='lastModifyDate']/text(),1,10)"/>
            </ms:metadataCreationDate>
        </ms:metadataInfo>
    </xsl:template>

	<xsl:template name="ValidationInfo">
		<xsl:choose>
			<xsl:when
				test="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ValidationInfo']/doc:element[@name='validated']/doc:element/doc:field[@name='value']">
				<ms:validationInfo>
					<ms:validated>
						<xsl:value-of
							select="translate(doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ValidationInfo']/doc:element[@name='validated']/doc:element/doc:field[@name='value'],'TF','tf')" />
					</ms:validated>
				</ms:validationInfo>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

    <xsl:template name="ResourceDocumentationInfo">
        <xsl:choose>
            <xsl:when test="doc:metadata/doc:element[@name='local']/doc:element[@name='demo']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
                <ms:resourceDocumentationInfo>
                    <ms:samplesLocation>
                        <xsl:value-of select="doc:metadata/doc:element[@name='local']/doc:element[@name='demo']/doc:element[@name='uri']/doc:element/doc:field[@name='value']" />
                    </ms:samplesLocation>
                </ms:resourceDocumentationInfo>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="ResourceCreationInfo">
        <xsl:variable name="fundingProjectNameCount" select="count(doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo']/doc:element[@name='projectName']/doc:element/doc:field[@name='value'])"/>
        <xsl:variable name="fundingTypeCount" select="count(doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo']/doc:element[@name='fundingType']/doc:element/doc:field[@name='value'])"/>
        <xsl:choose>
            <!-- We need fundingType for each projectName and vice versa, when there's at least one pair -->
            <xsl:when test="$fundingProjectNameCount = $fundingTypeCount  and $fundingProjectNameCount &gt; 0">
                <ms:resourceCreationInfo>
                        <xsl:choose>
                            <xsl:when test="not(starts-with(doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo']/doc:element[@name='projectName']/doc:element/doc:field[@name='value'],'#'))">
                                <ms:fundingProject>
                                    <ms:projectName>
                                        <xsl:value-of select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo']/doc:element[@name='projectName']/doc:element/doc:field[@name='value']"/>
                                    </ms:projectName>
                                    <xsl:variable name="funding" select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo']/doc:element[@name='fundingType']/doc:element/doc:field[@name='value']"/>
                                        <xsl:call-template name="FundingType">
                                            <xsl:with-param name="funding" select="$funding"/>
                                        </xsl:call-template>
                                </ms:fundingProject>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:for-each select="doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo']/doc:element[@name='projectName']/doc:element/doc:field[@name='value']">
                                    <xsl:variable name="compId" select="substring-before(.,'-')"/>
                                    <xsl:variable name="projectName" select="substring-after(.,'-')"/>
                                    <ms:fundingProject>
                                        <ms:projectName>
                                            <xsl:value-of select="$projectName" />
                                        </ms:projectName>
                                            <xsl:variable name="funding" select="substring-after(/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceCreationInfo#FundingInfo#ProjectInfo']/doc:element[@name='fundingType']/doc:element/doc:field[@name='value'][starts-with(.,$compId)],'-')"/>
                                            <xsl:call-template name="FundingType">
                                                <xsl:with-param name="funding" select="$funding"/>
                                            </xsl:call-template>
                                    </ms:fundingProject>
                                </xsl:for-each>
                            </xsl:otherwise>
                        </xsl:choose>                    
                </ms:resourceCreationInfo>
            </xsl:when>
            <xsl:when test="doc:metadata/doc:element[@name='local']/doc:element[@name='sponsor']/doc:element/doc:field[@name='value']">
                <ms:resourceCreationInfo>
                                <xsl:for-each select="doc:metadata/doc:element[@name='local']/doc:element[@name='sponsor']/doc:element/doc:field[@name='value']">
                                                <ms:fundingProject>
                                                    <ms:projectName>
                                                            <xsl:value-of select="tokenize(., ';')[3]"/>
                                                    </ms:projectName>
                                                    <ms:fundingType>
                                                            <xsl:value-of select="tokenize(., ';')[4]"/>
                                                    </ms:fundingType>
                                                </ms:fundingProject>
                                </xsl:for-each>
                </ms:resourceCreationInfo>
            </xsl:when>
            <xsl:otherwise>
                <xsl:choose>
                    <xsl:when test="not($fundingProjectNameCount = $fundingTypeCount)">
                        <!-- Implies funding info is incomplete -->
                        <xsl:variable name="msg" select="concat('Item with handle ', $handle,' has incomplete funding info')"/>
                        <xsl:variable name="iJustWantToLog" select="fn:logMissingMsg('fundingInfo', $handle, $msg)" />
                    </xsl:when>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

	<xsl:template name="FundingType">
		<xsl:param name="funding" />
		<ms:fundingType>
		<xsl:choose>
			<xsl:when test="$funding='EU'">euFunds</xsl:when>
			<xsl:when test="$funding='Own'">ownFunds</xsl:when>
			<xsl:when test="$funding='National'">nationalFunds</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$funding" />
			</xsl:otherwise>
		</xsl:choose>
		</ms:fundingType>
	</xsl:template>
	
	<xsl:template name="Type">
        <xsl:choose>
            <xsl:when test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='resourceType']/doc:element/doc:field[@name='value']">
                <xsl:value-of select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='resourceType']/doc:element/doc:field[@name='value']"/>
            </xsl:when>
            <xsl:when test="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']">
                <xsl:value-of select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='type']/doc:element/doc:field[@name='value']"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="fn:logMissing('type',$handle)"/>
            </xsl:otherwise>
        </xsl:choose>
	</xsl:template>
	
	<xsl:template name="MediaType">
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
	</xsl:template>

	<xsl:template name="DetailedType">
	   <!-- No detailed type for corpus -->
	   <xsl:if test="not($type='corpus')">
	       <xsl:choose>
	           <xsl:when test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='detailedType']/doc:element/doc:field[@name='value']">
                   <xsl:variable name="val"
                                 select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ContentInfo']/doc:element[@name='detailedType']/doc:element/doc:field[@name='value']"/>
                   <xsl:choose>
                       <xsl:when test="$type='languageDescription' and not($val='grammar')">other</xsl:when>
                       <xsl:otherwise>
                           <xsl:value-of select="$val"/>
                       </xsl:otherwise>
                   </xsl:choose>
	           </xsl:when>
	           <xsl:otherwise>
                   <xsl:value-of select="fn:logMissing('detailedType',$handle)"/>
	           </xsl:otherwise>
	       </xsl:choose>
	   </xsl:if>
	</xsl:template>
	
	<xsl:template name="ResourceComponentType">
        <xsl:param name="ns"/>
	   <ms:resourceComponentType>
	       <xsl:element name="ms:{$type}Info" namespace="{$ns}">
               <!-- easier to hack it like this than fix a typo in cmdi profile with inlined components :facepalm: -->
               <xsl:choose>
                   <xsl:when test="$type='languageDescription' and $ns='http://www.clarin.eu/cmd/'">
                       <ms:resoureType><xsl:value-of select="$type"/></ms:resoureType>
                   </xsl:when>
                   <xsl:otherwise>
                       <ms:resourceType><xsl:value-of select="$type"/></ms:resourceType>
                   </xsl:otherwise>
               </xsl:choose>
	           <!-- Everything but corpus should have detailedType -->
	           <xsl:if test="not($type='corpus')">
	               <xsl:element name="ms:{$type}Type" namespace="{$ns}"><xsl:value-of select="$detailedType"/></xsl:element>
	           </xsl:if>
	           <!-- Tools don't have linguality info -->
	           <xsl:if test="not($type='toolService')">
	               <xsl:variable name="upperMediaType">
	                   <xsl:value-of select="translate(substring($mediaType,1,1),'abcdefghijklmnopqrstuvwxyz','ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/><xsl:value-of select="substring($mediaType,2)"/>
	               </xsl:variable>
	               <xsl:element name="ms:{$type}MediaType" namespace="{$ns}">
	                   <xsl:element name="ms:{$type}{$upperMediaType}Info" namespace="{$ns}">
	                       <ms:mediaType><xsl:value-of select="$mediaType"/></ms:mediaType>
	                       <xsl:variable name="langCount" select="count(/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value'])"/>
	                       <xsl:choose>
	                           <xsl:when test="$langCount&gt;0">
	                               <ms:lingualityInfo>
	                                   <ms:lingualityType>
	                                       <xsl:choose>
	                                           <xsl:when test="$langCount=1">monolingual</xsl:when>
	                                           <xsl:when test="$langCount=2">bilingual</xsl:when>
	                                           <xsl:otherwise>multilingual</xsl:otherwise>
	                                       </xsl:choose>
	                                   </ms:lingualityType>
	                               </ms:lingualityInfo> 
	                               <xsl:for-each select="/doc:metadata/doc:element[@name='dc']/doc:element[@name='language']/doc:element[@name='iso']/doc:element/doc:field[@name='value']">
	                                   <ms:languageInfo>
	                                       <ms:languageId><xsl:value-of select="."/></ms:languageId>
	                                       <ms:languageName><xsl:value-of select="fn:getLangForCode(.)"/></ms:languageName>
	                                   </ms:languageInfo>
	                               </xsl:for-each>
	                           </xsl:when>
                               <xsl:otherwise>
                                   <xsl:value-of select="fn:logMissing('language',$handle)"/>
                               </xsl:otherwise>
	                       </xsl:choose>
                            <xsl:choose>
                                <xsl:when test="$mediaType='audio'">
                                        <ms:audioSizeInfo>
                                                <xsl:call-template name="SizeInfo" />
                                        </ms:audioSizeInfo>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:call-template name="SizeInfo"/>
                                </xsl:otherwise>
                            </xsl:choose>
	                   </xsl:element>
	               </xsl:element>
	           </xsl:if>
	           <xsl:if test="$type='toolService'">
		   <ms:languageDependent>
	               <xsl:choose>
	                   <xsl:when test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceComponentType#ToolServiceInfo']/doc:element[@name='languageDependent']/doc:element/doc:field[@name='value']">
	                       <xsl:value-of select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#ResourceComponentType#ToolServiceInfo']/doc:element[@name='languageDependent']/doc:element/doc:field[@name='value']"/>
	                   </xsl:when>
                       <xsl:otherwise>
                           <xsl:value-of select="fn:logMissing('languageDependent',$handle)"/>
                       </xsl:otherwise>
	               </xsl:choose>
		   </ms:languageDependent>
	           </xsl:if>
	       </xsl:element>
	   </ms:resourceComponentType>
	</xsl:template>

	<xsl:template name="SizeInfo">
		<ms:sizeInfo>
			<ms:size>
				<xsl:choose>
					<xsl:when
						test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#TextInfo#SizeInfo']/doc:element[@name='size']/doc:element/doc:field[@name='value']">
						<xsl:apply-templates select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#TextInfo#SizeInfo']/doc:element[@name='size']"/>
					</xsl:when>
                    <xsl:when
                        test="doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="tokenize(doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value'], ';')[1]"/>
                    </xsl:when>
					<xsl:otherwise>
                        			<xsl:variable name="iJustWantToLog" select="fn:logMissing('size',$handle)" />
						<xsl:value-of select="count(/doc:metadata/doc:element[@name='bundles']/doc:element[@name='bundle']/doc:field[@name='name'][text()='ORIGINAL']/../doc:element[@name='bitstreams']/doc:element[@name='bitstream'])"/>
					</xsl:otherwise>
				</xsl:choose>
			</ms:size>
			<ms:sizeUnit>
				<xsl:choose>
					<xsl:when
						test="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#TextInfo#SizeInfo']/doc:element[@name='sizeUnit']/doc:element/doc:field[@name='value']">
						<xsl:value-of
							select="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#TextInfo#SizeInfo']/doc:element[@name='sizeUnit']/doc:element/doc:field[@name='value']" />
					</xsl:when>
                    <xsl:when
                        test="doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value']">
                        <xsl:value-of select="tokenize(doc:metadata/doc:element[@name='local']/doc:element[@name='size']/doc:element[@name='info']/doc:element/doc:field[@name='value'], ';')[2]"/>
                    </xsl:when>
					<xsl:otherwise>
                        			<xsl:variable name="iJustWantToLog" select="fn:logMissing('size',$handle)" />
						<xsl:text>files</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</ms:sizeUnit>
		</ms:sizeInfo>
	</xsl:template>

	<xsl:template match="/doc:metadata/doc:element[@name='metashare']/doc:element[@name='ResourceInfo#TextInfo#SizeInfo']/doc:element[@name='size']">
		<xsl:choose>
			<xsl:when test="../doc:element[@name='sizeUnitMultiplier']/doc:element/doc:field[@name='value']='kilo'">
					<xsl:value-of select="./doc:element/doc:field[@name='value']*1000" />
			</xsl:when>
			<xsl:when test="../doc:element[@name='sizeUnitMultiplier']/doc:element/doc:field[@name='value']='hundred'">
					<xsl:value-of select="./doc:element/doc:field[@name='value']*100" />
			</xsl:when>
			<xsl:when test="../doc:element[@name='sizeUnitMultiplier']/doc:element/doc:field[@name='value']='mega' or ../doc:element[@name='sizeUnitMultiplier']/doc:element/doc:field[@name='value']='million'">
					<xsl:value-of select="format-number(./doc:element/doc:field[@name='value']*1000000,'0')" />
			</xsl:when>
			<xsl:when test="../doc:element[@name='sizeUnitMultiplier']/doc:element/doc:field[@name='value']='tera'">
					<xsl:value-of select="format-number(./doc:element/doc:field[@name='value']*1000000000000,'0')" />
			</xsl:when>
			<xsl:otherwise>
					<xsl:value-of select="./doc:element/doc:field[@name='value']" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
