package com.company.appMancuria.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class CodigosManager {
    private static final String PREFS_NAME = "MancuriaPrefs";
    private static final String KEY_LAST_CODE = "last_code_entered";
    private static final String KEY_WEEK_YEAR = "week_year_access";

    public static String generarCodigoAleatorio() {
        Random r = new Random();
        int n = 100000 + r.nextInt(900000); // 6 dígitos
        return String.valueOf(n);
    }

    public static String getIdentificadorSemanaActual() {
        Calendar cal = Calendar.getInstance();
        // Identificador formato: AAAA-SS (Año-Semana)
        return cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.WEEK_OF_YEAR);
    }

    public static boolean yaAccedioEstaSemana(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String guardado = prefs.getString(KEY_WEEK_YEAR, "");
        return guardado.equals(getIdentificadorSemanaActual());
    }

    public static void guardarAccesoExitoso(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_WEEK_YEAR, getIdentificadorSemanaActual()).apply();
    }
}
