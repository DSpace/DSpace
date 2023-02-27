export class FormFieldLanguageValueObject {
  value: string;
  language: string;

  constructor(value: string, language: string) {
    this.value = value;
    this.language = language;
  }
}

export interface LanguageCode {
  display: string;
  code: string;
}
