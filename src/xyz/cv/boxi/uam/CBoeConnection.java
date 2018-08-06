//Business Objects Enterprise User Account Manager
//An application for maintaining failback indentities in SAP Business Objects Enterprise
//v0.2.0
//
//MIT License
//(c) 2018, Christopher Vincent. All rights reserved. 
//https://github.com/chrisDSP/boe-user-account-manager

package xyz.cv.boxi.uam;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.crystaldecisions.sdk.exception.SDKException;
import com.crystaldecisions.sdk.framework.CrystalEnterprise;
import com.crystaldecisions.sdk.framework.IEnterpriseSession;
import com.crystaldecisions.sdk.occa.infostore.IInfoStore;
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


public class CBoeConnection {	
	
	private static final Logger logger = LogManager.getLogger();	
	public IEnterpriseSession boEnterpriseSession;
	public IInfoStore boInfoStore = null;
	SDKException failure = null;	
	private String query = null;
	private class CredentialObject {
		//until a class for credential/auth token deserialization is complete, the application must have plain text credentials input here. 
		final String cms = ""; //CMS server formatted as follows: myserver.com:cmsportnumber
		final String user = ""; //user name
		final String pass = ""; //password
		final String authtype = ""; //authentication provide. probably "secEnterprise"
	}
	
	private CredentialObject CO = new CredentialObject(); 	
	CBoeConnection() {		
		logger.info("Attempting to log in to Business Objects Enterprise...");
		try {
			boEnterpriseSession = CrystalEnterprise.getSessionMgr().logon(CO.user, CO.pass, CO.cms, CO.authtype);
			boInfoStore = (IInfoStore)boEnterpriseSession.getService("", "InfoStore");
			logger.info("Successfully connected to the Business Objects Enterprise CMS. Attempting to extract security metadata...");
		}		
		catch(SDKException e) {
	    	logger.error("Error encountered while connecting to Business Objects Enterprise. Crystal SDK message: " + e.getDetailMessage());
	    	}
	}
	
	void DisconnectFromBOE ( ) {
		if(boEnterpriseSession != null) {
            try{ 
            	boEnterpriseSession.logoff();
            } catch(Exception e_ignore_logoff_exceptions) {}
            logger.info("Successfully disconnected from the Business Objects Enterprise CMS.");
		}
	}
	
	public IInfoObjects QueryRunner(String myquery) {		
		query = myquery;
		IInfoObjects results = null;
		try {
			results = boInfoStore.query(query);			
		}
		catch(Exception e) {
			logger.error("An error occurred while submitting the query: " + e);
		}
		return results;
	}
	
	public void Commit (IInfoObjects commitQuery) {
		try {
			boInfoStore.commit(commitQuery);
		}
		
		catch(Exception e) {
			logger.error("An error occurred while commiting the change to the CMS database: " + e);
		}
	}
		

}
