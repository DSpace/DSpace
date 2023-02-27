import { Config } from './config.interface';
import { AutoSyncConfig } from './auto-sync-config.interface';

export interface CacheConfig extends Config {
  msToLive: {
    default: number;
  };
  // Cache-Control HTTP Header
  control: string;
  autoSync: AutoSyncConfig;
  // In-memory caches of server-side rendered (SSR) content. These caches can be used to limit the frequency
  // of re-generating SSR pages to improve performance.
  serverSide: {
    // Debug server-side caching.  Set to true to see cache hits/misses/refreshes in console logs.
    debug: boolean,
    // Cache specific to known bots.  Allows you to serve cached contents to bots only.
    botCache: {
      // Maximum number of pages (rendered via SSR) to cache. Setting max=0 disables the cache.
      max: number;
      // Amount of time after which cached pages are considered stale (in ms)
      timeToLive: number;
      // true = return page from cache after timeToLive expires. false = return a fresh page after timeToLive expires
      allowStale: boolean;
    },
    // Cache specific to anonymous users. Allows you to serve cached content to non-authenticated users.
    anonymousCache: {
      // Maximum number of pages (rendered via SSR) to cache. Setting max=0 disables the cache.
      max: number;
      // Amount of time after which cached pages are considered stale (in ms)
      timeToLive: number;
      // true = return page from cache after timeToLive expires. false = return a fresh page after timeToLive expires
      allowStale: boolean;
    }
  }
}
