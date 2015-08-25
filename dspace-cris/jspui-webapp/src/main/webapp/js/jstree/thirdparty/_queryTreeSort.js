/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
var _queryTreeSort = function(options) {
  var cfi, e, i, id, o, pid, rfi, ri, thisid, _i, _j, _len, _len1, _ref, _ref1;
  id = options.id || "id";
  pid = options.parentid || "parentid";
  ri = [];
  rfi = {};
  cfi = {};
  o = [];
  _ref = options.q;
  for (i = _i = 0, _len = _ref.length; _i < _len; i = ++_i) {
    e = _ref[i];
    rfi[e[id]] = i;
    if (cfi[e[pid]] == null) {
      cfi[e[pid]] = [];
    }
    cfi[e[pid]].push(options.q[i][id]);
  }
  _ref1 = options.q;
  for (_j = 0, _len1 = _ref1.length; _j < _len1; _j++) {
    e = _ref1[_j];
    if (rfi[e[pid]] == null) {
      ri.push(e[id]);
    }
  }
  while (ri.length) {
    thisid = ri.splice(0, 1);
    o.push(options.q[rfi[thisid]]);
    if (cfi[thisid] != null) {
      ri = cfi[thisid].concat(ri);
    }
  }
  return o;
};