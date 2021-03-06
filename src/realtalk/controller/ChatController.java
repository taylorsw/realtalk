package realtalk.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;

import realtalk.model.HallwayModel;
import realtalk.util.ChatManager;
import realtalk.util.ChatRoomInfo;
import realtalk.util.ChatRoomResultSet;
import realtalk.util.CommonUtilities;
import realtalk.util.IChatManager;
import realtalk.util.MessageInfo;
import realtalk.util.PullMessageResultSet;
import realtalk.util.RequestResultSet;
import realtalk.util.UserInfo;

/**
 * Controller that handles the chatroom model and handles all internal logic.
 * 
 * @author Colin Kho
 *
 */
public final class ChatController implements IChatController {
    private static final int RECENTLOCTIME = 5;
    private static ChatController instance = null;
    private HallwayModel chatModel;
    private Location locationRecent;
    // Keeps track of the current user in the RealTalk application.
    // Set to null if user not set / logged out.
    private UserInfo userinfo;
    
    // Variables used for debugging and testing
    private boolean debug;
    private IChatManager cm;
    
    private ChatController() {
        chatModel = new HallwayModel();
        this.userinfo = null;
        this.debug = false;
        this.cm = null;
    }
    
    /**
     * Static method that gets the current instance of the controller. It 
     * creates a new controller if the instance is not yet initialized.
     * 
     * @return ChatController's instance.
     */
    public static ChatController getInstance() {
        if (instance == null) {
            instance = new ChatController();
        }
        return instance;
    }
    
	@SuppressLint("NewApi")
	@Override
	public Location getRecentLocation() {
		if (locationRecent == null) {
			return null;
		}
		if (locationRecent.getTime() - System.currentTimeMillis() < 
		        TimeUnit.MINUTES.convert(RECENTLOCTIME, TimeUnit.MILLISECONDS)) {
			return locationRecent;
		}
		return null;
	}
	
	@Override
	public void setRecentLocation(Location location) {
		locationRecent = location;
	}
    
    /**
     * This initializes the ChatController by loading all the rooms that the user is currently in
     * from the last session. It takes a userinfo and is set to that user.
     * 
     * NOTE: Must be called from an async task
     * 
     * @param userinfo
     * @return true if initialization succeeded, false if it failed.
     */
    public boolean fInitialize(UserInfo userinfo) {
        this.userinfo = userinfo;
        return fRefresh();
    }
    
    /**
     * This method refreshes the chatrooms that the current instance user has joined.
     * 
     * NOTE: Must be called from an async task.
     * 
     * @return True if refresh succeeded, false if otherwise.
     */
    public boolean fRefresh() {
    	ChatRoomResultSet crrs;
    	if (!debug) {
    		crrs = ChatManager.crrsUsersChatrooms(userinfo);
    	} else {
    		crrs = cm.crrsUsersChatrooms(userinfo);
    	}
        // Clear the current chat model.
        chatModel = null;
        chatModel = new HallwayModel();
        if (crrs == null || !crrs.fSucceeded()) {
            return false;
        }    
        List<ChatRoomInfo> rgCri = crrs.rgcriGet();
        for (ChatRoomInfo chatroom : rgCri) {
            // Load chatrooms only if the request succeeded.
            chatModel.addRoom(chatroom.stName(), 
                    chatroom.stId(), 
                    chatroom.stDescription(), 
                    chatroom.getLatitude(), 
                    chatroom.getLongitude(), 
                    chatroom.stCreator(),
                    chatroom.numUsersGet(),
                    chatroom.timestampCreated());
            // Next load the messages for that chatroom.
            PullMessageResultSet pmrsMessages;
            if (!debug) {
            	pmrsMessages = ChatManager.pmrsChatLogGet(chatroom);
            } else {
            	pmrsMessages = cm.pmrsChatLogGet(chatroom);
            }
            if (!pmrsMessages.fIsSucceeded()) {
                // Else remove all rooms and return failure.
                chatModel = null;
                chatModel = new HallwayModel();
                return false;
            } else {
                // Load the messages into the correct room.
                chatModel.addRgMiToCrm(pmrsMessages.getRgmessage(), chatroom.stId());
            } 
        }
        return true;
    }
    
    /**
     * Adds a new message to the room. It also broadcasts to the chatroom activity if visible about the 
     * new message, prompting it to refresh itself.
     * 
     * @param msginfo  Message Info
     * @param roomId   Rooms Id
     * @param context  Context used to send broadcast.
     */
    public void addMessageToRoom(MessageInfo msginfo, String roomId, Context context) {
        if (chatModel.fChatRoomExists(roomId)) {
            chatModel.addMiToCrm(msginfo, roomId);
            
            // Alert ChatRoomActivity if running to retrieve the new message by refreshing view state.
            CommonUtilities.sendNewMessageAlert(context, roomId);
        }
    }
    
    /**
     * Returns an immutable list of messages that correspond to the given chatroom
     * 
     * @param roomId The rooms Id to retrieve messages from.
     * @return List of messages in an immutable read only list.
     */
    public List<MessageInfo> getMessagesFromChatRoom(String roomId) {
        return chatModel.rgmiGetFromCrmId(roomId);
    }
    
    /**
     * Gets a list of chatroominfo that represent the current state of the rooms available.
     * This method should be called after a successful initialize.
     * 
     * @return
     */
    public List<ChatRoomInfo> getChatRooms() {
        return chatModel.getRoomInfo();
    }
    
    /**
     * Adds the specified room to the chatcontroller. This subsequently makes it load all the messages from the
     * server, keeping a cache of the messages. This is the same as saying the user joins the room.
     * 
     * NOTE: This method should only be called in an async task.
     * 
     * @param chatroom ChatRoomInfo describing the room to join.
     * @return True if succeeded, false if otherwise.
     */

    public boolean joinRoom(ChatRoomInfo chatroom, boolean fAnon) {
        RequestResultSet rrs;
        if (!debug) {
            rrs = ChatManager.rrsJoinRoom(userinfo, chatroom, fAnon);
        } else {
            rrs = cm.rrsJoinRoom(userinfo, chatroom, fAnon);
        }
        if (!rrs.getfSucceeded()) {
            return false;
        }
        chatModel.addRoom(chatroom.stName(), 
                chatroom.stId(), 
                chatroom.stDescription(), 
                chatroom.getLatitude(), 
                chatroom.getLongitude(), 
                chatroom.stCreator(), 
                chatroom.timestampCreated());
        // Next load the messages for that chatroom.
        PullMessageResultSet pmrsMessages; 
		if (!debug) {
    		pmrsMessages = ChatManager.pmrsChatLogGet(chatroom);
		} else {
			pmrsMessages = cm.pmrsChatLogGet(chatroom);
		}
        if (!pmrsMessages.fIsSucceeded()) {
            // Else remove room and return failure
            chatModel.fRemoveRoom(chatroom.stId());
            return false;
        } else {
            // Load the messages into the correct room.
            chatModel.addRgMiToCrm(pmrsMessages.getRgmessage(), chatroom.stId());
        }
        return true;
    }
    
    /**
     * Leaves the specified room from the chatcontroller. This is the same as saying the user leaves the room.
     * 
     * @param chatroom
     * @return
     */
    public boolean leaveRoom(ChatRoomInfo chatroom) {
    	RequestResultSet crrs;
    	if (!debug) {
    		crrs = ChatManager.rrsLeaveRoom(userinfo, chatroom);
    	} else {
    		crrs = cm.rrsLeaveRoom(userinfo, chatroom);
    	}
    		
        if (!crrs.getfSucceeded()) {
            return false;
        }
        return chatModel.fRemoveRoom(chatroom.stId());
    }
    
    /**
     * Gets the current user logged in.
     * 
     * @return UserInfo describing the current user. null if logged out.
     */
    public UserInfo getUser() {
        return userinfo;
    }
    
    /**
     * This method is used to uninitialize the chat controller. This should be used when a user is logged out.
     */
    public void uninitialize() {
        userinfo = null;
        chatModel = null;
        chatModel = new HallwayModel();
        cm = null;
    }
    
    /**
     * Checks to see if the chatroom is already present in the controller. This is
     * same as saying that the user has already joined the room.
     * 
     * @param cri ChatRoomInfo
     * @return true if user is has already joined the chatroom and false if not.
     */
    public boolean fIsAlreadyJoined(ChatRoomInfo cri) {
        List<ChatRoomInfo> rgCri = getChatRooms();
        for (ChatRoomInfo roominfo : rgCri) {
            if (roominfo.stId().equals(cri.stId())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Posts a message to the chat server.
     * 
     * @param userinfo users info
     * @param chatroominfo chat rooms info
     * @param messageinfo messages info
     * 
     * @return RequestResultSet indicates if its successful or not.
     */
    public RequestResultSet rrsPostMessage(UserInfo userinfo, ChatRoomInfo chatroominfo, MessageInfo messageinfo) {
        return ChatManager.rrsPostMessage(userinfo, chatroominfo, messageinfo);
    }
    
    /*
     * TESTING Methods
     */
    
    /**
     * Sets the controller to be in debug mode and allows the ChatManager to be redefined.
     * 
     * @param fDebugMode
     */
    public void setDebugMode(boolean fDebugMode) {
    	this.debug = fDebugMode;
    }
    
    public void setChatManager(IChatManager cm) {
    	this.cm = cm;
    }
    
    /**
     * For testing purposes
     */
    public void fInitializeTest(UserInfo userinfo) {
        this.userinfo = userinfo;
    }
}
