package az.inci.bmsanbar.model;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.Objects;

public class Doc
{
    private String trxNo;
    private String trxDate;
    private int recStatus;
    private String pickStatus;
    private String whsCode;
    private String whsName;
    private String description;
    private String notes;
    private String pickArea;
    private String pickGroup;
    private String pickUser;
    private int itemCount;
    private int pickedItemCount;
    private String prevTrxNo;
    private String bpName;
    private String sbeName;
    private String bpCode;
    private String sbeCode;
    private String approveUser;
    private int trxTypeId;
    private double amount;
    private String srcWhsCode;
    private String srcWhsName;
    private String expCenterCode;
    private String expCenterName;
    private int activeSeconds;
    private List<Trx> trxList;

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

    public int getRecStatus()
    {
        return recStatus;
    }

    void setRecStatus(int recStatus)
    {
        this.recStatus = recStatus;
    }

    public String getPickStatus()
    {
        return pickStatus;
    }

    void setPickStatus(String pickStatus)
    {
        this.pickStatus = pickStatus;
    }

    public String getWhsCode()
    {
        return whsCode;
    }

    public void setWhsCode(String whsCode)
    {
        this.whsCode = whsCode;
    }

    public String getPickArea()
    {
        return pickArea;
    }

    public void setPickArea(String pickArea)
    {
        this.pickArea = pickArea;
    }

    public String getPickGroup()
    {
        return pickGroup;
    }

    public void setPickGroup(String pickGroup)
    {
        this.pickGroup = pickGroup;
    }

    public String getPickUser()
    {
        return pickUser;
    }

    public void setPickUser(String pickUser)
    {
        this.pickUser = pickUser;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getItemCount()
    {
        return itemCount;
    }

    public void setItemCount(int itemCount)
    {
        this.itemCount = itemCount;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Doc doc = (Doc) o;
        return Objects.equals(trxNo, doc.trxNo);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode()
    {
        return Objects.hash(trxNo);
    }

    public String getPrevTrxNo()
    {
        return prevTrxNo;
    }

    public void setPrevTrxNo(String prevTrxNo)
    {
        this.prevTrxNo = prevTrxNo;
    }

    public int getPickedItemCount()
    {
        return pickedItemCount;
    }

    public void setPickedItemCount(int pickedItemCount)
    {
        this.pickedItemCount = pickedItemCount;
    }

    public String getBpName()
    {
        return bpName;
    }

    public void setBpName(String bpName)
    {
        this.bpName = bpName;
    }

    public String getSbeName()
    {
        return sbeName;
    }

    public void setSbeName(String sbeName)
    {
        this.sbeName = sbeName;
    }

    public String getBpCode()
    {
        return bpCode;
    }

    public void setBpCode(String bpCode)
    {
        this.bpCode = bpCode;
    }

    public String getSbeCode()
    {
        return sbeCode;
    }

    public void setSbeCode(String sbeCode)
    {
        this.sbeCode = sbeCode;
    }

    public String getApproveUser()
    {
        return approveUser;
    }

    public void setApproveUser(String approveUser)
    {
        this.approveUser = approveUser;
    }

    public String getNotes()
    {
        return notes;
    }

    public void setNotes(String notes)
    {
        this.notes = notes;
    }

    public int getTrxTypeId()
    {
        return trxTypeId;
    }

    public void setTrxTypeId(int trxTypeId)
    {
        this.trxTypeId = trxTypeId;
    }

    public double getAmount()
    {
        return amount;
    }

    public void setAmount(double amount)
    {
        this.amount = amount;
    }

    public String getSrcWhsCode()
    {
        return srcWhsCode;
    }

    public void setSrcWhsCode(String srcWhsCode)
    {
        this.srcWhsCode = srcWhsCode;
    }

    public String getSrcWhsName()
    {
        return srcWhsName;
    }

    public void setSrcWhsName(String srcWhsName)
    {
        this.srcWhsName = srcWhsName;
    }

    public String getWhsName()
    {
        return whsName;
    }

    public void setWhsName(String whsName)
    {
        this.whsName = whsName;
    }

    public String getExpCenterCode()
    {
        return expCenterCode;
    }

    public void setExpCenterCode(String expCenterCode)
    {
        this.expCenterCode = expCenterCode;
    }

    public String getExpCenterName()
    {
        return expCenterName;
    }

    public void setExpCenterName(String expCenterName)
    {
        this.expCenterName = expCenterName;
    }

    public List<Trx> getTrxList()
    {
        return trxList;
    }

    public void setTrxList(List<Trx> trxList)
    {
        this.trxList = trxList;
    }

    public void addTrx(Trx trx)
    {
        trxList.add(trx);
    }

    public int getActiveSeconds()
    {
        return activeSeconds;
    }

    public void setActiveSeconds(int activeSeconds)
    {
        this.activeSeconds = activeSeconds;
    }
}
