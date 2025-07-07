package az.inci.bmsanbar.model.v2;

import lombok.Data;

@Data
public class ProductApproveRequestItem {
    private String invCode;
    private String invName;
    private String invBrand;
    private String barcode;
    private double qty;
}
