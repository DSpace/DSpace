import { Component, Inject, OnInit } from '@angular/core';
import { BitstreamDataService } from '../../../../../core/data/bitstream-data.service';
import { SearchResultListElementComponent } from '../../../../../shared/object-list/search-result-list-element/search-result-list-element.component';
import { ItemSearchResult } from '../../../../../shared/object-collection/shared/item-search-result.model';
import { listableObjectComponent } from '../../../../../shared/object-collection/shared/listable-object/listable-object.decorator';
import { ViewMode } from '../../../../../core/shared/view-mode.model';
import { Item } from '../../../../../core/shared/item.model';
import { Context } from '../../../../../core/shared/context.model';
import { RelationshipDataService } from '../../../../../core/data/relationship-data.service';
import { TruncatableService } from '../../../../../shared/truncatable/truncatable.service';
import { take } from 'rxjs/operators';
import { NotificationsService } from '../../../../../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { NameVariantModalComponent } from '../../name-variant-modal/name-variant-modal.component';
import { MetadataValue } from '../../../../../core/shared/metadata.models';
import { ItemDataService } from '../../../../../core/data/item-data.service';
import { SelectableListService } from '../../../../../shared/object-list/selectable-list/selectable-list.service';
import { DSONameService } from '../../../../../core/breadcrumbs/dso-name.service';
import { APP_CONFIG, AppConfig } from '../../../../../../config/app-config.interface';

@listableObjectComponent('PersonSearchResult', ViewMode.ListElement, Context.EntitySearchModalWithNameVariants)
@Component({
  selector: 'ds-person-search-result-list-submission-element',
  styleUrls: ['./person-search-result-list-submission-element.component.scss'],
  templateUrl: './person-search-result-list-submission-element.component.html'
})

/**
 * The component for displaying a list element for an item search result of the type Person
 */
export class PersonSearchResultListSubmissionElementComponent extends SearchResultListElementComponent<ItemSearchResult, Item> implements OnInit {
  allSuggestions: string[];
  selectedName: string;
  alternativeField = 'dc.title.alternative';

  /**
   * Display thumbnail if required by configuration
   */
  showThumbnails: boolean;

  constructor(protected truncatableService: TruncatableService,
              private relationshipService: RelationshipDataService,
              private notificationsService: NotificationsService,
              private translateService: TranslateService,
              private modalService: NgbModal,
              private itemDataService: ItemDataService,
              private bitstreamDataService: BitstreamDataService,
              private selectableListService: SelectableListService,
              protected dsoNameService: DSONameService,
              @Inject(APP_CONFIG) protected appConfig: AppConfig
  ) {
    super(truncatableService, dsoNameService, appConfig);
  }

  ngOnInit() {
    super.ngOnInit();
    const defaultValue = this.dso ? this.dsoNameService.getName(this.dso) : undefined;
    const alternatives = this.allMetadataValues(this.alternativeField);
    this.allSuggestions = [defaultValue, ...alternatives];

    this.relationshipService.getNameVariant(this.listID, this.dso.uuid)
      .pipe(take(1))
      .subscribe((nameVariant: string) => {
        this.selectedName = nameVariant || defaultValue;
        }
      );
    this.showThumbnails = this.appConfig.browseBy.showThumbnails;
  }

  select(value) {
    this.relationshipService.setNameVariant(this.listID, this.dso.uuid, value);
    this.selectableListService.isObjectSelected(this.listID, this.object)
      .pipe(take(1))
      .subscribe((selected) => {
        if (!selected) {
          this.selectableListService.selectSingle(this.listID, this.object);
        }
      });
  }

  selectCustom(value) {
    if (!this.allSuggestions.includes(value)) {
      this.openModal(value)
        .then(() => {
          // user clicked ok: store the name variant in the item
            const newName: MetadataValue = new MetadataValue();
            newName.value = value;

            const existingNames: MetadataValue[] = this.dso.metadata[this.alternativeField] || [];
            const alternativeNames = { [this.alternativeField]: [...existingNames, newName] };
            const updatedItem =
              Object.assign({}, this.dso, {
                metadata: {
                  ...this.dso.metadata,
                  ...alternativeNames
                },
              });
            this.itemDataService.update(updatedItem).pipe(take(1)).subscribe();
            this.itemDataService.commitUpdates();
      }).catch(() => {
        // user clicked cancel: use the name variant only for this relation, no further action required
      }).finally(() => {
        this.select(value);
      });
    }
  }

  openModal(value): Promise<any> {
    const modalRef = this.modalService.open(NameVariantModalComponent, { centered: true });

    const modalComp = modalRef.componentInstance;
    modalComp.value = value;
    return modalRef.result;
  }
}
