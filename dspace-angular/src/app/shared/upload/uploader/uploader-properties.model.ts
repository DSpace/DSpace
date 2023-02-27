import { MetadataMap } from '../../../core/shared/metadata.models';

/**
 * Properties to send to the REST API for uploading a bitstream
 */
export class UploaderProperties {
  /**
   * A custom name for the bitstream
   */
  name: string;

  /**
   * Metadata for the bitstream (e.g. dc.description)
   */
  metadata: MetadataMap;

  /**
   * The name of the bundle to upload the bitstream to
   */
  bundleName: string;
}
