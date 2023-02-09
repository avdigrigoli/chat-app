package net.whispwriting.mantischat;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

// AccountSettings should be a mainactivity with an XML
// Chat page is a home page
// include in the README.md we have to
// conversation and chat page
// ConversionActivity lisy of chats in chat class

public class ProfileActivity extends AppCompatActivity {

    private TextView displayName;
    private TextView status;
    private CircleImageView userImage;
    private Button sendFriendRequest;
    private Button declineFriendRequest;
    private ProgressDialog progress;
    private int currentState;
    private FirebaseUser currentUser;
    private FirebaseFirestore firestore;
    private FirebaseDatabase firebase;
    private String uid;
    private boolean success;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        Toolbar usersToolbar = findViewById(R.id.profileToolbar);
        setSupportActionBar(usersToolbar);
        getSupportActionBar().setTitle(" ");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        uid = getIntent().getStringExtra("userID");

        firestore = FirebaseFirestore.getInstance();
        firebase = FirebaseDatabase.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        displayName = (TextView) findViewById(R.id.DisplayNameText_other);
        status = (TextView) findViewById(R.id.statusText_other);
        userImage = (CircleImageView) findViewById(R.id.profile_image_other);
        sendFriendRequest = (Button) findViewById(R.id.friendRequest);
        declineFriendRequest = (Button) findViewById(R.id.declineFriendRequest);
        declineFriendRequest.setEnabled(false);
        declineFriendRequest.setVisibility(View.INVISIBLE);

        currentState = 0;

        progress = new ProgressDialog(this);
        progress.setTitle("Loading User Data");
        progress.setMessage("Please wait while the user's data is loaded");
        progress.setCanceledOnTouchOutside(false);
        progress.show();

        DocumentReference user = firestore.collection("Users").document(uid);
        user.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot dataSnapshot = task.getResult();
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.getString("name");
                        String userStatus = dataSnapshot.getString("status");
                        String image = dataSnapshot.getString("image");

                        displayName.setText(name);
                        status.setText(userStatus);
                        Picasso.get().load(image).placeholder(R.drawable.avatar).into(userImage);
                    }
                }
                DocumentReference friendRequests = firestore.collection("Friend_Requests").document(currentUser.getUid());
                friendRequests.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot dataSnapshot = task.getResult();
                            if (dataSnapshot.exists()) {
                                String requestType = dataSnapshot.getString(uid + "request_type");
                                if (requestType != null && requestType.equals("received")){
                                    currentState = 2;
                                    sendFriendRequest.setText("Accept Friend Request");
                                    declineFriendRequest.setEnabled(true);
                                    declineFriendRequest.setVisibility(View.VISIBLE);
                                }else if (requestType != null && requestType.equals("sent")){
                                    currentState = 1;
                                    sendFriendRequest.setText("Cancel Friend Request");
                                }
                            }
                        }
                    }
                });

                DocumentReference friends = firestore.collection("Users").document(currentUser.getUid());
                friends.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot dataSnapshot = task.getResult();
                            if (dataSnapshot.exists()) {
                                List<String> friends = (ArrayList<String>) dataSnapshot.get("friends");
                                if (friends.contains(uid)) {
                                    currentState = 3;
                                    sendFriendRequest.setText("Unfriend");
                                }
                            }
                        }
                        progress.dismiss();
                    }
                });
            }
        });

        if (currentUser.getUid().equals(uid)){
            sendFriendRequest.setEnabled(false);
            sendFriendRequest.setVisibility(View.INVISIBLE);
        }
        sendFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                sendFriendRequest.setEnabled(false);
                // Current states determines if a friend request has been sent or received, or if users are friends
                // 0 is nothing sent/received, not friends
                if (currentState == 0){
                    addFriendRequest();
                // 1 is has been sent
                }else if (currentState == 1){
                    removeFriendRequest("Send Friend Request");
                // 2 a friend request has been received
                }else if (currentState == 2){
                    removeFriendRequest("Unfriend");
                    changeFriend(true);
                    // users already friends
                }else if (currentState == 3){
                    changeFriend(false);
                }
            }
        });

        declineFriendRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeFriendRequest("Send Friend Request");
            }
        });
    }

    public void addFriendRequest(){
        Map<String, Object> friendRequests = new HashMap<>();
        final Map<String, Object> selfFriendRequests = new HashMap<>();
        selfFriendRequests.put(uid + "request_type", "sent");
        friendRequests.put(currentUser.getUid() + "request_type", "received");
        final CollectionReference friendRequestCollection = firestore.collection("Friend_Requests");
        friendRequestCollection.document(currentUser.getUid()).set(selfFriendRequests, SetOptions.merge());
        friendRequestCollection.document(uid).set(friendRequests, SetOptions.merge());

        friendRequestCollection.document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        List<String> requests = (ArrayList<String>) document.get("requests");
                        Map<String, Object> selfRequestsList = new HashMap<>();
                        if (requests != null) {
                            requests.add(currentUser.getUid());
                        }else{
                            requests = new ArrayList<>();
                            requests.add(currentUser.getUid());
                        }
                        selfRequestsList.put("requests", requests);
                        friendRequestCollection.document(uid).set(selfRequestsList, SetOptions.merge());
                    }
                }
            }
        });

        sendFriendRequest.setEnabled(true);
        currentState = 1;
        sendFriendRequest.setText("Cancel Request");
    }

    public void removeFriendRequest(final String buttonText){
        final Map<String, Object> friendRequests = new HashMap<>();
        Map<String, Object> selfFriendRequests = new HashMap<>();
        selfFriendRequests.put(uid + "request_type", FieldValue.delete());
        selfFriendRequests.put(currentUser.getUid() + "request_type", FieldValue.delete());
        friendRequests.put(currentUser.getUid() + "request_type", FieldValue.delete());
        friendRequests.put(uid + "request_type", FieldValue.delete());
        final CollectionReference friendRequestCollection = firestore.collection("Friend_Requests");
        friendRequestCollection.document(currentUser.getUid()).set(selfFriendRequests, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            friendRequestCollection.document(uid).set(friendRequests, SetOptions.merge())
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){
                                                sendFriendRequest.setText(buttonText);
                                                sendFriendRequest.setEnabled(true);
                                                declineFriendRequest.setEnabled(false);
                                                declineFriendRequest.setVisibility(View.INVISIBLE);
                                                currentState = 0;
                                            }
                                        }
                                    });
                        }
                    }
                });

        friendRequestCollection.document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        List<String> requests = (ArrayList<String>) document.get("requests");
                        if (requests != null) {
                            requests.remove(currentUser.getUid());
                            requests.remove(uid);
                            Map<String, Object> selfRequestsList = new HashMap<>();
                            selfRequestsList.put("requests", requests);
                            friendRequestCollection.document(uid).set(selfRequestsList, SetOptions.merge());
                        }
                    }
                }
            }
        });

        friendRequestCollection.document(currentUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        List<String> requests = (ArrayList<String>) document.get("requests");
                        if (requests != null) {
                            requests.remove(uid);
                            requests.remove(currentUser.getUid());
                            Map<String, Object> selfRequestsList = new HashMap<>();
                            selfRequestsList.put("requests", requests);
                            friendRequestCollection.document(currentUser.getUid()).set(selfRequestsList, SetOptions.merge());
                        }
                    }
                }
            }
        });
    }

    public void changeFriend(final boolean adding){
        final CollectionReference userRef = firestore.collection("Users");

        userRef.document(currentUser.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    final DocumentSnapshot selfDocument = task.getResult();
                    if (selfDocument.exists()) {
                        userRef.document(uid).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()){
                                    final DocumentSnapshot userDocument = task.getResult();
                                    if (userDocument.exists()){
                                        List<String> selfFriends = (ArrayList<String>) selfDocument.get("friends");
                                        if (adding)
                                            selfFriends.add(uid);
                                        else
                                            selfFriends.remove(uid);
                                        Map<String, Object> selfFriendMap = new HashMap<>();
                                        selfFriendMap.put("friends", selfFriends);
                                        userRef.document(currentUser.getUid()).set(selfFriendMap, SetOptions.merge())
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()){
                                                            List<String> userFriends = (ArrayList<String>) userDocument.get("friends");
                                                            if (adding)
                                                                userFriends.add(currentUser.getUid());
                                                            else
                                                                userFriends.remove(currentUser.getUid());
                                                            Map<String, Object> userFriendMap = new HashMap<>();
                                                            userFriendMap.put("friends", userFriends);
                                                            userRef.document(uid).set(userFriendMap, SetOptions.merge())
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()){
                                                                                if (!adding) {
                                                                                    sendFriendRequest.setText("Send Friend Request");
                                                                                    sendFriendRequest.setEnabled(true);
                                                                                    currentState = 0;
                                                                                }
                                                                            }
                                                                        }
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                }
                            }
                        });
                    }
                }
            }
        });
    }
}
