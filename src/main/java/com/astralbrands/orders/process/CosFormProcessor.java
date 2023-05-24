package com.astralbrands.orders.process;

import com.astralbrands.orders.constants.AppConstants;
import com.astralbrands.orders.dao.X3BPCustomerDao;
import org.apache.camel.Exchange;
import java.util.regex.*;
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

import static java.lang.Integer.valueOf;

@Component
public class CosFormProcessor implements BrandOrderForms, AppConstants {

    static Map<Integer, Map<String, Integer>> cosFormIndex = new HashMap<>();
    static {
        // COS Form Mappings
        Map<String, Integer> cosForm = new HashMap<>();
        cosForm.put(C_ITEM_NUM, 3); // Item # column
        cosForm.put(C_UPC, 4); // Product name column
        cosForm.put(C_EU, 5);
        cosForm.put(C_PRODUCT, 6); // Quantity of Units Ordered column
        cosForm.put(C_PROD_DESCR, 7); // Distributor Price column
        cosForm.put(C_PROD_SIZE, 8); // Quantity of Units Ordered column
        cosForm.put(C_DIST_PRICE, 14); // Distributor Price column
        cosForm.put(C_QTY, 15); // Distributor Price column

        cosForm.put(FORM_BRAND, 4);

        Map<String, Integer> cosFormTesters = new HashMap<>();
        cosFormTesters.put(C_TEST_SKU, 20); // Quantity of Units Ordered column
        cosFormTesters.put(C_TEST_UPC, 21); // Distributor Price column
        cosFormTesters.put(C_TEST_COST, 23);
        cosFormTesters.put(C_TEST_QTY, 25);
//        cosForm.put(TESTER_SKU, 18);
//        cosForm.put(TESTER_UPC, 19);
//        cosForm.put(TESTER_COST, 21);
//        cosForm.put(TESTER_QTY, 23);

        // Marketing Collateral Sheet
        Map<String, Integer> secondSheet = new HashMap<>();
        secondSheet.put(C_ITEM_NUM, 0);
        secondSheet.put(C_UPC, 1);
        secondSheet.put(C_PRODUCT, 2);
        secondSheet.put(C_QTY, 3);
        secondSheet.put(C_DIST_PRICE, 4);

        // Testers Sheet
        Map<String, Integer> thirdSheet = new HashMap<>();
        thirdSheet.put(C_ITEM_NUM, 0);
        thirdSheet.put(C_PRODUCT, 2);
        thirdSheet.put(C_DIST_PRICE, 5);
        thirdSheet.put(C_QTY, 6);

        cosFormIndex.put(0, cosForm);
        cosFormIndex.put(1, cosFormTesters);
        cosFormIndex.put(2, secondSheet);
        cosFormIndex.put(3, thirdSheet);
    }
    private String startPoint = "";
    private String startPoint2 = "";
    private String startPoint3 = "";
    private String startPoint4 = "";
    private String site = "";
    private String sellerID = "";
    private String siteID = "";
    private String customerRefNumber = "";
    private String date = "";
    private String shipVia = "";
    private String formBrand = "";
    private String brandType = "";
    private String cust_numType = "";
    private String cust_nameType = "";
    private String termsType = "";
    private String dateType = "";
    private String shipType = "";
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
            StringBuilder builder = new StringBuilder();
            log.info("Number of sheets we are processing :" + numOfSheet);
            // Loop to process multiple sheets
            for (int i = 0; i < numOfSheet - 1; i++) {
                Sheet firstSheet = workbook.getSheetAt(i);
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                if(i == 0) {
                    readSheet(firstSheet, prodEntry, evaluator, i); // Populates prodEntry with product info
                    log.info(i + " sheet name: " + firstSheet);
                    getTestItems(firstSheet, prodEntry, evaluator);
                } else {
                    readSheet(firstSheet, prodEntry, evaluator, i);
                }

            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            String date = currentDate();
            String header = populateHeader(fileName);
            log.info("data entry : " + prodEntry.toString());
            String data = header + NEW_LINE_STR + prodEntry.toString(); //+ sb.toString() + builder.toString(); // Formats a String with both Header line and product info lines
            if (data.length() > 0) { // Ensures the file isn't empty or invalid
                exchange.getMessage().setBody(data);
                exchange.setProperty(BRAND, "COSMEDIX");
                exchange.setProperty(CSV_DATA, data.replace(TILDE, COMMA)); // CSV file data for later processing
                exchange.setProperty("IFILE", data);
                exchange.getMessage().setHeader(Exchange.FILE_NAME, "COS_" + date + "_" + exchange.getProperty(CUST_NUMBER) + DOT_TXT); // txt file
                exchange.setProperty(IS_DATA_PRESENT, true);
                exchange.setProperty(SITE_NAME, brand);
            } else {
                exchange.setProperty(IS_DATA_PRESENT, false);
            }
        } catch (IOException e) {
            exchange.setProperty(IS_DATA_PRESENT, false);
        }
    }

    private void readSheet(Sheet firstSheet, StringBuilder dataEntry, FormulaEvaluator evaluator, int sheetIndex) {
        boolean entryStart = false;
        Map<String, Integer> indexMap = new HashMap<>(cosFormIndex.get(0));
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
            Cell firstCol = row.getCell(indexMap.get(C_ITEM_NUM)); // Gets the cell's position
            String firstHeader = getData(firstCol);// Converts the value to a String  - Name of the first column
            if (sheetIndex == 0) {
                formBrand = getData(row.getCell(3));
                if ("ASTRAL Customer Number:".equals(formBrand)) {
                    cust_numType = getData(row.getCell(4));
                }
//                if("CUSTOMER NAME:".equalsIgnoreCase(formBrand)) {
//                    cust_nameType = getData(row.getCell(4));
//                }
                if ("TERMS:".equals(formBrand)) {
                    poType = getData(row.getCell(4));
                }
            }
            index++;
            // Iterates through each row in the sheet until it reaches the column names
//            if (startPoint.equals(getData(row.getCell(3)).trim())) {   //  firstHeader != null &&  -----firstHeader.trim()
//                entryStart = true;
//                continue;
//            }
//            if(getData(row.getCell(3)).contains("Professional")) {
//                entryStart = true;
//                continue;
//            }
//            if(getData(row.getCell(3)).trim().equalsIgnoreCase(startPoint) ) {
//                entryStart = true;
//                continue;
//            }
//            if (!entryStart) {
//                continue;
//            }
//            if (entryStart && cells.size() >= maxValue.get()) {

            startPoint2 = "Quantity of Units Ordered";
            //if (startPoint.equals(firstHeader.trim()) && cells.size() >= maxValue.get()) {
//            if (getValue(row.getCell(3), evaluator) == null || getValue(row.getCell(3), evaluator).matches("[a-zA-Z]")) {
//                entryStart = false;
//                continue;
//                }
//            if (getValue(row.getCell(15), evaluator) == null) {
//                entryS tart = false;
//                continue;
//            }
            startPoint = "Item #";
            if(firstHeader != null && startPoint.equals(firstHeader.trim())) {
                entryStart = true;
                continue;
            }
            if(!entryStart) {
                continue;
            }
            if(entryStart && cells.size() >= maxValue.get()) {

                // Cell objects to hold value for the position of a column
                Cell retailItmNum = row.getCell(indexMap.get(C_ITEM_NUM));// Cell value for US Item Number
                Cell descr = row.getCell(indexMap.get(C_PRODUCT));

                Cell qtcol = row.getCell(indexMap.get(C_QTY)); // Cell value for the Quantity column
                Cell distCost = row.getCell(indexMap.get(C_DIST_PRICE));
                //--Integer.parseInt(String.valueOf(qtcol.getNumericCellValue()))
                if (retailItmNum != null && qtcol != null && isBlankCell(qtcol.toString())) {
                    String qty = getValue(qtcol, evaluator);
                    // Builds/Adds the product info lines into the ifile - Only if the Quantity column is greater than 0
                    if (qty.trim() != null && getNumeric(qty) > 0 && qty.trim().length() > 0) {    //--quantity.trim().length() > 0
                        String itemNum = getData(retailItmNum);
                        itemNum = itemNum.trim();
//                       itemNum = itemNum.replace(" ", "");
                        entry.add(CHAR_L);
                        entry.add(itemNum);
                        entry.add(getData(descr)); //Product Description
                        entry.add("COSCO"); //Stock site
                        entry.add(EA_STR); //Sales Unit
                        entry.add(String.valueOf(qtcol));//Quantity
                        entry.add(getValue(distCost, evaluator));//price
                        entry.add(ZERO);
                        entry.add(EMPTY_STR);
                        entry.add(EMPTY_STR);
                        dataEntry.append(entry).append(NEW_LINE_STR);
                        System.out.println(dataEntry.toString());
                    } else {
                        continue;
                    }
                }
            }
        }
    }
    private boolean isBlankCell(String cell) {
        if(cell == null || cell.trim().isEmpty()) {
            return false;
        } else {
            return true;
        }
    }
    private int getNumber(String quantity) {
        try {
            return Integer.parseInt(quantity);
        } catch (Exception e) {
            return 0;
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
        if(cell != null) {
            switch (cell.getCellType()) {
                case Cell.CELL_TYPE_STRING:
                    value = cell.getStringCellValue();
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    value = cell.getNumericCellValue();
                    break;
                case Cell.CELL_TYPE_ERROR:
                    value = cell.getErrorCellValue();
                    break;
                case Cell.CELL_TYPE_BLANK:
                    value = null;
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    value = evaluator.evaluate(cell);
                    if (value != null) {
                        double val = (double) value;
                        value = Math.round(val * 100.0) / 100.0;
                    }
                    return value.toString();
                default:
                    break;
            }
        }
        return value.toString();
    }

//    private boolean isEU(Cell cell) {
//        ArrayList<Cell> cells = new ArrayList<>();
//        Iterator<Cell> cellIterator = row.cellIterator();
//        cellIterator.forEachRemaining(cells::add);
//    }

    private double getNumeric(String quantity) {
        double conversion = 0;
        try {
            if(quantity != null || quantity.matches("[0-9]+") || !quantity.isEmpty()) {
                conversion = Double.parseDouble(quantity);
            }
        } catch (NumberFormatException e) {
            System.err.println("ERROR PARSING STRING CELL VALUE");
            e.printStackTrace();
            conversion = 0;
        }
        return conversion;
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
        Map<String, Integer> testMap = new HashMap<>(cosFormIndex.get(1));
        Optional<Integer> maxValue = testMap.entrySet().stream().map(entry -> entry.getValue())
                .max(Comparator.comparingInt(Integer::valueOf));
        log.info("sheet index :" + sheet + " and max index :" + maxValue);

        int index = 0;
        StringJoiner entry;
        for (Row row : sheet) {
            entry = new StringJoiner(TILDE);
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);

            Cell firstCol = row.getCell(testMap.get(TESTER_SKU));
            String firstHeader = getData(firstCol);

            if (firstHeader != null && "Tester SKU".equals(firstHeader.trim())) {
                entryStart = true;
                continue;
            }
            if (!entryStart) {
                continue;
            }
            if (entryStart && cells.size() >= maxValue.get()) {
                Cell testCol = row.getCell(testMap.get(TESTER_SKU));
                Cell testUpc = row.getCell(testMap.get(TESTER_UPC));
                Cell testCost = row.getCell(testMap.get(TESTER_COST)); // Cell value for the Quantity column
                Cell testQty = row.getCell(testMap.get(TESTER_QTY));

                String itemNum = getData(testCol);
                itemNum = itemNum.trim();
//                itemNum = itemNum.replace(" ", "");
//                if(testQty != null) {
//
//                }
                String quantity = getData(testQty);
                String description = "";
                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {
                    description = x3Dao.getItemDescription(getData(testUpc));
                    entry.add(CHAR_L);
                    entry.add(itemNum);
                    entry.add(description);
                    entry.add("COSCO");
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
        header.add("COSCO");//Sales site
        header.add("CBLK");//Order type
        header.add(EMPTY_STR);//Order number (blank)
        header.add(name);
        header.add(fileName[1]); // Date
        header.add(fileName[1]); // Date
        header.add("COSCO"); // Sales Site
        header.add(US_CURR); // Currency type
        // Loop to add 26 empty strings ('~') for the correct ifile format
        for (int i = 0; i < 26; i++) {
            header.add(EMPTY_STR);
        }
//        header.add(shipVia);
//        header.add(shipVia);
        header.add(x3Dao.getPaymentTerms(fileName[2]));
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

