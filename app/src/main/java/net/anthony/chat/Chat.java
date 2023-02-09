package net.whispwriting.mantischat;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;


public class Chat extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView usersListPage;
    public Intent lastIntent;
    public static String CHANNEL_ID = "main";
    private FirebaseUser user;
    private List<String> conversations;
    private FirebaseFirestore usersDatabase;
    private Query query;
    private String currentUserName, currentUserImage;
    private GoogleAd ad;
    private ChatAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null){
            startActivity(new Intent(this, Welcome.class));
            return;
        }
        setContentView(R.layout.activity_chat);
        Toolbar toolbar1 = (Toolbar) findViewById(R.id.chatToolbar);
        setSupportActionBar(toolbar1);
        getSupportActionBar().setTitle("Chat");
        Intent intent = new Intent(this, Chat.class);
        lastIntent = intent;
        createNotificationChannel();

        usersDatabase = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        query = usersDatabase.collection("Conversations").document(user.getUid()).collection("recipients");

        usersListPage = (RecyclerView) findViewById(R.id.chat_recycler);
        usersListPage.setHasFixedSize(true);
        usersListPage.setLayoutManager(new LinearLayoutManager(this));
        usersListPage.getRecycledViewPool().setMaxRecycledViews(0, 0);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar1, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            DocumentReference ref = FirebaseFirestore.getInstance().collection("Users").document(user.getUid());
            ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            currentUserName = document.getString("name");
                            currentUserImage = document.getString("image");
                            FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>()
                                    .setQuery(query.orderBy("timestamp", Query.Direction.DESCENDING), User.class).build();
                            adapter = new ChatAdapter(options, user, Chat.this, currentUserName, currentUserImage, conversations);
                            loadNewConversations();
                            usersListPage.setAdapter(adapter);
                            adapter.startListening();
                        }
                    }
                }
            });
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    private void loadNewConversations(){
        Log.w("SnapshotListener", "loadNewConversations method called");
        usersDatabase.collection("Conversations").document(user.getUid()).collection("recipients")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        try {
                            Log.w("SnapshotListener", "SnapshotListener event triggered");
                            System.err.println("SnapshotListener fired");
                            User user = value.getDocuments().get(0).toObject(User.class);
                            adapter.notifyDataSetChanged();
                        }catch(IndexOutOfBoundsException e){
                            Log.w("SnapshotListener", "Index out of bounds");
                        }
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

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        try {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START);
            } else {
                super.onBackPressed();
            }
        }
        catch (Exception e){
            startActivity(lastIntent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            Intent loginSplash = new Intent(this, Welcome.class);
            startActivity(loginSplash);
        }
        if (item.getItemId() == R.id.action_accounts){
            Intent accountSettings = new Intent(this, AccountSettings.class);
            startActivity(accountSettings);
        }

        return true;
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_chat) {
            onBackPressed();
        }
        if (id == R.id.nav_friends){
            startActivity(new Intent(this, FriendsList.class));
        }
        if (id == R.id.nav_search_users){
            startActivity(new Intent(this, Search.class));
        }
        if (id == R.id.nav_requests){
            startActivity(new Intent(this, FriendRequestList.class));
        }

        return true;
    }

    @SuppressLint("WrongConstant")
    private void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence name = getString(R.string.channelName);
            String description = getString(R.string.channelDescription);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_MAX);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static class UsersViewHolder extends RecyclerView.ViewHolder{

        View mView;

        public UsersViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }
        public void setName (String name){
            TextView userNameView = (TextView) mView.findViewById(R.id.userListName);
            userNameView.setText(name);
        }
        public void setLastMessage (String currentUserID, String otherUserID) {
            final TextView userStatusVIew = (TextView) mView.findViewById(R.id.userListStatus);
            userStatusVIew.setText("");
            DatabaseReference messages = FirebaseDatabase.getInstance().getReference().child("messages").child(currentUserID).child(otherUserID);
            messages.addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    Iterable<DataSnapshot> data = snapshot.getChildren();
                    Log.w("messages_snapshot", snapshot.getChildrenCount() + "");
                    Iterator<DataSnapshot> snaps = data.iterator();
                    DataSnapshot messageData;
                    do {
                        messageData = snaps.next();
                        String key = (String) messageData.getKey();
                        if (key.equals("message")) {
                            String value = (String) messageData.getValue();
                            userStatusVIew.setText(value);
                        }
                    }while (snaps.hasNext());
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
        public void setImg (String image){
            CircleImageView userImageView = (CircleImageView) mView.findViewById(R.id.userListImg);
            if (!image.equals("default"))
                Picasso.get().load(image).placeholder(R.drawable.avatar).into(userImageView);
        }
        public void delete(){
            ViewGroup group = (ViewGroup) mView.getParent();
            group.removeView(mView);
        }
    }
}
