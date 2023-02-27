import {
  HealthComponent,
  HealthInfoComponent,
  HealthInfoResponse,
  HealthResponse,
  HealthStatus
} from '../../health-page/models/health-component.model';

export const HealthResponseObj: HealthResponse = {
  'status': HealthStatus.UP_WITH_ISSUES,
  'components': {
    'db': {
      'status': HealthStatus.UP,
      'components': {
        'dataSource': {
          'status': HealthStatus.UP,
          'details': {
            'database': 'PostgreSQL',
            'result': 1,
            'validationQuery': 'SELECT 1'
          }
        },
        'dspaceDataSource': {
          'status': HealthStatus.UP,
          'details': {
            'database': 'PostgreSQL',
            'result': 1,
            'validationQuery': 'SELECT 1'
          }
        }
      }
    },
    'geoIp': {
      'status': HealthStatus.UP_WITH_ISSUES,
      'details': {
        'reason': 'The GeoLite Database file is missing (/var/lib/GeoIP/GeoLite2-City.mmdb)! Solr Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.'
      }
    },
    'solrOaiCore': {
      'status': HealthStatus.UP,
      'details': {
        'status': 0,
        'detectedPathType': 'particular core'
      }
    },
    'solrSearchCore': {
      'status': HealthStatus.UP,
      'details': {
        'status': 0,
        'detectedPathType': 'particular core'
      }
    },
    'solrStatisticsCore': {
      'status': HealthStatus.UP,
      'details': {
        'status': 0,
        'detectedPathType': 'particular core'
      }
    }
  }
};

export const HealthComponentOne: HealthComponent = {
  'status': HealthStatus.UP,
  'components': {
    'dataSource': {
      'status': HealthStatus.UP,
      'details': {
        'database': 'PostgreSQL',
        'result': 1,
        'validationQuery': 'SELECT 1'
      }
    },
    'dspaceDataSource': {
      'status': HealthStatus.UP,
      'details': {
        'database': 'PostgreSQL',
        'result': 1,
        'validationQuery': 'SELECT 1'
      }
    }
  }
};

export const HealthComponentTwo: HealthComponent = {
  'status': HealthStatus.UP_WITH_ISSUES,
  'details': {
    'reason': 'The GeoLite Database file is missing (/var/lib/GeoIP/GeoLite2-City.mmdb)! Solr Statistics cannot generate location based reports! Please see the DSpace installation instructions for instructions to install this file.'
  }
};

export const HealthInfoResponseObj: HealthInfoResponse = {
  'app': {
    'name': 'DSpace at My University',
    'dir': '/home/giuseppe/development/java/install/dspace7-review',
    'url': 'http://localhost:8080/server',
    'db': 'jdbc:postgresql://localhost:5432/dspace7',
    'solr': {
      'server': 'http://localhost:8983/solr',
      'prefix': ''
    },
    'mail': {
      'server': 'smtp.example.com',
      'from-address': 'dspace-noreply@myu.edu',
      'feedback-recipient': 'dspace-help@myu.edu',
      'mail-admin': 'dspace-help@myu.edu',
      'mail-helpdesk': 'dspace-help@myu.edu',
      'alert-recipient': 'dspace-help@myu.edu'
    },
    'cors': {
      'allowed-origins': 'http://localhost:4000'
    },
    'ui': {
      'url': 'http://localhost:4000'
    }
  },
  'java': {
    'vendor': 'Private Build',
    'version': '11.0.15',
    'runtime': {
      'name': 'OpenJDK Runtime Environment',
      'version': '11.0.15+10-Ubuntu-0ubuntu0.20.04.1'
    },
    'jvm': {
      'name': 'OpenJDK 64-Bit Server VM',
      'vendor': 'Private Build',
      'version': '11.0.15+10-Ubuntu-0ubuntu0.20.04.1'
    }
  },
  'version': '7.3-SNAPSHOT'
};

export const HealthInfoComponentOne: HealthInfoComponent = {
  'name': 'DSpace at My University',
  'dir': '/home/giuseppe/development/java/install/dspace7-review',
  'url': 'http://localhost:8080/server',
  'db': 'jdbc:postgresql://localhost:5432/dspace7',
  'solr': {
    'server': 'http://localhost:8983/solr',
    'prefix': ''
  },
  'mail': {
    'server': 'smtp.example.com',
    'from-address': 'dspace-noreply@myu.edu',
    'feedback-recipient': 'dspace-help@myu.edu',
    'mail-admin': 'dspace-help@myu.edu',
    'mail-helpdesk': 'dspace-help@myu.edu',
    'alert-recipient': 'dspace-help@myu.edu'
  },
  'cors': {
    'allowed-origins': 'http://localhost:4000'
  },
  'ui': {
    'url': 'http://localhost:4000'
  }
};

export const HealthInfoComponentTwo = {
  'version': '7.3-SNAPSHOT'
};
