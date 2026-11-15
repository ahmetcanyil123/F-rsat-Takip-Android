package com.antigravity.firsattakip.scraper;

import com.antigravity.firsattakip.model.ProductDeal;
import com.antigravity.firsattakip.model.ReportSection;
import com.antigravity.firsattakip.util.DealFilterAndGrouper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Çağrı Market ('ç') indirimli ürünler sayfasının ilk 10 sayfasını tarar.
 * Next.js SSR yanıtındaki gömülü ürün JSON verilerini ayıklar.
 */
public class CagriScraper {

    private static final String BASE_URL =
            "https://www.cagri.com/indirimli-urunler?siralama=discount_rate%2Cdesc";
    private static final String UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36";

    private static final Pattern PAT_PRODUCT = Pattern.compile(
            "\"name\":\"([^\"]+)\",\"code\":\"[^\"]+\",\"purchase_price\":[\\d\\.]+,"
                    + "\"selling_price\":([\\d\\.]+),\"discount_price\":([\\d\\.]+),\"discount_rate\":(\\d+)"
    );

    private final OkHttpClient client;

    public CagriScraper(OkHttpClient client) {
        this.client = client;
    }

    public interface ProgressCallback {
        void onProgress(String message);
    }

    public ReportSection scrape(ProgressCallback cb) {
        List<String> dynamicBrands = fetchDynamicBrands();
        List<ProductDeal> matched = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        int totalScanned = 0;

        for (int pg = 1; pg <= 10; pg++) {
            if (cb != null) {
                cb.onProgress("Çağrı Market: Sayfa " + pg + "/10 taranıyor...");
            }
            try {
                String url = (pg == 1) ? BASE_URL : (BASE_URL + "&sayfa=" + pg);
                String html = fetchString(url);
                if (html == null || html.isEmpty()) break;

                // Next.js RSC stringindeki kaçış karakterlerini (\" -> ") çöz
                String unescaped = html.replace("\\\"", "\"");
                Matcher m = PAT_PRODUCT.matcher(unescaped);
                int foundOnPage = 0;

                while (m.find()) {
                    foundOnPage++;
                    String name = decodeUnicodeEscapes(m.group(1)).trim();
                    if (name.isEmpty()) continue;

                    String key = name.toLowerCase(new Locale("tr", "TR"));
                    if (seenNames.contains(key)) continue;
                    seenNames.add(key);
                    totalScanned++;

                    double discPrice = safeDouble(m.group(2));
                    double discountAmount = safeDouble(m.group(3));
                    double discRate = safeDouble(m.group(4));
                    double origPrice = discPrice + discountAmount;

                    if (origPrice <= 0) origPrice = discPrice;
                    if (discRate == 0 && origPrice > discPrice && origPrice > 0) {
                        discRate = ((origPrice - discPrice) / origPrice) * 100.0;
                    }

                    if (DealFilterAndGrouper.passesFilter(origPrice, discRate)) {
                        String brand = DealFilterAndGrouper.extractBrandFromName(name, dynamicBrands);
                        matched.add(new ProductDeal(brand, name, origPrice, discPrice, discRate, ""));
                    }
                }

                if (foundOnPage == 0) break;
            } catch (Exception ex) {
                android.util.Log.w("CagriScraper", "Çağrı sayfa tarama hatası: " + ex);
                break;
            }
        }

        String subtitle;
        if (totalScanned > 0) {
            subtitle = "İlk 10 sayfada " + totalScanned + " ürün tarandı, kriterlere uyan "
                    + matched.size() + " ürün bulundu";
        } else {
            subtitle = "Çağrı Market sunucusuna ulaşılamadı veya ürün listesi boş döndü.";
        }

        ReportSection sec = new ReportSection(
                "🛍️ Çağrı Market İndirimli Ürünler (İlk 10 Sayfa)",
                BASE_URL,
                subtitle
        );
        sec.tableRows.addAll(DealFilterAndGrouper.groupDeals(matched));
        return sec;
    }

    private List<String> fetchDynamicBrands() {
        List<String> brands = new ArrayList<>();
        try {
            String json = fetchString("https://api.cagri.com/tag/tag/id/1994");
            if (json != null) {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("brandList") && root.get("brandList").isJsonArray()) {
                    for (JsonElement el : root.getAsJsonArray("brandList")) {
                        if (!el.isJsonObject()) continue;
                        JsonObject b = el.getAsJsonObject();
                        if (b.has("name") && !b.get("name").isJsonNull()) {
                            String bn = b.get("name").getAsString().trim();
                            if (!bn.isEmpty()) brands.add(bn);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return brands;
    }

    private String fetchString(String url) throws IOException {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .build();
        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) return null;
            return resp.body().string();
        }
    }

    private static double safeDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static String decodeUnicodeEscapes(String input) {
        if (!input.contains("\\u")) return input;
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            if (i + 5 < input.length() && input.charAt(i) == '\\' && input.charAt(i + 1) == 'u') {
                try {
                    int code = Integer.parseInt(input.substring(i + 2, i + 6), 16);
                    sb.append((char) code);
                    i += 6;
                    continue;
                } catch (NumberFormatException ignored) {
                }
            }
            sb.append(input.charAt(i));
            i++;
        }
        return sb.toString();
    }
}
