/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.jsptag;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.IViewer;
import org.dspace.app.util.MetadataExposure;
import org.dspace.app.webui.jsptag.DisplayItemMetadataUtils.DisplayMetadata;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

/**
 * <P>
 * JSP tag for displaying an item.
 * </P>
 * <P>
 * The fields that are displayed can be configured in <code>dspace.cfg</code>
 * using the <code>webui.itemdisplay.(style)</code> property. The form is
 * </P>
 * 
 * <PRE>
 * 
 * &lt;schema prefix&gt;.&lt;element&gt;[.&lt;qualifier&gt;|.*][(date)|(link)], ...
 * 
 * </PRE>
 * 
 * <P>
 * For example:
 * </P>
 * 
 * <PRE>
 * 
 * dc.title = Dublin Core element 'title' (unqualified)
 * dc.title.alternative = DC element 'title', qualifier 'alternative'
 * dc.title.* = All fields with Dublin Core element 'title' (any or no qualifier)
 * dc.identifier.uri(link) = DC identifier.uri, render as a link
 * dc.date.issued(date) = DC date.issued, render as a date
 * dc.identifier.doi(doi) = DC identifier.doi, render as link to http://dx.doi.org
 * dc.identifier.hdl(handle) = DC identifier.hanlde, render as link to http://hdl.handle.net
 * dc.relation.isPartOf(resolver) = DC relation.isPartOf, render as link to the base url of the resolver 
 *                                  according to the specified urn in the metadata value (doi:xxxx, hdl:xxxxx, 
 *                                  urn:issn:xxxx, etc.)
 * 
 * </PRE>
 * 
 * <P>
 * When using "resolver" in webui.itemdisplay to render identifiers as
 * resolvable links, the base URL is taken from
 * <code>webui.resolver.<n>.baseurl</code> where
 * <code>webui.resolver.<n>.urn</code> matches the urn specified in the metadata
 * value. The value is appended to the "baseurl" as is, so the baseurl need to
 * end with slash almost in any case. If no urn is specified in the value it
 * will be displayed as simple text.
 * 
 * <PRE>
 * 
 * webui.resolver.1.urn = doi
 * webui.resolver.1.baseurl = http://dx.doi.org/
 * webui.resolver.2.urn = hdl
 * webui.resolver.2.baseurl = http://hdl.handle.net/
 * 
 * </PRE>
 * 
 * For the doi and hdl urn defaults values are provided, respectively
 * http://dx.doi.org/ and http://hdl.handle.net/ are used.<br>
 * 
 * If a metadata value with style: "doi", "handle" or "resolver" matches a URL
 * already, it is simply rendered as a link with no other manipulation.
 * </P>
 * 
 * <PRE>
 * 
 * <P>
 * If an item has no value for a particular field, it won't be displayed. The
 * name of the field for display will be drawn from the current UI dictionary,
 * using the key:
 * </P>
 * 
 * <PRE>
 * 
 * &quot;metadata.&lt;style.&gt;.&lt;field&gt;&quot;
 * 
 * e.g. &quot;metadata.thesis.dc.title&quot; &quot;metadata.thesis.dc.contributor.*&quot;
 * &quot;metadata.thesis.dc.date.issued&quot;
 * 
 * 
 * if this key is not found will be used the more general one
 * 
 * &quot;metadata.&lt;field&gt;&quot;
 * 
 * e.g. &quot;metadata.dc.title&quot; &quot;metadata.dc.contributor.*&quot;
 * &quot;metadata.dc.date.issued&quot;
 * 
 * </PRE>
 * 
 * <P>
 * You need to specify which strategy use for select the style for an item.
 * </P>
 * 
 * <PRE>
 * 
 * plugin.single.org.dspace.app.webui.util.StyleSelection = \
 *                      org.dspace.app.webui.util.CollectionStyleSelection
 *                      #org.dspace.app.webui.util.MetadataStyleSelection
 * 
 * </PRE>
 * 
 * <P>
 * With the Collection strategy you can also specify which collections use which
 * views.
 * </P>
 * 
 * <PRE>
 * 
 * webui.itemdisplay.&lt;style&gt;.collections = &lt;collection handle&gt;, ...
 * 
 * </PRE>
 * 
 * <P>
 * FIXME: This should be more database-driven
 * </P>
 * 
 * <PRE>
 * 
 * webui.itemdisplay.thesis.collections = 123456789/24, 123456789/35
 * 
 * </PRE>
 * 
 * <P>
 * With the Metadata strategy you MUST specify which metadata use as name of the
 * style.
 * </P>
 * 
 * <PRE>
 * 
 * webui.itemdisplay.metadata-style = schema.element[.qualifier|.*]
 * 
 * e.g. &quot;dc.type&quot;
 * 
 * </PRE>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class ItemTag extends TagSupport {
	private static final String HANDLE_DEFAULT_BASEURL = "http://hdl.handle.net/";

	private static final String DOI_DEFAULT_BASEURL = "http://dx.doi.org/";

	/** Item to display */
	private transient Item item;

	/** Collections this item appears in */
	private transient Collection[] collections;

	/** The style to use - "default" or "full" */
	private String style;

	/** Whether to show preview thumbs on the item page */
	private boolean showThumbs;

	/** log4j logger */
	private static Logger log = Logger.getLogger(ItemTag.class);

	private static final long serialVersionUID = -3841266490729417240L;

	public ItemTag() {
		super();
		getThumbSettings();
	}

	public int doStartTag() throws JspException {
		try {
			if (style != null && style.equals("full")) {
				renderFull();
			} else {
				render();
			}
		} catch (SQLException sqle) {
			throw new JspException(sqle);
		} catch (IOException ie) {
			throw new JspException(ie);
		}

		return SKIP_BODY;
	}

	/**
	 * Get the item this tag should display
	 * 
	 * @return the item
	 */
	public Item getItem() {
		return item;
	}

	/**
	 * Set the item this tag should display
	 * 
	 * @param itemIn
	 *            the item to display
	 */
	public void setItem(Item itemIn) {
		item = itemIn;
	}

	/**
	 * Get the collections this item is in
	 * 
	 * @return the collections
	 */
	public Collection[] getCollections() {
		return (Collection[]) ArrayUtils.clone(collections);
	}

	/**
	 * Set the collections this item is in
	 * 
	 * @param collectionsIn
	 *            the collections
	 */
	public void setCollections(Collection[] collectionsIn) {
		collections = (Collection[]) ArrayUtils.clone(collectionsIn);
	}

	/**
	 * Get the style this tag should display
	 * 
	 * @return the style
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * Set the style this tag should display
	 * 
	 * @param styleIn
	 *            the Style to display
	 */
	public void setStyle(String styleIn) {
		style = styleIn;
	}

	public void release() {
		style = "default";
		item = null;
		collections = null;
	}

	/**
	 * Render an item in the given style
	 */
	private void render() throws IOException, SQLException, JspException {
		JspWriter out = pageContext.getOut();
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		Context context = UIUtil.obtainContext(request);

		out.println("<table class=\"table itemDisplayTable\">");

		for (DisplayMetadata display : DisplayItemMetadataUtils.getDisplayMetadata(context, request, item, style)) {
			out.print("<td class=\"metadataFieldLabel\">");
			out.print(display.label);
			out.print(":&nbsp;</td><td class=\"metadataFieldValue\">");
			out.print(display.value);
			out.println("</td></tr>");
		}

		listCollections();

		out.println("</table><br/>");

		listBitstreams();

		if (ConfigurationManager.getBooleanProperty("webui.licence_bundle.show"))

		{
			out.println("<br/><br/>");
			showLicence();
		}
	}

	/**
	 * Render full item record
	 */
	private void renderFull() throws IOException, SQLException {
		JspWriter out = pageContext.getOut();
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		Context context = UIUtil.obtainContext(request);

		// Get all the metadata
		Metadatum[] values = item.getMetadataWithoutPlaceholder(Item.ANY, Item.ANY, Item.ANY, Item.ANY);

		// Three column table - DC field, value, language
		out.println("<table class=\"table itemDisplayTable\">");
		out.println("<tr><th id=\"s1\" class=\"standard\">"
				+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.dcfield")
				+ "</th><th id=\"s2\" class=\"standard\">"
				+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.value")
				+ "</th><th id=\"s3\" class=\"standard\">"
				+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.lang")
				+ "</th></tr>");

		for (int i = 0; i < values.length; i++) {
			if (!MetadataExposure.isHidden(context, values[i].schema, values[i].element, values[i].qualifier)) {
				out.print("<tr><td headers=\"s1\" class=\"metadataFieldLabel\">");
				out.print(values[i].schema);
				out.print("." + values[i].element);

				if (values[i].qualifier != null) {
					out.print("." + values[i].qualifier);
				}

				out.print("</td><td headers=\"s2\" class=\"metadataFieldValue\">");
				out.print(Utils.addEntities(values[i].value));
				out.print("</td><td headers=\"s3\" class=\"metadataFieldValue\">");

				if (values[i].language == null) {
					out.print("-");
				} else {
					out.print(values[i].language);
				}

				out.println("</td></tr>");
			}
		}

		listCollections();

		out.println("</table>");

		listBitstreams();

		if (ConfigurationManager.getBooleanProperty("webui.licence_bundle.show")) {
			showLicence();
		}
	}

	/**
	 * List links to collections if information is available
	 */
	private void listCollections() throws IOException {
		JspWriter out = pageContext.getOut();
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

		if (collections != null) {
			out.print("<tr><td class=\"metadataFieldLabel\">");
			if (item.getHandle() == null) // assume workspace item
			{
				out.print(LocaleSupport.getLocalizedMessage(pageContext,
						"org.dspace.app.webui.jsptag.ItemTag.submitted"));
			} else {
				out.print(
						LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.appears"));
			}
			out.print("</td><td class=\"metadataFieldValue\"" + (style.equals("full") ? "colspan=\"2\"" : "") + ">");

			for (int i = 0; i < collections.length; i++) {
				out.print("<a href=\"");
				out.print(request.getContextPath());
				out.print("/handle/");
				out.print(collections[i].getHandle());
				out.print("\">");
				out.print(collections[i].getMetadata("name"));
				out.print("</a><br/>");
			}

			out.println("</td></tr>");
		}
	}

	/**
	 * List bitstreams in the item
	 */
	private void listBitstreams() throws IOException {
		JspWriter out = pageContext.getOut();
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		Locale locale = UIUtil.getSessionLocale(request);
		
		try {
			Bundle[] bundles = item.getBundles("ORIGINAL");

			boolean filesExist = false;

			for (Bundle bnd : bundles) {
				filesExist = bnd.getBitstreams().length > 0;
				if (filesExist) {
					break;
				}
			}

			// if user already has uploaded at least one file
			if (filesExist) {
				out.print("<div class=\"panel panel-default\">");
				out.println("<div class=\"panel-heading\"><h6 class=\"panel-title\">"
						+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.files")
						+ "</h6></div>");

				boolean html = false;
				String handle = item.getHandle();
				Bitstream primaryBitstream = null;

				Bundle[] bunds = item.getBundles("ORIGINAL");
				Bundle[] thumbs = item.getBundles("THUMBNAIL");

				// if item contains multiple bitstreams, display bitstream
				// description
				boolean multiFile = false;
				Bundle[] allBundles = item.getBundles();

				for (int i = 0, filecount = 0; (i < allBundles.length) && !multiFile; i++) {
					filecount += allBundles[i].getBitstreams().length;
					multiFile = (filecount > 1);
				}

				// check if primary bitstream is html
				if (bunds[0] != null) {
					Bitstream[] bits = bunds[0].getBitstreams();

					for (int i = 0; (i < bits.length) && !html; i++) {
						if (bits[i].getID() == bunds[0].getPrimaryBitstreamID()) {
							html = bits[i].getFormat().getMIMEType().equals("text/html");
							primaryBitstream = bits[i];
						}
					}
				}

				out.println("<table class=\"table panel-body\"><tr><th id=\"t1\" class=\"standard\">"
						+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.file")
						+ "</th>");

				if (multiFile) {

					out.println("<th id=\"t2\" class=\"standard\">" + LocaleSupport.getLocalizedMessage(pageContext,
							"org.dspace.app.webui.jsptag.ItemTag.description") + "</th>");
				}

				out.println("<th id=\"t3\" class=\"standard\">"
						+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.filesize")
						+ "</th><th id=\"t4\" class=\"standard\">" + LocaleSupport.getLocalizedMessage(pageContext,
								"org.dspace.app.webui.jsptag.ItemTag.fileformat"));
				
				/////////
				Context context = UIUtil.obtainContext(request);
				EPerson user = context.getCurrentUser();
				if (user == null) {
					// if no user logged in and no anonymous read
					Bitstream[] bitstreams = bundles[0].getBitstreams();
					boolean authorizedToView = AuthorizeManager.authorizeActionBoolean(context,bitstreams[0], Constants.READ);
					if (!authorizedToView) {
						out.println("</th><th style=\"text-align: center;\">");
						out.print(LocaleSupport.getLocalizedMessage(pageContext,
								"org.dspace.app.webui.jsptag.ItemTag.login-existent-user"));
						out.print(" <a class=\" btn btn-primary\" ");
						out.print("href=\"" +request.getContextPath()+"/login-in-page?url="+request.getContextPath()+"/handle/" + handle+"\"");
						out.print(LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.login"));
						out.print("</a>");
						out.println("</th></tr>");
					}
				}else {
					out.print("</th><th>&nbsp;</th></tr>");
				}
				// if primary bitstream is html, display a link for only that one to
				// HTMLServlet
				if (html) {
					// If no real Handle yet (e.g. because Item is in workflow)
					// we use the 'fake' Handle db-id/1234 where 1234 is the
					// database ID of the item.
					if (handle == null) {
						handle = "db-id/" + item.getID();
					}

					out.print("<tr><td headers=\"t1\" class=\"standard\">");
					out.print("<a target=\"_blank\" href=\"");
					out.print(request.getContextPath());
					out.print("/html/");
					out.print(handle + "/");
					out.print(UIUtil.encodeBitstreamName(primaryBitstream.getName(), Constants.DEFAULT_ENCODING));
					out.print("\">");
					out.print(primaryBitstream.getName());
					out.print("</a>");

					if (multiFile) {
						out.print("</td><td headers=\"t2\" class=\"standard\">");

						String desc = primaryBitstream.getDescription();
						out.print((desc != null) ? desc : "");
					}

					out.print("</td><td headers=\"t3\" class=\"standard\">");
					out.print(UIUtil.formatFileSize(primaryBitstream.getSize()));
					out.print("</td><td headers=\"t4\" class=\"standard\">");
					out.print(primaryBitstream.getFormatDescription());
					out.print("</td><td class=\"standard\"><a class=\"btn btn-primary\" target=\"_blank\" href=\"");
					out.print(request.getContextPath());
					out.print("/html/");
					out.print(handle + "/");
					out.print(UIUtil.encodeBitstreamName(primaryBitstream.getName(), Constants.DEFAULT_ENCODING));
					out.print("\">"
							+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.view")
							+ "</a></td></tr>");
				} else {
//					Context context = UIUtil.obtainContext(request);
					boolean showRequestCopy = false;
					if ("all".equalsIgnoreCase(ConfigurationManager.getProperty("request.item.type"))
							|| ("logged".equalsIgnoreCase(ConfigurationManager.getProperty("request.item.type"))
									&& context.getCurrentUser() != null)) {
						showRequestCopy = true;
					}

					for (int i = 0; i < bundles.length; i++) {
						int primaryBitID = bundles[i].getPrimaryBitstreamID();
						Bitstream primaryBit = null;
						if (primaryBitID != -1) {
							primaryBit = Bitstream.find(context, primaryBitID);
						}

						Bitstream[] bitstreams;
						if (primaryBit != null
								&& hideNotPrimaryBitstreams(context, request, pageContext, handle, primaryBit)) {
							bitstreams = new Bitstream[] { primaryBit };
						} else {
							bitstreams = bundles[i].getBitstreams();
						}

						for (int k = 0; k < bitstreams.length; k++) {
							// Skip internal types
							if (!bitstreams[k].getFormat().isInternal()) {
								List<ViewOption> viewOptions = getViewOptions(context, request, pageContext, handle,
										bitstreams[k]);

								// Work out what the bitstream link should be
								// (persistent
								// ID if item has Handle)
								String bsLink = "target=\"_blank\" href=\"";
								bsLink = bsLink + viewOptions.get(0).link;
								bsLink = bsLink + "\">";

								out.print("<tr><td headers=\"t1\" class=\"standard\">");
								out.print("<a ");
								out.print(bsLink);
								out.print(bitstreams[k].getName());
								out.print("</a>");

								if (multiFile) {
									out.print("</td><td headers=\"t2\" class=\"standard\">");

									String desc = bitstreams[k].getDescription();
									out.print((desc != null) ? desc : "");
								}

								out.print("</td><td headers=\"t3\" class=\"standard\">");
								out.print(UIUtil.formatFileSize(bitstreams[k].getSize()));
								out.print("</td><td headers=\"t4\" class=\"standard\">");
								out.print(bitstreams[k].getFormatDescription());
								out.print("</td><td class=\"standard\" align=\"center\">");

								// is there a thumbnail bundle?
								if ((thumbs.length > 0) && showThumbs) {
									String tName = bitstreams[k].getName() + ".jpg";
									String tAltText = LocaleSupport.getLocalizedMessage(pageContext,
											"org.dspace.app.webui.jsptag.ItemTag.thumbnail");
									Bitstream tb = thumbs[0].getBitstreamByName(tName);

									if (tb != null) {
										if (AuthorizeManager.authorizeActionBoolean(context, tb, Constants.READ)) {
											String myPath = request.getContextPath() + "/retrieve/" + tb.getID() + "/"
													+ UIUtil.encodeBitstreamName(tb.getName(),
															Constants.DEFAULT_ENCODING);

											out.print("<a ");
											out.print(bsLink);
											out.print("<img src=\"" + myPath + "\" ");
											out.print("alt=\"" + tAltText + "\" /></a><br />");
										}
									}
								}

								boolean authorizedToVew = AuthorizeManager.authorizeActionBoolean(context,
										bitstreams[k], Constants.READ);

								boolean ecommerceEnabled = ConfigurationManager.getBooleanProperty("ecommerce",
										"ecommerce.enabled", false);

								boolean canBuy = false;
								boolean isSale = false;
								String ean = "";
								if (ecommerceEnabled) {
									String saleSchema = ConfigurationManager.getProperty("ecommerce", "ecommerce.sale");
									String eanSchema = ConfigurationManager.getProperty("ecommerce", "ecommerce.ean");

									ean = item.getMetadata(eanSchema);

									String[] sale = Utils.tokenize(saleSchema);
									Metadatum[] metadatumArray = item.getMetadata(sale[0], sale[1], null, Item.ANY);

									if (metadatumArray != null && metadatumArray.length > 0) {
										isSale = true;
									}
//									EPerson user = context.getCurrentUser();
									Group[] groupList;
									groupList = Group.allMemberGroups(context, user);
									for (Metadatum m : metadatumArray) {
										String val = m.value;
										Group g;
										if (StringUtils.isNumeric(val)) {
											g = Group.find(context, Integer.parseInt(val));
										} else {
											g = Group.findByName(context, val);
										}
										if (StringUtils.equals(val, "Anonymous")) {
											canBuy = true;
											break;
										} else if (ArrayUtils.contains(groupList, g)) {
											canBuy = true;
											break;
										}
									}
								}
								if (authorizedToVew) {
									if (viewOptions.size() == 1) {
										out.print("<a class=\"btn btn-primary\" ");
										out.print(bsLink + viewOptions.get(0).label + "</a>");
									} else {
										out.println("&nbsp;&nbsp;<div class=\"btn-group\">");
										out.print("<a class=\"btn btn-primary\" href=\"" + viewOptions.get(0).link
												+ "\">");
										out.print(viewOptions.get(0).label);
										out.println("</a>");

										out.print(
												"<button type=\"button\" class=\"btn btn-primary dropdown-toggle\" data-toggle=\"dropdown\" "
														+ " aria-haspopup=\"true\" aria-expanded=\"false\"> "
														+ " <span class=\"caret\"></span> <span class=\"sr-only\">Toggle Dropdown</span> </button>");
										out.print("<ul class=\"dropdown-menu\"> ");

										for (int idx = 1; idx < viewOptions.size() - 1; idx++) {
											out.print("<li><a href=\"" + viewOptions.get(idx).link + "\">");
											out.print(viewOptions.get(0).label);
											out.print("</a></li>");
										}

										if (viewOptions.size() > 2) {
											out.print("<li role=\"separator\" class=\"divider\"></li> ");
										}
										out.print("<li><a href=\"" + viewOptions.get(viewOptions.size() - 1).link
												+ "\">");
										out.print(viewOptions.get(viewOptions.size() - 1).label);
										out.print("</a></li>");
										out.print("</ul> </div>");
									}
								} else {
									// not allowed to download
									if (canBuy) {
//										EPerson user = context.getCurrentUser();
										if (viewOptions.size() > 2) {
											out.print("<li role=\"separator\" class=\"divider\"></li> ");
										}
										// see btn buy
										String metadataPermalink = ConfigurationManager.getProperty("ecommerce",
												"ecommerce.permalink");
										String permalink = "";
										if (StringUtils.isNotBlank(ean)) {
											permalink = item.getMetadata(metadataPermalink);
										}
										out.print("<a class=\"btn btn-danger\" ");
										out.print("href=\"" + permalink + "\" target=_blank");
										out.print(LocaleSupport.getLocalizedMessage(pageContext,
												"org.dspace.app.webui.jsptag.ItemTag.sale"));
										out.print("</a> ");

									} else if (isSale) {
										// see label this item can be bought by these groups
										Metadatum[] metadatumArray = item.getMetadata("ec", "sale", null, Item.ANY);
										int j = 0;
										String str = "";
										String str2 = "";
										for (Metadatum m : metadatumArray) {
											j++;
											Group g = Group.findByName(context, m.value);
											String link = g.getMetadata("ec.product.permalink");
											Object[] params = new Object[2];
											params[0] = g.getName();
											params[1] = link;
											if (link != null) {
												str += LocaleSupport.getLocalizedMessage(pageContext,
														"org.dspace.app.webui.jsptag.ItemTag.reservedsale.group",
														params);
											} else {
												str += LocaleSupport.getLocalizedMessage(pageContext,
														"org.dspace.app.webui.jsptag.ItemTag.reservedsale.group-noinfo",
														new Object[] { g.getName() });
											}

											String space = null;
											if (j == metadatumArray.length - 1) {
												space = LocaleSupport.getLocalizedMessage(pageContext,
														"org.dspace.app.webui.jsptag.ItemTag.reservedsale.group.last-separator");
												str += space;
											} else if (j < metadatumArray.length - 1) {
												space = LocaleSupport.getLocalizedMessage(pageContext,
														"org.dspace.app.webui.jsptag.ItemTag.reservedsale.group.separator");
												str += space;
											}
										}
										Object[] params2 = new Object[1];
										params2[0] = str;
										str2 = LocaleSupport.getLocalizedMessage(pageContext,
												"org.dspace.app.webui.jsptag.ItemTag.reservedsale", params2);
										out.println(str2);

									} else {
										// not authorized to access but not for sale
										Set<Group> authorizedGroups = new HashSet<Group>();
										List<ResourcePolicy> rps = AuthorizeManager.getPoliciesActionFilter(context,
												bitstreams[k], Constants.READ);
										
										boolean isEmbargo = false;
										Date embargoDate = null;
										
										for (ResourcePolicy rp : rps) {
											Group g = rp.getGroup();
											if (g != null) {
												if (Group.ANONYMOUS_ID == g.getID()) {
													// there is a policy for the anonymous group, if it is not yet valid
													// it is an embargo otherwise it was an expired lease just ignore it
													if (rp.getStartDate() != null && rp.getStartDate().after(new Date())) {
														isEmbargo = true;
														embargoDate = rp.getStartDate();
													}
												} else {
													if (rp.isDateValid()) {
														authorizedGroups.add(g);
													}
												}
											}
										}

										if (!isEmbargo) {
											if (ConfigurationManager.getBooleanProperty("webui.itemtag.show-reserved-group", false)) {
												StringBuffer sb = new StringBuffer();
												int j = 0;
												for (Group g : authorizedGroups) {
													j++;
													
													String link = g.getMetadata("ec.product.permalink");
													if (link != null) {
														Object[] params = new Object[] { g.getName(), link };
														sb.append(LocaleSupport.getLocalizedMessage(pageContext,
																"org.dspace.app.webui.jsptag.ItemTag.reservedsale.group",
																params));
													} else {
														sb.append(LocaleSupport.getLocalizedMessage(pageContext,
																"org.dspace.app.webui.jsptag.ItemTag.reservedsale.group-noinfo",
																new Object[] { g.getName() }));
													}

													if (j == authorizedGroups.size() - 1) {
														sb.append(LocaleSupport.getLocalizedMessage(pageContext,
																"org.dspace.app.webui.jsptag.ItemTag.reserved.group.last-separator"));
													} else if (j < authorizedGroups.size() - 1) {
														sb.append(LocaleSupport.getLocalizedMessage(pageContext,
																"org.dspace.app.webui.jsptag.ItemTag.reserved.group.separator"));
													}
												}
		
												if (sb.length() > 0) {
													out.println(LocaleSupport.getLocalizedMessage(pageContext,
															"org.dspace.app.webui.jsptag.ItemTag.restricted-to",
															new Object[] { sb.toString() }));
												}
											}
										}
										else {
											out.append(LocaleSupport.getLocalizedMessage(pageContext,
													"org.dspace.app.webui.jsptag.ItemTag.embargo", new Object[] { 
															DateFormat.getDateInstance(DateFormat.LONG, locale).format(embargoDate)}));
										}

										if (showRequestCopy) {
											out.print(
													"&nbsp;<a class=\"btn btn-success\" href=\""
															+ request.getContextPath() + "/request-item?handle="
															+ handle + "&bitstream-id=" + bitstreams[k].getID() + "\">"
															+ LocaleSupport.getLocalizedMessage(pageContext,
																	"org.dspace.app.webui.jsptag.ItemTag.restrict")
															+ "</a>");
										}
									}
								}
							}
						}
						out.print("</td></tr>");
					}

				}

				out.println("</table>");
				out.println("</div>");

			}
		} catch (SQLException sqle) {
			throw new IOException(sqle.getMessage(), sqle);
		}
	}

	private void getThumbSettings() {
		showThumbs = ConfigurationManager.getBooleanProperty("webui.item.thumbnail.show");
	}

	/**
	 * Link to the item licence
	 */
	private void showLicence() throws IOException {
		JspWriter out = pageContext.getOut();
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

		Bundle[] bundles = null;
		try {
			bundles = item.getBundles("LICENSE");
		} catch (SQLException sqle) {
			throw new IOException(sqle.getMessage(), sqle);
		}

		out.println("<table align=\"center\" class=\"table attentionTable\"><tr>");

		out.println("<td class=\"attentionCell\"><p><strong>"
				+ LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.itemprotected")
				+ "</strong></p>");

		for (int i = 0; i < bundles.length; i++) {
			Bitstream[] bitstreams = bundles[i].getBitstreams();

			for (int k = 0; k < bitstreams.length; k++) {
				out.print("<div class=\"text-center\">");
				out.print("<strong><a class=\"btn btn-primary\" target=\"_blank\" href=\"");
				out.print(request.getContextPath());
				out.print("/retrieve/");
				out.print(bitstreams[k].getID() + "/");
				out.print(UIUtil.encodeBitstreamName(bitstreams[k].getName(), Constants.DEFAULT_ENCODING));
				out.print("\">" + LocaleSupport.getLocalizedMessage(pageContext,
						"org.dspace.app.webui.jsptag.ItemTag.viewlicence") + "</a></strong></div>");
			}
		}

		out.println("</td></tr></table>");
	}

	public static class ViewOption {
		String label;
		String link;
	}

	public static boolean hideNotPrimaryBitstreams(Context context, HttpServletRequest request, PageContext pageContext,
			String handle, Bitstream bit) throws UnsupportedEncodingException {

		List<String> hideNotPrimary = bit.getMetadataValue(IViewer.METADATA_STRING_HIDENOTPRIMARY);
		for (String h : hideNotPrimary) {
			if (BooleanUtils.toBoolean(h)) {
				return true;
			}
		}
		return false;
	}

	public static List<ViewOption> getViewOptions(Context context, HttpServletRequest request, PageContext pageContext,
			String handle, Bitstream bit) throws UnsupportedEncodingException {
		List<ViewOption> results = new ArrayList<ViewOption>();

		List<String> externalProviders = bit.getMetadataValue(IViewer.METADATA_STRING_PROVIDER);
		boolean showDownload = true;
		for (String externalProvider : externalProviders) {
			if (IViewer.STOP_DOWNLOAD.equals(externalProvider)) {
				showDownload = false;
				continue;
			}
			ViewOption opt = new ViewOption();
			opt.link = request.getContextPath() + "/explore?bitstream_id=" + bit.getID() + "&handle=" + handle
					+ "&provider=" + externalProvider;
			opt.label = LocaleSupport.getLocalizedMessage(pageContext,
					"org.dspace.app.webui.jsptag.ItemTag.explore." + externalProvider);
			results.add(opt);
		}

		if (showDownload) {
			ViewOption opt = new ViewOption();
			opt.link = request.getContextPath();

			if ((handle != null) && (bit.getSequenceID() > 0)) {
				opt.link = opt.link + "/bitstream/" + handle + "/" + bit.getSequenceID() + "/";
			} else {
				opt.link = opt.link + "/retrieve/" + bit.getID() + "/";
			}

			opt.link = opt.link + UIUtil.encodeBitstreamName(bit.getName(), Constants.DEFAULT_ENCODING);
			opt.label = LocaleSupport.getLocalizedMessage(pageContext, "org.dspace.app.webui.jsptag.ItemTag.view");
			results.add(opt);
		}
		return results;
	}
}
