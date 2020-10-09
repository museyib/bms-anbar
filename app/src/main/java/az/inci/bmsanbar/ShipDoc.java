package az.inci.bmsanbar;

public class ShipDoc
{
    private String regionCode;
    private String driverCode;
    private String vehicleCode;
    private String userId;
    private int count;

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

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }
}
