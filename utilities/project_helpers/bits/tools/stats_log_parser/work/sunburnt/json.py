from __future__ import absolute_import

import json, math

from .schema import SolrResponse, SolrResult


class SunburntJSONEncoder(json.JSONEncoder):
    def encode(self, o):
        if isinstance(o, SolrResponse):
            return self.encode(list(o))
        return super(SunburntJSONEncoder, self).encode(o)
        
    def default(self, obj):
        if hasattr(obj, 'isoformat'):
            return "%sZ" % (obj.replace(tzinfo=None).isoformat(), )
        if hasattr(obj, "strftime"):
            try:
                microsecond = obj.microsecond
            except AttributeError:
                microsecond = int(1000000*math.modf(obj.second)[0])
            if microsecond:
                return u"%s.%sZ" % (obj.strftime("%Y-%m-%dT%H:%M:%S"), microsecond)
            return u"%sZ" % (obj.strftime("%Y-%m-%dT%H:%M:%S"),)
        return super(SunburntJSONEncoder, self).default(obj)

def dump(obj, fp, *args, **kwargs):
    if isinstance(obj, SolrResponse):
        obj = list(obj)
    elif isinstance(obj, SolrResult):
        obj = obj.docs
    return json.dump(obj, fp, cls=SunburntJSONEncoder, *args, **kwargs)

def dumps(obj, *args, **kwargs):
    if isinstance(obj, SolrResponse):
        obj = list(obj)
    elif isinstance(obj, SolrResult):
        obj = obj.docs
    return json.dumps(obj, cls=SunburntJSONEncoder, *args, **kwargs)

load = json.load
loads = json.loads
