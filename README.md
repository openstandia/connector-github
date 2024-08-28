# GitHub Connector

## Description

[MidPoint](https://github.com/Evolveum/midpoint) Connector for [GitHub](https://github.com).
There are two connectors in this connector bundle.

1. GitHub Connector: For GitHub Enterprise with personal accounts
2. GitHub EMU Connector: For GitHub Enterprise with managed users

## GitHub Connector
### Capabilities and Features

* Schema: YES
* Provisioning: YES
* Live Synchronization: No
* Password: No
* Activation: No
* Script execution: No 

## GitHub EMU Connector
### Capabilities and Features

* Schema: YES
* Provisioning: YES
* Live Synchronization: No
* Password: No
* Activation: YES
* Script execution: No

## Build

Install JDK 11+ and [maven3](https://maven.apache.org/download.cgi) then build:

```
mvn install
```

After successful the build, you can find `connector-github-*.jar` in `target` directory.

## License

Licensed under the [Apache License 2.0](/LICENSE).
