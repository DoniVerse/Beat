package com.example.beat;

import android.app.Application;
import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;

public class BeatApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        configureGlide();
    }

    private void configureGlide() {
        Glide.init(this, new GlideBuilder()
                .setMemoryCache(new LruResourceCache(1024 * 1024 * 20)) // 20mb
                .setDiskCache(new InternalCacheDiskCacheFactory(this, 1024 * 1024 * 50))); // 50mb
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        Glide.get(this).trimMemory(level);
    }
} 