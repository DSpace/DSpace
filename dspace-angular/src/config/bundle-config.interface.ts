import { Config } from './config.interface';

export interface BundleConfig extends Config {

  /**
   * List of standard bundles to select in adding bitstreams to items
   * Used by {@link UploadBitstreamComponent}.
   */
  standardBundles: string[];

}
