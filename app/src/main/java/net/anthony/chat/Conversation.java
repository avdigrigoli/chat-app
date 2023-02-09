package net.whispwriting.mantischat;

import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class Conversation extends AppCompatActivity {

    private String userID, name, image;
    private DocumentReference userDoc;
    private CircleImageView profileIcon;
    private FloatingActionButton sendMessageButton;
    private FloatingActionButton attachImageButton;
    private EditText messageBox;
    private FirebaseUser currentUser;
    private DatabaseReference rootRef;
    private RecyclerView messagesView;
    private List<Message> messageList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;
    private StorageReference imageStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        final Toolbar convToolbar = findViewById(R.id.conversation_bar);
        setSupportActionBar(convToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        profileIcon = (CircleImageView) findViewById(R.id.profile_image_conv);
        sendMessageButton = (FloatingActionButton) findViewById(R.id.sendMessage);
        attachImageButton = (FloatingActionButton) findViewById(R.id.attachImageButton);
        messageBox = (EditText) findViewById(R.id.message);
        linearLayoutManager = new LinearLayoutManager(this);
        messagesView = (RecyclerView) findViewById(R.id.conversation_recycler);

        Point point = new Point();
        getWindowManager().getDefaultDisplay().getSize(point);

        messageAdapter = new MessageAdapter(messageList, this, messagesView, point.x, point.y);

        messagesView.setHasFixedSize(true);
        messagesView.setLayoutManager(linearLayoutManager);
        messagesView.getRecycledViewPool().setMaxRecycledViews(0,0);
        messagesView.setAdapter(messageAdapter);

        userID = getIntent().getStringExtra("userID");
        name = getIntent().getStringExtra("name");
        image = getIntent().getStringExtra("image");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        rootRef = FirebaseDatabase.getInstance().getReference();
        imageStorage = FirebaseStorage.getInstance().getReference();

        Picasso.get().load(image).placeholder(R.drawable.avatar).into(profileIcon);
        getSupportActionBar().setTitle(name);

        messagesView = (RecyclerView) findViewById(R.id.conversation_recycler);

        loadMessages();

        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage(messageBox.getText().toString(), "text");
                messageBox.setText("");
            }
        });

        attachImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(Conversation.this);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                sendMessage(resultUri.toString(), "image");
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void sendMessage(String message, String type){
        final Map<String, String> messageMap = new HashMap<>();
        DatabaseReference userMessagePush = rootRef.child("messages").child(currentUser.getUid())
                .child(userID).push();
        String pushID = userMessagePush.getKey();

        String currentUserRef = "messages/" + currentUser.getUid() + "/" + userID;
        String chatUserRef = "messages/" + userID + "/" + currentUser.getUid();

        final Map<String, Object> messageUserMap = new HashMap<>();
        messageUserMap.put(currentUserRef + "/" + pushID, messageMap);
        messageUserMap.put(chatUserRef + "/" + pushID, messageMap);

        if (type.equals("text")){
            if (!TextUtils.isEmpty(message)) {
                messageMap.put("message", message);
                messageMap.put("type", "text");
                messageMap.put("from", currentUser.getUid());
                messageMap.put("timestamp", System.currentTimeMillis() + "");
                rootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if (error != null) {
                            Log.w("CHAT", error.getMessage());
                        }
                    }
                });
            }
        }else{
            final StorageReference filepath = imageStorage.child("message_images").child(pushID + ".jpg");
            Uri imageUri = Uri.parse(message);
            filepath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(@NonNull Uri uri) {
                            messageMap.put("message", uri.toString());
                            messageMap.put("type", "image");
                            messageMap.put("from", currentUser.getUid());
                            messageMap.put("timestamp", System.currentTimeMillis() + "");
                            rootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                    if (error != null) {
                                        Log.w("CHAT", error.getMessage());
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    public void loadMessages(){
        rootRef.child("messages").child(currentUser.getUid()).child(userID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Message message = snapshot.getValue(Message.class);
                messageList.add(message);
                messageAdapter.notifyDataSetChanged();
                messagesView.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        messagesView.scrollToPosition(messageAdapter.getItemCount() - 1);
                    }
                });
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}