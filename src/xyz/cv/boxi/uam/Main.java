//Business Objects Enterprise User Account Manager
//An application for maintaining fallback indentities in SAP Business Objects Enterprise
//v0.2.0
//
//MIT License
//(c) 2018, Christopher Vincent. All rights reserved. 
//https://github.com/chrisDSP/boe-user-account-manager
	
package xyz.cv.boxi.uam;

import com.crystaldecisions.sdk.exception.*;
import com.businessobjects.multidimensional.data.tom.mdx.types.descflag.*;
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


public class Main {

	private static final Logger logger = LogManager.getLogger();	
	public static CBoeConnection BoConn = new CBoeConnection();	
	public static Set<Integer> AllADUserIDs = new HashSet<>();	
	public static void main(String[] args) {	
	    	logger.info("Attempting to log in to Business Objects Enterprise...");      
	        IInfoObjects groups = null; //global. all groups. we pass each AD and ENT group to the constructor for GroupProcessor
	        groups = BoConn.QueryRunner("SELECT * "
                    + " FROM CI_SYSTEMOBJECTS "
                    + " WHERE SI_PROGID='CrystalEnterprise.USERGROUP'"                    
                    + " ORDER BY SI_ID ASC ");
	        Iterator infoObjIter = groups.iterator();	        
	        while (infoObjIter.hasNext() ) {	
	        	IInfoObject filtergroup = (IInfoObject)infoObjIter.next();		        	
	        	CGroupObject thisGroup = new CGroupObject(filtergroup);	        	
	        	if (thisGroup.userChildrenFlag==true) {	        		
	        		logger.info("Check is necessary for : " + thisGroup.myGroupTitle + ". Calling checkNiblings()");	
	        		thisGroup.checkNiblings();	        		
	        	}	        	
	        	else {	        		
	        		logger.info("No check necessary for : " + thisGroup.myGroupTitle + ".");
	        	}
	        }	      
	        logger.info("Master user set contains: " + AllADUserIDs.size() + " distinct user IDs.");	        
	        for (Integer thisID : AllADUserIDs) {	        	
	        	CUserObject thisUser = new CUserObject(thisID);
	        }
	        logger.info("Main program loop finished.");
	        BoConn.DisconnectFromBOE();		
	}
}
