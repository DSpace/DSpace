/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.SAXOutputter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * This class represents data that is translated from simple HTML or plain text.
 * 
 * <p>This class represents a simple HTML fragment. It allows for user-supplied
 * HTML to be translated on the fly into DRI.
 * 
 * <p>At the present time it only supports the following tags: h1, h2, h3, h4, h5,
 * p, a, b, i, u, ol, li and img. Each are translated into their DRI equivalents.
 * Note that the "h" tags are translated into a paragraph of rend=heading.
 * 
 * <p>If the {@code linkbreaks} flag is set then line breaks are treated as paragraphs. This
 * allows plain text files to also be included and they will be mapped into DRI as 
 * well.
 * 
 * @author Scott Phillips
 * @author Jay Paz
 */
public class SimpleHTMLFragment extends AbstractWingElement {

	/** The HTML Fragment */
	private final String fragment;

	/** Determine if blank lines mark a new paragraph */
	private final boolean blankLines;

	/**
	 * Construct a fragment object for translating into DRI.
	 * 
	 * @param context
	 *            (Required) The context this element is contained in, such as
	 *            where to route SAX events and what i18n catalogue to use.
	 * @param blankLines
	 *            (Required) Determine if blank lines should be treated as
	 *            paragraphs delimeters.
	 * @param fragment
	 *            (Required) The HTML Fragment to be translated into DRI.
	 * @throws WingException passed through.
	 */
	protected SimpleHTMLFragment(WingContext context, boolean blankLines,
			String fragment) throws WingException {
		super(context);
		this.blankLines = blankLines;
		this.fragment = fragment;
	}

	/**
	 * Translate this element into SAX
	 * 
	 * @param contentHandler
	 *            (Required) The registered contentHandler where SAX events
	 *            should be routed too.
	 * @param lexicalHandler
	 *            (Required) The registered lexicalHandler where lexical events
	 *            (such as CDATA, DTD, etc) should be routed too.
	 * @param namespaces
	 *            (Required) SAX Helper class to keep track of namespaces able
	 *            to determine the correct prefix for a given namespace URI.
     * @throws org.xml.sax.SAXException on I/O error.
	 */
    @Override
	public void toSAX(ContentHandler contentHandler,
			LexicalHandler lexicalHandler, NamespaceSupport namespaces)
			throws SAXException {
		try {
			String xml = "<fragment>" + fragment + "</fragment>";

			ByteArrayInputStream inputStream = new ByteArrayInputStream(xml
					.getBytes("UTF-8"));

			SAXBuilder builder = new SAXBuilder();
			Document document = builder.build(inputStream);

			try {
				translate(document.getRootElement());
			} catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
				throw new JDOMException(
						"Error translating HTML fragment into DRI", e);
			}

			SAXFilter filter = new SAXFilter(contentHandler, lexicalHandler,
					namespaces);
			SAXOutputter outputter = new SAXOutputter();
			outputter.setContentHandler(filter);
			outputter.setLexicalHandler(filter);

			Element root = document.getRootElement();

			@SuppressWarnings("unchecked")
			// This cast is correct
			List<Element> children = root.getChildren();
			for (Element child : children) {
				outputter.output(child);
			}

		} catch (JDOMException e) {
                        // If we are here, then a parsing error occurred within the XHTML fragment.  We'll just assume
                        // that this is not supposed to be XHTML and display the fragment as plain text within <dri:p> tags.
			startElement(contentHandler, namespaces, Para.E_PARA, null);
			sendCharacters(contentHandler, fragment);
			endElement(contentHandler, namespaces, Para.E_PARA);
		} catch (IOException ioe) {
			throw new SAXException(ioe);
		}
	}

	/**
	 * Remove the given content from the Element.
	 * 
	 * If the content is an element then render it as text and include it's
	 * children in the parent.
	 * 
	 * @param content
	 *            The DOM Content to be removed.
	 */
	private void removeContent(Content content) {
		if (content instanceof Element) {
			// If it's an element replace the content with a text node.
			Element element = (Element) content;

			if (element.getContent().isEmpty()) {
				// The element contains nothing, we can use shorthand notation
				// for it.
				StringBuilder replacement = new StringBuilder().append("<").append(element.getName());

				@SuppressWarnings("unchecked")
				// This cast is correct
				List<Attribute> attributes = element.getAttributes();
				for (Attribute attribute : attributes) {
					replacement .append(" ").append(attribute.getName()).append("=\"").append(attribute.getValue()).append("\"").toString();
				}
				replacement.append("/>");

				Element parent = element.getParentElement();
				int index = parent.indexOf(element);
				parent.setContent(index, new Text(replacement.toString()));
			} else {
				// The element contains data
				StringBuilder prepend = new StringBuilder();
				prepend.append("<").append(element.getName());

				@SuppressWarnings("unchecked")
				// This cast is correct
				List<Attribute> attributes = element.getAttributes();
				for (Attribute attribute : attributes) {
					prepend.append(" ").append(attribute.getName()).append("=\"").append(attribute.getValue()).append("\"");
				}
				prepend.append(">");

				String postpend = "</" + element.getName() + ">";

				Element parent = element.getParentElement();
				int index = parent.indexOf(element);

				parent.addContent(index, new Text(postpend));
				parent.addContent(index, element.removeContent());
				parent.addContent(index, new Text(prepend.toString()));
				parent.removeContent(element);
			}
		} else {
			// If it's not an element just remove the content from the document.
			Element parent = content.getParentElement();
			parent.removeContent(content);
		}
	}

	/**
	 * Wrap the given set of contents into a paragraph and place it at the
	 * supplied index.
	 * 
	 * This method will also check for trivial paragraphs, i.e. those that
	 * contain nothing but white space. If they are found then they are removed.
	 * 
	 * @param parent
	 *            The parent element to attach the wrapped paragraph, too.
	 * @param index
	 *            The index within the parent for where the content should be
	 *            attached.
	 * @param contents
	 *            The contents that should be wrapped in a paragraph.
	 * @return whether a paragraph was actually added.
	 */
	private boolean paragraphWrap(Element parent, int index,
			List<Content> contents) {
		if (contents == null || contents.size() <= 0)
        {
            return false;
        }

		boolean empty = true;
		for (Content content : contents) {
			if (!empty)
            {
                continue;
            }

			if (content instanceof Text) {
				Text text = (Text) content;
				if (!"".equals(text.getTextNormalize()))
                {
                    empty = false;
                }
			} else {
				empty = false;
			}
		}

		if (empty)
        {
            return false;
        }

		// May be useful for debugging:
		// contents.add(0, new Text("("+index+") "));

		Element para = new Element(Para.E_PARA);
		para.addContent(contents);
		if (index >= 0)
        {
            parent.addContent(index, para);
        }
		else
        {
            parent.addContent(para);
        }

		return true;
	}

	/**
	 * Ensure that the given element only has the supplied attributes. Also
	 * remove any possible namespaces on the attributes.
	 * 
	 * @param element
	 *            The element to be checked.
	 * @param names
	 *            A list of all allowed attribute names, all others will be
	 *            removed.
	 */
	private void limitAttributes(Element element, String... names) {
		Map<String, String> attributes = new HashMap<>();
		for (String name : names) {
			String value = element.getAttributeValue(name);
			if (value != null)
            {
                attributes.put(name, value);
            }
		}

		element.setAttributes(new ArrayList<Attributes>());

		for (Map.Entry<String, String> attr : attributes.entrySet()) {
			element.setAttribute(attr.getKey(), attr.getValue());
		}
	}

	/**
	 * Move the old attribute to a new attribute.
	 * 
	 * @param element
	 *            The element
	 * @param oldName
	 *            The old attribute's name.
	 * @param newName
	 *            The new attribute's name.
	 */
	private void moveAttribute(Element element, String oldName, String newName) {
		Attribute attribute = element.getAttribute(oldName);
		if (attribute != null)
        {
            attribute.setName(newName);
        }
	}

	/**
	 * Translate the given HTML fragment into a DRI document.
	 * 
	 * The translation is broken up into two steps, 1) recurse through all
	 * elements and either translate them into their DRI equivalents or remove
	 * them from the document.
	 * 
	 * The second step, 2) is to iterate over all top level elements and ensure
	 * that they only consist of paragraphs. Also at this stage if linkBreaks is
	 * true then \n are treated as paragraph breaks.
	 * 
	 * @param parent
	 *            The Element to translate into DRI.
	 */
	private void translate(Element parent) {
		// Step 1:
		// Recurse through all elements and either
		// translate them or remove them.
		for (int i = 0; i < parent.getContentSize(); i++) {
			Content decedent = parent.getContent(i);

			if (decedent instanceof org.jdom.Text) {

			} else if (decedent instanceof Element) {
				Element element = (Element) decedent;
				String name = element.getName();

				// First all the DRI elements, allow them to pass.
				if ("p".equals(name)) {
					// Paragraphs are tricky, it may be either an HTML
					// or DRI <p> element. However, while HTML will allow
					// <p> to nest DRI does not, thus first we need to
					// check if this is at the block level, if it is then
					// we need remove it.

					if (parent.isRootElement()) {
						// The paragraph is not nested, so translate it to
						// a DRI <p>
						moveAttribute(element, "class", "rend");
						limitAttributes(element, "id", "n", "rend");

						translate(element);
					} else {
						// The paragraph is nested which is not allowed in
						// DRI, so remove it.
						removeContent(element);
					}
				} else if ("h1".equals(name) || "h2".equals(name)
						|| "h3".equals(name) || "h4".equals(name)
						|| "h5".equals(name)) {
					// The HTML <H1> tag is translated into the DRI
					// <p rend="heading"> tag.
					if (parent.isRootElement()) {
						limitAttributes(element);
						element.setName("p");
						element.setAttribute("rend", "heading");

						translate(element);
					} else {
						// DRI paragraphs can not be nested.
						removeContent(element);
					}
				} else if ("a".equals(name)) {
					// The HTML <a> tag is translated into the DRI
					// <xref> tag.
					moveAttribute(element, "href", "target");
					limitAttributes(element, "target");
					element.setName("xref");

					translate(element);
				} else if ("ol".equals(name)) {
					// the HTML tag <ol> its translated into the DRI
					// <list> tag
					// <list type="ordered" n="list_part_one"
					// id="css.submit.LicenseAgreement.list.list_part_one">
					moveAttribute(element, "class", "rend");
					limitAttributes(element, "id", "n", "rend");
					element.setName("list");
					element.setAttribute("type", "ordered");
					translate(element);
				} else if ("li".equals(name)) {
					// the HTML tag <li> its translated into the DRI
					// <item> tag
					moveAttribute(element, "class", "rend");
					limitAttributes(element, "id", "n", "rend");
					element.setName("item");
					translate(element);
				} else if ("b".equals(name)) {
					// The HTML <b> tag is translated to a highlight
					// element with a rend of bold.
					limitAttributes(element);
					element.setName("hi");
					element.setAttribute("rend", "bold");

					translate(element);
				} else if ("i".equals(name)) {
					// The HTML <i> tag is translated to a highlight
					// element with a rend of italic.
					limitAttributes(element);
					element.setName("hi");
					element.setAttribute("rend", "italic");

					translate(element);
				} else if ("u".equals(name)) {
					// The HTML <u> tag is translated to a highlight
					// element with a rend of underline.
					limitAttributes(element);
					element.setName("hi");
					element.setAttribute("rend", "underline");

					translate(element);
				} else if ("img".equals(name)) {
					// The HTML <img> element is translated into a DRI figure
					moveAttribute(element, "src", "source");
					limitAttributes(element, "source");
					element.setName("figure");

					translate(element);
				}
				// Next all the DRI elements that we allow to pass through.
				else if ("hi".equals(name)) {
					limitAttributes(element, "rend");

					translate(element);
				} else if ("xref".equals(name)) {
					limitAttributes(element, "target");

					translate(element);
				} else if ("figure".equals(name)) {
					limitAttributes(element, "rend", "source", "target");

					translate(element);
				} else {
					removeContent(decedent);
				}
			} else {
				removeContent(decedent);
			}
		}

		// Step 2:
		// Ensure that all top level elements are encapsulated inside
		// a block level element (i.e. a paragraph)
		if (parent.isRootElement()) {
			List<Content> removed = new ArrayList<>();
			for (int i = 0; i < parent.getContentSize(); i++) {
				Content current = parent.getContent(i);
				
				if ((current instanceof Element)
						&& ("p".equals(((Element) current).getName()))) {
					// A paragraph is being open, combine anything up to this
					// point into a paragraph.
					if (paragraphWrap(parent, i, removed)) {
						removed.clear();
						i++; // account for the field added
					}
				} else if ((current instanceof Element)
						&& ("list".equals(((Element) current).getName()))) {
					if (paragraphWrap(parent, i, removed)) {
						removed.clear();
						i++; // account for the field added
					}
				} else {
					// If we break paragraphs based upon blank lines then we
					// need to check if
					// there are any in this text element.
					if (this.blankLines && current instanceof Text) {
						String rawText = ((Text) current).getText();
						parent.removeContent(current);
						i--;// account text field removed.

						// Regular expression to split based upon blank lines.
						// FIXME: This may not work for windows people who
						// insist on using \r\n for line breaks.
						@SuppressWarnings("unchecked")
                        String[] parts = rawText.split("\n\\s*\n");
                        if (parts.length > 0) {
                            for (int partIdx = 0; partIdx < parts.length - 1; partIdx++) {
                                removed.add(new Text(parts[partIdx]));

                                if (paragraphWrap(parent, i+1, removed)) {
                                    removed.clear();
                                    i++;// account for the field added
                                }
                            }

                            removed.add(new Text(parts[parts.length - 1]));
                        }
					} else {
						removed.add(current);
						parent.removeContent(current);
						i--; // move back to account for the removed content.
					}
				}
			}

			// if anything is left, wrap it up in a para also.
			if (removed.size() > 0) {
				paragraphWrap(parent, -1, removed);
				removed.clear();
			}
		}
	}

	/**
	 * This is a simple SAX Handler that filters out start and end documents.
	 * This class is needed for two reasons, 1) namespaces need to be corrected
	 * from the originating HTML fragment, 2) to get around a JDOM bug where it
	 * can not output SAX events for just a document fragment. Since it only
	 * works with documents this class was created to filter out the events.
	 *
	 * <p>As far as I can tell, the first time the bug was identified is in the
	 * following email, point #1:
	 *
	 * <a href="http://www.servlets.com/archive/servlet/ReadMsg?msgId=491592&listName=jdom-interest">
     *   {@literal http://www.servlets.com/archive/servlet/ReadMsg?msgId=491592&listName=jdom-interest}
     * </a>
	 *
	 * <p>I, Scott Phillips, checked the JDOM CVS source tree on 3-8-2006 and the
	 * bug had not been patched at that time.
	 */
	public static class SAXFilter implements ContentHandler, LexicalHandler {

		private final String URI = WingConstants.DRI.URI;

		private final ContentHandler contentHandler;

		// private LexicalHandler lexicalHandler; may be used in the future
		private final NamespaceSupport namespaces;

		public SAXFilter(ContentHandler contentHandler,
				LexicalHandler lexicalHandler, NamespaceSupport namespaces) {
			this.contentHandler = contentHandler;
			// this.lexicalHandler = lexicalHandler;
			this.namespaces = namespaces;
		}

		/**
		 * Create the qName for the element with the given localName and
		 * namespace prefix.
		 * 
		 * @param localName
		 *            (Required) The element's local name.
		 * @return
		 */
		private String qName(String localName) {
			String prefix = namespaces.getPrefix(URI);

			if (prefix == null || prefix.equals(""))
            {
                return localName;
            }
			else
            {
                return prefix + ":" + localName;
            }
		}

		/** ContentHandler methods: */

        @Override
		public void endDocument() {
			// Filter out endDocument events
		}

        @Override
		public void startDocument() {
			// filter out startDocument events
		}

        @Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			contentHandler.characters(ch, start, length);
		}

        @Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {

			contentHandler.endElement(URI, localName, qName(localName));
		}

        @Override
		public void endPrefixMapping(String prefix) throws SAXException {
			// No namespaces may be declared.
		}

        @Override
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
			contentHandler.ignorableWhitespace(ch, start, length);
		}

        @Override
		public void processingInstruction(String target, String data)
				throws SAXException {
			// filter out processing instructions
		}

        @Override
		public void setDocumentLocator(Locator locator) {
			// filter out document locators
		}

        @Override
		public void skippedEntity(String name) throws SAXException {
			contentHandler.skippedEntity(name);
		}

        @Override
		public void startElement(String uri, String localName, String qName,
				Attributes atts) throws SAXException {
			contentHandler.startElement(URI, localName, qName(localName), atts);
		}

        @Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
			// No namespaces can be declared.
		}

		/* Lexical Handler methods: */

        @Override
		public void startDTD(String name, String publicId, String systemId)
				throws SAXException {
			// filter out DTDs
		}

        @Override
		public void endDTD() throws SAXException {
			// filter out DTDs
		}

        @Override
		public void startEntity(String name) throws SAXException {
			// filter out Entities
		}

        @Override
		public void endEntity(String name) throws SAXException {
			// filter out Entities
		}

        @Override
		public void startCDATA() throws SAXException {
			// filter out CDATA
		}

        @Override
		public void endCDATA() throws SAXException {
			// filter out CDATA
		}

        @Override
		public void comment(char[] ch, int start, int length)
				throws SAXException {
			// filter out comments;
		}
	}
}
 
