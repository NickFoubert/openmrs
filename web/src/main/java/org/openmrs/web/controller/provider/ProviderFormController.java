package org.openmrs.web.controller.provider;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Provider;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.propertyeditor.PersonEditor;
import org.openmrs.validator.ProviderValidator;
import org.openmrs.web.WebConstants;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/provider/provider.form")
public class ProviderFormController {
	
	protected final Log log = LogFactory.getLog(getClass());
	
	@InitBinder
	protected void initBinder(WebDataBinder binder) throws Exception {
		binder.registerCustomEditor(org.openmrs.Person.class, new PersonEditor());
	}
	
	@RequestMapping(method = RequestMethod.POST)
	public String onSubmit(HttpSession session, @RequestParam(required = false) String saveProviderButton,
	                       @RequestParam(required = false) String retireProviderButton,
	                       @RequestParam(required = false) String unretireProviderButton,
	                       @ModelAttribute("provider") Provider provider, BindingResult errors) throws Exception {
		new ProviderValidator().validate(provider, errors);
		
		if (!errors.hasErrors()) {
			if (Context.isAuthenticated()) {
				ProviderService service = Context.getProviderService();
				
				String message = "Provider.saved";
				if (saveProviderButton != null) {
					service.saveProvider(provider);
				} else if (retireProviderButton != null) {
					service.retireProvider(provider, provider.getRetireReason());
					message = "Provider.retired";
				} else if (unretireProviderButton != null) {
					service.unretireProvider(provider);
					message = "Provider.unretired";
				}
				
				session.setAttribute(WebConstants.OPENMRS_MSG_ATTR, message);
				return showList();
			}
		}
		
		return showForm();
	}
	
	@ModelAttribute("provider")
	public Provider formBackingObject(@RequestParam(required = false) String providerId) throws ServletException {
		Provider provider = new Provider();
		if (Context.isAuthenticated()) {
			if (providerId != null) {
				ProviderService ps = Context.getProviderService();
				return ps.getProvider(Integer.valueOf(providerId));
			}
		}
		return provider;
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public String showForm() {
		return "admin/provider/providerForm";
	}
	
	public String showList() {
		return "redirect:index.htm";
	}
}
