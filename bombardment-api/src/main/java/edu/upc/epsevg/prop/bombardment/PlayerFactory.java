package edu.upc.epsevg.prop.bombardment;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * Crea jugadors a partir d'una especificació de text:
 * {@code nom.complet.DeLaClasse} o {@code nom.complet.DeLaClasse(arg1,arg2,...)}.
 * Els arguments es converteixen al tipus del constructor (String, int, long, double, float, boolean).
 *
 * @author bernat
 */
public final class PlayerFactory {

    private PlayerFactory() {
    }

    public static IPlayer create(String spec) {
        return create(spec, Thread.currentThread().getContextClassLoader());
    }

    public static IPlayer create(String spec, ClassLoader loader) {
        spec = spec.trim();
        String className = spec;
        List<String> args = new ArrayList<>();
        int p = spec.indexOf('(');
        if (p >= 0) {
            if (!spec.endsWith(")")) {
                throw new IllegalArgumentException("Especificació de jugador mal formada: " + spec);
            }
            className = spec.substring(0, p).trim();
            String inner = spec.substring(p + 1, spec.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String a : inner.split(",")) {
                    args.add(a.trim());
                }
            }
        }
        try {
            Class<?> c = Class.forName(className, true, loader);
            if (!IPlayer.class.isAssignableFrom(c)) {
                throw new IllegalArgumentException(className + " no implementa IPlayer");
            }
            for (Constructor<?> k : c.getConstructors()) {
                if (k.getParameterCount() != args.size()) {
                    continue;
                }
                Object[] values = convert(k.getParameterTypes(), args);
                if (values != null) {
                    return (IPlayer) k.newInstance(values);
                }
            }
            throw new IllegalArgumentException("No hi ha cap constructor públic de " + className
                    + " compatible amb els arguments " + args);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("No trobo la classe del jugador: " + className, e);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new IllegalStateException("Error creant " + spec + ": " + cause, cause);
        }
    }

    private static Object[] convert(Class<?>[] types, List<String> args) {
        Object[] res = new Object[types.length];
        try {
            for (int i = 0; i < types.length; i++) {
                String a = args.get(i);
                Class<?> t = types[i];
                if (t == String.class) res[i] = a;
                else if (t == int.class || t == Integer.class) res[i] = Integer.parseInt(a);
                else if (t == long.class || t == Long.class) res[i] = Long.parseLong(a);
                else if (t == double.class || t == Double.class) res[i] = Double.parseDouble(a);
                else if (t == float.class || t == Float.class) res[i] = Float.parseFloat(a);
                else if (t == boolean.class || t == Boolean.class) {
                    if (!a.equalsIgnoreCase("true") && !a.equalsIgnoreCase("false")) return null;
                    res[i] = Boolean.parseBoolean(a);
                } else return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return res;
    }
}
