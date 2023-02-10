package az.inci.bmsanbar.model.v2;

import java.util.List;

import az.inci.bmsanbar.model.Trx;

public class InternalUseRequest
{
    private String userId;
    private String whsCode;
    private String expCenterCode;
    private String notes;
    private List<Trx> trxList;

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getWhsCode()
    {
        return whsCode;
    }

    public void setWhsCode(String whsCode)
    {
        this.whsCode = whsCode;
    }

    public String getExpCenterCode()
    {
        return expCenterCode;
    }

    public void setExpCenterCode(String expCenterCode)
    {
        this.expCenterCode = expCenterCode;
    }

    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public List<Trx> getTrxList()
    {
        return trxList;
    }

    public void setTrxList(List<Trx> trxList)
    {
        this.trxList = trxList;
    }
}
