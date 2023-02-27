import { URLCombiner } from '../core/url-combiner/url-combiner';
import { getRequestCopyModulePath } from '../app-routing-paths';

export function getRequestCopyRoute(token: string) {
  return new URLCombiner(getRequestCopyModulePath(), token).toString();
}

export const REQUEST_COPY_DENY_PATH = 'deny';

export function getRequestCopyDenyRoute(token: string) {
  return new URLCombiner(getRequestCopyRoute(token), REQUEST_COPY_DENY_PATH).toString();
}

export const REQUEST_COPY_GRANT_PATH = 'grant';

export function getRequestCopyGrantRoute(token: string) {
  return new URLCombiner(getRequestCopyRoute(token), REQUEST_COPY_GRANT_PATH).toString();
}
