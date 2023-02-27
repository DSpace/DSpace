import { ResourceType } from '../shared/resource-type';

export abstract class TypedObject {
  static type: ResourceType;
  type: ResourceType;
}
