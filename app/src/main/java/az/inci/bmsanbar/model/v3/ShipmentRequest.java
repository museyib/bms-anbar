package az.inci.bmsanbar.model.v3;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentRequest
{
    private String regionCode;
    private String driverCode;
    private String vehicleCode;
    private String userId;
    private List<ShipmentRequestItem> requestItems;
}
