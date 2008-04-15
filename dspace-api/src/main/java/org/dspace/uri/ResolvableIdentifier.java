/*
 * ResolvableIdentifier.java
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

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

/**
 * Interface to be implemented by any Identifier class which wants to be able
 * to offer resolution services.
 *
 * @author Richard Jones
 */
public interface ResolvableIdentifier
{
    /**
     * Get the context path of the URL which should be used in constructing
     * the URL space, and for which the IdentifierResolver will be able to
     * re-obtain the ResolvableIdentifier for.  This means the [context path]
     * part of the URL as follows:
     *
     * <code>[base URL]/[context path][extra path info]</code>
     *
     * @return
     */
    String getURLForm();

    /**
     * Return the string representation of the identifier type, such as "uuid"
     * or "hdl" etc.
     *
     * @return
     */
    String getIdentifierTypeRepresentation();

    /**
     * Return the canonical form of the specific identifier
     *
     * @return
     */
    String getCanonicalForm();

    /**
     * Get the ObjectIdentifier which backs this particular ResolvableIdentifier.
     *
     * In the special case that the ObjectIdentifier is the ResolvableIdentifier, then
     * this method should return a reference to itself (i.e. "this")
     * 
     * @return
     */
    ObjectIdentifier getObjectIdentifier();
}
