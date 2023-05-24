package com.astralbrands.orders.process;

import com.astralbrands.orders.constants.AppConstants;
import com.astralbrands.orders.dao.X3BPCustomerDao;
import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// *********************-FILE-NAMING-CONVENTION********************
//-----------------------qvc_com_tracking.xlsx---------------------

@Component
public class CommerceTrackingProcess implements BrandOrderForms, AppConstants {

	Logger log = LoggerFactory.getLogger(CommerceTrackingProcess.class);

	@Autowired
	X3BPCustomerDao x3BPCustomerDao;

	@Override
	public void process(Exchange exchange, String site, String[] fileName) {

		try {
			InputStream inputStream = exchange.getIn().getBody(InputStream.class);
			Workbook workbook = new XSSFWorkbook(inputStream);
			StringBuilder orderLines = new StringBuilder();
			Sheet firstSheet = workbook.getSheetAt(0);
			String response = "";
//			if(fileName[2].equalsIgnoreCase("LTRACK")) {
//				response = readSheetLys(firstSheet, orderLines);
//			} else {
//				response = readSheet(firstSheet, orderLines);
//			}
			response = readSheet(firstSheet, orderLines);
			System.out.println("Final response is : "+response);
			
			if (response != null) {
				exchange.setProperty(INPUT_FILE_NAME, "CommerceHubTrackingLYS.txt");
				exchange.getMessage().setBody(response);
				exchange.getMessage().setHeader(Exchange.FILE_NAME, "CommerceHubTrackingLYS.txt");
				exchange.setProperty(IS_DATA_PRESENT, true);
				exchange.setProperty(SITE_TWO, "TRACK");
				exchange.setProperty("IFILE", response);
				exchange.setProperty(CSV_DATA, response);
				exchange.setProperty(LOCAL_ORDER, LOCAL);
			} else {
				exchange.setProperty(IS_DATA_PRESENT, false);
			}
		} catch (Exception e) {
			System.out.println("Error is : " + e);
		}

	}

	private String readSheet(Sheet firstSheet, StringBuilder orderLines) {
		Boolean skipHeader = true;
		Pattern p = Pattern.compile(".*'([^']*)'.*");
		String orderNumber = null;
		Matcher orderMatchNum = null;
		String tmpOrderNum = null;

		for (Row row : firstSheet) {
			ArrayList<Cell> cells = new ArrayList<>();
			Iterator<Cell> cellIterator = row.cellIterator();
			cellIterator.forEachRemaining(cells::add);
			
			 // matcher for Order Number
			System.out.println("Size of cell is : "+cells.size());
			if (cells.size() > 4 && !getData(cells.get(2)).equalsIgnoreCase("N/A")) {
				orderNumber = getData(cells.get(1));
				orderMatchNum = p.matcher(orderNumber);
				if (skipHeader) {
					skipHeader = false;
				}
				else {
					if(orderMatchNum.matches() && !((orderMatchNum.group(1)).equalsIgnoreCase(tmpOrderNum))) {
						orderLines.append(getHeader(row));
						orderLines.append(NEW_LINE_STR);
						orderLines.append(getOrderLine(row));
						orderLines.append(NEW_LINE_STR);
						tmpOrderNum = orderMatchNum.group(1);
					}
					else if(!orderMatchNum.matches() && !(orderNumber.equalsIgnoreCase(tmpOrderNum))){
						orderLines.append(getHeader(row));
						orderLines.append(NEW_LINE_STR);
						orderLines.append(getOrderLine(row));
						orderLines.append(NEW_LINE_STR);
						tmpOrderNum = orderNumber;
					}
				
				}
			}
		}
		return orderLines.toString();

	}

	private String readSheetLys(Sheet firstSheet, StringBuilder orderLines) {
		Boolean skipHeader = true;
		String orderNumber = null;
		String tmpOrderNum = "";

		for (Row row : firstSheet) {
			ArrayList<Cell> cells = new ArrayList<>();
			Iterator<Cell> cellIterator = row.cellIterator();
			cellIterator.forEachRemaining(cells::add);

			// matcher for Order Number
			System.out.println("Size of cell is : "+cells.size());
			if (cells.size() > 4 && !getData(cells.get(2)).equalsIgnoreCase("N/A")) {
				orderNumber = getData(cells.get(1));
				if (skipHeader) {
					skipHeader = false;
				}
				else {
					if(tmpOrderNum.equalsIgnoreCase(getData(cells.get(1))) && orderNumber.equalsIgnoreCase(tmpOrderNum)) {
						orderLines.append(getHeaderLys(row));
						orderLines.append(NEW_LINE_STR);
						orderLines.append(getOrderLineLys(row));
						orderLines.append(NEW_LINE_STR);
						tmpOrderNum = getData(row.getCell(1));
					}
					else if(!tmpOrderNum.equals(getData(cells.get(1))) && !(orderNumber.equalsIgnoreCase(tmpOrderNum))){
						orderLines.append(getHeaderLys(row));
						orderLines.append(NEW_LINE_STR);
						orderLines.append(getOrderLineLys(row));
						orderLines.append(NEW_LINE_STR);
						tmpOrderNum = orderNumber;
					}

				}
			}
		}
		return orderLines.toString();

	}

	private String getOrderLineLys(Row row) {
		ArrayList<Cell> cells = new ArrayList<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cells::add);
//		Pattern p = Pattern.compile(".*'([^']*)'.*");
//		Matcher m1 = p.matcher(getData(cells.get(2)));

		StringJoiner lineBuilder = new StringJoiner(",");
		lineBuilder.add("S");
		lineBuilder.add("1");
		lineBuilder.add(getData(cells.get(2)));
		return lineBuilder.toString();
	}

	private String getOrderLine(Row row) {
		ArrayList<Cell> cells = new ArrayList<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cells::add);
		Pattern p = Pattern.compile(".*'([^']*)'.*");
		Matcher m1 = p.matcher(getData(cells.get(2)));

		StringJoiner lineBuilder = new StringJoiner(",");
		lineBuilder.add("S");
		lineBuilder.add("1");
		lineBuilder.add(m1.matches()?m1.group(1):getData(cells.get(2)));
		return lineBuilder.toString();
	}

	public String getData(Cell cell) {
		return new DataFormatter().formatCellValue(cell);
	}

	private String getHeaderLys(Row row) {

		ArrayList<Cell> cells = new ArrayList<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cells::add);
//		Pattern p = Pattern.compile(".*'([^']*)'.*");
//		Matcher trackingNum = p.matcher(getData(cells.get(2))); //matcher for Tracking number
//		Matcher orderNum = p.matcher(getData(cells.get(1))); // matcher for Order Number

		StringJoiner headerBuilder = new StringJoiner(",");
		headerBuilder.add("H");
		headerBuilder.add(x3BPCustomerDao.getDeliveryNumber(getData(cells.get(1))));
		String date = getDate(getData(cells.get(0)));
		headerBuilder.add(date); //Date
		headerBuilder.add(date); //date
		headerBuilder.add(getData(cells.get(2))); //Tracking number
		return headerBuilder.toString();
	}

	private String getHeader(Row row) {

		ArrayList<Cell> cells = new ArrayList<>();
		Iterator<Cell> cellIterator = row.cellIterator();
		cellIterator.forEachRemaining(cells::add);
		Pattern p = Pattern.compile(".*'([^']*)'.*");
		Matcher trackingNum = p.matcher(getData(cells.get(2))); //matcher for Tracking number
		Matcher orderNum = p.matcher(getData(cells.get(1))); // matcher for Order Number

		StringJoiner headerBuilder = new StringJoiner(",");
		headerBuilder.add("H");
		headerBuilder.add(x3BPCustomerDao.getDeliveryNumber(orderNum.matches()?orderNum.group(1):getData(cells.get(1))));
		String date = getDate(getData(cells.get(0)));
		headerBuilder.add(date); //Date
		headerBuilder.add(date); //date
		headerBuilder.add(trackingNum.matches()?trackingNum.group(1):getData(cells.get(2))); //Tracking number
		return headerBuilder.toString();
	}

	private String getDate(String date) {
		System.out.println("input Date is : " + date);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");
		LocalDate ld = LocalDate.parse(date, formatter);
		System.out.println("output Date is : " + ld.getMonthValue() + " " + ld.getDayOfMonth() + " " + ld.getYear());
		return ld.getYear() + "" + (ld.getMonthValue() < 10 ? ("0" + ld.getMonthValue()) : ld.getMonthValue()) + ""
				+ (ld.getDayOfMonth() < 10 ? ("0" + ld.getDayOfMonth()) : ld.getDayOfMonth());
	}
}
