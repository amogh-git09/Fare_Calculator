package com.amogh.farecalc;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.amogh.farecalc.R;

public class FareCalculatorActivity extends Activity implements OnItemClickListener {

	private static final String LOG_TAG = "ExampleApp";

	private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
	private static final String DIST_API_BASE = "https://maps.googleapis.com/maps/api/distancematrix";
	private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
	private static final String OUT_JSON = "/json";

	private static final String API_KEY = "AIzaSyCyxXzmczqpntbENM64pk4NcPuifYm9mkU";
	
	AutoCompleteTextView autoCompView;
	AutoCompleteTextView autoCompView2;
	Button distanceButton;
	EditText distanceEditText;
	EditText durationEditText;
	EditText costEditText;

	String[] texts = new String[2];
	
	public static final float DISTANCE_CONSTANT = 0.03f;
	public static final float DURATION_CONSTANT = 0.02f;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		autoCompView = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView);
		autoCompView2 = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView2);
		distanceEditText = (EditText) findViewById(R.id.distanceTextField);
		durationEditText = (EditText) findViewById(R.id.durationTextField);
		costEditText = (EditText) findViewById(R.id.costTextField);
		distanceButton = (Button) findViewById(R.id.distanceButton);
		
		distanceButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				InputMethodManager inputManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE); 

				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                           InputMethodManager.HIDE_NOT_ALWAYS);
				String origin = autoCompView.getText().toString();
				String dest = autoCompView2.getText().toString();
				origin = origin.replaceAll(" ", "+");
				dest = dest.replaceAll(" ", "+");
				
				StringBuilder sb = new StringBuilder(DIST_API_BASE + OUT_JSON);
				sb.append("?key=" + API_KEY);
				sb.append("&origins=" + origin);
				sb.append("&destinations=" + dest);

				try {
					URL url = new URL(sb.toString());
					CalcDistTask task = new CalcDistTask();
					task.execute(new URL[] {url});
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
		});

		autoCompView.requestFocus();
		autoCompView.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
		autoCompView.setOnItemClickListener(this);
		autoCompView2.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
		autoCompView2.setOnItemClickListener(this);
	}

	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		String str = (String) adapterView.getItemAtPosition(position);
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}

	private class CalcDistTask extends AsyncTask<URL, Integer, Integer[]>{

		@Override
		protected Integer[] doInBackground(URL... params) {
			Integer[] ret = new Integer[2];
			URL url = params[0];
			HttpURLConnection conn = null;
			StringBuilder jsonResults = new StringBuilder();

			try {
				System.out.println("URL: "+url);
				Log.d("URL", url.toString());
				conn = (HttpURLConnection) url.openConnection();
				Log.d("DEBUG", "0");
				InputStreamReader in = new InputStreamReader(conn.getInputStream());
				Log.d("DEBUG", "1");
				// Load the results into a StringBuilder
				int read;
				char[] buff = new char[1024];
				while ((read = in.read(buff)) != -1) {
					jsonResults.append(buff, 0, read);
				}
				Log.d("DEBUG", "2");
			} catch (MalformedURLException e) {
				Log.e(LOG_TAG, "Error processing Places API URL", e);
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error connecting to Places API", e);
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
				Log.d("JSON", jsonResults.toString());
			}
			
			try {
				JSONObject jsonObj = new JSONObject(jsonResults.toString());
				JSONArray rows = jsonObj.getJSONArray("rows");
				JSONArray elements = rows.getJSONObject(0).getJSONArray("elements");
				JSONObject elem = elements.getJSONObject(0);
				JSONObject dist = elem.getJSONObject("distance");
				JSONObject timeDuration = elem.getJSONObject("duration");

				Log.d("JSONPARSING", timeDuration.getString("value"));
				
				texts[0] = dist.getString("text");
				texts[1] = timeDuration.getString("text");
						
				int distance, duration;
				distance = Integer.parseInt(dist.getString("value"));
				duration = Integer.parseInt(timeDuration.getString("value"));
				
				ret[0] = distance;
				ret[1] = duration;
				
			} catch (JSONException e) {
				Log.e(LOG_TAG, "Cannot process JSON results", e);
			}
			
			return ret;
		}
		
		@Override
		protected void onPostExecute(Integer[] results){
			distanceEditText.setText("Distance: " + texts[0]);
			durationEditText.setText("Duration: " + texts[1]);
			costEditText.setText("Fare: " + getResources().getString(R.string.Rs) + " " 
					+ String.format("%.2f", DISTANCE_CONSTANT*results[0] 
					+ DURATION_CONSTANT*results[1]));
		}
	}
	
	public static ArrayList<String> autocomplete(String input) {
		ArrayList<String> resultList = null;

		HttpURLConnection conn = null;
		StringBuilder jsonResults = new StringBuilder();
		try {
			StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
			sb.append("?key=" + API_KEY);
			//sb.append("&components=country:in");
			sb.append("&input=" + URLEncoder.encode(input, "utf8"));

			URL url = new URL(sb.toString());
			
			System.out.println("URL: "+url);
			Log.d("URL", url.toString());
			conn = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(conn.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				jsonResults.append(buff, 0, read);
			}
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Error processing Places API URL", e);
			return resultList;
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error connecting to Places API", e);
			return resultList;
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		try {	
			// Create a JSON object hierarchy from the results
			JSONObject jsonObj = new JSONObject(jsonResults.toString());
			JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

			// Extract the Place descriptions from the results
			resultList = new ArrayList<String>(predsJsonArray.length());
			for (int i = 0; i < predsJsonArray.length(); i++) {
				System.out.println(predsJsonArray.getJSONObject(i).getString("description"));
				System.out.println("============================================================");
				resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, "Cannot process JSON results", e);
		}

		return resultList;
	}

	class GooglePlacesAutocompleteAdapter extends ArrayAdapter<String> implements Filterable {
		private ArrayList<String> resultList;

		public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId);
		}

		@Override
		public int getCount() {
			return resultList.size();
		}

		@Override
		public String getItem(int index) {
			return resultList.get(index);
		}

		@Override
		public Filter getFilter() {
			Log.d("FILTER", "get filter called");
			Filter filter = new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					Log.d("FILTER", "performFiltering called");
					FilterResults filterResults = new FilterResults();
					if (constraint != null) {
						// Retrieve the autocomplete results.
						Log.d("TAG", constraint.toString());
						resultList = autocomplete(constraint.toString());

						// Assign the data to the FilterResults
						filterResults.values = resultList;
						filterResults.count = resultList.size();
					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					Log.d("FILTER", "publishResults called");
					if (results != null && results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
			};
			return filter;
		}
	}
}