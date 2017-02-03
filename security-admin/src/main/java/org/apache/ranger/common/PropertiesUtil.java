/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 /**
 *
 */
package org.apache.ranger.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.credentialapi.CredentialReader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;



public class PropertiesUtil extends PropertyPlaceholderConfigurer {
    private static Map<String, String> propertiesMap = new HashMap<String, String>();
    private static final Logger logger = Logger.getLogger(PropertiesUtil.class);
    protected List<String> xmlPropertyConfigurer  = new ArrayList<String>();

    private PropertiesUtil() {

    }

    @Override
    protected void processProperties(
	    ConfigurableListableBeanFactory beanFactory, Properties props)
	    throws BeansException {

    // First let's add the system properties
	Set<Object> keySet = System.getProperties().keySet();
	for (Object key : keySet) {
	    String keyStr = key.toString();
	    propertiesMap.put(keyStr, System.getProperties()
		    .getProperty(keyStr).trim());
	}

	// Let's add our properties now
	keySet = props.keySet();
	for (Object key : keySet) {
	    String keyStr = key.toString();
	    propertiesMap.put(keyStr, props.getProperty(keyStr).trim());
	}
	
	// update system trust store path with custom trust store.
	if (propertiesMap!=null && propertiesMap.containsKey("ranger.truststore.file")) {
		System.setProperty("javax.net.ssl.trustStore", propertiesMap.get("ranger.truststore.file"));
		System.setProperty("javax.net.ssl.trustStorePassword", propertiesMap.get("ranger.truststore.password"));
		System.setProperty("javax.net.ssl.trustStoreType", KeyStore.getDefaultType());
		if(!StringUtils.isEmpty(propertiesMap.get("ranger.truststore.file"))){
			Path path = Paths.get(propertiesMap.get("ranger.truststore.file"));
			if (!Files.exists(path) || !Files.isReadable(path)) {
				logger.debug("Could not find or read truststore file '"+propertiesMap.get("ranger.truststore.file")+"'");
			}
		}
	}

	// update system key store path with custom key store.
	if (propertiesMap!=null && propertiesMap.containsKey("ranger.keystore.file")) {
		System.setProperty("javax.net.ssl.keyStore", propertiesMap.get("ranger.keystore.file"));
		System.setProperty("javax.net.ssl.keyStorePassword", propertiesMap.get("ranger.keystore.password"));
		System.setProperty("javax.net.ssl.keyStoreType", KeyStore.getDefaultType());
		if(!StringUtils.isEmpty(propertiesMap.get("ranger.keystore.file"))){
			Path path = Paths.get(propertiesMap.get("ranger.keystore.file"));
			if (!Files.exists(path) || !Files.isReadable(path)) {
				logger.debug("Could not find or read keystore file '"+propertiesMap.get("ranger.keystore.file")+"'");
			}
		}
	}

	//update credential from keystore
	if(propertiesMap!=null && propertiesMap.containsKey("ranger.credential.provider.path") && propertiesMap.containsKey("ranger.jpa.jdbc.credential.alias")){
		String path=propertiesMap.get("ranger.credential.provider.path");
		String alias=propertiesMap.get("ranger.jpa.jdbc.credential.alias");
		if(path!=null && alias!=null){
			String xaDBPassword=CredentialReader.getDecryptedString(path.trim(),alias.trim());
			if(xaDBPassword!=null&& !xaDBPassword.trim().isEmpty() &&
					!xaDBPassword.trim().equalsIgnoreCase("none")){
				propertiesMap.put("ranger.jpa.jdbc.password", xaDBPassword);
				props.put("ranger.jpa.jdbc.password", xaDBPassword);
			}else{
				logger.info("Credential keystore password not applied for XA DB; clear text password shall be applicable");
			}
		}
	}
	if(propertiesMap!=null && propertiesMap.containsKey("ranger.credential.provider.path") && propertiesMap.containsKey("ranger.jpa.audit.jdbc.credential.alias")){
		String path=propertiesMap.get("ranger.credential.provider.path");
		String alias=propertiesMap.get("ranger.jpa.audit.jdbc.credential.alias");
		if(path!=null && alias!=null){
			String auditDBPassword=CredentialReader.getDecryptedString(path.trim(), alias.trim());
			if(auditDBPassword!=null&& !auditDBPassword.trim().isEmpty() &&
					!auditDBPassword.trim().equalsIgnoreCase("none")){
				propertiesMap.put("ranger.jpa.audit.jdbc.password", auditDBPassword);
				props.put("ranger.jpa.audit.jdbc.password", auditDBPassword);
			}else{
				logger.info("Credential keystore password not applied for Audit DB; clear text password shall be applicable");
			}
		}
	}
	if(propertiesMap!=null && propertiesMap.containsKey("ranger.authentication.method")){
		String authenticationMethod=propertiesMap.get("ranger.authentication.method");
		if(authenticationMethod!=null && (authenticationMethod.equalsIgnoreCase("ACTIVE_DIRECTORY")||authenticationMethod.equalsIgnoreCase("AD"))){
			if(propertiesMap!=null && propertiesMap.containsKey("ranger.credential.provider.path") && propertiesMap.containsKey("ranger.ldap.ad.binddn.credential.alias")){
				String path=propertiesMap.get("ranger.credential.provider.path");
				String alias=propertiesMap.get("ranger.ldap.ad.binddn.credential.alias");
				if(path!=null && alias!=null){
					String bindDNPassword=CredentialReader.getDecryptedString(path.trim(), alias.trim());
					if(bindDNPassword!=null&& !bindDNPassword.trim().isEmpty() &&
							!bindDNPassword.trim().equalsIgnoreCase("none")){
						propertiesMap.put("ranger.ldap.ad.bind.password", bindDNPassword);
						props.put("ranger.ldap.ad.bind.password", bindDNPassword);
					}else{
						logger.info("Credential keystore password not applied for AD Bind DN; clear text password shall be applicable");
					}
				}
			}
		}
	}
	if(propertiesMap!=null && propertiesMap.containsKey("ranger.authentication.method")){
		String authenticationMethod=propertiesMap.get("ranger.authentication.method");
		if(authenticationMethod!=null && (authenticationMethod.equalsIgnoreCase("LDAP"))){
			if(propertiesMap!=null && propertiesMap.containsKey("ranger.credential.provider.path") && propertiesMap.containsKey("ranger.ldap.binddn.credential.alias")){
				String path=propertiesMap.get("ranger.credential.provider.path");
				String alias=propertiesMap.get("ranger.ldap.binddn.credential.alias");
				if(path!=null && alias!=null){
					String bindDNPassword=CredentialReader.getDecryptedString(path.trim(), alias.trim());
					if(bindDNPassword!=null&& !bindDNPassword.trim().isEmpty() &&
							!bindDNPassword.trim().equalsIgnoreCase("none")){
						propertiesMap.put("ranger.ldap.bind.password", bindDNPassword);
						props.put("ranger.ldap.bind.password", bindDNPassword);
					}else{
						logger.info("Credential keystore password not applied for LDAP Bind DN; clear text password shall be applicable");
					}
				}
			}
		}
	}
	if(propertiesMap!=null && propertiesMap.containsKey("ranger.audit.source.type")){
		String auditStore=propertiesMap.get("ranger.audit.source.type");
		if(auditStore!=null && (auditStore.equalsIgnoreCase("solr"))){
			if(propertiesMap!=null && propertiesMap.containsKey("ranger.credential.provider.path") && propertiesMap.containsKey("ranger.solr.audit.credential.alias")){
				String path=propertiesMap.get("ranger.credential.provider.path");
				String alias=propertiesMap.get("ranger.solr.audit.credential.alias");
				if(path!=null && alias!=null){
					String solrAuditPassword=CredentialReader.getDecryptedString(path.trim(), alias.trim());
					if(solrAuditPassword!=null&& !solrAuditPassword.trim().isEmpty() &&
							!solrAuditPassword.trim().equalsIgnoreCase("none")){
						propertiesMap.put("ranger.solr.audit.user.password", solrAuditPassword);
						props.put("ranger.solr.audit.user.password", solrAuditPassword);
					}else{
						logger.info("Credential keystore password not applied for Solr; clear text password shall be applicable");
					}
				}
			}
		}
	}
	if(propertiesMap!=null){
		String sha256PasswordUpdateDisable="false";
		if(propertiesMap.containsKey("ranger.sha256Password.update.disable")){
			sha256PasswordUpdateDisable=propertiesMap.get("ranger.sha256Password.update.disable");
			if(sha256PasswordUpdateDisable==null || sha256PasswordUpdateDisable.trim().isEmpty()|| !"true".equalsIgnoreCase(sha256PasswordUpdateDisable)){
				sha256PasswordUpdateDisable="false";
			}
		}
		propertiesMap.put("ranger.sha256Password.update.disable", sha256PasswordUpdateDisable);
		props.put("ranger.sha256Password.update.disable", sha256PasswordUpdateDisable);
	}
	if(RangerBizUtil.getDBFlavor()==AppConstants.DB_FLAVOR_MYSQL){
		if(propertiesMap!=null && propertiesMap.containsKey("ranger.db.ssl.enabled")){
			String db_ssl_enabled=propertiesMap.get("ranger.db.ssl.enabled");
			if(StringUtils.isEmpty(db_ssl_enabled)|| !"true".equalsIgnoreCase(db_ssl_enabled)){
				db_ssl_enabled="false";
			}
			db_ssl_enabled=db_ssl_enabled.toLowerCase();
			if("true".equalsIgnoreCase(db_ssl_enabled)){
				String db_ssl_required=propertiesMap.get("ranger.db.ssl.required");
				if(StringUtils.isEmpty(db_ssl_required)|| !"true".equalsIgnoreCase(db_ssl_required)){
					db_ssl_required="false";
				}
				db_ssl_required=db_ssl_required.toLowerCase();
				String db_ssl_verifyServerCertificate=propertiesMap.get("ranger.db.ssl.verifyServerCertificate");
				if(StringUtils.isEmpty(db_ssl_verifyServerCertificate)|| !"true".equalsIgnoreCase(db_ssl_verifyServerCertificate)){
					db_ssl_verifyServerCertificate="false";
				}
				db_ssl_verifyServerCertificate=db_ssl_verifyServerCertificate.toLowerCase();
				propertiesMap.put("ranger.db.ssl.enabled", db_ssl_enabled);
				props.put("ranger.db.ssl.enabled", db_ssl_enabled);
				propertiesMap.put("ranger.db.ssl.required", db_ssl_required);
				props.put("ranger.db.ssl.required", db_ssl_required);
				propertiesMap.put("ranger.db.ssl.verifyServerCertificate", db_ssl_verifyServerCertificate);
				props.put("ranger.db.ssl.verifyServerCertificate", db_ssl_verifyServerCertificate);
				String ranger_jpa_jdbc_url=propertiesMap.get("ranger.jpa.jdbc.url");
				if(!StringUtils.isEmpty(ranger_jpa_jdbc_url)){
					StringBuffer ranger_jpa_jdbc_url_ssl=new StringBuffer(ranger_jpa_jdbc_url);
					ranger_jpa_jdbc_url_ssl.append("?useSSL="+db_ssl_enabled+"&requireSSL="+db_ssl_required+"&verifyServerCertificate="+db_ssl_verifyServerCertificate);
					propertiesMap.put("ranger.jpa.jdbc.url", ranger_jpa_jdbc_url_ssl.toString());
					props.put("ranger.jpa.jdbc.url", ranger_jpa_jdbc_url_ssl.toString());
					logger.info("ranger.jpa.jdbc.url="+ranger_jpa_jdbc_url_ssl.toString());
				}
			}
		}
	}
	super.processProperties(beanFactory, props);
    }

    public static String getProperty(String key, String defaultValue) {
	if (key == null) {
	    return null;
	}
	String rtrnVal = propertiesMap.get(key);
	if (rtrnVal == null) {
	    rtrnVal = defaultValue;
	}
	return rtrnVal;
    }

    public static String getProperty(String key) {
	if (key == null) {
	    return null;
	}
	return propertiesMap.get(key);
    }

    public static String[] getPropertyStringList(String key) {
	if (key == null) {
	    return null;
	}
	String value = propertiesMap.get(key);
	if (value != null) {
	    String[] splitValues = value.split(",");
	    String[] returnValues = new String[splitValues.length];
	    for (int i = 0; i < splitValues.length; i++) {
		returnValues[i] = splitValues[i].trim();
	    }
	    return returnValues;
	} else {
	    return new String[0];
	}
    }

    public static Integer getIntProperty(String key, int defaultValue) {
	if (key == null) {
	    return defaultValue;
	}
	String rtrnVal = propertiesMap.get(key);
	if (rtrnVal == null) {
	    return defaultValue;
	}
	return Integer.valueOf(rtrnVal);
    }

    public static long getLongProperty(String key, long defaultValue) {
		if (key == null) {
		    return defaultValue;
		}
		String rtrnVal = propertiesMap.get(key);
		if (rtrnVal == null) {
		    return defaultValue;
		}
		return Long.valueOf(rtrnVal);
    }

    public static Integer getIntProperty(String key) {
	if (key == null) {
	    return null;
	}
	String rtrnVal = propertiesMap.get(key);
	if (rtrnVal == null) {
	    return null;
	}
	return Integer.valueOf(rtrnVal);
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
	if (key == null) {
	    return defaultValue;
	}
	String value = getProperty(key);
	if (value == null) {
	    return defaultValue;
	}
	return Boolean.parseBoolean(value);
    }
	public static Map<String, String> getPropertiesMap() {
		return propertiesMap;
	}
	public static Properties getProps() {
		Properties ret = new Properties();
		if (propertiesMap != null) {
			ret.putAll(propertiesMap);
		}
		return ret;
	}
}
