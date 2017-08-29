#### Description
Simple multiple client socket example with file copy support.
Project consists of two main programs (client, server).

Before you start client or server compile all sources:

```
javac -d {classpath destination} src/kg/talaszh/cmdexplorer/**/*.java 
```


##### Server

Server application accepts 3 arguments: 
* first - rootFolder(required), folder to serve files to client 
* second - serverPort, port to listen for new connections, by default 9001
* third - logginLevel, set server logging level, available logging options :[ALL, FINEST, FINER, FINE, INFO, WARNING, SEVERE, OFF]

By default client logs are written to `cmd-explorer-server.log` file in directory where client was started.

Apparently server tracks file download statistics streamed to `stats.txt`. 

###### Start server

Once you have successfully compiled you can run server: 

```
Program usage: 
java -cp {classpath directory} kg.talaszh.cmdexplorer.server.MainServer {rootFolder:required} {serverPort} {logginLevel}
```

For example:
```
javac -d bin src/kg/talaszh/cmdexplorer/**/*.java && java -cp ./bin kg.talaszh.cmdexplorer.server.MainServer ~/
```

To stop server type in console: `exit`

##### Client

Before you start client you can specify server port and logging level as application arguments.
 * first - serverPort, port to listen for new connections, by default 9001
 * second - logginLevel, set client logging level, available logging options :[ALL, FINEST, FINER, FINE, INFO, WARNING, SEVERE, OFF]

By default client logs are written to `cmd-explorer-client.log` file in directory where client was started.

```
Program usage:
java -cp {classpath directory} kg.talaszh.cmdexplorer.client.SocketClient {serverPort} {loggingLevel}
```

For example:
```
javac -d bin src/kg/talaszh/cmdexplorer/**/*.java && java -cp ./bin kg.talaszh.cmdexplorer.client.SocketClient
```