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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.HashMap;
import java.util.Map;

public class Login extends AppCompatActivity {

    private static final String TAG = "";
    private Toolbar rToolbar;
    private TextInputLayout email;
    private TextInputLayout password;
    private Button loginButton;
    private FirebaseAuth mAuth;
    private ProgressDialog rLogProgress;
    private FirebaseFirestore firestore;
    private CollectionReference users;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        rToolbar = (Toolbar) findViewById(R.id.loggingInToolbar);
        setSupportActionBar(rToolbar);
        getSupportActionBar().setTitle("Account Login");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mAuth = FirebaseAuth.getInstance();
        rLogProgress = new ProgressDialog(this);

        firestore = FirebaseFirestore.getInstance();
        users = firestore.collection("Users");

        email = (TextInputLayout) findViewById(R.id.lginemailInput);
        password = (TextInputLayout) findViewById(R.id.lginpasswdIn);
        loginButton = (Button) findViewById(R.id.logBtn);

        loginButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                String email_str = email.getEditText().getText().toString();
                String password_str = password.getEditText().getText().toString();
                if (!TextUtils.isEmpty(email_str) || !TextUtils.isEmpty(password_str)){
                    rLogProgress.setTitle("Logging in User");
                    rLogProgress.setCanceledOnTouchOutside(false);
                    rLogProgress.show();
                    loginUser(email_str, password_str, firestore);
                }
            }
        });
    }

    public void loginUser(String email, String password, final FirebaseFirestore firestore){

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull final Task<AuthResult> loginTask) {
                        if (loginTask.isSuccessful()) {
                            FirebaseInstanceId.getInstance().getInstanceId()
                                    .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<InstanceIdResult> task) {
                                            if (task.isSuccessful()) {
                                                Map<String, Object> userMap = new HashMap<>();
                                                userMap.put("deviceToken", task.getResult().getToken());
                                                firestore.collection("Users").document(loginTask.getResult().getUser().getUid())
                                                .set(userMap, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        if (task.isSuccessful()){
                                                            rLogProgress.dismiss();
                                                            Intent intent = new Intent(Login.this, Chat.class);
                                                            startActivity(intent);
                                                        }else{
                                                            rLogProgress.dismiss();
                                                            Toast.makeText(Login.this, "Failed to log in", Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });
                                            }else{
                                                rLogProgress.dismiss();
                                                Toast.makeText(Login.this, "Failed to log in", Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });
                        }else{
                            rLogProgress.hide();
                            Toast.makeText(Login.this, "Login failed. Check the form and try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });

    }
}
