package az.inci.bmsanbar.model.v4;

import az.inci.bmsanbar.activity.AppBaseActivity;
import lombok.Data;

@Data
public class Request<T> {
    private String userId;
    private String deviceId;
    private T data;

    public static <T> Request<T> create(AppBaseActivity context, T data) {
        Request<T> request = new Request<>();
        request.setUserId(context.getUser().getId());
        request.setDeviceId(context.getDeviceIdString());
        request.setData(data);
        return request;
    }
}
