package com.meowcat.app;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MeowCatApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Инициализация Cloudinary
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "dcyodbsms");
        config.put("api_key", "211473873675842");
        config.put("api_secret", "qejmiyFWmk_gCa_clq6uyI_UPAc");
        config.put("secure", true);

        MediaManager.init(this, config);
    }
}