/*
 * Identifiable.java
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.uri;

import java.util.List;

/**
 * Interface which must be implemented by any DSpace class wishing to expose any identifier
 * functionality.  Some objects may only wish to use SimpleIdentifiers, which cannot be
 * used to resolve to the object through the API.  Other objects may use different
 * combinations of identifiers depending on purpose.  Most likely configurations are:
 *
 * - SimpleIdentifier support only
 * - ObjectIdentifier and SimpleIdentifier support only
 * - Full Identifier support
 *
 * Implementations wishing not to support a particular identifier mechanism should implement
 * the methods associated with that mechanism to simply throw an UnsupportedIdentifierException
 * 
 * @author Richard Jones
 */
public interface Identifiable
{
    /**
     * Get an instance of a SimpleIdentifier which belongs to an object
     *
     * @return
     */
    SimpleIdentifier getSimpleIdentifier();

    /**
     * Set the SimpleIdentifier for the object
     *
     * @param sid
     * @throws UnsupportedIdentifierException
     */
    void setSimpleIdentifier(SimpleIdentifier sid) throws UnsupportedIdentifierException;

    /**
     * Get an instance of the object identifier which can be used to resolve to this object
     *
     * @return
     */
    ObjectIdentifier getIdentifier();

    /**
     * Set the ObjectIdentifier that this object should be resolvable by.  Implementations should ensure
     * that the given object identifier actually refers correctly to the object it is being set to by
     * validating or setting the relevant parameters in the passed ObjectIdentifier
     *
     * @param oid
     * @throws UnsupportedIdentifierException
     */
    void setIdentifier(ObjectIdentifier oid) throws UnsupportedIdentifierException;

    /**
     * Get a list of all the external identifiers associated with the item
     *
     * @return
     */
    List<ExternalIdentifier> getExternalIdentifiers();

    /**
     * Set the complete and exclusive list of external identifiers to be associated with
     * an item.  This should override any previously existing list of identifiers held by the item.
     * Implementation should ensure that the given external identifier actually refers to the
     * object's ObjectIdentifier either by validation, or setting the relevant parameters in the
     * passed ExternalIdentifier
     *
     * @param eids
     * @throws UnsupportedIdentifierException
     */
    void setExternalIdentifiers(List<ExternalIdentifier> eids) throws UnsupportedIdentifierException;

    /**
     * Add an ExternalIdentifier to the current list of external identifiers associated with
     * the item.  Implementation should ensure that any given external identifier actually refers
     * to the object's ObjectIdentifier either by validation, or setting the relevant parameters
     * in the passed ExternalIdentifier
     *
     * @param eid
     * @throws UnsupportedIdentifierException
     */
    void addExternalIdentifier(ExternalIdentifier eid) throws UnsupportedIdentifierException;
}
