package az.inci.bmsanbar;

import androidx.annotation.NonNull;

import java.util.Objects;

public class ShipTrx {
    private String regionCode;
    private String driverCode;
    private String srcTrxNo;
    private String vehicleCode;
    private String userId;

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getDriverCode() {
        return driverCode;
    }

    public void setDriverCode(String driverCode) {
        this.driverCode = driverCode;
    }

    public String getSrcTrxNo() {
        return srcTrxNo;
    }

    public void setSrcTrxNo(String srcTrxNo) {
        this.srcTrxNo = srcTrxNo;
    }

    public String getVehicleCode() {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode) {
        this.vehicleCode = vehicleCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @NonNull
    @Override
    public String toString() {
        return srcTrxNo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShipTrx trx = (ShipTrx) o;
        return srcTrxNo.equals(trx.srcTrxNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcTrxNo);
    }
}
