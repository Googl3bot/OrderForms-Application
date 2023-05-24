package com.astralbrands.orders.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.constants.AppConstants;
/*
	This Processor class reads the Excel file name
	If file name is not properly formatted exception thrown
	Based on the site(last substring before the ".extension")
	it will use the correct Processor(company) class to continue
 */
@Component
public class ReadXslxFileProcessor implements Processor, AppConstants {

	/*
		Object that is used to determine the correct Processor
		for the current file
	 */
	@Autowired
	private BrandOrderFormsFactory orderFormsFactory;

	private boolean localBrandFlag;

	Logger log = LoggerFactory.getLogger(ReadXslxFileProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		
		System.out.println("Exchange value is "+exchange);

		String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
		if (fileName == null) {
			exchange.setProperty(IS_DATA_PRESENT, false);
			return;
		}
		log.info("input file name: " + fileName);
		String[] fileNameData = fileName.split(UNDERSCORE); // File name must have two underscores separating 3 strings
		if (fileNameData.length < 3) {
			log.error("Invalid file name :" + fileName + ", File name should be customerId_date_site.xls");
			throw new RuntimeException("Invalid File name");
		}
		exchange.setProperty(INPUT_FILE_NAME, removeExtention(fileName));
		System.out.println("Exchange name is " + exchange);

		String NfileName = removeExtention(fileName);
		String[] currentFileNameData = NfileName.split(UNDERSCORE);
		exchange.setProperty(FILE_NAME_ARRAY, currentFileNameData);
		for(int i=0; i<fileNameData.length;i++) {
			System.out.println("File data is : " + currentFileNameData[i]);
		}

		String ext = fileName.substring(fileName.indexOf(DOT) + 1, fileName.lastIndexOf("x") + 1);
		System.out.println("File extension is : " + ext);
		String site = currentFileNameData[0];
		String siteTwo = currentFileNameData[2];

		site = site.toUpperCase();
		siteTwo = siteTwo.toUpperCase();
		String custNumberTwo = currentFileNameData[0];
		String custNumber = currentFileNameData[2];

		System.out.println("Customer Number is : " + custNumber);
		System.out.println("Site name is: " + site);
//		if(site.equalsIgnoreCase("NYKKA")) {
//			exchange.setProperty(NYKKA, "NYKKA");
//		}
//		if(site.equalsIgnoreCase("BOX")) {
//			exchange.setProperty(BOXES, "BOXES");
//			exchange.setProperty(ORDER_DATE, formDate);
//		}
		// FOR CHECKING IF ORDER FORM IS NOT INTERNATIONAL
		if((siteTwo.equalsIgnoreCase("PUR") || siteTwo.equalsIgnoreCase("BUT") || siteTwo.equalsIgnoreCase("COS")
				|| siteTwo.equalsIgnoreCase("TRACK")) || siteTwo.equalsIgnoreCase("LTRACK") || siteTwo.equalsIgnoreCase("US")
				|| siteTwo.equalsIgnoreCase("CA") || siteTwo.equalsIgnoreCase("LYS")) {

			exchange.setProperty(LOCAL_ORDER, "LOCAL");
			exchange.setProperty(CUST_NUM, custNumberTwo);
			exchange.setProperty(SITE_TWO, siteTwo);
			BrandOrderForms orderFormProcessor = orderFormsFactory.getLocalBrandOrderForms(siteTwo); // This is where the program chooses the correct Processor class
			orderFormProcessor.process(exchange, siteTwo, currentFileNameData); // Executes the correct Processor FOR LOCAL ORDER FORMS
		} else {
			exchange.setProperty(BRAND, "BRAND");
			exchange.setProperty(CUST_NUMBER, custNumber);
			exchange.setProperty(SITE_NAME, site);
			BrandOrderForms orderFormProcessor = orderFormsFactory.getBrandOrderForms(site); // This is where the program chooses the correct Processor class
			orderFormProcessor.process(exchange, site, currentFileNameData); // Executes the correct Processor FOR INTERNATIONAL ORDER FORMS
		}
	}

//	public String getFormType()


	// Removes the extension from the file's name and returns everything before the '.'
	public String removeExtention(String fileName) {
		return fileName.substring(0, fileName.indexOf(DOT));
	}

}
