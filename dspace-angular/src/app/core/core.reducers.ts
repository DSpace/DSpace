import { ActionReducerMap, } from '@ngrx/store';

import { objectCacheReducer } from './cache/object-cache.reducer';
import { indexReducer } from './index/index.reducer';
import { requestReducer } from './data/request.reducer';
import { authReducer } from './auth/auth.reducer';
import { jsonPatchOperationsReducer } from './json-patch/json-patch-operations.reducer';
import { serverSyncBufferReducer } from './cache/server-sync-buffer.reducer';
import { objectUpdatesReducer } from './data/object-updates/object-updates.reducer';
import { routeReducer } from './services/route.reducer';
import {
  bitstreamFormatReducer
} from '../admin/admin-registries/bitstream-formats/bitstream-format.reducers';
import { historyReducer } from './history/history.reducer';
import { metaTagReducer } from './metadata/meta-tag.reducer';
import { CoreState } from './core-state.model';

export const coreReducers: ActionReducerMap<CoreState> = {
  'bitstreamFormats': bitstreamFormatReducer,
  'cache/object': objectCacheReducer,
  'cache/syncbuffer': serverSyncBufferReducer,
  'cache/object-updates': objectUpdatesReducer,
  'data/request': requestReducer,
  'history': historyReducer,
  'index': indexReducer,
  'auth': authReducer,
  'json/patch': jsonPatchOperationsReducer,
  'metaTag': metaTagReducer,
  'route': routeReducer
};
