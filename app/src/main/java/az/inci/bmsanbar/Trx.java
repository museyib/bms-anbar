package az.inci.bmsanbar;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Objects;

public class Trx {
    private int position;
    private int trxId;
    private String trxNo;
    private String trxDate;
    private String pickStatus;
    private String invCode;
    private String invName;
    private double qty;
    private double pickedQty;
    private double packedQty;
    private String whsCode;
    private String pickArea;
    private String pickGroup;
    private String pickUser;
    private String approveUser;
    private String uom;
    private double uomFactor;
    private String invBrand;
    private String bpName;
    private String sbeName;
    private String barcode;
    private String prevTrxNo;
    private String notes;
    private int priority;

    public int getTrxId() {
        return trxId;
    }

    public void setTrxId(int trxId) {
        this.trxId = trxId;
    }

    public String getTrxNo() {
        return trxNo;
    }

    public void setTrxNo(String trxNo) {
        this.trxNo = trxNo;
    }

    public String getTrxDate() {
        return trxDate;
    }

    public void setTrxDate(String trxDate) {
        this.trxDate = trxDate;
    }

    public String getPickStatus() {
        return pickStatus;
    }

    public void setPickStatus(String pickStatus) {
        this.pickStatus = pickStatus;
    }

    public String getInvCode() {
        return invCode;
    }

    public void setInvCode(String invCode) {
        this.invCode = invCode;
    }

    public String getInvName() {
        return invName;
    }

    public void setInvName(String invName) {
        this.invName = invName;
    }

    public double getQty() {
        return qty;
    }

    public void setQty(double qty) {
        this.qty = qty;
    }

    public double getPickedQty() {
        return pickedQty;
    }

    public void setPickedQty(double pickedQty) {
        this.pickedQty = pickedQty;
    }

    public String getWhsCode() {
        return whsCode;
    }

    public void setWhsCode(String whsCode) {
        this.whsCode = whsCode;
    }

    public String getPickArea() {
        return pickArea;
    }

    public void setPickArea(String pickArea) {
        this.pickArea = pickArea;
    }

    public String getPickGroup() {
        return pickGroup;
    }

    public void setPickGroup(String pickGroup) {
        this.pickGroup = pickGroup;
    }

    public String getPickUser() {
        return pickUser;
    }

    public void setPickUser(String pickUser) {
        this.pickUser = pickUser;
    }

    public String getApproveUser() {
        return approveUser;
    }

    public void setApproveUser(String approveUser) {
        this.approveUser = approveUser;
    }

    public String getUom() {
        return uom;
    }

    public void setUom(String uom) {
        this.uom = uom;
    }


    public String getInvBrand() {
        return invBrand;
    }

    public void setInvBrand(String invBrand) {
        this.invBrand = invBrand;
    }

    public String getBpName() {
        return bpName;
    }

    public void setBpName(String bpName) {
        this.bpName = bpName;
    }

    public String getSbeName() {
        return sbeName;
    }

    public void setSbeName(String sbeName) {
        this.sbeName = sbeName;
    }

    public double getUomFactor() {
        return uomFactor;
    }

    public void setUomFactor(double uomFactor) {
        this.uomFactor = uomFactor;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getPrevTrxNo() {
        return prevTrxNo;
    }

    public void setPrevTrxNo(String prevTrxNo) {
        this.prevTrxNo = prevTrxNo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "Trx{" +
                "trxId=" + trxId +
                ", trxNo='" + trxNo + '\'' +
                ", trxDate='" + trxDate + '\'' +
                ", pickStatus='" + pickStatus + '\'' +
                ", invCode='" + invCode + '\'' +
                ", invName='" + invName + '\'' +
                ", qty=" + qty +
                ", pickedQty=" + pickedQty +
                ", whsCode='" + whsCode + '\'' +
                ", pickArea='" + pickArea + '\'' +
                ", pickGroup='" + pickGroup + '\'' +
                ", pickUser='" + pickUser + '\'' +
                ", approveUser='" + approveUser + '\'' +
                ", uom='" + uom + '\'' +
                ", uomFactor=" + uomFactor +
                ", invBrand='" + invBrand + '\'' +
                ", bpName='" + bpName + '\'' +
                ", sbeName='" + sbeName + '\'' +
                ", barcode='" + barcode + '\'' +
                ", prevTrxNo='" + prevTrxNo + '\'' +
                ", notes='" + notes + '\'' +
                ", priority=" + priority +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trx trx = (Trx) o;
        return trxId == trx.trxId;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(trxId);
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public double getPackedQty() {
        return packedQty;
    }

    public void setPackedQty(double packedQty) {
        this.packedQty = packedQty;
    }
}
