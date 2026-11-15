package com.antigravity.firsattakip.model;

/**
 * Aynı marka ve aynı indirim oranına sahip gruplanmış tablo satırı.
 */
public class GroupedDealRow {
    public final String brandDisplay;
    public final String productsDisplay;
    public final String origPriceDisplay;
    public final String discPriceDisplay;
    public final String discountRateDisplay;
    public final int rateInt;

    public GroupedDealRow(
            String brandDisplay,
            String productsDisplay,
            String origPriceDisplay,
            String discPriceDisplay,
            String discountRateDisplay,
            int rateInt
    ) {
        this.brandDisplay = (brandDisplay == null || brandDisplay.trim().isEmpty()) ? "Belirtilmemiş" : brandDisplay.trim();
        this.productsDisplay = productsDisplay == null ? "" : productsDisplay.trim();
        this.origPriceDisplay = origPriceDisplay == null ? "-" : origPriceDisplay.trim();
        this.discPriceDisplay = discPriceDisplay == null ? "-" : discPriceDisplay.trim();
        this.discountRateDisplay = discountRateDisplay == null ? "" : discountRateDisplay.trim();
        this.rateInt = rateInt;
    }
}
