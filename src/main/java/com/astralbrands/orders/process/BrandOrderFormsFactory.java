package com.astralbrands.orders.process;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
	This class provides a mechanism for the Router
	to choose the correct Processor for the file being uploaded
	Single function that compares the site(last substring) in
	the file's name and returns the proper Processor Object
*/
@Component
public class BrandOrderFormsFactory {

	
	@Autowired
	private CosmedixOrderProcessor cosmedixOrderProcessor;


	@Autowired
	private PurProcessor purProcessor;

	@Autowired
	private AloutteOrderProcessor aloutteOrderProcessor;

	@Autowired
	private CommerceTrackingProcess commerceTrackingProcess;

	@Autowired
	private CommerceHubProcessor commerceHubProcessor;
	@Autowired
	private NYKKAProcessor nykkaProcessor;

	@Autowired
	private BoxesExportProcess boxesExportProcess;

	@Autowired
	private PurFormProcessor purFormProcessor;

	@Autowired
	private CosFormProcessor cosFormProcessor;

	@Autowired
	private ButterFormProcessor butterFormProcessor;

	@Autowired
	private LysBoxProcessor lysBoxProcessor;

	@Autowired
	private PcaButterLondonProcessor pcaButterLondonProcessor;



//	@Autowired
//	private BrandOrdersExportProcessor brands;

//	@Autowired
//	private BoxesExportProcess sohProcessor;
	

	/*
		Checks the First String before the First '_', UNDERSCORE, in the filename to
		determine the correct company processor to use for the input Order Form file
	----------------Directs the exchange data, Input File, to the correct Order Form Processor-----------------
	 */
	public BrandOrderForms getBrandOrderForms(String site) {
		if ("COS".equals(site)) {
			return cosFormProcessor;
		}
		else if("PUR".equalsIgnoreCase(site)){
			return purFormProcessor;
		}
		else if("NYKKA".equalsIgnoreCase(site)) {
			return nykkaProcessor;
		}
		else if("BOX".equalsIgnoreCase(site)) {
			return boxesExportProcess;
		}
		else if("BUT".equalsIgnoreCase(site)){
			return butterFormProcessor;
		}
		else if("LYS".equalsIgnoreCase(site)) {
			return lysBoxProcessor;
		} else if ("PCA".equalsIgnoreCase(site)) {
			return pcaButterLondonProcessor;
		} else {
			return cosmedixOrderProcessor;
		}

	}

	public BrandOrderForms getLocalBrandOrderForms(String siteTwo) {
		if ("COS".equalsIgnoreCase(siteTwo)) {
			return cosmedixOrderProcessor;
		}
		else if("HUB".equalsIgnoreCase(siteTwo)) {
			return commerceHubProcessor;
		}
		else if("PUR".equalsIgnoreCase(siteTwo)) {
			return purProcessor;
		}
		else if(("US".equalsIgnoreCase(siteTwo) || "CA".equalsIgnoreCase(siteTwo))) {
			return aloutteOrderProcessor;
		}
		else if("TRACK".equalsIgnoreCase(siteTwo) || "LTRACK".equalsIgnoreCase(siteTwo)) {
			return commerceTrackingProcess;
		}
		else {
			return aloutteOrderProcessor;
		}
	}
}
