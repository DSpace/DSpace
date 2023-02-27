import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';

import { Item } from './item.model';

describe('Item', () => {

  let item: Item;
  const thumbnailBundleName = 'THUMBNAIL';
  const originalBundleName = 'ORIGINAL';
  const thumbnailPath = 'thumbnail.jpg';
  const bitstream1Path = 'document.pdf';
  const bitstream2Path = 'otherfile.doc';

  const nonExistingBundleName = 'c1e568f7-d14e-496b-bdd7-07026998cc00';
  let bitstreams;
  let remoteDataThumbnail;
  let remoteDataThumbnailList;
  let remoteDataFiles;
  let remoteDataBundles;

  beforeEach(() => {
    const thumbnail = {
      content: thumbnailPath
    };

    bitstreams = [{
      content: bitstream1Path
    }, {
      content: bitstream2Path
    }];

    remoteDataThumbnail = createSuccessfulRemoteDataObject$(thumbnail);
    remoteDataThumbnailList = createSuccessfulRemoteDataObject$(createPaginatedList([thumbnail]));
    remoteDataFiles = createSuccessfulRemoteDataObject$(createPaginatedList(bitstreams));

    // Create Bundles
    const bundles =
      [
        {
          name: thumbnailBundleName,
          primaryBitstream: remoteDataThumbnail,
          bitstreams: remoteDataThumbnailList
        },

        {
          name: originalBundleName,
          bitstreams: remoteDataFiles
        }];

    remoteDataBundles = createSuccessfulRemoteDataObject$(createPaginatedList(bundles));

    item = Object.assign(new Item(), { bundles: remoteDataBundles });
  });
});
