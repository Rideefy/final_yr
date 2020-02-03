package com.example.rideefy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.multidex.MultiDex;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CustomerSettings extends AppCompatActivity {


    private EditText etNameField, etPhoneField;
    private Button btnConfirm, btnBack;
    private FirebaseAuth mAuth;
    private DatabaseReference mCustomerDatabase;
    private String mName, mPhone, userID, mProfileUrl;
    private ImageView mImageView;
    private Uri resultUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MultiDex.install(this);

        setContentView(R.layout.activity_customer_settings);

        etNameField = (EditText)findViewById(R.id.etName);
        etPhoneField = (EditText)findViewById(R.id.etPhone);
        btnConfirm = (Button)findViewById(R.id.btnConfirm);
        btnBack = (Button)findViewById(R.id.btnBack);

        mAuth = FirebaseAuth.getInstance();
        userID = mAuth.getCurrentUser().getUid();
        mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userID);

        getuserInfo();

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                saveUserInfo();

            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                finish();
                return;

            }
        });

        mImageView = (ImageView)findViewById(R.id.ivProfile);
        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 1);

            }
        });

    }

    private void getuserInfo()
    {
        mCustomerDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if(dataSnapshot.exists() && dataSnapshot.getChildrenCount()>0)
                {
                    Map<String, Object> map= (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("Name")!=null)
                    {
                        mName=map.get("Name").toString();
                        etNameField.setText(mName);
                    }
                    if(map.get("Phone")!=null)
                    {
                        mPhone=map.get("Phone").toString();
                        etPhoneField.setText(mPhone);
                    }
                    if(map.get("profileImageUrl")!=null)
                    {
                       mProfileUrl = map.get("profileImageUrl").toString();
                        Glide.with(getApplication()).load(mProfileUrl).into(mImageView);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void saveUserInfo()
    {
        mName = etNameField.getText().toString();
        mPhone = etPhoneField.getText().toString();

        Map userInfo = new HashMap();
        userInfo.put("Name", mName);
        userInfo.put("Phone", mPhone);
        mCustomerDatabase.updateChildren(userInfo, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                Toast.makeText(CustomerSettings.this, "success", Toast.LENGTH_SHORT).show();
            }
        });

        if(resultUri != null)
        {
            final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userID);
            Bitmap bitmap = null;

            try {
                bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),resultUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG , 20, baos);

            byte[] data = baos.toByteArray();
            UploadTask uploadTask = filePath.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                    Toast.makeText(CustomerSettings.this, "Failure", Toast.LENGTH_SHORT).show();
                    finish();
                    return;

                }
            });

            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                    filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUrl = uri;
                            Map newImage = new HashMap();
                            newImage.put("profileImageUrl", downloadUrl.toString());
                            mCustomerDatabase.updateChildren(newImage);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CustomerSettings.this, "Exception "+e, Toast.LENGTH_SHORT).show();
                        }
                    });

                   /*Task<Uri> downloadUrl = taskSnapshot.getStorage().getStorage().getReference().getDownloadUrl();
                   //DatabaseReference userID = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers");
                   Map newImage = new HashMap();
                   newImage.put("profileImageUrl", downloadUrl.toString());
                   mCustomerDatabase.updateChildren(newImage);*/

                   finish();
                   return;

                }
            });
        }
        else
        {
            finish();
        }

        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1 && resultCode== Activity.RESULT_OK)
        {
            final Uri imageUri = data.getData();
            resultUri = imageUri;
            mImageView.setImageURI(resultUri);
        }

    }
}
