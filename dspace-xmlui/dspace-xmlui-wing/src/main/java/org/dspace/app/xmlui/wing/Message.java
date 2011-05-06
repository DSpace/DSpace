/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing;

import java.io.Serializable;

/**
 * 
 * This class represents an i18n message, which is composed of three parts: a 
 * catalogue, a key, and a set of dictionary parameters. The catalogue tells 
 * the translater where to find the key, the key tells the transformer which 
 * specific text should be used, and the parameters provide for non translated 
 * data to be inserted into the resulting string.
 * 
 * This class design such that the Message object can be made static my any 
 * class that needs to use it. If dicionary parameters are used then a new 
 * instance is created specificaly for those parameters, this prevents 
 * concurent threads from over writting parameters of each other.
 * 
 * @author Scott Phillips
 */

public class Message implements Serializable
{
    /** What catalogue this key is to be found in. */
    protected final String catalogue;

    /** The key to look up in the catalogue. */
    protected final String key;

    /**
     * Create a new translatable element.
     * 
     * @param catalogue
     *            The catalogue were this key is to be found.
     * @param key
     *            The key to look up in the catalogue.
     */
    public Message(String catalogue, String key)
    {
        this.catalogue = catalogue;
        this.key = key;
    }
    
    /**
     * 
     * @return The catalogue this key is to be found in.
     */
    public String getCatalogue()
    {
        return this.catalogue;
    }

    /**
     * 
     * @return The key to look-up in the catalogue.
     */
    public String getKey()
    {
        return this.key;
    }

    /** 
     * 
     * Parameterize this translate key by specifying 
     * dictionary parameters. This will not modify the 
     * current translate object but instead create a 
     * cloned copy that has been parameterized.
     * 
     * @param dictionaryParameters The dictionary parameters
     */
    public Message parameterize(Object ... dictionaryParameters)
    {
        return new ParameterizedMessage(catalogue,key,dictionaryParameters);
    }
    
    /**
     * Return any dictionary parameters that are used by this
     * translation message.
     * 
     * Since this is the basic implementation it does not support
     * parameters we just return an empty array.
     * 
     * @return Any parameters to the catalogue key
     */
    public Object[] getDictionaryParameters()
    {
        return new Object[0];
    }
    
    
    
    /**
     * 
     * Specialized translate class that handles parameterized messages.
     * Parameterized messages contain a catalogue and key like normal but
     * also add the ability for extra parameters to be added to the
     * message. These parameters are inserted into the final translated
     * string based upon the key's definition. 
     *
     * No one out side of this class should even know this class exists,
     * hence the privacy, but haveing two implementations allows us to
     * sepearete all the functionality for paramaterization into this
     * one place. Since most of the messages used are unparameterized
     * this is not wasted on them and is only invoked when needed. There 
     * may be some performance increase by doing this but i doubt it is 
     * of much consequence, instead the main reason is to be able to create
     * a new instance when messages are parameterized so that concurrent
     * threads do not step on each other.
     * 
     */
    private static class ParameterizedMessage extends Message 
    {
    	 /**
         * Parameters to the dictionary key, they may be filled into places in the
         * final translated version
         */
        private final Object[] dictionaryParameters;

        /**
         * Create a new translatable element.
         * 
         * @param catalogue
         *            The catalogue were this key is to be found.
         * @param key
         *            The key to look up in the catalogue.
         */
        public ParameterizedMessage(String catalogue, String key, Object ... dictionaryParameters)
        {
        	super(catalogue,key);
            this.dictionaryParameters = dictionaryParameters;
        }
        
        /**
         * Return the dicionary parameters for this message.
         * 
         * @return Any parameters to the catalogue key
         */
        public Object[] getDictionaryParameters()
        {
            return dictionaryParameters;
        }
    }
    
}
