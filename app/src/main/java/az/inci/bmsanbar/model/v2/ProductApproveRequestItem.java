package az.inci.bmsanbar.model.v2;

public class ProductApproveRequestItem
{
    private String invCode;
    private String invName;
    private String invBrand;
    private String barcode;
    private double qty;

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

    public String getBarcode()
    {
        return barcode;
    }

    public void setBarcode(String barcode)
    {
        this.barcode = barcode;
    }

    public double getQty()
    {
        return qty;
    }

    public void setQty(double qty)
    {
        this.qty = qty;
    }
}
