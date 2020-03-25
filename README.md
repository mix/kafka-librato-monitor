# kafka-librato-monitor
Plugin for [KafkaOffsetMonitor](https://github.com/quantifind/KafkaOffsetMonitor/) tool reporting offset data to [Librato](https://www.librato.com/) via [dropwizard metrics](https://github.com/dropwizard/metrics) Librato [reporter](https://github.com/librato/metrics-librato)

## Getting Started
These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites
Install sbt, scala if not already installed

### Installing
Currently kafka-offset-monitor is not available via public artifact repository, so before we build the plugin we need to build KafkaOffsetMonitor and publish it to maven local repo (for external only; for mix we have published the artifact in Mix Internal Repo):

```
sbt publishM2
```

Now we can build the plugin:

```
sbt clean assembly
```

### Running It
Check how to run kafka-offset-monitor and modify the command by adding a plugin assembly jar file to the classpath, and put Librato configuration properties into a pluginsArgs argument.

See original kafka-offset-monitor example command modified with Librato reporter plugin usage:

```
java -cp "kafka-offset-monitor-assembly-0.4.6-SNAPSHOT.jar:kafka-librato-monitor-assembly-0.4.6.jar" \
     com.quantifind.kafka.offsetapp.OffsetGetterWeb \
     --kafkaBrokers broker1:9092,broker2:9092
     --zk zk-server1,zk-server2,zk-server-3 \
     --port 8080 \
     --refresh 10.seconds \
     --retain 2.days \
     --pluginsArgs libratoEmail=user@company.com,libratoToken=123456789ABCDEFGHIJKL,libratoPrefix=kafka-monitor
     --kafkaSecurityProtocol SASL_PLAINTEXT 

```

The pluginArgs used by kafka-librato-monitor are:

- **libratoEmail** Librato Email (required)
- **libratoToken** Librato API Token (required)
- **libratoPrefix** Metrics prefix (default kafka-monitor)
- **libratoSource** Source Identifier (default hostname - if runs in docker it is usually container's id in bridge mode)
- **libratoReportingInterval** Reporting interval in ISO-8601 format (default PT30S)
- **metricsCacheExpiration** Metrics cache TTL in ISO-8601 format (default PT10M). Offset metrics are stored in expiring cache and reported to Librato periodically. If metrics are not updated they will be removed.

## Built With

* [KafkaOffsetMonitor](https://github.com/quantifind/KafkaOffsetMonitor/)
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

