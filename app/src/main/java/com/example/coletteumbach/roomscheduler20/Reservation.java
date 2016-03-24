package com.example.coletteumbach.roomscheduler20;

/**
 * Created by ColetteUmbach on 3/15/16.
 */

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.app.DatePickerDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ListView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;


/**
 * Created by ColetteUmbach on 3/5/16.
 */
public class Reservation extends FragmentActivity {

    static EditText edtdate;
    static EditText edtstarttime;
    static EditText edtendtime;
    static EditText edtroom;
    static EditText edtevent;
    static Button reserveBtn;

    static ArrayList<String> availRooms;
    static String listTitle = "Select a room:";

    static String date;
    static String startT;
    static String endT;
    static String room;
    static String email;
    static int rfid;


    ProgressBar pbbar;

    ConnectionClass connectionClass;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reservation);



        edtdate = (EditText) findViewById(R.id.edtdate);
        edtdate.setInputType(InputType.TYPE_NULL);
        edtstarttime = (EditText) findViewById(R.id.edtstarttime);
        edtstarttime.setInputType(InputType.TYPE_NULL);
        edtendtime = (EditText) findViewById(R.id.edtendtime);
        edtendtime.setInputType(InputType.TYPE_NULL);
        edtroom = (EditText)findViewById(R.id.edtroom);
        edtroom.setInputType(InputType.TYPE_NULL);
        edtevent = (EditText)findViewById(R.id.edtevent);
        reserveBtn = (Button) findViewById(R.id.reserveButton);
        pbbar = (ProgressBar) findViewById(R.id.pbbar);
        pbbar.setVisibility(View.GONE);

        reserveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AddReservation add = new AddReservation();
                add.execute("");

            }
        });

        //get email used during login and associated RFID
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            CharSequence emailChars = extras.getCharSequence("loginEmail");
            if (emailChars != null) {
                email = emailChars.toString();
            }
            rfid = extras.getInt("usrRFID");
        }

        connectionClass = new ConnectionClass();

    }



    protected ArrayList<String> getAvailRooms(){
        boolean isSuccess = false;
        int i = 0;



        date = edtdate.getText().toString();
        startT = edtstarttime.getText().toString();
        endT = edtendtime.getText().toString();
        String evtStart = date + " " + startT;
        String evtEnd = date + " " + endT;

        try {
            System.out.println("starting...");
            Connection con = connectionClass.CONN();
            if (con == null) {
                availRooms.add("Error in connection with SQL server");
            }
            else if ((date.trim().equals("")) || (startT.trim().equals("")) || (endT.trim().equals(""))) {
                availRooms.add("Please select a date and time first");
            }
            else if (endT.compareTo(startT)<= 0){
                availRooms.add("start time is later than end time");
            }
            else {
                String query = "SELECT R.Room " +
                        "FROM tbl_Rooms AS R " +
                        "LEFT JOIN tbl_Events AS E " +
                        "ON R.Room = E.Room " +
                        "WHERE E.Room IS NULL" +
                        " OR E.StartTime > '" + evtEnd +
                        "' OR E.EndTime < '" + evtStart + "'";
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                while(rs.next()){
                    String row = rs.getString("Room");
                    availRooms.add(row);
                    isSuccess=true;
                }
                if(!isSuccess)
                {
                    availRooms.add("No results");
                }

            }
        }
        catch (Exception ex)
        {
            availRooms.add("Exceptions");
        }

        return availRooms;
    }

    public void showDatePickerDialog(View v) {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new DatePickerFragment();
        dialog.show(getSupportFragmentManager(), "datePicker");
    }

    Handler h_StartTime = new Handler(){
        @Override
        public void handleMessage(Message msg){
            edtstarttime.setText(msg.getData().getString("starttime"));
        }
    };

    public void showStartTimePickerDialog(View v){
        DialogFragment dialog = new TimePickerFragment(h_StartTime,"starttime");
        dialog.show(getSupportFragmentManager(), "timePicker");
    }

    Handler h_EndTime = new Handler(){
        @Override
        public void handleMessage(Message msg){
            edtendtime.setText(msg.getData().getString("endtime"));
        }
    };

    public void showEndTimePickerDialog(View v){
        DialogFragment dialog = new TimePickerFragment(h_EndTime,"endtime");
        dialog.show(getSupportFragmentManager(), "timePicker");
    }

    public void showRoomPickerDialog(View view) {
        availRooms = new ArrayList<String>(); //reset rooms list every time the box is clicked
        DialogFragment dialog = new ListViewFragment(getAvailRooms());
        dialog.show(getSupportFragmentManager(), "roomPicker");
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        public DatePickerFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }

        public void onDateSet(DatePicker view, int year, int month, int day) {
            //Note: android starts counting months at 0, so add 1 to get expected value
            edtdate.setText(new StringBuilder().append(month+1).append("/")
                    .append(day).append("/").append(year));
        }
    }

    public static class TimePickerFragment extends DialogFragment{

        Handler h;
        String timeStr;
        DecimalFormat twoDig = new DecimalFormat("00");

        public TimePickerFragment(Handler arg_h, String outputStr){
            h = arg_h;
            timeStr = outputStr;
        }

        private TimePickerDialog.OnTimeSetListener callback = new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString(timeStr, twoDig.format(hourOfDay) + ":" + twoDig.format(minute));
                msg.setData(data);
                h.sendMessage(msg);
            }
        };

        public Dialog onCreateDialog(Bundle bundle){
            int hourOfDay = 12;
            int minute = 20;
            boolean is24HourView = false;
            TimePickerDialog tpdialog = new TimePickerDialog(getActivity(), callback, hourOfDay, minute, is24HourView);
            return tpdialog;
        }
    }

    public static class ListViewFragment extends DialogFragment implements
            AdapterView.OnItemClickListener {

        ArrayList<String> listitems;

        ListView mylist;

        public ListViewFragment(ArrayList<String> list){
            listitems = list;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.list_fragment, null, false);
            mylist = (ListView) view.findViewById(R.id.list);

            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            return view;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {

            super.onActivityCreated(savedInstanceState);

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                    android.R.layout.simple_list_item_1, listitems);

            mylist.setAdapter(adapter);

            mylist.setOnItemClickListener(this);

        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                                long id) {

            dismiss();
            //Toast.makeText(getActivity(), listitems[position], Toast.LENGTH_SHORT).show();
            edtroom.setText(new StringBuilder().append(listitems.get(position)));
        }

    }

    public class AddReservation extends AsyncTask<String, String, String> {



        String z = "";
        Boolean isSuccess = false;

        String date = edtdate.getText().toString();
        String startT = edtstarttime.getText().toString();
        String endT = edtendtime.getText().toString();
        String room = edtroom.getText().toString();
        String evtName = edtevent.getText().toString();

        @Override
        protected void onPreExecute() {
            pbbar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(String r) {
            pbbar.setVisibility(View.GONE);
            Toast.makeText(Reservation.this, r, Toast.LENGTH_SHORT).show();

        }

        @Override
        protected String doInBackground(String... params) {
            if (date.trim().equals(""))
                z = "Please select a date";
            else if (startT.trim().equals(""))
                z = "Please select a start time";
            else if (endT.trim().equals(""))
                z = "Please select an end time";
            else if (evtName.trim().equals(""))
                z = "Please enter your group's name";
            else {
                try {
                    Connection con = connectionClass.CONN();
                    if (con == null) {
                        z = "Error in connection with server";
                    } else {

                        //String dates = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH)
                        //        .format(Calendar.getInstance().getTime());
                        String query = "insert into tbl_Events (RFID,Event,Approved,Room,StartTime,EndTime) values"
                                +"('" + rfid + "','" + evtName +  "','" +  false  + "','"
                                + room  + "','" + date + " " + startT + "','" + date + " " + endT + "')";
                        PreparedStatement preparedStatement = con.prepareStatement(query);
                        preparedStatement.executeUpdate();
                        z = "Reserved successfully!";
                        isSuccess = true;
                    }
                } catch (Exception ex) {
                    isSuccess = false;
                    z = "Exceptions";
                }
            }
            return z;
        }
    }

}
