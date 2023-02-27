import { Config } from './config.interface';

export interface CollectionPageConfig extends Config {
  edit: {
    undoTimeout: number;
  };
}
