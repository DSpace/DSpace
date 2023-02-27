/**
 * Enum representing the Support Level of a Bitstream Format
 */
export enum SupportLevel {
  /**
   * Unknown for Bitstream Formats that are unknown to the system
   */
  Unknown = 0,

  /**
   * Known for Bitstream Formats that are known to the system, but not fully supported
   */
  Known = 1,

  /**
   * Supported for Bitstream Formats that are known to the system and fully supported
   */
  Supported = 2,
}
