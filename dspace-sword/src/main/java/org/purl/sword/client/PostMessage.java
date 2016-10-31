/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/**
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions 
 * are met:
 * 
 *  - Redistributions of source code must retain the above 
 *    copyright notice, this list of conditions and the 
 *    following disclaimer.
 *  
 *  - Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 *    
 *  - Neither the name of the Centre for Advanced Software and 
 *    Intelligent Systems (CASIS) nor the names of its 
 *    contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR 
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF 
 * SUCH DAMAGE.
 */
package org.purl.sword.client;

import java.io.File;

/**
 * Represents the details of a post to a server. The message holds all of the possible values
 * that are to be sent from the client to the server. Not all elements of the message 
 * must be filled in. Any required fields are defined in the current SWORD specification. 
 * 
 * @author Neil Taylor
 */
public class PostMessage 
{
    /**
     * The local filepath for the file to upload/deposit. 
     */
    private String filepath;
 
    /**
     * The URL of the destination server. 
     */
    private String destination; 
 
    /**
     * The filetype of the package that is to be uploaded. 
     */
    private String filetype;
 
    /**
     * The string with the username if the deposit is on behalf of another user. 
     */
    private String onBehalfOf; 
 
    /**
     * True if an MD5 checksum should be sent with the deposit. 
     */
    private boolean useMD5;
 
    /**
     * True if the deposit is a test and should not result in an actual deposit. 
     */
    private boolean noOp;
 
    /**
     * True if the verbose operation is requested. 
     */
    private boolean verbose; 
 
    /**
     * The packaging format for the deposit.
     */
    private String packaging;
 
    /**
     * True if the deposit should simulate a checksum error. The client should check this
     * field to determine if a correct MD5 checksum should be sent or whether the checksum should
     * be modified so that it generates an error at the server.
     */
    private boolean checksumError;
 
    /**
     * True if the deposit should corrupt the POST header. The client should check this
     * field to determine if a correct header should be sent or whether the header should
     * be modified so that it generates an error at the server.
     */
    private boolean corruptRequest;
 
    /** 
     * The Slug header value. 
     */
    private String slug; 
    
    /**
     * The user agent name
     */
    private String userAgent;
 
    /**
     * Get the filepath. 
     * 
     * @return The filepath. 
     */
    public String getFilepath() 
    {
        return filepath;
    }
    
    /**
     * Get the filename. This is the last element of the filepath 
     * that has been set in this class. 
     *
     * @return filename
     */
    public String getFilename() 
    {
        File file = new File(filepath);
        return file.getName(); 
    }
 
    /** 
     * Set the filepath. 
     *  
     * @param filepath The filepath. 
     */
    public void setFilepath(String filepath) 
    {
        this.filepath = filepath;
    }
 
    /**
     * Get the destination collection. 
     * 
     * @return The collection. 
     */
    public String getDestination() 
    {
        return destination;
    }
 
    /**
     * Set the destination collection. 
     * 
     * @param destination The destination. 
     */
    public void setDestination(String destination) 
    {
        this.destination = destination;
    }
 
    /**
     * Get the filetype. 
     * @return The filetype. 
     */
    public String getFiletype() 
    {
        return filetype;
    }
 
    /**
     * Set the filetype. 
     * 
     * @param filetype The filetype. 
     */
    public void setFiletype(String filetype) 
    {
        this.filetype = filetype;
    }
 
    /** 
     * Get the onBehalfOf value. 
     * 
     * @return The value. 
     */
    public String getOnBehalfOf() 
    {
        return onBehalfOf;
    }
 
    /**
     * Set the onBehalfOf value. 
     * 
     * @param onBehalfOf The value. 
     */
    public void setOnBehalfOf(String onBehalfOf) 
    {
        this.onBehalfOf = onBehalfOf;
    }
 
    /**
     * Get the MD5 status. 
     * @return The value. 
     */
    public boolean isUseMD5() 
    {
        return useMD5;
    }
 
    /**
     * Set the md5 state. 
     * 
     * @param useMD5 True if the message should use an MD5 checksum. 
     */
    public void setUseMD5(boolean useMD5) 
    {
        this.useMD5 = useMD5;
    }
 
    /**
     * Get the no-op state. 
     * 
     * @return The value. 
     */
    public boolean isNoOp() 
    {
        return noOp;
    }
 
    /**
     * Set the no-op state. 
     * 
     * @param noOp The no-op. 
     */
    public void setNoOp(boolean noOp) 
    {
        this.noOp = noOp;
    }
 
    /**
     * Get the verbose value. 
     * 
     * @return The value. 
     */
    public boolean isVerbose()
    {
        return verbose;
    }
 
    /**
     * Set the verbose state. 
     * 
     * @param verbose True if the post message should send a 
     * verbose header. 
     */
    public void setVerbose(boolean verbose) 
    {
        this.verbose = verbose;
    }
 
    /**
     * Get the packaging format. 
     * 
     * @return The value.
     */
    public String getPackaging() 
    {
        return packaging;
    }
 
    /**
     * Set the packaging format. 
     * 
     * @param packaging The packaging format. 
     */
    public void setFormatNamespace(String packaging) 
    {
        this.packaging = packaging;
    }
 
    /**
     * Get the status of the checksum error. 
     * 
     * @return True if the client should simulate a checksum error. 
     */
    public boolean getChecksumError() 
    {
        return checksumError;
    }
 
    /**
     * Set the state of the checksum error. 
     * 
     * @param checksumError True if the item should include a checksum error. 
     */
    public void setChecksumError(boolean checksumError) 
    {
        this.checksumError = checksumError;
    }
 
    /**
     * Get the status of the corrupt request flag.
     * 
     * @return True if the client should corrupt the POST header.
     */
    public boolean getCorruptRequest()
    {
        return corruptRequest;
    }
 
    /**
     * Set the state of the corrupt request flag.
     * 
     * @param corruptRequest True if the item should corrupt the POST header.
     */
    public void setCorruptRequest(boolean corruptRequest)
    {
        this.corruptRequest = corruptRequest;
    }
 
    /**
     * Set the Slug value. 
     * 
     * @param slug The value. 
     */
    public void setSlug(String slug)
    {
        this.slug = slug;
    }
 
    /**
     * Get the Slug value. 
     * 
     * @return The Slug. 
     */
    public String getSlug()
    {
        return this.slug; 
    }

    /**
     * @return the userAgent
     */
    public String getUserAgent() {
        return userAgent;
    }
    
    /**
     * Set the user agent
     * 
     * @param userAgent the userAgent to set
     */
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
