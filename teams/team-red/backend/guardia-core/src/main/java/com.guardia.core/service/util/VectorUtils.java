package com.guardia.core.service.util;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static String toPgVectorLiteral(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("El embedding no puede ser nulo ni vacío.");
        }
        StringBuilder sb = new StringBuilder(embedding.length * 10 + 2);
        sb.append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(embedding[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public static double distanciaCosenoASimilitudPorcentual(double distanciaCoseno) {
        double similitud = (1.0 - distanciaCoseno) * 100.0;
        if (similitud < 0) return 0;
        if (similitud > 100) return 100;
        return similitud;
    }
}