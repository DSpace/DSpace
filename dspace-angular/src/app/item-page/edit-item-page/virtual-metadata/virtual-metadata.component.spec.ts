import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of as observableOf } from 'rxjs';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { VirtualMetadataComponent } from './virtual-metadata.component';
import { Item } from '../../../core/shared/item.model';
import { ObjectUpdatesService } from '../../../core/data/object-updates/object-updates.service';
import { VarDirective } from '../../../shared/utils/var.directive';
import { APP_CONFIG } from '../../../../config/app-config.interface';
import { environment } from '../../../../environments/environment';

describe('VirtualMetadataComponent', () => {

  let comp: VirtualMetadataComponent;
  let fixture: ComponentFixture<VirtualMetadataComponent>;
  let de: DebugElement;

  let objectUpdatesService;

  const url = 'http://test-url.com/test-url';

  let item;
  let relatedItem;
  let relationshipId;

  beforeEach(() => {

    relationshipId = 'relationship id';

    item = Object.assign(new Item(), {
      uuid: 'publication',
      metadata: [],
    });

    relatedItem = Object.assign(new Item(), {
      uuid: 'relatedItem',
      metadata: [],
    });

    objectUpdatesService = jasmine.createSpyObj('objectUpdatesService', {
      isSelectedVirtualMetadata: observableOf(false),
      setSelectedVirtualMetadata: null,
    });

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [VirtualMetadataComponent, VarDirective],
      providers: [
        { provide: ObjectUpdatesService, useValue: objectUpdatesService },
        { provide: APP_CONFIG, useValue: environment }
      ], schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(VirtualMetadataComponent);
    comp = fixture.componentInstance;
    de = fixture.debugElement;

    comp.url = url;
    comp.leftItem = item;
    comp.rightItem = relatedItem;
    comp.relationshipId = relationshipId;

    fixture.detectChanges();
  });

  describe('when clicking the save button', () => {
    it('should emit a save event', () => {

      spyOn(comp.save, 'emit');
      fixture.debugElement
        .query(By.css('button.save'))
        .triggerEventHandler('click', null);
      expect(comp.save.emit).toHaveBeenCalled();
    });
  });

  describe('when clicking the close button', () => {
    it('should emit a close event', () => {

      spyOn(comp.close, 'emit');
      fixture.debugElement
        .query(By.css('button.close'))
        .triggerEventHandler('click', null);
      expect(comp.close.emit).toHaveBeenCalled();
    });
  });

  describe('when selecting an item', () => {
    it('should call the updates service setSelectedVirtualMetadata method', () => {

      fixture.debugElement
        .query(By.css('div.item'))
        .triggerEventHandler('click', null);
      expect(objectUpdatesService.setSelectedVirtualMetadata).toHaveBeenCalledWith(
        url,
        relationshipId,
        item.uuid,
        true
      );
    });
  });
});
