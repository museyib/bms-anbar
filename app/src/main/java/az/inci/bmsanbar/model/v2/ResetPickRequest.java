package az.inci.bmsanbar.model.v2;

public class ResetPickRequest
{
    private String trxNo;
    private String userId;

    public String getTrxNo()
    {
        return trxNo;
    }

    public void setTrxNo(String trxNo)
    {
        this.trxNo = trxNo;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }
}
