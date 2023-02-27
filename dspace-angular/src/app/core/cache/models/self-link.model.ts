import { autoserialize } from 'cerialize';

export class SelfLink {

  @autoserialize
  self: string;

  @autoserialize
  uuid: string;

}
