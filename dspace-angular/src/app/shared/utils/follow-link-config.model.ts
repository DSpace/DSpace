import { HALResource } from '../../core/shared/hal-resource.model';
import { hasValue } from '../empty.util';
import { FindListOptions } from '../../core/data/find-list-options.model';

/**
 * A class to send the retrieval of a {@link HALLink}
 */
export class FollowLinkConfig<R extends HALResource> {
  /**
   * The name of the link to fetch.
   * Can only be a {@link HALLink} of the object you're working with
   */
  name: keyof R['_links'];

  /**
   * {@link FindListOptions} for the query,
   * allows you to resolve the link using a certain page, or sorted
   * in a certain way
   */
  findListOptions?: FindListOptions;

  /**
   * A list of {@link FollowLinkConfig}s to
   * use on the retrieved object.
   */
  linksToFollow?: FollowLinkConfig<any>[];

  /**
   * Forward to rest which links we're following, so these can already be embedded
   */
  shouldEmbed? = true;

  /**
   * If this is true, the link will only be retrieved if there's no valid cached version. Defaults
   * to true
   */
  useCachedVersionIfAvailable? = true;

  /**
   * If this is true, the link will automatically be re-requested after the response becomes stale.
   * Defaults to true
   */
  reRequestOnStale? = true;

  /**
   * If this is false an error will be thrown if the link doesn't exist on the model it is used on
   * Defaults to false
   */
  isOptional? = false;
}

/**
 * A factory function for {@link FollowLinkConfig}s,
 * in order to create them in a less verbose way.
 *
 * @param linkName: the name of the link to fetch.
 * Can only be a {@link HALLink} of the object you're working with
 * @param findListOptions: {@link FindListOptions} for the query,
 * allows you to resolve the link using a certain page, or sorted
 * in a certain way
 * @param shouldEmbed: boolean to check whether to forward info on followLinks to rest,
 * so these can be embedded, default true
 * @param useCachedVersionIfAvailable: If this is true, the link will only be retrieved if there's
 * no valid cached version. Defaults
 * @param reRequestOnStale: Whether or not the link should automatically be re-requested after the
 * response becomes stale
 * @param isOptional: Whether or not to fail if the link doesn't exist
 * @param linksToFollow: a list of {@link FollowLinkConfig}s to
 * use on the retrieved object.
 */
export const followLink = <R extends HALResource>(
  linkName: keyof R['_links'],
  {
    findListOptions,
    shouldEmbed,
    useCachedVersionIfAvailable,
    reRequestOnStale,
    isOptional
  }: {
    findListOptions?: FindListOptions,
    shouldEmbed?: boolean,
    useCachedVersionIfAvailable?: boolean,
    reRequestOnStale?: boolean,
    isOptional?: boolean,
  } = {},
  ...linksToFollow: FollowLinkConfig<any>[]
): FollowLinkConfig<R> => {
  const followLinkConfig = {
    name: linkName,
    findListOptions: hasValue(findListOptions) ? findListOptions : new FindListOptions(),
    shouldEmbed: hasValue(shouldEmbed) ? shouldEmbed : true,
    useCachedVersionIfAvailable: hasValue(useCachedVersionIfAvailable) ? useCachedVersionIfAvailable : true,
    reRequestOnStale: hasValue(reRequestOnStale) ? reRequestOnStale : true,
    isOptional: hasValue(isOptional) ? isOptional : false,
    linksToFollow
  };
  return followLinkConfig;
};
