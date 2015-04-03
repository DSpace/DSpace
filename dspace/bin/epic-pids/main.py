# coding=utf-8
import re
import urllib
import urllib2
import logging
import sys

logging.basicConfig()
_logger = logging.getLogger()

_settings = {
    "username": None,
    "password": None,
}


class epic(object):

    PID_URL = "handle.gwdg.de/pidservice/"

    def __init__(self, u_str, p_str ):
        self._u = u_str
        self._p = p_str
        self._m = {}

    def init(self):
        self._m = self.list()

    def list(self):
        regexp_url = re.compile("<url>(.*)</url>")
        regexp_pid = re.compile("<pid>(.*)</pid>")
        xml = epic.cmd( "read/search?creator=" + self._u )
        r = {}
        # this is really stupid
        lls = xml.splitlines()
        for i in range(len(lls)):
            m = regexp_pid.search(lls[i])
            if m:
                url = None
                pid = m.group(1)
                m = regexp_url.search(lls[i+1])
                if not m:
                    #problem
                    pass
                else:
                    url = m.group(1)
                r[pid] = url
        return r

    def d(self):
        return self._m

    def dummies_i(self):
        for k, v in self._m.iteritems():
            if "dummy" in v:
                yield k, v

    def deletes_i(self):
        for k, v in self._m.iteritems():
            if "todelete" in v:
                yield k, v

    def replace(self, pid, url):
        _logger.info( "Updating [%s] -> %s", pid, url )
        return epic.cmd( "write/modify",
            {
                "pid": pid,
                "url": url,
            },
            self._u, self._p )

    def replace_all(self, url_what, url_with ):
        for pid, url in self._m.iteritems():
            if url_what in url:
                new_url = url.replace(url_what, url_with)
                print "%s ->\n%s\n\n" % (url, new_url)
                #self.replace(pid, new_url)

    @staticmethod
    def cmd( uri, params=None, u=None, p=None ):
        url = "https://%s%s" % (epic.PID_URL, uri)
        if u is not None:
            class _opener (urllib.FancyURLopener):
                def get_user_passwd(self, host, realm, clear_cache=0):
                    return (u, p)
            o = _opener()
            params = urllib.urlencode(params)
            xml = o.open(url, params).read()
        else:
            xml = urllib2.urlopen(url).read()
        return xml


#
#

if __name__ == '__main__':
    try:
        raise "Do not use directly - edit it before!"
        e = epic(_settings["username"], _settings["password"])
        e.init()

        # list urls
        # for k, v in e.d().iteritems():
        #     print "%s:%s" % (k, v)

        # list dummies
        print "DUMMIES"
        for k, v in e.dummies_i():
            print "%s:%s" % (k, v)

        # list deletes
        print "DELETES"
        for pid, url in e.deletes_i():
            print "%s:%s" % (pid, url)
            #print "->", "http://does.not.exist"
            #e.replace( pid, "http://does.not.exist" )

        #print e.replace( "11858/00-097C-0000-0005-AAF1-A", "123" )
        # e.replace_all( "https://ufal-point.mff.cuni.cz/xmlui/handle/",
        #                "https://lindat.mff.cuni.cz/repository/xmlui/handle/" )

    except Exception, e:
        _logger.exception( "EPIC problem" )
