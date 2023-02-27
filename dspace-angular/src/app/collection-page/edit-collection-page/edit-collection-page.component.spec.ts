import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { EditCollectionPageComponent } from './edit-collection-page.component';
import { SharedModule } from '../../shared/shared.module';
import { CollectionDataService } from '../../core/data/collection-data.service';
import { of as observableOf } from 'rxjs';

describe('EditCollectionPageComponent', () => {
  let comp: EditCollectionPageComponent;
  let fixture: ComponentFixture<EditCollectionPageComponent>;

  const routeStub = {
    data: observableOf({
      dso: { payload: {} }
    }),
    routeConfig: {
      children: [
        {
          path: 'mockUrl',
          data: {
            hideReturnButton: false
          }
        }
      ]
    },
    snapshot: {
      firstChild: {
        routeConfig: {
          path: 'mockUrl'
        }
      }
    }
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), SharedModule, CommonModule, RouterTestingModule],
      declarations: [EditCollectionPageComponent],
      providers: [
        { provide: CollectionDataService, useValue: {} },
        { provide: ActivatedRoute, useValue: routeStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EditCollectionPageComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('type', () => {
    it('should have the right type set', () => {
      expect((comp as any).type).toEqual('collection');
    });
  });
});
