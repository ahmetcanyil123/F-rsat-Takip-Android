package com.antigravity.firsattakip.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Bir market bölümünü (kampanya listesi veya indirim tablosu) temsil eder.
 */
public class ReportSection {
    public final String title;
    public final String sourceUrl;
    public final String subtitle;
    public final List<String> bulletItems = new ArrayList<>();
    public final List<GroupedDealRow> tableRows = new ArrayList<>();

    public ReportSection(String title, String sourceUrl, String subtitle) {
        this.title = title;
        this.sourceUrl = sourceUrl;
        this.subtitle = subtitle;
    }
}
