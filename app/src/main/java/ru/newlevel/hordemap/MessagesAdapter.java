package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private final RecyclerView recyclerView;
    public static Message lastDisplayedMessage = null;
    private List<Message> messages;
    boolean isAtBottom;
    private final ImageButton newMessageButton;
    private static File downloadsDir;


    public MessagesAdapter(RecyclerView recyclerView, ImageButton newMessageButton) {
        this.recyclerView = recyclerView;
        this.newMessageButton = newMessageButton;
    }

    @SuppressLint("NotifyDataSetChanged")
    void setMessages(@NonNull List<Message> newMessages) {
        System.out.println("Пришли в setMessages с количеством сообщений " + newMessages.size());
        lastDisplayedMessage = newMessages.get(newMessages.size() - 1);
        if (messages == null) {
            messages = newMessages;
            notifyDataSetChanged();
            recyclerView.scrollToPosition(getItemCount() - 1);
        } else if (!newMessages.equals(messages)) {
            isAtBottom = !recyclerView.canScrollVertically(1) && recyclerView.computeVerticalScrollRange() > recyclerView.getHeight();
            if (!isAtBottom && !newMessages.get(newMessages.size() - 1).getUserName().equals(User.getInstance().getUserName())) {
                newMessageButton.setVisibility(View.VISIBLE);
            }
            checkLatestMessageForDelete(newMessages);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void checkLatestMessageForDelete(@NonNull List<Message> latestMessages) {
        System.out.println("зашли в метод checkLatestMessageForDelete");
        Message previousMessage = messages.get(messages.size() - 1);
        int previousMessagePosition = messages.size() - 1;
        Message currentMessage = latestMessages.get(0);
        if (!previousMessage.getMessage().startsWith("http")
                && !currentMessage.getMessage().startsWith("http")
                && previousMessage.getUserName().equals(currentMessage.getUserName())) {
            messages.set(previousMessagePosition, currentMessage);
            notifyItemChanged(previousMessagePosition, 1);
            latestMessages.remove(0);
        }
        if (latestMessages.size() > 0) {
            messages.addAll(latestMessages);
            notifyItemRangeInserted(messages.size() - latestMessages.size(), latestMessages.size());
        }
        if (isAtBottom) {
            recyclerView.smoothScrollToPosition(getItemCount() - 1);
        }
    }

    public Message getItem(int position) {
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
        Message message = messages.get(position);
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

        @SuppressLint("SetTextI18n")
        public void bind(Message message) {
            dateFormat.setTimeZone(timeZone);
            senderTextView.setText(message.getUserName());
            timeTextView.setText(dateFormat.format(new Date(message.getTimestamp())));

            String messageText = message.getMessage();

            if (messageText.startsWith("https://firebasestorage")) {
                try {
                    String[] strings = messageText.split("&&&");
                    boolean hasFileSize = strings.length == 3;
                    String fileName = strings[1];
                    String fileSizeText = hasFileSize ? " (" + Integer.parseInt(strings[2]) / 1000 + "kb)" : "";

                    contentTextView.setText(getContentText(fileName, fileSizeText));
                    itemImageView.setVisibility(View.GONE);
                    button.setVisibility(View.GONE);

                    if (fileName.endsWith(".jpg")) {
                        itemImageView.setVisibility(View.VISIBLE);

                        if (message.getThumbnail() != null) {
                            itemImageView.setImageBitmap(message.getThumbnail());
                        }

                        File file = new File(downloadsDir, fileName);
                        if (file.exists()) {
                            setItemsInMessage(file, message);
                        }

                        itemImageView.setOnClickListener(v -> {
                            if (message.getFile() != null) {
                                openFullScreenImage(message.getFile());
                            } else {
                                StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(strings[0]);
                                GlideWrapper glideWrapper = new GlideWrapper();
                                glideWrapper.load(MapsActivity.getContext(), storageReference, itemImageView, message, fileName);
                            }
                        });
                    } else {
                        button.setVisibility(View.VISIBLE);
                        button.setOnClickListener(v -> MapsActivity.getViewModel().downloadFile(strings[0], fileName));
                    }
                } catch (Exception e) {
                    contentTextView.setText(messageText);
                    e.printStackTrace();
                }
            } else {
                button.setVisibility(View.GONE);
                itemImageView.setVisibility(View.GONE);
                contentTextView.setText(messageText);
            }
        }

        private String getContentText(String fileName, String fileSizeText) {
            if (fileName.endsWith(".jpg")) {
                return "Image:" + fileSizeText;
            } else {
                return fileName + fileSizeText;
            }
        }

        private void setItemsInMessage(File file, Message message) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            Bitmap thumbnailBitmap = ThumbnailUtils.extractThumbnail(bitmap, 120, 120);
            message.setThumbnail(thumbnailBitmap);
            message.setFile(file);
            itemImageView.setImageBitmap(thumbnailBitmap);
        }

        private void openFullScreenImage(File file) {
            Intent intent = new Intent(MapsActivity.getContext(), FullScreenImageActivity.class);
            intent.putExtra("imageUrl", file.getAbsolutePath());
            MapsActivity.getContext().startActivity(intent);
        }
    }

    public static class GlideWrapper {

        public void load(Context context, StorageReference storageReference, ImageView itemImageView, Message message, String fileName) {
            MapsActivity.makeToast("Изображение загружается, подождите");
            RequestBuilder<Drawable> requestBuilder = Glide.with(context).load(storageReference);

            RequestOptions options = new RequestOptions().error(R.drawable.download_image_error).override(1024, 1024).encodeQuality(50).diskCacheStrategy(DiskCacheStrategy.ALL);

            requestBuilder.apply(options).into(new CustomTarget<Drawable>() {
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                    Bitmap thumbnailBitmap = ThumbnailUtils.extractThumbnail(bitmap, 120, 120);
                    message.setThumbnail(thumbnailBitmap);
                    message.setFile(saveBitmapToDownloads(bitmap, fileName));
                    itemImageView.setImageBitmap(thumbnailBitmap);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {
                }
            });
        }

        public File saveBitmapToDownloads(Bitmap bitmap, String fileName) {
            downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            MediaScannerConnection.scanFile(MapsActivity.getContext(), new String[]{downloadsDir + "/" + fileName}, null, null);
            return file;
        }
    }
}
