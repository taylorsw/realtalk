package realtalk.activities;


import java.util.ArrayList;
import java.util.List;

import realtalk.asynctasks.RoomLeaverFromRoomList;
import realtalk.asynctasks.RoomLoader;
import realtalk.controller.ChatController;
import realtalk.util.ChatRoomInfo;
import realtalk.util.UserInfo;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.realtalk.R;

/**
 * Activity for selecting a chat room to join
 * 
 * @author Jordan Hazari and Taylor Williams
 *
 */
@SuppressLint("NewApi")
public class SelectRoomActivity extends Activity {
	private static final double HACKED_GPS_DISTANCE_CONSTANT_TO_BE_REMOVED = 500.0;
	public List<ChatRoomInfo> rgchatroominfo = new ArrayList<ChatRoomInfo>();
	private List<ChatRoomInfo> rgchatroominfoJoined = new ArrayList<ChatRoomInfo>();
	private List<ChatRoomInfo> rgchatroominfoUnjoined = new ArrayList<ChatRoomInfo>();
	public ChatRoomAdapter unJoinedAdapter;
	public ChatRoomAdapter joinedAdapter;
	private SharedPreferences sharedpreferencesLoginPrefs;
	private Editor editorLoginPrefs;
	private Location locationUser;
	private Boolean fAnon;

	/**
	 * Sets up the activity, and diplays a list of available rooms
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_select_room);
		sharedpreferencesLoginPrefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);
		editorLoginPrefs = sharedpreferencesLoginPrefs.edit();
		
		ImageView buttonCreateRoom = (ImageView) findViewById(R.id.createRoomId);
		buttonCreateRoom.setEnabled(false);
		
		fAnon = false;
		
		UserInfo userinfo = ChatController.getInstance().getUser();
		String stUser = userinfo.stUserName();
		TextView textviewRoomTitle = (TextView) findViewById(R.id.userTitle);
		textviewRoomTitle.setText(stUser);

		ListView listviewJoined = (ListView) findViewById(R.id.joined_list);
		listviewJoined.setClickable(false);
		
		ListView listviewUnjoined = (ListView) findViewById(R.id.unjoined_list);
		listviewUnjoined.setClickable(false);

		// when a room is clicked, starts a new ChatRoomActivity
        listviewJoined.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	ChatRoomInfo criSelected = rgchatroominfoJoined.get(position);
        		Intent itStartChat = new Intent(SelectRoomActivity.this, ChatRoomActivity.class);
        		itStartChat.putExtra("ROOM", criSelected);
        		itStartChat.putExtra("DEBUG", false);
        		SelectRoomActivity.this.startActivity(itStartChat);
        		SelectRoomActivity.this.finish();
            }
        });
        
		// when a room is clicked, starts a new ChatRoomActivity
        listviewUnjoined.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            	ChatRoomInfo criSelected = rgchatroominfoUnjoined.get(position);
        		Intent itStartChat = new Intent(SelectRoomActivity.this, ChatRoomActivity.class);
        		itStartChat.putExtra("ROOM", criSelected);
        		itStartChat.putExtra("DEBUG", false);
        		SelectRoomActivity.this.startActivity(itStartChat);
        		SelectRoomActivity.this.finish();
            }
        });

		// Binding resources Array to ListAdapter
		unJoinedAdapter = new ChatRoomAdapter(this, R.layout.message_item, rgchatroominfoUnjoined, false);
		listviewUnjoined.setAdapter(unJoinedAdapter);
		
		joinedAdapter = new ChatRoomAdapter(this, R.layout.message_item, rgchatroominfoJoined, true);
		listviewJoined.setAdapter(joinedAdapter);
		
		//location code:
		LocationManager locationmanager = (LocationManager) getSystemService(LOCATION_SERVICE);
		final double radiusMeters = 500.0;
		
		//load recent location first
		if (ChatController.getInstance().getRecentLocation() != null) {
			loadRooms(ChatController.getInstance().getRecentLocation(), radiusMeters);
		}
		
		Criteria criteria = new Criteria();
		String stBestProvider = locationmanager.getBestProvider(criteria, true);
		if (stBestProvider == null) {
			//no location providers, must ask user to enable a provider
			Toast.makeText(getApplicationContext(), R.string.turn_gps_on, Toast.LENGTH_SHORT).show();
		} else {
			// Define a listener that responds to location updates
			LocationListener locationListener = new LocationListener() {
			    private static final int LOCATION_COUNT = 5;
				private Location locationMostAccurate = null;
				private int locationCount = 0;
				public void onLocationChanged(Location location) {
					
					// Called when a new location is found by the network location provider.
					//if new location data is not usable...
					locationUser = location;
					loadRooms(locationUser, radiusMeters);
					ChatController.getInstance().setRecentLocation(locationUser);
					locationCount++;
					if (locationMostAccurate == null || locationMostAccurate.getAccuracy() >= locationUser.getAccuracy()) {
						locationMostAccurate = locationUser;
					}
					if (locationCount >= LOCATION_COUNT) {
						ImageView buttonCreateRoom = (ImageView) findViewById(R.id.createRoomId);
						buttonCreateRoom.setEnabled(true);
						buttonCreateRoom.setImageResource(R.drawable.createroom_icon);
					}
				}

		        public void onStatusChanged(String provider, int status, Bundle extras) {
		            switch (status) {
    		            case LocationProvider.OUT_OF_SERVICE:
    		            	Toast.makeText(getApplicationContext(), R.string.gps_out_of_service, Toast.LENGTH_SHORT).show();
    		                break;
    		            case LocationProvider.TEMPORARILY_UNAVAILABLE:
    		            	Toast.makeText(getApplicationContext(), R.string.gps_unavailable, Toast.LENGTH_SHORT).show();
    		                break;
		                default: 
		                    break;
		            }
		        }
				public void onProviderEnabled(String provider) {}
				public void onProviderDisabled(String provider) {}
			};

			// Register the listener with the Location Manager to receive location updates
			locationmanager.requestLocationUpdates(stBestProvider, 0, 0, locationListener);
		}
	}
	
	/**
	 * Refresh the page on resume
	 */
	@Override
	protected void onResume() {
		super.onResume();
		new Refresher().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	/**
	 * Loads the joined/available rooms to their respective lists
	 * on the page.
	 * 
	 * @param location your current location
	 * @param radiusMeters radius to search for rooms
	 */
	private void loadRooms(Location location, double radiusMeters) {
		new RoomLoader(this, this, location.getLatitude(), location.getLongitude(), HACKED_GPS_DISTANCE_CONSTANT_TO_BE_REMOVED).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
	
	/**
	 * Displays the details of a room, and gives the option
	 * to join/leave/chat anonomously
	 * 
	 * @param position the index of the lidt that was clicked
	 * @param fJoined whether or not you are already joined in the room
	 */
	private void getDetails(int position, boolean fJoined) {
		final ChatRoomInfo chatroominfo;
		String stJoinView;
		if(fJoined) {
			chatroominfo = rgchatroominfoJoined.get(position);
			stJoinView = "Enter";
		} else {
			chatroominfo = rgchatroominfoUnjoined.get(position);
			stJoinView = "Join";
		}
		
    	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		//set title
		alertDialogBuilder.setTitle(chatroominfo.stName());

		//set up the checkbox only if not joined
	    View viewCheckbox = View.inflate(this, R.layout.checkbox_anon, null);
		//set dialog message
		alertDialogBuilder
			.setMessage(Html.fromHtml("<b>Description: </b> " +  chatroominfo.stDescription() + 
									"<br/><br/><b>Active Users: </b> " + chatroominfo.numUsersGet() +
									"<br/><br/><b>Creator: </b> " + chatroominfo.stCreator()))
			.setCancelable(true);
		
		if (!fJoined) {
		    alertDialogBuilder.setView(viewCheckbox);
		}
		
		alertDialogBuilder.setNegativeButton(stJoinView, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				Intent itStartChat = new Intent(SelectRoomActivity.this, ChatRoomActivity.class);
        		itStartChat.putExtra("ROOM", chatroominfo);
        		itStartChat.putExtra("DEBUG", false);
        		itStartChat.putExtra("ANON", fAnon);
        		SelectRoomActivity.this.startActivity(itStartChat);
        		SelectRoomActivity.this.finish();
			}	
		});
		
		alertDialogBuilder.setPositiveButton("Back", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User cancelled the dialog
				dialog.cancel();
			}
		});
		
		if(fJoined) {	
			alertDialogBuilder.setNeutralButton("Leave", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					new RoomLeaverFromRoomList(SelectRoomActivity.this, chatroominfo).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
					dialog.cancel();
				}
			});
		}
		
		//create alert dialog
		AlertDialog alertdialogDeleteAcc = alertDialogBuilder.create();
		
		//show alert dialog
		alertdialogDeleteAcc.show();
	}
	
	/**
	 * This is called when the anonymous checkbox is clicked.
	 * @param view the checkbox
	 */
	public void onAnonCheckboxClicked(View view) {
	    fAnon = ((CheckBox) view).isChecked();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_room, menu);
		return true;
	}

	/**
	 * Menu options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//respond to menu item selection
		switch (item.getItemId()){
		case R.id.logout:
			this.clickLogout(getCurrentFocus());
			return true;
		case R.id.settings:
			this.clickSettings(getCurrentFocus());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	/**
	 * Opens the account settings page.
	 * Called when the "gear" icon is clicked.
	 */
	public void clickSettings(View view) {
		startActivity(new Intent(this, AccountSettingsActivity.class));
	}
	
	/**
	 * Prompts the user if they want to log out, and does so if they 
	 * choose "yes".  Called when the "door" icon is clicked.
	 */
	public void clickLogout(View view) {
    	AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		//set title
		alertDialogBuilder.setTitle("Log Out");
		
		//set dialog message
		alertDialogBuilder
			.setMessage("Are you sure you want to log out?")
			.setCancelable(true);
		
		alertDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				editorLoginPrefs.putBoolean("loggedIn", false);
				editorLoginPrefs.putString("loggedin_username", null);
				editorLoginPrefs.putString("loggedin_password", null);
				editorLoginPrefs.commit();
				Intent itLogin = new Intent(SelectRoomActivity.this, LoginActivity.class);
				startActivity(itLogin);
				finish();
			}	
		});
		
		alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User cancelled the dialog
				dialog.cancel();
			}
		});
		
		//create alert dialog
		AlertDialog alertdialogDeleteAcc = alertDialogBuilder.create();
		
		//show alert dialog
		alertdialogDeleteAcc.show();
		
	}

	/**
	 * Gets the user's current location and opens the create room page.
	 * Called when the "green plus" icon is clicked.
	 */
	public void createRoom(View view) {
		Intent itCreateRoom = new Intent(this, CreateRoomActivity.class);
		itCreateRoom.putExtra("LATITUDE", locationUser.getLatitude());
		itCreateRoom.putExtra("LONGITUDE", locationUser.getLongitude());
		this.startActivity(itCreateRoom);
	}

	/**
	 * Adapter for the lists of chatrooms.
	 * 
	 * @author Jordan Hazari
	 *
	 */
	public class ChatRoomAdapter extends ArrayAdapter<ChatRoomInfo> {

		private List<ChatRoomInfo> rgchatroominfo;
		private boolean fJoined;

		public ChatRoomAdapter(Context context, int textViewResourceId, List<ChatRoomInfo> rgchatroominfo, boolean fJoined) {
			super(context, textViewResourceId, rgchatroominfo);
			this.rgchatroominfo = rgchatroominfo;
			this.fJoined = fJoined;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = vi.inflate(R.layout.room_item, null);
			}
			ChatRoomInfo chatroominfo = rgchatroominfo.get(position);
			if (chatroominfo != null) {
				TextView textviewTop = (TextView) view.findViewById(R.id.toptext);
				TextView textviewBottom = (TextView) view.findViewById(R.id.bottomtext);
				ImageView button = (ImageView) view.findViewById(R.id.detailsButton);
				
				OnClickListener listener = new OnClickListener() {
				    @Override
				    public void onClick(View view) {
				    	getDetails((Integer) view.getTag(), fJoined);
				    }
				};
				
				if (textviewTop != null) {
					textviewTop.setText(chatroominfo.stName());
				}
				if(textviewBottom != null) {
					textviewBottom.setText("\t" + chatroominfo.numUsersGet() + " users");
				}
				if(button != null) {
					button.setTag(position);
					button.setOnClickListener(listener);
					button.setFocusable(false);
				}
			}
			return view;
		}
	}
	
	/**
	 * AsyncTask that refreshes the page.
	 * 
	 * @author Jordan Hazari
	 *
	 */
	class Refresher extends AsyncTask<String, String, Boolean> {
	    
        @Override
        protected Boolean doInBackground(String... params) {
        	ChatController.getInstance().fRefresh();
        	return true;
        }
	}
}
