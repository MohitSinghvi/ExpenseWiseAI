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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.shashank.expensemanager.R;
import com.shashank.expensemanager.transactionDb.AppDatabase;
import com.shashank.expensemanager.transactionDb.AppExecutors;
import com.shashank.expensemanager.transactionDb.TransactionEntry;
import com.shashank.expensemanager.transactionDb.TransactionViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private static AppDatabase appDatabase;

    private TransactionViewModel transactionViewModel;
    private List<TransactionEntry> transactionEntries;


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
        appDatabase = AppDatabase.getInstance(getContext());

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        inputEditText = view.findViewById(R.id.inputEditText);
        sendButton = view.findViewById(R.id.sendButton);
        clearButton = view.findViewById(R.id.clearButton);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(messageAdapter);

        transactionViewModel = ViewModelProviders.of(this)
                .get(TransactionViewModel.class);
        transactionViewModel.getExpenseList().observe(getViewLifecycleOwner(), new Observer<List<TransactionEntry>>() {
            @Override
            public void onChanged(List<TransactionEntry> entries) {
                transactionEntries = entries;
            }
        });

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
                        JSONObject jsonObject = new JSONObject(responseJson);

                        if (jsonObject.getBoolean("insert_to_database")) {
                            JSONArray valuesToInsert = jsonObject.getJSONArray("values_to_insert");

                            for (int i = 0; i < valuesToInsert.length(); i++) {
                                JSONObject transactionObject = valuesToInsert.getJSONObject(i);
                                int amount = transactionObject.getInt("amount");
                                String category = transactionObject.getString("category");
                                String description = transactionObject.getString("description");
                                String dateStr = transactionObject.getString("date");
                                String transactionType = transactionObject.getString("transactionType");

                                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
                                Date date = null;
                                try {
                                    date = sdf.parse(dateStr);
                                } catch (Exception error) {

                                }


                                final TransactionEntry mTransactionEntry = new TransactionEntry(amount,
                                        category,
                                        description,
                                        date,
                                        transactionType
                                );

                                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        appDatabase.transactionDao().insertExpense(mTransactionEntry);
                                    }
                                });
                            }

                            messageList.add(new Message("Database updated!", false));
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            conversationHistory += "AI BOT: Database updated!\\n";
                        } else {
                            String replyMessage = jsonObject.getString("message");
                            messageList.add(new Message(replyMessage, false));
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            conversationHistory += "AI BOT: " + replyMessage + "\\n";
                        }
                    } catch (IOException | JSONException e) {
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