package com.example.myapplication.features.contract;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.graphics.Canvas;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.myapplication.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ContractPdfPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_HTML = "EXTRA_HTML";
    public static final String EXTRA_FILE_NAME = "EXTRA_FILE_NAME";
    public static final String EXTRA_SHARE_SUBJECT = "EXTRA_SHARE_SUBJECT";
    public static final String EXTRA_SHARE_TEXT = "EXTRA_SHARE_TEXT";
    public static final String EXTRA_AUTO_SHARE = "EXTRA_AUTO_SHARE";

    private WebView webView;
    private String htmlContent;
    private String fileName;
    private String shareSubject;
    private String shareText;
    private boolean autoShare;
    private boolean autoShareTriggered;
    private boolean pageReady;
    private File generatedPdfFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.parseColor("#1976D2"));
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(false);
        }

        setContentView(R.layout.activity_contract_pdf_preview);

        webView = findViewById(R.id.webViewPreview);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        View actionPrint = findViewById(R.id.actionPrint);
        View actionShare = findViewById(R.id.actionShare);

        Intent intent = getIntent();
        htmlContent = intent != null ? intent.getStringExtra(EXTRA_HTML) : "";
        fileName = intent != null ? intent.getStringExtra(EXTRA_FILE_NAME) : null;
        shareSubject = intent != null ? intent.getStringExtra(EXTRA_SHARE_SUBJECT) : null;
        shareText = intent != null ? intent.getStringExtra(EXTRA_SHARE_TEXT) : null;
        autoShare = intent != null && intent.getBooleanExtra(EXTRA_AUTO_SHARE, false);

        fileName = normalizePdfFileName(fileName);
        toolbar.setTitle(fileName);
        toolbar.setNavigationOnClickListener(v -> finish());

        final int baseToolbarHeight = (int) (56 * getResources().getDisplayMetrics().density);
        final int baseToolbarTopPadding = toolbar.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), baseToolbarTopPadding + systemBars.top, v.getPaddingRight(),
                    v.getPaddingBottom());
            android.view.ViewGroup.LayoutParams lp = v.getLayoutParams();
            lp.height = baseToolbarHeight + systemBars.top;
            v.setLayoutParams(lp);
            return insets;
        });

        WebSettings settings = webView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageReady = true;
                if (autoShare && !autoShareTriggered) {
                    autoShareTriggered = true;
                    shareCurrentDocument();
                }
            }
        });

        webView.loadDataWithBaseURL(null, nullToEmpty(htmlContent), "text/HTML", "UTF-8", null);

        actionPrint.setOnClickListener(v -> printCurrentDocument());
        actionShare.setOnClickListener(v -> shareCurrentDocument());
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }

    private void printCurrentDocument() {
        ensurePdfFileReady(new PdfReadyCallback() {
            @Override
            public void onReady(@NonNull File pdfFile) {
                openPdfFile(pdfFile);
            }

            @Override
            public void onError(@NonNull String message) {
                Toast.makeText(ContractPdfPreviewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void shareCurrentDocument() {
        ensurePdfFileReady(new PdfReadyCallback() {
            @Override
            public void onReady(@NonNull File pdfFile) {
                sharePdfFile(pdfFile);
            }

            @Override
            public void onError(@NonNull String message) {
                Toast.makeText(ContractPdfPreviewActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ensurePdfFileReady(@NonNull PdfReadyCallback callback) {
        if (generatedPdfFile != null && generatedPdfFile.exists()) {
            callback.onReady(generatedPdfFile);
            return;
        }
        if (!pageReady) {
            callback.onError("Vui lòng đợi tải nội dung xong rồi thử lại");
            return;
        }

        File outDir = new File(getCacheDir(), "contracts");
        if (!outDir.exists() && !outDir.mkdirs()) {
            callback.onError("Không thể tạo thư mục PDF");
            return;
        }

        File outFile = new File(outDir, fileName);
        if (!writePdfFromWebViewToFile(outFile)) {
            callback.onError("Xuất PDF thất bại");
            return;
        }

        generatedPdfFile = outFile;
        callback.onReady(outFile);
    }

    private void openPdfFile(@NonNull File pdfFile) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(buildPdfUri(pdfFile), "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Mở PDF để in"));
        } catch (Exception e) {
            Toast.makeText(this, "Không tìm thấy ứng dụng mở PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePdfFile(@NonNull File pdfFile) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, buildPdfUri(pdfFile));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, nullToEmpty(shareSubject));
            if (shareText != null && !shareText.trim().isEmpty()) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ hợp đồng PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Không thể chia sẻ file PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private android.net.Uri buildPdfUri(@NonNull File pdfFile) {
        return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdfFile);
    }

    @NonNull
    private String normalizePdfFileName(String input) {
        String raw = input == null ? "" : input.trim();
        if (raw.isEmpty()) {
            return "HopDong_hd001.pdf";
        }
        String safe = raw.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (!safe.toLowerCase().endsWith(".pdf")) {
            safe = safe + ".pdf";
        }
        return safe;
    }

    private boolean writePdfFromWebViewToFile(@NonNull File outFile) {
        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        FileOutputStream fos = null;
        try {
            final int pageWidth = 1240;
            final int pageHeight = 1754;
            final int margin = 44;
            final int printableWidth = pageWidth - (margin * 2);
            final int printableHeight = pageHeight - (margin * 2);

            int oldLeft = webView.getLeft();
            int oldTop = webView.getTop();
            int oldRight = webView.getRight();
            int oldBottom = webView.getBottom();

            int widthSpec = View.MeasureSpec.makeMeasureSpec(printableWidth, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            webView.measure(widthSpec, heightSpec);

            int contentHeight = webView.getMeasuredHeight();
            if (contentHeight <= 0) {
                return false;
            }

            webView.layout(0, 0, printableWidth, contentHeight);

            int pageNo = 1;
            for (int yOffset = 0; yOffset < contentHeight; yOffset += printableHeight) {
                android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(
                        pageWidth, pageHeight, pageNo).create();
                android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                canvas.drawColor(android.graphics.Color.WHITE);
                canvas.save();
                canvas.translate(margin, margin - yOffset);
                webView.draw(canvas);
                canvas.restore();

                document.finishPage(page);
                pageNo++;
            }

            webView.layout(oldLeft, oldTop, oldRight, oldBottom);

            fos = new FileOutputStream(outFile);
            document.writeTo(fos);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            document.close();
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private interface PdfReadyCallback {
        void onReady(@NonNull File pdfFile);

        void onError(@NonNull String message);
    }

    @NonNull
    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
