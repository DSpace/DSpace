<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->
<!--
    Templates to cover the common dri elements.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

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


    <!-- First and foremost come the div elements, which are the only elements directly under body. Every
        document has a body and every body has at least one div, which may in turn contain other divs and
        so on. Divs can be of two types: interactive and non-interactive, as signified by the attribute of
        the same name. The two types are handled separately.
    -->

    <!-- Non-interactive divs get turned into HTML div tags. The general process, which is found in many
        templates in this stylesheet, is to call the template for the head element (creating the HTML h tag),
        handle the attributes, and then apply the templates for the all children except the head. The id
        attribute is -->
    <xsl:template match="dri:div" priority="1">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-static-div</xsl:with-param>
            </xsl:call-template>
            <xsl:choose>
                    <!--  does this element have any children -->
                        <xsl:when test="child::node()">
                                <xsl:apply-templates select="*[not(name()='head')]"/>
                    </xsl:when>
                        <!-- if no children are found we add a space to eliminate self closing tags -->
                        <xsl:otherwise>
                                &#160;
                        </xsl:otherwise>
                </xsl:choose>
        </div>
        <xsl:variable name="itemDivision">
                        <xsl:value-of select="@n"/>
                </xsl:variable>
                <xsl:variable name="xrefTarget">
                        <xsl:value-of select="./dri:p/dri:xref/@target"/>
                </xsl:variable>
                <xsl:if test="$itemDivision='item-view'">
                    <xsl:call-template name="cc-license">
                        <xsl:with-param name="metadataURL" select="./dri:referenceSet/dri:reference/@url"/>
                    </xsl:call-template>
                </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>

    <!-- Interactive divs get turned into forms. The priority attribute on the template itself
        signifies that this template should be executed if both it and the one above match the
        same element (namely, the div element).

        Strictly speaking, XSL should be smart enough to realize that since one template is general
        and other more specific (matching for a tag and an attribute), it should apply the more
        specific once is it encounters a div with the matching attribute. However, the way this
        decision is made depends on the implementation of the XSL parser is not always consistent.
        For that reason explicit priorities are a safer, if perhaps redundant, alternative. -->
    <xsl:template match="dri:div[@interactive='yes']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">top</xsl:with-param>
        </xsl:apply-templates>
        <form>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-interactive-div</xsl:with-param>
            </xsl:call-template>
            <xsl:attribute name="action"><xsl:value-of select="@action"/></xsl:attribute>
            <xsl:attribute name="method"><xsl:value-of select="@method"/></xsl:attribute>
            <xsl:if test="@method='multipart'">
                <xsl:attribute name="method">post</xsl:attribute>
                <xsl:attribute name="enctype">multipart/form-data</xsl:attribute>
            </xsl:if>
            <xsl:attribute name="onsubmit">javascript:tSubmit(this);</xsl:attribute>
                        <!--For Item Submission process, disable ability to submit a form by pressing 'Enter'-->
                        <xsl:if test="starts-with(@n,'submit')">
                                <xsl:attribute name="onkeydown">javascript:return disableEnterKey(event);</xsl:attribute>
            </xsl:if>
                        <xsl:apply-templates select="*[not(name()='head')]"/>

        </form>
        <!-- JS to scroll form to DIV parent of "Add" button if jump-to -->
        <xsl:if test="/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo']">
          <script type="text/javascript">
            <xsl:text>var button = document.getElementById('</xsl:text>
            <xsl:value-of select="translate(@id,'.','_')"/>
            <xsl:text>').elements['</xsl:text>
            <xsl:value-of select="concat('submit_',/dri:document/dri:meta/dri:pageMeta/dri:metadata[@element='page'][@qualifier='jumpTo'],'_add')"/>
            <xsl:text>'];</xsl:text>
            <xsl:text>
                      if (button != null) {
                        var n = button.parentNode;
                        for (; n != null; n = n.parentNode) {
                            if (n.tagName == 'DIV') {
                              n.scrollIntoView(false);
                              break;
                           }
                        }
                      }
            </xsl:text>
          </script>
        </xsl:if>
        <xsl:apply-templates select="@pagination">
            <xsl:with-param name="position">bottom</xsl:with-param>
        </xsl:apply-templates>
    </xsl:template>


    <!-- Special case for divs tagged as "notice" -->
    <xsl:template match="dri:div[@n='general-message']" priority="3">
        <div>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-notice-div</xsl:with-param>
            </xsl:call-template>
                <xsl:apply-templates />
        </div>
    </xsl:template>


    <!-- Next come the three structural elements that divs that contain: table, p, and list. These are
        responsible for display of static content, forms, and option lists. The fourth element under
        body, referenceSet, is used to reference blocks of metadata and will be discussed further down.
    -->


    <!-- First, the table element, used for rendering data in tabular format. In DRI tables consist of
        an optional head element followed by a set of row tags. Each row in turn contains a set of cells.
        Rows and cells can have different roles, the most common ones being header and data (with the
        attribute omitted in the latter case). -->
    <xsl:template match="dri:table">
        <xsl:apply-templates select="dri:head"/>
        <table>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table</xsl:with-param>
            </xsl:call-template>
            <!-- rows and cols attributes are not allowed in strict
            <xsl:attribute name="rows"><xsl:value-of select="@rows"/></xsl:attribute>
            <xsl:attribute name="cols"><xsl:value-of select="@cols"/></xsl:attribute>

            <xsl:if test="count(dri:row[@role='header']) &gt; 0">
                    <thead>
                        <xsl:apply-templates select="dri:row[@role='header']"/>
                    </thead>
            </xsl:if>
            <tbody>
                <xsl:apply-templates select="dri:row[not(@role='header')]"/>
            </tbody>
            -->
            <xsl:apply-templates select="dri:row"/>
        </table>
    </xsl:template>

    <!-- Header row, most likely filled with header cells -->
    <xsl:template match="dri:row[@role='header']" priority="2">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-header-row</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </tr>
    </xsl:template>

    <!-- Header cell, assumed to be one since it is contained in a header row -->
    <xsl:template match="dri:row[@role='header']/dri:cell | dri:cell[@role='header']" priority="2">
        <th>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-header-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </th>
    </xsl:template>


    <!-- Normal row, most likely filled with data cells -->
    <xsl:template match="dri:row" priority="1">
        <tr>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-row
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </tr>
    </xsl:template>

    <!-- Just a plain old table cell -->
    <xsl:template match="dri:cell" priority="1">
        <td>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-cell
                    <xsl:if test="(position() mod 2 = 0)">even</xsl:if>
                    <xsl:if test="(position() mod 2 = 1)">odd</xsl:if>
                </xsl:with-param>
            </xsl:call-template>
            <xsl:if test="@rows">
                <xsl:attribute name="rowspan">
                    <xsl:value-of select="@rows"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:if test="@cols">
                <xsl:attribute name="colspan">
                    <xsl:value-of select="@cols"/>
                </xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </td>
    </xsl:template>






    <!-- Second, the p element, used for display of text. The p element is a rich text container, meaning it
        can contain text mixed with inline elements like hi, xref, figure and field. The cell element above
        and the item element under list are also rich text containers.
    -->
    <xsl:template match="dri:p">
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



    <!-- Finally, we have the list element, which is used to display set of data. There are several different
        types of lists, as signified by the type attribute, and several different templates to handle them. -->

    <!-- First list type is the bulleted list, a list with no real labels and no ordering between elements. -->
    <xsl:template match="dri:list[@type='bulleted']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-bulleted-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>

    <!-- The item template creates an HTML list item element and places the contents of the DRI item inside it.
        Additionally, it checks to see if the currently viewed item has a label element directly preceeding it,
        and if it does, applies the label's template before performing its own actions. This mechanism applies
        to the list item templates as well. -->
    <xsl:template match="dri:list[@type='bulleted']/dri:item" priority="2" mode="nested">
        <li>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'dri:label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]"/>
            </xsl:if>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <!-- The case of nested lists is handled in a similar way across all lists. You match the sub-list based on
        its parent, create a list item approtiate to the list type, fill its content from the sub-list's head
        element and apply the other templates normally. -->
    <xsl:template match="dri:list[@type='bulleted']/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>


    <!-- Second type is the ordered list, which is a list with either labels or names to designate an ordering
        of some kind. -->
    <xsl:template match="dri:list[@type='ordered']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ol>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-ordered-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested">
                <xsl:sort select="dri:item/@n"/>
            </xsl:apply-templates>
        </ol>
    </xsl:template>

    <xsl:template match="dri:list[@type='ordered']/dri:item" priority="2" mode="nested">
        <li>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]"/>
            </xsl:if>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="dri:list[@type='ordered']/dri:list" priority="3" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>


    <!-- Progress list used primarily in forms that span several pages. There isn't a template for the nested
        version of this list, mostly because there isn't a use case for it. -->
    <xsl:template match="dri:list[@type='progress']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-progress-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="dri:item"/>
        </ul>
    </xsl:template>

    <xsl:template match="dri:list[@type='progress']/dri:item" priority="2">
        <li>
            <xsl:attribute name="class">
                <xsl:value-of select="@rend"/>
                <xsl:if test="position()=1">
                    <xsl:text> first</xsl:text>
                </xsl:if>
                <xsl:if test="descendant::dri:field[@type='button']">
                    <xsl:text> button</xsl:text>
                </xsl:if>
                <xsl:if test="position()=last()">
                    <xsl:text> last</xsl:text>
                </xsl:if>
            </xsl:attribute>
            <xsl:apply-templates />
        </li>
        <xsl:if test="not(position()=last())">
            <li class="arrow">
                <xsl:text>&#8594;</xsl:text>
            </li>
        </xsl:if>
    </xsl:template>


    <!-- The third type of list is the glossary (gloss) list. It is essentially a list of pairs, consisting of
        a set of labels, each followed by an item. Unlike the ordered and bulleted lists, gloss is implemented
        via HTML definition list (dd) element. It can also be changed to work as a two-column table. -->
    <xsl:template match="dri:list[@type='gloss']" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <dl>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-gloss-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </dl>
    </xsl:template>

    <xsl:template match="dri:list[@type='gloss']/dri:item" priority="2" mode="nested">
        <dd>
            <xsl:apply-templates />
        </dd>
    </xsl:template>

    <xsl:template match="dri:list[@type='gloss']/dri:label" priority="2" mode="nested">
        <dt>
            <span>
                <xsl:attribute name="class">
                    <xsl:text>ds-gloss-list-label </xsl:text>
                    <xsl:value-of select="@rend"/>
                </xsl:attribute>
                <xsl:apply-templates />
                <xsl:text>:</xsl:text>
            </span>
        </dt>
    </xsl:template>

    <xsl:template match="dri:list[@type='gloss']/dri:list" priority="3" mode="nested">
        <dd>
            <xsl:apply-templates select="."/>
        </dd>
    </xsl:template>


    <!-- The next list type is one without a type attribute. In this case XSL makes a decision: if the items
        of the list have labels the the list will be made into a table-like structure, otherwise it is considered
        to be a plain unordered list and handled generically. -->
    <!-- TODO: This should really be done with divs and spans instead of tables. Form lists have already been
        converted so the solution here would most likely mirror that one -->
    <xsl:template match="dri:list[not(@type)]" priority="2">
        <xsl:apply-templates select="dri:head"/>
        <xsl:if test="count(dri:label)>0">
            <table>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-gloss-list</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="labeled"/>
            </table>
        </xsl:if>
        <xsl:if test="count(dri:label)=0">
            <ul>
                <xsl:call-template name="standardAttributes">
                    <xsl:with-param name="class">ds-simple-list</xsl:with-param>
                </xsl:call-template>
                <xsl:apply-templates select="dri:item" mode="nested"/>
            </ul>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:list[not(@type)]/dri:item" priority="2" mode="labeled">
        <tr>
            <xsl:attribute name="class">
                <xsl:text>ds-table-row </xsl:text>
                <xsl:if test="(position() mod 2 = 0)">even </xsl:if>
                <xsl:if test="(position() mod 2 = 1)">odd </xsl:if>
                <xsl:value-of select="@rend"/>
            </xsl:attribute>
            <xsl:if test="name(preceding-sibling::*[position()=1]) = 'label'">
                <xsl:apply-templates select="preceding-sibling::*[position()=1]" mode="labeled"/>
            </xsl:if>
            <td>
                <xsl:apply-templates />
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="dri:list[not(@type)]/dri:label" priority="2" mode="labeled">
        <td>
            <xsl:if test="count(./node())>0">
                <span>
                    <xsl:attribute name="class">
                        <xsl:text>ds-gloss-list-label </xsl:text>
                        <xsl:value-of select="@rend"/>
                    </xsl:attribute>
                    <xsl:apply-templates />
                    <xsl:text>:</xsl:text>
                </span>
            </xsl:if>
        </td>
    </xsl:template>

    <xsl:template match="dri:list[not(@type)]/dri:item" priority="2" mode="nested">
        <li>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list-item</xsl:with-param>
            </xsl:call-template>
            <!-- Wrap orphaned sub-lists into the preceding item -->
            <xsl:variable name="node-set1" select="./following-sibling::dri:list"/>
            <xsl:variable name="node-set2" select="./following-sibling::dri:item[1]/following-sibling::dri:list"/>
            <xsl:apply-templates />
            <xsl:apply-templates select="$node-set1[count(.|$node-set2) != count($node-set2)]"/>
        </li>
    </xsl:template>


    <!-- Finally, the following templates match list types not mentioned above. They work for lists of type
        'simple' as well as any unknown list types. -->
    <xsl:template match="dri:list" priority="1">
        <xsl:apply-templates select="dri:head"/>
        <ul>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates select="*[not(name()='head')]" mode="nested"/>
        </ul>
    </xsl:template>


    <!-- Generic label handling: simply place the text of the element followed by a period and space. -->
    <xsl:template match="dri:label" priority="1" mode="nested">
        <xsl:copy-of select="./node()"/>
    </xsl:template>

    <!-- Generic item handling for cases where nothing special needs to be done -->
    <xsl:template match="dri:item" mode="nested">
        <li>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-simple-list-item</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </li>
    </xsl:template>

    <xsl:template match="dri:list/dri:list" priority="1" mode="nested">
        <li>
            <xsl:apply-templates select="."/>
        </li>
    </xsl:template>



    <!-- From here on out come the templates for supporting elements that are contained within structural
        ones. These include head (in all its myriad forms), rich text container elements (like hi and figure),
        as well as the field tag and its related elements. The head elements are done first. -->

    <!-- The first (and most complex) case of the header tag is the one used for divisions. Since divisions can
        nest freely, their headers should reflect that. Thus, the type of HTML h tag produced depends on how
        many divisions the header tag is nested inside of. -->
    <!-- The font-sizing variable is the result of a linear function applied to the character count of the heading text -->
    <xsl:template match="dri:div/dri:head" priority="3">
        <xsl:variable name="head_count" select="count(ancestor::dri:div)"/>
        <!-- with the help of the font-sizing variable, the font-size of our header text is made continuously variable based on the character count -->
        <xsl:variable name="font-sizing" select="365 - $head_count * 80 - string-length(current())"></xsl:variable>
        <xsl:element name="h{$head_count}">
            <!-- in case the chosen size is less than 120%, don't let it go below. Shrinking stops at 120% -->
            <xsl:choose>
                <xsl:when test="$font-sizing &lt; 120">
                    <xsl:attribute name="style">font-size: 120%;</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="style">font-size: <xsl:value-of select="$font-sizing"/>%;</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-div-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </xsl:element>
    </xsl:template>

    <!-- The second case is the header on tables, which always creates an HTML h3 element -->
    <xsl:template match="dri:table/dri:head" priority="2">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-table-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>

    <!-- The third case is the header on lists, which creates an HTML h3 element for top level lists and
        and h4 elements for all sublists. -->
    <xsl:template match="dri:list/dri:head" priority="2" mode="nested">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-list-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>

    <xsl:template match="dri:list/dri:list/dri:head" priority="3" mode="nested">
        <h4>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-sublist-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h4>
    </xsl:template>

    <!-- The fourth case is the header on referenceSets, to be discussed below, which creates an HTML h2 element
        for all cases. The reason for this simplistic approach has to do with referenceSets being handled
        differently in many cases, making it difficult to treat them as either divs (with scaling headers) or
        lists (with static ones). In this case, the simplest solution was chosen, although it is subject to
        change in the future. -->
    <xsl:template match="dri:referenceSet/dri:head" priority="2">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-list-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>

    <!-- Finally, the generic header element template, given the lowest priority, is there for cases not
        covered above. It assumes nothing about the parent element and simply generates an HTML h3 tag -->
    <xsl:template match="dri:head" priority="1">
        <h3>
            <xsl:call-template name="standardAttributes">
                <xsl:with-param name="class">ds-head</xsl:with-param>
            </xsl:call-template>
            <xsl:apply-templates />
        </h3>
    </xsl:template>




    <!-- Next come the components of rich text containers, namely: hi, xref, figure and, in case of interactive
        divs, field. All these can mix freely with text as well as contain text of their own. The templates for
        the first three elements are fairly straightforward, as they simply create HTML span, a, and img tags,
        respectively. -->

    <xsl:template match="dri:hi">
        <span>
            <xsl:attribute name="class">emphasis</xsl:attribute>
            <xsl:if test="@rend">
                <xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            </xsl:if>
            <xsl:apply-templates />
        </span>
    </xsl:template>

    <xsl:template match="dri:xref">
        <a>
            <xsl:if test="@target">
                <xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="@rend">
                <xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="@n">
                <xsl:attribute name="name"><xsl:value-of select="@n"/></xsl:attribute>
            </xsl:if>

            <xsl:if test="@onclick">
                <xsl:attribute name="onclick"><xsl:value-of select="@onclick"/></xsl:attribute>
            </xsl:if>

            <xsl:apply-templates />
        </a>
    </xsl:template>

    <xsl:template match="dri:figure">
        <xsl:if test="@target">
            <a>
                <xsl:attribute name="href"><xsl:value-of select="@target"/></xsl:attribute>
                <xsl:if test="@title">
                	<xsl:attribute name="title"><xsl:value-of select="@title"/></xsl:attribute>
                </xsl:if>
                <xsl:if test="@rend">
                	<xsl:attribute name="class"><xsl:value-of select="@rend"/></xsl:attribute>
                </xsl:if>
                <img>
                    <xsl:attribute name="src"><xsl:value-of select="@source"/></xsl:attribute>
                    <xsl:attribute name="alt"><xsl:apply-templates /></xsl:attribute>
                </img>
                <xsl:attribute name="border"><xsl:text>none</xsl:text></xsl:attribute>
            </a>
        </xsl:if>
        <xsl:if test="not(@target)">
            <img>
                <xsl:attribute name="src"><xsl:value-of select="@source"/></xsl:attribute>
                <xsl:attribute name="alt"><xsl:apply-templates /></xsl:attribute>
            </img>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
