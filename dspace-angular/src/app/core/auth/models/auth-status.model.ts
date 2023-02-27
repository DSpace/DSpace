import { autoserialize, deserialize, deserializeAs } from 'cerialize';
import { Observable } from 'rxjs';
import { link, typedObject } from '../../cache/builders/build-decorators';
import { IDToUUIDSerializer } from '../../cache/id-to-uuid-serializer';
import { RemoteData } from '../../data/remote-data';
import { EPerson } from '../../eperson/models/eperson.model';
import { EPERSON } from '../../eperson/models/eperson.resource-type';
import { Group } from '../../eperson/models/group.model';
import { GROUP } from '../../eperson/models/group.resource-type';
import { HALLink } from '../../shared/hal-link.model';
import { ResourceType } from '../../shared/resource-type';
import { excludeFromEquals } from '../../utilities/equals.decorators';
import { AuthError } from './auth-error.model';
import { AUTH_STATUS } from './auth-status.resource-type';
import { AuthTokenInfo } from './auth-token-info.model';
import { AuthMethod } from './auth.method';
import { CacheableObject } from '../../cache/cacheable-object.model';
import { PaginatedList } from '../../data/paginated-list.model';

/**
 * Object that represents the authenticated status of a user
 */
@typedObject
export class AuthStatus implements CacheableObject {
  static type = AUTH_STATUS;

  /**
   * The unique identifier of this auth status
   */
  @autoserialize
  id: string;

  /**
   * The type for this AuthStatus
   */
  @excludeFromEquals
  @autoserialize
  type: ResourceType;

  /**
   * The UUID of this auth status
   * This UUID is generated client-side and isn't used by the backend.
   * It is based on the ID, so it will be the same for each refresh.
   */
  @deserializeAs(new IDToUUIDSerializer('auth-status'), 'id')
  uuid: string;

  /**
   * True if REST API is up and running, should never return false
   */
  @autoserialize
  okay: boolean;

  /**
   * If the auth status represents an authenticated state
   */
  @autoserialize
  authenticated: boolean;

  /**
   * The {@link HALLink}s for this AuthStatus
   */
  @deserialize
  _links: {
    self: HALLink;
    eperson: HALLink;
    specialGroups: HALLink;
  };

  /**
   * The EPerson of this auth status
   * Will be undefined unless the eperson {@link HALLink} has been resolved.
   */
  @link(EPERSON)
  eperson?: Observable<RemoteData<EPerson>>;

  /**
   * The SpecialGroup of this auth status
   * Will be undefined unless the SpecialGroup {@link HALLink} has been resolved.
   */
  @link(GROUP, true)
  specialGroups?: Observable<RemoteData<PaginatedList<Group>>>;

  /**
   * True if the token is valid, false if there was no token or the token wasn't valid
   */
  @autoserialize
  token?: AuthTokenInfo;

  /**
   * Authentication error if there was one for this status
   */
  // TODO should be refactored to use the RemoteData error
  @autoserialize
  error?: AuthError;

  /**
   * All authentication methods enabled at the backend
   */
  @autoserialize
  authMethods: AuthMethod[];

}
