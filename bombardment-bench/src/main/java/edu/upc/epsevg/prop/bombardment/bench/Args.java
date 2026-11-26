package edu.upc.epsevg.prop.bombardment.bench;

import java.util.LinkedHashMap;
import java.util.Map;

/** Arguments de línia de comandes de la forma clau=valor. */
final class Args {

    private Args() {
    }

    static Map<String, String> parse(String[] args) {
        Map<String, String> m = new LinkedHashMap<>();
        for (String a : args) {
            int i = a.indexOf('=');
            if (i <= 0) throw new IllegalArgumentException("Argument no vàlid (cal clau=valor): " + a);
            m.put(a.substring(0, i).trim(), a.substring(i + 1).trim());
        }
        return m;
    }
}
