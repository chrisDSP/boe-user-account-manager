# boe-user-account-manager
An application for maintaining failback indentities in SAP Business Objects Enterprise

BOE User Account Manager addresses an issue with SAP Business Objects Enterprise where user identities and content are deleted when a user's only login alias becomes disabled.

BOE UAM queries the Business Objects Enterprise CMS, checks for identities with only a WinAD authentication alias, and creates a fallback secEnterprise alias for the user. 

The application also analyzes the CMS permissions tree for the WinAD authentication provider and mirrors its structure for the secEnterprise authentication provider.

BOE UAM requires SAP's proprietary libraries to query and update CMS metadata. See the SAP Business Objects Java Developer guide for more information. 

Aside from these, the only other dependency requirements at this time are log4j2. 
