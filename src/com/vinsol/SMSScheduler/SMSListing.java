package com.vinsol.SMSScheduler;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

public class SMSListing extends ListActivity {
	
	int typeOfPage;
	ListView lv;
	
	ArrayList<Message> messagesList;
	
	Context context;
	
	/**==========================================================================
	 * method onCreate()
	 *===========================================================================*/
	@Override
    public void onCreate(Bundle onSavedInstanceState) {
	    	
		super.onCreate(onSavedInstanceState);
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
    
    	setContentView(R.layout.message_listing);
    	
    	typeOfPage = getIntent().getIntExtra(Constant.TYPE_OF_SMS_LISTING_PAGE, Constant.PAGE_TYPE_SCHEDULED);
    	
    	context = this;
    	
    	ArrayList<HashMap<String, String>> listViewData = new ArrayList<HashMap<String, String>>();
    	
    	if(typeOfPage == Constant.PAGE_TYPE_SCHEDULED) {
    		messagesList = new SMSSchedulerDBHelper(this).retrieveMessages(Constant.ALL_TIME, Constant.STATUS_SCHEDULED);
    	} else if(typeOfPage == Constant.PAGE_TYPE_SENT) {
    		messagesList = new SMSSchedulerDBHelper(this).retrieveMessages(Constant.ALL_TIME, Constant.STATUS_SENT);
    	}
    	
    	int sizeOfMessagesList;
    	
    	if(messagesList == null || messagesList.isEmpty()) {
    		sizeOfMessagesList = 0;
    	} else {
    		sizeOfMessagesList = messagesList.size();
    	}
    	
    	
		for(int i=0; i<sizeOfMessagesList; i++){
			HashMap<String, String> map = new HashMap<String, String>();
			
			long scheduledTimeInMilliSecond = messagesList.get(i).scheduledTimeInMilliSecond;
			String dateString = CalendarDateConverter.getDateString(scheduledTimeInMilliSecond);
			String timeString = CalendarDateConverter.getTimeString(scheduledTimeInMilliSecond);
			
			map.put(Constant.MESSAGE_BODY, messagesList.get(i).messageBody);
			map.put(Constant.SCHEDULED_DATE, dateString);
			map.put(Constant.SCHEDULED_TIME, timeString);
			map.put(Constant.STATUS, messagesList.get(i).getStatusInString());
			
			listViewData.add(map);
		}
		
	    lv = this.getListView();
	    
	    lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				view.performLongClick();
			}
	    }) ;
	    
	    registerForContextMenu(lv);
	    	    
	    SimpleAdapter mSchedule = new SimpleAdapter
	    			(this, listViewData, 
    						R.layout.message_listing_one_message_view, 
    						new String[] {Constant.MESSAGE_BODY, 
    									  Constant.SCHEDULED_DATE, 
    									  Constant.SCHEDULED_TIME, 
    									  Constant.STATUS}, 
    						new int[] {R.id.message_listing_one_message_view_message_body,
	    							   R.id.message_listing_one_message_view_send_date,
	    							   R.id.message_listing_one_message_view_send_time,
	    							   R.id.message_listing_one_message_view_status});
		
	    if(mSchedule == null){
			Log.v("","mSchedule is null" );
		}else{
			setListAdapter(mSchedule);
		}
	}//end method onCreate
	
	/**=============================================================================
	 * method onCreateContextMenu
	 *==============================================================================*/
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		int positionOfClickedListItem = ((AdapterContextMenuInfo)menuInfo).position;
		String MessageBody = messagesList.get(positionOfClickedListItem).messageBody;
	
		menu.setHeaderTitle(MessageBody);
		if(typeOfPage == Constant.PAGE_TYPE_SCHEDULED) {
			menu.add(0, R.id.SMS_LISTING_CONTEXT_MENU_EDIT, 0, "Update");
		}
		menu.add(0, R.id.SMS_LISTING_CONTEXT_MENU_DELETE, 0,  "Delete");
		menu.add(0, R.id.SMS_LISTING_CONTEXT_MENU_ADD_TO_TEMPLATE, 0,  "Add to Templates");
	}

	/**=============================================================================
	 * method onContextItemSelected
	 *==============================================================================*/
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		final int positionOfClickedListItem = info.position;
		
		switch (item.getItemId()) {
			case R.id.SMS_LISTING_CONTEXT_MENU_EDIT: {
				long idOfClickedMessage = messagesList.get(positionOfClickedListItem).id;
				   
				ArrayList<Receiver> receiversArrayList = new SMSSchedulerDBHelper(context).retrieveReceivers(idOfClickedMessage);
				   
				MessageAndReceivers.message = messagesList.get(positionOfClickedListItem);
				MessageAndReceivers.receivers = receiversArrayList;
				  
				new IntentHandler().gotoScheduleSMSEditPage(context);
				return true;
			}
			case R.id.SMS_LISTING_CONTEXT_MENU_DELETE: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Delete")
					   .setMessage("Are you sure?")
				       .setCancelable(false)
				       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   long idOfClickedMessage = messagesList.get(positionOfClickedListItem).id;
				        	   boolean isDeleted = new SMSSchedulerDBHelper(context).deleteMessage(idOfClickedMessage);
				        	   
				        	   if(isDeleted) {
				        		   if(typeOfPage == Constant.PAGE_TYPE_SCHEDULED) {
				        			   new IntentHandler().gotoSMSListingTabActivity(context, Constant.SELECTED_TAB_SCHEDULED_SMS);
				        		   } else if(typeOfPage == Constant.PAGE_TYPE_SENT) {
				        			   new IntentHandler().gotoSMSListingTabActivity(context, Constant.SELECTED_TAB_SENT_SMS);
				        		   }
				        	   }else {
				        		   Toast.makeText(context,
        				   			  context.getString(R.string.toast_message_sms_listing_problem_in_delete),
        				   			  Toast.LENGTH_LONG).show();
				        	   }
				           }
				       })
				       .setNegativeButton("No", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				                dialog.cancel();
				           }
				       });
				AlertDialog deleteAlert = builder.create();
				deleteAlert.show();
				return true;
			}
			case R.id.SMS_LISTING_CONTEXT_MENU_ADD_TO_TEMPLATE: {
				String messageBody = messagesList.get(positionOfClickedListItem).messageBody;
				
				long resultRow = new SMSSchedulerDBHelper(SMSListing.this).addTemplate(messageBody);
				if(resultRow == Constant.TEMPLATE_ALREADY_EXIST) {
					Toast.makeText(SMSListing.this, getString(R.string.toast_message_sms_listing_template_already_exist), Toast.LENGTH_SHORT).show();
				}else if(resultRow == -1) {
					Toast.makeText(SMSListing.this, getString(R.string.toast_message_sms_listing_problem_in_adding_template), Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(SMSListing.this, getString(R.string.toast_message_sms_listing_template_added_successfully), Toast.LENGTH_SHORT).show();
				}
				return true;
			}
			default: {
				return super.onContextItemSelected(item);
			}
		}//end switch
	}//end method onContextItemSelected
	
	/**==================================================================
	 * method onCreateOptionsMenu(Menu menu)
	 * create optionMenu
	 *===================================================================*/
	public boolean onCreateOptionsMenu(Menu menu) {
	    new OptionMenuHelper().createOptionMenu(menu, Constant.OPTION_MENU_FOR_SMS_LISTING);
	    return true;
	}//end method onCreateOptionsMenu

	/**==================================================================
	 * method onOptionsItemSelected(MenuItem item)
	 * called when an option item is being clicked
	 *===================================================================*/
	public boolean onOptionsItemSelected(MenuItem item) {
	    return new OptionMenuHelper().onOptionsItemSelected(this, item);
	}//end method onOptionsItemSelected
	
}//end class MessageListing