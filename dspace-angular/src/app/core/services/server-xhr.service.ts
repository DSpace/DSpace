/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

import { XhrFactory } from '@angular/common';
import { Injectable } from '@angular/core';
import { Agent as HttpAgent, AgentOptions as HttpAgentOptions } from 'http';
import { Agent as HttpsAgent } from 'https';
import { prototype, XMLHttpRequest } from 'xhr2';

/**
 * Allow HTTP sessions to be kept alive.
 * Without this configuration, Angular re-connects to REST multiple times per SSR cycle.
 * https://nodejs.org/api/http.html#new-agentoptions
 */
const agentOptions: HttpAgentOptions = {
  keepAlive: true,
  keepAliveMsecs: 60 * 1000,
};

// Agents need to be reused between requests, otherwise keep-alive doesn't help.
const httpAgent = new HttpAgent(agentOptions);
const httpsAgent = new HttpsAgent(agentOptions);

/**
 * Contructs the XMLHttpRequest instances used for all HttpClient requests.
 * Emulated by https://github.com/pwnall/node-xhr2 on the server.
 * This class overrides the built-in Angular implementation to set additional configuration.
 *
 * Changes:
 * - Turn off restriction for cookie headers to allow us to set cookies in requests to the backend.
 *   This was added to be able to perform a working XSRF request from the node server, as it needs to set a cookie for the XSRF token.
 * - Override NodeJS HTTP(S) agents to keep sessions alive between requests.
 *   This improves SSR performance by reducing REST request overhead on the server.
 *
 * Note that this must be provided in ServerAppModule;
 * it doesn't work when added as a Universal engine provider.
 */
@Injectable()
export class ServerXhrService implements XhrFactory {
  build(): XMLHttpRequest {
    prototype._restrictedHeaders.cookie = false;
    const xhr = new XMLHttpRequest();

    // This call is specific to xhr2 and will probably break if we use another library.
    // https://github.com/pwnall/node-xhr2#features
    (xhr as any).nodejsSet({
      httpAgent,
      httpsAgent,
    });

    return xhr;
  }
}
