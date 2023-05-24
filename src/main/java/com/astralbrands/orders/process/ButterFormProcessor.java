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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class ButterFormProcessor implements BrandOrderForms, AppConstants {
    static Map<Integer, Map<String, Integer>> butterFormIndex = new HashMap<>();
    static {
        // PUR Form Mappings

        Map<String, Integer> butterForm = new HashMap<>();

        butterForm.put(B_VENDOR_ID, 3); // Item # column
        butterForm.put(B_UPC, 4); // Product name column
        butterForm.put(B_PRODUCT_DESCR, 5); // Quantity of Units Ordered column
        butterForm.put(B_SHADE_NAME, 6); // Shade
        butterForm.put(B_WEIGHT, 10); // Distributor Price column
        butterForm.put(B_DIST_COST, 14); // Distributor Price column
        butterForm.put(B_QTY, 15); // Quantity of Units Ordered column

        butterForm.put(FORM_BRAND, 4);

        Map<String, Integer> butterFormTester = new HashMap<>();

        butterFormTester.put(B_TEST_SKU, 20); // Quantity of Units Ordered column
        butterFormTester.put(B_TEST_UPC, 21); // Distributor Price column
        butterFormTester.put(B_TEST_COST, 23);
        butterFormTester.put(B_TEST_QTY, 25);
        butterFormTester.put(B_TEST_PRICE, 26);

        butterFormIndex.put(0, butterForm);
        butterFormIndex.put(1, butterFormTester);
    }
    private String startPoint = "";
    private String site = "";
    private String sellerID = "";
    private String siteID = "";
    private String customerRefNumber = "";
    private String date = "";
    private String shipVia = "";
    private String formBrand = "";
    private String cust_numType = "";
    //
    private String poType = "";

    @Autowired
    X3BPCustomerDao x3Dao;

    Logger log = LoggerFactory.getLogger(PurFormProcessor.class);

    @Override
    public void process(Exchange exchange, String brand, String[] fileName) {
        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        try {
            Workbook workbook = new XSSFWorkbook(inputStream); // Object to hold multiple Excel sheets
            int numOfSheet = workbook.getNumberOfSheets();

            String headerStr = "";
            StringBuilder prodEntry = new StringBuilder();
            StringBuilder sb = new StringBuilder();
            StringBuilder ab = new StringBuilder();
            log.info("Number of sheets we are processing :" + numOfSheet);
            // Loop to process multiple sheets
            for (int i = 0; i < numOfSheet - 1; i++) {
                Sheet firstSheet = workbook.getSheetAt(i);
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                readSheet(firstSheet, prodEntry, evaluator, i); // Populates prodEntry with product info
                log.info(i + " sheet name: " + firstSheet);
                if(i == 0) {
                    getTestItems(firstSheet, prodEntry, evaluator);
//                    getCoreKit(firstSheet, ab, evaluator);
                }
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            String exchPropValue = fileName[0];
            String header = populateHeader(fileName);
            log.info("data entry : " + prodEntry.toString());
            String date = currentDate();
            String data = header + NEW_LINE_STR + prodEntry.toString(); //+ sb.toString() + ab.toString(); // Formats a String with both Header line and product info lines
            if (data.length() > 0) { // Ensures the file isn't empty or invalid
                exchange.getMessage().setBody(data);
                exchange.setProperty(CSV_DATA, data.replace(TILDE, COMMA)); // CSV file data for later processing
                exchange.setProperty("IFILE", data);
                exchange.getMessage().setHeader(Exchange.FILE_NAME, "BUT_" + date + "_" + exchange.getProperty(CUST_NUMBER) + DOT_TXT); // txt file
                exchange.setProperty(IS_DATA_PRESENT, true);
//                exchange.setProperty(SITE_NAME, brand);
                exchange.setProperty(BRAND, exchPropValue);
                exchange.setProperty(SITE_NAME, "BUTTER");
            } else {
                exchange.setProperty(IS_DATA_PRESENT, false);
            }
        } catch (IOException e) {
            exchange.setProperty(IS_DATA_PRESENT, false);
        }
    }

    private void readSheet(Sheet firstSheet, StringBuilder dataEntry, FormulaEvaluator evaluator, int sheetIndex) {
        boolean entryStart = false;
        Map<String, Integer> indexMap = new HashMap<>(butterFormIndex.get(0));
        Optional<Integer> maxValue = indexMap.entrySet().stream().map(entry -> entry.getValue())
                .max(Comparator.comparingInt(Integer::valueOf));
        log.info("sheet index :" + sheetIndex + " and max index :" + maxValue);

        int index = 0;
        StringJoiner entry;
        for (Row row : firstSheet) {
            entry = new StringJoiner(TILDE);
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);
            Cell firstCol = row.getCell(indexMap.get(B_VENDOR_ID)); // Gets the cell's position
            String firstHeader = getData(firstCol);// Converts the value to a String  - Name of the first column
            if (sheetIndex == 0) {
                formBrand = getData(row.getCell(3));
                if("ASTRAL Customer Number:".equals(formBrand)) {
                    cust_numType = getData(row.getCell(4));
                }
                if("PO #:".equals(formBrand)) {
                    poType = getData(row.getCell(4));
                }
            }
            startPoint = "Vendor ID";
            index++;
            // Iterates through each row in the sheet until it reaches the column names
            // SKIPS unnecessary Lines
            if (row.getCell(3) == null) {
                continue;
            }
            if (getData(row.getCell(15)) == null || getNumeric(getData(row.getCell(15))) <= 0) {
                continue;
            }
            // Cell objects to hold value for the position of a column
            Cell vendorID = row.getCell(indexMap.get(B_VENDOR_ID));// Cell value for US Item Number
            Cell boUpc = row.getCell(indexMap.get(B_UPC));
            Cell descr = row.getCell(indexMap.get(B_PRODUCT_DESCR));
//          Cell descr1 = row.getCell(indexMap.get(C_F_S));
            Cell qtcol = row.getCell(indexMap.get(B_QTY)); // Cell value for the Quantity column
            Cell distCost = row.getCell(indexMap.get(B_DIST_COST));
//          Cell priceCol = row.getCell(indexMap.get(BO_PRICE));// Cell value for the product price column

//			log.info(index + " index " + getData(firstCol) + ", prodcol :" + getData(prodCol)
//			+ ", qtCol :" + getData(qtcol) + ", price col :" + getData(priceCol));
            String quantity = getData(qtcol); // Gets the value for the Quantity column
            // Builds/Adds the product info lines into the ifile - Only if the Quantity column is greater than 0
            if (quantity.trim().length() > 0 && getNumeric(quantity) > 0) { //--quantity != null &&

                entry.add(CHAR_L);
                entry.add(getData(vendorID).trim());
                entry.add(getData(descr)); //Product Description
                entry.add("BUTCO"); //Stock site
                entry.add(EA_STR); //Sales Unit
                entry.add(getData(qtcol));//Quantity
                entry.add(getValue(distCost, evaluator));//price
                entry.add(ZERO);
                entry.add(EMPTY_STR);
                entry.add(EMPTY_STR);
                dataEntry.append(entry).append(NEW_LINE_STR);
                System.out.println(dataEntry.toString());
            }

        }
    }

    /*
        Function to take a cell (Excel spreadsheet cell) as a param
        Use 'switch' statement to determine the cell value's type
        Retrieves that value and returns it as a type String
        CELL_.._FORMULA - Returns a number value for Price column
        -----Excludes the '$'-------
     */
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

//    private boolean isEU(Cell cell) {
//        ArrayList<Cell> cells = new ArrayList<>();
//        Iterator<Cell> cellIterator = row.cellIterator();
//        cellIterator.forEachRemaining(cells::add);
//    }

    private int getNumeric(String quantity) {
        try {
            return Integer.parseInt(quantity);
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatDate(String date) {
        String newDate = null;
        try {
            String pattern = "yyyyMMdd";
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            Date d = df.parse(date);
            newDate = df.format(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return newDate;
    }

    private void getTestItems(Sheet sheet, StringBuilder sb, FormulaEvaluator evaluator) {
        boolean entryStart = false;
        Map<String, Integer> indexMap = new HashMap<>(butterFormIndex.get(1));
        Optional<Integer> maxValue = indexMap.entrySet().stream().map(entry -> entry.getValue())
                .max(Comparator.comparingInt(Integer::valueOf));
        log.info("sheet index :" + sheet + " and max index :" + maxValue);

        int index = 0;
        StringJoiner entry;
        for (Row row : sheet) {
            entry = new StringJoiner(TILDE);
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);

            Cell firstCol = row.getCell(indexMap.get(B_TEST_SKU));
            String firstHeader = getData(firstCol);

            if (firstHeader != null && "TESTER ID".equals(firstHeader.trim())) {
                entryStart = true;
                continue;
            }
            if (!entryStart) {
                continue;
            }
            if (entryStart && cells.size() >= maxValue.get()) {
                Cell testCol = row.getCell(indexMap.get(B_TEST_SKU));
                Cell testUpc = row.getCell(indexMap.get(B_TEST_UPC));
                Cell testCost = row.getCell(indexMap.get(B_TEST_COST)); // Cell value for the Quantity column
                Cell testQty = row.getCell(indexMap.get(B_TEST_QTY));

                String itemNum = getData(testCol);
                itemNum = itemNum.trim();
                itemNum = itemNum.replace(" ", "");
                String quantity = getData(testQty);
                String description = "";
                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {
                    description = x3Dao.getItemDescription(getData(testUpc));
                    entry.add(CHAR_L);
                    entry.add(itemNum);
                    entry.add(description);
                    entry.add("BUTCO");
                    entry.add(EA_STR);
                    entry.add(getData(testQty));
                    entry.add(getValue(testCost, evaluator));
                    entry.add(ZERO);
                    entry.add(EMPTY_STR);
                    entry.add(EMPTY_STR);
                    sb.append(entry).append(NEW_LINE_STR);
                }
            }
        }
    }

    private void getCoreKit(Sheet sheet, StringBuilder ab, FormulaEvaluator evaluator) {
        boolean entryStart = false;
        Map<String, Integer> indexMap = new HashMap<>(butterFormIndex.get(0));
        Optional<Integer> maxValue = indexMap.entrySet().stream().map(entry -> entry.getValue())
                .max(Comparator.comparingInt(Integer::valueOf));
        log.info("sheet index :" + sheet + " and max index :" + maxValue);

        int index = 0;
        StringJoiner entry;
        for (Row row : sheet) {
            entry = new StringJoiner(TILDE);
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);

            Cell firstCol = row.getCell(1);
            String firstHeader = getData(firstCol);

            if (firstHeader != null && "CORE KIT".equals(firstHeader.trim())) {
                entryStart = true;
                continue;
            }
            if (!entryStart) {
                continue;
            }
            if (entryStart && cells.size() >= maxValue.get()) {
                Cell coreItmNum = row.getCell(indexMap.get(RETAIL_ITEM_NUM));
                Cell coreUpc = row.getCell(indexMap.get(RETAIL_UPC));
                Cell desc = row.getCell(indexMap.get(B_PRODUCT_DESCR)); // Cell value for the Quantity column
//                Cell euUpc = row.getCell(indexMap.get(EU_UPC_NUM));
//                Cell descr = row.getCell(indexMap.get(PROD_DESC));
                Cell distCost = row.getCell(indexMap.get(DIST_PRICE));
                Cell coreQty = row.getCell(indexMap.get(B_QTY));

                String quantity = getData(coreQty);
                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {
                    entry.add(CHAR_L);
                    entry.add(getData(coreItmNum));
                    entry.add(getData(desc));
                    entry.add("BUTCO");
                    entry.add(EA_STR);
                    entry.add(quantity);
                    entry.add(getValue(distCost, evaluator));
                    entry.add(ZERO);
                    entry.add(EMPTY_STR);
                    entry.add(EMPTY_STR);
                    ab.append(entry).append(NEW_LINE_STR);
                }
            }
        }
//        return sb.toString();
    }

    /*
        Populates the first line in the text file with
        information regarding the order's origin
     */
    public String populateHeader(String[] fileName) {
        StringJoiner header = new StringJoiner(TILDE);
        fileName.toString();
        String name = fileName[2];
        name = name.substring(0, name.indexOf(DOT));
        header.add(CHAR_E);
        header.add("BUTCO");//Sales site
        header.add("BBLK");//Order type
        header.add(EMPTY_STR);//Order number (blank)
        header.add(name);
        header.add(fileName[1]); // Date
        header.add(fileName[1]); // Date
        header.add("BUTCO"); // Sales Site
        header.add(US_CURR); // Currency type
        // Loop to add 26 empty strings ('~') for the correct ifile format
        for (int i = 0; i < 26; i++) {
            header.add(EMPTY_STR);
        }
//        header.add(shipVia);
//        header.add(shipVia);
        header.add(x3Dao.getPaymentTerms(poType));
        return header.toString();
    }

    private String currentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.now();
//		LocalDate date = LocalDate.parse(today, formatter);
        String td = date.format(formatter);
        return td;
    }
}
