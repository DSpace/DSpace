import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';

import { cold } from 'jasmine-marbles';
import { of as observableOf } from 'rxjs';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { BitstreamAuthorizationsComponent } from './bitstream-authorizations.component';
import { Bitstream } from '../../core/shared/bitstream.model';
import { createSuccessfulRemoteDataObject } from '../../shared/remote-data.utils';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';

describe('BitstreamAuthorizationsComponent', () => {
  let comp: BitstreamAuthorizationsComponent<DSpaceObject>;
  let fixture: ComponentFixture<BitstreamAuthorizationsComponent<any>>;

  const bitstream = Object.assign(new Bitstream(), {
    sizeBytes: 10000,
    metadata: {
      'dc.title': [
        {
          value: 'file name',
          language: null
        }
      ]
    },
    _links: {
      content: { href: 'file-selflink' }
    }
  });

  const bitstreamRD = createSuccessfulRemoteDataObject(bitstream);

  const routeStub = {
    data: observableOf({
      bitstream: bitstreamRD
    })
  };

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        CommonModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })
      ],
      declarations: [BitstreamAuthorizationsComponent],
      providers: [
        { provide: ActivatedRoute, useValue: routeStub },
        ChangeDetectorRef,
        BitstreamAuthorizationsComponent,
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BitstreamAuthorizationsComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    comp = null;
    fixture.destroy();
  });

  it('should create', () => {
    expect(comp).toBeTruthy();
  });

  it('should init dso remote data properly', (done) => {
    const expected = cold('(a|)', { a: bitstreamRD });
    expect(comp.dsoRD$).toBeObservable(expected);
    done();
  });
});
