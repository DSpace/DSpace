<?xml version="1.0" encoding="UTF-8" ?>
<!--
 
 
 The contents of this file are subject to the license and copyright
 detailed in the LICENSE and NOTICE files at the root of the source
 tree and available online at
 
 http://www.dspace.org/license/
 Developed by DSpace @ Lyncode <dspace@lyncode.com>
 
 > http://www.openarchives.org/OAI/2.0/oai_cerif.xsd
 
 Global namespace:
 	oai_cerif		openaire tag
 	dc				used in oai_cerif:Publication
 	xsi				used in oai_cerif:Publication
 	
 Local Namespace:
 	oai_cerif:Publication	oai:Type xmlns:oai="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"
 	oai_cerif:Publication	oai:Type scheme="https://w3id.org/cerif/vocab/OrganisationTypes"
 -->
<xsl:stylesheet
	xmlns:oai_cerif="https://www.openaire.eu/cerif-profile/1.1/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:doc="http://www.lyncode.com/xoai"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/ http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd https://www.openaire.eu/cerif-profile/1.1/ https://www.openaire.eu/schema/cris/1.1/openaire-cerif-profile.xsd"
    version="1.0">
    <xsl:output omit-xml-declaration="yes" method="xml" indent="yes" />

	<xsl:variable name="placeholder">#PLACEHOLDER_PARENT_METADATA_VALUE#</xsl:variable>

	<!-- Date format -->
	<xsl:template name="formatdate">
		<xsl:param name="datestr" />
		<xsl:choose>
			<xsl:when test="translate(., '123456789', '000000000') = '0000-00-00'">
			    <!-- result if valid -->
			    <xsl:value-of select="$datestr"/>
			</xsl:when>		
			<xsl:otherwise>
				<xsl:variable name="year"  select="substring-after(substring-after($datestr,'-'),'-')" />
				<xsl:variable name="day" select="substring-before($datestr,'-')" />
				<xsl:variable name="month"   select="substring-before(substring-after($datestr,'-'),'-')" />
				<xsl:value-of select="concat($year,'-',$month,'-',$day)"/>			
			</xsl:otherwise>		
		</xsl:choose>
	</xsl:template>
	    		
    <!-- transate dc.type to Type xmlns="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types" -->
    <xsl:template name="oai_publicationtype">
        <xsl:param name="type" select="other"/>
        <xsl:choose>
		<xsl:when test="$type='Publications'">Publications</xsl:when>
        </xsl:choose>
    </xsl:template>
    
    <xsl:template name="oai_producttype">
        <xsl:param name="type" select="other"/>
        <xsl:choose>
		<xsl:when test="$type='Products'">Products</xsl:when>
	</xsl:choose>
  	</xsl:template>
  	
  	 <xsl:template name="oai_patenttype">
        <xsl:param name="type" select="other"/>
        <xsl:choose>
	        <xsl:when test="$type='Patents'">Patents</xsl:when>
	   	</xsl:choose>
  	</xsl:template>

  	 <xsl:template name="oai_eventtype">
        <xsl:param name="type" select="other"/>
        <xsl:choose>
			<xsl:when test="$type='workshop' or $type='Workshop'">https://w3id.org/cerif/vocab/EventTypes#Workshop</xsl:when>
			<xsl:otherwise>https://w3id.org/cerif/vocab/EventTypes#Conference</xsl:otherwise>
	   	</xsl:choose>
  	</xsl:template>
  	  	
    <!-- translate ou.type to Type xmlns="https://w3id.org/cerif/vocab/OrganisationTypes" -->
    <xsl:template name="oai_outype">
        <xsl:param name="type" select="'Academic Institute'"/>
        <xsl:choose>
            
            <!-- An academic institution is an educational institution dedicated to education and research, which grants academic degrees. -->
            <xsl:when test="$type='Academic Institute'">https://w3id.org/cerif/vocab/OrganisationTypes#AcademicInstitute</xsl:when>
            <!-- A university is an institution of higher education and research, which grants academic degrees in a variety of subjects. A university is a corporation that provides both undergraduate education and postgraduate education. -->
            <xsl:when test="$type='University'">https://w3id.org/cerif/vocab/OrganisationTypes#University</xsl:when>
    		<!-- The term "university college" is used in a number of countries to denote college institutions that provide tertiary education but do not have full or independent university status. A university college is often part of a larger university. The precise usage varies from country to country.. -->
            <xsl:when test="$type='University College'">https://w3id.org/cerif/vocab/OrganisationTypes#UniversityCollege</xsl:when>
    		<!-- A research institute is an establishment endowed for doing research. Research institutes may specialize in basic research or may be oriented to applied research. -->
    		<xsl:when test="$type='Research Institute'">https://w3id.org/cerif/vocab/OrganisationTypes#ResearchInstitute</xsl:when>
    		<!-- A strategic research institute's core mission is to provide analyses that respond to the needs of decision-makers. -->
    		<xsl:when test="$type='Strategic Research Insitute'">https://w3id.org/cerif/vocab/OrganisationTypes#StrategicResearchInsitute</xsl:when>
    		<!-- A company is a form of business organization. In the United States, a company is a corporation—or, less commonly, an association, partnership, or union—that carries on an industrial enterprise." Generally, a company may be a "corporation, partnership, association, joint-stock company, trust, fund, or organized group of persons, whether incorporated or not, and (in an official capacity) any receiver, trustee in bankruptcy, or similar official, or liquidating agent, for any of the foregoing." In English law, and therefore in the Commonwealth realms, a company is a form of body corporate or corporation, generally registered under the Companies Acts or similar legislation. It does not include a partnership or any other unincorporated group of persons. -->
    		<xsl:when test="$type='Company'">https://w3id.org/cerif/vocab/OrganisationTypes#Company</xsl:when>
    		<!-- Small and medium enterprises (also SMEs, small and medium businesses, SMBs, and variations thereof) are companies whose headcount or turnover falls below certain limits. EU Member States traditionally have their own definition of what constitutes an SME, for example the traditional definition in Germany had a limit of 250 employees, while, for example, in Belgium it could have been 100. But now the EU has started to standardize the concept. Its current definition categorizes companies with fewer than 10 employees as "micro", those with fewer than 50 employees as "small", and those with fewer than 250 as "medium". -->
    		<xsl:when test="$type='SME'">https://w3id.org/cerif/vocab/OrganisationTypes#SME</xsl:when>
    		<!-- A government is the organization, or agency through which a political unit exercises its authority, controls and administers public policy, and directs and controls the actions of its members or subjects. -->
    		<xsl:when test="$type='Government'">https://w3id.org/cerif/vocab/OrganisationTypes#Government</xsl:when>
    		<!-- Higher education or post-secondary education refers to a level of education that is provided at academies, universities, colleges, seminaries, institutes of technology, and certain other collegiate- level institutions, such as vocational schools, trade schools, and career colleges, that award academic degrees or professional certifications. -->
    		<xsl:when test="$type='Higher Education'">https://w3id.org/cerif/vocab/OrganisationTypes#HigherEducation</xsl:when>
    		<!-- An organization that is incorporated under state law and whose purpose is not to make profit, but rather to further a charitable, civic, scientific, or other lawful purpose. -->
    		<xsl:when test="$type='Private non-profit'">https://w3id.org/cerif/vocab/OrganisationTypes#Privatenon-profit</xsl:when>
    		<!-- An intergovernmental organization, sometimes rendered as an international governmental organization and both abbreviated as IGO, is an organization composed primarily of sovereign states (referred to as member states), or of other intergovernmental organizations. Intergovernmental organizations are often called international organizations, although that term may also include international nongovernmental organization such as international non-profit organizations or multinational corporations. -->
    		<xsl:when test="$type='Intergovernmental'">https://w3id.org/cerif/vocab/OrganisationTypes#Intergovernmental</xsl:when>
    		<!-- A charitable organization is a type of non-profit organization (NPO). It differs from other types of NPOs in that it centers on philanthropic goals (e.g. charitable, educational, religious, or other activities serving the public interest or common good). The legal definition of charitable organization (and of Charity) varies according to the country and in some instances the region of the country in which the charitable organization operates. The regulation, tax treatment, and the way in which charity law affects charitable organizations also varies. -->
    		<xsl:when test="$type='Charity'">https://w3id.org/cerif/vocab/OrganisationTypes#Charity</xsl:when>
    		<!-- Hospitals, trusts and other bodies receiving funding from central governement through the national insurance scheme. -->
    		<xsl:when test="$type='National Health Service'">https://w3id.org/cerif/vocab/OrganisationTypes#NationalHealthService</xsl:when>
    	</xsl:choose>
    </xsl:template>
    
    <!-- translate funding.type to Type xmlns="https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types" -->
    <xsl:template name="oai_fundingtype">
        <xsl:param name="type" select="'Academic Institute'"/>
        <xsl:choose>
    		<!-- Funding Programme (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#FundingProgramme): A funding programme or a similar scheme that funds some number of proposals. -->
    		<xsl:when test="$type='Funding Programme'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#FundingProgramme</xsl:when>
    		<!-- Call (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Call): Call for proposals: a specific campaign for the funder to solicit proposals from interested researchers and institutions -->
    		<xsl:when test="$type='Call'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Call</xsl:when>
    		<!-- Tender (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Tender): Tender for services or deliveries: a specific campaign for the funder to solicit offers for services or deliveries. -->
    		<xsl:when test="$type='Tender'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Tender</xsl:when>
    		<!-- Gift (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Gift): A donation connected with specific terms and conditions. -->
    		<xsl:when test="$type='Gift'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Gift</xsl:when>
    		<!-- Internal Funding (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#InternalFunding): Internal funds used to amend or replace external funding. -->
    		<xsl:when test="$type='Internal Funding'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#InternalFunding</xsl:when>
    		<!-- Contract (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Contract) -->
    		<xsl:when test="$type='Contract'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Contract</xsl:when>
    		<!-- Award (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Award): -->
    		<xsl:when test="$type='Award'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Award</xsl:when>
    		<!-- Grant (https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Grant): -->
    		<xsl:when test="$type='Grant'">https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#Grant</xsl:when>
    	</xsl:choose>
    </xsl:template>

  	 <xsl:template name="oai_accessrights">
        <xsl:param name="rights" select="other"/>
        <xsl:choose>
			<xsl:when test="contains($rights, 'c_f1cf')">http://purl.org/coar/access_right/c_f1cf</xsl:when>
			<xsl:otherwise><xsl:value-of select="$rights" /></xsl:otherwise>
	   	</xsl:choose>
  	</xsl:template>
  	
  	<xsl:template name="oai_dateembargoed">
        <xsl:param name="rights" select="other"/>
        <xsl:choose>
			<xsl:when test="contains($rights, 'c_f1cf')"><xsl:value-of select="substring($rights, 42, 10)" /></xsl:when>
			<xsl:otherwise></xsl:otherwise>
	   	</xsl:choose>
  	</xsl:template>
	<!--  CHOOSER RULES -->
	
	<!-- 
		item_chooser: Template that handle publication using an item selected among publications, products, patents. 
	
    	Example of parameters:
	    	selector
	 -->
	<xsl:template name="item_chooser" match="/">
		<xsl:param name="selector" />
		
		<xsl:for-each select="$selector">
			
			<!-- check type (is a publication or a product or ...) -->
        	<xsl:variable name="typeCerifEntityType">
                <xsl:value-of select="doc:metadata/doc:element[@name='item']/doc:element[@name='cerifentitytype']/doc:element/doc:field[@name='value']" />
            </xsl:variable>
            
            <!-- check product -->
            <xsl:variable name="type_product"><xsl:call-template name="oai_producttype"><xsl:with-param name="type" select="$typeCerifEntityType" /></xsl:call-template></xsl:variable>
            <!-- check patent  -->
            <xsl:variable name="type_patent"><xsl:call-template name="oai_patenttype"><xsl:with-param name="type" select="$typeCerifEntityType" /></xsl:call-template></xsl:variable>
            <!-- check publication -->
            <xsl:variable name="type_publication"><xsl:call-template name="oai_publicationtype"><xsl:with-param name="type" select="$typeCerifEntityType" /></xsl:call-template></xsl:variable>
            
            <xsl:choose>
            	<xsl:when test="$type_product != ''">
            		<xsl:call-template name="product">
		        		<xsl:with-param name="selector" select="doc:metadata" />
		        	</xsl:call-template>
            	</xsl:when>
            	<xsl:when test="$type_patent != ''">
					<xsl:call-template name="patent">
		        		<xsl:with-param name="selector" select="doc:metadata" />
		        	</xsl:call-template>
            	</xsl:when>
            	<xsl:when test="$type_publication != ''">
            		<!-- [Note] $type_publication is always not empty.  -->
            		<xsl:call-template name="publication">
		        		<xsl:with-param name="selector" select="doc:metadata" />
		        	</xsl:call-template>
            	</xsl:when>
            </xsl:choose>
            
		</xsl:for-each>
	</xsl:template>
	    
    <!--	
    	ou: Template that handle Organization Unit.
    -->
	<xsl:template name="ou" match="/">
		<xsl:param name="selector" />
		<xsl:param name="ou_id" />

		<xsl:choose>
		<xsl:when test="$ou_id!=''">		
		<xsl:for-each select="$selector">
			<oai_cerif:OrgUnit id="{$ou_id}">
				<xsl:for-each select="doc:element[@name='crisou']/doc:element[@name='type']/doc:element/doc:element">
				<xsl:variable name="ou_type">
					<xsl:value-of select="doc:field[@name='value']/text()"></xsl:value-of>
				</xsl:variable>
				<oai_cerif:Type scheme="https://w3id.org/cerif/vocab/OrganisationTypes">
            		<xsl:call-template name="oai_outype"><xsl:with-param name="type" select="$ou_type" /></xsl:call-template>
            	</oai_cerif:Type>
            	</xsl:for-each>
            	
            	<xsl:for-each select="doc:element[@name='crisou']/doc:element[@name='acronym']/doc:element/doc:element">
					<oai_cerif:Acronym><xsl:value-of select="doc:field[@name='value']/text()"></xsl:value-of></oai_cerif:Acronym>
            	</xsl:for-each>
            	
            	<xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:element">
					<oai_cerif:Name xml:lang="en"><xsl:value-of select="doc:field[@name='value']" /></oai_cerif:Name>
				</xsl:for-each>
			
				<xsl:variable name="identifiertypeid" select="doc:element[@name='ouidentifier']/doc:element[@name='ouidentifiertypeid']/doc:element/doc:element/doc:field[@name='value']" />
				<xsl:variable name="identifierid" select="doc:element[@name='ouidentifier']/doc:element[@name='ouidentifierid']/doc:element/doc:element/doc:field[@name='value']" />
				<xsl:if test="$identifiertypeid">
					<oai_cerif:Identifier type="{$identifiertypeid}"><xsl:value-of select="$identifierid"/></oai_cerif:Identifier>
				</xsl:if>
            	<xsl:for-each select="doc:element[@name='crisou']/doc:element[@name='crossrefid']/doc:element/doc:element">
					<oai_cerif:Identifier type=""><xsl:value-of select="doc:field[@name='value']" /></oai_cerif:Identifier>
				</xsl:for-each>
				            	
            	<xsl:for-each select="doc:element[@name='crisou']/doc:element[@name='email']/doc:element/doc:element">
					<oai_cerif:ElectronicAddress><xsl:value-of select="doc:field[@name='value']/text()"></xsl:value-of></oai_cerif:ElectronicAddress>
            	</xsl:for-each>
            	
				<xsl:for-each select="doc:element[@name='crisou']/doc:element[@name='parentorgunit']/doc:element/doc:element/doc:element[@name='value']">				
					<xsl:variable name="parentorgunit_id">
						<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
					</xsl:variable>
			    	<oai_cerif:PartOf>
             		<xsl:choose>
             		<xsl:when test="$parentorgunit_id!=''">
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$parentorgunit_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="ou_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
			    	</oai_cerif:PartOf>
				</xsl:for-each>
			</oai_cerif:OrgUnit>			
		</xsl:for-each>
		</xsl:when>
		<xsl:otherwise>
			<oai_cerif:OrgUnit>
				<xsl:for-each select="$selector">
					<oai_cerif:Name xml:lang="en">
						<xsl:value-of select="." />
					</oai_cerif:Name>
				</xsl:for-each>
			</oai_cerif:OrgUnit>
		</xsl:otherwise>
		</xsl:choose>		
    </xsl:template>

    <!--	
    	person: Template that handle Person.
    -->
    <xsl:template name="person" match="/">
    	<xsl:param name="selector" />
    	<xsl:param name="person_id" />

    	<xsl:choose>
		<xsl:when test="$person_id!=''">    	
    	<xsl:for-each select="$selector">
	    <oai_cerif:Person id="{$person_id}"> 
	        <oai_cerif:PersonName>
          		<xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:element">
          		<xsl:choose>
					<xsl:when test="contains(./doc:field[@name='value'], ',')">
						<oai_cerif:FamilyNames><xsl:value-of select="substring-before(., ',')" /></oai_cerif:FamilyNames>
						<oai_cerif:FirstNames><xsl:value-of select="substring-after(., ', ')" /></oai_cerif:FirstNames>
					</xsl:when>
					<xsl:otherwise>
						<oai_cerif:FamilyNames><xsl:value-of select="./doc:field[@name='value']" /></oai_cerif:FamilyNames>					
					</xsl:otherwise>
				</xsl:choose>
           		</xsl:for-each>	        
	    	</oai_cerif:PersonName>
	    	
           	<xsl:for-each select="doc:element[@name='crisrp']/doc:element[@name='orcid']/doc:element/doc:element">
               	<oai_cerif:ORCID><xsl:value-of select="doc:field[@name='value']" /></oai_cerif:ORCID>
           	</xsl:for-each>
           	
           	<xsl:for-each select="doc:element[@name='crisrp']/doc:element[@name='authorid']/doc:element/doc:element">
               	<oai_cerif:ResearcherID><xsl:value-of select="doc:field[@name='value']" /></oai_cerif:ResearcherID>
           	</xsl:for-each>
           	
           	<xsl:for-each select="doc:element[@name='crisrp']/doc:element[@name='scopusid']/doc:element/doc:element">
               	<oai_cerif:ScopusAuthorID><xsl:value-of select="doc:field[@name='value']" /></oai_cerif:ScopusAuthorID>
           	</xsl:for-each>
           	
           	<xsl:for-each select="doc:element[@name='crisrp']/doc:element[@name='email']/doc:element">
               	<oai_cerif:ElectronicAddress><xsl:value-of select="doc:field[@name='value']" /></oai_cerif:ElectronicAddress>
           	</xsl:for-each>

			<xsl:variable name="affiliationstartdate" select="doc:element[@name='affiliation']/doc:element[@name='affiliationstartdate']/doc:element//doc:field[@name='value']"/>
			<xsl:variable name="affiliationenddate" select="doc:element[@name='affiliation']/doc:element[@name='affiliationenddate']/doc:element//doc:field[@name='value']"/>			
						                    
		    <!-- oai_cerif:Affiliation [START] -->
		    <xsl:choose>
		    <xsl:when test="doc:element[@name='affiliation']/doc:element[@name='affiliationorgunit']/doc:element/doc:element">
				<xsl:for-each select="doc:element[@name='affiliation']/doc:element[@name='affiliationorgunit']/doc:element/doc:element">
		        	<xsl:variable name="counter" select="position()"/>				
					<xsl:variable name="affiliationorgunit_id">
	             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>

						<oai_cerif:Affiliation>
						<xsl:if
							test="$affiliationstartdate[$counter] != '' and $affiliationstartdate[$counter]!=$placeholder ">
							<xsl:attribute name="startDate">		
								<xsl:call-template name="formatdate">
									<xsl:with-param name="datestr" select="$affiliationstartdate[$counter]" />
								</xsl:call-template>
							</xsl:attribute>
						</xsl:if>
						<xsl:if
							test="$affiliationenddate[$counter] != '' and $affiliationenddate[$counter]!=$placeholder ">
							<xsl:attribute name="endDate">
								<xsl:call-template name="formatdate">
									<xsl:with-param name="datestr" select="$affiliationenddate[$counter]" />
								</xsl:call-template>							
							</xsl:attribute>
						</xsl:if>
			        	<!-- only value with relation equal to author uuid will be processed -->
						<xsl:call-template name="ou">
							<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
							<xsl:with-param name="ou_id" select="$affiliationorgunit_id" />
						</xsl:call-template>
	            	</oai_cerif:Affiliation>
		      	</xsl:for-each>
	      	</xsl:when>
	      	<xsl:otherwise>
				<xsl:for-each select="doc:element[@name='crisrp']/doc:element[@name='dept']/doc:element/doc:element">
					<xsl:variable name="affiliationorgunit_id">
	             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
			        <oai_cerif:Affiliation>
			        	<!-- only value with relation equal to author uuid will be processed -->
						<xsl:call-template name="ou">
							<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
							<xsl:with-param name="ou_id" select="$affiliationorgunit_id" />
						</xsl:call-template>
	            	</oai_cerif:Affiliation>
		      	</xsl:for-each>	      	
	      	</xsl:otherwise>
	      	</xsl:choose>
	        <!-- oai_cerif:Affiliation [END] -->
		</oai_cerif:Person>
		</xsl:for-each>
		</xsl:when>
		<xsl:otherwise>
		<oai_cerif:Person/>
		</xsl:otherwise>
		</xsl:choose>		
    </xsl:template>
    
    <!--	
    	project: Template that handle Project.
    -->
	<xsl:template name="project" match="/">
		<xsl:param name="selector" />
		<xsl:param name="project_id" />
        
        <xsl:choose>
		<xsl:when test="$project_id!=''">            
        <xsl:for-each select="$selector">
        <oai_cerif:Project id="{$project_id}">
	        
			<xsl:variable name="type_funding"><xsl:call-template name="oai_fundingtype"><xsl:with-param name="type" select="doc:element[@name='crispj']/doc:element[@name='grant']/doc:element[@name='type']/doc:element/doc:field[@name='value']" /></xsl:call-template></xsl:variable>
			<xsl:variable name="currency_funding"><xsl:value-of select="doc:element[@name='crispj']/doc:element[@name='grant']/doc:element[@name='currency']/doc:element/doc:field[@name='value']" /></xsl:variable>
			<xsl:variable name="amount_funding"><xsl:value-of select="doc:element[@name='crispj']/doc:element[@name='grant']/doc:element[@name='amount']/doc:element/doc:field[@name='value']" /></xsl:variable>
			<xsl:variable name="identifier_funding"><xsl:value-of select="doc:element[@name='crispj']/doc:element[@name='grant']/doc:element[@name='identifier']/doc:element/doc:field[@name='value']" /></xsl:variable>
	        	        	
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='acronym']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Acronym><xsl:value-of select="." /></oai_cerif:Acronym>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Title xml:lang="en"><xsl:value-of select="." /></oai_cerif:Title>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='date']/doc:element[@name='start']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:StartDate><xsl:call-template name="formatdate"><xsl:with-param name="datestr" select="." /></xsl:call-template></oai_cerif:StartDate>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='date']/doc:element[@name='end']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:EndDate><xsl:call-template name="formatdate"><xsl:with-param name="datestr" select="." /></xsl:call-template></oai_cerif:EndDate>
	        </xsl:for-each>
	        
	        <xsl:if test="(doc:element[@name='crispj']/doc:element[@name='coordinator']/doc:element/doc:element/doc:field[@name='value']!='' or doc:element[@name='crispj']/doc:element[@name='partnerou']/doc:element/doc:element/doc:field[@name='value']!='' or doc:element[@name='crispj']/doc:element[@name='organization']/doc:element/doc:element/doc:field[@name='value']!='')">
	        <!-- Consortium [START] -->
	        <oai_cerif:Consortium>
	        <!-- Coordinator -->
            <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='coordinator']/doc:element/doc:element">
             	<xsl:variable name="coordinator_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	<oai_cerif:Coordinator>
            		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>
					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$coordinator_id" />
					</xsl:call-template>
           		</oai_cerif:Coordinator>
            </xsl:for-each>
            <!-- Partner -->
            <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='partnerou']/doc:element/doc:element">
             	<xsl:variable name="partner_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	<oai_cerif:Partner>
            		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>
					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$partner_id" />
					</xsl:call-template>
           		</oai_cerif:Partner>
            </xsl:for-each>
            <!-- Contractor -->
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='contractorou']/doc:element/doc:element">
             	<xsl:variable name="organization_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	<oai_cerif:Contractor>
            		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>
					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$organization_id" />
					</xsl:call-template>
           		</oai_cerif:Contractor>
            </xsl:for-each>
            <!-- Member -->
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='memberou']/doc:element/doc:element">
             	<xsl:variable name="organization_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	<oai_cerif:Member>
            		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>
					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$organization_id" />
					</xsl:call-template>
           		</oai_cerif:Member>
            </xsl:for-each>            
	        </oai_cerif:Consortium>
	        <!-- Consortium [END] -->
	        </xsl:if>
	        
	       	<xsl:if test="(doc:element[@name='crispj']/doc:element[@name='investigator']/doc:element/doc:element/doc:field[@name='value']!='' or doc:element[@name='crispj']/doc:element[@name='coinvestigators']/doc:element/doc:element/doc:field[@name='value']!='')">
	        <!-- Team [START] -->
	        <oai_cerif:Team>
	        <!-- PrincipalInvestigator -->
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='investigator']/doc:element/doc:element">
             	<xsl:variable name="principalinvestigator_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	<oai_cerif:PrincipalInvestigator>
            		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>
					<xsl:call-template name="person">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="person_id" select="$principalinvestigator_id" />
					</xsl:call-template>
           		</oai_cerif:PrincipalInvestigator>
            </xsl:for-each>
            <!-- Member -->
            <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='coinvestigators']/doc:element/doc:element">
             	<xsl:variable name="member_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	<oai_cerif:Member>
					<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>            	
					<xsl:call-template name="person">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="person_id" select="$member_id" />
					</xsl:call-template>
           		</oai_cerif:Member>
            </xsl:for-each>
	        </oai_cerif:Team>
	        <!-- Team [END] -->
	        </xsl:if>
	        
	        <xsl:if test="(doc:element[@name='crispj']/doc:element[@name='funder']/doc:element/doc:element/doc:field[@name='value']!='' or doc:element[@name='crispj']/doc:element[@name='fundingProgram']/doc:element/doc:element/doc:field[@name='value']!='')">
	        <!-- Funded [START] -->
	        <oai_cerif:Funded>	        
        	<xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='funder']/doc:element/doc:element">
        		<xsl:variable name="funderby_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	<oai_cerif:By>
            		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>
					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$funderby_id" />
					</xsl:call-template>
           		</oai_cerif:By>
        	</xsl:for-each>
        	<xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='funding']/doc:element/doc:element">
        		<oai_cerif:As xmlns:oafunding="https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types">
        			<oai_cerif:Funding>
						<xsl:choose>
							<xsl:when test="$type_funding!=''">
								<oafunding:Type><xsl:value-of select="$type_funding" /></oafunding:Type>
							</xsl:when>		
							<xsl:otherwise>
								<oafunding:Type>https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#InternalFunding</oafunding:Type>
							</xsl:otherwise>
						</xsl:choose>
						<xsl:if test="$amount_funding!=''">
							<oai_cerif:Amount currency="{$currency_funding}"><xsl:value-of select="$amount_funding" /></oai_cerif:Amount>
						</xsl:if>																			        				
	        			<xsl:if test="$identifier_funding!=''">
							<oai_cerif:Identifier type="https://w3id.org/cerif/vocab/IdentifierTypes#ProjectReference"><xsl:value-of select="$identifier_funding" /></oai_cerif:Identifier>
						</xsl:if>
						<oai_cerif:PartOf>
							<oai_cerif:Funding>
								<oafunding:Type>https://www.openaire.eu/cerif-profile/vocab/OpenAIRE_Funding_Types#FundingProgramme</oafunding:Type>
								<oai_cerif:Name xml:lang="en"><xsl:value-of select="./doc:field[@name='value']" /></oai_cerif:Name>
							</oai_cerif:Funding>	
						</oai_cerif:PartOf>        				
        			</oai_cerif:Funding>	
        		</oai_cerif:As>
        	</xsl:for-each>
	        </oai_cerif:Funded>
	        <!-- Funded [END] -->
	        </xsl:if>
	        	        
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='description']/doc:element[@name='keywords']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Keyword xml:lang="en"><xsl:value-of select="." /></oai_cerif:Keyword>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Abstract xml:lang="en"><xsl:value-of select="." /></oai_cerif:Abstract>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='status']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Status scheme="https://cerif.eurocris.org/model#Project_Classification"><xsl:value-of select="." /></oai_cerif:Status>
	        </xsl:for-each>
	        
	        <!--  REVIEW METADATA [START]: uses, oamandate -->
	        <xsl:for-each select="doc:element[@name='crispj']/doc:element[@name='relation']/doc:element[@name='equipment']/doc:element">
	        	<xsl:variable name="equipment_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
	        	<oai_cerif:Uses>
					<!-- <oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName> -->            	
					<xsl:call-template name="equipment">
						<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
						<xsl:with-param name="equipment_id" select="$equipment_id" />
					</xsl:call-template>
	        	</oai_cerif:Uses>
	        </xsl:for-each>
	        
	        <xsl:variable name="typeid" select="doc:element[@name='crispj']/doc:element[@name='oamandate']/doc:element/doc:element/doc:field[@name='value']" />
			<xsl:if test="$typeid">
				<oai_cerif:OAMandate mandated="{$typeid}" />
			</xsl:if>
	        <!--  REVIEW METADATA [END] -->
        </oai_cerif:Project>
        </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
        <oai_cerif:Project>
	        <xsl:for-each select="$selector">
	        	<oai_cerif:Title xml:lang="en"><xsl:value-of select="." /></oai_cerif:Title>
	        </xsl:for-each>
		</oai_cerif:Project>
        </xsl:otherwise>
        </xsl:choose>        
    </xsl:template>
    
    <!--	
    	event: Template that handle Event.
    -->
	<xsl:template name="events" match="/">
		<xsl:param name="selector" />
		<xsl:param name="event_id" />

        <xsl:choose>
        <xsl:when test="$event_id!=''">            
        <xsl:for-each select="$selector">
        <oai_cerif:Event id="{$event_id}">
                	
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='type']/doc:element/doc:element/doc:field[@name='value']">
	        	<xsl:variable name="type_event"><xsl:call-template name="oai_eventtype"><xsl:with-param name="type" select="." /></xsl:call-template></xsl:variable>
	        	<oai_cerif:Type scheme="https://w3id.org/cerif/vocab/EventTypes"><xsl:value-of select="$type_event" /></oai_cerif:Type>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='acronym']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Acronym><xsl:value-of select="." /></oai_cerif:Acronym>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Name xml:lang="en"><xsl:value-of select="." /></oai_cerif:Name>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='place']/doc:element[@name='location']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Place><xsl:value-of select="." /></oai_cerif:Place>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='place']/doc:element[@name='country']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Country><xsl:value-of select="." /></oai_cerif:Country>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='date']/doc:element[@name='start']/doc:element/doc:field[@name='value']">
				<oai_cerif:StartDate><xsl:call-template name="formatdate"><xsl:with-param name="datestr" select="." /></xsl:call-template></oai_cerif:StartDate>				        
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='date']/doc:element[@name='end']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:EndDate><xsl:call-template name="formatdate"><xsl:with-param name="datestr" select="." /></xsl:call-template></oai_cerif:EndDate>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='description']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Description xml:lang="en"><xsl:value-of select="." /></oai_cerif:Description>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='description']/doc:element[@name='keywords']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Keyword xml:lang="en"><xsl:value-of select="." /></oai_cerif:Keyword>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='organizerou']/doc:element/doc:element[@name='value']">
	        	<oai_cerif:Organizer>
					<xsl:variable name="organizer_id">
						<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
					</xsl:variable>
             		<xsl:choose>
             		<xsl:when test="$organizer_id!=''">
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$organizer_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="ou_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:Organizer>
	        </xsl:for-each>
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='organizerpj']/doc:element/doc:element/doc:element[@name='value']">
	        	<oai_cerif:Organizer>
					<xsl:variable name="organizer_id">
						<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
					</xsl:variable>
             		<xsl:choose>
             		<xsl:when test="$organizer_id!=''">
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="project_id" select="$organizer_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="project_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:Organizer>
	        </xsl:for-each>	        
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='sponsorou']/doc:element/doc:element/doc:element[@name='value']">
	        	<oai_cerif:Sponsor>
					<xsl:variable name="sponsor_id">
						<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
					</xsl:variable>
             		<xsl:choose>
             		<xsl:when test="$sponsor_id!=''">
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$sponsor_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="ou_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:Sponsor>
	        </xsl:for-each>
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='sponsorpj']/doc:element/doc:element/doc:element[@name='value']">
	        	<oai_cerif:Sponsor>
					<xsl:variable name="sponsor_id">
						<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
					</xsl:variable>
             		<xsl:choose>
             		<xsl:when test="$sponsor_id!=''">
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="project_id" select="$sponsor_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="project_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:Sponsor>
	        </xsl:for-each>	        
	        
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='partnerou']/doc:element/doc:element/doc:element[@name='value']">
	        	<oai_cerif:Partner>
					<xsl:variable name="partner_id">
						<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
					</xsl:variable>
             		<xsl:choose>
             		<xsl:when test="$partner_id!=''">
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$partner_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="ou_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:Partner>
	        </xsl:for-each>
	        <xsl:for-each select="doc:element[@name='crisevent']/doc:element[@name='partnerpj']/doc:element/doc:element/doc:element[@name='value']">
	        	<oai_cerif:Partner>
					<xsl:variable name="partner_id">
						<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
					</xsl:variable>
             		<xsl:choose>
             		<xsl:when test="$partner_id!=''">
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="project_id" select="$partner_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="project_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:Partner>
	        </xsl:for-each>
        </oai_cerif:Event>
        </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
	        <oai_cerif:Event>
		        <xsl:for-each select="$selector">
		        	<oai_cerif:Name xml:lang="en"><xsl:value-of select="." /></oai_cerif:Name>
		        </xsl:for-each>
	        </oai_cerif:Event>
        </xsl:otherwise>
        </xsl:choose>        
	</xsl:template>
	
	 <!--	
    	equipment: Template that handle Equipment.
    -->
	<xsl:template name="equipment" match="/">
		<xsl:param name="selector" />
		<xsl:param name="equipment_id" />

        <xsl:choose>
        <xsl:when test="$equipment_id!=''">            
        <xsl:for-each select="$selector">
        <oai_cerif:Equipment id="{$equipment_id}">
        		        
	        <xsl:for-each select="doc:element[@name='crisequipment']/doc:element[@name='acronym']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Acronym><xsl:value-of select="." /></oai_cerif:Acronym>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Name xml:lang="en"><xsl:value-of select="." /></oai_cerif:Name>
	        </xsl:for-each>

	        <xsl:for-each select="doc:element[@name='crisequipment']/doc:element[@name='identifier']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Identifier type="Institution assigned unique equipment identifier"><xsl:value-of select="." /></oai_cerif:Identifier>
	        </xsl:for-each>

	        <xsl:for-each select="doc:element[@name='crisequipment']/doc:element[@name='description']/doc:element/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Description xml:lang="en"><xsl:value-of select="." /></oai_cerif:Description>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='crisequipment']/doc:element[@name='ownerou']/doc:element/doc:element">
	        	<oai_cerif:Owner>
	        		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName> 
	        		<xsl:variable name="owner_id">
             			<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             		</xsl:variable>
                    <xsl:call-template name="ou">
            			<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
            			<xsl:with-param name="ou_id" select="$owner_id" />
            		</xsl:call-template>
	        	</oai_cerif:Owner>
	        </xsl:for-each>
	        <xsl:for-each select="doc:element[@name='crisequipment']/doc:element[@name='ownerrp']/doc:element/doc:element">
	        	<oai_cerif:Owner>
	        		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName> 
	        		<xsl:variable name="owner_id">
             			<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             		</xsl:variable>
                    <xsl:call-template name="person">
            			<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
            			<xsl:with-param name="person_id" select="$owner_id" />
            		</xsl:call-template>
	        	</oai_cerif:Owner>
	        </xsl:for-each>	        	        	        
        </oai_cerif:Equipment>
        </xsl:for-each>
        </xsl:when>
        <xsl:otherwise>
	        <oai_cerif:Equipment>
	        <xsl:for-each select="$selector">
	        	<oai_cerif:Name xml:lang="en"><xsl:value-of select="." /></oai_cerif:Name>
	        </xsl:for-each>
	        </oai_cerif:Equipment>
        </xsl:otherwise>
        </xsl:choose>        
   	</xsl:template>

	<!-- 
		journal: Template that handle publication using a journal
	 -->
	 <xsl:template name="journal" match="/">
		<xsl:param name="selector" />
		<xsl:param name="journal_id" />
		
		<xsl:choose>
        <xsl:when test="$journal_id!=''">
		<xsl:for-each select="$selector">
        <oai_cerif:Publication id="{$journal_id}">
            <oai_cerif:Type xmlns:oai_cerif="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types">http://purl.org/coar/resource_type/c_0640</oai_cerif:Type>
           
    		<!-- oai_cerif:PublishedIn, Title (crisjournals.journalsname) --> 
            <xsl:for-each select="doc:element[@name='crisjournals']/doc:element[@name='journalsname']/doc:element/doc:element/doc:field[@name='value']">
                <oai_cerif:Title xml:lang="en"><xsl:value-of select="." /></oai_cerif:Title>
            </xsl:for-each>
            
            <!-- oai_cerif:PublishedIn, ISSN -->
            <xsl:for-each select="doc:element[@name='crisjournals']/doc:element[@name='journalsissn']/doc:element/doc:element/doc:field[@name='value']">
                <oai_cerif:ISSN><xsl:value-of select="." /></oai_cerif:ISSN>
            </xsl:for-each>
       	</oai_cerif:Publication>
   		</xsl:for-each>
   		</xsl:when>
   		<xsl:otherwise>
	   		<oai_cerif:Publication>
	   		<xsl:for-each select="$selector">
    	   		<oai_cerif:Type xmlns:oai_cerif="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types">http://purl.org/coar/resource_type/c_0640</oai_cerif:Type>
	        	<oai_cerif:Title xml:lang="en"><xsl:value-of select="." /></oai_cerif:Title>
	        </xsl:for-each>
	   		</oai_cerif:Publication>
   		</xsl:otherwise>
   		</xsl:choose>
   	</xsl:template>
   		
    <!--	
    	publication: Template that handle publication using a publication item
    -->
	<xsl:template name="publication" match="/">
		<xsl:param name="selector" />
							
		<xsl:for-each select="$selector">
       	<xsl:variable name="item_prop_id">
            <xsl:value-of select="doc:element[@name='others']/doc:field[@name='handle']" />
        </xsl:variable>
        
        <oai_cerif:Publication id="{$item_prop_id}">
            
            <xsl:for-each select="doc:element[@name='item']/doc:element[@name='openairecristype']/doc:element/doc:field[@name='value']">
	            <oai_cerif:Type xmlns:oai_cerif="https://www.openaire.eu/cerif-profile/vocab/COAR_Publication_Types"><xsl:value-of select="." /></oai_cerif:Type>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
                <oai_cerif:Title xml:lang="en"><xsl:value-of select="." /></oai_cerif:Title>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element[@name='alternative']/doc:element/doc:field[@name='value']">
                <oai_cerif:Subtitle xml:lang="en"><xsl:value-of select="." /></oai_cerif:Subtitle>
            </xsl:for-each>
            
            <!-- oai_cerif:PublishedIn [START] -->
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='publication']/doc:element/doc:element[@name='authority']">
            	<oai_cerif:PublishedIn>
                    <xsl:call-template name="publication">
            			<xsl:with-param name="selector" select="." />
            		</xsl:call-template>
                </oai_cerif:PublishedIn>
            </xsl:for-each>
            <!-- oai_cerif:PublishedIn [END] -->
            
            <!-- oai_cerif:PartOf [START] -->
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartof']/doc:element">
            	<oai_cerif:PartOf>
            	    <xsl:variable name="journals_id">
                        <xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
                    </xsl:variable>
            
                    <oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']/text()" /></oai_cerif:DisplayName>        
            		<!-- by desing dc.relation.ispartof is always a JournalAuthority -->
            		<xsl:call-template name="journal">
            			<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
            			<xsl:with-param name="journal_id" select="$journals_id" />
            		</xsl:call-template>
            	</oai_cerif:PartOf>
           	</xsl:for-each>
            <!-- oai_cerif:PartOf [END] -->
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
            	<xsl:if test=". != ''">
                	<oai_cerif:PublicationDate><xsl:value-of select="." /></oai_cerif:PublicationDate>
                </xsl:if>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='volume']/doc:element/doc:field[@name='value']">
                <oai_cerif:Volume><xsl:value-of select="." /></oai_cerif:Volume>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='issue']/doc:element/doc:field[@name='value']">
                <oai_cerif:Issue><xsl:value-of select="." /></oai_cerif:Issue>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='edition']/doc:element/doc:field[@name='value']">
                <oai_cerif:Edition><xsl:value-of select="." /></oai_cerif:Edition>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='startpage']/doc:element/doc:field[@name='value']">
                <oai_cerif:StartPage><xsl:value-of select="." /></oai_cerif:StartPage>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='endpage']/doc:element/doc:field[@name='value']">
                <oai_cerif:EndPage><xsl:value-of select="." /></oai_cerif:EndPage>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field[@name='value']">
                <oai_cerif:DOI><xsl:value-of select="." /></oai_cerif:DOI>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='others']/doc:field[@name='handle']">
                <oai_cerif:Handle><xsl:value-of select="." /></oai_cerif:Handle>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='pmid']/doc:element/doc:field[@name='value']">
                <oai_cerif:PMCID><xsl:value-of select="." /></oai_cerif:PMCID>
            </xsl:for-each>

			<xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isi']/doc:element/doc:field[@name='value']">
                <oai_cerif:ISI-Number><xsl:value-of select="." /></oai_cerif:ISI-Number>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='scopus']/doc:element/doc:field[@name='value']">
                <oai_cerif:SCP-Number><xsl:value-of select="." /></oai_cerif:SCP-Number>
            </xsl:for-each>

			<xsl:for-each
				select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='isbn']/doc:element/doc:field[@name='value']">
				<xsl:variable name="isbn_plain">
					<xsl:value-of
						select="translate(., 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ():-', '')" />
				</xsl:variable>
				<xsl:variable name="isbn_empty">
					<xsl:value-of
						select="normalize-space(translate($isbn_plain, '1234567890-', ''))" />
				</xsl:variable>
				<xsl:choose>
					<xsl:when
						test="(contains(., 'PRINT') or contains(., 'Print') or contains(., 'print')) and $isbn_empty=''">
						<oai_cerif:ISBN medium="http://issn.org/vocabularies/Medium#Print"><xsl:value-of select="normalize-space($isbn_plain)" /></oai_cerif:ISBN>
					</xsl:when>
					<xsl:when
						test="(contains(., 'ONLINE') or contains(., 'Online') or contains(., 'online')) and $isbn_empty=''">
						<oai_cerif:ISBN medium="http://issn.org/vocabularies/Medium#Online"><xsl:value-of select="normalize-space($isbn_plain)" /></oai_cerif:ISBN>
					</xsl:when>
					<xsl:otherwise>
						<xsl:if test="$isbn_empty=''">
							<oai_cerif:ISBN><xsl:value-of select="normalize-space($isbn_plain)" /></oai_cerif:ISBN>
						</xsl:if>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:for-each>

            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='url']/doc:element/doc:field[@name='value']">
            	<xsl:if test="position() = 1">
                	<oai_cerif:URL><xsl:value-of select="." /></oai_cerif:URL>
                </xsl:if>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
            	<xsl:if test="position() = 1">
                	<oai_cerif:URN><xsl:value-of select="." /></oai_cerif:URN>
                </xsl:if>
            </xsl:for-each>
            
            <xsl:if test="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']!=''">
            <!-- oai_cerif:Authors [START] -->
            <oai_cerif:Authors>
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element">
             	<xsl:variable name="dc_contributor_author_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	    <oai_cerif:Author>
            	    		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']" /></oai_cerif:DisplayName>
							<xsl:call-template name="person">
								<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
								<xsl:with-param name="person_id" select="$dc_contributor_author_id" />
							</xsl:call-template>
					</oai_cerif:Author>           		
            </xsl:for-each>
            </oai_cerif:Authors>
            <!-- oai_cerif:Authors [END] -->
            </xsl:if>
            
            <xsl:if test="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='editor']/doc:element/doc:field[@name='value']!=''">
            <!-- oai_cerif:Editors [START] -->
            <oai_cerif:Editors>
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='editor']/doc:element">
             	<xsl:variable name="dc_contributor_editor_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	    <oai_cerif:Editor>
            	    		<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']" /></oai_cerif:DisplayName>
							<xsl:call-template name="person">
								<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
								<xsl:with-param name="person_id" select="$dc_contributor_editor_id" />
							</xsl:call-template>
					</oai_cerif:Editor>           		
            </xsl:for-each>
            </oai_cerif:Editors>
            <!-- oai_cerif:Editors [END] -->
            </xsl:if>
            
            <xsl:if test="doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']!=''">
            <!-- oai_cerif:Publishers [START] -->
            <oai_cerif:Publishers>
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
             	<xsl:variable name="dc_publisher_id">
             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>             		
           	    	<oai_cerif:Publisher>
						<oai_cerif:DisplayName><xsl:value-of select="." /></oai_cerif:DisplayName>           	    	
						<xsl:call-template name="ou">
							<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
							<xsl:with-param name="ou_id" select="$dc_publisher_id" />
						</xsl:call-template>
					</oai_cerif:Publisher>           		
            </xsl:for-each>
            </oai_cerif:Publishers>
            <!-- oai_cerif:Publishers [END] -->
            </xsl:if>
            
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Keyword xml:lang="en"><xsl:value-of select="." /></oai_cerif:Keyword>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Abstract xml:lang="en"><xsl:value-of select="." /></oai_cerif:Abstract>
	        </xsl:for-each>

	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:OriginatesFrom>	        
		        	<xsl:variable name="project_id">
	             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
             		
             		<xsl:choose>
             		<xsl:when test="$project_id!=''">
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="project_id" select="$project_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="project_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:OriginatesFrom>					
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='conference']/doc:element/doc:field[@name='value']">
				<oai_cerif:PresentedAt>	        
		        	<xsl:variable name="dc_relation_conference_id">
	             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
             		
             		<xsl:choose>
             		<xsl:when test="$dc_relation_conference_id!=''">
 					<xsl:call-template name="events">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="event_id" select="$dc_relation_conference_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="events">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="event_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
				</oai_cerif:PresentedAt>
	       	</xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='item']/doc:element[@name='grantfulltext']/doc:element/doc:field[@name='value']">
	        	<ns4:Access xmlns:ns4="http://purl.org/coar/access_right">
	        		<xsl:variable name="accessright">
								<xsl:choose>
									<xsl:when test="contains(., 'open')">
										<xsl:text>http://purl.org/coar/access_right/c_abf2</xsl:text>
									</xsl:when>
									<xsl:when test="contains(., 'restricted')">
										<xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
									</xsl:when>
									<xsl:when test="contains(., 'embargo')">
						
											<xsl:text>http://purl.org/coar/access_right/c_f1cf/</xsl:text>
											<xsl:value-of select="substring(., 9, 4)" />
											<xsl:text>-</xsl:text>
											<xsl:value-of select="substring(., 13, 2)" />
											<xsl:text>-</xsl:text>
											<xsl:value-of select="substring(., 15, 2)" />
						
									</xsl:when>
									<xsl:when test="contains(., 'reserved')">
										<xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:text>http://purl.org/coar/access_right/c_14cb</xsl:text>
									</xsl:otherwise>
								</xsl:choose>
					</xsl:variable>
	        		<xsl:variable name="accessrightdate">
							<xsl:call-template name="oai_dateembargoed">
	        					<xsl:with-param name="rights" select="$accessright"/>
	        				</xsl:call-template>	             			
             		</xsl:variable>
             		<xsl:if test="$accessrightdate!=''">
		        		<xsl:attribute name="endDate"><xsl:value-of select="$accessrightdate" /></xsl:attribute>
	        		</xsl:if>
	        		<xsl:call-template name="oai_accessrights">
	        				<xsl:with-param name="rights" select="$accessright"/>
	        		</xsl:call-template>
	        	</ns4:Access>
	        </xsl:for-each>

        </oai_cerif:Publication>
        </xsl:for-each>
	</xsl:template>
	
	<!--	
    	product: Template that handle publication using a product item
    -->
	<xsl:template name="product" match="/">
		<xsl:param name="selector" />
							
		<xsl:for-each select="$selector">
       	<xsl:variable name="item_prop_id">
            <xsl:value-of select="doc:element[@name='others']/doc:field[@name='handle']" />
        </xsl:variable>
        
        <oai_cerif:Product id="{$item_prop_id}">
            
            <xsl:for-each select="doc:element[@name='item']/doc:element[@name='openairecristype']/doc:element/doc:field[@name='value']">
	            <oai_cerif:Type xmlns:oai_cerif="https://www.openaire.eu/cerif-profile/vocab/COAR_Product_Types"><xsl:value-of select="." /></oai_cerif:Type>
            </xsl:for-each>            
                        
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
                <oai_cerif:Name xml:lang="en"><xsl:value-of select="." /></oai_cerif:Name>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='version']/doc:element/doc:field[@name='value']">
                <oai_cerif:VersionInfo xml:lang="en"><xsl:value-of select="." /></oai_cerif:VersionInfo>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='ark']/doc:element/doc:field[@name='value']">
                <oai_cerif:ARK><xsl:value-of select="." /></oai_cerif:ARK>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='doi']/doc:element/doc:field[@name='value']">
                <oai_cerif:DOI><xsl:value-of select="." /></oai_cerif:DOI>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='others']/doc:field[@name='handle']">
                <oai_cerif:Handle><xsl:value-of select="." /></oai_cerif:Handle>
            </xsl:for-each>
            
			<xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='url']/doc:element/doc:field[@name='value']">
                <oai_cerif:URL><xsl:value-of select="." /></oai_cerif:URL>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='uri']/doc:element/doc:field[@name='value']">
                <oai_cerif:URN><xsl:value-of select="." /></oai_cerif:URN>
            </xsl:for-each>
            
            <xsl:if test="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']!=''">
            <!-- oai_cerif:Creators [START] -->
            <oai_cerif:Creators>
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element">
             	<xsl:variable name="dc_contributor_author_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	    <oai_cerif:Creator>
            	    	<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']" /></oai_cerif:DisplayName>
							<xsl:call-template name="person">
								<xsl:with-param name="selector" select="./doc:element[@name='authority']" />
								<xsl:with-param name="person_id" select="$dc_contributor_author_id" />
							</xsl:call-template>
					</oai_cerif:Creator>
            </xsl:for-each>
            </oai_cerif:Creators>
            <!-- oai_cerif:Creators [END] -->
            </xsl:if>
                     
			<xsl:if test="doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']!=''">                        
            <!-- oai_cerif:Publishers [START] -->
            <oai_cerif:Publishers>
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
             	<xsl:variable name="dc_publisher_id">
             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
           	    	<oai_cerif:Publisher>
             			<oai_cerif:DisplayName><xsl:value-of select="." /></oai_cerif:DisplayName>           	    	
						<xsl:call-template name="ou">
							<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
							<xsl:with-param name="ou_id" select="$dc_publisher_id" />
						</xsl:call-template>
					</oai_cerif:Publisher>           		
            </xsl:for-each>
            </oai_cerif:Publishers>
            <!-- oai_cerif:Publishers [END] -->
            </xsl:if>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='rights']/doc:element[@name='license']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:License><xsl:value-of select="." /></oai_cerif:License>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Description xml:lang="en"><xsl:value-of select="." /></oai_cerif:Description>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Keyword xml:lang="en"><xsl:value-of select="." /></oai_cerif:Keyword>
	        </xsl:for-each>
            
            <!-- oai_cerif:PartOf [START] -->
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='ispartof']/doc:element/doc:element[@name='value']">
            	<oai_cerif:PartOf>
		        	<xsl:variable name="journals_id">
	             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
             		
             		<xsl:choose>
             		<xsl:when test="$journals_id!=''">
 					<xsl:call-template name="journal">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="journal_id" select="$journals_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="journal">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="journal_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
            	</oai_cerif:PartOf>
           	</xsl:for-each>
            <!-- oai_cerif:PartOf [END] -->

	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:OriginatesFrom>	        
	        		<xsl:variable name="project_id">
             			<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             		</xsl:variable>
             		<!-- <oai_cerif:DisplayName><xsl:value-of select="." /></oai_cerif:DisplayName> -->
             		<xsl:choose>
             		<xsl:when test="$project_id!=''">
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="project_id" select="$project_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="project_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>	        	
	        	</oai_cerif:OriginatesFrom>					
	        </xsl:for-each>

	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='equipment']/doc:element">
	        	<oai_cerif:GeneratedBy>	        
		        	<xsl:variable name="generatedby_id">
	             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
             		
             		<xsl:choose>
             		<xsl:when test="$generatedby_id!=''">
 					<xsl:call-template name="equipment">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="equipment_id" select="$generatedby_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="equipment">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="equipment_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>	        	
	        	</oai_cerif:GeneratedBy>					
	        </xsl:for-each>

	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='conference']/doc:element/doc:element[@name='value']">
				<oai_cerif:PresentedAt>
		        	<xsl:variable name="dc_relation_conference_id">
	             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
             		
             		<xsl:choose>
             		<xsl:when test="$dc_relation_conference_id!=''">
 					<xsl:call-template name="events">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="event_id" select="$dc_relation_conference_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="events">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="event_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	       		</oai_cerif:PresentedAt>				 	        
	       	</xsl:for-each>

	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='references']/doc:element/doc:element[@name='authority']">
	        	<oai_cerif:References>	        
 					<xsl:call-template name="publication">
						<xsl:with-param name="selector" select="." />
					</xsl:call-template>
	        	</oai_cerif:References>		
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='publication']/doc:element/doc:element[@name='authority']">
	        	<oai_cerif:References>	        
 					<xsl:call-template name="publication">
						<xsl:with-param name="selector" select="." />
					</xsl:call-template>
	        	</oai_cerif:References>		
	        </xsl:for-each>
	        	        	       	
	        <xsl:for-each select="doc:element[@name='item']/doc:element[@name='grantfulltext']/doc:element/doc:field[@name='value']">
	        	<ns4:Access xmlns:ns4="http://purl.org/coar/access_right">
	        		<xsl:variable name="accessright">
								<xsl:choose>
									<xsl:when test="contains(., 'open')">
										<xsl:text>http://purl.org/coar/access_right/c_abf2</xsl:text>
									</xsl:when>
									<xsl:when test="contains(., 'restricted')">
										<xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
									</xsl:when>
									<xsl:when test="contains(., 'embargo')">
						
											<xsl:text>http://purl.org/coar/access_right/c_f1cf/</xsl:text>
											<xsl:value-of select="substring(., 9, 4)" />
											<xsl:text>-</xsl:text>
											<xsl:value-of select="substring(., 13, 2)" />
											<xsl:text>-</xsl:text>
											<xsl:value-of select="substring(., 15, 2)" />
						
									</xsl:when>
									<xsl:when test="contains(., 'reserved')">
										<xsl:text>http://purl.org/coar/access_right/c_16ec</xsl:text>
									</xsl:when>
									<xsl:otherwise>
										<xsl:text>http://purl.org/coar/access_right/c_14cb</xsl:text>
									</xsl:otherwise>
								</xsl:choose>
					</xsl:variable>
	        		<xsl:variable name="accessrightdate">
							<xsl:call-template name="oai_dateembargoed">
	        					<xsl:with-param name="rights" select="$accessright"/>
	        				</xsl:call-template>	             			
             		</xsl:variable>
             		<xsl:if test="$accessrightdate!=''">
		        		<xsl:attribute name="endDate"><xsl:value-of select="$accessrightdate" /></xsl:attribute>
	        		</xsl:if>
	        		<xsl:call-template name="oai_accessrights">
	        				<xsl:with-param name="rights" select="$accessright"/>
	        		</xsl:call-template>
	        	</ns4:Access>
	        </xsl:for-each>
        </oai_cerif:Product>
        </xsl:for-each>
	</xsl:template>
	
	<!--	
    	patent: Template that handle publication using a patent item
    -->
	<xsl:template name="patent" match="/">
		<xsl:param name="selector" />
							
		<xsl:for-each select="$selector">
       	<xsl:variable name="item_prop_id">
            <xsl:value-of select="doc:element[@name='others']/doc:field[@name='handle']" />
        </xsl:variable>
        
        <oai_cerif:Patent id="{$item_prop_id}">

           <xsl:for-each select="doc:element[@name='item']/doc:element[@name='openairecristype']/doc:element/doc:field[@name='value']">
	            <oai_cerif:Type xmlns:oai_cerif="https://www.openaire.eu/cerif-profile/vocab/COAR_Patent_Types"><xsl:value-of select="." /></oai_cerif:Type>
            </xsl:for-each>
                        
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='title']/doc:element/doc:field[@name='value']">
                <oai_cerif:Title xml:lang="en"><xsl:value-of select="." /></oai_cerif:Title>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='date']/doc:element[@name='issued']/doc:element/doc:field[@name='value']">
            	<xsl:if test=". != ''">
                	<oai_cerif:RegistrationDate><xsl:value-of select="." /></oai_cerif:RegistrationDate>
                </xsl:if>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dcterms']/doc:element[@name='dateAccepted']/doc:element/doc:field[@name='value']">
            	<xsl:if test=". != ''">
                	<oai_cerif:ApprovalDate><xsl:value-of select="." /></oai_cerif:ApprovalDate>
				</xsl:if>	               	
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='publisher']/doc:element/doc:field[@name='value']">
            	<oai_cerif:Issuer>
	             	<xsl:variable name="dc_publisher_id">
	             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
           			<oai_cerif:DisplayName><xsl:value-of select="." /></oai_cerif:DisplayName>
					<xsl:call-template name="ou">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="ou_id" select="$dc_publisher_id" />
					</xsl:call-template>
				</oai_cerif:Issuer>
            </xsl:for-each>
            
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='identifier']/doc:element[@name='patentno']/doc:element/doc:field[@name='value']">
                <oai_cerif:PatentNumber><xsl:value-of select="." /></oai_cerif:PatentNumber>
            </xsl:for-each>
            
            <xsl:if test="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element/doc:field[@name='value']!=''">
            <!-- oai_cerif:Inventors [START] -->
            <oai_cerif:Inventors>
            <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='contributor']/doc:element[@name='author']/doc:element">
             	<xsl:variable name="dc_contributor_author_id">
             		<xsl:value-of select="./doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
            	    <oai_cerif:Inventor>
            	    	<oai_cerif:DisplayName><xsl:value-of select="./doc:field[@name='value']" /></oai_cerif:DisplayName>
            	    	<xsl:for-each select="./doc:element[@name='authority']">           	    	
							<xsl:call-template name="person">
								<xsl:with-param name="selector" select="." />
								<xsl:with-param name="person_id" select="$dc_contributor_author_id" />
							</xsl:call-template>
						</xsl:for-each>						
					</oai_cerif:Inventor>
            </xsl:for-each>
            </oai_cerif:Inventors>
            <!-- oai_cerif:Inventors [END] -->
            </xsl:if>
                     
			<xsl:if test="doc:element[@name='dcterms']/doc:element[@name='rightsHolder']/doc:element/doc:field[@name='value']!=''">                        
            <!-- oai_cerif:Holders [START] -->
            <oai_cerif:Holders>
            <xsl:for-each select="doc:element[@name='dcterms']/doc:element[@name='rightsHolder']/doc:element/doc:field[@name='value']">
             	<xsl:variable name="dc_holder_id">
             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
             	</xsl:variable>
           	    	<oai_cerif:Holder>
						<oai_cerif:DisplayName><xsl:value-of select="." /></oai_cerif:DisplayName>           	    	
						<xsl:call-template name="ou">
							<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
							<xsl:with-param name="ou_id" select="$dc_holder_id" />
						</xsl:call-template>
					</oai_cerif:Holder>           		
            </xsl:for-each>
            </oai_cerif:Holders>
            <!-- oai_cerif:Holders [END] -->
            </xsl:if>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='description']/doc:element[@name='abstract']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Abstract xml:lang="en"><xsl:value-of select="." /></oai_cerif:Abstract>
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='subject']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:Keyword xml:lang="en"><xsl:value-of select="." /></oai_cerif:Keyword>
	        </xsl:for-each>

	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element/doc:field[@name='value']">
	        	<oai_cerif:OriginatesFrom>	        
		        	<xsl:variable name="project_id">
	             		<xsl:value-of select="../doc:element[@name='authority']/doc:element[@name='others']/doc:field[@name='handle']/text()" />
	             	</xsl:variable>
             		<!-- <oai_cerif:DisplayName><xsl:value-of select="." /></oai_cerif:DisplayName> -->
             		<xsl:choose>
             		<xsl:when test="$project_id!=''">
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="../doc:element[@name='authority']" />
						<xsl:with-param name="project_id" select="$project_id" />
					</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
 					<xsl:call-template name="project">
						<xsl:with-param name="selector" select="." />
						<xsl:with-param name="project_id" select="''" />
					</xsl:call-template>					
					</xsl:otherwise>
					</xsl:choose>
	        	</oai_cerif:OriginatesFrom>					
	        </xsl:for-each>
	        
	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='patent']/doc:element/doc:element[@name='authority']">
	        	<oai_cerif:Predecessor>	        
 					<xsl:call-template name="patent">
						<xsl:with-param name="selector" select="." />
					</xsl:call-template>
	        	</oai_cerif:Predecessor>		
	        </xsl:for-each>

	        <xsl:for-each select="doc:element[@name='dc']/doc:element[@name='relation']/doc:element[@name='references']/doc:element/doc:element[@name='authority']">
	        	<oai_cerif:References>	        
 					<xsl:call-template name="publication">
						<xsl:with-param name="selector" select="." />
					</xsl:call-template>
	        	</oai_cerif:References>		
	        </xsl:for-each>	        
        </oai_cerif:Patent>
        </xsl:for-each>
	</xsl:template>
	
    <xsl:template match="/">

        <!-- item -->
        <xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='type']/text()='item'">
        	<!-- check type (is a publication or a product or ...) -->
        	<xsl:call-template name="item_chooser">
				<xsl:with-param name="selector" select="." />
			</xsl:call-template>
        </xsl:if>
        
        <!-- ou -->
        <xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='type']/text()='ou'">
	        <xsl:variable name="orgunit_id">
	 			<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()" />
		    </xsl:variable>
        	<xsl:call-template name="ou">
		    	<xsl:with-param name="selector" select="doc:metadata" />
		        <xsl:with-param name="ou_id" select="$orgunit_id" />
		   	</xsl:call-template>
        </xsl:if>
        
        <!-- rp -->
        <xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='type']/text()='rp'">
	        <xsl:variable name="rp_id">
            	<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()" />
            </xsl:variable>
			<xsl:call-template name="person">
				<xsl:with-param name="selector" select="doc:metadata" />
				<xsl:with-param name="person_id" select="$rp_id" />
			</xsl:call-template>
        </xsl:if>
                
        <!-- project -->
        <xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='type']/text()='project'">
	        <xsl:variable name="project_id">
            	<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()" />
            </xsl:variable>
			<xsl:call-template name="project">
				<xsl:with-param name="selector" select="doc:metadata" />
				<xsl:with-param name="project_id" select="$project_id" />
			</xsl:call-template>
        </xsl:if>
        
        <!-- events -->
        <xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='type']/text()='events'">
	        <xsl:variable name="events_id">
            	<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()" />
            </xsl:variable>
			<xsl:call-template name="events">
				<xsl:with-param name="selector" select="doc:metadata" />
				<xsl:with-param name="event_id" select="$events_id" />
			</xsl:call-template>
        </xsl:if>
        
        <!-- journals -->
        <xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='type']/text()='journals'">
        	<xsl:variable name="journals_id">
            	<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()" />
            </xsl:variable>
			<xsl:call-template name="journal">
				<xsl:with-param name="selector" select="doc:metadata" />
				<xsl:with-param name="journal_id" select="$journals_id" />
			</xsl:call-template>
        </xsl:if>
        
        <!-- equipment -->
        <xsl:if test="doc:metadata/doc:element[@name='others']/doc:field[@name='type']/text()='equipment'">
	        <xsl:variable name="equipment_id">
            	<xsl:value-of select="doc:metadata/doc:element[@name='others']/doc:field[@name='handle']/text()" />
            </xsl:variable>
			<xsl:call-template name="equipment">
				<xsl:with-param name="selector" select="doc:metadata" />
				<xsl:with-param name="equipment_id" select="$equipment_id" />
			</xsl:call-template>
        </xsl:if>        
    </xsl:template>
	
</xsl:stylesheet>
