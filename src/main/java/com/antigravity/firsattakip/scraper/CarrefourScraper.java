package com.antigravity.firsattakip.scraper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.antigravity.firsattakip.model.ProductDeal;
import com.antigravity.firsattakip.model.ReportSection;
import com.antigravity.firsattakip.util.DealFilterAndGrouper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CarrefourSA ('c') Fırsatlar sayfasını (ilk 15 sayfa) Android'in yerleşik WebView
 * tarayıcı motoru üzerinden tarar (Cloudflare korumasını gerçek tarayıcı oturumu ile aşar).
 */
public class CarrefourScraper {

    private static final String BASE_URL =
            "https://www.carrefoursa.com/firsatlar/c/9002?q=%3AbestSeller";
    private static final String UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36";

    private static final Pattern PAT_X_AL_Y_ODE =
            Pattern.compile("(\\d+)\\s*AL\\s*(\\d+)\\s*[ÖO]DE", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_PAYFOUR =
            Pattern.compile("PAYFOUR.*?([\\d\\.,]+)", Pattern.CASE_INSENSITIVE);

    private final Context appContext;

    public CarrefourScraper(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public interface ProgressCallback {
        void onProgress(String message);
    }

    public ReportSection scrape(int maxPages, ProgressCallback cb) {
        if (cb != null) cb.onProgress("CarrefourSA: Tarayıcı oturumu hazırlanıyor...");

        CompletableFuture<List<String>> futurePages = new CompletableFuture<>();
        Handler mainHandler = new Handler(Looper.getMainLooper());

        mainHandler.post(() -> startWebViewFetch(maxPages, cb, futurePages, mainHandler));

        List<String> htmlPages;
        try {
            htmlPages = futurePages.get(45, TimeUnit.SECONDS);
        } catch (Exception ex) {
            android.util.Log.w("CarrefourScraper", "CarrefourSA zaman aşımı veya hata: " + ex);
            htmlPages = new ArrayList<>();
        }

        List<ProductDeal> matched = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();
        int totalScanned = 0;

        if (htmlPages != null) {
            for (String pageHtml : htmlPages) {
                if (pageHtml == null || pageHtml.isEmpty()) continue;
                Document doc = Jsoup.parse(pageHtml);
                Elements cards = doc.select("ul.product-listing li.product-listing-item");
                if (cards.isEmpty()) {
                    cards = doc.select("li.product-listing-item");
                }
                for (Element card : cards) {
                    Element dl = card.selectFirst("div.dataLayerItemData");
                    String cId = "";
                    if (dl != null) {
                        cId = dl.attr("data-item_id").trim();
                        if (cId.isEmpty()) cId = dl.attr("data-item_name").trim();
                    }
                    if (cId.isEmpty()) {
                        Element h3 = card.selectFirst("h3.item-name");
                        if (h3 != null) cId = h3.text().trim();
                    }
                    if (cId.isEmpty() || seenIds.contains(cId)) continue;
                    seenIds.add(cId);
                    totalScanned++;

                    ProductDeal deal = parseCard(card, dl);
                    if (deal != null) {
                        matched.add(deal);
                    }
                }
            }
        }

        String subtitle;
        if (totalScanned > 0) {
            subtitle = "İlk " + maxPages + " sayfada " + totalScanned + " ürün tarandı, kriterlere uyan "
                    + matched.size() + " ürün bulundu";
        } else {
            subtitle = "CarrefourSA güvenlik koruması (Cloudflare/Bot koruması) veya ağ nedeniyle yanıt vermedi.";
        }

        ReportSection sec = new ReportSection(
                "🏪 CarrefourSA Fırsatlar (İlk " + maxPages + " Sayfa)",
                BASE_URL,
                subtitle
        );
        sec.tableRows.addAll(DealFilterAndGrouper.groupDeals(matched));
        return sec;
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void startWebViewFetch(
            int maxPages,
            ProgressCallback cb,
            CompletableFuture<List<String>> futurePages,
            Handler mainHandler
    ) {
        WebView webView = new WebView(appContext);
        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setDatabaseEnabled(true);
        ws.setUserAgentString(UA);

        webView.onResume();
        webView.resumeTimers();

        final List<String> collected = new ArrayList<>();
        final boolean[] started = {false};

        class JsBridge {
            @JavascriptInterface
            public void onPageHtml(int pg, String html) {
                if (html != null && !html.isEmpty()) {
                    collected.add(html);
                }
                if (cb != null) {
                    cb.onProgress("CarrefourSA: Sayfa " + pg + "/" + maxPages + " alındı");
                }
            }

            @JavascriptInterface
            public void onAllDone() {
                mainHandler.post(() -> {
                    try { webView.destroy(); } catch (Exception ignored) {}
                });
                if (!futurePages.isDone()) {
                    futurePages.complete(collected);
                }
            }
        }

        webView.addJavascriptInterface(new JsBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (started[0]) return;
                started[0] = true;

                // İlk sayfanın HTML'ini al, sonra 2..maxPages sayfalarını aynı oturum içinde fetch ile çek
                String js = "(async function() {"
                        + "  try {"
                        + "    AndroidBridge.onPageHtml(1, document.documentElement.outerHTML);"
                        + "    for (let pg = 2; pg <= " + maxPages + "; pg++) {"
                        + "      const r = await fetch('/firsatlar/c/9002?q=%3AbestSeller&page=' + pg + '&isScroll=true', {"
                        + "        headers: {'X-Requested-With': 'XMLHttpRequest'}"
                        + "      });"
                        + "      if (!r.ok) break;"
                        + "      const txt = await r.text();"
                        + "      if (!txt || !txt.includes('product-listing-item')) break;"
                        + "      AndroidBridge.onPageHtml(pg, txt);"
                        + "    }"
                        + "  } catch (e) {}"
                        + "  AndroidBridge.onAllDone();"
                        + "})();";

                mainHandler.postDelayed(() -> {
                    try {
                        view.evaluateJavascript(js, null);
                    } catch (Exception e) {
                        if (!futurePages.isDone()) futurePages.complete(collected);
                    }
                }, 2500);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                android.util.Log.w("CarrefourScraper", "WebView error: " + description);
            }
        });

        // Emniyet zaman aşımı
        mainHandler.postDelayed(() -> {
            if (!futurePages.isDone()) {
                mainHandler.post(() -> {
                    try { webView.destroy(); } catch (Exception ignored) {}
                });
                futurePages.complete(collected);
            }
        }, 40000);

        webView.loadUrl(BASE_URL);
    }

    private ProductDeal parseCard(Element card, Element dl) {
        String name = "";
        String brand = "";
        Double origPrice = null;
        Double discPrice = null;
        String campType = "";

        if (dl != null) {
            name = dl.attr("data-item_name").trim();
            brand = dl.attr("data-item_brand").trim();
            origPrice = DealFilterAndGrouper.parseTl(dl.attr("data-first_price"));
            discPrice = DealFilterAndGrouper.parseTl(dl.attr("data-price"));
            campType = dl.attr("data-campaign_type").trim();
        }

        if (name.isEmpty()) {
            Element h3 = card.selectFirst("h3.item-name");
            if (h3 != null) name = h3.text().trim();
        }
        if (name.isEmpty()) return null;

        if (origPrice == null) {
            Element pOld = card.selectFirst(".priceLineThrough");
            if (pOld != null) origPrice = DealFilterAndGrouper.parseTl(pOld.text());
        }
        if (discPrice == null) {
            Element pNew = card.selectFirst(".item-price");
            if (pNew != null) {
                String c = pNew.attr("content");
                discPrice = DealFilterAndGrouper.parseTl(c.isEmpty() ? pNew.text() : c);
            }
        }

        if (origPrice == null && discPrice != null) origPrice = discPrice;
        if (discPrice == null && origPrice != null) discPrice = origPrice;
        if (origPrice == null || origPrice <= 0) return null;

        double discRate = 0.0;
        if (origPrice > discPrice) {
            discRate = ((origPrice - discPrice) / origPrice) * 100.0;
        }

        String promoNote = "";
        if (campType.isEmpty()) {
            Element cb = card.selectFirst(".camp-badge");
            if (cb != null) campType = cb.text().trim();
        }

        if (!campType.isEmpty()) {
            String upper = campType.toUpperCase(new Locale("tr", "TR"));
            Matcher mXy = PAT_X_AL_Y_ODE.matcher(upper);
            if (mXy.find()) {
                int xBuy = Integer.parseInt(mXy.group(1));
                int yPay = Integer.parseInt(mXy.group(2));
                if (xBuy > yPay && yPay > 0) {
                    double effRate = ((double) (xBuy - yPay) / xBuy) * 100.0;
                    if (effRate > discRate) {
                        discRate = effRate;
                        discPrice = origPrice * ((double) yPay / xBuy);
                        promoNote = campType;
                    }
                }
            }
            Matcher mPf = PAT_PAYFOUR.matcher(campType);
            if (mPf.find()) {
                Double pfPrice = DealFilterAndGrouper.parseTl(mPf.group(1));
                if (pfPrice != null && pfPrice > 0 && pfPrice < discPrice) {
                    double pfRate = ((origPrice - pfPrice) / origPrice) * 100.0;
                    if (pfRate > discRate) {
                        discPrice = pfPrice;
                        discRate = pfRate;
                        promoNote = campType;
                    }
                }
            }
        }

        if (brand.isEmpty()
                || brand.equalsIgnoreCase("MARKASIZ")
                || brand.toUpperCase(Locale.ROOT).startsWith("BRN-")) {
            brand = DealFilterAndGrouper.extractBrandFromName(name, null);
        }

        if (DealFilterAndGrouper.passesFilter(origPrice, discRate)) {
            return new ProductDeal(brand, name, origPrice, discPrice, discRate, promoNote);
        }
        return null;
    }
}
