# kafka-librato-monitor
Plugin to [KafkaOffsetMonitor](https://github.com/quantifind/KafkaOffsetMonitor) tool reporting offset data to [Librato](https://www.librato.com/) via [dropwizard metrics](https://github.com/dropwizard/metrics) Librato [reporter](https://github.com/librato/metrics-librato) 

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites
Install sbt, scala if not already installed

### Installing
Currently KafkaOffsetMonitor is not available via public artifact repository, so before we build the plugin we need to build KafkaOffsetMonitor and publish it to maven local repo (for external only; for mix we have published the artifact in Mix Internal Repo):

```
sbt publishM2
```

Now we can build the plugin:

```
sbt clean assembly
```

### Running It
Check how to run KafkaOffsetMonitor and modify the command by adding a plugin assembly jar file to the classpath, and put Librato configuration properties into a pluginsArgs argument.

See original KafkaOffsetMonitor example command modified with Librato reporter plugin usage:

```
java -cp "KafkaOffsetMonitor-assembly-0.3.0-SNAPSHOT.jar:kafka-librato-monitor-assembly-0.1.0.jar" \
     com.quantifind.kafka.offsetapp.OffsetGetterWeb \
     --zk zk-server1,zk-server2 \
     --port 8080 \
     --refresh 10.seconds \
     --retain 2.days \
     --pluginsArgs libratoUsername=user@company.com,libratoToken=123456789ABCDEFGHIJKL,libratoPrefix=kafka-lag
```

The pluginArgs used by kafka-librato-monitor are:

- **libratoUsername** Librato Email (required)
- **libratoToken** Librato API Token (required)
- **libratoPrefix** Metrics prefix (default kafka-lag)
- **libratoSource** Source Identifier (default hostname - if runs in docker it is usually container's id in bridge mode)
- **libratoReportPeriod** Reporting period in seconds (default 30)
- **metricsCacheExpireSeconds** Metrics cache TTL in mires (default 600). Offset metrics are stored in expiring cache and reported to Librato periodically. If metrics are not updated they will be removed.

## Built With

* [KafkaOffsetMonitor](https://github.com/quantifind/KafkaOffsetMonitor)
* [Dropwizard Metrics](http://metrics.dropwizard.io/) - Toolkit of ways to measure the behavior of an application
* [metrics-librato](https://github.com/librato/metrics-librato) - Librato reporter for Dropwizard Metrics

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/mix/kafka-librato-monitor/tags). 

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments
* Special thanks to authors & maintainers of [KafkaOffsetMonitor](https://github.com/quantifind/KafkaOffsetMonitor) for creating this awesome tool with support for plugins.
* This project is replica of [kafka-offset-monitor-graphite](https://github.com/allegro/kafka-offset-monitor-graphite) for Librato. Thank you for providing this graphite plugin. Future work - maybe we could convert this project into generic dropwizard monitor.
* This Readme is inspired from [Readme Template](https://gist.github.com/PurpleBooth/109311bb0361f32d87a2)
