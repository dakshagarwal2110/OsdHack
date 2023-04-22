package com.collegemeet.android.osdhack;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.util.HashMap;
import java.util.Map;

public class EditDetails extends AppCompatActivity {

    RelativeLayout profilePictureChange;


    private RelativeLayout genderChangeFragment, nameChangeFragment, dateOfBirthChangeFragment, emailChangeFragment;
    public RelativeLayout editDetailsParent;
    private FrameLayout container_fragment_details;
    private ImageView cancelDetails;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference reference;
    public static final String TAG = "MyTag";
    private Uri profileImageUri;
    private ProgressDialog progressDialog;
    private RelativeLayout about;
    ActivityResultLauncher<String> mGetContent;
    String names = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_details);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        reference = FirebaseStorage.getInstance().getReference();


        progressDialog = new ProgressDialog(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.black));
        }
        progressDialog.setMessage("Profile image is updating...");


        profilePictureChange = findViewById(R.id.profile_picture_change_settings);

        profilePictureChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                mGetContent.launch("image/*");
            }

        });

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                Intent intent = new Intent(EditDetails.this, CropperActivity.class);
                intent.putExtra("Data", result.toString());
                startActivityForResult(intent, 101);

            }
        });


    }


    public Boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network nw = connectivityManager.getActiveNetwork();
            if (nw == null) {
                return false;
            }
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        } else {
            NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
            return nwInfo != null && nwInfo.isConnected();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri resultUri = null;

        if (resultCode == -1 && requestCode == 101) {
            String result = data.getStringExtra("Result");

            if (result != null) {
                resultUri = Uri.parse(result);

                progressDialog.setCancelable(false);
                progressDialog.show();
                SharedPreferences.Editor editor;
                SharedPreferences preferences;
                preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                editor = preferences.edit();


                String path = mAuth.getCurrentUser().getUid() + System.currentTimeMillis() + "." + getExtension(resultUri);
                StorageReference fileRef = reference.child("Profile images").child(path);

                //uploading photo in storage
                Uri finalResultUri = resultUri;
                fileRef.putFile(resultUri).addOnSuccessListener(taskSnapshot -> {

                    //getting token of profile image
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        profileImageUri = uri;


                        DocumentReference detailsReference = db.collection("users").document(mAuth.getCurrentUser().getUid()).collection("everything")
                                .document("details");

                        db.runTransaction(transaction -> {

                            transaction.update(detailsReference, "profile image", profileImageUri.toString());


                            return null;
                        }).addOnSuccessListener(o -> {

                            editor.putString(MainActivityContainingFragment.PROFILE_IMAGE, finalResultUri.toString());
                            editor.apply();
                            Toast.makeText(getApplicationContext(), "Profile image updated...", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            finish();

                        }).addOnFailureListener(e -> {

                            Toast.makeText(getApplicationContext(), "Error occurred!", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            finish();

                        });

                    });


                }).addOnFailureListener(e -> Toast.makeText(getApplicationContext(), "Image not updated", Toast.LENGTH_SHORT).show());

            }


        }


    }

    public String getExtension(Uri result) {
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(getApplicationContext().getContentResolver().getType(result));
    }
}