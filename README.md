# Gradle Metrics Plugin

![Support Status](https://img.shields.io/badge/nebula-supported-brightgreen.svg)
[![Build Status](https://travis-ci.org/nebula-plugins/gradle-metrics-plugin.svg?branch=master)](https://travis-ci.org/nebula-plugins/gradle-metrics-plugin)
[![Coverage Status](https://coveralls.io/repos/nebula-plugins/gradle-metrics-plugin/badge.svg?branch=master&service=github)](https://coveralls.io/github/nebula-plugins/gradle-metrics-plugin?branch=master)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/gradle-metrics-plugin?utm_source=badgeutm_medium=badgeutm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/gradle-metrics-plugin.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Collects Gradle build metrics and log events and and persists them to an external datastore. 

# Usage

To include, add the following to your build.gradle:

If newer than Gradle 2.1 you may use

    plugins {
        id 'nebula.metrics' version '4.2.1'
    }

*or*

    buildscript {
        repositories { jcenter() }

        dependencies {
            classpath 'com.netflix.nebula:gradle-metrics-plugin:4.2.1'
        }
    }

    apply plugin: 'nebula.metrics'

# Example Build Data

```json
{
  "project": {
    "name": "test-metrics",
    "version": "1.0"
  },
  "events": [
    {
      "description": "startup",
      "type": "init",
      "elapsedTime": 954
    },
    {
      "description": "settings",
      "type": "configure",
      "elapsedTime": 151
    },
    {
      "description": "projectsLoading",
      "type": "configure",
      "elapsedTime": 196
    },
    {
      "description": ":",
      "type": "configure",
      "elapsedTime": 1163
    },
    {
      "description": ":classpath",
      "type": "resolve",
      "elapsedTime": 359
    },
    {
      "description": "task",
      "type": "execution",
      "elapsedTime": 59
    }
  ],
  "tasks": [
    {
      "description": ":projects",
      "result": {
        "status": "success"
      },
      "startTime": "2015-11-04T18:32:04.917Z",
      "elapsedTime": 59
    }
  ],
  "tests": [],
  "info": {
    "build": {
      "gradle": {
        "version": "2.8",
        "parameters": {
          "taskRequests": [
            {
              "args": [
                "projects"
              ]
            }
          ],
          "excludedTaskNames": [],
          "buildProjectDependencies": true,
          "currentDir": "/Users/user/dev/tests/test-metrics-plugin",
          "searchUpwards": true,
          "projectProperties": [],
          "gradleUserHomeDir": "/Users/user/.gradle",
          "useEmptySettings": false,
          "initScripts": [],
          "dryRun": false,
          "rerunTasks": false,
          "profile": false,
          "continueOnFailure": false,
          "offline": false,
          "refreshDependencies": false,
          "recompileScripts": false,
          "parallelThreadCount": 0,
          "configureOnDemand": false
        }
      },
      "type": "gradle"
    },
    "scm": {
      "type": "unknown"
    },
    "ci": {
      "type": "unknown"
    },
    "environmentVariables": [ ], 
    "systemProperties": [ ], 
    "javaVersion": "unknown"
  },
  "result": {
    "status": "success"
  },
  "startTime": "2015-11-04T18:32:02.086Z",
  "elapsedTime": 3852,
  "testCount": 0,
  "eventsCount": 6,
  "eventsElapsedTime": 2882,
  "tasksElapsedTime": 59,
  "testElapsedTime": 0,
  "finishedTime": "2015-11-04T18:32:05.938Z",
  "taskCount": 1
}
```
 
# Example Log Data
 
```
TBD
```
 
# Data Population

`gradle-build-metrics` can be currently configured to persist data against either Elasticsearch, a generic REST endpoint or to a Splunk Indexer/Forwarder. If configured 
to use Elasticsearch, data is persisted to the `build-metrics-default` and `logstash-build-metrics-default-yyyyMM` indices for 
`build` and `log` events respectively. For the REST configuration, both types of data are POSTed to the same endpoint, but the payload 
varies based on type:
 
build data: 

```json
{
  "eventName" : "build_metrics",
  "payload" : {
    "buildId": "generated build id",
    "build": "escaped build json"
  }
}
```

logs:

```json
[
  {
    "eventName": "build_logs",
    "payload": {
      "buildId": "generated build id",
      "log": "log data"
    }
  },
  {
    "eventName": "build_logs",
    "payload": {
      "buildId": "generated build id",
      "log": "log data 2"
    }
  }
]
```

If configured to use Splunk, there will be two input types to index the build event:

* *Splunk Forwarder*
* *Splunk indexer HTTP Event Collector*

If **FORWARDER** is chosen, the data build event will be persisted in the forwarder configured *index* and *sourcetype*. If **HTTP_COLLECTOR** is chosen the data will be indexed according to the event collector configuration in the Splunk Indexer.

# Custom Metrics 

In some scenarios you might want to include additional metrics in your build reports. This is supported though 
integration with the [nebula.info-broker](https://github.com/nebula-plugins/gradle-info-plugin#info-broker-plugin-broker) 
plugin, which acts as a bridging point between plugins that wish to generate custom metrics and `gradle-build-metrics`. 

To use, write a plugin that depends on [nebula.info-broker](https://github.com/nebula-plugins/gradle-info-plugin#info-broker-plugin-broker)
and use code similar to this:

```groovy
project.plugins.withType(InfoBrokerPlugin) { 
    it.addReport('myCustomReport', 'some value or object')
}
``` 

Every report will appear as a top-level element in the data model. 

# Configuration

Configuration should be done via the `metrics` Gradle extension. 
 
### Elasticsearch configuration  
 
    metrics {
        dispatcherType = 'ES_HTTP'              // default is the Elasticsearch HTTP client. ES_CLIENT is also available.
        hostname = 'myescluster'                // default is 'localhost'
        httpPort = 59300                        // default is 9200
        indexName = 'myindexname'               // default is 'default'. Builds metrics and log data index names
                                                // are derived from this setting (e.g. `build-metrics-default` and 
                                                // `logstash-build-metrics-default-yyyyMM`)
        rollingIndex = true                     // default is false. If true, appends `-yyyyMM` to build-metrics index.                                         
                  
        esBasicAuthUsername = 'user'            // only for ES_HTTP.  If not set, no authentication will be used.
        esBasicAuthPassword = 'pass'            // this value should be read in from a properties file
        
        metricsIndexMappingFile = 'loc/of/file' // location of the mapping file for creation of rolling metrics indices (optional)
    }

### REST configuration 
 
    metrics {
        dispatcherType = 'REST'
        restUri = 'https://server.com/rest/endpoint'      // default is 'http://localhost/metrics'
        restBuildEventName = 'my_build_events'            // default is 'build_metrics'
        restLogEventName = 'my_log_events'                // default is 'build_logs'  

        headers['myHeader_key'] = 'header_value'          // optional, default is empty MAP object
    } 

### Splunk configuration 

**IMPORTANT**: to use `gradle-metrics-plugin` with Splunk, it is required to add the self-signed Splunk ssl certificate to the JDK keyStore.

#### Splunk HTTP_COLLETOR

    metrics {
        dispatcherType = 'SPLUNK'

        splunkInputType = 'HTTP_COLLECTOR'                                        // input type to persist the build event
        splunkUri = 'https://mysplunk.com/services/collector'                     // splunk indexer URI for Http Collector
        headers['Authorization'] = 'Splunk 8BA5A780-6B3A-472D-BF2F-CF4E9FFF4E9D'  // Splunk Auth token is mandatory Authorization header for HTTP Collector  
    }

#### Splunk FORWARDER

    metrics {
        dispatcherType = 'SPLUNK'

        splunkInputType = 'FORWARDER'                                                                    // input type to persist the build event
        splunkUri = 'https://mysplunk.com/services/receivers/simple?index=main&sourcetype=gradle_builds' // splunk forwarder URI, here the sourcetype and the index could be defined
        headers['Authorization'] = 'Basic YWRtaW46Y2hhbmdlbWU='                                          // basic auth with user and password base64 is mandatory to request via Splunk Forwarder 
    }

# Metrics

Metrics include:

* Info - Gradle start parameters, system properties and environment variables. SCM and GIT information if the gradle-info-plugin has been applied
* Project - name and version
* Events - configuration, dependency resolution, task execution
* Task executions - result, elapsed time per task
* Tests - result, elapsed time per test
* Result - success, failure with throwable, elapsed time
* Custom metrics that have been provided via `nebula.info-broker`

# Example Elasticsearch Queries

## Builds by result status

    GET /build-metrics/build/_search?search_type=count
    {
        "aggs" : {
            "build_results" : {
                "terms" : { "field" : "result.status" }
            }
        }
    }

## Builds with failures

    GET /build-metrics/build/_search?search_type=count
    {
        "filter": {
            "term": { "result.status": "failure" }
        }
    }

## Builds that did not complete

    GET /build-metrics/build/_search?search_type=count
    {
        "filter": {
            "term": { "result.status": "unknown" }
        }
    }

## Builds with failed task executions

    GET /build-metrics/build/_search?search_type=count
    {
        "filter": {
            "nested": {
                "path": "tasks",
                "filter": {
                    "term": { "tasks.result.status": "failure" }
                }
            }
        }
    }

## Builds with test failures

    GET /build-metrics/build/_search?search_type=count
    {
        "filter": {
            "nested": {
                  "path": "tests",
                  "filter": {
                        "term": { "tests.result.status": "failure" }
                  }
            }
        }
    }

## Average elapsed time by event type

    GET build-metrics/build/_search?search_type=count
    {
        "aggs" : {
            "elapsedByType" : {
                "nested" : {
                    "path" : "events"
                },
                "aggs" : {
                    "eventsByType" : {
                        "terms": { "field": "events.type" },
                        "aggs": {
                            "elapsedTime": {
                                "avg" : { "field" : "events.elapsedTime" }
                            }
                        }
                    }
                }
            }
        }
    }

## Average elapsed time for a given event type

    GET build-metrics/build/_search?search_type=count
    {
        "aggs" : {
            "elapsedByType" : {
                "nested" : {
                    "path" : "events"
                },
                "aggs" : {
                    "eventsByType" : {
                        "filter": {
                            "term": { "events.type": "<TYPE>" }
                        },
                        "aggs": {
                            "elapsedTime": {
                                "sum" : { "field" : "events.elapsedTime" }
                            }
                        }
                    }
                }
            }
        }
    }

Where `<TYPE>` is one of `init`, `configure`, `resolve`, `execution` or the exceptional case, `unknown`.

LICENSE
=======

Copyright 2015-2016 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
