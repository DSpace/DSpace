import { Component, Input, OnInit } from '@angular/core';
import { NgxGalleryImage, NgxGalleryOptions } from '@kolkov/ngx-gallery';
import { MediaViewerItem } from '../../../core/shared/media-viewer-item.model';
import { NgxGalleryAnimation } from '@kolkov/ngx-gallery';
import { Observable } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';

/**
 * This componenet render an image gallery for the image viewer
 */
@Component({
  selector: 'ds-media-viewer-image',
  templateUrl: './media-viewer-image.component.html',
  styleUrls: ['./media-viewer-image.component.scss'],
})
export class MediaViewerImageComponent implements OnInit {
  @Input() images: MediaViewerItem[];
  @Input() preview?: boolean;
  @Input() image?: string;

  loggedin: boolean;

  galleryOptions: NgxGalleryOptions[];
  galleryImages: NgxGalleryImage[];

  /**
   * Whether or not the current user is authenticated
   */
  isAuthenticated$: Observable<boolean>;

  constructor(private authService: AuthService) {}

  /**
   * Thi method sets up the gallery settings and data
   */
  ngOnInit(): void {
    this.isAuthenticated$ = this.authService.isAuthenticated();
    this.galleryOptions = [
      {
        preview: this.preview !== undefined ? this.preview : true,
        image: true,
        imageSize: 'contain',
        thumbnails: false,
        imageArrows: false,
        startIndex: 0,
        imageAnimation: NgxGalleryAnimation.Slide,
        previewCloseOnEsc: true,
        previewZoom: true,
        previewRotate: true,
        previewFullscreen: true,
      },
    ];

    if (this.image) {
      this.galleryImages = [
        {
          small: this.image,
          medium: this.image,
          big: this.image,
        },
      ];
    } else {
      this.galleryImages = this.convertToGalleryImage(this.images);
    }
  }

  /**
   * This method convert an array of MediaViewerItem into NgxGalleryImage array
   * @param medias input NgxGalleryImage array
   */
  convertToGalleryImage(medias: MediaViewerItem[]): NgxGalleryImage[] {
    const mappadImages = [];
    for (const image of medias) {
      if (image.format === 'image') {
        mappadImages.push({
          small: image.thumbnail
            ? image.thumbnail
            : './assets/images/replacement_image.svg',
          medium: image.thumbnail
            ? image.thumbnail
            : './assets/images/replacement_image.svg',
          big: image.bitstream._links.content.href,
        });
      }
    }
    return mappadImages;
  }
}
