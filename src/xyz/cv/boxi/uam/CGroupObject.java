//Business Objects Enterprise User Account Manager
//An application for maintaining fallback indentities in SAP Business Objects Enterprise
//v0.2.0
//
//MIT License
//(c) 2018, Christopher Vincent. All rights reserved. 
//https://github.com/chrisDSP/boe-user-account-manager

package xyz.cv.boxi.uam;

import com.crystaldecisions.sdk.exception.*;
import com.businessobjects.multidimensional.data.tom.mdx.types.descflag.SELF;
import com.businessobjects.sdk.plugin.desktop.common.*;
import com.crystaldecisions.sdk.framework.*;
import com.crystaldecisions.sdk.plugin.*;
import com.crystaldecisions.sdk.plugin.desktop.user.*;
import com.crystaldecisions.sdk.plugin.desktop.usergroup.*;
import com.crystaldecisions.sdk.plugin.desktop.common.*;
import com.crystaldecisions.sdk.occa.infostore.*;
import com.crystaldecisions.sdk.occa.security.*;
import com.crystaldecisions.sdk.occa.pluginmgr.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CGroupObject {
	private static final Logger logger = LogManager.getLogger();
	public IInfoObject myGroupInfoObj;
	public IUserGroup myGroupCast;
	public String myGroupTitle;
	public String myGroupType;
	public int myID;
	public int myCountParentGroups;
	public int myCountChildGroups;
	public Set<Integer> myParentsIDs;
	private IInfoObjects myQuery;
	public IInfoObject myParentInfoObj;
	public IUserGroup myParentCast;
	private int objID;
	private int complementID;
	private String complementName;
	public boolean userChildrenFlag;
	public boolean complementPresent = false;
	Set<Integer> allmyusers = null;
	Set<Integer> allmychildgroupIDs = null;
	Set<IInfoObject> allmychildgroupIInfoObjects = null;
	Set<Integer> missingNephews;
	Map<String, Integer> childnamesWithID = new HashMap<String, Integer>();
	Set<Integer> allmyparents;
	Set<Integer> mySiblingsIDs;	
	
	//CONSTRUCTOR 1: BY IINFOOBJECT
	//	Initial all-group bulk query returns an IInfoObjects (IInfoObject collection)
	
	public CGroupObject (IInfoObject thisgroup) {
		myID = thisgroup.getID();
		myGroupTitle = thisgroup.getTitle();
		myGroupCast = (IUserGroup)thisgroup;
		myCountParentGroups = myGroupCast.getParentGroups().toArray().length;    	
		myParentsIDs = (Set<Integer>)myGroupCast.getParentGroups();		
		myGroupType = getMyGroupType();		
	}
	
	//CONSTRUCTOR 2: BY ID
	//	For looking up a group's parents from IUserGroup.getParentGroups()	
	public CGroupObject (int thisid) {
		myQuery = Main.BoConn.QueryRunner("SELECT * "
                + " FROM CI_SYSTEMOBJECTS "
                + " WHERE SI_PROGID='CrystalEnterprise.USERGROUP'"                    
                + " AND SI_ID ='"
                + thisid + "'");	
		myGroupInfoObj = (IInfoObject)myQuery.get(0);
		myGroupTitle = myGroupInfoObj.getTitle();
		myGroupCast = (IUserGroup)myGroupInfoObj;					
		myID = thisid;
		myParentsIDs = (Set<Integer>)myGroupCast.getParentGroups();	
		myGroupType = getMyGroupType();
	}
	
	private String getMyGroupType() {
		
		if (myGroupTitle.toUpperCase().contains("AD\\")) {			
			complementName = myGroupTitle.toUpperCase().replace("AD\\", "ENT\\");			
			userChildrenFlag = true;			
			//add AD users to the master list
			mergeADUsersToMasterSet();			
			return "AD";		
		}
		
		if (myGroupTitle.toUpperCase().contains("ENT\\")) {			
			complementName = myGroupTitle.toUpperCase().replace("ENT\\", "AD\\");			
			userChildrenFlag = true;			
			return "ENT";			
		}		
		else {			
			complementName = "No complement.";			
			userChildrenFlag = false;			
			return "ELSE";			
		}
	}
	
	private void mergeADUsersToMasterSet() {		
		Main.AllADUserIDs.addAll(getMyUsers());			
	}
	
	public Set<IInfoObject> getMyChildGroups() {			
		allmychildgroupIDs = (Set<Integer>)myGroupCast.getSubGroups();		 
		Set<IInfoObject> IInfoObjectSet = new HashSet<IInfoObject>();		
		Iterator childiterator = allmychildgroupIDs.iterator();		
		while (childiterator.hasNext()) {			
			Integer thischild = (Integer)childiterator.next();			
			IInfoObject thischildIUG = null;			
			logger.debug("Attempting to query for the group with ID: " + thischild + ".");			
			myQuery = Main.BoConn.QueryRunner("SELECT * "
	                + " FROM CI_SYSTEMOBJECTS "
	                + " WHERE SI_PROGID='CrystalEnterprise.USERGROUP'"                    
	                + " AND SI_ID ='"
	                + thischild + "'");		
			thischildIUG = (IInfoObject)myQuery.get(0);		
			IInfoObjectSet.add(thischildIUG);
			logger.debug("Adding IInfoObject for group: " + thischildIUG.getTitle() + " to the Set.");
		}		
		return IInfoObjectSet; 	 
	}	
	
	public Set<Integer> getMyUsers() {		
		logger.info("Call made to return the group called " + myGroupTitle + "'s user ID set, which contains: " + myGroupCast.getUsers().toArray().length + " users.");
		return (Set<Integer>)myGroupCast.getUsers();				
	}
	
	public void checkNiblings() {
		//if I am a group with users for children, check my parents' children (siblings) for a complementary group.
			//if one doesn't exist, set a flag
			//if one does exist, check the user membership Sets for equality
				//if they are not equal, make them equal.
		
		//if I am a group with other groups for children (no direct user membership), forget me and continue processing other groups.
		
		Iterator ParentIterator = myParentsIDs.iterator();		
		while (ParentIterator.hasNext()) {			
			//instantiate a new CGroupObject for each of this group's parent groups
			CGroupObject thisParent = new CGroupObject((int)ParentIterator.next());			
			logger.info("checkNiblings() method says:" + " my parent group is: " + thisParent.myGroupTitle + ".");			
			allmychildgroupIInfoObjects = thisParent.getMyChildGroups();			
			//remove the current group IInfoObject from my parent's children to get the current group's siblings only			
			Iterator allmysiblings = allmychildgroupIInfoObjects.iterator();			
			while (allmysiblings.hasNext()) {								
				CGroupObject thisSibling = new CGroupObject((IInfoObject)allmysiblings.next());				
				if (thisSibling.myGroupTitle.toUpperCase().equals(myGroupTitle)) {					
					continue;					
				}						
				logger.debug("One of the siblings of group: " + myGroupTitle + " is called: " + thisSibling.myGroupTitle.toUpperCase() + ".");
				logger.debug("Looking for: " + complementName + ".");					
				if (thisSibling.myGroupTitle.toUpperCase().equals(complementName)) {					
					complementPresent = true;
					logger.info("The group: " + thisSibling.myGroupTitle.toUpperCase() + " is my compliment. Proceed to user membership set comparison.");					
					if (thisSibling.getMyUsers() == allmyusers) {						
						logger.info("The group sets are equal. Membership is the same between these two groups.");		
					}					
					else {						
						logger.info("Membership is unequal!");						
						//Call ticketing system SOAP service to create an incident ticket. 
					}					
				}									
			}
			
			if (complementPresent!=true) {				 
					logger.info("No complementary sibling group found.");					
					if (myGroupType == "AD\\") {
						//typical case. go ahead and attempt to create a complementary group. 
						balanceComplementGroup();
					}					
					if (myGroupType == "ENT\\") {
						//extremely unusual case. create a service ticket so this can be reviewed. 
						logger.error("User security metadata problem. Creating an incident ticket to flag this case for manual review.");
						//Call ticketing system SOAP service to create an incident ticket. 
					}			
			}		
		}
		
	}
	
	public void balanceUserMembership (CGroupObject unbalancedComplement) {		
		Set<Integer> usersToAdd = new HashSet<Integer>();		
		Set<Integer> usersToRemove = new HashSet<Integer>();			
		CGroupObject thisComplement = unbalancedComplement;		
		//in cases for user addition, we'll consider the 'AD\' group membership to be authoritative and we'll immediately balance membership.
		if (myGroupType == "AD\\") {			
			for (Integer thisUser : allmyusers) {				
				if (!(thisComplement.allmyusers.contains(thisUser))) {					
					logger.debug("User with ID: " + thisUser + " is not present in my complementary group, called: " + thisComplement.myGroupTitle +". Queueing user for addition.");
					usersToAdd.add(thisUser);												
				}																		
			}		
			for (Integer thisUser : thisComplement.allmyusers) {				
				if (!(allmyusers.contains(thisUser))) {					
					logger.debug("User with ID: " + thisUser + " is present in my complement, " + thisComplement.myGroupTitle + " but not me. Creating ticket so deletion can be processed.");
					usersToRemove.add(thisUser);														
				}				
			}
			
		}		
		if (usersToAdd.size()>0) {			
			thisComplement.addUsers(usersToAdd, thisComplement.myID);			
		}		
		if (usersToRemove.size()>0) {
			logger.debug("Creating an incident ticket to flag manual removal of: " + usersToRemove.size() + " users from the group: " + thisComplement.myGroupTitle + ".");
			//Call ticketing system SOAP service to create an incident ticket. 
		}		
	}
	
	public void balanceComplementGroup () {
		//create an ENT\ group that needs to exist
		//objects used for group addition, new IInfoObjects for commit		
		IInfoObjects newGroupIInfoObjects = null; 
		IPluginMgr newGroupPluginMgr = null;
		IPluginInfo newUserGroupPlugin = null;
		IUserGroup newUserGroupPluginInfo = null; 
	   	IInfoObject newGroup = null;
	   	Set<Integer> newGroupUsers = null;		
		try {	
		   	newGroupPluginMgr = Main.BoConn.boInfoStore.getPluginMgr();			
			newUserGroupPlugin = newGroupPluginMgr.getPluginInfo("CrystalEnterprise.UserGroup");
			newGroupIInfoObjects = Main.BoConn.boInfoStore.newInfoObjectCollection();
			newGroup = newGroupIInfoObjects.add(newUserGroupPlugin);
			newGroup.setTitle(complementName);
			newGroup.setDescription("Autocreated by UAM Utility.");
			newUserGroupPluginInfo = (IUserGroup) newGroup;
			newGroupUsers = newUserGroupPluginInfo.getUsers();
			newGroupUsers.addAll(allmyusers);
			Main.BoConn.boInfoStore.commit(newGroupIInfoObjects);		
		}
		catch (Exception e) {
			logger.error("Error occurred during the creation of a complementary group: " + e);
			//Call ticketing system SOAP service to create an incident ticket. 
		}		
		logger.info("A complementary group called: " + complementName + " was created and populated with all " + allmyusers.size() + " of the users from " + myGroupTitle + ".");			
	}

	public void addUsers (Set<Integer> users, Integer complementID) {
		//we don't really need to pass the ID. this will be cleaned up in the next release.
		Set<Integer> _users = users;
		Integer addToGroup = complementID;		
		for (Integer user : _users) {			
			IUser thisIUser = null;
			IInfoObjects myQuery = null;
			myQuery = Main.BoConn.QueryRunner("SELECT * "
	                + " FROM CI_SYSTEMOBJECTS "
	                + " WHERE SI_PROGID='CrystalEnterprise.User'"                    
	                + " AND SI_ID ='"
	                + user + "'");
			thisIUser = (IUser)myQuery.get(0);
			thisIUser.getGroups().add(addToGroup);
			try {
				Main.BoConn.boInfoStore.commit(myQuery);
			}
			catch (SDKException e) {				
				logger.error("Crystal SDK error encountered while committing a new user: " + e);				
			}			
			
		}		
		
	}

	

}
