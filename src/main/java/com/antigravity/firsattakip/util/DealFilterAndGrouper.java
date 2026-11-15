package com.antigravity.firsattakip.util;

import com.antigravity.firsattakip.model.GroupedDealRow;
import com.antigravity.firsattakip.model.ProductDeal;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Fırsat filtreleme, marka ayıklama, Türkçe fiyat formatlama ve
 * aynı marka + aynı indirim oranı gruplama yardımcıları.
 */
public final class DealFilterAndGrouper {

    private static final Locale TR_LOCALE = new Locale("tr", "TR");

    private static final List<String> KNOWN_BRANDS = Arrays.asList(
            "Johnson's Baby", "Johnsons Baby", "Dr. Oetker", "Dr Oetker", "Oral-B", "Oral B",
            "Head & Shoulders", "Head&Shoulders", "Pantene Pro-V", "L'Oreal Paris", "Loreal Paris",
            "Le Petit Marseillais", "Gliss Kur", "Nivea Men", "Nivea Soft", "Rexona Men",
            "Axe Black", "Dove Men", "Old Spice", "Gillette Venus", "Gillette Mach3",
            "Gillette Fusion", "Gillette Blue3", "Gillette Permatik", "Colgate Total",
            "Colgate Optik", "Sensodyne Promine", "Parodontax", "Corega", "Listerine",
            "Selpak Professional", "Solo Bambu", "Familia Plus", "Papia Biocare",
            "Sleepy Natural", "Sleepy Ecologic", "Sleepy Bio", "Molfix Pure", "Prima Premium",
            "Bebanthol Baby", "Sudocrem", "Uni Baby", "Dalin", "Mustela",
            "Vanish Kosla", "Cif Krem", "Cif Power", "Domestos", "Bingo Soft", "Bingo Matik",
            "Perwoll", "Persil", "Ariel", "Omo Active", "Yumoş Extra", "Vernel Max",
            "Finish Quantum", "Finish Classic", "Fairy Platinum", "Fairy Hepsi", "Pril Excellence",
            "Bref Power", "Air Wick", "Airwick", "Glade", "Febreze", "Marc", "Calgon",
            "Porçöz", "Asperox", "Sarı Güç", "Mavi Güç", "HighGenic", "Mr. Muscle", "Mr Muscle",
            "Scotch-Brite", "Scotch Brite", "Vileda", "Parex", "Koroplast", "Cook", "Macromax",
            "Lipton Ice Tea", "Coca-Cola", "Coca Cola", "Pepsi Max", "Fanta", "Sprite",
            "Red Bull", "Monster Energy", "Nescafe Gold", "Nescafé Gold", "Nescafe Classic",
            "Kahve Dünyası", "Kurukahveci Mehmet Efendi", "Mehmet Efendi", "Jacobs Monarch",
            "Tchibo", "illy", "Starbucks", "Caffe Nero", "Doğuş Çay", "Çaykur", "Ofçay",
            "Dimes Cool", "Cappy", "Fuse Tea", "Didi", "Sırma", "Beypazarı", "Kızılay", "Kınık",
            "Erikli", "Saka", "Hayat Su", "Pınar Yaşam", "Damla", "Uludağ", "Doğanay",
            "Sütaş", "Pınar", "İçim", "Sek", "Torku", "Altınkılıç", "Eker", "Activia",
            "Danone", "Polonez", "Cumhuriyet", "Namet", "Apikoğlu", "Aytaç", "Banvit",
            "Şenpiliç", "Beypiliç", "Erpiliç", "Gedik", "CP", "Uzman Kasap",
            "SuperFresh", "Superfresh", "Feast", "Dardanel", "Tamek", "Tat", "Öncü", "Yudum",
            "Komili", "Kırlangıç", "Tariş", "Kristal", "Marmarabirlik", "Fora", "Cem",
            "Yayla", "Reis", "Filiz", "Barilla", "Pastavilla", "Ankara Makarna", "Arbella",
            "Nuh'un Ankara", "Nuhun Ankara", "Söke", "Sinangil", "Pakmaya", "Knorr", "Calve",
            "Hellmann's", "Hellmanns", "Heinz", "Kühne", "Kuhne", "Kemal Kükrer",
            "Fersan", "Balküpü", "Balparmak", "Anavarza", "Koska", "Seğmen", "Tahsin Baba",
            "Peyman", "Tadım", "Çerezza", "Doritos", "Ruffles", "Lay's", "Lays", "Çitos",
            "Cheetos", "Pringles", "Eti", "Ülker", "Şölen", "Tadelle", "Sarelle", "Nutella",
            "Kinder", "Ferrero", "Raffaello", "Haribo", "Jelibon", "Bebeto", "Vivident",
            "First", "Falım", "Mentos", "Polo", "Olips", "Rocco", "Milka", "Toblerone",
            "Nestle", "Nestlé", "Damak", "KitKat", "Kitkat", "Twix", "Snickers", "Mars",
            "Bounty", "M&M's", "Züber", "Fellas", "Corny", "Wasa", "Schar", "Schär",
            "Royal Canin", "Pro Plan", "Purina One", "Felix", "Friskies", "Whiskas", "Pedigree",
            "Gourmet", "Dreamies", "Wanpy", "Reflex", "Enjoy", "Jungle",
            "Paşabahçe", "Lav", "Luminarc", "Borcam", "Tefal", "Karaca", "Emsan", "Korkmaz",
            "Schafer", "Fakir", "Arzum", "Sinbo", "Kiwi", "Philips", "Braun", "Remington",
            "Xiaomi", "Samsung", "Apple", "Spigen", "Duracell", "Varta", "Panasonic",
            "Hot Wheels", "Barbie", "Fisher-Price", "Fisher Price", "Lego", "Play-Doh",
            "Adel", "Faber-Castell", "Faber Castell", "Pritt", "Bic", "Rotring", "Pensan",
            "Purina", "Urban", "Blade", "Emotion", "Deotak", "Remove", "Veet", "Vi-Vet",
            "Proline", "Bado", "Cango", "Petlove", "Golf", "Algida", "Tursil", "Trendline"
    );

    private DealFilterAndGrouper() {
    }

    /**
     * Kriter 1: İndirim oranı >= %40 (yuvarlama payı ile >= 39.5)
     * Kriter 2: Normal fiyatı > 1000 TL ve indirim oranı >= %25 (>= 24.5)
     */
    public static boolean passesFilter(double origPrice, double discountRate) {
        if (origPrice <= 0) {
            return false;
        }
        if (discountRate >= 39.5) {
            return true;
        }
        return origPrice > 1000.0 && discountRate >= 24.5;
    }

    public static String formatTl(double val) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(TR_LOCALE);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return df.format(val) + " TL";
    }

    public static Double parseTl(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String cleaned = text.replaceAll("[^0-9,\\.]", "").trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        if (cleaned.contains(",") && cleaned.contains(".")) {
            if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else {
                cleaned = cleaned.replace(",", "");
            }
        } else if (cleaned.contains(",")) {
            cleaned = cleaned.replace(",", ".");
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String extractBrandFromName(String name, List<String> dynamicBrands) {
        if (name == null || name.trim().isEmpty()) {
            return "Belirtilmemiş";
        }
        String clean = name.trim();
        List<String> allBrands = new ArrayList<>(KNOWN_BRANDS);
        if (dynamicBrands != null) {
            allBrands.addAll(dynamicBrands);
        }
        allBrands.sort((a, b) -> Integer.compare(b.length(), a.length()));

        String lower = clean.toLowerCase(TR_LOCALE);
        for (String b : allBrands) {
            String bc = b.trim();
            if (bc.isEmpty()) continue;
            String bl = bc.toLowerCase(TR_LOCALE);
            if (lower.startsWith(bl + " ") || lower.equals(bl)) {
                return toDisplayBrand(bc);
            }
        }

        String[] parts = clean.split("\\s+");
        if (parts.length >= 2 && (parts[0].length() <= 2 || parts[0].endsWith("."))) {
            return parts[0] + " " + parts[1];
        }
        return parts[0];
    }

    private static String toDisplayBrand(String raw) {
        if (raw.equals(raw.toUpperCase(TR_LOCALE)) && raw.length() > 3) {
            String[] words = raw.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String w : words) {
                if (w.isEmpty()) continue;
                if (sb.length() > 0) sb.append(" ");
                sb.append(w.substring(0, 1).toUpperCase(TR_LOCALE))
                        .append(w.substring(1).toLowerCase(TR_LOCALE));
            }
            return sb.toString();
        }
        return raw;
    }

    public static String shortenProductName(String fullName, String brand) {
        String name = fullName == null ? "" : fullName.trim();
        if (brand != null && !brand.isEmpty()
                && !brand.equalsIgnoreCase("Markasız")
                && !brand.equalsIgnoreCase("Belirtilmemiş")) {
            Pattern p = Pattern.compile("^" + Pattern.quote(brand.trim()) + "[\\s\\-]+",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
            String stripped = p.matcher(name).replaceFirst("").trim();
            if (!stripped.isEmpty()) {
                return stripped;
            }
        }
        return name;
    }

    private static class DealGroupKey {
        final String brandKey;
        final String brandDisplay;
        final int rateInt;
        final String promoNote;

        DealGroupKey(String brandKey, String brandDisplay, int rateInt, String promoNote) {
            this.brandKey = brandKey == null ? "" : brandKey;
            this.brandDisplay = (brandDisplay == null || brandDisplay.trim().isEmpty()) ? "Belirtilmemiş" : brandDisplay.trim();
            this.rateInt = rateInt;
            this.promoNote = promoNote == null ? "" : promoNote.trim();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof DealGroupKey)) return false;
            DealGroupKey other = (DealGroupKey) o;
            return rateInt == other.rateInt
                    && brandKey.equals(other.brandKey)
                    && promoNote.equals(other.promoNote);
        }

        @Override
        public int hashCode() {
            int result = brandKey.hashCode();
            result = 31 * result + rateInt;
            result = 31 * result + promoNote.hashCode();
            return result;
        }
    }

    public static List<GroupedDealRow> groupDeals(List<ProductDeal> rawDeals) {
        if (rawDeals == null || rawDeals.isEmpty()) {
            return Collections.emptyList();
        }

        // Tekilleştir
        Map<String, ProductDeal> unique = new LinkedHashMap<>();
        for (ProductDeal d : rawDeals) {
            if (d == null || d.name == null) continue;
            String key = d.name.trim().toLowerCase(TR_LOCALE) + "|"
                    + Math.round(d.origPrice * 100) + "|"
                    + Math.round(d.discPrice * 100);
            unique.putIfAbsent(key, d);
        }

        Map<DealGroupKey, List<ProductDeal>> groups = new LinkedHashMap<>();

        for (ProductDeal p : unique.values()) {
            String rawBrand = p.brand == null ? "" : p.brand.trim();
            if (rawBrand.isEmpty()
                    || rawBrand.equalsIgnoreCase("MARKASIZ")
                    || rawBrand.toUpperCase(Locale.ROOT).startsWith("BRN-")) {
                rawBrand = extractBrandFromName(p.name, null);
            }
            if (rawBrand.isEmpty()) {
                rawBrand = "Belirtilmemiş";
            }
            String brandKey = rawBrand.toLowerCase(TR_LOCALE);
            String brandDisp = toDisplayBrand(rawBrand);
            int rateInt = (int) Math.round(p.discRate);
            String promo = p.promoNote == null ? "" : p.promoNote.trim();

            DealGroupKey key = new DealGroupKey(brandKey, brandDisp, rateInt, promo);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        List<DealGroupKey> sortedKeys = new ArrayList<>(groups.keySet());
        sortedKeys.sort((k1, k2) -> {
            int cmp = Integer.compare(k2.rateInt, k1.rateInt);
            if (cmp != 0) return cmp;
            return k1.brandDisplay.compareToIgnoreCase(k2.brandDisplay);
        });

        List<GroupedDealRow> rows = new ArrayList<>();
        for (DealGroupKey gk : sortedKeys) {
            List<ProductDeal> items = groups.get(gk);
            if (items == null || items.isEmpty()) continue;

            Set<String> shortNames = new LinkedHashSet<>();
            TreeSet<Double> origPrices = new TreeSet<>();
            TreeSet<Double> discPrices = new TreeSet<>();

            for (ProductDeal it : items) {
                shortNames.add(shortenProductName(it.name, gk.brandDisplay));
                origPrices.add(Math.round(it.origPrice * 100.0) / 100.0);
                discPrices.add(Math.round(it.discPrice * 100.0) / 100.0);
            }

            String prodCol;
            if (shortNames.size() == 1) {
                prodCol = shortNames.iterator().next();
            } else {
                prodCol = "[" + shortNames.size() + " Ürün] " + String.join(" • ", shortNames);
            }

            String origCol = formatPriceRange(origPrices);
            String discCol = formatPriceRange(discPrices);
            String rateCol = "%" + gk.rateInt + (gk.promoNote.isEmpty() ? "" : " (" + gk.promoNote + ")");

            rows.add(new GroupedDealRow(gk.brandDisplay, prodCol, origCol, discCol, rateCol, gk.rateInt));
        }

        return rows;
    }

    private static String formatPriceRange(TreeSet<Double> prices) {
        if (prices.isEmpty()) return "-";
        if (prices.size() == 1) return formatTl(prices.first());
        if (prices.size() == 2) return formatTl(prices.first()) + " / " + formatTl(prices.last());
        return formatTl(prices.first()) + " - " + formatTl(prices.last());
    }
}
