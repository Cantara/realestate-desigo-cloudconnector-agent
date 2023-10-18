# realestate-desigo-cloudconnector-app
Read sensor observations from Siemens Desigo. Distribute these to the cloud e.g. by MQTT or Azure Digital Twin

# Getting Started

## List of Sensors organized by rooms
See [DesigoTfmRec.csv_template](./DesigoTfmRec.csv_template) for a list of sensors with placement in the buildings, and rooms.
This template may be used to configure all sensors that should be imported from Desigo. 

## Import and configuration
1. Copy your DesigoTfmRec.csv to import-data/DesigoTfmRec.csv
2. Rename local_override.properties_template to local_override.properties.

### Import from Azure Storage Account Table
In local_override.properties, set the following properties:
```
sensormappings.azure.enabled=true
sensormappings.azure.connectionString=<Insert from Azure Storage Account>
sensormappings.azure.tableName=Desigo
```


### Required properties
In local_override.properties, set the following properties:
```
sd.api.prod=true
sd.api.username=....
sd.api.password=....
sd.api.url=https://<desigoServer>:<port>/<contextroot>/api/
importsensorsQuery.realestates=RealEstate1,RealEstate2...
```
**importsensorsQuery.realestates** is a comma separated list of the RealEstate names identified in the DesigoTfmRec.csv file. Only RealEstates in this list will be imported.
The intention is to support filtering of which sensors, from defined buildings the agent should import.


### Distribution of observations
```
distribution.azure.connectionString=HostName=<yourIoTHub>.azure-devices.net;SharedAccessKeyName=iothubowner;SharedAccessKey=<yourSharedAccessKey>
```
Azure ConnectionString is copied from Azure IoT Hub, Devices, <yourDevice>, Connection string-primary key. 
See [Azure IoT Hub configuration](https://github.com/Cantara/realestate-azure-client-lib) for details.

## Start the application
```
java -jar path_to/realestate-desigo-cloudconnector-agent-<version>.jar
```

### Monitoring
http://localhost:8081/metayscloudconnector/health/pretty
```json
{
  "Status": "UP",
  "version": "unknown",
  "ip": "127.0.1.1",
  "running since": "2023-09-04T13:08:23.493546367Z",
  "DesigoStreamClient.isLoggedIn": "UP",
  "DesigoStreamImporter.isHealthy": "UP",
  "webserver.running": "UP",
  "AzureObservationDistributionClient-isConnected: ": "true",
  "AzureObservationDistributionClient-numberofMessagesObserved: ": "1",
  "mappedIdRepository.size": "68",
  "DesigoStreamClient-isHealthy: ": "true",
  "DesigoStreamClient-isLoggedIn: ": "true",
  "DesigoStreamClient-isStreamOpen: ": "true",
  "DesigoStreamImporter-isHealthy: ": "true",
  "now": "2023-09-04T13:08:37.632070133Z"
}
```

### Alerting
There is support for Slack alerting.
When the connection to Desigo fails, or the distribution to Azure IoT Hub is lost, an alert is sent to Slack.
When the connection to Desigo is restored, or the distribution to Azure IoT Hub is restored, an ack is sent to Slack.
```
slack_alerting_enabled=true
slack_token=...
slack_alarm_channel=...
slack_warning_channel=...
```

# Development

```
mvn clean install
java -jar target/desigo-cloudconnector-app-<version>.jar
```

## Testing with Desigo Mock
### Start Mock server
````
mvn -Dmockserver.serverPort=1080 -Dmockserver.logLevel=INFO org.mock-server:mockserver-maven-plugin:5.15.0:runForked
`````
### Stop Mock server
````
mvn -Dmockserver.serverPort=1080 org.mock-server:mockserver-maven-plugin:5.15.0:stopForked
````

### Add Login mock

Run  [MockServerSetup.java](src/test/java/no/cantara/realestate/desigo/cloudconnector/MockServerSetup.java)


### Updates
* 0.7.0 - Support for Streaming of sensors from Desigo, and distributing these observations.

