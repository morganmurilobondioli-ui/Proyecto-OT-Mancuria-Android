package com.company.appMancuria.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.company.appMancuria.R;
import com.company.appMancuria.models.EmpresaConfig;
import com.company.appMancuria.models.OrdenTrabajo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OtPdfGenerator {
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 34;
    private static final int RED = Color.rgb(219, 45, 44);
    private static final int DARK = Color.rgb(20, 20, 20);
    private static final int TEXT = Color.rgb(34, 34, 34);
    private static final int MUTED = Color.rgb(105, 105, 105);
    private static final int LIGHT = Color.rgb(244, 245, 247);

    private final Context context;
    private final PdfDocument document = new PdfDocument();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    private final NumberFormat money = NumberFormat.getNumberInstance(Locale.US);

    private Canvas canvas;
    private PdfDocument.Page currentPage;
    private int pageNumber = 0;
    private int y = MARGIN;

    private OtPdfGenerator(Context context) {
        this.context = context;
        money.setMinimumFractionDigits(2);
        money.setMaximumFractionDigits(2);
    }

    public static Uri generate(Context context, OrdenTrabajo orden, EmpresaConfig empresa) throws Exception {
        OtPdfGenerator generator = new OtPdfGenerator(context);
        return generator.create(orden, empresa);
    }

    private Uri create(OrdenTrabajo orden, EmpresaConfig empresa) throws Exception {
        startPage();
        drawHeader(empresa, orden);
        drawVehicleSummary(orden);
        drawServices(orden);
        drawPartsTable(orden);
        drawTotals(orden);
        drawFooter(empresa);
        finishPage();

        String fileName = "OT_" + cleanFilePart(orden.getPlaca()) + "_" + cleanFilePart(orden.getId()) + ".pdf";
        Uri uri = writeToDownloads(fileName);
        document.close();
        return uri;
    }

    private void startPage() {
        pageNumber++;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
        currentPage = document.startPage(pageInfo);
        canvas = currentPage.getCanvas();
        y = MARGIN;
        canvas.drawColor(Color.WHITE);
    }

    private void finishPage() {
        if (currentPage != null) {
            document.finishPage(currentPage);
            currentPage = null;
        }
    }

    private void drawHeader(EmpresaConfig empresa, OrdenTrabajo orden) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(DARK);
        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 98), 18, 18, paint);

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_logo);
        if (logo != null) {
            RectF logoRect = new RectF(MARGIN + 16, y + 18, MARGIN + 174, y + 80);
            canvas.drawBitmap(logo, null, logoRect, null);
        }

        drawText(empresa.getNombreComercial().toUpperCase(Locale.ROOT), PAGE_WIDTH - MARGIN - 220, y + 32, 15, Color.WHITE, true);
        drawWrappedText(empresa.getRubro(), PAGE_WIDTH - MARGIN - 220, y + 52, 206, 10, Color.rgb(220, 220, 220), false, 12);
        drawText("RUC: " + empresa.getRuc(), PAGE_WIDTH - MARGIN - 220, y + 83, 10, Color.WHITE, true);

        y += 116;

        drawText("ORDEN DE TRABAJO", MARGIN, y, 13, MUTED, true);
        drawText("OT " + shortId(orden.getId()), PAGE_WIDTH - MARGIN - 120, y, 13, RED, true);
        y += 10;
    }

    private void drawVehicleSummary(OrdenTrabajo orden) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(RED);
        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 82), 18, 18, paint);

        drawText("PLACA", MARGIN + 18, y + 28, 12, Color.WHITE, true);
        drawText(orden.getPlaca().toUpperCase(Locale.ROOT), MARGIN + 18, y + 66, 34, Color.WHITE, true);
        drawText(orden.getEstado().toUpperCase(Locale.ROOT), PAGE_WIDTH - MARGIN - 135, y + 36, 13, Color.WHITE, true);
        drawText(dateFormat.format(new Date(orden.getFechaIngreso())), PAGE_WIDTH - MARGIN - 135, y + 61, 11, Color.WHITE, false);
        y += 102;

        drawInfoGrid("DUEÑO", orden.getClienteNombre(), "VEHICULO", orden.getMarcaModelo());
        drawInfoGrid("KILOMETRAJE", formatKm(orden.getKilometraje()), "LLEGADA", dateFormat.format(new Date(orden.getFechaIngreso())));
    }

    private void drawServices(OrdenTrabajo orden) {
        section("TRABAJOS / SERVICIOS");
        if (orden.getFallasReportadas().isEmpty()) {
            drawWrappedText("Sin servicios registrados.", MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 12, TEXT, false, 16);
            y += 20;
        } else {
            for (String falla : orden.getFallasReportadas()) {
                ensureSpace(34);
                drawText("-", MARGIN, y + 13, 12, RED, true);
                int used = drawWrappedText(falla, MARGIN + 16, y, PAGE_WIDTH - (MARGIN * 2) - 16, 12, TEXT, false, 15);
                y += Math.max(18, used);
            }
        }

        String trabajo = orden.getTrabajoRealizado();
        if (trabajo != null && !trabajo.trim().isEmpty()) {
            y += 6;
            drawText("OBSERVACIONES", MARGIN, y, 10, MUTED, true);
            y += 10;
            y += drawWrappedText(trabajo, MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 11, TEXT, false, 15);
        }
        y += 8;
    }

    private void drawPartsTable(OrdenTrabajo orden) {
        section("PIEZAS USADAS");
        drawTableHeader();

        if (orden.getPiezasUsadas().isEmpty()) {
            y += 8;
            drawText("Sin piezas registradas.", MARGIN + 8, y + 16, 11, MUTED, false);
            y += 28;
            return;
        }

        int item = 1;
        for (OrdenTrabajo.PiezaUsada pieza : orden.getPiezasUsadas()) {
            if (pieza == null) continue;
            ensureSpace(38);
            int rowTop = y;
            drawText(String.valueOf(item++), MARGIN + 8, rowTop + 18, 10, TEXT, false);
            drawWrappedText(pieza.getNombre(), MARGIN + 34, rowTop + 6, 245, 10, TEXT, false, 12);
            drawText(String.valueOf(pieza.getCantidad()), PAGE_WIDTH - MARGIN - 166, rowTop + 18, 10, TEXT, false);
            drawText("S/ " + money(pieza.getPrecioUnitario()), PAGE_WIDTH - MARGIN - 118, rowTop + 18, 10, TEXT, false);
            drawText("S/ " + money(pieza.getSubtotal()), PAGE_WIDTH - MARGIN - 56, rowTop + 18, 10, TEXT, true);
            y += 34;
            drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, Color.rgb(232, 232, 232));
        }
        y += 10;
    }

    private void drawTotals(OrdenTrabajo orden) {
        ensureSpace(112);
        int left = PAGE_WIDTH - MARGIN - 210;
        paint.setColor(LIGHT);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(left, y, PAGE_WIDTH - MARGIN, y + 96), 14, 14, paint);

        drawAmountLine("Mano de obra", orden.getMontoManoObra(), left + 14, y + 24, false);
        drawAmountLine("Piezas", orden.getTotalPiezas(), left + 14, y + 48, false);
        drawLine(left + 14, y + 60, PAGE_WIDTH - MARGIN - 14, Color.rgb(215, 215, 215));
        drawAmountLine("TOTAL", orden.getMontoTotal(), left + 14, y + 84, true);
        y += 114;
    }

    private void drawFooter(EmpresaConfig empresa) {
        ensureSpace(72);
        drawLine(MARGIN, PAGE_HEIGHT - 82, PAGE_WIDTH - MARGIN, RED);
        drawWrappedText(empresa.getNotaPdf(), MARGIN, PAGE_HEIGHT - 66, PAGE_WIDTH - (MARGIN * 2), 9, MUTED, false, 11);
        drawText(empresa.getDireccion(), MARGIN, PAGE_HEIGHT - 34, 9, MUTED, false);
        drawText(empresa.getCorreo() + "  |  Tel: " + empresa.getTelefono(), MARGIN, PAGE_HEIGHT - 20, 9, MUTED, false);
    }

    private void drawInfoGrid(String labelA, String valueA, String labelB, String valueB) {
        ensureSpace(58);
        int colW = (PAGE_WIDTH - (MARGIN * 2) - 12) / 2;
        drawInfoBox(MARGIN, y, colW, labelA, valueA);
        drawInfoBox(MARGIN + colW + 12, y, colW, labelB, valueB);
        y += 62;
    }

    private void drawInfoBox(int x, int top, int w, String label, String value) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(LIGHT);
        canvas.drawRoundRect(new RectF(x, top, x + w, top + 48), 12, 12, paint);
        drawText(label, x + 12, top + 17, 8, MUTED, true);
        drawWrappedText(value, x + 12, top + 30, w - 24, 12, TEXT, true, 13);
    }

    private void section(String title) {
        ensureSpace(44);
        y += 8;
        drawText(title, MARGIN, y + 15, 12, RED, true);
        drawLine(MARGIN, y + 24, PAGE_WIDTH - MARGIN, Color.rgb(225, 225, 225));
        y += 36;
    }

    private void drawTableHeader() {
        ensureSpace(28);
        paint.setColor(DARK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 28), 8, 8, paint);
        drawText("#", MARGIN + 8, y + 18, 9, Color.WHITE, true);
        drawText("PIEZA", MARGIN + 34, y + 18, 9, Color.WHITE, true);
        drawText("CANT.", PAGE_WIDTH - MARGIN - 170, y + 18, 9, Color.WHITE, true);
        drawText("P. UNIT.", PAGE_WIDTH - MARGIN - 124, y + 18, 9, Color.WHITE, true);
        drawText("IMPORTE", PAGE_WIDTH - MARGIN - 62, y + 18, 9, Color.WHITE, true);
        y += 30;
    }

    private void drawAmountLine(String label, double amount, int x, int baseline, boolean total) {
        drawText(label, x, baseline, total ? 13 : 10, total ? RED : MUTED, true);
        drawText("S/ " + money(amount), PAGE_WIDTH - MARGIN - 84, baseline, total ? 14 : 11, total ? RED : TEXT, true);
    }

    private int drawWrappedText(String text, int x, int baseline, int maxWidth, int textSize, int color, boolean bold, int lineHeight) {
        String value = text != null ? text.trim() : "";
        if (value.isEmpty()) return 0;

        setupPaint(textSize, color, bold);
        int lines = 0;
        for (String paragraph : value.split("\\r?\\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    canvas.drawText(line.toString(), x, baseline + (lines * lineHeight), paint);
                    lines++;
                    line = new StringBuilder(word);
                }
            }
            if (line.length() > 0) {
                canvas.drawText(line.toString(), x, baseline + (lines * lineHeight), paint);
                lines++;
            }
        }
        return Math.max(lineHeight, lines * lineHeight);
    }

    private void drawText(String text, int x, int baseline, int textSize, int color, boolean bold) {
        setupPaint(textSize, color, bold);
        canvas.drawText(text != null ? text : "", x, baseline, paint);
    }

    private void setupPaint(int textSize, int color, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
    }

    private void drawLine(int x1, int y1, int x2, int color) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(color);
        canvas.drawLine(x1, y1, x2, y1, paint);
    }

    private void ensureSpace(int height) {
        if (y + height < PAGE_HEIGHT - 96) return;
        drawFooter(new EmpresaConfig());
        finishPage();
        startPage();
    }

    private Uri writeToDownloads(String fileName) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Mancuria");
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("No se pudo crear el archivo PDF");
            try (OutputStream os = resolver.openOutputStream(uri)) {
                document.writeTo(os);
            }
            return uri;
        }

        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Mancuria");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("No se pudo crear la carpeta Mancuria");
        File file = new File(dir, fileName);
        try (OutputStream os = new FileOutputStream(file)) {
            document.writeTo(os);
        }
        return Uri.fromFile(file);
    }

    private String shortId(String id) {
        if (id == null || id.trim().isEmpty()) return "SIN-ID";
        return id.length() > 8 ? id.substring(0, 8).toUpperCase(Locale.ROOT) : id.toUpperCase(Locale.ROOT);
    }

    private String cleanFilePart(String value) {
        String text = value != null ? value.trim() : "";
        if (text.isEmpty()) return "orden";
        return text.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String money(double value) {
        return money.format(Math.max(0.0, value));
    }

    private String formatKm(int km) {
        return NumberFormat.getIntegerInstance(Locale.US).format(Math.max(0, km)) + " km";
    }
}
