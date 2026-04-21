package org.example;

import org.apache.zookeeper.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusterMonitor implements Watcher {

    private ZooKeeper lidhjaZooKeeper;
    private static final String RRUGA_KRYESORE = "/cluster";
    private Set<String> nyjetEMeparshme = new HashSet<>();
    private static final DateTimeFormatter FORMATI_ORARIT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public ClusterMonitor(String adresaLidhjes) throws Exception {
        // Lidhemi me clusterin me te 3 adresat per fault tolerance
        lidhjaZooKeeper = new ZooKeeper(adresaLidhjes, 3000, this);
        Thread.sleep(1000);

        // Krijojme nyjen kryesore nese nuk ekziston
        if (lidhjaZooKeeper.exists(RRUGA_KRYESORE, false) == null) {
            lidhjaZooKeeper.create(RRUGA_KRYESORE, new byte[0],
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    // Thirret automatikisht nga Zookeeper kur ndodh nje ndryshim
    @Override
    public void process(WatchedEvent ngjarje) {
        if (ngjarje.getType() == Event.EventType.NodeChildrenChanged) {
            try {
                kontrolloNdryshimet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Kontrollon nese nje nyje u shtua ose u hoq
    private void kontrolloNdryshimet() throws Exception {
        List<String> nyjetAktive = lidhjaZooKeeper.getChildren(RRUGA_KRYESORE, true);
        Set<String> bashkesiAktuale = new HashSet<>(nyjetAktive);
        String ora = LocalTime.now().format(FORMATI_ORARIT);

        // Kontrollo per nyje te reja
        for (String nyja : bashkesiAktuale) {
            if (!nyjetEMeparshme.contains(nyja)) {
                System.out.println("[" + ora + "] ✅ NYJE E RE U BASHKUA: " + nyja);
            }
        }

        // Kontrollo per nyje qe u larguan (failure)
        for (String nyja : nyjetEMeparshme) {
            if (!bashkesiAktuale.contains(nyja)) {
                System.out.println("[" + ora + "] ❌ NYJE DESHTOI: " + nyja);
            }
        }

        nyjetEMeparshme = bashkesiAktuale;
        shfaqStatusinEClusterit(nyjetAktive);
    }

    public void regjistroNyjen(String emriNyjes) throws Exception {
        String rruga = RRUGA_KRYESORE + "/" + emriNyjes;
        // Fshij nyjen nese ekziston nga sesioni i meparshem
        if (lidhjaZooKeeper.exists(rruga, false) != null) {
            lidhjaZooKeeper.delete(rruga, -1);
        }
        // EPHEMERAL - fshihet automatikisht nese nyja crashes
        lidhjaZooKeeper.create(rruga, "aktive".getBytes(),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
        System.out.println("✅ Nyja u regjistrua: " + emriNyjes);
    }

    public void shfaqStatusinEClusterit(List<String> nyjetAktive) {
        String ora = LocalTime.now().format(FORMATI_ORARIT);
        System.out.println("\n[" + ora + "] 📊 STATUSI I CLUSTERIT:");
        System.out.println("  Nyje aktive: " + nyjetAktive.size() + "/3");
        for (String nyja : nyjetAktive) {
            System.out.println("  → " + nyja + " [AKTIVE]");
        }
        if (nyjetAktive.isEmpty()) {
            System.out.println("  ⚠️ Asnje nyje aktive!");
        }
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        // Silence DEBUG logs
        System.setProperty("zookeeper.root.logger", "ERROR");
        System.setProperty("slf4j.detectLoggerNameMismatch", "false");

        System.out.println("🚀 Sistemi i Monitorimit te Clusterit po starton...\n");

        // Lidhemi me te 3 nyjet per fault tolerance
        ClusterMonitor monitori = new ClusterMonitor(
                "localhost:2181,localhost:2182,localhost:2183"
        );

        // Regjistrojme 3 nyje te simuluara
        monitori.regjistroNyjen("nyja-1");
        monitori.regjistroNyjen("nyja-2");
        monitori.regjistroNyjen("nyja-3");

        // Shfaqim statusin fillestar
        List<String> fillestare = monitori.lidhjaZooKeeper
                .getChildren(RRUGA_KRYESORE, true);
        monitori.nyjetEMeparshme = new HashSet<>(fillestare);
        monitori.shfaqStatusinEClusterit(fillestare);

        System.out.println("👀 Duke monitoruar clusterin... (Ctrl+C per te ndalur)");
        System.out.println("💡 Provo: docker stop cluster-monitoring-zoo1-1\n");

        Thread.sleep(Long.MAX_VALUE);
    }
}