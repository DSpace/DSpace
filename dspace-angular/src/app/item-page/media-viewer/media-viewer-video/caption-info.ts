/*
  The class is designed to host information related to Video Captioning support
  and used in HTML 5 video track
  src: source vtt file
  srclang: two letter language code
  langLabel: language label
 */
export class CaptionInfo {
  constructor(public src: string, public srclang: string, public langLabel: string ) {
  }
}
