package net.whispwriting.mantischat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class UsernameUpdate extends AppCompatActivity {

    private Toolbar toolbar;
    private TextInputLayout setUser;
    private Button setUserButton;
    private FirebaseFirestore database;
    private FirebaseUser currentUser;
    private ProgressDialog progressDialogue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username_update);
        toolbar = (Toolbar) findViewById(R.id.UserToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Account Name");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final String uid = currentUser.getUid();
        database = FirebaseFirestore.getInstance();

        setUser = (TextInputLayout) findViewById(R.id.setUser);
        setUserButton = (Button) findViewById(R.id.setUserButton);

        String user = getIntent().getStringExtra("name");
        setUser.getEditText().setText(user);

        progressDialogue = new ProgressDialog(this);

        setUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialogue.setTitle("Saving Changes");
                progressDialogue.setMessage("Please wait");
                progressDialogue.show();
                String username = setUser.getEditText().getText().toString();
                DocumentReference userDoc = database.collection("Users").document(uid);
                Map<String, Object> newName = new HashMap<>();
                newName.put("name", username);
                userDoc.set(newName, SetOptions.merge()).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            progressDialogue.dismiss();
                            Intent accountSettings = new Intent(UsernameUpdate.this, AccountSettings.class);
                            startActivity(accountSettings);
                        }else{
                            Toast.makeText(getApplicationContext(), "Saving failed. Check the form and try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });


    }
}