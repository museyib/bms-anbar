package az.inci.bmsanbar.model.v2;

public class TransferRequestItem
{
    private String invCode;
    private double qty;

    public String getInvCode()
    {
        return invCode;
    }

    public void setInvCode(String invCode)
    {
        this.invCode = invCode;
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
