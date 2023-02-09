package net.whispwriting.mantischat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class Registration extends AppCompatActivity {

    private TextInputLayout username;
    private TextInputLayout email;
    private TextInputLayout password;
    private Button regBtn;
    private FirebaseAuth mAuth;
    private Toolbar rToolbar;
    private ProgressDialog rRegProgress;
    private DatabaseReference cDatabase;
    private FirebaseFirestore firestore;
    private CollectionReference users;
    private DocumentReference userRef;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_registration);
        rToolbar = (Toolbar) findViewById(R.id.register_toolbar);
        setSupportActionBar(rToolbar);
        getSupportActionBar().setTitle("Account Registration");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firestore = FirebaseFirestore.getInstance();
        users = firestore.collection("Users");

        rRegProgress = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        username = (TextInputLayout) findViewById(R.id.unameIn);
        email = (TextInputLayout) findViewById(R.id.emailInput);
        password = (TextInputLayout) findViewById(R.id.passwdIn);
        regBtn = (Button) findViewById(R.id.regBtn);

        regBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String username_str = username.getEditText().getText().toString();
                String email_str = email.getEditText().getText().toString();
                String password_str = password.getEditText().getText().toString();
                if (!TextUtils.isEmpty(username_str) || !TextUtils.isEmpty(email_str) || !TextUtils.isEmpty(password_str)) {
                    rRegProgress.setTitle("Registering User");
                    rRegProgress.setCanceledOnTouchOutside(false);
                    rRegProgress.show();
                    register_user(username_str, email_str, password_str);
                }

            }
        });
    }

    private void register_user(final String usernameStr, final String emailStr, final String passwordStr) {

    mAuth.createUserWithEmailAndPassword(emailStr, passwordStr).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            if (task.isSuccessful()) {
                final FirebaseUser curretnUser = FirebaseAuth.getInstance().getCurrentUser();
                String uid = curretnUser.getUid();
                userRef = firestore.collection("Users").document(uid);
                FirebaseInstanceId.getInstance().getInstanceId()
                        .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                            @Override
                            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                if (task.isSuccessful()){
                                    HashMap<String, Object> userMap = new HashMap<>();
                                    userMap.put("name", usernameStr);
                                    userMap.put("status", "Hi, I haven't set my status yet.");
                                    userMap.put("image", "default");
                                    userMap.put("thumb_image", "default");
                                    userMap.put("deviceToken", task.getResult().getToken());
                                    userMap.put("user_id", curretnUser.getUid());
                                    List<String> friends = new ArrayList<>();
                                    userMap.put("friends", friends);

                                    userRef.set(userMap).addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull Task task) {

                                            if (task.isSuccessful()){
                                                rRegProgress.dismiss();
                                                Intent chatload = new Intent(Registration.this, Chat.class);
                                                startActivity(chatload);
                                                finish();
                                            }

                                        }
                                    });
                                }
                            }
                        });

            } else {
                rRegProgress.hide();
                Toast.makeText(Registration.this, "Unable to register. Check the form and try again.", Toast.LENGTH_LONG).show();
            }
        }
    });
    }
}