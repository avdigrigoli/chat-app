package net.whispwriting.mantischat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class Search extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private RecyclerView usersListPage;
    private FirebaseFirestore usersDatabase;
    private CircleImageView userListImg;
    private FloatingActionButton searchButton;
    private GoogleAd ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        Toolbar usersToolbar = findViewById(R.id.UserListToolbar);
        setSupportActionBar(usersToolbar);
        getSupportActionBar().setTitle("");

        usersDatabase = FirebaseFirestore.getInstance();

        usersListPage = (RecyclerView) findViewById(R.id.UserListPage);
        usersListPage.setHasFixedSize(true);
        usersListPage.setLayoutManager(new LinearLayoutManager(this));
        usersListPage.getRecycledViewPool().setMaxRecycledViews(0, 0);

        userListImg = (CircleImageView) findViewById(R.id.userListImg);
        searchButton = (FloatingActionButton) findViewById(R.id.searchButton);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, usersToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText searchText = (EditText) findViewById(R.id.searchText);
                searchUsers(searchText.getText().toString());
            }
        });
    }

    protected void searchUsers(String name) {
        Query query = usersDatabase.collection("Users");
        FirestoreRecyclerOptions<User> options = new FirestoreRecyclerOptions.Builder<User>().setQuery(query.orderBy("name").startAt(name).endAt(name + "~"), User.class).build();
        FirestoreRecyclerAdapter<User, UsersViewHolder> adapter = new FirestoreRecyclerAdapter<User, UsersViewHolder>(options) {
            @Override
            public UsersViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.single_user_layout, parent, false);
                return new UsersViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull UsersViewHolder usersViewHolder, int i, @NonNull User users) {
                usersViewHolder.setName(users.name);
                usersViewHolder.setStatus(users.status);
                usersViewHolder.setImg(users.image);

                final String userID = getSnapshots().getSnapshot(i).getId();
                usersViewHolder.mView.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View view){
                        Intent profilePage = new Intent(Search.this, ProfileActivity.class);
                        profilePage.putExtra("userID", userID);
                        startActivity(profilePage);
                    }
                });
            }

        };
        usersListPage.setAdapter(adapter);
        adapter.startListening();
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
            Picasso.get().load(image).placeholder(R.drawable.avatar).into(userImageView);
        }
    }
}
