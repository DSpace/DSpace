import { followLink, FollowLinkConfig } from './follow-link-config.model';
import { Relationship } from '../../core/shared/item-relationships/relationship.model';

/**
 * Get the query for looking up items by relation type
 * @param {string} relationType   Relation type
 * @param {string} itemUUID       Item UUID
 * @returns {string}              Query
 */
export function getQueryByRelations(relationType: string, itemUUID: string): string {
  return `query=relation.${relationType}:"${itemUUID}"`;
}

/**
 * Get the filter for a relation with the item's UUID
 * @param relationType    The type of relation e.g. 'isAuthorOfPublication'
 * @param itemUUID        The item's UUID
 */
export function getFilterByRelation(relationType: string, itemUUID: string): string {
  return `f.${relationType}=${itemUUID},equals`;
}

/**
 * Creates links to follow for the leftItem and rightItem. Links will include
 * @param showThumbnail thumbnail image configuration
 * @returns followLink array
 */
export function itemLinksToFollow(showThumbnail: boolean):  FollowLinkConfig<Relationship>[] {
  let linksToFollow: FollowLinkConfig<Relationship>[];
  if (showThumbnail) {
    linksToFollow = [
      followLink('leftItem',{}, followLink('thumbnail')),
      followLink('rightItem',{}, followLink('thumbnail'))
    ];
  } else {
    linksToFollow = [followLink('leftItem'), followLink('rightItem')];
  }
  return linksToFollow;
}
