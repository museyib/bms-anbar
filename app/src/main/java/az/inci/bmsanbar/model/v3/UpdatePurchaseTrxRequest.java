package az.inci.bmsanbar.model.v3;

import java.util.List;

import lombok.Data;

@Data
public class UpdatePurchaseTrxRequest {
    private String userId;
    private String deviceId;
    private String trxNo;
    private int trxId;
    private double qty;
}
