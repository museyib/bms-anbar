package az.inci.bmsanbar.model;

import java.util.Objects;

public class InvBarcode
{
    private String invCode;
    private String barcode;
    private String uom;
    private double uomFactor;
    private boolean defined;

    public String getInvCode()
    {
        return invCode;
    }

    public void setInvCode(String invCode)
    {
        this.invCode = invCode;
    }

    public String getBarcode()
    {
        return barcode;
    }

    public void setBarcode(String barcode)
    {
        this.barcode = barcode;
    }

    public double getUomFactor()
    {
        return uomFactor;
    }

    public void setUomFactor(double uomFactor)
    {
        this.uomFactor = uomFactor;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvBarcode barcode1 = (InvBarcode) o;
        return Objects.equals(barcode, barcode1.barcode);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(barcode);
    }

    public boolean isDefined()
    {
        return defined;
    }

    public void setDefined(boolean defined)
    {
        this.defined = defined;
    }

    public String getUom()
    {
        return uom;
    }

    public void setUom(String uom)
    {
        this.uom = uom;
    }
}
