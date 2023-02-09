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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendRequestList extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private RecyclerView usersListPage;
    private FirebaseFirestore usersDatabase;
    private CircleImageView userListImg;
    private FirebaseUser user;
    private List<String> friends;
    private Query query;
    private GoogleAd ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Toolbar usersToolbar = findViewById(R.id.FriendListToolbar);
        setSupportActionBar(usersToolbar);
        getSupportActionBar().setTitle("Friend Requests");

        usersDatabase = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        usersListPage = (RecyclerView) findViewById(R.id.FriendListPage);
        usersListPage.setHasFixedSize(true);
        usersListPage.setLayoutManager(new LinearLayoutManager(this));
        userListImg = (CircleImageView) findViewById(R.id.userListImg);

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
        DocumentReference ref = FirebaseFirestore.getInstance().collection("Friend_Requests").document(user.getUid());
        ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        friends = (ArrayList<String>) document.get("requests");
                        if (friends != null && friends.size() > 0) {
                            FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>().setQuery(query.whereIn("user_id", friends), User.class).build();
                            FirestoreRecyclerAdapter<User, UsersViewHolder> adapter = new FirestoreRecyclerAdapter<User, UsersViewHolder>(options) {
                                @Override
                                public UsersViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                                    View view = LayoutInflater.from(parent.getContext())
                                            .inflate(R.layout.single_user_layout, parent, false);
                                    return new UsersViewHolder(view);
                                }

                                @Override
                                protected void onBindViewHolder(@NonNull final UsersViewHolder usersViewHolder, int i, @NonNull final User users) {
                                    if (users != null) {
                                        usersViewHolder.setName(users.name);
                                        usersViewHolder.setStatus(users.status);
                                        usersViewHolder.setImg(users.image);

                                        final String userID = getSnapshots().getSnapshot(i).getId();
                                        usersViewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                CharSequence[] options = new CharSequence[]{"Accept Friend Request", "Decline Friend Request"};
                                                AlertDialog.Builder alert = new AlertDialog.Builder(FriendRequestList.this);
                                                alert.setTitle(users.name);
                                                alert.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        if (i == 0){
                                                            removeFriendRequest(userID);
                                                            changeFriend(true, userID);
                                                        }else if (i == 1){
                                                            removeFriendRequest(userID);
                                                            changeFriend(false, userID);
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

    public void removeFriendRequest(final String uid){
        final Map<String, Object> friendRequests = new HashMap<>();
        Map<String, Object> selfFriendRequests = new HashMap<>();
        selfFriendRequests.put(uid + "request_type", FieldValue.delete());
        selfFriendRequests.put(user.getUid() + "request_type", FieldValue.delete());
        friendRequests.put(user.getUid() + "request_type", FieldValue.delete());
        friendRequests.put(uid + "request_type", FieldValue.delete());
        final CollectionReference friendRequestCollection = FirebaseFirestore.getInstance().collection("Friend_Requests");
        friendRequestCollection.document(user.getUid()).set(selfFriendRequests, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            friendRequestCollection.document(uid).set(friendRequests, SetOptions.merge());
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
                            requests.remove(user.getUid());
                            requests.remove(uid);
                            Map<String, Object> selfRequestsList = new HashMap<>();
                            selfRequestsList.put("requests", requests);
                            friendRequestCollection.document(uid).set(selfRequestsList, SetOptions.merge());
                        }
                    }
                }
            }
        });

        friendRequestCollection.document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()){
                        List<String> requests = (ArrayList<String>) document.get("requests");
                        if (requests != null) {
                            requests.remove(uid);
                            requests.remove(user.getUid());
                            Map<String, Object> selfRequestsList = new HashMap<>();
                            selfRequestsList.put("requests", requests);
                            friendRequestCollection.document(user.getUid()).set(selfRequestsList, SetOptions.merge());
                        }
                    }
                }
            }
        });
    }

    public void changeFriend(final boolean adding, final String uid){
        final CollectionReference userRef = FirebaseFirestore.getInstance().collection("Users");

        userRef.document(user.getUid()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
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
                                        userRef.document(user.getUid()).set(selfFriendMap, SetOptions.merge())
                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()){
                                                            List<String> userFriends = (ArrayList<String>) userDocument.get("friends");
                                                            if (adding)
                                                                userFriends.add(user.getUid());
                                                            else
                                                                userFriends.remove(user.getUid());
                                                            Map<String, Object> userFriendMap = new HashMap<>();
                                                            userFriendMap.put("friends", userFriends);
                                                            userRef.document(uid).set(userFriendMap, SetOptions.merge());
                                                            finish();
                                                            startActivity(new Intent(FriendRequestList.this, FriendRequestList.class));
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
            startActivity(new Intent(this, FriendsList.class));
        }
        if (id == R.id.nav_search_users){
            startActivity(new Intent(this, Search.class));
        }
        if (id == R.id.nav_requests){
            onBackPressed();
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
