package az.inci.bmsanbar.model.v2;

import java.util.List;

public class ProductApproveRequest
{
    private String trxNo;
    private String trxDate;
    private String notes;
    private int status;
    private String userId;
    private List<ProductApproveRequestItem> requestItems;

    public String getTrxNo()
    {
        return trxNo;
    }

    public void setTrxNo(String trxNo)
    {
        this.trxNo = trxNo;
    }

    public String getTrxDate()
    {
        return trxDate;
    }

    public void setTrxDate(String trxDate)
    {
        this.trxDate = trxDate;
    }

    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public List<ProductApproveRequestItem> getRequestItems()
    {
        return requestItems;
    }

    public void setRequestItems(List<ProductApproveRequestItem> requestItems)
    {
        this.requestItems = requestItems;
    }
}
