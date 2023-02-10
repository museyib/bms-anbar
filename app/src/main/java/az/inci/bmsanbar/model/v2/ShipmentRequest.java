package az.inci.bmsanbar.model.v2;

public class ShipmentRequest
{
    private String regionCode;
    private String driverCode;
    private String srcTrxNo;
    private String vehicleCode;
    private String userId;
    private String shipStatus;

    public String getRegionCode()
    {
        return regionCode;
    }

    public void setRegionCode(String regionCode)
    {
        this.regionCode = regionCode;
    }

    public String getDriverCode()
    {
        return driverCode;
    }

    public void setDriverCode(String driverCode)
    {
        this.driverCode = driverCode;
    }

    public String getSrcTrxNo()
    {
        return srcTrxNo;
    }

    public void setSrcTrxNo(String srcTrxNo)
    {
        this.srcTrxNo = srcTrxNo;
    }

    public String getVehicleCode()
    {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode)
    {
        this.vehicleCode = vehicleCode;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public String getShipStatus()
    {
        return shipStatus;
    }

    public void setShipStatus(String shipStatus)
    {
        this.shipStatus = shipStatus;
    }
}
