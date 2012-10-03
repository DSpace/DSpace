#! /usr/bin/env python

""" Utility program to try to gently line wrap preformatted text inside an XML tag

"""

import sys, string
import codecs

def wrapElementLines(wrap=72,src=sys.stdin, dst=sys.stdout, srcenc='iso8859', dstenc='utf-8', elem='screen'):

    # Tell python how to encode the output (generally utf-8) 
    (e,d,sr,sw) = codecs.lookup(dstenc)
    out = sw(dst)

    # Tell python how to interpret the input  (generally iso8859)
    (e,d,sr,sw) = codecs.lookup(srcenc)
    inp = sr(src)

    inElem = False
    openElem = "<"+elem+">"
    closeElem = "</"+elem+">"

    for line in inp:
        try:
            index = line.index(openElem)
            inElem = True
        except:
            pass
        if inElem:
            try:
                line.index(closeElem)
                inElem = False
            except:
                while len(line) > wrap:
                    lim = indexlimit(line,wrap)
                    try:
                        space = line[0:lim].rindex(' ')
                        out.write(line[0:space]+"\n\t") # " \\\n\t"
                        line = line[space+1:]
                    except:
                        out.write(line[0:lim]+"\n")  # "\\\n"
                        line = line[wrap:]
        out.write(line)

def indexlimit(string,limit=72):
    tag=False
    ent=False
    ptr=0
    cnt=0
    for ch in string:
        ptr = ptr + 1
        if ch == '<' and not tag:
            tag=True
        elif ch == '>' and tag:
            tag=False
        elif ch == '&' and not ent:
            ent=True
        elif ch == ';' and ent:
            cnt = cnt + 1
            ent=False
        elif not (tag or ent):
            cnt = cnt + 1
            if cnt >= limit:
                return ptr
    return ptr
   
src = sys.stdin
dst = sys.stdout

if len(sys.argv) > 1:
    src = open(sys.argv[1])
if len(sys.argv) > 2:
    dst = open(sys.argv[2],"w")

wrapElementLines(src=src,dst=dst,wrap=70,srcenc="utf-8")
