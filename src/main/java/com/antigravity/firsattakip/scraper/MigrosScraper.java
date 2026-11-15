package com.antigravity.firsattakip.scraper;

import com.antigravity.firsattakip.model.ProductDeal;
import com.antigravity.firsattakip.model.ReportSection;
import com.antigravity.firsattakip.util.DealFilterAndGrouper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Migros ('m') verilerini REST API üzerinden tarar:
 * 1. Tüm Kampanyalar Özeti (/kampanyalar)
 * 2. Çoklu İndirim Kampanyaları ve Hediyeli Ürünler (ilk 10 sayfa)
 * 3. Money İndirimli Market Ürünleri (ilk 10 sayfa)
 */
public class MigrosScraper {

    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private static final Pattern PAT_X_AL_Y_ODE =
            Pattern.compile("(\\d+)\\s*AL\\s*(\\d+)\\s*[ÖO]DE", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_SECOND_DISC =
            Pattern.compile("2\\.?\\s*S[İI]\\s*%?\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_MULTI_CRM =
            Pattern.compile("(\\d+)\\s*['’]?l[iıuü]\\s+al[ıi]mda\\s+(?:adet\\s+)?([\\d\\.,]+)\\s*TL",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern PAT_OVER_CRM =
            Pattern.compile("([\\d\\.,]+)\\s*TL\\s+ve\\s+[üu]zeri.*?([\\d\\.,]+)\\s*TL",
                    Pattern.CASE_INSENSITIVE);

    private final OkHttpClient client;

    public MigrosScraper(OkHttpClient client) {
        this.client = client;
    }

    public interface ProgressCallback {
        void onProgress(String message);
    }

    public List<ReportSection> scrapeAll(ProgressCallback cb) {
        List<ReportSection> sections = new ArrayList<>();

        try {
            if (cb != null) cb.onProgress("Migros: Kampanyalar listeleniyor...");
            sections.add(scrapeCampaignSummary());
        } catch (Exception e) {
            android.util.Log.w("MigrosScraper", "Kampanya özeti hatası: " + e);
        }

        try {
            if (cb != null) cb.onProgress("Migros: Çoklu indirim kampanyaları (10 sayfa) taranıyor...");
            sections.add(scrapeScreen(
                    "🎁 2. Migros Çoklu İndirim Kampanyaları ve Hediyeli Ürünler",
                    "https://www.migros.com.tr/coklu-indirim-kampanyalari-ve-hediyeli-urunler-dt-1?sirala=cok-satanlar",
                    "coklu-indirim-kampanyalari-ve-hediyeli-urunler-dt-1?sirala=cok-satanlar",
                    10
            ));
        } catch (Exception e) {
            android.util.Log.w("MigrosScraper", "Çoklu indirim hatası: " + e);
        }

        try {
            if (cb != null) cb.onProgress("Migros: Money indirimli ürünler (10 sayfa) taranıyor...");
            sections.add(scrapeScreen(
                    "💳 3. Migros Money İndirimli Market Ürünleri (İlk 10 Sayfa)",
                    "https://www.migros.com.tr/money-indirimli-market-urunleri-dt-1",
                    "money-indirimli-market-urunleri-dt-5",
                    10
            ));
        } catch (Exception e) {
            android.util.Log.w("MigrosScraper", "Money indirim hatası: " + e);
        }

        return sections;
    }

    private ReportSection scrapeCampaignSummary() {
        Set<String> titles = new LinkedHashSet<>();
        try {
            String url = "https://www.migros.com.tr/rest/shopping-lists/placeholder/CAMPAIGN_LIST";
            String body = fetchJson(url);
            if (body != null && !body.trim().isEmpty()) {
                JsonElement parsed = JsonParser.parseString(body);
                if (parsed.isJsonObject()) {
                    JsonObject root = parsed.getAsJsonObject();
                    if (root.has("data") && root.get("data").isJsonArray()) {
                        for (JsonElement el : root.getAsJsonArray("data")) {
                            if (!el.isJsonObject()) continue;
                            JsonObject obj = el.getAsJsonObject();
                            if (obj.has("name") && !obj.get("name").isJsonNull()) {
                                String name = obj.get("name").getAsString().trim();
                                if (!name.isEmpty() && !"analiz sonuçları".equalsIgnoreCase(name)) {
                                    titles.add(name);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            android.util.Log.w("MigrosScraper", "scrapeCampaignSummary parse error: " + ex);
        }

        ReportSection sec = new ReportSection(
                "🛒 1. Migros Tüm Kampanyalar Özeti",
                "https://www.migros.com.tr/kampanyalar",
                "Toplam " + titles.size() + " aktif kampanya"
        );
        sec.bulletItems.addAll(titles);
        return sec;
    }

    private ReportSection scrapeScreen(
            String sectionTitle,
            String displayUrl,
            String screenPath,
            int maxPages
    ) {
        List<ProductDeal> matched = new ArrayList<>();
        int totalScanned = 0;

        for (int pg = 1; pg <= maxPages; pg++) {
            try {
                String sep = screenPath.contains("?") ? "&" : "?";
                String apiUrl = "https://www.migros.com.tr/rest/search/screens/" + screenPath + sep + "sayfa=" + pg;
                String body = fetchJson(apiUrl);
                if (body == null || body.trim().isEmpty()) break;

                JsonElement parsed = JsonParser.parseString(body);
                if (!parsed.isJsonObject()) break;

                JsonObject root = parsed.getAsJsonObject();
                if (!root.has("successful") || !root.get("successful").isJsonPrimitive()
                        || !root.get("successful").getAsBoolean()) break;

                if (!root.has("data") || !root.get("data").isJsonObject()) break;
                JsonObject data = root.getAsJsonObject("data");
                if (data == null || !data.has("searchInfo") || !data.get("searchInfo").isJsonObject()) break;

                JsonObject searchInfo = data.getAsJsonObject("searchInfo");
                int pageCount = searchInfo.has("pageCount") && !searchInfo.get("pageCount").isJsonNull()
                        ? searchInfo.get("pageCount").getAsInt() : 1;

                if (!searchInfo.has("storeProductInfos") || !searchInfo.get("storeProductInfos").isJsonArray()) break;
                JsonArray prods = searchInfo.getAsJsonArray("storeProductInfos");
                if (prods == null || prods.size() == 0) break;

                totalScanned += prods.size();
                for (JsonElement el : prods) {
                    if (!el.isJsonObject()) continue;
                    try {
                        ProductDeal deal = parseProduct(el.getAsJsonObject());
                        if (deal != null) {
                            matched.add(deal);
                        }
                    } catch (Exception ignored) {}
                }

                if (pg >= pageCount) break;
            } catch (Exception ex) {
                android.util.Log.w("MigrosScraper", "Sayfa ayrıştırma hatası (" + pg + "): " + ex);
                break;
            }
        }

        String subtitle;
        if (totalScanned > 0) {
            subtitle = "İlk " + maxPages + " sayfada " + totalScanned + " ürün tarandı, kriterlere uyan "
                    + matched.size() + " ürün bulundu";
        } else {
            subtitle = "Migros sunucusundan bu kategori için veri alınamadı.";
        }

        ReportSection sec = new ReportSection(
                sectionTitle,
                displayUrl,
                subtitle
        );
        sec.tableRows.addAll(DealFilterAndGrouper.groupDeals(matched));
        return sec;
    }

    private ProductDeal parseProduct(JsonObject pr) {
        String name = pr.has("name") && !pr.get("name").isJsonNull()
                ? pr.get("name").getAsString().trim() : "";
        if (name.isEmpty()) return null;

        String brand = "";
        if (pr.has("brand") && pr.get("brand").isJsonObject()) {
            JsonObject bObj = pr.getAsJsonObject("brand");
            if (bObj.has("name") && !bObj.get("name").isJsonNull()) {
                brand = bObj.get("name").getAsString().trim();
            }
        }
        if (brand.isEmpty()) {
            brand = DealFilterAndGrouper.extractBrandFromName(name, null);
        }

        long regKurus = pr.has("regularPrice") && !pr.get("regularPrice").isJsonNull()
                ? pr.get("regularPrice").getAsLong() : 0L;
        if (regKurus <= 0) return null;

        long shownKurus = pr.has("shownPrice") && !pr.get("shownPrice").isJsonNull()
                ? pr.get("shownPrice").getAsLong() : regKurus;

        double origPrice = regKurus / 100.0;
        double discPrice = shownKurus / 100.0;
        double discRate = pr.has("discountRate") && !pr.get("discountRate").isJsonNull()
                ? pr.get("discountRate").getAsDouble() : 0.0;

        if (origPrice > discPrice && discRate == 0.0) {
            discRate = ((origPrice - discPrice) / origPrice) * 100.0;
        }

        String promoNote = "";

        if (pr.has("badges") && pr.get("badges").isJsonArray()) {
            for (JsonElement bEl : pr.getAsJsonArray("badges")) {
                if (!bEl.isJsonObject()) continue;
                JsonObject b = bEl.getAsJsonObject();
                String bName = b.has("name") && !b.get("name").isJsonNull()
                        ? b.get("name").getAsString() : "";
                String bVal = b.has("value") && !b.get("value").isJsonNull()
                        ? b.get("value").getAsString().trim() : "";
                if ("CROSS_PROMOTED".equals(bName) && !bVal.isEmpty()) {
                    String valUpper = bVal.toUpperCase(new Locale("tr", "TR"));
                    Matcher mXy = PAT_X_AL_Y_ODE.matcher(valUpper);
                    if (mXy.find()) {
                        int xBuy = Integer.parseInt(mXy.group(1));
                        int yPay = Integer.parseInt(mXy.group(2));
                        if (xBuy > yPay && yPay > 0) {
                            double effRate = ((double) (xBuy - yPay) / xBuy) * 100.0;
                            if (effRate > discRate) {
                                discRate = effRate;
                                discPrice = origPrice * ((double) yPay / xBuy);
                                promoNote = bVal;
                            }
                        }
                    }
                    Matcher mSec = PAT_SECOND_DISC.matcher(valUpper);
                    if (mSec.find()) {
                        double secDisc = Double.parseDouble(mSec.group(1));
                        double effRate = secDisc / 2.0;
                        if (effRate > discRate) {
                            discRate = effRate;
                            discPrice = origPrice * (1.0 - effRate / 100.0);
                            promoNote = bVal;
                        }
                    }
                }
            }
        }

        if (pr.has("crmDiscountTags") && pr.get("crmDiscountTags").isJsonArray()) {
            for (JsonElement cEl : pr.getAsJsonArray("crmDiscountTags")) {
                if (!cEl.isJsonObject()) continue;
                JsonObject cTag = cEl.getAsJsonObject();
                String tagText = cTag.has("tag") && !cTag.get("tag").isJsonNull()
                        ? cTag.get("tag").getAsString().trim() : "";
                if (tagText.isEmpty()) continue;

                Matcher mMulti = PAT_MULTI_CRM.matcher(tagText);
                if (mMulti.find()) {
                    int qty = Integer.parseInt(mMulti.group(1));
                    Double targetP = DealFilterAndGrouper.parseTl(mMulti.group(2));
                    if (targetP != null && targetP > 0) {
                        double unitP = targetP < origPrice ? targetP : (targetP / qty);
                        if (unitP < discPrice) {
                            discPrice = unitP;
                            discRate = ((origPrice - discPrice) / origPrice) * 100.0;
                            promoNote = tagText;
                        }
                    }
                } else {
                    Matcher mOver = PAT_OVER_CRM.matcher(tagText);
                    if (mOver.find()) {
                        Double promoP = DealFilterAndGrouper.parseTl(mOver.group(2));
                        if (promoP != null && promoP > 0 && promoP < discPrice) {
                            discPrice = promoP;
                            discRate = ((origPrice - discPrice) / origPrice) * 100.0;
                            promoNote = tagText;
                        }
                    }
                }
            }
        }

        if (promoNote.isEmpty() && origPrice > discPrice) {
            promoNote = "Money İndirimi";
        }

        if (DealFilterAndGrouper.passesFilter(origPrice, discRate)) {
            return new ProductDeal(brand, name, origPrice, discPrice, discRate, promoNote);
        }
        return null;
    }

    private String fetchJson(String url) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept", "application/json")
                .build();
        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                return null;
            }
            return resp.body().string();
        }
    }
}
