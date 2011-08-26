package org.swordapp.server;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ErrorDocument
{
    private String errorUri;
    private Map<String, Integer> errorCodes = new HashMap<String, Integer>();
    private String summary = null;
    private String verboseDescription = null;
    private int status;

    public ErrorDocument(String errorUri)
    {
        this(errorUri, -1, null, null);
    }

    public ErrorDocument(String errorUri, int status)
    {
        this(errorUri, status, null, null);
    }

    public ErrorDocument(String errorUri, String verboseDescription)
    {
        this(errorUri, -1, null, verboseDescription);
    }

    public ErrorDocument(String errorUri, int status, String verboseDescription)
    {
        this(errorUri, status, null, verboseDescription);
    }

    public ErrorDocument(String errorUri, int status, String summary, String verboseDescription)
    {
        // set up the error codes mapping
        this.errorCodes.put(UriRegistry.ERROR_BAD_REQUEST, 400); // bad request
        this.errorCodes.put(UriRegistry.ERROR_CHECKSUM_MISMATCH, 412); // precondition failed
        this.errorCodes.put(UriRegistry.ERROR_CONTENT, 415); // unsupported media type
        this.errorCodes.put(UriRegistry.ERROR_MEDIATION_NOT_ALLOWED, 412); // precondition failed
        this.errorCodes.put(UriRegistry.ERROR_METHOD_NOT_ALLOWED, 405); // method not allowed
        this.errorCodes.put(UriRegistry.ERROR_TARGET_OWNER_UNKNOWN, 403); // forbidden
		this.errorCodes.put(UriRegistry.ERROR_MAX_UPLOAD_SIZE_EXCEEDED, 413); // forbidden

        this.errorUri = errorUri;
        this.summary = summary;
        this.verboseDescription = verboseDescription;
        this.status = status;
    }

    public int getStatus()
    {
        if (this.status > -1)
        {
            return this.status;
        }
        
        if (this.errorCodes.containsKey(errorUri))
        {
            return this.errorCodes.get(errorUri);
        }
        else
        {
            return 400; // bad request
        }
    }

    public void writeTo(Writer out, SwordConfiguration config)
            throws IOException, SwordServerException
    {
        // do the XML serialisation
        Element error = new Element("sword:error", UriRegistry.SWORD_TERMS_NAMESPACE);
        error.addAttribute(new Attribute("href", this.errorUri));

        // write some boiler-plate text into the document
        Element title = new Element("atom:title", UriRegistry.ATOM_NAMESPACE);
        Element updates = new Element("atom:updated", UriRegistry.ATOM_NAMESPACE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        updates.appendChild(sdf.format(new Date()));
        Element generator = new Element("atom:generator", UriRegistry.ATOM_NAMESPACE);
        generator.addAttribute(new Attribute("uri", config.generator()));
        generator.addAttribute(new Attribute("version", config.generatorVersion()));
        if (config.administratorEmail() != null)
        {
            generator.appendChild(config.administratorEmail());
        }
        Element treatment = new Element("sword:treatment", UriRegistry.SWORD_TERMS_NAMESPACE);
        treatment.appendChild("Processing failed");

        error.appendChild(title);
        error.appendChild(updates);
        error.appendChild(generator);
        error.appendChild(treatment);

        // now add the operational bits
        if (this.summary != null)
        {
            Element summary = new Element("atom:summary", UriRegistry.ATOM_NAMESPACE);
            summary.appendChild(this.summary);
            error.appendChild(summary);
        }

        if (this.verboseDescription != null)
        {
            Element vd = new Element("sword:verboseDescription", UriRegistry.SWORD_TERMS_NAMESPACE);
            vd.appendChild(this.verboseDescription);
            error.appendChild(vd);
        }

        try
        {
            // now get it written out
            Document doc = new Document(error);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Serializer serializer = new Serializer(baos, "ISO-8859-1");
            serializer.write(doc);
            out.write(baos.toString());
        }
        catch (UnsupportedEncodingException e)
        {
            throw new SwordServerException(e);
        }
    }
}
