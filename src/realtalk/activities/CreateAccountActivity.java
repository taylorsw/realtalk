package realtalk.activities;

import realtalk.asynctasks.UserAdder;
import realtalk.util.CommonUtilities;
import realtalk.util.UserInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import com.realtalk.R;

/**
 * Activity for creating an account
 * 
 * @author Brandon Lee
 *
 */
public class CreateAccountActivity extends Activity {
	
	private static final String DEFAULT_ID = "someID";
	private static final int USERNAME_MAX_LENGTH = 20;
	private static final int PASSWORD_MAX_LENGTH = 20;
	public ProgressDialog progressdialog;
	
	/**
	 * Sets up the activity
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_create_account);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.create_account, menu);
		return true;
	}
	
	/**
	 * This method is called when user clicks "Create Account" button.  It reads
	 * the information that the user has provided and attempts to create an account.
	 * 
	 * @param view
	 */
	public void addUser(View view) {
		EditText edittextUser = (EditText) findViewById(R.id.user);
		EditText edittextPword = (EditText) findViewById(R.id.pword);
		EditText edittextConfPword = (EditText) findViewById(R.id.conf_pword);
		String stUsername = edittextUser.getText().toString().trim();
		String stPword = edittextPword.getText().toString();
		String stConf = edittextConfPword.getText().toString();
		
		//if any fields are blank, dialog box pops up
		if(stPword.equals("") || stUsername.trim().equals("") || stConf.equals("")) {
			AlertDialog.Builder alertdialogbuilder = new AlertDialog.Builder(this);
			//set title
			alertdialogbuilder.setTitle("Invalid fields");
			
			//set dialog message
			alertdialogbuilder
				.setMessage("Please fill in all of the fields.")
				.setCancelable(false)
				.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//close the dialog box if this button is clicked
						dialog.cancel();
					}	
			});
			
			//create alert dialog
			AlertDialog alertdialogEmptyField = alertdialogbuilder.create();
			
			//show alert dialog
			alertdialogEmptyField.show();
		} else if(!stPword.equals(stConf)) {	//if the password doesn't match the confirmation password
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			//set title
			alertDialogBuilder.setTitle("Invalid fields");
			
			//set dialog message
			alertDialogBuilder
				.setMessage("Passwords do not match.  Please try again.")
				.setCancelable(false)
				.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//close the dialog box if this button is clicked
						dialog.cancel();
					}	
			});
			
			//create alert dialog
			AlertDialog alertdialogBadPword = alertDialogBuilder.create();
			
			//show alert dialog
			alertdialogBadPword.show();	
		} else if(stUsername.indexOf(" ") != -1) { //username has a space in it
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			//set title
			alertDialogBuilder.setTitle("Invalid username");
			
			//set dialog message
			alertDialogBuilder
				.setMessage("Username may not contain spaces.  Please try again.")
				.setCancelable(false)
				.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//close the dialog box if this button is clicked
						dialog.cancel();
					}	
			});
			
			//create alert dialog
			AlertDialog alertdialogBadPword = alertDialogBuilder.create();
			
			//show alert dialog
			alertdialogBadPword.show();		
		} else if(stUsername.length() > USERNAME_MAX_LENGTH) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			//set title
			alertDialogBuilder.setTitle("Invalid Username");
			
			//set dialog message
			alertDialogBuilder
				.setMessage("Username must not exceed 20 characters.  Please try again.")
				.setCancelable(false)
				.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//close the dialog box if this button is clicked
						dialog.cancel();
					}	
			});
			
			//create alert dialog
			AlertDialog alertdialogBadUsername = alertDialogBuilder.create();
			
			//show alert dialog
			alertdialogBadUsername.show();
		} else if(stPword.length() > PASSWORD_MAX_LENGTH) {
			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			//set title
			alertDialogBuilder.setTitle("Invalid Password");
			
			//set dialog message
			alertDialogBuilder
				.setMessage("Password must not exceed 20 characters.  Please try again.")
				.setCancelable(false)
				.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						//close the dialog box if this button is clicked
						dialog.cancel();
					}	
			});
			
			//create alert dialog
			AlertDialog alertdialogBadPword = alertDialogBuilder.create();
			
			//show alert dialog
			alertdialogBadPword.show();
		} else {
			new UserAdder(this, new UserInfo(stUsername, CommonUtilities.hash(stPword), DEFAULT_ID), this).execute();
		}
	}
}
