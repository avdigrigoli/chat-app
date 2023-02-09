package net.whispwriting.mantischat;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendsList extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private RecyclerView usersListPage;
    private FirebaseFirestore usersDatabase;
    private CircleImageView userListImg;
    private FirebaseUser user;
    private List<String> friends;
    private Query query;
    private String currentUserName, currentUserImage;
    private GoogleAd ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Toolbar usersToolbar = findViewById(R.id.FriendListToolbar);
        setSupportActionBar(usersToolbar);
        getSupportActionBar().setTitle("Friends");

        usersDatabase = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        usersListPage = (RecyclerView) findViewById(R.id.FriendListPage);
        userListImg = (CircleImageView) findViewById(R.id.userListImg);

        usersListPage.setHasFixedSize(true);
        usersListPage.setLayoutManager(new LinearLayoutManager(this));
        usersListPage.getRecycledViewPool().setMaxRecycledViews(0, 0);

        query = usersDatabase.collection("Users");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, usersToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        DocumentReference ref = FirebaseFirestore.getInstance().collection("Users").document(user.getUid());
        ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        currentUserName = document.getString("name");
                        currentUserImage = document.getString("image");
                        friends = (ArrayList<String>) document.get("friends");
                        if (friends.size() > 0) {
                            FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>().setQuery(query.whereIn("user_id", friends), User.class).build();
                            FirestoreRecyclerAdapter<User, UsersViewHolder> adapter = new FirestoreRecyclerAdapter<User, UsersViewHolder>(options) {
                                @Override
                                public UsersViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                                    View view = LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.single_user_layout, parent, false);
                                    return new UsersViewHolder(view);
                                }

                                @Override
                                protected void onBindViewHolder(@NonNull UsersViewHolder usersViewHolder, int i, @NonNull final User users) {
                                    if (users != null) {
                                        usersViewHolder.setName(users.name);
                                        usersViewHolder.setStatus(users.status);
                                        usersViewHolder.setImg(users.image);

                                        final String userID = getSnapshots().getSnapshot(i).getId();
                                        usersViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                CharSequence[] options = new CharSequence[]{"View Profile", "Send Message"};
                                                AlertDialog.Builder alert = new AlertDialog.Builder(FriendsList.this);
                                                alert.setTitle(users.name);
                                                alert.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        if (i == 0){
                                                            Intent profilePage = new Intent(FriendsList.this, ProfileActivity.class);
                                                            profilePage.putExtra("userID", userID);
                                                            startActivity(profilePage);
                                                        }else if (i == 1){
                                                            Intent conversationIntent = new Intent(FriendsList.this, Conversation.class);
                                                            conversationIntent.putExtra("userID", userID);
                                                            conversationIntent.putExtra("name", users.name);
                                                            conversationIntent.putExtra("image", users.image);
                                                            updateConversationsStack(user.getUid(), userID, users.name, users.image);
                                                            updateConversationsStack(userID, user.getUid(), currentUserName, currentUserImage);
                                                            startActivity(conversationIntent);
                                                        }
                                                    }
                                                });
                                                AlertDialog dialog =  alert.create();
                                                dialog.show();
                                            }
                                        });
                                    }
                                }
                            };
                            usersListPage.setAdapter(adapter);
                            adapter.startListening();
                        }
                    }
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
            //startActivity(lastIntent);
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
            Intent intent = new Intent (this, Chat.class);
            startActivity(intent);
        }
        if (id == R.id.nav_friends){
            onBackPressed();
        }
        if (id == R.id.nav_search_users){
            startActivity(new Intent(this, Search.class));
        }
        if (id == R.id.nav_requests){
            startActivity(new Intent(this, FriendRequestList.class));
        }

        return true;
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
        public void setStatus (String status) {
            TextView userStatusVIew = (TextView) mView.findViewById(R.id.userListStatus);
            userStatusVIew.setText(status);
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
