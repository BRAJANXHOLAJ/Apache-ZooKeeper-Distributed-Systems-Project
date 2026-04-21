# Apache ZooKeeper Distributed Systems Project

## Course

Programim në Sistemet e Shpërndara

## Topic

Implementation of distributed coordination mechanisms using Apache ZooKeeper.

---

## Overview

This project demonstrates the use of ZooKeeper as a coordination service for building distributed system primitives. The implementation includes:

* Cluster Node Monitoring
* Distributed Locking
* Distributed Queue
* Group Messaging (Bonus)
* System Recovery (Bonus)

The system is implemented in Java using the native ZooKeeper API and managed as a Maven project.

---

## Project Structure

```
src/
 └── main/java/org/example/
     ├── App.java
     ├── ClusterMonitor.java
     ├── DistributedLock.java
     ├── DistributedQueue.java
resources/
 └── simplelogger.properties
```

---

## Requirements

* Java 17 or higher (project configured with Java 22)
* Maven
* ZooKeeper cluster with at least 3 nodes

---

## Setup and Execution

### 1. Start ZooKeeper Cluster

Run three ZooKeeper instances:

```
localhost:2181
localhost:2182
localhost:2183
```

---

### 2. Build the Project

```
mvn clean install
```

---

### 3. Run Modules

#### Cluster Monitoring

```
java -cp target/untitled-1.0-SNAPSHOT.jar org.example.ClusterMonitor
```

#### Distributed Lock

```
java -cp target/untitled-1.0-SNAPSHOT.jar org.example.DistributedLock
```

#### Distributed Queue

```
java -cp target/untitled-1.0-SNAPSHOT.jar org.example.DistributedQueue
```

---

## 1. Cluster Node Monitoring

### Design

* Uses a parent znode `/cluster`
* Each node registers itself using an ephemeral znode

Example:

```
/cluster/nyja-1
/cluster/nyja-2
/cluster/nyja-3
```

### Functionality

* Detects node joins and failures
* Uses ZooKeeper watchers (`NodeChildrenChanged`)
* Maintains current cluster state

### Failure Handling

* When a node disconnects, its ephemeral znode is removed automatically
* The monitoring service detects the change and updates the cluster status

---

## 2. Distributed Locking

### Design

* Lock path: `/lock`
* Each client creates an ephemeral sequential znode

Example:

```
/lock/kerkese-00000001
/lock/kerkese-00000002
```

### Algorithm

1. Client creates a sequential node
2. Retrieves and sorts all lock nodes
3. If it has the smallest sequence number, it acquires the lock
4. Otherwise, it watches the node immediately before it

### Properties

* Guarantees mutual exclusion
* Ensures fair ordering (FIFO)

### Edge Case Handling

* Client failure releases the lock automatically (ephemeral node deletion)
* Race conditions avoided through sequential ordering
* Avoids unnecessary notifications by watching only the predecessor node

---

## 3. Distributed Queue

### Design

* Queue path: `/queue`
* Producers create sequential znodes
* Consumers process elements in order

Example:

```
/queue/item-00000001
/queue/item-00000002
```

### Functionality

* FIFO ordering ensured through sequential nodes
* Multiple producers and consumers supported
* Each item is deleted after consumption

### Fault Tolerance

* Handles concurrent access
* Retries on conflicts (`NoNodeException`)
* Remains consistent under node failures

---

## 4. Group Messaging (Bonus)

### Design

```
/groups/group1/members/
/groups/group1/messages/
```

### Functionality

* Dynamic group membership using ephemeral nodes
* Message broadcasting through shared znodes
* Clients receive updates via watchers

---

## 5. System Recovery (Bonus)

### Mechanisms

* Leader election handled automatically by ZooKeeper quorum
* Data replication across nodes
* Automatic client reconnection and session recovery
* Majority-based operation under network partition

### Behavior Under Failure

* Node failures trigger automatic reconfiguration
* Minority partitions become inactive to maintain consistency

---

## Design Decisions

| Component          | Rationale                    |
| ------------------ | ---------------------------- |
| Ephemeral nodes    | Automatic failure detection  |
| Sequential nodes   | Ordering and synchronization |
| Watchers           | Event-driven architecture    |
| Three-node cluster | Fault tolerance via quorum   |

---

## Trade-offs

* Prioritizes consistency over availability (CAP theorem)
* Slight increase in latency due to quorum-based writes
* Not suitable for large-scale data storage

---

## Testing

The system has been tested with:

* Concurrent producers and consumers
* Multiple clients competing for locks
* Simulated node failures
* Continuous monitoring of cluster state

---

## Author

Name: Brajan Xholaj
Project: Apache ZooKeeper Distributed Systems

---

## Status

All core components are implemented and functional.
Bonus components (Group Messaging and System Recovery) are implemented and tested.
