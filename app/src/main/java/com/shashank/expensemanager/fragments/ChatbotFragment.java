package com.shashank.expensemanager.fragments;

import static com.shashank.expensemanager.activities.MainActivity.fab;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.transactionDb.TransactionViewModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import io.noties.markwon.Markwon;


interface ChatbotAPI {
    @POST("chatbot")
    Call<ResponseBody> sendMessage(@Body RequestBody requestBody);
}
class Message {
    private String text;
    private boolean isUserMessage;

    public Message(String text, boolean isUserMessage) {
        this.text = text;
        this.isUserMessage = isUserMessage;
    }

    public String getText() {
        return text;
    }

    public boolean isUserMessage() {
        return isUserMessage;
    }
}

class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message);

        Markwon markwon = Markwon.create(holder.itemView.getContext());
        markwon.setMarkdown(holder.messageTextView, message.getText());

        if (message.isUserMessage()) {
            holder.messageTextView.setBackgroundResource(R.drawable.background_user_message);
            holder.messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
        } else {
            holder.messageTextView.setBackgroundResource(R.drawable.background_bot_message);
            holder.messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private TextView messageTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }

        public void bind(Message message) {
            messageTextView.setText(message.getText());
            if (message.isUserMessage()) {
                messageTextView.setBackgroundResource(R.drawable.background_user_message);
                messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                messageTextView.setBackgroundResource(R.drawable.background_bot_message);
                messageTextView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }
        }
    }
}
public class ChatbotFragment extends Fragment {
    private List<Message> messageList;
    private MessageAdapter messageAdapter;
    private EditText inputEditText;
    private Button sendButton;
    private Button clearButton;
    private String conversationHistory = "";

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        Log.i("fragment", String.valueOf(isVisibleToUser));
        if (isVisibleToUser){
            fab.setVisibility(View.GONE);
        } else{
            fab.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatbot, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        inputEditText = view.findViewById(R.id.inputEditText);
        sendButton = view.findViewById(R.id.sendButton);
        clearButton = view.findViewById(R.id.clearButton);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(messageAdapter);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userMessage = inputEditText.getText().toString().trim();
                if (!userMessage.isEmpty()) {
                    messageList.add(new Message(userMessage, true));
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    inputEditText.setText("");

                    conversationHistory += "USER: " + userMessage + "\\n";
                    Log.i("YOYOYOYOYO", "ASDASDSA");
                    sendMessageToAPI(userMessage);
                }
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                messageList.clear();
                messageAdapter.notifyDataSetChanged();
                conversationHistory = "";
            }
        });

        return view;
    }

    private void sendMessageToAPI(String userMessage) {
        String metadata = getMetadataFromTransactionTable();
        String json = "{\"metadata\":\"" + metadata + "\",\"message\":\"" + conversationHistory + "\"}";

        Log.i("jsonData", json);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        ChatbotAPI chatbotAPI = retrofit.create(ChatbotAPI.class);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), json);
        Call<ResponseBody> call = chatbotAPI.sendMessage(requestBody);

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseJson = response.body().string();
                        // Parse the response JSON and extract the reply message
                        String replyMessage = extractReplyMessage(responseJson);

                        Log.i("replyMessage yo", replyMessage);

                        messageList.add(new Message(replyMessage, false));
                        messageAdapter.notifyItemInserted(messageList.size() - 1);
                        conversationHistory += "AI BOT: " + replyMessage + "\\n";
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("APIError", "Response unsuccessful. Code: " + response.code() + ", Message: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("NetworkFailure", "Error sending request: " + t.getMessage());
                t.printStackTrace();
            }
        });
    }

    private String extractReplyMessage(String responseJson) {
        try {
            JSONObject jsonObject = new JSONObject(responseJson);
            return jsonObject.getString("reply_message");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

    private String getMetadataFromTransactionTable() {
        // Retrieve the transaction entries from the database
        List<TransactionEntry> transactionEntries = TransactionViewModel.getExpenseList().getValue();

        StringBuilder metadata = new StringBuilder();
        for (TransactionEntry entry : transactionEntries) {
            metadata.append(entry.getAmount()).append(" - ")
                    .append(entry.getCategory()).append(" - ")
                    .append(entry.getDescription()).append(" - ")
                    .append(entry.getDate()).append(" - ")
                    .append(entry.getTransactionType())
                    .append("\\n");
        }

        return metadata.toString();
    }
}