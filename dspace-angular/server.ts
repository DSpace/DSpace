/**
 * *** NOTE ON IMPORTING FROM ANGULAR AND NGUNIVERSAL IN THIS FILE ***
 *
 * If your application uses third-party dependencies, you'll need to
 * either use Webpack or the Angular CLI's `bundleDependencies` feature
 * in order to adequately package them for use on the server without a
 * node_modules directory.
 *
 * However, due to the nature of the CLI's `bundleDependencies`, importing
 * Angular in this file will create a different instance of Angular than
 * the version in the compiled application code. This leads to unavoidable
 * conflicts. Therefore, please do not explicitly import from @angular or
 * @nguniversal in this file. You can export any needed resources
 * from your application's main.server.ts file, as seen below with the
 * import for `ngExpressEngine`.
 */

import 'zone.js/node';
import 'reflect-metadata';
import 'rxjs';

/* eslint-disable import/no-namespace */
import * as morgan from 'morgan';
import * as express from 'express';
import * as ejs from 'ejs';
import * as compression from 'compression';
import * as expressStaticGzip from 'express-static-gzip';
/* eslint-enable import/no-namespace */

import axios from 'axios';
import LRU from 'lru-cache';
import isbot from 'isbot';
import { createCertificate } from 'pem';
import { createServer } from 'https';
import { json } from 'body-parser';

import { existsSync, readFileSync } from 'fs';
import { join } from 'path';

import { enableProdMode } from '@angular/core';

import { ngExpressEngine } from '@nguniversal/express-engine';
import { REQUEST, RESPONSE } from '@nguniversal/express-engine/tokens';

import { environment } from './src/environments/environment';
import { createProxyMiddleware } from 'http-proxy-middleware';
import { hasNoValue, hasValue } from './src/app/shared/empty.util';

import { UIServerConfig } from './src/config/ui-server-config.interface';

import { ServerAppModule } from './src/main.server';

import { buildAppConfig } from './src/config/config.server';
import { APP_CONFIG, AppConfig } from './src/config/app-config.interface';
import { extendEnvironmentWithAppConfig } from './src/config/config.util';
import { logStartupMessage } from './startup-message';
import { TOKENITEM } from 'src/app/core/auth/models/auth-token-info.model';


/*
 * Set path for the browser application's dist folder
 */
const DIST_FOLDER = join(process.cwd(), 'dist/browser');
// Set path fir IIIF viewer.
const IIIF_VIEWER = join(process.cwd(), 'dist/iiif');

const indexHtml = join(DIST_FOLDER, 'index.html');

const cookieParser = require('cookie-parser');

const appConfig: AppConfig = buildAppConfig(join(DIST_FOLDER, 'assets/config.json'));

// cache of SSR pages for known bots, only enabled in production mode
let botCache: LRU<string, any>;

// cache of SSR pages for anonymous users. Disabled by default, and only available in production mode
let anonymousCache: LRU<string, any>;

// extend environment with app config for server
extendEnvironmentWithAppConfig(environment, appConfig);

// The Express app is exported so that it can be used by serverless Functions.
export function app() {

  const router = express.Router();

  /*
   * Create a new express application
   */
  const server = express();

  // Tell Express to trust X-FORWARDED-* headers from proxies
  // See https://expressjs.com/en/guide/behind-proxies.html
  server.set('trust proxy', environment.ui.useProxies);

  /*
   * If production mode is enabled in the environment file:
   * - Enable Angular's production mode
   * - Initialize caching of SSR rendered pages (if enabled in config.yml)
   * - Enable compression for SSR reponses. See [compression](https://github.com/expressjs/compression)
   */
  if (environment.production) {
    enableProdMode();
    initCache();
    server.use(compression({
      // only compress responses we've marked as SSR
      // otherwise, this middleware may compress files we've chosen not to compress via compression-webpack-plugin
      filter: (_, res) => res.locals.ssr,
    }));
  }

  /*
   * Enable request logging
   * See [morgan](https://github.com/expressjs/morgan)
   */
  server.use(morgan('dev'));

  /*
   * Add cookie parser middleware
   * See [cookie-parser](https://github.com/expressjs/cookie-parser)
   */
  server.use(cookieParser());

  /*
   * Add JSON parser for request bodies
   * See [body-parser](https://github.com/expressjs/body-parser)
   */
  server.use(json());

  // Our Universal express-engine (found @ https://github.com/angular/universal/tree/master/modules/express-engine)
  server.engine('html', (_, options, callback) =>
    ngExpressEngine({
      bootstrap: ServerAppModule,
      providers: [
        {
          provide: REQUEST,
          useValue: (options as any).req,
        },
        {
          provide: RESPONSE,
          useValue: (options as any).req.res,
        },
        {
          provide: APP_CONFIG,
          useValue: environment
        }
      ]
    })(_, (options as any), callback)
  );

  server.engine('ejs', ejs.renderFile);

  /*
   * Register the view engines for html and ejs
   */
  server.set('view engine', 'html');
  server.set('view engine', 'ejs');

  /**
   * Serve the robots.txt ejs template, filling in the origin variable
   */
  server.get('/robots.txt', (req, res) => {
    res.setHeader('content-type', 'text/plain');
    res.render('assets/robots.txt.ejs', {
      'origin': req.protocol + '://' + req.headers.host
    });
  });

  /*
   * Set views folder path to directory where template files are stored
   */
  server.set('views', DIST_FOLDER);

  /**
   * Proxy the sitemaps
   */
  router.use('/sitemap**', createProxyMiddleware({
    target: `${environment.rest.baseUrl}/sitemaps`,
    pathRewrite: path => path.replace(environment.ui.nameSpace, '/'),
    changeOrigin: true
  }));

  /**
   * Checks if the rateLimiter property is present
   * When it is present, the rateLimiter will be enabled. When it is undefined, the rateLimiter will be disabled.
   */
  if (hasValue((environment.ui as UIServerConfig).rateLimiter)) {
    const RateLimit = require('express-rate-limit');
    const limiter = new RateLimit({
      windowMs: (environment.ui as UIServerConfig).rateLimiter.windowMs,
      max: (environment.ui as UIServerConfig).rateLimiter.max
    });
    server.use(limiter);
  }

  /*
   * Serve static resources (images, i18n messages, …)
   * Handle pre-compressed files with [express-static-gzip](https://github.com/tkoenig89/express-static-gzip)
   */
  router.get('*.*', addCacheControl, expressStaticGzip(DIST_FOLDER, {
    index: false,
    enableBrotli: true,
    orderPreference: ['br', 'gzip'],
  }));

  /*
  * Fallthrough to the IIIF viewer (must be included in the build).
  */
  router.use('/iiif', express.static(IIIF_VIEWER, { index: false }));

  /**
   * Checking server status
   */
  server.get('/app/health', healthCheck);

  /**
   * Default sending all incoming requests to ngApp() function, after first checking for a cached
   * copy of the page (see cacheCheck())
   */
  router.get('*', cacheCheck, ngApp);

  server.use(environment.ui.nameSpace, router);

  return server;
}

/*
 * The callback function to serve server side angular
 */
function ngApp(req, res) {
  if (environment.universal.preboot) {
    // Render the page to user via SSR (server side rendering)
    serverSideRender(req, res);
  } else {
    // If preboot is disabled, just serve the client
    console.log('Universal off, serving for direct client-side rendering (CSR)');
    clientSideRender(req, res);
  }
}

/**
 * Render page content on server side using Angular SSR. By default this page content is
 * returned to the user.
 * @param req current request
 * @param res current response
 * @param sendToUser if true (default), send the rendered content to the user.
 * If false, then only save this rendered content to the in-memory cache (to refresh cache).
 */
function serverSideRender(req, res, sendToUser: boolean = true) {
  // Render the page via SSR (server side rendering)
  res.render(indexHtml, {
    req,
    res,
    preboot: environment.universal.preboot,
    async: environment.universal.async,
    time: environment.universal.time,
    baseUrl: environment.ui.nameSpace,
    originUrl: environment.ui.baseUrl,
    requestUrl: req.originalUrl,
  }, (err, data) => {
    if (hasNoValue(err) && hasValue(data)) {
      // save server side rendered page to cache (if any are enabled)
      saveToCache(req, data);
      if (sendToUser) {
        res.locals.ssr = true;  // mark response as SSR (enables text compression)
        // send rendered page to user
        res.send(data);
      }
    } else if (hasValue(err) && err.code === 'ERR_HTTP_HEADERS_SENT') {
      // When this error occurs we can't fall back to CSR because the response has already been
      // sent. These errors occur for various reasons in universal, not all of which are in our
      // control to solve.
      console.warn('Warning [ERR_HTTP_HEADERS_SENT]: Tried to set headers after they were sent to the client');
    } else {
      console.warn('Error in server-side rendering (SSR)');
      if (hasValue(err)) {
        console.warn('Error details : ', err);
      }
      if (sendToUser) {
        console.warn('Falling back to serving direct client-side rendering (CSR).');
        clientSideRender(req, res);
      }
    }
  });
}

/**
 * Send back response to user to trigger direct client-side rendering (CSR)
 * @param req current request
 * @param res current response
 */
function clientSideRender(req, res) {
  res.sendFile(indexHtml);
}


/*
 * Adds a Cache-Control HTTP header to the response.
 * The cache control value can be configured in the config.*.yml file
 * Defaults to max-age=604,800 seconds (1 week)
 */
function addCacheControl(req, res, next) {
  // instruct browser to revalidate
  res.header('Cache-Control', environment.cache.control || 'max-age=604800');
  next();
}

/*
 * Initialize server-side caching of pages rendered via SSR.
 */
function initCache() {
  if (botCacheEnabled()) {
    // Initialize a new "least-recently-used" item cache (where least recently used pages are removed first)
    // See https://www.npmjs.com/package/lru-cache
    // When enabled, each page defaults to expiring after 1 day
    botCache = new LRU( {
      max: environment.cache.serverSide.botCache.max,
      ttl: environment.cache.serverSide.botCache.timeToLive || 24 * 60 * 60 * 1000, // 1 day
      allowStale: environment.cache.serverSide.botCache.allowStale ?? true // if object is stale, return stale value before deleting
    });
  }

  if (anonymousCacheEnabled()) {
    // NOTE: While caches may share SSR pages, this cache must be kept separately because the timeToLive
    // may expire pages more frequently.
    // When enabled, each page defaults to expiring after 10 seconds (to minimize anonymous users seeing out-of-date content)
    anonymousCache = new LRU( {
      max: environment.cache.serverSide.anonymousCache.max,
      ttl: environment.cache.serverSide.anonymousCache.timeToLive || 10 * 1000, // 10 seconds
      allowStale: environment.cache.serverSide.anonymousCache.allowStale ?? true // if object is stale, return stale value before deleting
    });
  }
}

/**
 * Return whether bot-specific server side caching is enabled in configuration.
 */
function botCacheEnabled(): boolean {
  // Caching is only enabled if SSR is enabled AND
  // "max" pages to cache is greater than zero
  return environment.universal.preboot && environment.cache.serverSide.botCache.max && (environment.cache.serverSide.botCache.max > 0);
}

/**
 * Return whether anonymous user server side caching is enabled in configuration.
 */
function anonymousCacheEnabled(): boolean {
  // Caching is only enabled if SSR is enabled AND
  // "max" pages to cache is greater than zero
  return environment.universal.preboot && environment.cache.serverSide.anonymousCache.max && (environment.cache.serverSide.anonymousCache.max > 0);
}

/**
 * Check if the currently requested page is in our server-side, in-memory cache.
 * Caching is ONLY done for SSR requests. Pages are cached base on their path (e.g. /home or /search?query=test)
 */
function cacheCheck(req, res, next) {
  // Cached copy of page (if found)
  let cachedCopy;

  // If the bot cache is enabled and this request looks like a bot, check the bot cache for a cached page.
  if (botCacheEnabled() && isbot(req.get('user-agent'))) {
    cachedCopy = checkCacheForRequest('bot', botCache, req, res);
  } else if (anonymousCacheEnabled() && !isUserAuthenticated(req)) {
    cachedCopy = checkCacheForRequest('anonymous', anonymousCache, req, res);
  }

  // If cached copy exists, return it to the user.
  if (cachedCopy) {
    res.locals.ssr = true;  // mark response as SSR-generated (enables text compression)
    res.send(cachedCopy);

    // Tell Express to skip all other handlers for this path
    // This ensures we don't try to re-render the page since we've already returned the cached copy
    next('router');
  } else {
    // If nothing found in cache, just continue with next handler
    // (This should send the request on to the handler that rerenders the page via SSR
    next();
  }
}

/**
 * Checks if the current request (i.e. page) is found in the given cache. If it is found,
 * the cached copy is returned. When found, this method also triggers a re-render via
 * SSR if the cached copy is now expired (i.e. timeToLive has passed for this cached copy).
 * @param cacheName name of cache (just useful for debug logging)
 * @param cache LRU cache to check
 * @param req current request to look for in the cache
 * @param res current response
 * @returns cached copy (if found) or undefined (if not found)
 */
function checkCacheForRequest(cacheName: string, cache: LRU<string, any>, req, res): any {
  // Get the cache key for this request
  const key = getCacheKey(req);

  // Check if this page is in our cache
  let cachedCopy = cache.get(key);
  if (cachedCopy) {
    if (environment.cache.serverSide.debug) { console.log(`CACHE HIT FOR ${key} in ${cacheName} cache`); }

    // Check if cached copy is expired (If expired, the key will now be gone from cache)
    // NOTE: This will only occur when "allowStale=true", as it means the "get(key)" above returned a stale value.
    if (!cache.has(key)) {
      if (environment.cache.serverSide.debug) { console.log(`CACHE EXPIRED FOR ${key} in ${cacheName} cache. Re-rendering...`); }
      // Update cached copy by rerendering server-side
      // NOTE: In this scenario the currently cached copy will be returned to the current user.
      // This re-render is peformed behind the scenes to update cached copy for next user.
      serverSideRender(req, res, false);
    }
  } else {
    if (environment.cache.serverSide.debug) { console.log(`CACHE MISS FOR ${key} in ${cacheName} cache.`); }
  }

  // return page from cache
  return cachedCopy;
}

/**
 * Create a cache key from the current request.
 * The cache key is the URL path (NOTE: this key will also include any querystring params).
 * E.g. "/home" or "/search?query=test"
 * @param req current request
 * @returns cache key to use for this page
 */
function getCacheKey(req): string {
  // NOTE: this will return the URL path *without* any baseUrl
  return req.url;
}

/**
 * Save page to server side cache(s), if enabled. If caching is not enabled or a user is authenticated, this is a noop
 * If multiple caches are enabled, the page will be saved to any caches where it does not yet exist (or is expired).
 * (This minimizes the number of times we need to run SSR on the same page.)
 * @param req current page request
 * @param page page data to save to cache
 */
function saveToCache(req, page: any) {
  // Only cache if no one is currently authenticated. This means ONLY public pages can be cached.
  // NOTE: It's not safe to save page data to the cache when a user is authenticated. In that situation,
  // the page may include sensitive or user-specific materials. As the cache is shared across all users, it can only contain public info.
  if (!isUserAuthenticated(req)) {
    const key = getCacheKey(req);
    // Avoid caching "/reload/[random]" paths (these are hard refreshes after logout)
    if (key.startsWith('/reload')) { return; }

    // If bot cache is enabled, save it to that cache if it doesn't exist or is expired
    // (NOTE: has() will return false if page is expired in cache)
    if (botCacheEnabled() && !botCache.has(key)) {
      botCache.set(key, page);
      if (environment.cache.serverSide.debug) { console.log(`CACHE SAVE FOR ${key} in bot cache.`); }
    }

    // If anonymous cache is enabled, save it to that cache if it doesn't exist or is expired
    if (anonymousCacheEnabled() && !anonymousCache.has(key)) {
      anonymousCache.set(key, page);
      if (environment.cache.serverSide.debug) { console.log(`CACHE SAVE FOR ${key} in anonymous cache.`); }
    }
  }
}

/**
 * Whether a user is authenticated or not
 */
function isUserAuthenticated(req): boolean {
  // Check whether our DSpace authentication Cookie exists or not
  return req.cookies[TOKENITEM];
}

/*
 * Callback function for when the server has started
 */
function serverStarted() {
  console.log(`[${new Date().toTimeString()}] Listening at ${environment.ui.baseUrl}`);
}

/*
 * Create an HTTPS server with the configured port and host
 * @param keys SSL credentials
 */
function createHttpsServer(keys) {
  createServer({
    key: keys.serviceKey,
    cert: keys.certificate
  }, app).listen(environment.ui.port, environment.ui.host, () => {
    serverStarted();
  });
}

function run() {
  const port = environment.ui.port || 4000;
  const host = environment.ui.host || '/';

  // Start up the Node server
  const server = app();
  server.listen(port, host, () => {
    serverStarted();
  });
}

function start() {
  logStartupMessage(environment);

  /*
  * If SSL is enabled
  * - Read credentials from configuration files
  * - Call script to start an HTTPS server with these credentials
  * When SSL is disabled
  * - Start an HTTP server on the configured port and host
  */
  if (environment.ui.ssl) {
    let serviceKey;
    try {
      serviceKey = readFileSync('./config/ssl/key.pem');
    } catch (e) {
      console.warn('Service key not found at ./config/ssl/key.pem');
    }

    let certificate;
    try {
      certificate = readFileSync('./config/ssl/cert.pem');
    } catch (e) {
      console.warn('Certificate not found at ./config/ssl/key.pem');
    }

    if (serviceKey && certificate) {
      createHttpsServer({
        serviceKey: serviceKey,
        certificate: certificate
      });
    } else {
      console.warn('Disabling certificate validation and proceeding with a self-signed certificate. If this is a production server, it is recommended that you configure a valid certificate instead.');

      process.env.NODE_TLS_REJECT_UNAUTHORIZED = '0'; // lgtm[js/disabling-certificate-validation]

      createCertificate({
        days: 1,
        selfSigned: true
      }, (error, keys) => {
        createHttpsServer(keys);
      });
    }
  } else {
    run();
  }
}

/*
 * The callback function to serve health check requests
 */
function healthCheck(req, res) {
  const baseUrl = `${environment.rest.baseUrl}${environment.actuators.endpointPath}`;
  axios.get(baseUrl)
    .then((response) => {
      res.status(response.status).send(response.data);
    })
    .catch((error) => {
      res.status(error.response.status).send({
        error: error.message
      });
    });
}
// Webpack will replace 'require' with '__webpack_require__'
// '__non_webpack_require__' is a proxy to Node 'require'
// The below code is to ensure that the server is run only when not requiring the bundle.
declare const __non_webpack_require__: NodeRequire;
const mainModule = __non_webpack_require__.main;
const moduleFilename = (mainModule && mainModule.filename) || '';
if (moduleFilename === __filename || moduleFilename.includes('iisnode')) {
  start();
}

export * from './src/main.server';
