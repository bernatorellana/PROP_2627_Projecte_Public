package edu.upc.epsevg.prop.bombardment.bench;

import edu.upc.epsevg.prop.bombardment.engine.EngineOptions;
import edu.upc.epsevg.prop.bombardment.engine.GameRecord;
import edu.upc.epsevg.prop.bombardment.engine.Match;
import edu.upc.epsevg.prop.bombardment.engine.MatchResult;
import edu.upc.epsevg.prop.bombardment.engine.Openings;
import edu.upc.epsevg.prop.bombardment.engine.PlayerSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import edu.upc.epsevg.prop.bombardment.bench.ProjectFiles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Locale;
import java.util.Map;
import edu.upc.epsevg.prop.bombardment.PlayerType;

/**
 * Banc de proves (c): challenge de moltes partides entre dos jugadors configurables.
 *
 * <pre>
 * Ús: Challenge fitxer.properties [clau=valor ...]
 *     Challenge playerA=... playerB=profe1 games=20 ...
 * </pre>
 * Claus (valors per defecte entre parèntesis):
 * <ul>
 * <li>{@code name} nom del challenge</li>
 * <li>{@code playerA}, {@code playerB}: vegeu {@link PlayerSpec} (obligatoris)</li>
 * <li>{@code size} (8), {@code rows} (2), {@code timeMs} (1000)</li>
 * <li>{@code openings}: fitxer d'obertures. Si no n'hi ha, es generen amb {@code openings.count} (50),
 * {@code openings.plies} (4) i {@code openings.seed} (1). Partides = 2 × obertures.</li>
 * <li>{@code parallel} (1): partides simultànies. No en poseu més que nuclis físics / 2.</li>
 * <li>{@code isolated} (true), {@code graceMs} (100), {@code jvmArgs} (-Xmx1g -XX:ActiveProcessorCount=1)</li>
 * <li>{@code output} (results): carpeta on es desen el resum (HTML) i totes les partides (CSV)</li>
 * </ul>
 * Les rutes relatives dels fitxers es resolen respecte la carpeta del fitxer de configuració.
 *
 * @author bernat
 */
public final class Challenge {

    private Challenge() {
    }

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        Path base = Paths.get(".");
        List<String> overrides = new ArrayList<>();
        for (String a : args) {
            if (a.contains("=")) {
                overrides.add(a);
            } else {
                Path f = ProjectFiles.locate(a);
                try (Reader r = java.nio.file.Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    p.load(r);
                }
                base = f.toAbsolutePath().getParent();
            }
        }
        for (String o : overrides) {
            int i = o.indexOf('=');
            p.setProperty(o.substring(0, i).trim(), o.substring(i + 1).trim());
        }
        if (args.length == 0) {
            Path f = ProjectFiles.locate("challenges/exemple.properties");
            System.out.println("Sense arguments: executo " + f + "\n");
            try (Reader r = java.nio.file.Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                p.load(r);
            }
            base = f.toAbsolutePath().getParent();
        }
        if (p.getProperty("playerA") == null || p.getProperty("playerB") == null) {
            System.err.println("Cal indicar playerA i playerB. Vegeu la documentació de la classe Challenge.");
            System.exit(2);
        }
        MatchResult r = run(p, base);
        System.exit(r == null ? 1 : 0);
    }

    /** Executa el challenge descrit per p. Escriu progrés per consola i desa CSV + resum. */
    public static MatchResult run(Properties p, Path base) throws IOException, InterruptedException {
        String name = p.getProperty("name", "challenge");
        PlayerSpec a = PlayerSpec.parse(p.getProperty("playerA"));
        PlayerSpec b = PlayerSpec.parse(p.getProperty("playerB"));
        int size = Integer.parseInt(p.getProperty("size", "8"));
        int rows = Integer.parseInt(p.getProperty("rows", "2"));
        long timeMs = Long.parseLong(p.getProperty("timeMs", "1000"));
        int parallel = Integer.parseInt(p.getProperty("parallel", "1"));
        EngineOptions opt = new EngineOptions();
        opt.isolated = Boolean.parseBoolean(p.getProperty("isolated", "true"));
        opt.graceMs = Long.parseLong(p.getProperty("graceMs", "100"));
        if (p.getProperty("jvmArgs") != null) opt.jvmArgs = Arrays.asList(p.getProperty("jvmArgs").trim().split("\\s+"));
        Path out = base.resolve(p.getProperty("output", "results")).normalize();
        opt.logDir = out.resolve("logs");

        List<String> openings;
        String desc;
        if (p.getProperty("openings") != null && !p.getProperty("openings").isBlank()) {
            Path f = base.resolve(p.getProperty("openings"));
            openings = Openings.load(f);
            desc = f.getFileName().toString();
            if (p.getProperty("openings.count") != null) {   // per fer proves ràpides amb les primeres N obertures
                int count = Math.min(openings.size(), Integer.parseInt(p.getProperty("openings.count").trim()));
                openings = new java.util.ArrayList<>(openings.subList(0, count));
                desc += " (primeres " + count + ")";
            }
        } else {
            int count = Integer.parseInt(p.getProperty("openings.count", "50"));
            int plies = Integer.parseInt(p.getProperty("openings.plies", "4"));
            long seed = Long.parseLong(p.getProperty("openings.seed", "1"));
            openings = Openings.generate(size, rows, count, plies, seed);
            desc = "generades: " + count + " × " + plies + " plies, llavor " + seed;
        }
        for (String fen : openings) {
            if (fen.split("/").length != size) {
                throw new IllegalArgumentException("Les obertures no són de mida " + size + ": " + fen);
            }
        }
        int total = openings.size() * 2;
        System.out.printf("== %s ==%nA = %s%nB = %s%n%dx%d, %d ms/jugada, %d partides (%d obertures, %s), %d en paral·lel, %s%n%n",
                name, a.getLabel(), b.getLabel(), size, size, timeMs, total, openings.size(), desc, parallel,
                opt.isolated ? "jugadors aïllats" : "jugadors al mateix procés (NO vàlid per avaluar)");

        long t0 = System.currentTimeMillis();
        MatchResult r = new Match(a, b, openings, timeMs, parallel, opt).run((partial, g) -> {
            GameRecord rec = g.record;
            String res = g.scoreA() == 1 ? "guanya A" : g.scoreA() == 0 ? "guanya B" : "taules  ";
            System.out.printf("[%3d/%d] obertura %3d, A=%s: %s  %-12s %3d plies  prof A %.1f B %.1f  | A %.1f%%%s%n",
                    partial.games.size(), total, g.opening, g.colourA == edu.upc.epsevg.prop.bombardment.PlayerType.PLAYER1 ? "W" : "B",
                    res, rec.end, rec.plies.size(), rec.avgDepth(g.colourA), rec.avgDepth(g.colourA.opposite()),
                    100 * partial.scoreA(), rec.detail != null ? "  !! " + rec.detail : "");
        });
        double secs = (System.currentTimeMillis() - t0) / 1000.0;
        System.out.println();
        System.out.println(r.summaryLine());
        if (!r.forfeitsOfA().isEmpty()) System.out.println("Incidències d'A: " + r.forfeitsOfA());
        if (!r.forfeitsOfB().isEmpty()) System.out.println("Incidències de B: " + r.forfeitsOfB());
        System.out.printf("Temps total: %.0f s%n", secs);

        Files.createDirectories(out);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        String fileBase = name.replaceAll("[^A-Za-z0-9._-]", "_") + "-" + stamp;
        writeCsv(out.resolve(fileBase + ".csv"), r);
        writeSummary(out.resolve(fileBase + ".html"), r, p, desc, secs);
        System.out.println("Resultats a " + out.resolve(fileBase + ".html") + " (resum) i .csv (totes les partides)");
        return r;
    }

    static void writeCsv(Path f, MatchResult r) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(f, StandardCharsets.UTF_8))) {
            w.println("game,opening,colourA,scoreA,end,plies,depthA,depthB,nodesA,nodesB,maxMsA,maxMsB,detail,startFen,moves");
            for (MatchResult.Game g : r.games) {
                GameRecord rec = g.record;
                w.printf(java.util.Locale.ROOT, "%d,%d,%s,%.1f,%s,%d,%.2f,%.2f,%.0f,%.0f,%d,%d,\"%s\",%s,%s%n",
                        g.index, g.opening, g.colourA, g.scoreA(), rec.end, rec.plies.size(),
                        rec.avgDepth(g.colourA), rec.avgDepth(g.colourA.opposite()),
                        rec.avgNodes(g.colourA), rec.avgNodes(g.colourA.opposite()),
                        rec.maxElapsed(g.colourA), rec.maxElapsed(g.colourA.opposite()),
                        rec.detail == null ? "" : rec.detail.replace('"', '\''), rec.startFen, rec.moveList());
            }
        }
    }

    static void writeSummary(Path f, MatchResult r, Properties p, String desc, double secs) throws IOException {
        String size = p.getProperty("size", "8");
        StringBuilder h = new StringBuilder();
        h.append("<!doctype html><html lang=\"ca\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" ")
                .append("content=\"width=device-width,initial-scale=1\"><title>").append(esc(p.getProperty("name", "challenge")))
                .append("</title><style>").append(CSS).append("</style></head><body><main>");
        h.append("<h1>").append(esc(p.getProperty("name", "challenge"))).append("</h1>");
        h.append("<p class=\"meta\">A: <b>").append(esc(r.labelA)).append("</b> (").append(esc(r.nameA))
                .append(") · B: <b>").append(esc(r.labelB)).append("</b> (").append(esc(r.nameB)).append(")<br>")
                .append("Tauler ").append(size).append("×").append(size).append(" · ")
                .append(p.getProperty("timeMs", "1000")).append(" ms/jugada · obertures: ").append(esc(desc))
                .append(" · ").append(r.games.size()).append(" partides · ")
                .append(String.format(Locale.ROOT, "%.0f s", secs)).append("</p>");
        h.append("<div class=\"tiles\">");
        tile(h, "Puntuació A", String.format(Locale.ROOT, "%.1f%%", 100 * r.scoreA()),
                String.format(Locale.ROOT, "± %.1f (error estàndard)", 100 * r.standardError()));
        tile(h, "A guanya / taules / B guanya", r.winsA() + " / " + r.draws() + " / " + r.winsB(), "");
        tile(h, "Diferència Elo A − B", Double.isInfinite(r.eloDiff()) ? (r.eloDiff() > 0 ? "+∞" : "−∞")
                : String.format(Locale.ROOT, "%+.0f", r.eloDiff()), "");
        tile(h, "Profunditat mitjana", String.format(Locale.ROOT, "%.2f vs %.2f", r.avgDepthA(), r.avgDepthB()),
                String.format(Locale.ROOT, "%.1f plies per partida", r.avgPlies()));
        h.append("</div>");
        long timeMs = Long.parseLong(p.getProperty("timeMs", "1000").trim());
        long graceMs = Long.parseLong(p.getProperty("graceMs", "100").trim());
        h.append("<h2>Incidències</h2>");
        incidents(h, "A", r.labelA, r.forfeitsOfA(), r.maxElapsedA(), timeMs, graceMs);
        incidents(h, "B", r.labelB, r.forfeitsOfB(), r.maxElapsedB(), timeMs, graceMs);
        h.append("<h2>Partides</h2><table><thead><tr><th>#</th><th>Obertura</th><th>A juga</th><th>Resultat A</th>")
                .append("<th class=\"l\">Final</th><th>Plies</th><th>Prof. A</th><th>Prof. B</th><th class=\"l\">Detall</th></tr></thead><tbody>");
        for (MatchResult.Game g : r.games) {
            GameRecord rec = g.record;
            String res = g.scoreA() == 1 ? "<span class=\"w\">guanya</span>" : g.scoreA() == 0
                    ? "<span class=\"lo\">perd</span>" : "taules";
            h.append(String.format(Locale.ROOT, "<tr><td>%d</td><td>%d</td><td>%s</td><td>%s</td><td class=\"l\">%s</td>"
                    + "<td>%d</td><td>%.1f</td><td>%.1f</td><td class=\"l\">%s</td></tr>", g.index, g.opening,
                    g.colourA == PlayerType.PLAYER1 ? "blanques" : "negres", res, rec.end.text, rec.plies.size(),
                    rec.avgDepth(g.colourA), rec.avgDepth(g.colourA.opposite()), esc(rec.detail)));
        }
        h.append("</tbody></table><p class=\"meta\">Dades completes (amb les jugades): fitxer .csv del mateix nom.</p>")
                .append("</main></body></html>");
        Files.writeString(f, h.toString(), StandardCharsets.UTF_8);
    }

    private static void tile(StringBuilder h, String label, String value, String sub) {
        h.append("<div class=\"tile\"><div class=\"k\">").append(esc(label)).append("</div><div class=\"v\">")
                .append(esc(value)).append("</div><div class=\"s\">").append(esc(sub)).append("</div></div>");
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Caixa d'incidències d'un jugador: en vermell si n'hi ha, perquè no passin desapercebudes. */
    private static void incidents(StringBuilder h, String side, String label, Map<GameRecord.End, Integer> f,
                                  long maxMs, long timeMs, long graceMs) {
        int total = f.values().stream().mapToInt(Integer::intValue).sum();
        h.append("<div class=\"inc ").append(total == 0 ? "ok" : "ko").append("\"><b>").append(side).append(" · ")
                .append(esc(label)).append("</b>: ");
        if (total == 0) {
            h.append("cap partida perduda per incidències.");
        } else {
            h.append(total).append(total == 1 ? " partida perduda" : " partides perdudes").append(" per incidències:<ul>");
            for (Map.Entry<GameRecord.End, Integer> e : f.entrySet()) {
                String why;
                switch (e.getKey()) {
                    case HUNG: why = "no ha respost a timeout() dins el marge de " + graceMs + " ms: el procés s'ha mort"; break;
                    case ERROR: why = "excepció o jugada nul·la (vegeu el detall i el fitxer de registre a logs/)"; break;
                    case ILLEGAL: why = "jugada il·legal"; break;
                    case CRASHED: why = "el procés del jugador ha mort (p.ex. OutOfMemoryError o System.exit)"; break;
                    default: why = e.getKey().text;
                }
                h.append("<li><b>").append(e.getValue()).append("</b> × ").append(esc(why)).append("</li>");
            }
            h.append("</ul>");
        }
        h.append("<div class=\"s\">Temps màxim de resposta en una jugada vàlida: ").append(maxMs).append(" ms (límit ")
                .append(timeMs).append(" + ").append(graceMs).append(" ms de marge)");
        if (timeMs > 0 && maxMs > timeMs + graceMs / 2) h.append(" · <b>aneu just de temps</b>");
        h.append("</div></div>");
    }

    private static final String CSS = ":root{--bg:#faf8f4;--fg:#1d1b18;--muted:#6b665e;--line:#e4dfd6;--card:#fff;--good:#1d7a3a;--bad:#b3261e}"
            + "@media (prefers-color-scheme:dark){:root{--bg:#181715;--fg:#ece8e1;--muted:#a39d93;--line:#34312c;--card:#211f1c;--good:#6bcf8a;--bad:#ff8a80}}"
            + "body{margin:0;background:var(--bg);color:var(--fg);font:15px/1.45 system-ui,-apple-system,Segoe UI,sans-serif}"
            + "main{max-width:1000px;margin:0 auto;padding:20px 16px}h1{margin:0 0 4px;font-size:24px}h2{margin:24px 0 8px;font-size:17px}"
            + ".meta{color:var(--muted);margin:0}.tiles{display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:12px;margin-top:16px}"
            + ".tile{background:var(--card);border:1px solid var(--line);border-radius:10px;padding:12px 14px}"
            + ".k{color:var(--muted);font-size:13px}.v{font-size:24px;font-weight:700;font-variant-numeric:tabular-nums}.s{color:var(--muted);font-size:12px}"
            + "table{border-collapse:collapse;width:100%;font-variant-numeric:tabular-nums;font-size:14px}"
            + "th,td{padding:4px 8px;border-bottom:1px solid var(--line);text-align:right}th{color:var(--muted);font-weight:600}"
            + ".l{text-align:left}.w{color:var(--good);font-weight:600}.lo{color:var(--bad);font-weight:600}"
            + ".inc{border:1px solid var(--line);border-left:4px solid var(--good);border-radius:8px;padding:8px 12px;margin:8px 0;background:var(--card)}"
            + ".inc.ko{border-left-color:var(--bad)}.inc.ko>b:first-child{color:var(--bad)}.inc ul{margin:4px 0 4px 18px;padding:0}";
}
