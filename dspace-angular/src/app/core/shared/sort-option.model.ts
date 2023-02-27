import { autoserialize } from 'cerialize';

export class SortOption {
  @autoserialize
  name: string;

  @autoserialize
  metadata: string;
}
