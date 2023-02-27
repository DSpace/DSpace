import { Component, Input, OnInit } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { BitstreamDataService } from '../../core/data/bitstream-data.service';
import { PaginatedList } from '../../core/data/paginated-list.model';
import { RemoteData } from '../../core/data/remote-data';
import { BitstreamFormat } from '../../core/shared/bitstream-format.model';
import { Bitstream } from '../../core/shared/bitstream.model';
import { Item } from '../../core/shared/item.model';
import { MediaViewerItem } from '../../core/shared/media-viewer-item.model';
import { getFirstSucceededRemoteDataPayload } from '../../core/shared/operators';
import { hasValue } from '../../shared/empty.util';
import { followLink } from '../../shared/utils/follow-link-config.model';

/**
 * This componenet renders the media viewers
 */

@Component({
  selector: 'ds-media-viewer',
  templateUrl: './media-viewer.component.html',
  styleUrls: ['./media-viewer.component.scss'],
})
export class MediaViewerComponent implements OnInit {
  @Input() item: Item;
  @Input() videoOptions: boolean;

  mediaList$: BehaviorSubject<MediaViewerItem[]>;

  isLoading: boolean;

  thumbnailPlaceholder = './assets/images/replacement_document.svg';

  constructor(protected bitstreamDataService: BitstreamDataService) {}

  /**
   * This metod loads all the Bitstreams and Thumbnails and contert it to media item
   */
  ngOnInit(): void {
    this.mediaList$ = new BehaviorSubject([]);
    this.isLoading = true;
    this.loadRemoteData('ORIGINAL').subscribe((bitstreamsRD) => {
      if (bitstreamsRD.payload.page.length === 0) {
        this.isLoading = false;
        this.mediaList$.next([]);
      } else {
        this.loadRemoteData('THUMBNAIL').subscribe((thumbnailsRD) => {
          for (
            let index = 0;
            index < bitstreamsRD.payload.page.length;
            index++
          ) {
            bitstreamsRD.payload.page[index].format
              .pipe(getFirstSucceededRemoteDataPayload())
              .subscribe((format) => {
                const current = this.mediaList$.getValue();
                const mediaItem = this.createMediaViewerItem(
                  bitstreamsRD.payload.page[index],
                  format,
                  thumbnailsRD.payload && thumbnailsRD.payload.page[index]
                );
                this.mediaList$.next([...current, mediaItem]);
              });
          }
          this.isLoading = false;
        });
      }
    });
  }

  /**
   * This method will retrieve the next page of Bitstreams from the external BitstreamDataService call.
   * @param bundleName Bundle name
   */
  loadRemoteData(
    bundleName: string
  ): Observable<RemoteData<PaginatedList<Bitstream>>> {
    return this.bitstreamDataService
      .findAllByItemAndBundleName(
        this.item,
        bundleName,
        {},
        true,
        true,
        followLink('format')
      )
      .pipe(
        filter(
          (bitstreamsRD: RemoteData<PaginatedList<Bitstream>>) =>
            hasValue(bitstreamsRD) &&
            (hasValue(bitstreamsRD.errorMessage) || hasValue(bitstreamsRD.payload))
        ),
        take(1)
      );
  }

  /**
   * This method create MediaViewerItem from incoming bitstreams
   * @param original original remote data bitstream
   * @param format original bitstream format
   * @param thumbnail trunbnail remote data bitstream
   */
  createMediaViewerItem(
    original: Bitstream,
    format: BitstreamFormat,
    thumbnail: Bitstream
  ): MediaViewerItem {
    const mediaItem = new MediaViewerItem();
    mediaItem.bitstream = original;
    mediaItem.format = format.mimetype.split('/')[0];
    mediaItem.mimetype = format.mimetype;
    mediaItem.thumbnail = thumbnail ? thumbnail._links.content.href : null;
    return mediaItem;
  }
}
