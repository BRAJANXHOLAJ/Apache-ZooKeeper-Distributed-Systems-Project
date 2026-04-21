package org.example;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DistributedLock implements Watcher {

    private ZooKeeper lidhjaZooKeeper;
    private static final String RRUGA_BLLOKIMIT = "/lock";
    private String emriNyjesAktuale;
    private final String emriPunonjesit;
    private CountDownLatch pritja = new CountDownLatch(1);

    public DistributedLock(String adresaLidhjes, String emriPunonjesit) throws Exception {
        this.emriPunonjesit = emriPunonjesit;
        lidhjaZooKeeper = new ZooKeeper(adresaLidhjes, 3000, this);
        Thread.sleep(1000);

        // Krijo /lock nese nuk ekziston
        try {
            lidhjaZooKeeper.create(RRUGA_BLLOKIMIT, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        } catch (KeeperException.NodeExistsException e) {
            // Nyja ekziston tashmë, vazhdojmë normalisht
        }
    }
    @Override
    public void process(WatchedEvent ngjarje) {
        if (ngjarje.getType() == Event.EventType.NodeDeleted) {
            pritja.countDown(); // njoftojme punonjesin te provojë perseri
        }
    }

    public void fitoBllokim() throws Exception {
        // Krijojme nje nyje sekuenciale ephemeral
        emriNyjesAktuale = lidhjaZooKeeper.create(
                RRUGA_BLLOKIMIT + "/kerkese-",
                emriPunonjesit.getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );

        String emriShkurter = emriNyjesAktuale.replace(RRUGA_BLLOKIMIT + "/", "");
        System.out.println("[" + emriPunonjesit + "] 🎫 Kerkesa u krijua: " + emriShkurter);

        while (true) {
            List<String> kerkesat = lidhjaZooKeeper.getChildren(RRUGA_BLLOKIMIT, false);
            Collections.sort(kerkesat);

            String kerkesaJone = emriNyjesAktuale.replace(RRUGA_BLLOKIMIT + "/", "");
            int pozicioni = kerkesat.indexOf(kerkesaJone);

            if (pozicioni == 0) {
                // Jemi te paret - fitojme bllokim!
                System.out.println("[" + emriPunonjesit + "] 🔒 BLLOKIMI U FITUA!");
                return;
            }

            // Presim nyjen perpara nesh
            String nyjaParaardhese = kerkesat.get(pozicioni - 1);
            System.out.println("[" + emriPunonjesit + "] ⏳ Duke pritur: " + nyjaParaardhese);

            pritja = new CountDownLatch(1);
            Stat stat = lidhjaZooKeeper.exists(
                    RRUGA_BLLOKIMIT + "/" + nyjaParaardhese, this
            );

            if (stat != null) {
                pritja.await(); // presojme derisa nyja perpara fshihet
            }
        }
    }

    public void liroBllokim() throws Exception {
        lidhjaZooKeeper.delete(emriNyjesAktuale, -1);
        System.out.println("[" + emriPunonjesit + "] 🔓 Bllokimi u lirua!");
    }

    public void mbyll() throws Exception {
        lidhjaZooKeeper.close();
    }

    // Simulojme 3 punonjes konkurrent
    public static void main(String[] args) throws Exception {
        String adresa = "localhost:2181,localhost:2182,localhost:2183";

        // Krijojme 3 thread-e qe konkurrojne per bllokim
        Thread[] punonjesit = new Thread[3];
        for (int i = 0; i < 3; i++) {
            final String emri = "Punonjes-" + (i + 1);
            punonjesit[i] = new Thread(() -> {
                try {
                    DistributedLock bllokimi = new DistributedLock(adresa, emri);
                    System.out.println("[" + emri + "] 🚀 Duke u nisur...");

                    bllokimi.fitoBllokim();

                    // Simulojme pune
                    System.out.println("[" + emri + "] ⚙️  Duke punuar (2 sekonda)...");
                    Thread.sleep(2000);

                    bllokimi.liroBllokim();
                    bllokimi.mbyll();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        // Startojme te gjithe njekohesisht
        for (Thread p : punonjesit) p.start();
        for (Thread p : punonjesit) p.join();

        System.out.println("\n✅ Te gjithe punonjesit perfunduan!");
    }
}