import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';

/**
 * This pipe explicitly escapes the sanitization of a URL,
 * only use this when you are sure the URL is indeed safe
 */

@Pipe({ name: 'dsSafeUrl' })
export class SafeUrlPipe implements PipeTransform {
  constructor(private domSanitizer: DomSanitizer) { }
  transform(url) {
    return this.domSanitizer.bypassSecurityTrustResourceUrl(url);
  }
}
