const { Router } = require('express');
const util = require('util');

// Our API for demos only
import { fakeDataBase } from './db';
import { fakeDemoRedisCache } from './cache';

import COMMUNITIES from './data/communities.json';
import COLLECTIONS from './data/collections.json';
import ITEMS from './data/items.json';
import BUNDLES from './data/bundles.json';
import BITSTREAMS from './data/bitstreams.json';

// you would use cookies/token etc
const USER_ID = 'f9d98cf1-1b96-464e-8755-bcc2a5c09077'; // hardcoded as an example

// Our API for demos only
export function serverApi(req, res) {
  const key = USER_ID + '/data.json';
  const cache = fakeDemoRedisCache.get(key);
  if (cache !== undefined) {
    console.log('/data.json Cache Hit');
    return res.json(cache);
  }
  console.log('/data.json Cache Miss');

  fakeDataBase.get()
    .then((data) => {
      fakeDemoRedisCache.set(key, data);
      return data;
    })
    .then((data) => res.json(data));
}

function toHALResponse(req, data, included?) {
  const result = {
    _embedded: data,
    _links: {
      self: req.protocol + '://' + req.get('host') + req.originalUrl
    }
  };
  if (included && Array.isArray(included) && included.length > 0) {
    Object.assign(result, {
      included: included
    });
  }
  return result;
}

export function createMockApi() {

  const router = Router();

  router.route('/communities').get((req, res) => {
    console.log('GET');
    // 70ms latency
    setTimeout(() => {
      res.json(toHALResponse(req, COMMUNITIES));
    }, 0);
  });

  router.param('community_id', (req, res, next, communityId) => {
    // ensure correct prop type
    const id = req.params.community_id;
    try {
      req.community_id = id;
      req.community = COMMUNITIES.communities.find((community) => {
        return community.id === id;
      });
      next();
    } catch (e) {
      next(new Error('failed to load community'));
    }
  });

  router.route('/communities/:community_id').get((req, res) => {
    res.json(toHALResponse(req, req.community));
  });

  router.route('/collections').get((req, res) => {
    console.log('GET');
    // 70ms latency
    setTimeout(() => {
      res.json(toHALResponse(req, COLLECTIONS));
    }, 0);
  });

  router.param('collection_id', (req, res, next, collectionId) => {
    // ensure correct prop type
    const id = req.params.collection_id;
    try {
      req.collection_id = id;
      req.collection = COLLECTIONS.collections.find((collection) => {
        return collection.id === id;
      });
      next();
    } catch (e) {
      next(new Error('failed to load collection'));
    }
  });

  router.route('/collections/:collection_id').get((req, res) => {
    res.json(toHALResponse(req, req.collection));
  });

  router.route('/items').get((req, res) => {
    console.log('GET');
    // 70ms latency
    setTimeout(() => {
      res.json(toHALResponse(req, ITEMS));
    }, 0);
  });

  router.param('item_id', (req, res, next, itemId) => {
    // ensure correct prop type
    const id = req.params.item_id;
    try {
      req.item_id = id;
      req.itemRD$ = ITEMS.items.find((item) => {
        return item.id === id;
      });
      next();
    } catch (e) {
      next(new Error('failed to load item'));
    }
  });

  router.route('/items/:item_id').get((req, res) => {
    res.json(toHALResponse(req, req.itemRD$));
  });

  router.route('/bundles').get((req, res) => {
    console.log('GET');
    // 70ms latency
    setTimeout(() => {
      res.json(toHALResponse(req, BUNDLES));
    }, 0);
  });

  router.param('bundle_id', (req, res, next, bundleId) => {
    // ensure correct prop type
    const id = req.params.bundle_id;
    try {
      req.bundle_id = id;
      req.bundle = BUNDLES.bundles.find((bundle) => {
        return bundle.id === id;
      });
      next();
    } catch (e) {
      next(new Error('failed to load item'));
    }
  });

  router.route('/bundles/:bundle_id').get((req, res) => {
    // console.log('GET', util.inspect(req.bundle, { colors: true }));
    res.json(toHALResponse(req, req.bundle));
  });

  router.route('/bitstreams').get((req, res) => {
    console.log('GET');
    // 70ms latency
    setTimeout(() => {
      res.json(toHALResponse(req, BITSTREAMS));
    }, 0);
  });

  router.param('bitstream_id', (req, res, next, bitstreamId) => {
    // ensure correct prop type
    const id = req.params.bitstream_id;
    try {
      req.bitstream_id = id;
      req.bitstream = BITSTREAMS.bitstreams.find((bitstream) => {
        return bitstream.id === id;
      });
      next();
    } catch (e) {
      next(new Error('failed to load item'));
    }
  });

  router.route('/bitstreams/:bitstream_id').get((req, res) => {
    // console.log('GET', util.inspect(req.bitstream, { colors: true }));
    res.json(toHALResponse(req, req.bitstream));
  });

  return router;
}
