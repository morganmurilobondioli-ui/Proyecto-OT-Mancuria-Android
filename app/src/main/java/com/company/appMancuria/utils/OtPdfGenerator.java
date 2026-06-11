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
import java.util.List;
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
    private static final int TOTAL_TOP = 690;
    private static final int FOOTER_TOP = 772;

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

    public static PdfResult generate(Context context, OrdenTrabajo orden, EmpresaConfig empresa) throws Exception {
        OtPdfGenerator generator = new OtPdfGenerator(context);
        return generator.create(orden, empresa);
    }

    private PdfResult create(OrdenTrabajo orden, EmpresaConfig empresa) throws Exception {
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
        return new PdfResult(uri, fileName, "Descargas/Mancuria/" + fileName);
    }

    public static class PdfResult {
        private final Uri uri;
        private final String fileName;
        private final String displayPath;

        PdfResult(Uri uri, String fileName, String displayPath) {
            this.uri = uri;
            this.fileName = fileName;
            this.displayPath = displayPath;
        }

        public Uri getUri() { return uri; }
        public String getFileName() { return fileName; }
        public String getDisplayPath() { return displayPath; }
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
        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 86), 16, 16, paint);

        Bitmap logo = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_logo);
        if (logo != null) {
            RectF logoRect = new RectF(MARGIN + 16, y + 18, MARGIN + 174, y + 72);
            canvas.drawBitmap(logo, null, logoRect, null);
        }

        int infoX = PAGE_WIDTH - MARGIN - 224;
        drawText(empresa.getNombreComercial().toUpperCase(Locale.ROOT), infoX, y + 27, 14, Color.WHITE, true);
        drawWrappedText(empresa.getRubro(), infoX, y + 45, 210, 8, Color.rgb(220, 220, 220), false, 10, 3);
        drawText("RUC: " + empresa.getRuc(), infoX, y + 76, 9, Color.WHITE, true);

        y += 102;
        drawText("ORDEN DE TRABAJO", MARGIN, y, 13, MUTED, true);
        drawText("OT " + shortId(orden.getId()), PAGE_WIDTH - MARGIN - 120, y, 13, RED, true);
        y += 8;
    }

    private void drawVehicleSummary(OrdenTrabajo orden) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(RED);
        canvas.drawRoundRect(new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 70), 16, 16, paint);

        drawText("PLACA", MARGIN + 18, y + 23, 10, Color.WHITE, true);
        drawText(orden.getPlaca().toUpperCase(Locale.ROOT), MARGIN + 18, y + 57, 31, Color.WHITE, true);
        drawText(orden.getEstado().toUpperCase(Locale.ROOT), PAGE_WIDTH - MARGIN - 135, y + 31, 12, Color.WHITE, true);
        drawText(dateFormat.format(new Date(orden.getFechaIngreso())), PAGE_WIDTH - MARGIN - 135, y + 54, 10, Color.WHITE, false);
        y += 84;

        drawInfoGrid("DUENO", orden.getClienteNombre(), "VEHICULO", orden.getMarcaModelo());
        drawInfoGrid("KILOMETRAJE", formatKm(orden.getKilometraje()), "LLEGADA", dateFormat.format(new Date(orden.getFechaIngreso())));
    }

    private void drawServices(OrdenTrabajo orden) {
        section("TRABAJOS / SERVICIOS");
        int sectionTop = y;
        List<String> servicios = orden.getFallasReportadas();

        if (servicios.isEmpty()) {
            drawWrappedText("Sin servicios registrados.", MARGIN, y, PAGE_WIDTH - (MARGIN * 2), 10, TEXT, false, 13);
            y += 16;
        } else {
            int colW = (PAGE_WIDTH - (MARGIN * 2) - 20) / 2;
            int leftY = y;
            int rightY = y;
            for (int i = 0; i < servicios.size(); i++) {
                int x = i % 2 == 0 ? MARGIN : MARGIN + colW + 20;
                int rowY = i % 2 == 0 ? leftY : rightY;
                drawText("-", x, rowY + 10, 9, RED, true);
                int used = drawWrappedText(servicios.get(i), x + 12, rowY, colW - 12, 9, TEXT, false, 12, 2);
                rowY += Math.max(14, used);
                if (i % 2 == 0) leftY = rowY; else rightY = rowY;
            }
            y = Math.max(leftY, rightY);
        }

        String trabajo = orden.getTrabajoRealizado();
        if (trabajo != null && !trabajo.trim().isEmpty()) {
            y += 5;
            drawText("OBSERVACIONES", MARGIN, y + 9, 9, MUTED, true);
            paint.setColor(LIGHT);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(MARGIN + 100, y - 4, PAGE_WIDTH - MARGIN, y + 24), 8, 8, paint);
            drawWrappedText(trabajo, MARGIN + 110, y + 11, PAGE_WIDTH - (MARGIN * 2) - 120, 9, TEXT, false, 11, 2);
            y += 30;
        }

        y = Math.min(Math.max(y + 3, sectionTop + 34), 420);
    }

    private void drawPartsTable(OrdenTrabajo orden) {
        section("PIEZAS USADAS");
        List<OrdenTrabajo.PiezaUsada> piezas = orden.getPiezasUsadas();

        int count = 0;
        for (OrdenTrabajo.PiezaUsada pieza : piezas) {
            if (pieza != null) count++;
        }

        if (count == 0) {
            drawText("Sin piezas registradas.", MARGIN + 8, y + 14, 10, MUTED, false);
            y += 22;
            return;
        }

        int gap = 12;
        int colW = (PAGE_WIDTH - (MARGIN * 2) - gap) / 2;
        int rowsPerColumn = Math.max(1, (int) Math.ceil(count / 2.0));
        int available = Math.max(120, TOTAL_TOP - y - 18);
        int rowHeight = Math.max(21, Math.min(32, (available - 24) / rowsPerColumn));
        int rowFont = rowHeight <= 23 ? 8 : 9;

        drawCompactPartsHeader(MARGIN, y, colW);
        drawCompactPartsHeader(MARGIN + colW + gap, y, colW);
        int startY = y + 24;
        int item = 1;
        int visibleIndex = 0;

        for (OrdenTrabajo.PiezaUsada pieza : piezas) {
            if (pieza == null) continue;
            int col = visibleIndex / rowsPerColumn;
            int row = visibleIndex % rowsPerColumn;
            int x = col == 0 ? MARGIN : MARGIN + colW + gap;
            int rowTop = startY + (row * rowHeight);

            drawText(String.valueOf(item++), x + 7, rowTop + 14, rowFont, TEXT, false);
            drawWrappedText(pieza.getNombre(), x + 25, rowTop + 10, colW - 126, rowFont, TEXT, false, rowFont + 2, 2);
            drawTextRight(pieza.getCantidad() + " x " + money(pieza.getPrecioUnitario()), x + colW - 54, rowTop + 14, rowFont, MUTED, false);
            drawTextRight("S/ " + money(pieza.getSubtotal()), x + colW - 6, rowTop + 14, rowFont, TEXT, true);
            drawLine(x, rowTop + rowHeight - 2, x + colW, Color.rgb(232, 232, 232));
            visibleIndex++;
        }

        y = startY + (rowsPerColumn * rowHeight) + 8;
    }

    private void drawTotals(OrdenTrabajo orden) {
        y = Math.max(y + 6, TOTAL_TOP);
        int left = PAGE_WIDTH - MARGIN - 210;
        paint.setColor(LIGHT);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(left, y, PAGE_WIDTH - MARGIN, y + 70), 12, 12, paint);

        drawAmountLine("Mano de obra", orden.getMontoManoObra(), left + 14, y + 19, false);
        drawAmountLine("Piezas", orden.getTotalPiezas(), left + 14, y + 38, false);
        drawLine(left + 14, y + 47, PAGE_WIDTH - MARGIN - 14, Color.rgb(215, 215, 215));
        drawAmountLine("TOTAL", orden.getMontoTotal(), left + 14, y + 62, true);
        y += 78;
    }

    private void drawFooter(EmpresaConfig empresa) {
        drawLine(MARGIN, FOOTER_TOP, PAGE_WIDTH - MARGIN, RED);
        drawWrappedText(empresa.getNotaPdf(), MARGIN, FOOTER_TOP + 15, PAGE_WIDTH - (MARGIN * 2), 8, MUTED, false, 10, 2);
        drawText(empresa.getDireccion(), MARGIN, PAGE_HEIGHT - 35, 8, MUTED, false);
        drawText(empresa.getCorreo() + "  |  Tel: " + empresa.getTelefono(), MARGIN, PAGE_HEIGHT - 22, 8, MUTED, false);
    }

    private void drawInfoGrid(String labelA, String valueA, String labelB, String valueB) {
        int colW = (PAGE_WIDTH - (MARGIN * 2) - 12) / 2;
        drawInfoBox(MARGIN, y, colW, labelA, valueA);
        drawInfoBox(MARGIN + colW + 12, y, colW, labelB, valueB);
        y += 52;
    }

    private void drawInfoBox(int x, int top, int w, String label, String value) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(LIGHT);
        canvas.drawRoundRect(new RectF(x, top, x + w, top + 42), 10, 10, paint);
        drawText(label, x + 12, top + 15, 7, MUTED, true);
        drawWrappedText(value, x + 12, top + 28, w - 24, 10, TEXT, true, 11, 2);
    }

    private void section(String title) {
        y += 8;
        drawText(title, MARGIN, y + 12, 10, RED, true);
        drawLine(MARGIN, y + 20, PAGE_WIDTH - MARGIN, Color.rgb(225, 225, 225));
        y += 30;
    }

    private void drawCompactPartsHeader(int x, int top, int width) {
        paint.setColor(DARK);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRoundRect(new RectF(x, top, x + width, top + 20), 6, 6, paint);
        drawText("#", x + 7, top + 14, 7, Color.WHITE, true);
        drawText("PIEZA", x + 25, top + 14, 7, Color.WHITE, true);
        drawText("CANT. x P.U.", x + width - 88, top + 14, 7, Color.WHITE, true);
        drawText("IMP.", x + width - 40, top + 14, 7, Color.WHITE, true);
    }

    private void drawAmountLine(String label, double amount, int x, int baseline, boolean total) {
        drawText(label, x, baseline, total ? 12 : 9, total ? RED : MUTED, true);
        drawText("S/ " + money(amount), PAGE_WIDTH - MARGIN - 84, baseline, total ? 12 : 10, total ? RED : TEXT, true);
    }

    private int drawWrappedText(String text, int x, int baseline, int maxWidth, int textSize, int color, boolean bold, int lineHeight) {
        return drawWrappedText(text, x, baseline, maxWidth, textSize, color, bold, lineHeight, Integer.MAX_VALUE);
    }

    private int drawWrappedText(String text, int x, int baseline, int maxWidth, int textSize, int color, boolean bold, int lineHeight, int maxLines) {
        String value = text != null ? text.trim() : "";
        if (value.isEmpty()) return 0;

        setupPaint(textSize, color, bold);
        int lines = 0;
        String[] paragraphs = value.split("\\r?\\n");
        for (String paragraph : paragraphs) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split("\\s+")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (paint.measureText(candidate) <= maxWidth) {
                    line = new StringBuilder(candidate);
                } else {
                    if (!drawWrappedLine(line.toString(), x, baseline, lineHeight, lines, maxLines)) {
                        return Math.max(lineHeight, lines * lineHeight);
                    }
                    lines++;
                    line = new StringBuilder(word);
                }
            }
            if (line.length() > 0) {
                if (!drawWrappedLine(line.toString(), x, baseline, lineHeight, lines, maxLines)) {
                    return Math.max(lineHeight, lines * lineHeight);
                }
                lines++;
            }
        }
        return Math.max(lineHeight, lines * lineHeight);
    }

    private boolean drawWrappedLine(String line, int x, int baseline, int lineHeight, int lineIndex, int maxLines) {
        if (lineIndex >= maxLines) return false;
        canvas.drawText(line, x, baseline + (lineIndex * lineHeight), paint);
        return true;
    }

    private void drawText(String text, int x, int baseline, int textSize, int color, boolean bold) {
        setupPaint(textSize, color, bold);
        canvas.drawText(text != null ? text : "", x, baseline, paint);
    }

    private void drawTextRight(String text, int right, int baseline, int textSize, int color, boolean bold) {
        String value = text != null ? text : "";
        setupPaint(textSize, color, bold);
        canvas.drawText(value, right - paint.measureText(value), baseline, paint);
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

    private Uri writeToDownloads(String fileName) throws Exception {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Mancuria");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("No se pudo crear el archivo PDF");
            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) throw new IllegalStateException("No se pudo abrir el archivo PDF");
                document.writeTo(os);
            }
            ContentValues published = new ContentValues();
            published.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(uri, published, null, null);
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
