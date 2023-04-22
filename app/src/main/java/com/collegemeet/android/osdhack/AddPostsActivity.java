package com.collegemeet.android.osdhack;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddPostsActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "MyTag";
    private static final String TAGO = "MyTago";
    public static final String CAPTION_MAP_KEY = "caption";
    public static final String POST_URI_KEY = "post_uri";
    public static final String TYPE_OF_POST = "type_of_post";
    public static final String UID_OF_TAGGED_TO_WORKER_CLASS = "uid_of_tagged_to_worker_class";
    public static final String YOTAL_TAGGED_USERS = "yotal_tagged_users";
    FirebaseAuth mAuth;
    ImageView cancel;
    EditText caption;
    Button browse, upload, tag;
    ImageView imagePost;
    VideoView videoPost;
    String postType;
    Uri postUri, oldPostUri;
    ProgressDialog progressDialog;
    public static final String PROGRESS_STRING = "progress";
    public static final String COMPLETED_STRING = "completed";
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    StorageReference storage;
    FirebaseFirestore fireStore;

    public static final String POST_UPLOAD = "post_uploaded";
    public static final String PATH_POST = "path";
    public static final String PATH_POST_STORAGE = "storage_path";


    Intent intent;
    ScrollView layout;

    ArrayList<String> uidsFetched, namesFetched;
    int number_of_tagged;
    TextView namesTaggedTv  ,heading;
    String names = "";
    ActivityResultLauncher<String> mGetContent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_posts);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.black));
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("detailsTaggedUsers"));
        initViews();
        fireStore = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        uidsFetched = new ArrayList<>();
        namesFetched = new ArrayList<>();
        number_of_tagged = 0;
        namesTaggedTv = findViewById(R.id.taggedUsersNamesTv);
        heading = findViewById(R.id.PostTextView);


        if(preferences.getString("newEvent" , "").equals("newOne")){
            heading.setText("Add event");

        }


        storage = FirebaseStorage.getInstance().getReference();
        editor.putString(PROGRESS_STRING, "0% uploaded");
        editor.putString(COMPLETED_STRING, "Not done");
        editor.apply();
        tag = findViewById(R.id.AddTagsButton);



        //start deleting post



        //end deleting post

        intent = getIntent();

        if (intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE).equals("photo")) {
            videoPost.setVisibility(View.GONE);
            postType = "image/*";
        } else if (intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE).equals("reels")) {
            imagePost.setVisibility(View.GONE);
            postType = "video/*";
        }

        cancel.setOnClickListener(v -> finish());

        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyBoard(v);
                mGetContent.launch("image/*");
            }

        });

        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri result) {
                Intent intent = new Intent(AddPostsActivity.this , CropperActivity.class);
                intent.putExtra("Data" , result.toString());
                startActivityForResult(intent , 101);

            }
        });

        upload.setOnClickListener(v -> {

            hideSoftKeyBoard(v);

            if (isNetworkAvailable(this)) {
                if (postUri != null) {

                    if (intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE).equals("reels")) {
                        long size = (getFileSize(postUri) / (1024 * 1024));
                        if (size > 3) {
                            Snackbar snackbar = Snackbar
                                    .make(layout, "Maximum size of Reel can be 3 Mb, you can compress it online.", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }
                    } else if (intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE).equals("photo")) {

                        if (isNetworkAvailable(AddPostsActivity.this)) {
                            if (caption.getText().toString().trim().equals("")) {
                                Snackbar snackbar = Snackbar
                                        .make(layout, "Please add a caption to your image", Snackbar.LENGTH_LONG);
                                snackbar.show();
                            } else {
                                startUploadingImage();
                            }

                        } else {
                            Snackbar snackbar = Snackbar
                                    .make(layout, "Network error!", Snackbar.LENGTH_LONG);
                            snackbar.show();
                        }

                    }

                } else {
                    Snackbar snackbar = Snackbar
                            .make(layout, "Choose a file to post", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }

            } else {
                Snackbar snackbar = Snackbar
                        .make(layout, "Network error!", Snackbar.LENGTH_LONG);
                snackbar.show();
            }

        });




//        upload.setOnClickListener(v -> {
//
//            hideSoftKeyBoard(v);
//
//            if (isNetworkAvailable(this)) {
//                if (postUri != null) {
//
//                    if (intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE).equals("reels")) {
//                        long size = (getFileSize(postUri) / (1024 * 1024));
//                        if (size > 3) {
//                            Snackbar snackbar = Snackbar
//                                    .make(layout, "Maximum size of Reel can be 3 Mb, you can compress it online.", Snackbar.LENGTH_LONG);
//                            snackbar.show();
//                        } else {
//                            if (caption.getText().toString().equals("")) {
//                                Snackbar snackbar = Snackbar
//                                        .make(layout, "Add a caption to your reel", Snackbar.LENGTH_LONG);
//                                snackbar.show();
//                            } else {
//                                startUploadingVideoReel();
//                            }
//                        }
//                    } else if (intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE).equals("photo")) {
//
//                        if (isNetworkAvailable(AddPostsActivity.this)) {
//                            long size = (getFileSize(oldPostUri) / (1024 * 1024));
//                            if (size > 10) {
//                                Snackbar snackbar = Snackbar
//                                        .make(layout, "Maximum size of image can be 10 Mb", Snackbar.LENGTH_LONG);
//                                snackbar.show();
//                            } else {
//                                if (caption.getText().toString().trim().equals("")) {
//                                    Snackbar snackbar = Snackbar
//                                            .make(layout, "Add a caption to your image", Snackbar.LENGTH_LONG);
//                                    snackbar.show();
//                                } else {
//                                    startUploadingImage();
//                                }
//                            }
//
//                        } else {
//                            Snackbar snackbar = Snackbar
//                                    .make(layout, "Network error!", Snackbar.LENGTH_LONG);
//                            snackbar.show();
//                        }
//
//                    }
//
//                } else {
//                    Snackbar snackbar = Snackbar
//                            .make(layout, "Choose a file to post", Snackbar.LENGTH_LONG);
//                    snackbar.show();
//                }
//
//            } else {
//                Snackbar snackbar = Snackbar
//                        .make(layout, "Network error!", Snackbar.LENGTH_LONG);
//                snackbar.show();
//            }
//
//        });

    }

    public BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            uidsFetched = intent.getStringArrayListExtra("uid_of_tagged_user");
            number_of_tagged = intent.getIntExtra("total_number_of_tags", 0);
            namesFetched = intent.getStringArrayListExtra("names_of_tagged_users_list");
            int number_of_tagged_names = intent.getIntExtra("total_number_of_names", 0);

            Toast.makeText(AddPostsActivity.this, number_of_tagged + " users tagged", Toast.LENGTH_SHORT).show();
            if (number_of_tagged != 0) {
                namesTaggedTv.setVisibility(View.VISIBLE);
                for (int i = 0; i < number_of_tagged_names; i++) {
                    names = names + namesFetched.get(i) + " \n";
                }
                namesTaggedTv.setText(names);
            }
        }
    };


    private void startUploadingImage() {
        progressDialog.setTitle("Uploading post");
        progressDialog.setCancelable(false);
        progressDialog.setMessage(preferences.getString(PROGRESS_STRING, ""));
        progressDialog.show();


        Map<String, Object> postImageDetails = new HashMap<>();



        postImageDetails.put(CAPTION_MAP_KEY, caption.getText().toString());
        postImageDetails.put(POST_URI_KEY, postUri.toString());
        postImageDetails.put(TYPE_OF_POST, intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE));
        if (number_of_tagged != 0) {
            String[] l = uidsFetched.toArray(new String[uidsFetched.size()]);

            postImageDetails.put(UID_OF_TAGGED_TO_WORKER_CLASS, l);
            postImageDetails.put(YOTAL_TAGGED_USERS, number_of_tagged);

        } else {
            postImageDetails.put(UID_OF_TAGGED_TO_WORKER_CLASS, "no_tagged");
            postImageDetails.put(YOTAL_TAGGED_USERS, 0);
        }







        Data dataPost = new Data.Builder().putAll(postImageDetails).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PostUploadWorkerClass.class)
                .setInputData(dataPost)
                .build();

        WorkManager workManager = WorkManager.getInstance(AddPostsActivity.this);
        workManager.enqueue(request);


    }


    private void startUploadingVideoReel() {


        progressDialog.setTitle("Uploading post...");
        progressDialog.setCancelable(false);
        progressDialog.setMessage(preferences.getString(PROGRESS_STRING, ""));
        progressDialog.show();


        Map<String, Object> postVideoReelDetails = new HashMap<>();

        postVideoReelDetails.put(CAPTION_MAP_KEY, caption.getText().toString());
        postVideoReelDetails.put(POST_URI_KEY, postUri.toString());
        postVideoReelDetails.put(TYPE_OF_POST, intent.getStringExtra(ProfileFragment.POST_TYPE_PREFERENCE));


        Data dataPost = new Data.Builder().putAll(postVideoReelDetails).build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PostUploadWorkerClass.class)
                .setInputData(dataPost)
                .build();

        WorkManager workManager = WorkManager.getInstance(AddPostsActivity.this);
        workManager.enqueue(request);


    }


    private void initViews() {
        cancel = findViewById(R.id.cancel_post_activity);
        caption = findViewById(R.id.postCaptionEditText);
        browse = findViewById(R.id.PostBrowseButton);
        upload = findViewById(R.id.PostUploadButton);
        imagePost = findViewById(R.id.image_post_image_view);
        videoPost = findViewById(R.id.video_post_video_view);
        layout = findViewById(R.id.Add_post_parentLayout);
        progressDialog = new ProgressDialog(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = preferences.edit();

    }

    public long getFileSize(Uri file) {
        Cursor returnCursor = getContentResolver().
                query(file, null, null, null, null);
        int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
        returnCursor.moveToFirst();

        long size = returnCursor.getLong(sizeIndex);
        returnCursor.close();

        return size;
    }

    private void hideSoftKeyBoard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
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
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PROGRESS_STRING)) {

            progressDialog.setMessage(preferences.getString(PROGRESS_STRING, ""));
            if ((preferences.getString(PROGRESS_STRING, "")).equals("100% uploaded")) {
                progressDialog.setMessage("Please wait...");
            }

        } else if (key.equals(COMPLETED_STRING)) {

            if (preferences.getString(COMPLETED_STRING, "").equals("done")) {
                caption.setText("");
                imagePost.setVisibility(View.GONE);
                videoPost.setVisibility(View.GONE);
                postUri = null;
                names = "";
                uidsFetched.clear();
                namesFetched.clear();
                namesTaggedTv.setText(names);
                number_of_tagged=0;
                progressDialog.dismiss();
                editor.putString(PROGRESS_STRING, "0% uploaded");
                editor.putString(COMPLETED_STRING, "Not done");
                editor.apply();
                Snackbar snackbar = Snackbar
                        .make(layout, "Post Uploaded", Snackbar.LENGTH_LONG);
                snackbar.show();

            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);

        editor.remove("newEvent");
        editor.apply();

        names = "";
        uidsFetched.clear();
        namesFetched.clear();
        namesTaggedTv.setText(names);
        number_of_tagged=0;
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode ==-1 && requestCode ==101){
            String result = data.getStringExtra("Result");
            Uri resultUri = null;
            if(result != null){
                resultUri = Uri.parse(result);
                postUri = resultUri;
            }

            imagePost.setVisibility(View.VISIBLE);
            imagePost.setImageURI(resultUri);

        }


    }


}