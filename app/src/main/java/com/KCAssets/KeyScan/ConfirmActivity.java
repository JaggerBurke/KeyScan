package com.KCAssets.KeyScan;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ConfirmActivity extends AppCompatActivity {


    /***********************************************************
     * Initializations
     **********************************************************/
    ListView listView;
    CustomAdapter customAdapter;
    ArrayList<String> scannedIDs;
    ArrayList<String> Descriptions;
    ArrayList<String> Manufacturers;
    ArrayList<String> Models;
    ArrayList<String> SerialNumbers;
    ArrayList<String> CalibrationDates;
    ArrayList<String> CalibrationDatesF;
    ArrayList<String> Category;
    ArrayList<String> Division;
    ArrayList<String> PastDue;
    EditText jobNumber;
    EditText testType;
    String testTypeValue;
    String jobNumberValue;
    Button submitBtn;
    ProgressDialog progressDialog;
    String personName;
    String formattedDate;
    String accessToken;


    /***********************************************************
     * onCreate
     **********************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.confirm_screen);


        /***********************************************************
         * Local Initializations
         **********************************************************/
        listView = findViewById(R.id.listview_id);
        submitBtn = findViewById(R.id.submitBtn);

        scannedIDs = getIntent().getStringArrayListExtra("scannedIDs");
        Descriptions = getIntent().getStringArrayListExtra("Descriptions");
        Manufacturers = getIntent().getStringArrayListExtra("Manufacturers");
        Models = getIntent().getStringArrayListExtra("Models");
        SerialNumbers = getIntent().getStringArrayListExtra("SerialNumbers");
        CalibrationDates = getIntent().getStringArrayListExtra("CalibrationDates");
        CalibrationDatesF = getIntent().getStringArrayListExtra("CalibrationDatesF");
        Category = getIntent().getStringArrayListExtra("Category");
        Division = getIntent().getStringArrayListExtra("Division");
        PastDue = getIntent().getStringArrayListExtra("PastDue");
        updateListView(scannedIDs, Descriptions, PastDue);

        boolean hasPastDueAssets = checkPastDueAssets(PastDue);
        if (hasPastDueAssets) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Warning");
            builder.setMessage("One or more Assets are past due for Calibration (marked in red)");
            builder.setPositiveButton("OK", null);
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            personName = account.getDisplayName();
        }

        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        formattedDate = dateFormat.format(currentDate);

        AccessTokenManager accessTokenManager = new AccessTokenManager(this);
        accessTokenManager.checkAccessTokenExpiration();


        /***********************************************************
         * Toolbar
         **********************************************************/
        ImageView backArrow = findViewById(R.id.backArrow);
        TextView title = findViewById(R.id.title);
        RelativeLayout bottomBar = findViewById(R.id.bottomBar);
        bottomBar.setVisibility(View.GONE);
        ImageView keylogo = findViewById(R.id.keylogo);
        keylogo.setVisibility(View.GONE);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ConfirmActivity.this, TestingActivity.class);
                startActivity(intent);
                finish();
            }
        });
        title.setText("Confirmation");


        /***********************************************************
         * EditTexts
         **********************************************************/
        testType = findViewById(R.id.test);
        testType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used in this case
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Store the updated input in the variable
                testTypeValue = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used in this case
            }
        });

        jobNumber = findViewById(R.id.job);
        jobNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used in this case
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Store the updated input in the variable
                jobNumberValue = s.toString();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used in this case
            }
        });


        /***********************************************************
         * Submit Button
         **********************************************************/
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AccessTokenManager accessTokenManager = new AccessTokenManager(ConfirmActivity.this);
                accessTokenManager.checkAccessTokenExpiration();

                if (!scannedIDs.isEmpty()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ConfirmActivity.this);
                    builder.setTitle("Confirmation");
                    builder.setMessage("Are you sure you want to submit list?");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (jobNumberValue != null && testTypeValue != null) {
                                createEquipmentList();
                            } else {
                                Toast.makeText(ConfirmActivity.this, "Error: Job Number or Test Type inputs are empty", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                    builder.setNegativeButton("No", null);
                    builder.show();
                } else {
                    Toast.makeText(ConfirmActivity.this, "Error: Asset List is Empty", Toast.LENGTH_LONG).show();
                }
            }
        });
    }


    /***********************************************************
     * Adapter and Past Due Handling
     **********************************************************/
    private void updateListView(ArrayList<String> scannedIDs, ArrayList<String> Descriptions, ArrayList<String> PastDue) {
        customAdapter = new CustomAdapter(getApplicationContext(), scannedIDs, Descriptions, PastDue);
        listView.setAdapter(customAdapter);
    }

    private boolean checkPastDueAssets(ArrayList<String> pastDue) {
        for (String pastDueValue : pastDue) {
            if (pastDueValue.equals("1")) {
                return true;
            }
        }
        return false;
    }


    /***********************************************************
     * Equipment List Google Sheet
     **********************************************************/
    private void createEquipmentList() {
        // Disable the submit button to prevent multiple clicks
        submitBtn.setEnabled(false);

        // Show a progress dialog to indicate the operation is in progress
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Creating equipment list...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Start the AsyncTask to perform the network operations in the background
        new CreateEquipmentListTask().execute();
    }

    private class CreateEquipmentListTask extends AsyncTask<Void, Void, String> {
        private Sheets sheetsService;
        private String spreadsheetId;
        private String databaseId;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Create the Google Sheets service
            sheetsService = createSheetsService();
        }

        @Override
        protected String doInBackground(Void... voids) {
            // Create a new Google Sheets spreadsheet
            Spreadsheet spreadsheet = createSpreadsheet(sheetsService);

            if (spreadsheet != null) {
                // Get the spreadsheet ID
                spreadsheetId = spreadsheet.getSpreadsheetId();

                // Add data to the spreadsheet
                List<List<Object>> data = prepareDataForSheet();
                writeDataToSheet(sheetsService, spreadsheetId, data);

                databaseId = "1sdWT3yQUiqbyI6_k1YhBkHXMqrl4J0hRoAm0nqWZKgI"; // Replace with your existing spreadsheet ID
                List<List<Object>> existingData = prepareDataForDatabase();
                writeDataToDatabase(sheetsService, databaseId, existingData);

                return spreadsheetId;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String spreadsheetId) {
            super.onPostExecute(spreadsheetId);

            // Dismiss the progress dialog
            progressDialog.dismiss();

            // Enable the submit button
            submitBtn.setEnabled(true);

            if (spreadsheetId != null) {
                // Data has been successfully written to the sheet
                // You can perform any necessary actions here, such as showing a success message
                finish();
                startActivity(new Intent(ConfirmActivity.this, FinishActivity.class));
            } else {
                // There was an error creating the sheet or writing data
                // You can display an error message or handle the error as needed
                Toast.makeText(ConfirmActivity.this, "Error. Check network connection and try refreshing login", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Sheets createSheetsService() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        accessToken = sharedPreferences.getString("access_token", null);

        HttpTransport httpTransport = new NetHttpTransport();
        JsonFactory jsonFactory = new JacksonFactory();

        GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);

        return new Sheets.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("KeyScan")
                .build();
    }

    private Spreadsheet createSpreadsheet(Sheets service) {
        Spreadsheet spreadsheet = new Spreadsheet();
        SpreadsheetProperties properties = new SpreadsheetProperties();
        properties.setTitle(jobNumberValue + "_" + testTypeValue + "_EquipmentList");
        spreadsheet.setProperties(properties);

        try {
            return service.spreadsheets().create(spreadsheet).execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<List<Object>> prepareDataForDatabase() {
        // Prepare the data to be added to the existing sheet
        List<List<Object>> addData = new ArrayList<>();

        // Add your data to the 'data' list
        for (int i = 0; i < scannedIDs.size(); i++) {
            List<Object> row = new ArrayList<>();
            row.add(scannedIDs.get(i));
            row.add(Descriptions.get(i));
            row.add(Manufacturers.get(i));
            row.add(Models.get(i));
            row.add(SerialNumbers.get(i));
            row.add(CalibrationDates.get(i));
            row.add(CalibrationDatesF.get(i));
            row.add(Category.get(i));
            row.add(Division.get(i));
            row.add(personName);
            row.add("Testing");
            row.add(formattedDate);
            addData.add(row);
        }
        return addData;
    }

    private List<List<Object>> prepareDataForSheet() {
        List<List<Object>> data = new ArrayList<>();

        // Add the column headers
        List<Object> headers = new ArrayList<>();
        headers.add("Asset");
        headers.add("Description");
        headers.add("Manufacturer");
        headers.add("Model");
        headers.add("Serial #");
        headers.add("Calibration Due");
        // Add more headers for other data arrays if needed
        data.add(headers);

        // Add the data rows
        for (int i = 0; i < scannedIDs.size(); i++) {
            List<Object> row = new ArrayList<>();
            row.add(scannedIDs.get(i));
            row.add(Descriptions.get(i));
            row.add(Manufacturers.get(i));
            row.add(Models.get(i));
            row.add(SerialNumbers.get(i));
            row.add(CalibrationDates.get(i));
            // Add more values for other data arrays if needed
            data.add(row);
        }
        return data;
    }

    private void writeDataToSheet(Sheets sheetsService, String spreadsheetId, List<List<Object>> data) {
        WriteDataToSheetTask writeTask = new WriteDataToSheetTask(sheetsService, spreadsheetId, data);
        writeTask.execute();
    }

    private class WriteDataToSheetTask extends AsyncTask<Void, Void, Void> {
        private Sheets sheetsService;
        private String spreadsheetId;
        private List<List<Object>> values;

        public WriteDataToSheetTask(Sheets sheetsService, String spreadsheetId, List<List<Object>> values) {
            this.sheetsService = sheetsService;
            this.spreadsheetId = spreadsheetId;
            this.values = values;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Prepare the data to be written to the sheet
                ValueRange valueRange = new ValueRange();
                valueRange.setValues(values);

                // Perform the write operation
                sheetsService.spreadsheets().values()
                        .update(spreadsheetId, "Sheet1!A1", valueRange)
                        .setValueInputOption("RAW")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI or perform any post-execution tasks
        }
    }

    private void writeDataToDatabase(Sheets sheetsService, String spreadsheetId, List<List<Object>> data) {
        WriteDataToDatabaseTask writeTask = new WriteDataToDatabaseTask(sheetsService, spreadsheetId, data);
        writeTask.execute();
    }

    private class WriteDataToDatabaseTask extends AsyncTask<Void, Void, Void> {
        private Sheets sheetsService;
        private String spreadsheetId;
        private List<List<Object>> values;

        public WriteDataToDatabaseTask(Sheets sheetsService, String spreadsheetId, List<List<Object>> values) {
            this.sheetsService = sheetsService;
            this.spreadsheetId = spreadsheetId;
            this.values = values;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // Prepare the data to be written to the sheet
                ValueRange valueRange = new ValueRange();
                valueRange.setValues(values);

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
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // Update UI or perform any post-execution tasks
        }
    }
}