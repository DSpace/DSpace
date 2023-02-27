import { FormFieldMetadataValueObject } from '../../../shared/form/builder/models/form-field-metadata-value.model';
import { MetadataMapInterface } from '../../shared/metadata.models';

/**
 * An interface to represent submission's form section data.
 * A map of metadata keys to an ordered list of FormFieldMetadataValueObject objects.
 */
export interface WorkspaceitemSectionFormObject extends MetadataMapInterface {
  [metadata: string]: FormFieldMetadataValueObject[];
}
