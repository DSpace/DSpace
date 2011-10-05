@REM
@REM buildpath.bat
@REM
@REM Version: $Revision$
@REM
@REM Date: $Date$
@REM
@REM Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
@REM
@REM Redistribution and use in source and binary forms, with or without
@REM modification, are permitted provided that the following conditions are
@REM met:
@REM
@REM - Redistributions of source code must retain the above copyright
@REM notice, this list of conditions and the following disclaimer.
@REM
@REM - Redistributions in binary form must reproduce the above copyright
@REM notice, this list of conditions and the following disclaimer in the
@REM documentation and/or other materials provided with the distribution.
@REM
@REM - Neither the name of the DSpace Foundation nor the names of its
@REM contributors may be used to endorse or promote products derived from
@REM this software without specific prior written permission.
@REM
@REM THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
@REM ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
@REM LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
@REM A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
@REM HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
@REM INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
@REM BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
@REM OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
@REM ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
@REM TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
@REM USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
@REM DAMAGE.
@REM

@REM A simple script to facilitate building a CLASSPATH dynamically
@REM in dsrun.bat

@echo off

set DSPACE_CLASSPATH=%DSPACE_CLASSPATH%;%~s1
