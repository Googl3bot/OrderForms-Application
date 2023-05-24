package com.astralbrands.orders.router;

import org.apache.camel.ShutdownRunningTask;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.engine.DurationRoutePolicyFactory;
import org.apache.camel.spi.RoutePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.process.CsvFileProcessor;
import com.astralbrands.orders.process.EmailProcessor;
import com.astralbrands.orders.process.ReadXslxFileProcessor;

/*
	----------File Path is defined in "application.properties"------------
	Router class to configure endpoints/processes
	Takes an input file from the specified path "file:{{direct-deposit.input.file-path}}"
	Applies the ReadXslxFile processor, if "isDataPresent" is true,  to determine the site and direct
	it to the correct processor. A new processor takes the file and formats the name/header/data
	into a text file. Then the data is routed to the CsvFileProcessor to create a csv file for the processed data.
	All data is routed to the export file path, "file:{{direct-deposit.output.file-path}}", where two files are
	created on the local machine. The EmailProcessor is the last process that sends an email upon successful completion
 */
@Component
public class AppRouter extends RouteBuilder {

	Logger log = LoggerFactory.getLogger(AppRouter.class);

	@Autowired
	private ReadXslxFileProcessor readXslxFileProcessor;

	@Autowired
	private EmailProcessor emailProcessor;

	@Autowired
	private CsvFileProcessor csvFileProcessor;

	@Value("${cron.expression}")
	String cronScheduler;
	@Value("${ynsoht}")
	private Object ynsoht;

	@Override
	public void configure() throws Exception {
		log.info("route configuration started ");
		// Scheduler defined on Line 51 - value assigned in app.properties file
		onException(Exception.class).log("Exception while processing order detail, please check log for more details")
				.end();
		//scheduler=spring&scheduler.cron={{cron.expression}}&
		from("file:{{direct-deposit.input.file-path}}?delete=false&delay={{input.process.delay}}&moveFailed={{direct-deposit.error.file.path}}")
				.id("inputRouter").log("Read EXCEL file ${exchange}").process(readXslxFileProcessor).choice().when()
				.exchangeProperty("isDataPresent").choice().when().exchangeProperty("SITE_TWO").to("direct:exportFile").to("direct:sendNotification").otherwise()
				.when().exchangeProperty("BRAND").to("direct:exportBrands").to("direct:sendNotification").choice().when().exchangeProperty("NYKKA").to("direct:exportBrands")
				.to("direct:sendNotification").otherwise().choice().when().exchangeProperty("BOXES").to("direct:exportBoxes").to("direct:sendNotification")
				.otherwise().log("file is already processed or empty").endChoice().end();
																	//----------.to("direct:preparedCsvFile")--.to("direct:preparedCsvFile")--.to("direct:preparedCsvFile")--.to("direct:preparedCsvFile")
//		from("direct:site").id("direct:choice").choice().when(exchangeProperty("SITE_NAME").isEqualTo(ynsoht)).process(csvFileProcessor)
//						.to("file:{{direct-deposit.output.ynsoht}}").otherwise().to("direct:preparedCsvFile")
//						.to("direct:exportFile").endChoice().end();
//		from("direct:preparedCsvFile").id("direct:preparedCsvFile").log("preparedCsvFile").process(csvFileProcessor).choice().when().exchangeProperty("SITE_TWO").to("direct:exportFile").to("direct:sendNotification")
//				.otherwise().when().exchangeProperty("BRAND").to("direct:exportBrands").to("direct:sendNotification").otherwise()
//				.when().exchangeProperty("NYKKA").to("direct:exportNykka").to("direct:sendNotification").otherwise().when().exchangeProperty("BOXES").to("direct:exportBoxes").to("direct:sendNotification")
//				.endChoice().end();//.process(csvFileProcessor).choice().when()
//				.exchangeProperty("SITE").to("direct:exportYnsoht").otherwise().when().exchangeProperty("BOXES").to("direct:exportBoxes").otherwise().when()
//				.exchangeProperty("BRAND").to("direct:exportBrands").otherwise().to("direct:exportFile").endChoice().end();
		from("direct:exportBoxes").id("exportBoxes").log("--------------------------40 BOXES ORDER------------------------").to("file:{{direct-deposit.output.boxes.file-path}}").to("direct:sendNotification").end();
		from("direct:exportYnsoht").id("exportYnsoht").to("file:{{direct-deposit.output.ynsoht.file-path}}").to("direct:sendNotification").end();
		from("direct:exportBrands").id("exportBrands").log("------------------BRAND ORDER-------------------").to("file:{{direct-deposit.output.brands.file-path}}").to("direct:sendNotification").end();
		from("direct:exportFile").id("exportFile").log("----------------------------LOCAL ORDER--------------------").to("file:{{direct-deposit.output.file-path}}").to("direct:sendNotification").end();
		from("direct:exportNykka").id("NYKKA-FILE").log("---------------------NYKKA ORDER---------------------").to("file:{{direct-deposit.output.nykka.file-path}}").to("direct:sendNotification").end();
		from("direct:sendNotification").id("sendNotification").log("send Notification").process(emailProcessor)
				.log("email notification sent").stop().end(); //.to("direct:preparedCsvFile")
	}

}
