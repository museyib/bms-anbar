package az.inci.bmsanbar.model.v3;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShipmentRequestItem {
    private String srcTrxNo;
    private String shipStatus;
}
