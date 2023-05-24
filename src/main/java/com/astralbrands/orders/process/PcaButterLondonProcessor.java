package com.astralbrands.orders.process;

import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.astralbrands.orders.process.BrandOrderForms;
import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.constants.AppConstants;
import com.astralbrands.orders.dao.X3BPCustomerDao;


@Component
public class PcaButterLondonProcessor implements AppConstants,  BrandOrderForms {

        static Map<String, Integer> colName = new HashMap<>();
        static {
                colName.put(PCA_STATUS, 0);
                colName.put(CATEGORY, 1);
                colName.put(Vendor_ID, 2);
                colName.put(UPC, 3);
                colName.put(PRODUCT, 4);
                colName.put(SHADE_NAME, 5);
                colName.put(FILL_WEIGHT, 6);
                colName.put(US_SRP, 7);
                colName.put(INT_SRP, 8);
                colName.put(DISTRIBUTED_DISCOUNT_PERCENTAGE, 9);
                colName.put(PCA_DIST_COST, 10);
                colName.put(PCA_QTY, 11);
                colName.put(EXTENDED_PRICE, 12);
        }

        // QUERY TO GET "ITMREF_0" FOR PRODUCT LINES
        @Value("${db.query}")
        static String query;
        // QUERY TO GET BASE PRICE FOR EACH PRODUCT'S "ITMREF_0"
        @Value("${db.query1}")
        static String queryPrice;

        @Autowired
        JdbcTemplate jdbcTemplate;

        //Constant Brand Numbers
        static String siteID = "";
        static String siteName;
        static String sellerID;
        static String id = "460000271";
        static String orderType = "BBLK";
        static String site = "BUTCO";
        static int index = 0;

        @Override
        public void process(Exchange exchange, String site, String[] fileNameData) {
                try {
                        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
                        Workbook workbook = new XSSFWorkbook(inputStream); // Object to hold multiple sheets for processing
                        StringBuilder txtFileData = new StringBuilder();
                        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                        Sheet firstSheet = workbook.getSheetAt(0); // Gets the first sheet from the Workbook Object
                        populateTxtString(firstSheet,txtFileData, evaluator); // Processes the sheet and formats the data for the new TXT & CSV files
                        String today = currentDate(); // Returns the current date when this program runs
                        System.out.println("Output data is : "+ txtFileData);
                        index++;

                        if (txtFileData != null) { // Ensures the current sheet is valid/contains data
                                exchange.setProperty(CSV_DATA, txtFileData.toString().replace(TILDE, COMMA)); //Removed '.txt' for the '.csv' output file name
                                exchange.getMessage().setBody(txtFileData); // Sets the exchange value as the new formatted data in this Processor class
                                exchange.setProperty("IFILE", txtFileData);
                                exchange.getMessage().setHeader(Exchange.FILE_NAME,  today + "_PCA" + index + DOT_TXT); //Formats '.txt' file with today's date
                                exchange.setProperty(SITE_NAME, site);
                                exchange.setProperty(BRAND, "PCA-butterLondon");
                                exchange.setProperty(IS_DATA_PRESENT, true);
                        } else {
                                exchange.setProperty(IS_DATA_PRESENT, false);
                        }
                }
                catch (Exception e) {
                        e.printStackTrace();
                }

        }

//        private void getFormType(Cell cell) {
//                String cellValue = getValue(cell);
//
//                if(cellValue.equalsIgnoreCase("PCNGS")) {
//                        site = "PUR";
//                        siteID = "19999991";
//                        siteName = "PURCO";
//                        sellerID = "PNYK";
//                } else if(cellValue.equalsIgnoreCase("CLNGS")) {
//                        site = "COS";
//                        siteID = "29999991";
//                        siteName = "COSCO";
//                        sellerID = "CNYK";
//                } else if(cellValue.equalsIgnoreCase("BNYK")) {
//                        site = "BUT";
//                        siteID = "69999991";
//                        siteName = "BUTCO";
//                        sellerID = "BNYK";
//                }
//        }

//    public String formatDate(String date) {
//        String datePart = date.substring(0, 10);
//        String rt = getDate(datePart);
//        return rt;
//    }


        /*------------------------------------------------------------------------------------
            Function to build the structure for the files || Calls two local functions
            to populate the StringBuilder with the sheet's cell values in a specific format
            for X3.
        *******--Builds/Populates a String formatted with all header lines & product info lines from the sheet--*******
         ------------------------------------------------------------------------------------------------------------*/
        private String populateTxtString(Sheet firstSheet, StringBuilder txtFileBuilder, FormulaEvaluator evaluator) {

                boolean skipHeader = true;

                String tmpPO = EMPTY_STR; // Order #
                System.out.println("String builder is : " + txtFileBuilder.toString());
                System.out.println("Number of cells are : " + firstSheet);
                DataFormatter df = new DataFormatter();
                Integer scale = 13;
                Integer precision = 27;
//        BigDecimal bd = new BigDecimal(scale, precision);
                List<String> productSKUs = new ArrayList<>();
                txtFileBuilder.append(getHeader(true) + "\n");
//                txtFileBuilder.append(getHeader(true));
                for (Row row : firstSheet) {
                        ArrayList<Cell> cells = new ArrayList<>();
                        Iterator<Cell> cellIterator = row.cellIterator();
                        cellIterator.forEachRemaining(cells::add);
                        String qty = "QTY";
//                        Cell poNum = row.getCell(colName.get(ORDER_NO));
                        //cells.size();
                        System.out.println(cells.size());
                        if (row.getCell(colName.get(PCA_QTY)) == null) {
                                continue;
                        }
                        if (row.getCell(colName.get(PCA_QTY)).toString().trim().equalsIgnoreCase(qty)) {// Starts after the row with column names
                                continue;
                        }
                        // If a product has the same Order# then add product info under same customer line
//                                else if(tmpPO.equals(getData(poNum))){
                        System.out.println("tmpPo number is : " + tmpPO);
                        // Builds the formatted data from the Excel sheet in a String
                        System.out.println("row value is " + row.getCell(0));
                        //System.out.println("String builder is : "+ txtFileBuilder.toString()); //Duplicate line
                        if (row.getCell(colName.get(PCA_QTY)) != null && row.getCell(colName.get(PCA_QTY)).toString().length() > 0) {
                                if (row.getCell(colName.get(PCA_QTY)).getNumericCellValue() > 0) {
                                        txtFileBuilder.append(getOrderLine(row, evaluator));
                                        txtFileBuilder.append(NEW_LINE_STR);
                                }
                        }
//                                else {
//                                        continue;
//                                }
//                                if (row.getCell(colName.get(PCA_QTY)) != null || row.getCell(colName.get(PCA_QTY)).toString().length() > 0) {
//                                        txtFileBuilder.append(getOrderLine(row, evaluator)); // Builds the corresponding product info line
//                                } else {
//                                        continue;
//                                }
                        System.out.println("String builder is : " + txtFileBuilder.toString());

//                                        tmpPO = getData(poNum); // Order #
                        System.out.println("tmpPo number is : " + tmpPO);
                        if (getData(row.getCell(colName.get(PCA_QTY) - 1)).trim().equalsIgnoreCase("TOTAL")) {

                        }
                }
                System.out.println("Text file is : " + txtFileBuilder.toString());
                return txtFileBuilder.toString();
        }
//                }


        /*---------------------------------------------------------------------------------------------------------------
            Iterates through cells pertaining to Product information in the given row and adds the values to an ArrayList
            Returns a formatted String separated by '~' after each value obtained from each cell
            in the current row of the Excel Sheet
            ----------Builds the product info line in the TXT file-----------
         --------------------------------------------------------------------------------------------------------------*/
        private String getOrderLine(Row row, FormulaEvaluator evaluator) {

                // Cell Objects to hold Order Form Data
                Cell vendorID = row.getCell(colName.get(Vendor_ID));
                Cell UPCcell = row.getCell(colName.get(UPC));
                Cell productDecsription = row.getCell(colName.get(PRODUCT));
                Cell distCost = row.getCell(colName.get(PCA_DIST_COST));
                Cell pcaQTY = row.getCell(colName.get(PCA_QTY));
                Cell extPrice = row.getCell(colName.get(EXTENDED_PRICE));
//                Cell qty = row.getCell(colName.get(PCA_QTY));
                String qty = getValue(pcaQTY, evaluator);
                qty = qty.substring(0, qty.indexOf("."));
                String price = getValue(distCost, evaluator);
                price = price.substring(0, price.indexOf("."));


//                String pcaUPC = getValue(UPCcell);
//                String pcaID = getValue(vendorID);
////                String description = getValue(productDecsription);
//                String pcaQty = getValue(pcaQTY);
//                String distributerPrice = getValue(distPrice);


                StringJoiner lineBuilder = new StringJoiner("~");
                System.out.println("Line builder is : "+ lineBuilder.toString() + "\n");
                lineBuilder.add("L");
                lineBuilder.add(getData(vendorID)); //SKU
                lineBuilder.add(getData(productDecsription)); //Description
                lineBuilder.add(site);
                lineBuilder.add("EA"); //Sales Unit
                lineBuilder.add(qty); //Quantity
                lineBuilder.add(price); //Base Price
                lineBuilder.add(EMPTY_STR);
                lineBuilder.add(EMPTY_STR);
                lineBuilder.add(EMPTY_STR);
                lineBuilder.add(EMPTY_STR);
                lineBuilder.add(EMPTY_STR);
                System.out.println(lineBuilder.toString());
                return lineBuilder.toString();
        }

        // Formats a cell's data value within a row/column of an Excel sheet
        public String getData(Cell cell) {
                return new DataFormatter().formatCellValue(cell).toString();
        }

//        private String getValue(Cell cell, FormulaEvaluator evaluator) {
//                Object value = null;
//                if (cell != null) {
//                        switch (cell.getCellType()) {
//                                case Cell.CELL_TYPE_STRING:
//                                        value = cell.getStringCellValue();
//                                        break;
//                                case Cell.CELL_TYPE_NUMERIC:
//                                        value = cell.getNumericCellValue();
//                                        break;
//                                case Cell.CELL_TYPE_FORMULA:
//                                        value = evaluator.evaluate(cell);
//                                        break;
//                                default:
//                                        break;
//                        }
//                }
//                return value.toString();
//        }

        private String getValue(Cell cell, FormulaEvaluator evaluator) {
                Object value = new Object();
                switch (cell.getCellType()) {
                        case Cell.CELL_TYPE_STRING:
                                value = cell.getStringCellValue();
                                break;
                        case Cell.CELL_TYPE_NUMERIC:
                                value = cell.getNumericCellValue();
                                break;
                        case Cell.CELL_TYPE_FORMULA:
                                CellValue cellValue = evaluator.evaluate(cell);
                                if (cellValue != null) {
                                        double val = cellValue.getNumberValue();
                                        value = Math.round(val * 100.0) / 100.0;
                                }
                                break;
                        default:
                                break;
                }
                return value.toString();
        }

        // Converts a Cell object value to a String
//        private String getValue(Cell cell) {
//                if (cell != null) {
//                        System.out.println("Value is : "+cell.toString());
//                }
//                String value = getData(cell);
////		System.out.println("Value is : "+value);
//                if(value.toString().equalsIgnoreCase("N/A")) {
//                        return EMPTY_STR;
//                }
//                return value.toString();
//        }


        /*--------------------------------------------------------------------------------
            Obtains the customer's information along with the order number, Shipping site,
            Sales Site, Order Type, Customer order reference, Date, and currency.
            Formatted in a specific order for X3
            ----------Builds each header line in the TXT file-----------
         -------------------------------------------------------------------------------*/
        private String getHeader(boolean row) {

//                ArrayList<Cell> cells = new ArrayList<>();
//                Iterator<Cell> cellIterator = row.cellIterator();
//                cellIterator.forEachRemaining(cells::add);

                //---------CELL OBJECTS TO HOLD A CELL'S VALUE FOR THE POSITION OF COLUMN NAMES---------


                StringJoiner headerBuilder = new StringJoiner("~");
                System.out.println("headerBuilder is : "+ headerBuilder.toString());
                //System.out.println(Header is : "+row);
                headerBuilder.add("E");
                headerBuilder.add(site); //Sales Site/BUTCO/PURCO
                headerBuilder.add(orderType); //Order Type/BQVCD/PQVCD
                headerBuilder.add(EMPTY_STR);
                headerBuilder.add(id);
                headerBuilder.add(currentDate()); //Order Site ID/BUTTER - '460000147'/PUR - '410000042'
                headerBuilder.add(currentDate()); //Date
                headerBuilder.add(site); // Sales Site
                headerBuilder.add("USD"); //Currency
                for(int i=0; i<26; i++) {
                        headerBuilder.add(EMPTY_STR);
                }
                headerBuilder.add("NET30WTR");
                System.out.println(headerBuilder.toString());
                return headerBuilder.toString();
        }

        private String getDate(String date) {
                System.out.println("Date is : "+date);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate ld = LocalDate.parse(date,formatter);
                //System.out.println("Date is : "+ld.getMonthValue()+" "+ld.getDayOfMonth()+" "+ld.getYear());
                return ld.getYear()+""+(ld.getMonthValue()<10?("0"+ld.getMonthValue()):ld.getMonthValue())+""+(ld.getDayOfMonth()<10?("0"+ld.getDayOfMonth()):ld.getDayOfMonth());
        }

        private String currentDate() {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                LocalDate date = LocalDate.now();
//		LocalDate date = LocalDate.parse(today, formatter);
                String td = date.format(formatter);
                return td;
        }
}
