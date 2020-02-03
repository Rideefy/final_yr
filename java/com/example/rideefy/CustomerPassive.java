package com.example.rideefy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.security.Key;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class CustomerPassive extends AppCompatActivity implements ActivityConstants{

   private ListView listView;
   private ArrayList<String> arrayList;
   private ArrayAdapter<String> arrayAdapter;
   private User user;
   String mDatasnapshotValue [] = new String[50];
    int lengthOfSnapshot, count =0;
    String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_passive);


        user = new User();
        listView = (ListView)findViewById(R.id.listView);
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("driverSchedule");
        arrayList = new ArrayList<>();
        arrayAdapter = new ArrayAdapter<String>(CustomerPassive.this,R.layout.cust_list_view,R.id.CustTextView,arrayList);


        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){


                    for(DataSnapshot ds : dataSnapshot.getChildren()){

                        String m = ds.getKey();
                        mDatasnapshotValue[count++] = m;
                        user = ds.getValue(User.class);
                        arrayList.add("Name: "+user.getName().toString()+"\n"+"Contact: "+user.getPhone().toString()+"\n"+
                                        "Date: "+user.getDate().toString()+"\n"+"Time: "+user.getTime()+"\n"+"Source: "+user.getSource().toString()+"\n"+
                                        "Destination: "+user.getDest().toString()+"\n"+"Seat: "+user.getSeat());
                        Toast.makeText(CustomerPassive.this, mDatasnapshotValue[0]+""+mDatasnapshotValue[1], Toast.LENGTH_SHORT).show();

                    }

                    listView.setAdapter(arrayAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int pos = position;
                String driverId = mDatasnapshotValue[pos];
                Intent intent = new Intent(CustomerPassive.this, DriverRouteMap.class);
                intent.putExtra("calling-activity",ActivityConstants.CustomerPassive);
                intent.putExtra("driverId",driverId);
                startActivity(intent);

            }
        });

    }
}
