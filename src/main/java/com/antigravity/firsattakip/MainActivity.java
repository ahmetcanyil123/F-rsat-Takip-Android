package com.antigravity.firsattakip;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.antigravity.firsattakip.export.ReportExporter;
import com.antigravity.firsattakip.model.ReportSection;
import com.antigravity.firsattakip.scraper.CagriScraper;
import com.antigravity.firsattakip.scraper.CarrefourScraper;
import com.antigravity.firsattakip.scraper.MigrosScraper;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Ana ekran: 'm' (Migros), 'ç' (Çağrı), 'c' (CarrefourSA) veya 't' (Tümü)
 * seçimini alır, taramayı yürütür ve tarihli Word (.docx) + PDF (.pdf) üretir.
 */
public class MainActivity extends AppCompatActivity {

    private EditText etLetter;
    private MaterialButton btnRunLetter, btnMigros, btnCagri, btnCarrefour, btnAll;
    private MaterialButton btnOpenPdf, btnOpenDocx, btnShareFiles;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private WebView webPreview;
    private LinearLayout layoutExportActions;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    private ReportExporter.ExportResult lastExport;
    private volatile boolean isRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etLetter = findViewById(R.id.etLetter);
        btnRunLetter = findViewById(R.id.btnRunLetter);
        btnMigros = findViewById(R.id.btnMigros);
        btnCagri = findViewById(R.id.btnCagri);
        btnCarrefour = findViewById(R.id.btnCarrefour);
        btnAll = findViewById(R.id.btnAll);
        btnOpenPdf = findViewById(R.id.btnOpenPdf);
        btnOpenDocx = findViewById(R.id.btnOpenDocx);
        btnShareFiles = findViewById(R.id.btnShareFiles);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        webPreview = findViewById(R.id.webPreview);
        layoutExportActions = findViewById(R.id.layoutExportActions);

        webPreview.setBackgroundColor(0xFF0F172A);
        webPreview.getSettings().setJavaScriptEnabled(true);
        webPreview.getSettings().setDomStorageEnabled(true);
        webPreview.loadDataWithBaseURL(
                null,
                "<html><body style='background:#0f172a;color:#94a3b8;font-family:sans-serif;padding:16px;'>"
                        + "Yukarıdan <b>m</b>, <b>ç</b>, <b>c</b> veya <b>t</b> seçerek taramayı başlatın."
                        + "</body></html>",
                "text/html",
                "utf-8",
                null
        );

        btnMigros.setOnClickListener(v -> triggerScan("m"));
        btnCagri.setOnClickListener(v -> triggerScan("ç"));
        btnCarrefour.setOnClickListener(v -> triggerScan("c"));
        btnAll.setOnClickListener(v -> triggerScan("t"));

        btnRunLetter.setOnClickListener(v -> triggerFromInput());
        etLetter.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                triggerFromInput();
                return true;
            }
            return false;
        });

        btnOpenPdf.setOnClickListener(v -> {
            if (lastExport != null) openFile(lastExport.pdfFile, "application/pdf");
        });
        btnOpenDocx.setOnClickListener(v -> {
            if (lastExport != null) {
                openFile(lastExport.docxFile,
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            }
        });
        btnShareFiles.setOnClickListener(v -> shareBothFiles());
    }

    private void triggerFromInput() {
        String raw = etLetter.getText().toString().trim().toLowerCase(new Locale("tr", "TR"));
        if (raw.equals("m") || raw.equals("migros")) {
            triggerScan("m");
        } else if (raw.equals("ç") || raw.equals("cagri") || raw.equals("çağrı")) {
            triggerScan("ç");
        } else if (raw.equals("c") || raw.equals("carrefour") || raw.equals("carrefoursa")) {
            triggerScan("c");
        } else if (raw.equals("t") || raw.equals("tümü") || raw.equals("tumu")) {
            triggerScan("t");
        } else {
            Toast.makeText(this, "Geçerli bir harf girin: m, ç, c veya t", Toast.LENGTH_SHORT).show();
        }
    }

    private void triggerScan(String code) {
        if (isRunning) {
            Toast.makeText(this, "Tarama zaten devam ediyor...", Toast.LENGTH_SHORT).show();
            return;
        }
        isRunning = true;
        setButtonsEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        layoutExportActions.setVisibility(View.GONE);
        updateStatus("Başlatılıyor [" + code + "]...");

        executor.execute(() -> {
            try {
                List<ReportSection> sections = new ArrayList<>();
                String title;

                if ("m".equals(code)) {
                    title = "🛒 Migros Sanal Market Fırsat Raporu [M]";
                    sections.addAll(new MigrosScraper(httpClient).scrapeAll(this::updateStatus));
                } else if ("ç".equals(code)) {
                    title = "🛍️ Çağrı Market İndirimli Ürünler Raporu [Ç]";
                    sections.add(new CagriScraper(httpClient).scrape(this::updateStatus));
                } else if ("c".equals(code)) {
                    title = "🏪 CarrefourSA Fırsatlar Raporu [C]";
                    sections.add(new CarrefourScraper(this).scrape(15, this::updateStatus));
                } else {
                    title = "🌐 Tüm Marketler Fırsat Raporu [T]";
                    try {
                        sections.addAll(new MigrosScraper(httpClient).scrapeAll(this::updateStatus));
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Migros hatası: " + e.getMessage(), e);
                    }
                    try {
                        sections.add(new CagriScraper(httpClient).scrape(this::updateStatus));
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Çağrı hatası: " + e.getMessage(), e);
                    }
                    try {
                        sections.add(new CarrefourScraper(this).scrape(15, this::updateStatus));
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Carrefour hatası: " + e.getMessage(), e);
                    }
                }

                updateStatus("Word (.docx) ve PDF (.pdf) oluşturuluyor...");
                ReportExporter.ExportResult res = ReportExporter.exportAll(this, code, title, sections);
                lastExport = res;

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setButtonsEnabled(true);
                    isRunning = false;
                    layoutExportActions.setVisibility(View.VISIBLE);
                    tvStatus.setText("Hazır: " + res.pdfFile.getName() + " & " + res.docxFile.getName());
                    webPreview.loadDataWithBaseURL(null, res.htmlContent, "text/html", "utf-8", null);
                });
            } catch (Exception ex) {
                android.util.Log.e("MainActivity", "Tarama hatası", ex);
                String err = ex.getMessage();
                if (err == null || err.trim().isEmpty()) {
                    err = ex.getClass().getSimpleName();
                }
                if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                    err += " (" + ex.getCause().getMessage() + ")";
                }
                String finalErr = err;
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    setButtonsEnabled(true);
                    isRunning = false;
                    tvStatus.setText("Hata: " + finalErr);
                    Toast.makeText(MainActivity.this, "Hata: " + finalErr, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void updateStatus(String msg) {
        runOnUiThread(() -> tvStatus.setText(msg));
    }

    private void setButtonsEnabled(boolean enabled) {
        btnRunLetter.setEnabled(enabled);
        btnMigros.setEnabled(enabled);
        btnCagri.setEnabled(enabled);
        btnCarrefour.setEnabled(enabled);
        btnAll.setEnabled(enabled);
    }

    private void openFile(File file, String mimeType) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Dosyayı Aç"));
        } catch (Exception e) {
            Toast.makeText(this, "Açacak uygulama bulunamadı, Paylaş butonunu kullanabilirsiniz.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void shareBothFiles() {
        if (lastExport == null) return;
        ArrayList<Uri> uris = new ArrayList<>();
        uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", lastExport.pdfFile));
        uris.add(FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", lastExport.docxFile));

        Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);
        share.setType("*/*");
        share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "Word ve PDF Raporunu Paylaş / Kaydet"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
