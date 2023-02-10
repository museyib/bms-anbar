package az.inci.bmsanbar;

import androidx.multidex.MultiDexApplication;

public class App extends MultiDexApplication
{
    private AppConfig config;

    @Override
    public void onCreate()
    {
        super.onCreate();
        setConfig(new AppConfig());
    }

    public AppConfig getConfig()
    {
        return config;
    }

    public void setConfig(AppConfig config)
    {
        this.config = config;
    }
}
