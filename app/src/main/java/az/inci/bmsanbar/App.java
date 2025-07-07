package az.inci.bmsanbar;

import androidx.multidex.MultiDexApplication;

import az.inci.bmsanbar.model.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class App extends MultiDexApplication {
    private User user;

    @Override
    public void onCreate() {
        super.onCreate();
        user = new User();
    }
}
