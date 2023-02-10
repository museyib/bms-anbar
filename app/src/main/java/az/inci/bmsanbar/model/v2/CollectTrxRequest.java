package az.inci.bmsanbar.model.v2;

public class CollectTrxRequest
{
    private int trxId;
    private double qty;
    private String pickStatus;
    private int seconds;

    public int getTrxId()
    {
        return trxId;
    }

    public void setTrxId(int trxId)
    {
        this.trxId = trxId;
    }

    public double getQty()
    {
        return qty;
    }

    public void setQty(double qty)
    {
        this.qty = qty;
    }

    public String getPickStatus()
    {
        return pickStatus;
    }

    public void setPickStatus(String pickStatus)
    {
        this.pickStatus = pickStatus;
    }

    public int getSeconds()
    {
        return seconds;
    }

    public void setSeconds(int seconds)
    {
        this.seconds = seconds;
    }
}
