from __future__ import absolute_import

import datetime, math, re, warnings

try:
    import mx.DateTime
except ImportError:
    warnings.warn(
        "mx.DateTime not found, retricted to Python datetime objects",
        ImportWarning)
    mx = None


year = r'[+/-]?\d+'
tzd = r'Z|((?P<tzd_sign>[-+])(?P<tzd_hour>\d\d):(?P<tzd_minute>\d\d))'
extended_iso_template = r'(?P<year>'+year+r""")
               (-(?P<month>\d\d)
               (-(?P<day>\d\d)
            ([T%s](?P<hour>\d\d)
                :(?P<minute>\d\d)
               (:(?P<second>\d\d)
               (.(?P<fraction>\d+))?)?
               ("""+tzd+""")?)?
               )?)?"""
extended_iso = extended_iso_template % " "
extended_iso_re = re.compile('^'+extended_iso+'$', re.X)

def datetime_from_w3_datestring(s):
    """ We need to extend ISO syntax (as permitted by the standard) to allow
    for dates before 0AD and after 9999AD. This is how to parse such a string"""
    m = extended_iso_re.match(s)
    if not m:
        raise ValueError
    d = m.groupdict()
    d['year'] = int(d['year'])
    d['month'] = int(d['month'] or 1)
    d['day'] = int(d['day'] or 1)
    d['hour'] = int(d['hour'] or 0)
    d['minute'] = int(d['minute'] or 0)
    d['fraction'] = d['fraction'] or '0'
    d['second'] = float("%s.%s" % ((d['second'] or '0'), d['fraction']))
    del d['fraction']
    if d['tzd_sign']:
        if d['tzd_sign'] == '+':
            tzd_sign = 1
        elif d['tzd_sign'] == '-':
            tzd_sign = -1
        try:
            tz_delta = datetime_delta_factory(tzd_sign*int(d['tzd_hour']),
                                              tzd_sign*int(d['tzd_minute']))
        except DateTimeRangeError:
            raise ValueError(e.args[0])
    else:
        tz_delta = datetime_delta_factory(0, 0)
    del d['tzd_sign']
    del d['tzd_hour']
    del d['tzd_minute']
    try:
        dt = datetime_factory(**d) + tz_delta
    except DateTimeRangeError:
        raise ValueError(e.args[0])
    return dt


class DateTimeRangeError(ValueError):
    pass
    

if mx:
    def datetime_factory(**kwargs):
        try:
            return mx.DateTime.DateTimeFrom(**kwargs)
        except mx.DateTime.RangeError:
            raise DateTimeRangeError(e.args[0])
else:
    def datetime_factory(**kwargs):
        second = kwargs.get('second')
        if second is not None:
            f, i = math.modf(second)
            kwargs['second'] = int(i)
            kwargs['microsecond'] = int(f * 1000000)
        try:
            return datetime.datetime(**kwargs)
        except ValueError, e:
            raise DateTimeRangeError(e.args[0])

if mx:
    def datetime_delta_factory(hours, minutes):
        return mx.DateTime.DateTimeDelta(0, hours, minutes)
else:
    def datetime_delta_factory(hours, minutes):
        return datetime.timedelta(hours=hours, minutes=minutes)
