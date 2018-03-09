/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.rdf.negotiation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author Pascal-Nicolas Becker (dspace -at- pascal -hyphen- becker -dot- de)
 */
public class MediaRange
{
    // defined in RFC 2616
    public static final double DEFAULT_QVALUE = 1.0;
    
    // RFC 2616 defines syntax of the accept header using several patterns
    // the patterns are defined in the parts 2.2, 3.6, 3.7, 3.9 and 14.1 of the rfc
    
    // SEPARATOR: ( ) < > @ , ; : \ " / [ ] ? = { } <space> <tabulator>
    // the separators can be used in as class inside square brackets. To be able
    // to negate the class, the spearators necessary square brackets are not
    // included in the string.
    public static final String separators = "()<>@,;:\\\\\"/\\[\\]?={} \\t";

    // TOKEN: ANY US ASCII except ctl an separtor
    public static final String token = "[\\040-\\0176" + "&&[^" + separators + "]]+";

    // "\" followed by any US ASCII character (octets 0 - 177)
    public static final String quotedPair = "(?:\\\\[\\00-\\0177])";

    // any 8 bit sequence, except CTLs (00-037, 0177) and " (042) but including LWS
    public static final String qdtext = "(?:[\\040\\041\\043-\\0176\\0178-\\0377]|"
            + "(?:\\r\\n)?[ \\t]+)";

    // ( <"> *(qdtext | quoted-pair) <">
    public static final String quotedString = "(?:\"(?:" + qdtext + "|" + quotedPair + ")*\")";

    public static final String nonQualityParam = "(?:\\s*;\\s*(?!q\\s*=)(" + token + ")=" 
            + "(" + token + "|" + quotedString + ")" + ")";
    public static final String qualityParam = "(?:;\\s*q\\s*=\\s*(0(?:\\.\\d{0,3})?|1(?:\\.0{0,3})?))";

    // group 0 contains the hole matched media range
    // group 1 contains the type
    // group 2 contains the subtype
    // group 3 contains all parameters before the quality parameter if any
    // group 4 contains the name of the last parameter before the quality parameter if any
    // group 5 contains the value of the last parameter before the quality parameter if any
    // group 6 contains the quality value if any
    // group 7 contains all parameters after the quality parameter if any
    // group 8 contains the name of the last parameter after the quality paremeter if any
    // group 9 contains the value of the laster parameter after the quality paremeter if any
    public static final String mediaRangeRegex = "(?:(" + token + ")/(" + token + "?)"
            + "(" + nonQualityParam + "*)" + qualityParam + "?(" + nonQualityParam + "*))";
    
    private final static Logger log = Logger.getLogger(MediaRange.class);
    
    protected final String type;
    protected final String subtype;
    protected final double qvalue;
    // would be good to take a Map for the parameters, but if we get multiple 
    // parameters with the same name, we would have a problem.
    protected final List<String> parameterNames;
    protected final List<String> parameterValues;
    

    private MediaRange() {
        throw new RuntimeException("Default constructor of MediaRange must "
                + "not be called. Use static methods instead.");
    }
    
    public MediaRange(String mediarange)
            throws IllegalArgumentException, IllegalStateException
    {
        Pattern mediaRangePattern = Pattern.compile("^" + mediaRangeRegex + "$");

        Matcher rangeMatcher = mediaRangePattern.matcher(mediarange.trim());
        if (!rangeMatcher.matches())
        {
            log.warn("Provided media range ('" + mediarange.trim() + "') "
                    + "does not comply with RFC 2616.");
            throw new IllegalArgumentException("Provided media range ('" 
                    + mediarange + "') does not comply with RFC 2616.");
        }
        
        String type = rangeMatcher.group(1);
        String subtype = rangeMatcher.group(2);
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(subtype))
        {
            throw new IllegalArgumentException("A media range had an unparsable type or subtype.");
        }
        type = type.trim().toLowerCase();
        subtype = subtype.trim().toLowerCase();
        if (type.equals("*") && !subtype.equals("*"))
        {
            throw new IllegalArgumentException("A media range's type cannot "
                    + "be wildcarded if its subtype isn't as well.");
        }
        // initalize with defualt value, parse later
        double qvalue = DEFAULT_QVALUE;
        // initialize empty lists, parse parameters later
        List<String> parameterNames = new ArrayList<>();
        List<String> parameterValues = new ArrayList<>();
        
        // parse qvalue
        if (!StringUtils.isEmpty(rangeMatcher.group(6)))
        {
            // parse provided quality value
            try
            {
                qvalue = Double.parseDouble(rangeMatcher.group(6));
            }
            catch (NumberFormatException ex)
            {
                // the regex should assure that the qvalue is parseable.
                // if we get a NumberFormatException, we did something terribly 
                // wrong.
                log.fatal("A quality value ('" + rangeMatcher.group(6) + "') "
                        + "was unparsable. We probably have a problem with our "
                        + "regex!", ex);
                throw new IllegalStateException(ex);
            }
        }
        
        // parse parameters
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmpty(rangeMatcher.group(3)))
        {
            sb.append(rangeMatcher.group(3));
        }
        if (!StringUtils.isEmpty(rangeMatcher.group(7)))
        {
            sb.append(rangeMatcher.group(7));
        }
        if (sb.length() > 0)
        {
            String unparsedParameters = sb.toString();
            Pattern paramPattern = Pattern.compile(nonQualityParam);
            Matcher m = paramPattern.matcher(unparsedParameters);
            if (!m.matches())
            {
                // the mediarange string matched our mediaRangeRegex, but the
                // parsed parameters doesn't?!
                log.fatal("Unable to parse the parameters ('"
                        + unparsedParameters + "') of a previously parsed media "
                        + "range!");
                throw new IllegalStateException("Run into problems while parsing "
                        + "a substring of a previuosly succesfully parsed string.");
            }
            while (m.find())
            {
                if (!StringUtils.isEmpty(m.group(1)))
                {
                    parameterNames.add(m.group(1).trim().toLowerCase());
                    parameterValues.add(StringUtils.isEmpty(m.group(2)) ? "" : m.group(2).trim());
                }
            }
        }
        
        this.type = type;
        this.subtype = subtype;
        this.qvalue = qvalue;
        this.parameterNames = parameterNames;
        this.parameterValues = parameterValues;
    }

    public double getQvalue() {
        return this.qvalue;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }
    
    public List<String> getParameterValues() {
        return parameterValues;
    }
    
    public boolean typeIsWildcard()
    {
        return (StringUtils.equals(type, "*"));
    }
    
    public boolean subtypeIsWildcard()
    {
        return (StringUtils.equals(subtype, "*"));
    }

}
