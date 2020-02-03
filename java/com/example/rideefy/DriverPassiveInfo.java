package com.example.rideefy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.format.Time;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DriverPassiveInfo extends AppCompatActivity {

    private EditText mName, mPhone, mSeat;
    private TextView mDate, mTime,mSource,mDest;
    private TimePickerDialog mTimePickerDialog;
    private TimePickerDialog.OnTimeSetListener listener;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private Button mSave, mRoute;
    String date, time , source;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_passive_info);

        mName = (EditText) findViewById(R.id.driName);
        mPhone = (EditText) findViewById(R.id.driContact);
        mSource = (TextView) findViewById(R.id.driSource);
        mDest = (TextView) findViewById(R.id.driDest);
        mDate = (TextView) findViewById(R.id.driDate);
        mTime = (TextView) findViewById(R.id.driTime);
        mSeat = (EditText) findViewById(R.id.driSeat);
        mSave = (Button) findViewById(R.id.driSave);
        mRoute = (Button) findViewById(R.id.driRoute);

        mSource.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(DriverPassiveInfo.this,DriverRouteMap.class);
                startActivityForResult(intent,102);

            }
        });



        mDest.setOnClickListener(new View.OnClickListener() {
           @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverPassiveInfo.this, DriverRouteMap.class);
                String dest = mDest.getText().toString().toString();
                intent.putExtra("dest",dest);
                startActivity(intent);
            }
        });



        mDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month  = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(DriverPassiveInfo.this,android.R.style.Widget_Material_ActionBar_Solid,mDateSetListener,
                        year,month,day );
                datePickerDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.GREEN));
                datePickerDialog.show();
            }
        });

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month+=1;
                 date = year+"/"+month+"/"+dayOfMonth;
                mDate.setText(date);
            }
        };

        mTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Time time = null;
                try {
                    time = Time.class.newInstance();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                }
                int hourOfDay = time.hour;
                int minute = time.minute;
                boolean is24Hour = true;

                mTimePickerDialog = new TimePickerDialog(DriverPassiveInfo.this, listener, hourOfDay, minute, is24Hour);
                mTimePickerDialog.getWindow();
                mTimePickerDialog.show();

            }
        });

        listener = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                time = hourOfDay+":"+minute;
                mTime.setText(time);
            }
        };




        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mName.getText().toString().trim();
                String phone = mPhone.getText().toString().trim();
                //String source = mSource.getText().toString().trim();
                String dest = mDest.getText().toString().trim();
                //String date = mDate.getText().toString().trim();
               // String time = mTime.getText().toString().trim();
                String seat = mSeat.getText().toString().trim();

                String driverId = FirebaseAuth.getInstance().getUid();
                DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference("driverSchedule").child(driverId);

                HashMap map = new HashMap();
                map.put("Name", name);
                map.put("Phone", phone);
                map.put("Source", source);
                map.put("Dest", dest);
                map.put("Date", date);
                map.put("Time", time);
                map.put("Seat", seat);

                driverRef.updateChildren(map).addOnSuccessListener(new OnSuccessListener() {
                    @Override
                    public void onSuccess(Object o) {
                        Toast.makeText(DriverPassiveInfo.this, "Info Saved!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DriverPassiveInfo.this, "error " + e, Toast.LENGTH_SHORT).show();
                    }
                });


            }
        });

        mRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DriverPassiveInfo.this,DriverRouteMap.class);
                startActivity(intent);
                //DriverRouteMap routeMap;
               // routeMap.showRoute();
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==102){
            source = data.getStringExtra("source");
            mSource.setText(source);

        }
    }
}
