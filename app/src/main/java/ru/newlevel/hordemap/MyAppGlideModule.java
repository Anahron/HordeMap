package ru.newlevel.hordemap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;


@GlideModule
public class MyAppGlideModule extends AppGlideModule {
    private static Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void applyOptions(Context context, GlideBuilder builder) {
        // Установка размера кэша в зависимости от доступной памяти устройства
        MemorySizeCalculator calculator = new MemorySizeCalculator.Builder(context)
                .setMemoryCacheScreens(2)
                .build();
        builder.setMemoryCache(new LruResourceCache(calculator.getMemoryCacheSize()));
    }

    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.append(StorageReference.class, InputStream.class, new StorageReferenceLoaderFactory(context));
    }

    public static class StorageReferenceLoaderFactory implements ModelLoaderFactory<StorageReference, InputStream> {
        private Context context;

        public StorageReferenceLoaderFactory(Context context) {
            this.context = context;
        }

        @Override
        public ModelLoader<StorageReference, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new StorageReferenceLoader(context);
        }

        @Override
        public void teardown() {
            // No-op
        }
    }

    public static class StorageReferenceLoader implements ModelLoader<StorageReference, InputStream> {
        private Context context;

        public StorageReferenceLoader(Context context) {
            this.context = context;
        }

        @Nullable
        @Override
        public LoadData<InputStream> buildLoadData(@NonNull StorageReference storageReference, int width, int height, @NonNull Options options) {
            return new LoadData<>(new ObjectKey(storageReference), new StorageReferenceFetcher(storageReference));
        }

        @Override
        public boolean handles(@NonNull StorageReference storageReference) {
            return true;
        }
    }


    public static class StorageReferenceFetcher implements DataFetcher<InputStream> {
        private final StorageReference storageReference;
        private InputStream inputStream;

        public StorageReferenceFetcher(StorageReference storageReference) {
            this.storageReference = storageReference;
        }

        @Override
        public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
            storageReference.getBytes(Long.MAX_VALUE)
                    .addOnSuccessListener(bytes -> {
                        inputStream = new ByteArrayInputStream(bytes);
                        callback.onDataReady(inputStream);
                    })
                    .addOnFailureListener(callback::onLoadFailed);
        }

        @Override
        public void cleanup() {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cancel() {
            // No-op
        }

        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }
    }
}
