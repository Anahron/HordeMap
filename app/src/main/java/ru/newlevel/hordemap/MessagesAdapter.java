package ru.newlevel.hordemap;


import static com.bumptech.glide.load.engine.DiskCacheStrategy.ALL;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static List<Messages> messages;
    public static Messages lastDisplayedMessage;

    @SuppressLint("NotifyDataSetChanged")
    public void setMessages(List<Messages> messages) {
        MessagesAdapter.messages = messages;
        notifyDataSetChanged();
    }

    public void setLatestMessages(List<Messages> latestMessages) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.addAll(latestMessages);
        notifyItemRangeInserted(messages.size() - latestMessages.size(), latestMessages.size());

        if (!latestMessages.isEmpty() && !messages.get(messages.size() - 1).getMassage().startsWith("http") && !messages.get(messages.size() - 2).getMassage().startsWith("http")) {
            Messages previousMessage = null;
            if (messages.size() > 1) {
                previousMessage = messages.get(messages.size() - 2);
            }

            Messages currentMessage = latestMessages.get(latestMessages.size() - 1);
            if (previousMessage != null && previousMessage.getUserName().equals(currentMessage.getUserName())) {
                notifyItemRemoved(messages.size() - 2);
                messages.remove(previousMessage);
            }
        }
    }

    public Messages getItem(int position) {
        if (messages != null && position >= 0 && position < messages.size()) {
            return messages.get(position);
        }
        return null;
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Messages message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages != null ? messages.size() : 0;
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        private final TextView senderTextView;
        private final TextView contentTextView;
        private final TextView timeTextView;
        @SuppressLint("SimpleDateFormat")
        private final DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        private final Button button;
        private final TimeZone timeZone = TimeZone.getDefault();
        private final ImageView itemImageView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.textViewUsername);
            contentTextView = itemView.findViewById(R.id.textViewMessage);
            timeTextView = itemView.findViewById(R.id.textViewTime);
            button = itemView.findViewById(R.id.download_button);
            itemImageView = itemView.findViewById(R.id.imageView);
        }

        public void bind(Messages message) {
            lastDisplayedMessage = message;
            System.out.println("messege получен" + message.getMassage());
            dateFormat.setTimeZone(timeZone);
            senderTextView.setText(message.getUserName());
            timeTextView.setText(dateFormat.format(new Date(message.getTimestamp())));
            if (message.getMassage().startsWith("https://firebasestorage")) {
                try {
                    String[] strings = message.getMassage().split("&&&");
                    if (strings[1].endsWith(".jpg")) {
                        contentTextView.setText(strings.length == 3 ? "Image:" + Integer.parseInt(strings[2]) / 1000 + "kb" : strings[1]);
                     //   contentTextView.setVisibility(View.GONE);
                        itemImageView.setVisibility(View.VISIBLE);
                        button.setVisibility(View.GONE);
                        itemImageView.setOnClickListener(v -> openFullScreenImage(strings[0]));
                    } else {
                        itemImageView.setVisibility(View.GONE);
                        button.setVisibility(View.VISIBLE);
                        button.setOnClickListener(v12 -> DataUpdateService.getInstance().downloadFile(strings[0], strings[1]));
                        contentTextView.setText(strings.length == 3 ? strings[1] + " (" + Integer.parseInt(strings[2]) / 1000 + "kb)" : strings[1]);
                    }
                } catch (Exception e) {
                    contentTextView.setText(message.getMassage());
                    e.printStackTrace();
                }
            } else {
                button.setVisibility(View.GONE);
                itemImageView.setVisibility(View.GONE);
                contentTextView.setText(message.getMassage());
            }
        }

        private void openFullScreenImage(String imageUrl) {
            Intent intent = new Intent(MapsActivity.getContext(), FullScreenImageActivity.class);
            intent.putExtra("imageUrl", imageUrl);
            MapsActivity.getContext().startActivity(intent);
        }
    }

    public static class GlideWrapper {
        public static void load(Context context, StorageReference storageReference, String Uri, String fileName) {
            RequestBuilder<Bitmap> requestBuilder = Glide.with(context)
                    .asBitmap()
                    .load(storageReference);

            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.loading_image) // Замените на свой ресурс заглушки
                    .error(R.drawable.download_image_error) // Замените на свой ресурс ошибки
                    .priority(Priority.HIGH)
                    .diskCacheStrategy(DiskCacheStrategy.ALL);

            requestBuilder
                    .apply(options)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            // Сохранение Bitmap во внешнем хранилище
                            File file = new File(Environment.getExternalStorageDirectory(), fileName);
                            try (FileOutputStream fos = new FileOutputStream(file)) {
                                resource.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.flush();
                                // Файл сохранен успешно
                            } catch (IOException e) {
                                e.printStackTrace();
                                // Обработка ошибки сохранения файла
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }
    }

    public static class GlideCacheModule {

        @SuppressLint("VisibleForTests")
        public static void installGlideCache(Context context, int cacheSizeBytes) {
            GlideBuilder builder = new GlideBuilder();
            builder.setDiskCache(new InternalCacheDiskCacheFactory(context, cacheSizeBytes));
            Glide.init(context, builder);
        }
    }
}
