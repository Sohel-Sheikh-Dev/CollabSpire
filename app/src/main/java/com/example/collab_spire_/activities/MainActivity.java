package com.example.collab_spire_.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

//import com.example.collab_spire_.adapters.UsersAdapter;
import com.example.collab_spire_.adapters.UsersAdapter;
import com.example.collab_spire_.databinding.ActivityMainBinding;
//import com.example.collab_spire_.listeners.UsersListener;
//import com.example.collab_spire_.models.User;
import com.example.collab_spire_.listeners.UsersListener;
import com.example.collab_spire_.models.User;
import com.example.collab_spire_.utilities.Constants;
import com.example.collab_spire_.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements UsersListener {

    private PreferenceManager preferenceManager;
    ActivityMainBinding binding;
    private List<User> users;
    private UsersAdapter usersAdapter;
    private TextView textErrorMessage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageView imageConference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(getApplicationContext());
        imageConference = binding.imageConference;

        TextView textTitle = binding.textTitle;
        textTitle.setText(String.format(
                "%s %s",
                preferenceManager.getString(Constants.KEY_FIRST_NAME),
                preferenceManager.getString(Constants.KEY_LAST_NAME)
        ));

        binding.textSignOut.setOnClickListener(v -> signOut());

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                sendFCMTokenToDatabase(task.getResult().getToken());
            }
        });

        RecyclerView userRecyclerView = binding.usersRecyclerView;
        textErrorMessage = binding.textErrorMessage;


        users = new ArrayList<>();
        usersAdapter = new  UsersAdapter(users,this);
        userRecyclerView.setAdapter(usersAdapter);
        swipeRefreshLayout = binding.swipeRefreshlayout;
        swipeRefreshLayout.setOnRefreshListener(this::getUsers);
        getUsers();
    }

    private void getUsers() {
        swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS).get().addOnCompleteListener(task -> {
            swipeRefreshLayout.setRefreshing(false);
            String myUserId = preferenceManager.getString(Constants.KEY_USER_ID);
            if (task.isSuccessful() && task.getResult() != null) {
                users.clear();
                for (QueryDocumentSnapshot documentSnapshot : task.getResult()) {

                    if (myUserId.equals(documentSnapshot.getId())) {
                        continue;
                    }
                    User user = new User();
                    user.firstName = documentSnapshot.getString(Constants.KEY_FIRST_NAME);
                    user.lastName = documentSnapshot.getString(Constants.KEY_LAST_NAME);
                    user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                    user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                    users.add(user);
                }

                if (users.size() > 0) {
                    usersAdapter.notifyDataSetChanged();
                } else {
                    textErrorMessage.setText(String.format("%s", "No users available"));
                    textErrorMessage.setVisibility(View.VISIBLE);
                }

            } else {
                textErrorMessage.setText(String.format("%s", "No users available"));
                textErrorMessage.setVisibility(View.VISIBLE);
            }
        });
    }

    private void sendFCMTokenToDatabase(String token) {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
//                .addOnSuccessListener(aVoid -> Toast.makeText(MainActivity.this, "Token updated successfully", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to send token: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void signOut() {
        Toast.makeText(this, "Signing out...", Toast.LENGTH_SHORT).show();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clearPreferences();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Unable to sign out", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void initiateVideoMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(this, user.firstName + " " + user.lastName + " is not available for the meeting", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra("user", user);
            intent.putExtra("type", "video");
            startActivity(intent);

//            Toast.makeText(this, " Video meeting with " + user.firstName + " " + user.lastName, Toast.LENGTH_SHORT).show();

        }
    }

    @Override
    public void initiateAudioMeeting(User user) {
        if (user.token == null || user.token.trim().isEmpty()) {
            Toast.makeText(this, user.firstName + " " + user.lastName + " is not available for the meeting", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
            intent.putExtra("user",user);
            intent.putExtra("type","audio");
            startActivity(intent);

//            Toast.makeText(this, " Audio meeting with " + user.firstName + " " + user.lastName, Toast.LENGTH_SHORT).show();

        }

    }

    @Override
    public void onMultipleUserSelection(Boolean ismultipleUsersSelected) {
        if(ismultipleUsersSelected){
            imageConference.setVisibility(View.VISIBLE);
            imageConference.setOnClickListener(v -> {
                Intent intent = new Intent(getApplicationContext(), OutgoingInvitationActivity.class);
                intent.putExtra("selectedUsers",new Gson().toJson(usersAdapter.getSelectedusers()));
                intent.putExtra("type","video");
                intent.putExtra("isMultiple",true);
                startActivity(intent);
            });
        }else{
            imageConference.setVisibility(View.GONE);
        }
    }
}
