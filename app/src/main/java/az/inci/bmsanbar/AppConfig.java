package az.inci.bmsanbar;

import az.inci.bmsanbar.model.User;

public class AppConfig
{
    static final String DB_NAME = "BMS_ANBAR";
    static final int DB_VERSION = 13;

    public static final int PICK_MODE = 0;
    public static final int PACK_MODE = 1;
    public static final int SHIP_MODE = 2;
    public static final int APPROVE_MODE = 3;
    public static final int PRODUCT_APPROVE_MODE = 4;
    public static final int INV_ATTRIBUTE_MODE = 5;

    public static final int VIEW_MODE = 0;
    public static final int NEW_MODE = 1;

    private User user;
    private String serverUrl = "http://185.129.0.46:8022";
    private String imageUrl = "http://185.129.0.46:8025";
    private int connectionTimeout = 5;
    private boolean cameraScanning = false;

    public User getUser()
    {
        return user;
    }

    public void setUser(User user)
    {
        this.user = user;
    }

    public String getServerUrl()
    {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl)
    {
        this.serverUrl = serverUrl;
    }

    public int getConnectionTimeout()
    {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout)
    {
        this.connectionTimeout = connectionTimeout;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public boolean isCameraScanning()
    {
        return cameraScanning;
    }

    public void setCameraScanning(boolean cameraScanning)
    {
        this.cameraScanning = cameraScanning;
    }
}
