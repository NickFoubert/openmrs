<%@ include file="/WEB-INF/template/include.jsp" %>

<openmrs:require privilege="View Encounters" otherwise="/login.htm" redirect="/admin/encounters/encounter.form" />

<c:choose>
<c:when test="${param.inPopup}">
<%@ include file="/WEB-INF/template/headerMinimal.jsp" %>
</c:when>
<c:otherwise>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>
</c:otherwise>
</c:choose>

<openmrs:htmlInclude file="/scripts/timepicker/timepicker.js" />
<openmrs:htmlInclude file="/dwr/interface/DWRVisitService.js"/>
<openmrs:htmlInclude file="/dwr/interface/DWREncounterService.js"/>
<openmrs:htmlInclude file="/dwr/interface/DWRProviderService.js" />

<script type="text/javascript">
	var providersCount = ${fn:length(encounter.providersByRoles)};
	var numberOfClonedElements = 0;
	
	$j(document).ready( function() {
		toggleVisibility(document, "div", "description");
		<c:if test="${encounter.encounterId != null}">
		toggleRowVisibilityForClass("obs", "voided", true);
		voidedClicked(document.getElementById("voided"));
		</c:if>
	});

	function mouseover(row, isDescription) {
		if (row.className.indexOf("searchHighlight") == -1) {
			row.className = "searchHighlight " + row.className;
			var other = getOtherRow(row, isDescription);
			other.className = "searchHighlight " + other.className;
		}
	}
	function mouseout(row, isDescription) {
		var c = row.className;
		row.className = c.substring(c.indexOf(" ") + 1, c.length);
		var other = getOtherRow(row, isDescription);
		c = other.className;
		other.className = c.substring(c.indexOf(" ") + 1, c.length);
	}
	function getOtherRow(row, isDescription) {
		if (isDescription == null) {
			var other = row.nextSibling;
			if (other.tagName == null)
				other = other.nextSibling;
		}
		else {
			var other = row.previousSibling;
			if (other.tagName == null)
				other = other.previousSibling;
		}
		return other;
	}
	function click(obsId) {
		document.location = "${pageContext.request.contextPath}/admin/observations/obs.form?obsId=" + obsId;
		return false;
	}
	
	function voidedClicked(input) {
		var reason = document.getElementById("voidReason");
		var voidedBy = document.getElementById("voidedBy");
		if (input) {
		if (input.checked) {
			reason.style.display = "";
			if (voidedBy)
				voidedBy.style.display = "";
		}
		else {
			reason.style.display = "none";
			if (voidedBy)
				voidedBy.style.display = "none";
		}
		}
	}

	var patientBoxFilledIn = false;
	function enableSaveButton(formFieldId, id) {
		if (formFieldId == "patientId")
			patientBoxFilledIn = true;

		// only enable if both the patient and at least one provider has been added
		if (patientBoxFilledIn && providersCount > 0)
			document.getElementById("saveEncounterButton").disabled = false;
	}
	
	function updateSaveButtonAndVisits(formFieldId, patientObj, isPageLoad){
		enableSaveButton(formFieldId, patientObj);
		//if this is on page load, we already have the patient's visits 
		//select populated with spring's referenced data in the http request
		if(!isPageLoad)
			updateVisits(patientObj);
	}
	
	//Repopulates the select element for visit with a new list of visits for the specified patient
	function updateVisits(patientObj){
		DWRVisitService.findVisitsByPatient(patientObj.patientId, false, false, function(visits) {
			var options = '<option value=""></option>';
			if(visits){
		    	for (var i = 0; i < visits.length; i++) {
		    		options += '<option value="' + visits[i].visitId + '">' + visits[i].startDatetimeString +
		    		' ' + visits[i].visitType + ' ' +visits[i].personName + 
		    		((visits[i].indicationConcept) ? ' '+visits[i].indicationConcept :'') + 
		    		((visits[i].location) ? ' '+visits[i].location :'') + '</option>';
		    	}
			}

			$j("select#visitSelect").html(options);
		});
	}

	function removeProvider(buttonObj) {
		removeNode(buttonObj.parentNode.parentNode);
		providersCount--;
		if(providersCount == 0)
			$j("saveEncounterButton").attr('disabled', 'disabled');
		
	}
	
	function addProvider(providerObj){
		var index = ${fn:length(encounter.providersByRoles)}+numberOfClonedElements;
		var providerTemplateRow = document.getElementById("addNewProviderTemplate");
		var newRow = providerTemplateRow.cloneNode(true);
		newRow.id = '';
		var inputs = newRow.getElementsByTagName("input");
		var errorSpans = newRow.getElementsByTagName("span");
		var selects = newRow.getElementsByTagName("select");
		var displayInputObj = null;
		var formFieldObj = null;
		var selectElement = null;
		for (var i = 0; i < inputs.length; i++) {
			var input = inputs[i];
			if(input){
				if(input.type == 'text' && input.name == 'providerName') {
					input.id = input.id.replace('[x]', '[' + index + ']');
					displayInputObj = input;
				}else if(input.type == 'hidden' && input.name == 'providerIds') {
					input.id = input.id.replace('[x]', '[' + index + ']');
					$j(input).addClass('providerInput');
					formFieldObj = input;
				}
			}
		}
		//find the role select element
		for (var i = 0; i < selects.length; i++) {
			var select = selects[i];
			if(select && select.name == 'encounterRoleIds') {
				select.id = select.id.replace('[x]', '[' + index + ']');
				$j(select).addClass('roleSelect');
				selectElement = select;
			}
		}
		//find the error messages spans and assign them ids
		for (var i = 0; i < errorSpans.length; i++) {
			var span = errorSpans[i];
			if(span.id == 'providerErrors') {
				span.id = formFieldObj.id+'-errors';
				continue;
			}else if(span.id == 'roleErrors') {
				span.id = selectElement.id+'-errors';
				continue;
			}
		}
		
		providerTemplateRow.parentNode.insertBefore(newRow, providerTemplateRow);
		addAutoComplete(displayInputObj.id, formFieldObj.id, new CreateCallback().providerCallback(), 'providerId',
				'<spring:message code="Provider.search.placeholder" javaScriptEscape="true"/>')
		$j(newRow).show();
		providersCount++;
		numberOfClonedElements++;
		enableSaveButton(null, null);
	}
	
	//Called when the form is submitted to check if the roles and providers have been set for new providers.
	//It also removes the hidden provider template row
	function onFormSubmission(){
		var hasErrors = false;
		$j.each($j('.providerInput'), function(index, providerInput) {
				if($j(providerInput).val() == ''){
					hasErrors = true;
					document.getElementById(providerInput.id+'-errors').style.display = '';
				}else
					document.getElementById(providerInput.id+'-errors').style.display = 'none';
		});
		$j.each($j('.roleSelect'), function(index, roleSelect) {
			if(roleSelect.selectedIndex == 0){
				hasErrors = true;
				document.getElementById(roleSelect.id+'-errors').style.display = '';
			}else
				document.getElementById(roleSelect.id+'-errors').style.display = 'none';
		});
		
		if(hasErrors)
			return false;
		
		removeNode(document.getElementById("addNewProviderTemplate"));
		
		return true;
	}
	
</script>

<style>
	#table th { text-align: left; }
	td.fieldNumber { 
		width: 5px;
		white-space: nowrap;
	}
</style>

<c:if test="${encounter.patient.patientId != null}">
<a href="../../patientDashboard.form?patientId=${encounter.patient.patientId}"><spring:message code="patientDashboard.viewDashboard"/></a>
</c:if>

<h2><spring:message code="Encounter.manage.title"/></h2>

<spring:hasBindErrors name="encounter">
	<spring:message code="fix.error"/>
	<div class="error">
		<c:forEach items="${errors.allErrors}" var="error">
			<spring:message code="${error.code}" text="${error.code}"/><br/>
		</c:forEach>
	</div>
	<br />
</spring:hasBindErrors>

<b class="boxHeader"><spring:message code="Encounter.summary"/></b>
<form method="post" onsubmit="return onFormSubmission()">
<div class="box">
	<table cellpadding="3" cellspacing="0">
		<tr>
			<th><spring:message code="Encounter.patient"/></th>
			<td>
				<spring:bind path="encounter.patient">
					<openmrs_tag:patientField formFieldName="patientId" searchLabelCode="Patient.find" initialValue="${status.value.patientId}" linkUrl="${pageContext.request.contextPath}/admin/patients/patient.form" callback="updateSaveButtonAndVisits" allowSearch="${encounter.encounterId == null}"/>
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th><spring:message code="Encounter.location"/></th>
			<td>
				<spring:bind path="encounter.location">
					<openmrs_tag:locationField formFieldName="location" initialValue="${status.value}"/>
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th><spring:message code="Encounter.datetime"/></th>
			<td>
				<spring:bind path="encounter.encounterDatetime">			
					<input type="text" name="${status.expression}" size="20" 
						   value="${status.value}" onfocus="showDateTimePicker(this)" />
				   (<spring:message code="general.format"/>: <openmrs:dateTimePattern />)
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if> 
				</spring:bind>
			</td>
		</tr>
		<tr>
		<th><spring:message code="Encounter.visit" /></th>
		<td>
			<spring:bind path="encounter.visit">
				<select id="visitSelect" name="${status.expression}">
					<option value=""></option>
					<c:forEach items="${patientVisits}" var="visit">
						<option value="${visit.visitId}" <c:if test="${visit.visitId == status.value}">selected="selected"</c:if>>
							 <openmrs:formatDate date="${visit.startDatetime}" />
							 ${visit.visitType.name} ${visit.patient.personName}
							<c:if test="${visit.indication != null}"> ${visit.indication.name}</c:if>
							<c:if test="${visit.location != null}"> ${visit.location}</c:if>
						</option>
					</c:forEach>
				</select>
				<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
			</spring:bind>
		</td>
		</tr>
		<tr>
			<th><spring:message code="Encounter.type"/></th>
			<td>
				<spring:bind path="encounter.encounterType">
					<c:choose>
						<c:when test="${encounter.encounterId == null}">
							<select name="encounterType">
								<c:forEach items="${encounterTypes}" var="type">
									<option value="${type.encounterTypeId}" <c:if test="${type.encounterTypeId == status.value}">selected</c:if>>${type.name}</option>
								</c:forEach>
							</select>
						</c:when>
						<c:otherwise>
							${encounter.encounterType.name}
						</c:otherwise>
					</c:choose>
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</tr>
		<tr>
			<th><spring:message code="Encounter.form"/></th>
			<td>
				<spring:bind path="encounter.form">
					<c:choose>
						<c:when test="${encounter.encounterId == null}">
							<select name="form">
								<option value=""></option>
								<c:forEach items="${forms}" var="form">
									<option value="${form.formId}" <c:if test="${form.formId == status.value}">selected</c:if>>${form.name}</option>
								</c:forEach>
							</select>
						</c:when>
						<c:otherwise>
							${encounter.form.name} v${encounter.form.version}
						</c:otherwise>
					</c:choose>
					<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
				</spring:bind>
			</td>
		</tr>
		<c:if test="${encounter.encounterId != null}">
			<tr>
				<th><spring:message code="general.createdBy" /></th>
				<td>
					<a href="#View User" onclick="return gotoUser(null, '${encounter.creator.userId}')">${encounter.creator.personName}</a> -
					<openmrs:formatDate date="${encounter.dateCreated}" type="medium" />
				</td>
			</tr>
			<tr>
				<th><spring:message code="general.voided" /></th>
				<td>
					<spring:bind path="encounter.voided">
						<input type="hidden" name="_${status.expression}" />
						<input type="checkbox" name="${status.expression}" id="voided" onClick="voidedClicked(this)" <c:if test="${encounter.voided}">checked</c:if> />					
						<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
					</spring:bind>
				</td>
			</tr>
			<tr id="voidReason">
				<th><spring:message code="general.voidReason" /></th>
				<td>
					<spring:bind path="encounter.voidReason">
						<input type="text" value="${status.value}" name="${status.expression}" size="40" />
						<c:if test="${status.errorMessage != ''}"><span class="error">${status.errorMessage}</span></c:if>
					</spring:bind>
				</td>
			</tr>
			<c:if test="${encounter.voidedBy != null}">
				<tr id="voidedBy">
					<th><spring:message code="general.voidedBy" /></th>
					<td>
						<a href="#View User" onclick="return gotoUser(null, '${encounter.voidedBy.userId}')">${encounter.voidedBy.personName}</a> -
						<openmrs:formatDate date="${encounter.dateVoided}" type="medium" />
					</td>
				</tr>
			</c:if>
		</c:if>
	</table>
</div>

	<br/>
	<div class="boxHeader">
		<b><spring:message code="Provider.header"/></b>
	</div>
	<div class="box">
	<table cellspacing="0" cellpadding="2" width="98%" id="providers">
		<tr id="providersListingHeaderRow">
			<th><spring:message code="Role.role"/></th>
			<th><spring:message code="Provider.name"/></th>
			<th><spring:message code="Provider.identifier"/></th>
			<th></th>
		</tr>
		<c:forEach items="${encounter.providersByRoles}" var="providerRole">
			<c:forEach items="${providerRole.value}" var="provider">
				<tr>
					<td>${providerRole.key.name}
						<input type="hidden" name="encounterRoleIds" value="${providerRole.key.encounterRoleId}" />
					</td>
					<td>${provider}
						<input type="hidden" name="providerIds" value="${provider.providerId}" />
					</td>
					<td>${provider.identifier}</td>
					<td><input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onClick="removeProvider(this, ${providerRole.key.encounterRoleId}, ${provider.providerId})" /></td>
				</tr>
			</c:forEach>
		</c:forEach>
		<tr id="addNewProviderTemplate" style="display:none;">
			<td>
				<select id="roleIds[x]" name="encounterRoleIds">
					<option value=""></option>
					<c:forEach items="${encounterRoles}" var="encounterRole">
						<option value="${encounterRole.encounterRoleId}">
							${encounterRole.name}
						</option>
					</c:forEach>
				</select>
				<span id="roleErrors" class="error" style="display: none;">
					<spring:message code="Encounter.provider.selectEncounterRole"/>
				</span>
			</td>
			<td>
			<input id="providers[x]" type="text" name="providerName"  />
			<span id="providerErrors" class="error" style="display: none;">
				<spring:message code="Encounter.provider.selectProvider"/>
			</span>
			<input id="providerIds[x]" type="hidden" name="providerIds"  />
			</td>
			<td id="identifierColumn-[x]"></td>
			<td>
				<input type="button" value='<spring:message code="general.remove"/>' class="smallButton" onclick="removeProvider(this)" />
			</td>
		</tr>
	</table>
	<input type="button" id="addProviderButton" value='<spring:message code="Provider.add"/>' class="smallButton" onclick="addProvider()" />
	</div>
	<br/>
	<input type="hidden" name="phrase" value='<request:parameter name="phrase" />'/>
	<input type="submit" id="saveEncounterButton" value='<spring:message code="Encounter.save"/>' >
	&nbsp;
	<input type="button" value='<spring:message code="general.cancel"/>' onclick="history.go(-1); return; document.location='index.htm?autoJump=false&phrase=<request:parameter name="phrase"/>'">
	</form>
	
<c:if test="${encounter.encounterId != null}">
	<br/>
	<openmrs:extensionPoint pointId="org.openmrs.admin.encounters.encounterFormBeforeObs" type="html" parameters="encounterId=${encounter.encounterId}">
		<openmrs:hasPrivilege privilege="${extension.requiredPrivilege}">
			<div class="boxHeader" style="font-weight: bold;"><spring:message code="${extension.title}" /></div>
			<div class="box" style="padding: 0px 0px 5px;"><spring:message code="${extension.content}" />
  				<c:if test="${extension.portletUrl != null}">
   					<openmrs:portlet url="${extension.portletUrl}" moduleId="${extension.moduleId}" id="${extension.portletUrl}" encounterId="${encounter.encounterId}" parameters="allowEdits=true"/>
 				</c:if>
			</div>
			<br />
		</openmrs:hasPrivilege>
	</openmrs:extensionPoint>
	
	<br/>
	<div class="boxHeader">
		<span style="float: right">
			<a href="#" id="showDescription" onClick="return toggleVisibility(document, 'div', 'description')"><spring:message code="general.toggle.description"/></a> |
			<a href="#" id="showVoided" onClick="return toggleRowVisibilityForClass('obs', 'voided', true);"><spring:message code="general.toggle.voided"/></a>
		</span>
		<b><spring:message code="Encounter.observations"/></b>
	</div>
	<div class="box">
	<table cellspacing="0" cellpadding="2" width="98%" id="obs">
		<tr id="obsListingHeaderRow">
			<th class="fieldNumber"></th>
			<th class="obsConceptName"><spring:message code="Obs.concept"/></th>
			<th class="obsValue"><spring:message code="Obs.value"/></th>
			<th class="obsAlerts"></th>
			<th class="obsCreator"><spring:message code="Obs.creator.or.changedBy"/></th>
		</tr>
		<c:forEach items="${obsMap}" var="obsEntry" varStatus="status">
			<c:set var="obsList" value="${obsEntry.value}" scope="request"/>
		    <c:set var="field" value="${obsEntry.key}" scope="request"/>
		    <c:set var="level" value="0" scope="request"/>
			<c:import url="obsDisplay.jsp" />
		</c:forEach>
	</table>
	</div>
	
	<br />
	<div id="encounterFormAddObsMenu">
		<openmrs:hasPrivilege privilege="Add Observations">
			<div>
				<a href="${pageContext.request.contextPath}/admin/observations/obs.form?encounterId=${encounter.encounterId}">
					<spring:message code="Obs.add"/>
				</a>
			</div>
		</openmrs:hasPrivilege>
		<openmrs:extensionPoint pointId="org.openmrs.admin.encounters.encounterFormAddObsMenu" type="html" requiredClass="org.openmrs.module.web.extension.LinkProviderExtension">
			<c:forEach items="${extension.links}" var="link">
				<openmrs:hasPrivilege privilege="${link.requiredPrivilege}">
					<div>
						<a href="<c:url value="${link.url}" />"><spring:message code="${link.label}"/></a>
					</div>
				</openmrs:hasPrivilege>
			</c:forEach>
		</openmrs:extensionPoint>
	</div>
	<br />
	
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp" %>
