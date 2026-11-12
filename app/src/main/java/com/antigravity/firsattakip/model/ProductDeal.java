package com.antigravity.firsattakip.model;

/**
 * Tek bir ürünün indirim ve fiyat bilgilerini tutan model.
 */
public class ProductDeal {
    public final String brand;
    public final String name;
    public final double origPrice;
    public final double discPrice;
    public final double discRate;
    public final String promoNote;

    public ProductDeal(
            String brand,
            String name,
            double origPrice,
            double discPrice,
            double discRate,
            String promoNote
    ) {
        this.brand = brand;
        this.name = name;
        this.origPrice = origPrice;
        this.discPrice = discPrice;
        this.discRate = discRate;
        this.promoNote = promoNote == null ? "" : promoNote.trim();
    }
}
