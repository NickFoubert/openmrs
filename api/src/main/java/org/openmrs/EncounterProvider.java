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

package org.openmrs;

/**
 * Mapping Class between Encounters and Providers which allows many to many relationship.
 */
public class EncounterProvider extends BaseOpenmrsData {
	
	private int encounterProviderId;
	
	private int encounterId;
	
	private int providerId;
	
	private int encounterRoleId;
	
	public void setEncounterProviderId(Integer encounterProviderId) {
		this.encounterProviderId = encounterProviderId;
	}
	
	public Integer getEncounterProviderId() {
		return this.encounterProviderId;
	}
	
	@Override
	public Integer getId() {
		return getEncounterProviderId();
	}
	
	@Override
	public void setId(Integer id) {
		setEncounterProviderId(id);
	}
	
	public int getEncounterId() {
		return encounterId;
	}
	
	public void setEncounterId(int encounterId) {
		this.encounterId = encounterId;
	}
	
	public int getProviderId() {
		return providerId;
	}
	
	public void setProviderId(int providerId) {
		this.providerId = providerId;
	}
	
	public int getEncounterRoleId() {
		return encounterRoleId;
	}
	
	public void setEncounterRoleId(int encounterRoleId) {
		this.encounterRoleId = encounterRoleId;
	}
}
