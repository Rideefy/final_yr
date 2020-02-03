package com.example.rideefy;

public class User {

    private String Name, Phone, Date, Time, Source, Dest, Seat;

    public User() {
    }

    public User(String name, String phone, String date, String time, String source, String dest, String seat) {
        Name = name;
        Phone = phone;
        Date = date;
        Time = time;
        Source = source;
        Dest = dest;
        Seat = seat;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public String getTime() {
        return Time;
    }

    public void setTime(String time) {
        Time = time;
    }

    public String getSource() {
        return Source;
    }

    public void setSource(String source) {
        Source = source;
    }

    public String getDest() {
        return Dest;
    }

    public void setDest(String dest) {
        Dest = dest;
    }

    public String getSeat() {
        return Seat;
    }

    public void setSeat(String seat) {
        Seat = seat;
    }
}
