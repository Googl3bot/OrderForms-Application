package com.astralbrands.orders.process;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.constants.AppConstants;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/*
	Processor to take the current exchange data
	and format it into a '.csv' file format
 */

@Component
public class CsvFileProcessor implements Processor, AppConstants {

	@Override
	public void process(Exchange exchange) throws Exception {
		String today = extension();
		String csvFileData = exchange.getProperty(CSV_DATA, String.class);
		exchange.getMessage().setBody(csvFileData);
		exchange.getMessage().setHeader(Exchange.FILE_NAME, today + exchange.getProperty(INPUT_FILE_NAME) + DOT_CSV);
	}

	// Function to add current month + day to the front of the new '.csv' file
	// Corrects the problem of having duplicate file names in the output directory for CSV files
	private String extension() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMdd"); // Formats the date in the given format
		LocalDate date = LocalDate.now(); // Gets the current date when the program runs
//		LocalDate date = LocalDate.parse(today, formatter);
		String sd = date.format(formatter) + "_";
		return sd;
	}

}
