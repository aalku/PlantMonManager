# PlantMonManager
Manager for PlantMonitor

Written in Java. Does not work with Java 9 or higher on Windows.

Installation:

- Install Java 8+ ( > 8 fails on windows > 7 )

- Install Maven

- Install Git

- Install influxdb:
		https://docs.influxdata.com/influxdb/v1.5/introduction/installation/

- Prepare influxdb

```
  $ sudo service influxd start
  $ influx
    > create database influxdatabase
    > exit
```

- Install grafana (optional):

Debian x86/x64:
  
```
$ echo "deb https://packagecloud.io/grafana/stable/debian/ stretch main" | sudo tee /etc/apt/sources.list.d/grafana.list
	  $ curl https://packagecloud.io/gpg.key | sudo apt-key add -
	  $ sudo apt-get update; sudo apt-get install grafana
	  $ sudo service grafana-server start
```

  Raspberry Pi equivalent:
  
		https://github.com/fg2it/grafana-on-raspberry/wiki
		
- Install PlantMonitor:
```
  $ git clone https://github.com/aalku/PlantMonManager.git
  $ cd PlantMonManager
  $ mvn package
```

- Run PlantMonitor:
```
  $ sudo java -jar target/*.jar
```

- Configure grafana:

	Go to http://192.168.1.230:3000/
  
	Create influxdb datasource:
	```
    url=http://localhost:8086
		username=user
		password=~
		database=influxdatabase
  ```
	Import JSON dashboard from file content: grafana_dashboard.json
