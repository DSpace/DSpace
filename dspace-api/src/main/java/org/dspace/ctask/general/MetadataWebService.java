/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.XMLConstants;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;

import org.dspace.content.MetadataValue;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Mutative;
import org.dspace.curate.Suspendable;

/**
 * MetadataWebService task calls a web service using metadata from
 * passed item to obtain data. Depending on configuration, this
 * data may be assigned to item metadata fields, or just recorded in the
 * task result string. Task succeeds if web service call succeeds and 
 * configured updates occur, fails if task user not authorized or item
 * lacks metadata to call service, and returns error in all other cases
 * (except skip status for non-item objects).
 * Intended use: cataloging tool in workflow and general curation.
 * The task uses a URL 'template' to compose the service call, e.g.
 * 
 * {@code http://www.sherpa.ac.uk/romeo/api29.php?issn=\{dc.identifier.issn\}}
 * 
 * Task will substitute the value of the passed item's metadata field
 * in the {parameter} position. If multiple values are present in the
 * item field, the first value is used.
 * 
 * The task uses another property (the datamap) to determine what data
 * to extract from the service response and how to use it, e.g.
 * 
 * {@code //publisher/name=>dc.publisher,//romeocolour}
 * 
 * Task will evaluate the left-hand side (or entire token) of each
 * comma-separated token in the property as an XPath 1.0 expression into
 * the response document, and if there is a mapping symbol (e.g. {@code '=>'}) and
 * value, it will assign the response document value(s) to the named
 * metadata field in the passed item. If the response document contains
 * multiple values, they will all be assigned to the item field. The
 * mapping symbol governs the nature of metadata field assignment:
 * 
 * {@code '->'} mapping will add to any existing values in the item field
 * {@code '=>'} mapping will replace any existing values in the item field
 * {@code '~>'} mapping will add *only* if item field has no existing values
 * 
 * Unmapped data (without a mapping symbol) will simply be added to the task
 * result string, prepended by the XPath expression (a little prettified).
 * Each label/value pair in the result string is separated by a space, 
 * unless the optional 'separator' property is defined.
 * 
 * A very rudimentary facility for transformation of data is supported, e.g.
 * 
 * {@code http://www.crossref.org/openurl/?id=\{doi:dc.relation.isversionof\}&format=unixref}
 *
 * The 'doi:' prefix will cause the task to look for a 'transform' with that
 * name, which is applied to the metadata value before parameter substitution
 * occurs. Transforms are defined in a task property such as the following:
 * 
 * {@code transform.doi = match 10. trunc 60}
 * 
 * This means exclude the value string up to the occurrence of '10.', then
 * truncate after 60 characters. The only transform functions currently defined:
 * 
 * {@code 'cut' <number>} = remove number leading characters
 * {@code 'trunc' <number>} = remove trailing characters after number length
 * {@code 'match' <pattern>} = start match at pattern
 * {@code 'text' <characters>} = append literal characters (enclose in ' ' when whitespace needed)
 * 
 * If the transform results in an invalid state (e.g. cutting more characters
 * than are in the value), the condition will be logged and the 
 * un-transformed value used.
 *
 * Transforms may also be used in datamaps, e.g.
 * 
 * {@code //publisher/name=>shorten:dc.publisher,//romeocolour}
 *  
 * which would apply the 'shorten' transform to the service response value(s)
 * prior to metadata field assignment.
 *
 * An optional property 'headers' may be defined to stipulate any HTTP headers
 * required in the service call. The property syntax is double-pipe separated headers:
 * 
 * {@code Accept: text/xml||Cache-Control: no-cache}
 * 
 * @author richardrodgers
 */
@Mutative
@Suspendable
public class MetadataWebService extends AbstractCurationTask implements NamespaceContext
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(MetadataWebService.class);
    // transform token parsing pattern
	protected Pattern ttPattern = Pattern.compile("\'([^\']*)\'|(\\S+)");
    // URL of web service with template parameters
	protected String urlTemplate = null;
    // template parameter
	protected String templateParam = null;
    // Item metadata field to use in service call
	protected String lookupField = null;
    // Optional transformation of lookupField
	protected String lookupTransform = null;
    // response data to map/record
	protected List<MetadataWebServiceDataInfo> dataList = null;
    // response document parsing tools
	protected DocumentBuilder docBuilder = null;
    // language for metadata fields assigned
	protected String lang = null;
    // field separator in result string
	protected String fieldSeparator = null;
    // optional XML namespace map
	protected Map<String, String> nsMap = new HashMap<String, String>();
    // optional HTTP headers
	protected Map<String, String> headers = new HashMap<String, String>();
    
    /**
     * Initializes task
     * @param curator  Curator object performing this task
     * @param taskId the configured local name of the task 
     */
    @Override
    public void init(Curator curator, String taskId) throws IOException {
    	super.init(curator, taskId);
    	lang = configurationService.getProperty("default.language");
        String fldSep = taskProperty("separator");
        fieldSeparator = (fldSep != null) ? fldSep : " ";
    	urlTemplate = taskProperty("template");
    	templateParam = urlTemplate.substring(urlTemplate.indexOf("{") + 1,
    			                              urlTemplate.indexOf("}"));
    	String[] parsed = parseTransform(templateParam);
    	lookupField = parsed[0];
    	lookupTransform = parsed[1];
    	dataList = new ArrayList<>();
    	for (String entry : taskArrayProperty("datamap")) {
    		entry = entry.trim();
    		String src = entry;
    		String mapping = null;
    		String field = null;
    		int mapIdx = getMapIndex(entry);
    		if (mapIdx > 0) {
    			src = entry.substring(0, mapIdx);
    			mapping = entry.substring(mapIdx, mapIdx + 2);
    			field = entry.substring(mapIdx + 2);
    		}
    		int slIdx = src.lastIndexOf("/");
        	String label = (slIdx > 0) ? src.substring(slIdx + 1) : src;
    		dataList.add(new MetadataWebServiceDataInfo(this, src, label, mapping, field));
    	}
        String hdrs = taskProperty("headers");
        if (hdrs != null) {
            for (String header : hdrs.split("\\|\\|")) {
                int split = header.indexOf(":");
                headers.put(header.substring(0, split).trim(), header.substring(split + 1).trim());
            }
        }
    	// initialize response document parser
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	factory.setNamespaceAware(true);
    	try {
    		docBuilder = factory.newDocumentBuilder();
    	} catch (ParserConfigurationException pcE) {
    		log.error("caught exception: " + pcE);
    		// no point in continuing
    		throw new IOException(pcE.getMessage(), pcE);
    	}
    }
    
    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException if IO error
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException  {
    	
    	int status = Curator.CURATE_SKIP;
    	StringBuilder resultSb = new StringBuilder();
    	
        if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;
            String itemId = item.getHandle();
            if (itemId == null) {
            	// we are still in workflow - no handle assigned - try title
            	List<MetadataValue> titleDc = itemService.getMetadata(item, "dc", "title", null, Item.ANY);
            	String title = (titleDc.size() > 0) ? titleDc.get(0).getValue() : "untitled - dbId: " + item.getID();
            	itemId = "Workflow item: " + title;
            } else {
                itemId = "handle: " + itemId;
            }
            resultSb.append(itemId);
            // Only proceed if item has a value for service template parameter
			List<MetadataValue> dcVals = itemService.getMetadataByMetadataString(item, lookupField);
            if (dcVals.size() > 0 && dcVals.get(0).getValue().length() > 0) {
            	String value = transform(dcVals.get(0).getValue(), lookupTransform);
            	status = callService(value, item, resultSb);
            } else {
            	resultSb.append(" lacks metadata value required for service: ").append(lookupField);
            	status = Curator.CURATE_FAIL;
            }
        } else {
           resultSb.append("Object skipped");
        }
        report(resultSb.toString());
        setResult(resultSb.toString());
        return status;
    }
    
	protected int callService(String value, Item item, StringBuilder resultSb) throws IOException {
    	
    	String callUrl = urlTemplate.replaceAll("\\{" + templateParam + "\\}", value);
    	HttpClient client = new DefaultHttpClient();
    	HttpGet req = new HttpGet(callUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            req.addHeader(entry.getKey(), entry.getValue());
        }
    	HttpResponse resp = client.execute(req);
    	int status = Curator.CURATE_ERROR;
    	int statusCode = resp.getStatusLine().getStatusCode();
    	if (statusCode == HttpStatus.SC_OK) {
    		HttpEntity entity = resp.getEntity();
    		if (entity != null) {
    			// boiler-plate handling taken from Apache 4.1 javadoc
    			InputStream instream = entity.getContent();
                try {
                	Document doc = docBuilder.parse(instream);
                	status = processResponse(doc, item, resultSb);
            	} catch (SAXException saxE) {
            		log.error("caught exception: " + saxE);
            		resultSb.append(" unable to read response document");
                } catch (RuntimeException ex) {
                	// In case of an unexpected exception you may want to abort
                	// the HTTP request in order to shut down the underlying
                	// connection and release it back to the connection manager.
                	req.abort();
                	log.error("caught exception: " + ex);
                	throw ex;
                } finally {
                	// Closing the input stream will trigger connection release
                	instream.close();
                }
                // When HttpClient instance is no longer needed,
                // shut down the connection manager to ensure
                // immediate deallocation of all system resources
                client.getConnectionManager().shutdown();
    		} else {
    			log.error(" obtained no valid service response");
    			resultSb.append("no service response");
    		}
    	} else {
    		log.error("service returned non-OK status: " + statusCode);
    		resultSb.append("no service response");
    	}
    	return status;
    }
    
	protected int processResponse(Document doc, Item item, StringBuilder resultSb) throws IOException {
       	boolean update = false;
       	int status = Curator.CURATE_ERROR;
       	List<String> values = new ArrayList<String>();
       	checkNamespaces(doc);
       	try {
       		for (MetadataWebServiceDataInfo info : dataList) {
       			NodeList nodes = (NodeList)info.getExpr().evaluate(doc, XPathConstants.NODESET);
       			values.clear();
       			// if data found and we are mapping, check assignment policy
       			if (nodes.getLength() > 0 && info.getMapping() != null) {
       				if ("=>".equals(info.getMapping())) {
       					itemService.clearMetadata(Curator.curationContext(), item, info.getSchema(), info.getElement(), info.getQualifier(), Item.ANY);
       				} else if ("~>".equals(info.getMapping())) {
       					if (itemService.getMetadata(item, info.getSchema(), info.getElement(), info.getQualifier(), Item.ANY).size() > 0) {
       						// there are values, so don't overwrite
       						continue;
       					}
       				} else {
       					for (MetadataValue dcVal : itemService.getMetadata(item, info.getSchema(), info.getElement(), info.getQualifier(), Item.ANY)) {
       						values.add(dcVal.getValue());
       					}
       				}
       			}
       			for (int i = 0; i < nodes.getLength(); i++) {
       				Node node = nodes.item(i);
       				String tvalue = transform(node.getFirstChild().getNodeValue(), info.getTransform());
       				// assign to metadata field if mapped && not present
       				if (info.getMapping() != null && ! values.contains(tvalue)) {
       					itemService.addMetadata(Curator.curationContext(), item, info.getSchema(), info.getElement(), info.getQualifier(), lang, tvalue);
       					update = true;
       				}
       				// add to result string in any case
       				resultSb.append(fieldSeparator).append(info.getLabel()).append(": ").append(tvalue);
       			}
       		}
       		// update Item if it has changed
       		if (update) {
				itemService.update(Curator.curationContext(), item);
       		}
       		status = Curator.CURATE_SUCCESS;
       	} catch (AuthorizeException authE) {
    		log.error("caught exception: " + authE);
    		resultSb.append(" not authorized to update");
    		status = Curator.CURATE_FAIL;
       	} catch (SQLException sqlE) {
    		log.error("caught exception: " + sqlE);
    		resultSb.append(" error updating metadata");
       	} catch (XPathExpressionException xpeE) {
    		log.error("caught exception: " + xpeE);
    		resultSb.append(" error reading response document");
       	}
        return status;
    }
    
	protected String transform(String value, String transDef) {
    	if (transDef == null) {
    		return value;
    	}
    	String[] tokens = tokenize(transDef);
    	String retValue = value;
    	for (int i = 0; i < tokens.length; i+= 2) {
    		if ("cut".equals(tokens[i]) || "trunc".equals(tokens[i])) {
    			int index = Integer.parseInt(tokens[i+1]);
    			if (retValue.length() > index) {
    				if ("cut".equals(tokens[i])) {
    					retValue = retValue.substring(index);
    				} else {
    					retValue = retValue.substring(0, index);
    				}
    			} else if ("cut".equals(tokens[i])) {
    				log.error("requested cut: " + index + " exceeds value length");
    				return value;
    			}
    		} else if ("match".equals(tokens[i])) {
    			int index2 = retValue.indexOf(tokens[i+1]);
    			if (index2 > 0) {
    				retValue = retValue.substring(index2);
    			} else {
    				log.error("requested match: " + tokens[i+1] + " failed");
    				return value;
    			}
    		} else if ("text".equals(tokens[i])) {
    			retValue = retValue + tokens[i+1];
    		} else {
    			log.error(" unknown transform operation: " + tokens[i]);
    			return value;
    		}
    	}
    	return retValue;
    }
    
	protected String[] tokenize(String text)  {
    	List<String> list = new ArrayList<String>();
    	Matcher m = ttPattern.matcher(text);
    	while (m.find()) {
    		if (m.group(1) != null) {
    			list.add(m.group(1));
            } else if (m.group(2) != null) {
                list.add(m.group(2));
            }
        }
        return list.toArray(new String[0]);
    }
    
	protected int getMapIndex(String mapping) {
    	int index = mapping.indexOf("->");
    	if (index == -1) {
    		index = mapping.indexOf("=>");
    	}
    	if (index == -1) {
    		index = mapping.indexOf("~>");
    	}
    	return index;
    }

	protected String[] parseTransform(String field) {
    	String[] parsed = new String[2];
    	parsed[0] = field;
       	int txIdx = field.indexOf(":");
    	if (txIdx > 0) {
    		// transform specified
    		String txName = field.substring(0, txIdx);
    		parsed[1] = taskProperty("transform." + txName);
    		if (parsed[1] == null) {
    			log.error("no transform found for: " + txName);
    		}
    		parsed[0] = field.substring(txIdx + 1);
    	}
    	return parsed;
    }

	protected void checkNamespaces(Document document) throws IOException {
    	// skip if already done
    	if (dataList.get(0).getExpr() != null) {
    	    return;
    	}
    	try {
    	    XPath xpath = XPathFactory.newInstance().newXPath();
    	    String prefix = null;
            NamedNodeMap attrs = document.getDocumentElement().getAttributes();
    		for (int i = 0; i < attrs.getLength(); i++) {
    		    Node n = attrs.item(i);
                String name = n.getNodeName();
                // remember if a namespace
                if (name.startsWith("xmlns")) {
                    if (! "xmlns".equals(name)) {
                        // it is a declared (non-default) namespace - capture prefix
                        nsMap.put(name.substring(name.indexOf(":") + 1), n.getNodeValue());
                    } else {
                        // it is the default name space - mint a unique prefix
                        prefix = "pre";
                        nsMap.put(prefix, n.getNodeValue());
                    }
                }
            }
            if (nsMap.size() > 0) {
    		    xpath.setNamespaceContext(this);
            }
    		// now compile the XPath expressions
    		for (MetadataWebServiceDataInfo info : dataList) {
			    info.setExpr(xpath.compile(mangleExpr(info.getXpsrc(), prefix)));
		    }
    	} catch (XPathExpressionException xpeE) {
    		log.error("caught exception: " + xpeE);
        	// no point in continuing
        	throw new IOException(xpeE.getMessage(), xpeE);  			
    	}
    }
    
    protected String mangleExpr(String expr, String prefix) {
    	if (prefix == null) {
    		return expr;
    	}
    	// OK the drill is to prepend all node names with the prefix
    	// *unless* the node name already has a prefix.
    	StringBuilder sb = new StringBuilder();
    	int i = 0;
    	while (i < expr.length()) {
    		if (expr.charAt(i) == '/') {
    			sb.append("/");
    			i++;
    		} else {
    			int next = expr.indexOf("/", i);
    			String token = (next > 0) ? expr.substring(i, next) : expr.substring(i);
    			if (! token.startsWith("@") && token.indexOf(":") < 0) {
    				sb.append(prefix).append(":");
    			}
    			sb.append(token);
    			i += token.length();
    		}
    	}
    	return sb.toString();
    }
    
    // ---- NamespaceContext methods ---- //
    
    @Override
	public String getNamespaceURI(String prefix) {
        if (prefix == null) {
        	throw new NullPointerException("Null prefix");
        } else if ("xml".equals(prefix)) {
        	return XMLConstants.XML_NS_URI;
        }
        String nsURI = nsMap.get(prefix);
        return (nsURI != null) ? nsURI : XMLConstants.NULL_NS_URI;
    }

    @Override
	public String getPrefix(String uri) {
        throw new UnsupportedOperationException();
    }

    @Override
	public Iterator getPrefixes(String uri) {
        throw new UnsupportedOperationException();
    }
}
