import { createSelector, MemoizedSelector } from '@ngrx/store';
import { hasValue, isNotEmpty } from '../../shared/empty.util';
import { coreSelector } from '../core.selectors';
import { URLCombiner } from '../url-combiner/url-combiner';
import { IndexState, MetaIndexState } from './index.reducer';
import { IndexName } from './index-name.model';
import { CoreState } from '../core-state.model';

/**
 * Return the given url without `embed` params.
 *
 * E.g. https://rest.api/resource?size=5&embed=subresource&rpp=3
 * becomes https://rest.api/resource?size=5&rpp=3
 *
 * When you index a request url you don't want to include
 * embed params because embedded data isn't relevant when
 * you want to know
 *
 * @param url The url to use
 */
export const getUrlWithoutEmbedParams = (url: string): string => {
  if (isNotEmpty(url)) {
    try {
      const parsed = new URL(url);
      if (isNotEmpty(parsed.search)) {
        const parts = parsed.search.split(/[?|&]/)
          .filter((part: string) => isNotEmpty(part))
          .filter((part: string) => !(part.startsWith('embed=') || part.startsWith('embed.size=')));
        let args = '';
        if (isNotEmpty(parts)) {
          args = `?${parts.join('&')}`;
        }
        url = new URLCombiner(parsed.origin, parsed.pathname, args).toString();
        return url;
      }
    } catch (e) {
      // Ignore parsing errors. By default, we return the original string below.
    }
  }

  return url;
};

/**
 * Parse the embed size params from a url
 * @param url The url to parse
 */
export const getEmbedSizeParams = (url: string): { name: string, size: number }[] => {
  if (isNotEmpty(url)) {
    try {
      const parsed = new URL(url);
      if (isNotEmpty(parsed.search)) {
        return parsed.search.split(/[?|&]/)
          .filter((part: string) => isNotEmpty(part))
          .map((part: string) => part.match(/^embed.size=([^=]+)=(\d+)$/))
          .filter((matches: RegExpMatchArray) => hasValue(matches) && hasValue(matches[1]) && hasValue(matches[2]))
          .map((matches: RegExpMatchArray) => {
            return { name: matches[1], size: Number(matches[2]) };
          });
      }
    } catch (e) {
      // Ignore parsing errors. By default, we return an empty result below.
    }
  }

  return [];
};

/**
 * Return the MetaIndexState based on the CoreSate
 *
 * @returns
 *    a MemoizedSelector to select the MetaIndexState
 */
export const metaIndexSelector: MemoizedSelector<CoreState, MetaIndexState> = createSelector(
  coreSelector,
  (state: CoreState) => state.index
);

/**
 * Return the object index based on the MetaIndexState
 * It contains all objects in the object cache indexed by UUID
 *
 * @returns
 *    a MemoizedSelector to select the object index
 */
export const objectIndexSelector: MemoizedSelector<CoreState, IndexState> = createSelector(
  metaIndexSelector,
  (state: MetaIndexState) => state[IndexName.OBJECT]
);

/**
 * Return the request index based on the MetaIndexState
 *
 * @returns
 *    a MemoizedSelector to select the request index
 */
export const requestIndexSelector: MemoizedSelector<CoreState, IndexState> = createSelector(
  metaIndexSelector,
  (state: MetaIndexState) => state[IndexName.REQUEST]
);

/**
 * Return the alternative link index based on the MetaIndexState
 *
 * @returns
 *    a MemoizedSelector to select the alternative link index
 */
export const alternativeLinkIndexSelector: MemoizedSelector<CoreState, IndexState> = createSelector(
  metaIndexSelector,
  (state: MetaIndexState) => state[IndexName.ALTERNATIVE_OBJECT_LINK]
);

/**
 * Return the self link of an object in the object-cache based on its UUID
 *
 * @param uuid
 *    the UUID for which you want to find the matching self link
 * @returns
 *    a MemoizedSelector to select the self link
 */
export const selfLinkFromUuidSelector =
  (uuid: string): MemoizedSelector<CoreState, string> => createSelector(
    objectIndexSelector,
    (state: IndexState) => hasValue(state) ? state[uuid] : undefined
  );

/**
 * Return the UUID of a GET request based on its href
 *
 * @param href
 *    the href of the GET request
 * @returns
 *    a MemoizedSelector to select the UUID
 */
export const uuidFromHrefSelector =
  (href: string): MemoizedSelector<CoreState, string> => createSelector(
    requestIndexSelector,
    (state: IndexState) => hasValue(state) ? state[getUrlWithoutEmbedParams(href)] : undefined
  );

/**
 * Return the self link of an object based on its alternative link
 *
 * @param altLink
 *    the alternative link of an object
 * @returns
 *    a MemoizedSelector to select the object self link
 */
export const selfLinkFromAlternativeLinkSelector =
  (altLink: string): MemoizedSelector<CoreState, string> => createSelector(
    alternativeLinkIndexSelector,
    (state: IndexState) => hasValue(state) ? state[altLink] : undefined
  );
