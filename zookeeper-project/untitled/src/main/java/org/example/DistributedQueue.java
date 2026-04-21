package org.example;

import org.apache.zookeeper.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributedQueue {

    private ZooKeeper lidhjaZooKeeper;
    private static final String RRUGA_QUEUE = "/queue";
    private static final DateTimeFormatter FORMATI =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public DistributedQueue(String adresaLidhjes) throws Exception {
        CountDownLatch lidhjaGate = new CountDownLatch(1);
        lidhjaZooKeeper = new ZooKeeper(adresaLidhjes, 3000, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                lidhjaGate.countDown();
            }
        });
        lidhjaGate.await();

        // Krijo /queue nese nuk ekziston
        try {
            lidhjaZooKeeper.create(RRUGA_QUEUE, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
            // Queue ekziston tashmë
        }
    }

    // PRODUCER: shton nje element ne queue
    public void shtoElement(String emriProducers, String vlera) throws Exception {
        String rruga = lidhjaZooKeeper.create(
                RRUGA_QUEUE + "/item-",
                vlera.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
        String emriShkurter = rruga.replace(RRUGA_QUEUE + "/", "");
        String ora = LocalTime.now().format(FORMATI);
        System.out.println("[" + ora + "] 📥 [" + emriProducers +
                "] Shtoi: '" + vlera + "' → " + emriShkurter);
    }

    // CONSUMER: merr elementin e pare nga queue
    public String merElement(String emriConsumer) throws Exception {
        while (true) {
            List<String> elementet = lidhjaZooKeeper.getChildren(RRUGA_QUEUE, false);

            if (elementet.isEmpty()) {
                String ora = LocalTime.now().format(FORMATI);
                System.out.println("[" + ora + "] ⏳ [" + emriConsumer +
                        "] Queue është bosh, duke pritur...");
                Thread.sleep(1000);
                continue;
            }

            Collections.sort(elementet);
            String elementi = elementet.get(0); // merr te parin (FIFO)
            String rruga = RRUGA_QUEUE + "/" + elementi;

            try {
                byte[] te_dhenat = lidhjaZooKeeper.getData(rruga, false, null);
                String vlera = new String(te_dhenat);
                lidhjaZooKeeper.delete(rruga, -1);
                String ora = LocalTime.now().format(FORMATI);
                System.out.println("[" + ora + "] 📤 [" + emriConsumer +
                        "] Mori: '" + vlera + "' ← " + elementi);
                return vlera;
            } catch (KeeperException.NoNodeException e) {
                // Nje consumer tjeter e mori para nesh, provojme perseri
            }
        }
    }

    public void mbyll() throws Exception {
        lidhjaZooKeeper.close();
    }

    public static void main(String[] args) throws Exception {
        String adresa = "localhost:2181,localhost:2182,localhost:2183";

        System.out.println("🚀 Distributed Queue po starton...\n");

        // 2 Producers dhe 2 Consumers
        Thread producer1 = new Thread(() -> {
            try {
                DistributedQueue q = new DistributedQueue(adresa);
                for (int i = 1; i <= 5; i++) {
                    q.shtoElement("Producer-1", "Porosia-P1-" + i);
                    Thread.sleep(500);
                }
                q.mbyll();
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread producer2 = new Thread(() -> {
            try {
                DistributedQueue q = new DistributedQueue(adresa);
                for (int i = 1; i <= 5; i++) {
                    q.shtoElement("Producer-2", "Porosia-P2-" + i);
                    Thread.sleep(700);
                }
                q.mbyll();
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread consumer1 = new Thread(() -> {
            try {
                DistributedQueue q = new DistributedQueue(adresa);
                for (int i = 0; i < 5; i++) {
                    q.merElement("Consumer-1");
                    Thread.sleep(800);
                }
                q.mbyll();
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread consumer2 = new Thread(() -> {
            try {
                DistributedQueue q = new DistributedQueue(adresa);
                for (int i = 0; i < 5; i++) {
                    q.merElement("Consumer-2");
                    Thread.sleep(800);
                }
                q.mbyll();
            } catch (Exception e) { e.printStackTrace(); }
        });

        // Startojme producers fillimisht
        producer1.start();
        producer2.start();
        Thread.sleep(500); // i japim kohe producers te shtojne disa items
        consumer1.start();
        consumer2.start();

        producer1.join();
        producer2.join();
        consumer1.join();
        consumer2.join();

        System.out.println("\n✅ Queue u perfundua!");
    }
}