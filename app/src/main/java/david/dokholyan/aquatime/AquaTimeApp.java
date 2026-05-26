package david.dokholyan.aquatime;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class AquaTimeApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();


        MapKitFactory.setApiKey("80160654-6817-4b79-89d8-ba87567080f4");
        MapKitFactory.initialize(this);
    }
}