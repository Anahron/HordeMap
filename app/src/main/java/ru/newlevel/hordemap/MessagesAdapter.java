package ru.newlevel.hordemap;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {

    private static List<Messages> messages;

    public void setMessages(List<Messages> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void setLatestMessages(List<Messages> latestMessages) {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        messages.addAll(latestMessages);
        notifyItemRangeInserted(messages.size() - latestMessages.size(), latestMessages.size());

        if (!latestMessages.isEmpty()) {
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

    public class MessageViewHolder extends RecyclerView.ViewHolder {

        private TextView senderTextView;
        private TextView contentTextView;
        private TextView timeTextView;
        private DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        private TimeZone timeZone = TimeZone.getDefault();

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderTextView = itemView.findViewById(R.id.textViewUsername);
            contentTextView = itemView.findViewById(R.id.textViewMessage);
            timeTextView = itemView.findViewById(R.id.textViewTime);
        }

        public void bind(Messages message) {
            dateFormat.setTimeZone(timeZone);
            senderTextView.setText(message.getUserName());
            timeTextView.setText(dateFormat.format(new Date(message.getTimestamp())));
            contentTextView.setText(message.getMassage());

        }

    }
    }
