import { URLCombiner } from '../../core/url-combiner/url-combiner';
import { getRegistriesModuleRoute } from '../admin-routing-paths';

export const BITSTREAMFORMATS_MODULE_PATH = 'bitstream-formats';

export function getBitstreamFormatsModuleRoute() {
  return new URLCombiner(getRegistriesModuleRoute(), BITSTREAMFORMATS_MODULE_PATH).toString();
}
