package com.bradley.whatsappclone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bradley.whatsappclone.Adapters.MessageAdapter;
import com.bradley.whatsappclone.Model.Chat;
import com.bradley.whatsappclone.Model.User;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MessageActivity extends AppCompatActivity {

    TextView userName;
    ImageView userImage;

    EditText msgEditText;
    ImageButton btnSend;

    FirebaseUser mFirebaseUser;
    DatabaseReference mReference;
    Intent mIntent;

    MessageAdapter mMessageAdapter;
    List<Chat> mChat;
    RecyclerView getMsgRecyclerView;

    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        //Widgets
        userName = findViewById(R.id.contacts_username);
        userImage = findViewById(R.id.contacts_image);
        btnSend = findViewById(R.id.btn_send);
        msgEditText = findViewById(R.id.text_send);

        //RecyclerView
        getMsgRecyclerView = findViewById(R.id.msg_recycler_view);
        getMsgRecyclerView.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        getMsgRecyclerView.setLayoutManager(linearLayoutManager);



        Toolbar toolbar = findViewById(R.id.contacts_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mIntent = getIntent();
        userId = mIntent.getStringExtra("userid");

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mReference = FirebaseDatabase.getInstance().getReference("Users").child(userId);

        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                userName.setText(user.getUsername());

                if(user.getImageURL().equals("default")) {
                    userImage.setImageResource(R.mipmap.ic_launcher_round);
                } else {
                    Glide.with(MessageActivity.this)
                            .load(user.getImageURL())
                            .into(userImage);
                }

                readMessages(mFirebaseUser.getUid(), userId, user.getImageURL());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = msgEditText.getText().toString();
                if(!message.equals("")) {
                    sendMessage(mFirebaseUser.getUid(), userId, message);
                } else {
                    Toast.makeText(getApplicationContext(), "Please type message o send", Toast.LENGTH_SHORT).show();
                }

                msgEditText.setText("");
            }
        });

    }

    private void sendMessage(String sender, String receiver, String message) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", sender);
        hashMap.put("receiver", receiver);
        hashMap.put("message", message);

        reference.child("Chats").push().setValue(hashMap);

        // Adding User to chat fragment: Latest chats with contacts

        final DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(mFirebaseUser.getUid())
                .child(userId);
        chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()) {
                    chatRef.child("id").setValue(userId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }

    public void readMessages(final String myId, final String contactId, final String imageUrl) {
        mChat = new ArrayList<>();

        mReference = FirebaseDatabase.getInstance().getReference("Chats");
        mReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mChat.clear();

                for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);

                    if((chat.getReceiver().equals(myId) && chat.getSender().equals(contactId)) ||
                            (chat.getReceiver().equals(contactId) && chat.getSender().equals(myId))) {
                        mChat.add(chat);
                    }

                    mMessageAdapter = new MessageAdapter(getApplicationContext(), mChat, imageUrl);
                    getMsgRecyclerView.setAdapter(mMessageAdapter);
                }

                mMessageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkStatus(String status) {
        mReference = FirebaseDatabase.getInstance().getReference("User").child(mFirebaseUser.getUid());

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("status", status);

        mReference.updateChildren(hashMap);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkStatus("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        checkStatus("offline");
    }
}