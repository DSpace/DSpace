import { DsoEditMetadataChangeType, DsoEditMetadataForm } from './dso-edit-metadata-form';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { MetadataValue } from '../../core/shared/metadata.models';

describe('DsoEditMetadataForm', () => {
  let form: DsoEditMetadataForm;
  let dso: DSpaceObject;

  beforeEach(() => {
    dso = Object.assign(new DSpaceObject(), {
      metadata: {
        'dc.title': [
          Object.assign(new MetadataValue(), {
            value: 'Test Title',
            language: 'en',
            place: 0,
          }),
        ],
        'dc.subject': [
          Object.assign(new MetadataValue(), {
            value: 'Subject One',
            language: 'en',
            place: 0,
          }),
          Object.assign(new MetadataValue(), {
            value: 'Subject Two',
            language: 'en',
            place: 1,
          }),
          Object.assign(new MetadataValue(), {
            value: 'Subject Three',
            language: 'en',
            place: 2,
          }),
        ],
      },
    });
    form = new DsoEditMetadataForm(dso.metadata);
  });


  describe('adding a new value', () => {
    beforeEach(() => {
      form.add();
    });

    it('should add an empty value to \"newValue\" with no place yet and editing set to true', () => {
      expect(form.newValue).toBeDefined();
      expect(form.newValue.originalValue.place).toBeUndefined();
      expect(form.newValue.newValue.place).toBeUndefined();
      expect(form.newValue.editing).toBeTrue();
    });

    it('should not mark the form as changed yet', () => {
      expect(form.hasChanges()).toEqual(false);
    });

    describe('and assigning a value and metadata field to it', () => {
      let mdField: string;
      let value: string;
      let expectedPlace: number;

      beforeEach(() => {
        mdField = 'dc.subject';
        value = 'Subject Four';
        form.newValue.newValue.value = value;
        form.setMetadataField(mdField);
        expectedPlace = form.fields[mdField].length - 1;
      });

      it('should add the new value to the values of the relevant field', () => {
        expect(form.fields[mdField][expectedPlace].newValue.value).toEqual(value);
      });

      it('should set its editing flag to false', () => {
        expect(form.fields[mdField][expectedPlace].editing).toBeFalse();
      });

      it('should set both its original and new place to match its position in the value array', () => {
        expect(form.fields[mdField][expectedPlace].newValue.place).toEqual(expectedPlace);
        expect(form.fields[mdField][expectedPlace].originalValue.place).toEqual(expectedPlace);
      });

      it('should clear \"newValue\"', () => {
        expect(form.newValue).toBeUndefined();
      });

      it('should mark the form as changed', () => {
        expect(form.hasChanges()).toEqual(true);
      });

      describe('discard', () => {
        beforeEach(() => {
          form.discard();
        });

        it('should remove the new value', () => {
          expect(form.fields[mdField][expectedPlace]).toBeUndefined();
        });

        it('should mark the form as unchanged again', () => {
          expect(form.hasChanges()).toEqual(false);
        });

        describe('reinstate', () => {
          beforeEach(() => {
            form.reinstate();
          });

          it('should re-add the new value', () => {
            expect(form.fields[mdField][expectedPlace].newValue.value).toEqual(value);
          });

          it('should mark the form as changed once again', () => {
            expect(form.hasChanges()).toEqual(true);
          });
        });
      });
    });
  });

  describe('removing a value entirely (not just marking deleted)', () => {
    it('should remove the value on the correct index', () => {
      form.remove('dc.subject', 1);
      expect(form.fields['dc.subject'].length).toEqual(2);
      expect(form.fields['dc.subject'][0].newValue.value).toEqual('Subject One');
      expect(form.fields['dc.subject'][1].newValue.value).toEqual('Subject Three');
    });
  });

  describe('moving a value', () => {
    beforeEach(() => {
      form.fields['dc.subject'][0].newValue.place = form.fields['dc.subject'][1].originalValue.place;
      form.fields['dc.subject'][1].newValue.place = form.fields['dc.subject'][0].originalValue.place;
      form.fields['dc.subject'][0].confirmChanges();
      form.fields['dc.subject'][1].confirmChanges();
    });

    it('should mark the value as changed', () => {
      expect(form.fields['dc.subject'][0].hasChanges()).toEqual(true);
      expect(form.fields['dc.subject'][1].hasChanges()).toEqual(true);
    });

    it('should mark the form as changed', () => {
      expect(form.hasChanges()).toEqual(true);
    });

    describe('discard', () => {
      beforeEach(() => {
        form.discard();
      });

      it('should reset the moved values their places to their original values', () => {
        expect(form.fields['dc.subject'][0].newValue.place).toEqual(form.fields['dc.subject'][0].originalValue.place);
        expect(form.fields['dc.subject'][1].newValue.place).toEqual(form.fields['dc.subject'][1].originalValue.place);
      });

      it('should mark the form as unchanged again', () => {
        expect(form.hasChanges()).toEqual(false);
      });

      describe('reinstate', () => {
        beforeEach(() => {
          form.reinstate();
        });

        it('should move the values to their new places again', () => {
          expect(form.fields['dc.subject'][0].newValue.place).toEqual(form.fields['dc.subject'][1].originalValue.place);
          expect(form.fields['dc.subject'][1].newValue.place).toEqual(form.fields['dc.subject'][0].originalValue.place);
        });

        it('should mark the form as changed once again', () => {
          expect(form.hasChanges()).toEqual(true);
        });
      });
    });
  });

  describe('marking a value deleted', () => {
    beforeEach(() => {
      form.fields['dc.title'][0].change = DsoEditMetadataChangeType.REMOVE;
    });

    it('should mark the value as changed', () => {
      expect(form.fields['dc.title'][0].hasChanges()).toEqual(true);
    });

    it('should mark the form as changed', () => {
      expect(form.hasChanges()).toEqual(true);
    });

    describe('discard', () => {
      beforeEach(() => {
        form.discard();
      });

      it('should remove the deleted mark from the value', () => {
        expect(form.fields['dc.title'][0].change).toBeUndefined();
      });

      it('should mark the form as unchanged again', () => {
        expect(form.hasChanges()).toEqual(false);
      });

      describe('reinstate', () => {
        beforeEach(() => {
          form.reinstate();
        });

        it('should re-mark the value as deleted', () => {
          expect(form.fields['dc.title'][0].change).toEqual(DsoEditMetadataChangeType.REMOVE);
        });

        it('should mark the form as changed once again', () => {
          expect(form.hasChanges()).toEqual(true);
        });
      });
    });
  });

  describe('editing a value', () => {
    const value = 'New title';

    beforeEach(() => {
      form.fields['dc.title'][0].editing = true;
      form.fields['dc.title'][0].newValue.value = value;
    });

    it('should not mark the form as changed yet', () => {
      expect(form.hasChanges()).toEqual(false);
    });

    describe('and confirming the changes', () => {
      beforeEach(() => {
        form.fields['dc.title'][0].confirmChanges(true);
      });

      it('should mark the value as changed', () => {
        expect(form.fields['dc.title'][0].hasChanges()).toEqual(true);
      });

      it('should mark the form as changed', () => {
        expect(form.hasChanges()).toEqual(true);
      });

      describe('discard', () => {
        beforeEach(() => {
          form.discard();
        });

        it('should reset the changed value to its original value', () => {
          expect(form.fields['dc.title'][0].newValue.value).toEqual(form.fields['dc.title'][0].originalValue.value);
        });

        it('should mark the form as unchanged again', () => {
          expect(form.hasChanges()).toEqual(false);
        });

        describe('reinstate', () => {
          beforeEach(() => {
            form.reinstate();
          });

          it('should put the changed value back in place', () => {
            expect(form.fields['dc.title'][0].newValue.value).toEqual(value);
          });

          it('should mark the form as changed once again', () => {
            expect(form.hasChanges()).toEqual(true);
          });
        });
      });
    });
  });
});
