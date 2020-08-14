package az.inci.bmsanbar;

class AppConfig {
    static final String DB_NAME= "BMS_ANBAR";
    static final int DB_VERSION=2;

    static final int PICK_MODE=0;
    static final int PACK_MODE=1;
    static final int DLV_MODE=2;

    static final int VIEW_MODE=0;
    static final int NEW_MODE=1;

    private User user;
    private String serverUrl ="http://192.168.0.5:8022";
    private String imageUrl ="http://192.168.0.5:8025";
    private int connectionTimeout=5;

    User getUser() {
        return user;
    }

    void setUser(User user) {
        this.user = user;
    }

    String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
