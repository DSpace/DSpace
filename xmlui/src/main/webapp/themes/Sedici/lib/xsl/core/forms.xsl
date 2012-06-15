<!--

	Template extraido de tema mirage con el mismo nombre.
	Solo redefino el template necesario. Para modificar otros dirigirse al template original y copiar los necesarios.
-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
	xmlns:dri="http://di.tamu.edu/DRI/1.0/"
	xmlns:mets="http://www.loc.gov/METS/"
	xmlns:xlink="http://www.w3.org/TR/xlink/"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
	xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:mods="http://www.loc.gov/mods/v3"
	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>
    

    <!-- Fieldset (instanced) field stuff, in the case of non-composites -->
    <xsl:template match="dri:field[dri:field/dri:instance | dri:params/@operations]" priority="2">
        <xsl:apply-templates select="dri:help" mode="help"/>

        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="especialSimpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>

                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
				<!-- <xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>                  -->    
              </div>
        </xsl:if>
        
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit"  value="xmlui.forms.add" i18n:attr="value" id="{concat('submit_',@n,'_add')}" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
	        </xsl:if>
           </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:error" mode="error"/>
    </xsl:template>
    
   
<!--   <input type="hidden" value="Aacn Clinical Issues" name="dc_title_alternative_1">
<input class="ds-authority-value " type="text" onchange="javascript: return DSpaceAuthorityOnChange(this, '_confidence','');" value="1079-0713" name="dc_title_alternative_authority_1" readonly="readonly">
<input class="ds-authority-confidence-input" type="hidden" value="failed" name="dc_title_alternative_confidence_1">
-->   

    <!-- Template especiales para la muestra de los valores en las subscripciones del perfil -->     
    <xsl:template match="dri:field[@n='subscriptions']" priority="2">
        <xsl:apply-templates select="dri:help" mode="help"/>
        <!-- Create the first field normally -->
        <xsl:apply-templates select="." mode="normalField"/>
        <!-- Follow it up with an ADD button if the add operation is specified. This allows
            entering more than one value for this field. -->
        <xsl:if test="contains(dri:params/@operations,'add')">
            <!-- Add buttons should be named "submit_[field]_add" so that we can ignore errors from required fields when simply adding new values-->
            <input type="submit" value="xmlui.profile.subscriptions.add" id="{concat('submit_',@n,'_add')}" name="{concat('submit_',@n,'_add')}" class="ds-button-field ds-add-button" i18n:attr="value">
              <!-- Make invisible if we have choice-lookup popup that provides its own Add. -->
              <xsl:if test="dri:params/@choicesPresentation = 'lookup'">
                <xsl:attribute name="style">
                  <xsl:text>display:none;</xsl:text>
                </xsl:attribute>
        	  </xsl:if>
           </input>
        </xsl:if>
        <br/>
        <xsl:apply-templates select="dri:error" mode="error"/>
        <xsl:if test="dri:instance">
            <div class="ds-previous-values">
                <!-- Iterate over the dri:instance elements contained in this field. The instances contain
                    stored values as either "interpreted", "raw", or "default" values. -->
                <xsl:call-template name="subscriptionsSimpleFieldIterator">
                    <xsl:with-param name="position">1</xsl:with-param>
                </xsl:call-template>
                <!-- Conclude with a DELETE button if the delete operation is specified. This allows
                    removing one or more values stored for this field. -->
                <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
                    <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
                    <input type="submit" value="xmlui.profile.subscriptions.remove" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" i18n:attr="value"/>
                </xsl:if>
                <!-- Behind the scenes, add hidden fields for every instance set. This is to make sure that
                    the form still submits the information in those instances, even though they are no
                    longer encoded as HTML fields. The DRI Reference should contain the exact attributes
                    the hidden fields should have in order for this to work properly. -->
				<xsl:apply-templates select="dri:instance" mode="hiddenInterpreter"/>                    
              </div>
        </xsl:if>

    </xsl:template>
    
    <!-- Utilizo un field iterator especial pues los instances del subscription no tienen el valor, y lo tengo que sacar del list -->
    <xsl:template name="subscriptionsSimpleFieldIterator">
        <xsl:param name="position"/>
        
        <xsl:if test="dri:instance[position()=$position]">
            <xsl:variable name="authValue" select="substring-before(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>            
            <xsl:variable name="authLabel" select="substring-after(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>
            <xsl:variable name="valor"><xsl:value-of select="dri:instance[position()=$position]/dri:value/@option"/></xsl:variable>
            <input type="checkbox" value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}">
              <xsl:copy-of select="dri:option[@returnValue=$valor]"/>
            </input>
             <!-- look for authority value in instance. -->
            <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">             	
              <xsl:call-template name="multipleAuthorityInputFields">
                <xsl:with-param name="name" select="@n"/>
      			<xsl:with-param name="position" select="$position"/>
     			<xsl:with-param name="value" select="dri:instance[position()=$position]/dri:value"/>
      			<xsl:with-param name="authValue" select="$authValue"/>
      			<xsl:with-param name="authLabel" select="$authLabel"/>
      			<xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
              </xsl:call-template>
            </xsl:if>
            <br/>
            <xsl:call-template name="subscriptionsSimpleFieldIterator">
                <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    
    <!-- Este template recibirá en la propiedad authority formateado de la siguiente manera
	     authorityKey#authorityLabel -->
    <xsl:template name="especialSimpleFieldIterator">
        <xsl:param name="position"/>
        
        <xsl:if test="dri:instance[position()=$position]">
        
        	<div class="previous-value-item">
        
	            <xsl:variable name="authValue" select="substring-before(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>
	            <xsl:variable name="authLabel" select="substring-after(dri:instance[position()=$position]/dri:value[@type='authority'], '#')"/>
	            <xsl:variable name="checkboxId" select="concat(@n,'_checkbox_',$position)"/>
	            <input type="checkbox" id="{$checkboxId}"  value="{concat(@n,'_',$position)}" name="{concat(@n,'_selected')}" style="display:none;"/> 
	
	            <xsl:apply-templates select="dri:instance[position()=$position]" mode='inputChange'>
	               <xsl:with-param name="position" select="$position"/>
	            </xsl:apply-templates>
	
	             <!-- look for authority value in instance. -->
	            <xsl:if test="dri:instance[position()=$position]/dri:value[@type='authority']">             	
	              <xsl:call-template name="multipleAuthorityInputFields">
	                <xsl:with-param name="name" select="@n"/>
	      			<xsl:with-param name="position" select="$position"/>
	     			<xsl:with-param name="value" select="dri:instance[position()=$position]/dri:value"/>
	      			<xsl:with-param name="authValue" select="$authValue"/>
	      			<xsl:with-param name="authLabel" select="$authLabel"/>
	      			<xsl:with-param name="confValue" select="dri:instance[position()=$position]/dri:value[@type='authority']/@confidence"/>
	              </xsl:call-template>
	            </xsl:if>
	            
	             <!-- Conclude with a DELETE button if the delete operation is specified. This allows
	                removing one value stored for this field. -->
	            <xsl:if test="contains(dri:params/@operations,'delete') and dri:instance">
	                <!-- Delete buttons should be named "submit_[field]_delete" so that we can ignore errors from required fields when simply removing values-->
	                <input type="submit" value="xmlui.forms.remove" i18n:attr="value" name="{concat('submit_',@n,'_delete')}" class="ds-button-field ds-delete-button" onClick="eliminarMetadato('{$checkboxId}');"/>
	            </xsl:if>
	            
            </div>
            
            <xsl:call-template name="especialSimpleFieldIterator">
                <xsl:with-param name="position"><xsl:value-of select="$position + 1"/></xsl:with-param>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
     
    <!-- TEMPLATE PARA GENERAR LOS INPUt PARA MODIFICACIONES -->
    
    <xsl:template match="dri:instance" mode="inputChange">
        <xsl:param name="position" select="1"/>
        <xsl:choose>
             <xsl:when test="../@type= 'textarea'">
             	<textarea>
                    <xsl:call-template name="fieldAttributes"/>
                    <xsl:attribute name="class">ds-textarea-field submit-textarea</xsl:attribute>
                    <xsl:attribute name="name"><xsl:value-of select="concat(../@n,'_',$position)"/></xsl:attribute>
                    <xsl:choose>
                       <xsl:when test="not(../dri:params[@cols])">
                           <xsl:call-template name="textAreaCols"/>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:choose>
                       <xsl:when test="not(../dri:params[@rows])">
                           <xsl:call-template name="textAreaRows"/>
                       </xsl:when>
                    </xsl:choose>
			        <xsl:value-of select="dri:value" disable-output-escaping="yes"/>
                </textarea> 
             </xsl:when>
             <xsl:when test="../@type= 'text' and not(../dri:params/@authorityControlled='yes')">
			       <input type="text">
			           <xsl:attribute name="class">ds-text-field submit-text</xsl:attribute>
			           <xsl:attribute name="name"><xsl:value-of select="concat(../@n,'_',$position)"/></xsl:attribute>
			           <xsl:attribute name="value">
			               <xsl:value-of select="dri:value[@type='raw']"/>
			           </xsl:attribute>
			       </input>
             </xsl:when>
             <xsl:when test="not(../dri:params/@authorityControlled='yes') ">
                    <xsl:value-of select="dri:value[@type='raw']"/>                    
             </xsl:when>
             <xsl:when test="not(../dri:params/@choicesPresentation='suggest') ">
                    <xsl:value-of select="dri:value[@type='raw']"/>                    
             </xsl:when>
        </xsl:choose>
        
        <xsl:apply-templates select="." mode="i18n-field">
        	<xsl:with-param name="position" select="$position"/>
        </xsl:apply-templates>

    </xsl:template>
    
    <xsl:template match="/dri:document/dri:body/dri:div[@id='aspect.xmlworkflow.WorkflowTransformer.div.perform-task']/dri:head">
        <xsl:variable name="handle"><xsl:value-of select="substring-after(substring-before(../dri:referenceSet/dri:reference/@url,'/mets.xml'), 'metadata/')"/></xsl:variable>
        <xsl:variable name="head_count" select="count(ancestor::dri:*[dri:head])"/>
        <xsl:variable name="class">ds-div-head</xsl:variable>
        <xsl:element name="h{$head_count}">
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class" select="$class"/>
            </xsl:call-template>
            <xsl:apply-templates/>
            <xsl:choose>
            	<xsl:when test="contains($handle,'internal')">
            		(<i18n:text>xmlui.forms.claimedAction.non_installed</i18n:text>)
            	</xsl:when>
            	<xsl:otherwise>
	            	(<a>
		        	  <xsl:attribute name="href">
		        	  <xsl:value-of select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='contextPath']"/>/<xsl:value-of select="$handle"/>
                      </xsl:attribute>		        	  
		        	  <xsl:value-of select="substring-after($handle, 'handle/')"/>
		            </a>)
            	</xsl:otherwise>
            </xsl:choose>
        </xsl:element>

    </xsl:template>
    
    <!-- An item in a nested "form" list -->
    <xsl:template match="dri:list[@id='aspect.submission.StepTransformer.list.submit-review']//dri:list[@type='form']/dri:item[not(@id)]" priority="3">
        <li>
                <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">
                    <xsl:text>ds-form-item </xsl:text>

                <!-- Row counting voodoo, meant to impart consistent row alternation colors to the form lists.
                    Should probably be chnaged to a system that is more straitforward. -->
                <xsl:choose>
                    <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 0">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">odd </xsl:if>
                        
                    </xsl:when>
                    <xsl:when test="(count(../../..//dri:item) - count(../../..//dri:list[@type='form'])) mod 2 = 1">
                        <!--<xsl:if test="count(../dri:item) > 3">-->
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 1)">even </xsl:if>
                            <xsl:if test="(count(preceding-sibling::dri:item | ../../preceding-sibling::dri:item/dri:list[@type='form']/dri:item) mod 2 = 0)">odd </xsl:if>
                        
                    </xsl:when>
                </xsl:choose>
                <!--
                <xsl:if test="position()=last() and dri:field[@type='button'] and not(dri:field[not(@type='button')])">last</xsl:if>
                    -->
               </xsl:with-param>
            </xsl:call-template>
            
            <xsl:call-template name="pick-label"/>

            <xsl:choose>
                <xsl:when test="dri:field[@type='composite']">
                    <xsl:apply-templates mode="formComposite"/>
                </xsl:when>
                <xsl:otherwise>
                    <div class="ds-form-content">
                        <xsl:choose>
                        	<xsl:when test="count(*)>0">
                        		<xsl:apply-templates/>
                        	</xsl:when>
                        	<xsl:otherwise>
                        		<xsl:value-of select="." disable-output-escaping="yes"/>
                        	</xsl:otherwise>
                        </xsl:choose>
                        
                        <!-- special name used in submission UI review page -->
                        <xsl:if test="@n = 'submit-review-field-with-authority'">
                          <xsl:call-template name="authorityConfidenceIcon">
                            <xsl:with-param name="confidence" select="substring-after(./@rend, 'cf-')"/>
                          </xsl:call-template>
                        </xsl:if>
                    </div>
                </xsl:otherwise>
            </xsl:choose>
        </li>
    </xsl:template>
	<!-- Normal field rendering -->
    <xsl:template match="dri:field" mode="normalField">
        <xsl:variable name="confidenceIndicatorID" select="concat(translate(@id,'.','_'),'_confidence_indicator')"/>
        <xsl:choose>
			<!-- TODO: this has changed drammatically (see form3.xml) -->
			<xsl:when test="@type= 'select'">
				<select>
					<xsl:call-template name="fieldAttributes" />
					<xsl:apply-templates />
				</select>
			</xsl:when>
               
           	<xsl:when test="@type= 'textarea'">
                <textarea>
                    <xsl:call-template name="fieldAttributes"/>

                    <!--
                        if the cols and rows attributes are not defined we need to call
                        the tempaltes for them since they are required attributes in strict xhtml
                     -->
                    <xsl:choose>
                        <xsl:when test="not(./dri:params[@cols])">
                               <xsl:call-template name="textAreaCols"/>
                        </xsl:when>
                    </xsl:choose>
                    <xsl:choose>
                        <xsl:when test="not(./dri:params[@rows])">
                               <xsl:call-template name="textAreaRows"/>
                        </xsl:when>
                    </xsl:choose>

                    <xsl:apply-templates />
                    
                    <xsl:choose>
                        <xsl:when test="./dri:value[@type='raw']">
                            <xsl:copy-of select="./dri:value[@type='raw']/node()"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of select="./dri:value[@type='default']/node()"/>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:if  test="string-length(./dri:value) &lt; 1">
                       <i18n:text>xmlui.dri2xhtml.default.textarea.value</i18n:text>
                    </xsl:if>
                </textarea>

				<xsl:apply-templates select="." mode="i18n-field"/>

				<!-- add place to store authority value -->
				<xsl:if test="dri:params/@authorityControlled">
					<xsl:variable name="confidence">
						<xsl:if test="./dri:value[@type='authority']">
							<xsl:value-of select="./dri:value[@type='authority']/@confidence" />
						</xsl:if>
					</xsl:variable>
					<!-- add authority confidence widget -->
					<xsl:call-template name="authorityConfidenceIcon">
						<xsl:with-param name="confidence" select="$confidence" />
						<xsl:with-param name="id" select="$confidenceIndicatorID" />
					</xsl:call-template>
					<xsl:call-template name="authorityInputFields">
						<xsl:with-param name="name" select="@n" />
						<xsl:with-param name="id" select="@id" />
						<xsl:with-param name="authValue" select="dri:value[@type='authority']/text()" />
						<xsl:with-param name="confValue" select="dri:value[@type='authority']/@confidence" />
						<xsl:with-param name="confIndicatorID" select="$confidenceIndicatorID" />
						<xsl:with-param name="unlockButton" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/@n" />
						<xsl:with-param name="unlockHelp" select="dri:value[@type='authority']/dri:field[@rend='ds-authority-lock']/dri:help" />
					</xsl:call-template>
				</xsl:if>
				
				<!-- add choice mechanisms -->
				<xsl:choose>
					<xsl:when test="dri:params/@choicesPresentation = 'suggest'">
						<xsl:call-template name="addAuthorityAutocomplete">
							<xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID" />
							<xsl:with-param name="confidenceName">
								<xsl:value-of select="concat(@n,'_confidence')" />
							</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="dri:params/@choicesPresentation = 'lookup'">
						<xsl:call-template name="addLookupButton">
							<xsl:with-param name="isName" select="'false'" />
							<xsl:with-param name="confIndicator" select="$confidenceIndicatorID" />
						</xsl:call-template>
					</xsl:when>
				</xsl:choose>
			</xsl:when>

			<!-- This is changing drammatically -->
			<xsl:when test="@type= 'checkbox' or @type= 'radio'">
				<fieldset>
					<xsl:call-template name="standardAttributes">
						<xsl:with-param name="class">
							<xsl:text>ds-</xsl:text>
							<xsl:value-of select="@type" />
							<xsl:text>-field </xsl:text>
							<xsl:if test="dri:error">
								<xsl:text>error </xsl:text>
							</xsl:if>
						</xsl:with-param>
					</xsl:call-template>
					<xsl:attribute name="id"><xsl:value-of select="generate-id()" /></xsl:attribute>
					<xsl:if test="dri:label">
						<legend>
							<xsl:apply-templates select="dri:label" mode="compositeComponent" />
						</legend>
					</xsl:if>
					<xsl:apply-templates />
				</fieldset>
			</xsl:when>
			
           <!--
               <input>
                           <xsl:call-template name="fieldAttributes"/>
                   <xsl:if test="dri:value[@checked='yes']">
                               <xsl:attribute name="checked">checked</xsl:attribute>
                   </xsl:if>
                   <xsl:apply-templates/>
               </input>
               -->
            <xsl:when test="@type= 'composite'">
                <!-- TODO: add error and help stuff on top of the composite -->
                <span class="ds-composite-field">
                    <xsl:apply-templates select="dri:field" mode="compositeComponent"/>
                </span>
                <xsl:apply-templates select="dri:field/dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:error" mode="compositeComponent"/>
                <xsl:apply-templates select="dri:field/dri:help" mode="compositeComponent"/>
                <!--<xsl:apply-templates select="dri:help" mode="compositeComponent"/>-->
            </xsl:when>

                   <!-- text, password, file, and hidden types are handled the same.
                       Buttons: added the xsl:if check which will override the type attribute button
                           with the value 'submit'. No reset buttons for now...
	                    -->
			<xsl:otherwise>
				<input>
					<xsl:call-template name="fieldAttributes" />
					<xsl:if test="@type='button'">
						<xsl:attribute name="type">submit</xsl:attribute>
					</xsl:if>
					<xsl:attribute name="value">
                        <xsl:choose>
                            <xsl:when test="./dri:value[@type='raw']">
                                <xsl:value-of select="./dri:value[@type='raw']" />
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="./dri:value[@type='default']" />
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:attribute>
					<xsl:if test="dri:value/i18n:text">
						<xsl:attribute name="i18n:attr">value</xsl:attribute>
					</xsl:if>
					<xsl:apply-templates />
				</input>
		
				<xsl:apply-templates select="." mode="i18n-field"/>
		
				<xsl:variable name="confIndicatorID" select="concat(@id,'_confidence_indicator')" />
				<xsl:if test="dri:params/@authorityControlled">
					<xsl:variable name="confidence">
						<xsl:if test="./dri:value[@type='authority']">
							<xsl:value-of select="./dri:value[@type='authority']/@confidence" />
						</xsl:if>
					</xsl:variable>
					<!-- add authority confidence widget -->
					<xsl:call-template name="authorityConfidenceIcon">
						<xsl:with-param name="confidence" select="$confidence" />
						<xsl:with-param name="id" select="$confidenceIndicatorID" />
					</xsl:call-template>
					<xsl:call-template name="authorityInputFields">
						<xsl:with-param name="name" select="@n" />
						<xsl:with-param name="id" select="@id" />
						<xsl:with-param name="authValue"
							select="dri:value[@type='authority']/text()" />
						<xsl:with-param name="confValue"
							select="dri:value[@type='authority']/@confidence" />
					</xsl:call-template>
				</xsl:if>
				<xsl:choose>
					<xsl:when test="dri:params/@choicesPresentation = 'suggest'">
						<xsl:call-template name="addAuthorityAutocomplete">
							<xsl:with-param name="confidenceIndicatorID" select="$confidenceIndicatorID" />
							<xsl:with-param name="confidenceName">
								<xsl:value-of select="concat(@n,'_confidence')" />
							</xsl:with-param>
						</xsl:call-template>
					</xsl:when>
					<xsl:when test="dri:params/@choicesPresentation = 'lookup'">
						<xsl:call-template name="addLookupButton">
							<xsl:with-param name="isName" select="'false'" />
							<xsl:with-param name="confIndicator" select="$confidenceIndicatorID" />
						</xsl:call-template>
					</xsl:when>
				</xsl:choose>
			</xsl:otherwise>
        </xsl:choose>
    </xsl:template>

	<xsl:template match="dri:field|dri:instance" mode="i18n-field">
		<xsl:param name="position" select="''"/>
		
		<xsl:if test="dri:params[@i18nable = 'yes']|../dri:params[@i18nable = 'yes']">
			<!-- this element's name -->
			<xsl:variable name="name">
				<xsl:choose>
				 	<!-- assumes it's a dri:instance -->
					<xsl:when test="../dri:params[@i18nable = 'yes']">
						<xsl:value-of select="concat(../@n,'_lang')"/>					
					</xsl:when>
					<!-- assumes it's a dri:field -->
					<xsl:otherwise>
						<xsl:value-of select="concat(@n,'_lang')"/>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:if test="$position != ''">
					<xsl:value-of select="concat('_',$position)"/>
				</xsl:if>
			</xsl:variable>

			<!-- we show the combo only to administrators. Normal user get a hidden input with default language -->
			<xsl:choose>
				<xsl:when test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element = 'administrator'] = 'true'">
					<xsl:variable name="lang" select="dri:value[@type='lang']"/>
					
					<select class="ds-select-field select-lang">
						<xsl:attribute name="name"><xsl:value-of select="$name"/></xsl:attribute>
						<!-- option list -->
						<xsl:apply-templates select="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='supported_locale']">
							<xsl:with-param name="lang" select="$lang"/>
						</xsl:apply-templates>
					</select>
				</xsl:when>

				<xsl:otherwise>
					<!-- we take the default language from de repository information -->
					<xsl:variable name="siteMETSUrl">
			            <xsl:text>cocoon:/</xsl:text>
			            <xsl:value-of select="/dri:document/dri:meta/dri:repositoryMeta/dri:repository/@url"/>
			            <xsl:text>?sections=dmdSec</xsl:text>
			        </xsl:variable>
				
					<xsl:variable name="default_lang" select="document($siteMETSUrl)/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='default' and @qualifier='language']"/>
					
					<input type="hidden">
						<xsl:attribute name="name">
							<xsl:value-of select="$name"/>
						</xsl:attribute>
						<xsl:attribute name="value">
							<xsl:value-of select="$default_lang"/>
						</xsl:attribute>
					</input>
				</xsl:otherwise>
			</xsl:choose>
			
		</xsl:if>
	</xsl:template>
	
	<!-- generates the language options for metadata lanugage selection -->
	<xsl:template match="dri:metadata[@element='supported_locale']">
		<xsl:param name="lang"/>
		<option>
			<xsl:attribute name="value">
				<xsl:value-of select="."/>
			</xsl:attribute>
			<xsl:if test="string(.) = $lang">
				<xsl:attribute name="selected">true</xsl:attribute>
			</xsl:if>
			<i18n:text>xmlui.dri2xhtml.METS-1.0.code-value-<xsl:value-of select="."></xsl:value-of></i18n:text>
		</option>
	</xsl:template>

</xsl:stylesheet>
