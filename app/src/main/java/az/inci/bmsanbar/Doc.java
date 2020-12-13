package az.inci.bmsanbar;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.Objects;

class Doc
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

    String getTrxNo()
    {
        return trxNo;
    }

    void setTrxNo(String trxNo)
    {
        this.trxNo = trxNo;
    }

    String getTrxDate()
    {
        return trxDate;
    }

    void setTrxDate(String trxDate)
    {
        this.trxDate = trxDate;
    }

    int getRecStatus()
    {
        return recStatus;
    }

    void setRecStatus(int recStatus)
    {
        this.recStatus = recStatus;
    }

    String getPickStatus()
    {
        return pickStatus;
    }

    void setPickStatus(String pickStatus)
    {
        this.pickStatus = pickStatus;
    }

    String getWhsCode()
    {
        return whsCode;
    }

    void setWhsCode(String whsCode)
    {
        this.whsCode = whsCode;
    }

    String getPickArea()
    {
        return pickArea;
    }

    void setPickArea(String pickArea)
    {
        this.pickArea = pickArea;
    }

    String getPickGroup()
    {
        return pickGroup;
    }

    void setPickGroup(String pickGroup)
    {
        this.pickGroup = pickGroup;
    }

    String getPickUser()
    {
        return pickUser;
    }

    void setPickUser(String pickUser)
    {
        this.pickUser = pickUser;
    }

    String getDescription()
    {
        return description;
    }

    void setDescription(String description)
    {
        this.description = description;
    }

    int getItemCount()
    {
        return itemCount;
    }

    void setItemCount(int itemCount)
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

    @NonNull
    @Override
    public String toString() {
        return "Doc{" +
                "trxNo='" + trxNo + '\'' +
                ", trxDate='" + trxDate + '\'' +
                ", recStatus=" + recStatus +
                ", pickStatus='" + pickStatus + '\'' +
                ", whsCode='" + whsCode + '\'' +
                ", whsName='" + whsName + '\'' +
                ", description='" + description + '\'' +
                ", notes='" + notes + '\'' +
                ", pickArea='" + pickArea + '\'' +
                ", pickGroup='" + pickGroup + '\'' +
                ", pickUser='" + pickUser + '\'' +
                ", itemCount=" + itemCount +
                ", pickedItemCount=" + pickedItemCount +
                ", prevTrxNo='" + prevTrxNo + '\'' +
                ", bpName='" + bpName + '\'' +
                ", sbeName='" + sbeName + '\'' +
                ", bpCode='" + bpCode + '\'' +
                ", sbeCode='" + sbeCode + '\'' +
                ", approveUser='" + approveUser + '\'' +
                ", trxTypeId=" + trxTypeId +
                ", amount=" + amount +
                ", srcWhsCode='" + srcWhsCode + '\'' +
                ", srcWhsName='" + srcWhsName + '\'' +
                '}';
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

    public void setWhsName(String whsName)
    {
        this.whsName=whsName;
    }

    public String getWhsName()
    {
        return whsName;
    }
}
