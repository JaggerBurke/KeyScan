package com.KCAssets.KeyScan;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;
import org.json.JSONArray;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CalibrationActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {


    /***********************************************************
     * Initializations
     **********************************************************/
    private Button scanBtn;
    private Button inputBtn;
    private ListView scannedListView;
    private ScannedItemAdapter scannedListAdapter;
    private List<String> scannedList;
    private Button finishBtn;
    String sheetID = "1SMbK5i-QeR9aCZYSb5IfeXxfsNYhHwTkWRzrRTEticQ";
    String personName;
    String formattedDate;
    String calType;
    String accessToken;


    JSONArray jsonArray;
    ArrayList<String> listID = new ArrayList<String>();
    ArrayList<String> listDescript = new ArrayList<String>();
    ArrayList<String> listManufact = new ArrayList<String>();
    ArrayList<String> listModel = new ArrayList<String>();
    ArrayList<String> listSerial = new ArrayList<String>();
    ArrayList<String> listCalDate = new ArrayList<String>();
    ArrayList<String> scannedIDs = new ArrayList<>();
    ArrayList<String> listCalDateF = new ArrayList<String>();
    ArrayList<String> listCategory = new ArrayList<String>();
    ArrayList<String> listDivision = new ArrayList<String>();
    ProgressDialog progressDialog;


    /***********************************************************
     * OnCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /***********************************************************
         * Token Check and Local Initializations
         **********************************************************/
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if(account!=null) {
            personName = account.getDisplayName();
        }

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        formattedDate = dateFormat.format(currentDate);

        RelativeLayout bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setVisibility(View.GONE);

        ImageView keylogo = findViewById(R.id.keylogo);
        keylogo.setVisibility(View.GONE);

        Button clearBtn = findViewById(R.id.clearBtn);
        clearBtn.setVisibility(View.GONE);

        ImageView backArrow = findViewById(R.id.backArrow);
        TextView title = findViewById(R.id.title);

        scanBtn = findViewById(R.id.scanBtn);
        scannedListView = findViewById(R.id.scannedList);
        finishBtn = findViewById(R.id.finishBtn);
        inputBtn = findViewById(R.id.inputBtn);

        finishBtn.setText("Submit");

        SharedPreferences prefs = getSharedPreferences("IDs", Context.MODE_PRIVATE);

        scannedList = new ArrayList<>();
        scannedListAdapter = new ScannedItemAdapter(this, scannedList, prefs);
        scannedListView.setAdapter(scannedListAdapter);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);
        Log.d("CalibrationActivity", "Access Token: " + accessToken);

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();


        /***********************************************************
         * Spinner
         **********************************************************/
        Spinner spinnerCal = findViewById(R.id.spinnerCal);
        ArrayAdapter<CharSequence> adapterCal = ArrayAdapter.createFromResource(this,
                R.array.cal_options, android.R.layout.simple_spinner_item);
        adapterCal.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCal.setAdapter(adapterCal);
        spinnerCal.setOnItemSelectedListener(this);


        /***********************************************************
         * Button Clicks
         **********************************************************/
        scanBtn.setOnClickListener(v -> scanCode());
        inputBtn.setOnClickListener(v -> showManualInputDialog());

        finishBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!scannedList.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CalibrationActivity.this);
                    builder.setTitle("Confirmation");
                    builder.setMessage("Are you sure you want to submit calibration info?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            performExcelLookup();
                        }
                    });
                    builder.setNegativeButton("No", null);
                    builder.show();
                } else {
                    Toast.makeText(CalibrationActivity.this, "Error: Asset List is Empty", Toast.LENGTH_LONG).show();
                }
            }
        });

        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!scannedList.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CalibrationActivity.this);
                    builder.setTitle("Confirmation");
                    builder.setMessage("Navigating back will remove the current list of scanned IDs. Are you sure you want to proceed?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            startActivity(new Intent(CalibrationActivity.this, WelcomeActivity.class));
                        }
                    });
                    builder.setNegativeButton("No", null);
                    builder.show();
                } else {
                    finish();
                    startActivity(new Intent(CalibrationActivity.this, WelcomeActivity.class));
                }
            }
        });
        title.setText("Calibration");
    }


    /***********************************************************
     * Spinner Control
     **********************************************************/
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        calType = parent.getItemAtPosition(position).toString();
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Handle empty selection, if needed
    }


    /***********************************************************
     * Barcode and Input Handling
     **********************************************************/
    private void scanCode() {
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(true);
        options.setCaptureActivity(CaptureAct.class);
        barLauncher.launch(options);
    }

    private void storeBarcode(String barcode) {
        scannedList.add(barcode);
        scannedListAdapter.notifyDataSetChanged();
        // You can perform any additional logic or processing here
    }

    ActivityResultLauncher<ScanOptions> barLauncher = registerForActivityResult(new ScanContract(), result -> {
        if (result.getContents() != null) {
            String barcode = result.getContents();
            storeBarcode(barcode);
        }
    });

    private void showManualInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Type ID exactly how it appears in asset list");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String assetId = input.getText().toString().trim();
                if (!TextUtils.isEmpty(assetId)) {
                    storeBarcode(assetId);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    /***********************************************************
     * Asset List Data Pull
     **********************************************************/
    private void performExcelLookup() {
        // Disable the submit button to prevent multiple clicks
        finishBtn.setEnabled(false);

        // Show a progress dialog to indicate the operation is in progress
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sending Calibration Data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String range = "'Asset List'!A:P";
        Sheets sheetsService = createSheetsService();
        new ReadDataFromSheetTask(sheetsService, range).execute();
    }

    private class ReadDataFromSheetTask extends AsyncTask<Void, Void, List<List<Object>>> {
        private Sheets sheetsService;
        private String range;

        public ReadDataFromSheetTask(Sheets sheetsService, String range) {
            this.sheetsService = sheetsService;
            this.range = range;
        }

        @Override
        protected List<List<Object>> doInBackground(Void... voids) {
            try {
                // Read the data from the sheet
                ValueRange response = sheetsService.spreadsheets().values()
                        .get(sheetID, range)
                        .execute();
                return response.getValues();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<List<Object>> values) {

            if (values != null) {
                // Clear the existing lists before populating with new data
                listID.clear();
                listDescript.clear();
                listManufact.clear();
                listModel.clear();
                listSerial.clear();
                listCalDate.clear();
                listCalDateF.clear();
                listCategory.clear();
                listDivision.clear();

                // Iterate through the rows and find matches for scanned IDs
                for (int i = 1; i < values.size(); i++) {
                    List<Object> row = values.get(i);
                    String id = row.get(0).toString();

                    // Check if the ID is present in the scannedList
                    if (scannedList.contains(id)) {
                        String descript = row.get(1).toString();
                        String manufact = row.get(2).toString();
                        String model = row.get(3).toString();
                        String serial = row.get(4).toString();
                        String caldate = row.get(5).toString();
                        String caldatef = row.get(6).toString();
                        String category = row.get(7).toString();
                        String division = row.get(15).toString();

                        // Store the matched values in the lists
                        listID.add(id);
                        listDescript.add(descript);
                        listManufact.add(manufact);
                        listModel.add(model);
                        listSerial.add(serial);
                        listCalDate.add(caldate);
                        listCalDateF.add(caldatef);
                        listCategory.add(category);
                        listDivision.add(division);
                    }
                }
                sendInfo();
            } else {
                // Error occurred while reading the data from the sheet
                Toast.makeText(CalibrationActivity.this, "Error. Check network connection and try refreshing login", Toast.LENGTH_LONG).show();
            }
        }
    }


    /***********************************************************
     * Send Info to Database
     **********************************************************/
    private void sendInfo() {
        // Start the AsyncTask to perform the network operations in the background
        new SendInfoTask().execute();
    }

    private class SendInfoTask extends AsyncTask<Void, Void, Boolean> {
        private Sheets sheetsService;
        private String databaseId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Create the Google Sheets service
            sheetsService = createSheetsService();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            databaseId = "1sdWT3yQUiqbyI6_k1YhBkHXMqrl4J0hRoAm0nqWZKgI"; // Replace with your existing spreadsheet ID
            List<List<Object>> existingData = prepareDataForDatabase();
            return writeDataToDatabase(sheetsService, databaseId, existingData);
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

            progressDialog.dismiss();
            // Enable the submit button
            finishBtn.setEnabled(true);

            if (success) {
                // Data has been successfully written to the sheet
                // You can perform any necessary actions here, such as showing a success message
                finish();
                startActivity(new Intent(CalibrationActivity.this, FinishActivityCal.class));
            } else {
                // There was an error creating the sheet or writing data
                // You can display an error message or handle the error as needed
                Toast.makeText(CalibrationActivity.this, "Error. Check network connection and try refreshing login", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean writeDataToDatabase(Sheets sheetsService, String spreadsheetId, List<List<Object>> data) {
        try {
            // Prepare the data to be written to the sheet
            ValueRange valueRange = new ValueRange();
            valueRange.setValues(data);

            // Get the current data range of the sheet
            String range = "Output!A:L";
            ValueRange result = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
            List<List<Object>> currentData = result.getValues();

            // Calculate the next empty row index
            int nextRowIndex = currentData != null ? currentData.size() + 1 : 1;

            // Append the data to the next empty row
            String appendRange = "Output!A" + nextRowIndex;
            sheetsService.spreadsheets().values()
                    .append(spreadsheetId, appendRange, valueRange)
                    .setValueInputOption("RAW")
                    .execute();

            return true; // Write operation succeeded
        } catch (IOException e) {
            e.printStackTrace();
            return false; // Write operation failed
        }
    }

    private Sheets createSheetsService() {
        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("KeyScan")
                .build();
    }

    private List<List<Object>> prepareDataForDatabase() {
        // Prepare the data to be added to the existing sheet
        List<List<Object>> addData = new ArrayList<>();

        // Add your data to the 'data' list
        for (int i = 0; i < scannedList.size(); i++) {
            List<Object> row = new ArrayList<>();
            row.add(scannedList.get(i));
            row.add(listDescript.get(i));
            row.add(listManufact.get(i));
            row.add(listModel.get(i));
            row.add(listSerial.get(i));
            row.add(listCalDate.get(i));
            row.add(listCalDateF.get(i));
            row.add(listCategory.get(i));
            row.add(listDivision.get(i));
            row.add(personName);
            row.add("Calibration - "+calType);
            row.add(formattedDate);
            addData.add(row);
        }
        return addData;
    }

}