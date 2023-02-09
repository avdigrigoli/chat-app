package net.whispwriting.mantischat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatAdapter extends FirestoreRecyclerAdapter<User, Chat.UsersViewHolder> {

    private FirebaseUser user;
    private Context context;
    private String currentUserName;
    private String currentUserImage;
    private List<String> conversations;

    public ChatAdapter(@NonNull FirestoreRecyclerOptions<User> options, FirebaseUser user,
                       Context context, String currentUserName, String currentUserImage,
                       List<String> conversations) {
        super(options);
        this.user = user;
        this.context = context;
        this.currentUserName = currentUserName;
        this.currentUserImage = currentUserImage;
        this.conversations = conversations;
    }

    @Override
    public Chat.UsersViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.single_user_layout, parent, false);
        return new Chat.UsersViewHolder(view);
    }

    @Override
    protected void onBindViewHolder(@NonNull Chat.UsersViewHolder usersViewHolder, int i, @NonNull final User users) {
        final String userID = getSnapshots().getSnapshot(i).getId();
        usersViewHolder.setName(users.name);
        usersViewHolder.setLastMessage(user.getUid(), userID);
        usersViewHolder.setImg(users.image);

        usersViewHolder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent conversationIntent = new Intent(context, Conversation.class);
                conversationIntent.putExtra("userID", userID);
                conversationIntent.putExtra("name", users.name);
                conversationIntent.putExtra("image", users.image);
                updateConversationsStack(user.getUid(), userID, users.name, users.image);
                updateConversationsStack(userID, user.getUid(), currentUserName, currentUserImage);
                context.startActivity(conversationIntent);
            }
        });
    }

    private void updateConversationsStack(final String userID, final String otherUserID, String otherUserName, String otherUserImage){
        FirebaseFirestore.getInstance().collection("Users")
                .document(userID).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult().exists()){
                            DocumentSnapshot document = task.getResult();
                            List<String> conversations = (ArrayList<String>) document.get("conversations");
                            Map<String, Object> conversationMap = new HashMap<>();
                            if (conversations == null) {
                                conversations = new ArrayList<>();
                            }
                            if (!conversations.contains(otherUserID)){
                                conversations.add(otherUserID);
                            }
                            conversationMap.put("conversations", conversations);
                            FirebaseFirestore.getInstance().collection("Users")
                                    .document(userID).set(conversationMap, SetOptions.merge());
                        }
                    }
                });
        Map<String, Object> conversationMap = new HashMap<>();
        conversationMap.put("timestamp", System.currentTimeMillis());
        conversationMap.put("user_id", otherUserID);
        conversationMap.put("name", otherUserName);
        conversationMap.put("image", otherUserImage);
        FirebaseFirestore.getInstance().collection("Conversations")
                .document(userID).collection("recipients").document(otherUserID).set(conversationMap, SetOptions.merge());
    }

    public void add(User user){
        getSnapshots();
    }
}
