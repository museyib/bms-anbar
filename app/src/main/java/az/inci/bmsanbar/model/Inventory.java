package az.inci.bmsanbar.model;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Inventory
{
    private String invCode;
    private String invName;
    private String barcode;
    private String invBrand;
    private String internalCount;
    private double price;
    private double whsQty;

    public static Inventory parseFromTrx(Trx trx)
    {
        Inventory inventory = new Inventory();
        inventory.setInvCode(trx.getInvCode());
        inventory.setInvName(trx.getInvName());
        inventory.setInvBrand(trx.getInvBrand());
        inventory.setBarcode(trx.getBarcode());
        inventory.setPrice(trx.getPrice());

        return inventory;
    }

    public String getInvCode()
    {
        return invCode;
    }

    public void setInvCode(String invCode)
    {
        this.invCode = invCode;
    }

    public String getInvName()
    {
        return invName;
    }

    public void setInvName(String invName)
    {
        this.invName = invName;
    }

    public String getInvBrand()
    {
        return invBrand;
    }

    public void setInvBrand(String invBrand)
    {
        this.invBrand = invBrand;
    }

    public double getPrice()
    {
        return price;
    }

    public void setPrice(double price)
    {
        this.price = price;
    }

    @Override
    @NonNull
    public String toString()
    {
        return invCode + " | " + invName + " | " + invBrand;
    }

    public String getBarcode()
    {
        return barcode;
    }

    public void setBarcode(String barcode)
    {
        this.barcode = barcode;
    }

    public String getInternalCount()
    {
        return internalCount;
    }

    public void setInternalCount(String internalCount)
    {
        this.internalCount = internalCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Inventory inventory = (Inventory) o;
        return Objects.equals(invCode, inventory.invCode) &&
                Objects.equals(barcode, inventory.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(invCode, barcode);
    }

    public double getWhsQty() {
        return whsQty;
    }

    public void setWhsQty(double whsQty) {
        this.whsQty = whsQty;
    }
}
