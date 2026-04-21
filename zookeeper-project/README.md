

 📦 Apache ZooKeeper Distributed Systems Project



Programim në Sistemet e Shpërndara

🎯 Tema

Implementimi i disa mekanizmave të koordinimit të sistemeve të shpërndara duke përdorur Apache ZooKeeper.



 🧠 Përmbledhje

Ky projekt demonstron përdorimin praktik të ZooKeeper për ndërtimin e:

* Cluster Node Monitoring
* Distributed Locking
* Distributed Queue
* (Bonus) Group Messaging
* (Bonus) System Recovery

Sistemi është ndërtuar në **Java (Maven project)** dhe përdor API-të native të ZooKeeper.



#🏗️ Struktura e Projektit

src/
 └── main/java/org/example/
     ├── App.java
     ├── ClusterMonitor.java
     ├── DistributedLock.java
     ├── DistributedQueue.java
resources/
 └── simplelogger.properties




# ⚙️ Kërkesat

* Java 17+ (projekti përdor Java 22)
* Maven
* ZooKeeper cluster (3 nodes)



# 🚀 Setup & Ekzekutimi

## 1. Start ZooKeeper Cluster (3 nodes)


localhost:2181
localhost:2182
localhost:2183


(Mund të përdoret Docker ose instalim manual)



## 2. Build projekti

mvn clean install


## 3. Run modulet

### 🔹 Cluster Monitoring


java -cp target/untitled-1.0-SNAPSHOT.jar org.example.ClusterMonitor


### 🔹 Distributed Lock


java -cp target/untitled-1.0-SNAPSHOT.jar org.example.DistributedLock


### 🔹 Distributed Queue


java -cp target/untitled-1.0-SNAPSHOT.jar org.example.DistributedQueue




# 📡 1. Cluster Node Monitoring

## 🔧 Implementimi

* Përdor **ephemeral znodes** në `/cluster`
* Çdo node regjistrohet si:


/cluster/nyja-1
/cluster/nyja-2


## 📊 Funksionaliteti

* Monitoron:

  * Node join
  * Node failure
* Përdor **watchers (NodeChildrenChanged)**

## ⚠️ Failure Handling

* Nëse node bie:
  → ephemeral node fshihet automatikisht
  → sistemi detekton failure në kohë reale



# 🔒 2. Distributed Locking

## 🔧 Implementimi

* Path: `/lock`
* Çdo klient krijon:


EPHEMERAL_SEQUENTIAL node


Shembull:


/lock/kerkese-00000001
/lock/kerkese-00000002


## ⚙️ Algoritmi

1. Klienti krijon nyje
2. Kontrollon renditjen
3. Nëse është i pari → fiton lock
4. Nëse jo → pret nyjen paraardhëse

## ✅ Garanton

* Mutual exclusion
* Fair ordering (FIFO)

## ⚠️ Edge Cases

* Crash i klientit → lock lirohet automatikisht
* Race conditions → shmangen me sequential nodes
* Thundering herd → shmanget (watch vetëm predecessor)

---

# 📥 3. Distributed Queue

## 🔧 Implementimi

* Path: `/queue`
* Producer:

```bash
EPHEMERAL_SEQUENTIAL
```

* Consumer:

  * lexon elementin më të vogël
  * e fshin pas procesimit

## 📦 Shembull

```bash
/queue/item-00000001
/queue/item-00000002
```

## ✅ Garanton

* FIFO ordering
* Multiple producers & consumers
* No duplication

## 🧪 Testime

* Concurrent producers
* Concurrent consumers
* Node failures
* Retry mechanism (NoNodeException)

---

# 💬 4. (BONUS) Group Messaging

## 🔧 Implementimi

* Path:

```bash
/groups/group1/members/
/groups/group1/messages/
```

## ⚙️ Funksionaliteti

* Dynamic membership (ephemeral nodes)
* Broadcasting me watchers

## 📡 Mekanizmi

* Çdo client dëgjon `/messages`
* Mesazhet shpërndahen në kohë reale

---

# ♻️ 5. (BONUS) System Recovery

## 🔧 Mekanizmat

### ✔ Leader Election

* Automatic (ZooKeeper quorum)

### ✔ Data Replication

* State replikohet në cluster

### ✔ Failover

* Client reconnect automatik

### ✔ Network Partition

* Vetëm majority partition funksionon

---

# 📊 Dizajni & Vendimet Kryesore

| Komponenti       | Arsyeja                       |
| ---------------- | ----------------------------- |
| Ephemeral nodes  | Detect failures automatikisht |
| Sequential nodes | Ordering dhe locking          |
| Watchers         | Event-driven system           |
| 3-node cluster   | Fault tolerance (quorum)      |

---

# ⚖️ Trade-offs

* Consistency > Availability (CAP theorem)
* Latency pak më e lartë për shkak të quorum
* ZooKeeper nuk përdoret për storage të madh

---

# ❓ Pyetje të Mundshme në Mbrojtje

* Pse ZooKeeper dhe jo database?
* Si garanton mutual exclusion?
* Çfarë ndodh në network partition?
* Çfarë është quorum?
* Ephemeral vs Persistent nodes?

---

# 👨‍💻 Autor

* Emri: Brajan Xholaj
* Projekti: Apache ZooKeeper Distributed Systems

---

# ✅ Statusi

✔ Core tasks të implementuara
✔ Bonus tasks të implementuara
✔ Testuar me multiple threads & failure scenarios

