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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class CUserObject {

	private static final Logger logger = LogManager.getLogger();
	public Integer UserID;
	private IInfoObjects myQuery;	
	public IInfoObject myUserInfoObj;
	public IUser myUserCast;	
	public String myUserTitle;
	public String myUserName;
	public boolean missingAliasFlag;
	public IUserAliases myAliases;
	private IUserAlias myNewAlias;
	private Set<String> myAliasTypes = new HashSet<>();
	
	//CONSTRUCTOR: This object will always be instantiated by UserID.
	public CUserObject(Integer myUserID) {		
		UserID = myUserID;		
		myQuery = Main.BoConn.QueryRunner("SELECT * "
                + " FROM CI_SYSTEMOBJECTS "
                + " WHERE SI_KIND='USER'"                    
                + " AND SI_ID ='"
                + UserID + "'");		
		myUserInfoObj = (IInfoObject)myQuery.get(0);
		myUserCast = (IUser)myUserInfoObj;		
		myUserTitle = myUserCast.getTitle();
		myAliases = myUserCast.getAliases();		
		logger.info("User title: " + myUserTitle + " is being processed.");		
		Iterator Aliases = myAliases.iterator();		
		while(Aliases.hasNext()) {
			IUserAlias thisAlias = (IUserAlias)Aliases.next();
			myAliasTypes.add(thisAlias.getAuthentication());
			logger.info("One of my aliases is of auth type: " + thisAlias.getAuthentication() + " with name: " + thisAlias.getName() + " .");			
		}		
		evaluateAliases();
	}
	
	private void evaluateAliases() {
		if ((myAliasTypes.contains("secWinAD")&&!(myAliasTypes.contains("secEnterprise")))) {
			logger.debug("WinAD present but secEnterprise missing!");			
		}
		
		if ((myAliasTypes.contains("secEnterprise")&&!(myAliasTypes.contains("secWinAD"))))  {
			logger.debug("Remove this user immediately!");	
		}
	}
	
	private void addSecEntAlias() {
		//add a secEnterprise alias. final arg is 'false' to immediately disable the alias to allow for the systems administrator to control access to the failover identity. 
		myNewAlias = myAliases.addExisting("secEnterprise" + ":" + myUserCast.getTitle(),"secEnterprise" + ":#" + myUserCast.getID(), false);		
		Main.BoConn.Commit(myQuery);
		logger.info("User Alias addition was commited to CMS!");		
	}	
}
