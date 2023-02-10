package az.inci.bmsanbar.model.v2;

import java.util.List;

public class TransferRequest
{
    private String srcWhsCode;
    private String trgWhsCode;
    private String userId;
    private List<TransferRequestItem> requestItems;

    public String getSrcWhsCode()
    {
        return srcWhsCode;
    }

    public void setSrcWhsCode(String srcWhsCode)
    {
        this.srcWhsCode = srcWhsCode;
    }

    public String getTrgWhsCode()
    {
        return trgWhsCode;
    }

    public void setTrgWhsCode(String trgWhsCode)
    {
        this.trgWhsCode = trgWhsCode;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public List<TransferRequestItem> getRequestItems()
    {
        return requestItems;
    }

    public void setRequestItems(List<TransferRequestItem> requestItems)
    {
        this.requestItems = requestItems;
    }
}
