/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.web.dwr;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Provider;
import org.openmrs.api.APIException;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.messagesource.MessageSourceService;

/**
 * DWR Provider methods. The methods in here are used in the webapp to get data from the database
 * via javascript calls.
 * 
 * @see PatientService
 */

public class DWRProviderService {
	
	private static final Log log = LogFactory.getLog(DWREncounterService.class);
	
	public Vector<Object> findProvider(String phrase, boolean includeVoided, Integer start, Integer length) {
		Vector<Object> providerListItem = new Vector<Object>();
		List<Provider> providerList = Context.getProviderService().getProviders(phrase, start, length);
		
		if (providerList.size() == 0) {
			MessageSourceService mss = Context.getMessageSourceService();
			providerListItem.add(mss.getMessage("Provider.noMatchesFound", new Object[] { phrase }, Context.getLocale()));
		} else {
			for (Provider p : providerList) {
				if (!p.isRetired() || includeVoided == true)
					providerListItem.add(new ProviderListItem(p));
			}
		}
		return providerListItem;
	}
	
	public Map<String, Object> findProviderCountAndProvider(String phrase, boolean includeVoided, Integer start,
	                                                        Integer length) throws APIException {
		Map<String, Object> providerMap = new HashMap<String, Object>();
		Vector<Object> objectList = findProvider(phrase, includeVoided, start, length);
		try {
			providerMap.put("count", Context.getProviderService().getCountOfProviders(phrase));
			providerMap.put("objectList", objectList);
		}
		catch (Exception e) {
			log.error("Error while searching for encounters", e);
			objectList.clear();
			objectList.add(Context.getMessageSourceService().getMessage("Provider.search.error") + " - " + e.getMessage());
			providerMap.put("count", 0);
			providerMap.put("objectList", objectList);
		}
		return providerMap;
	}
	
}
