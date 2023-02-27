import { Bitstream } from './bitstream.model';

/**
 * Model representing a media viewer item
 */
export class MediaViewerItem {
  /**
   * Incoming Bitsream
   */
  bitstream: Bitstream;

  /**
   * Incoming Bitsream format type
   */
  format: string;

  /**
   * Incoming Bitsream format mime type
   */
  mimetype: string;

  /**
   * Incoming Bitsream thumbnail
   */
  thumbnail: string;
}
