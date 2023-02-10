package az.inci.bmsanbar.model.v2;

public class ShipDocInfo
{
    private String driverCode;
    private String driverName;
    private String vehicleCode;
    private String deliverNotes;
    private String shipStatus;

    public String getDriverCode()
    {
        return driverCode;
    }

    public void setDriverCode(String driverCode)
    {
        this.driverCode = driverCode;
    }

    public String getDriverName()
    {
        return driverName;
    }

    public void setDriverName(String driverName)
    {
        this.driverName = driverName;
    }

    public String getVehicleCode()
    {
        return vehicleCode;
    }

    public void setVehicleCode(String vehicleCode)
    {
        this.vehicleCode = vehicleCode;
    }

    public String getDeliverNotes()
    {
        return deliverNotes;
    }

    public void setDeliverNotes(String deliverNotes)
    {
        this.deliverNotes = deliverNotes;
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
