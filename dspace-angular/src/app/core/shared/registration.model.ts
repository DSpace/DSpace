import { typedObject } from '../cache/builders/build-decorators';
import { ResourceType } from './resource-type';
import { REGISTRATION } from './registration.resource-type';
import { UnCacheableObject } from './uncacheable-object.model';

@typedObject
export class Registration implements UnCacheableObject {
  static type = REGISTRATION;
  /**
   * The object type
   */
  type: ResourceType;

  /**
   * The email linked to the registration
   */
  email: string;

  /**
   * The user linked to the registration
   */
  user: string;

  /**
   * The token linked to the registration
   */
  token: string;
  /**
   * The token linked to the registration
   */
  groupNames: string[];
  /**
   * The token linked to the registration
   */
  groups: string[];
}
