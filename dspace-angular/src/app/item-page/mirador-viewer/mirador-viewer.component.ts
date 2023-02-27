import { ChangeDetectionStrategy, Component, Inject, Input, OnInit, PLATFORM_ID } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Item } from '../../core/shared/item.model';
import { environment } from '../../../environments/environment';
import { BitstreamDataService } from '../../core/data/bitstream-data.service';
import { Observable, of } from 'rxjs';
import { map, take } from 'rxjs/operators';
import { isPlatformBrowser } from '@angular/common';
import { MiradorViewerService } from './mirador-viewer.service';
import { HostWindowService, WidthCategory } from '../../shared/host-window.service';
import { BundleDataService } from '../../core/data/bundle-data.service';

@Component({
  selector: 'ds-mirador-viewer',
  styleUrls: ['./mirador-viewer.component.scss'],
  templateUrl: './mirador-viewer.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [ MiradorViewerService ]
})
export class MiradorViewerComponent implements OnInit {

  @Input() object: Item;

  /**
   * A previous dspace search query.
   */
  @Input() query: string;

  /**
   * True if searchable.
   */
  @Input() searchable: boolean;

  /**
   * Hides embedded viewer in dev mode.
   */
  isViewerAvailable = true;

  /**
   * The url for the iframe.
   */
  iframeViewerUrl: Observable<SafeResourceUrl>;

  /**
   * Sets the viewer to show or hide thumbnail side navigation menu.
   */
  multi = false;

  /**
   * Hides the thumbnail navigation menu on smaller viewports.
   */
  notMobile = false;

  viewerMessage = 'Sorry, the Mirador viewer is not currently available in development mode.';

  constructor(private sanitizer: DomSanitizer,
              private viewerService: MiradorViewerService,
              private bitstreamDataService: BitstreamDataService,
              private bundleDataService: BundleDataService,
              private hostWindowService: HostWindowService,
              @Inject(PLATFORM_ID) private platformId: any) {
  }

  /**
   * Creates the url for the Mirador iframe. Adds parameters for the displaying the search panel, query results,
   * or  multi-page thumbnail navigation.
   */
  setURL() {
    // The path to the REST manifest endpoint.
    const manifestApiEndpoint = encodeURIComponent(environment.rest.baseUrl + '/iiif/'
      + this.object.id + '/manifest');
    // The Express path to Mirador viewer.
    let viewerPath = '/iiif/mirador/index.html?manifest=' + manifestApiEndpoint;
    if (this.searchable) {
      // Tell the viewer add search to menu.
      viewerPath += '&searchable=' + this.searchable;
    }
    if (this.query) {
      // Tell the viewer to execute a search for the query term.
      viewerPath += '&query=' + this.query;
    }
    if (this.multi) {
      // Tell the viewer to add thumbnail navigation. If searchable, thumbnail navigation is added by default.
      viewerPath += '&multi=' + this.multi;
    }
    if (this.notMobile) {
      viewerPath += '&notMobile=true';
    }

    // TODO: Should the query term be trusted here?
    return this.sanitizer.bypassSecurityTrustResourceUrl(viewerPath);
  }

  ngOnInit(): void {
    /**
     * Initializes the iframe url observable.
     */
    if (isPlatformBrowser(this.platformId)) {

      // Viewer is not currently available in dev mode so hide it in that case.
      this.isViewerAvailable = this.viewerService.showEmbeddedViewer();

      // The notMobile property affects the thumbnail navigation
      // menu by hiding it for smaller viewports. This will not be
      // responsive to resizing.
      this.hostWindowService.widthCategory
          .pipe(take(1))
          .subscribe((category: WidthCategory) => {
            this.notMobile = !(category === WidthCategory.XS || category === WidthCategory.SM);
          });

      // Set the multi property. The default mirador configuration adds a right
      // thumbnail navigation panel to the viewer when multi is 'true'.

      // Set the multi property to 'true' if the item is searchable.
      if (this.searchable) {
        this.multi = true;
        const observable = of('');
        this.iframeViewerUrl = observable.pipe(
          map((val) => {
            return this.setURL();
          })
        );
      } else {
        // Set the multi property based on the image count in IIIF-eligible bundles.
        // Any count greater than 1 sets the value to 'true'.
        this.iframeViewerUrl = this.viewerService.getImageCount(
          this.object,
          this.bitstreamDataService,
          this.bundleDataService).pipe(
          map(c => {
            if (c > 1) {
              this.multi = true;
            }
            return this.setURL();
          })
        );
      }
    }
  }
}
