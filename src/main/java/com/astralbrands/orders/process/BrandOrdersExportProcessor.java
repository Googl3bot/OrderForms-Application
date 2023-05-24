package com.astralbrands.orders.process;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.constants.AppConstants;
import com.astralbrands.orders.dao.X3BPCustomerDao;


@Component
public class BrandOrdersExportProcessor implements BrandOrderForms, AppConstants{
    @Autowired
    X3BPCustomerDao x3BPCustomerDao;

    // Map Objects to hold Key/Value pairs for the column name and the column position
    static Map<Integer, Map<String, Integer>> colIndexMap = new HashMap<>();

    static {
        // PUR Form Mappings
        Map<String, Integer> purForm = new HashMap<>();
        purForm.put(RETAIL_ITEM_NUM, 3); // Item # column
        purForm.put(RETAIL_UPC, 4); // Product name column
        purForm.put(EU_RETAIL_NUM, 5); // Quantity of Units Ordered column
        purForm.put(EU_UPC_NUM, 6); // Distributor Price column
        purForm.put(BO_PRODUCT_DESCRIPTION, 8); // Quantity of Units Ordered column
        purForm.put(C_F_S, 9); // Distributor Price column
        purForm.put(SHADE_NUM, 10); // Distributor Price column
        purForm.put(BO_PRICE, 11); // Quantity of Units Ordered column
        purForm.put(DIST_PRICE, 14); // Distributor Price column
        purForm.put(BO_QTY_ORDERED, 15);
        purForm.put(EXT_PRICE, 16);
        purForm.put(TESTER_SKU, 18);
        purForm.put(TESTER_UPC, 19);

        purForm.put(FORM_BRAND, 4);


        // BUTTER Form Mappings
        Map<String, Integer> butterMap = new HashMap<>();
        butterMap.put(B_VENDOR_ID, 3);
        butterMap.put(B_UPC, 4);
        butterMap.put(B_PRODUCT_DESCR, 5);
        butterMap.put(B_SHADE_NAME, 6);
        butterMap.put(B_EU, 7);
        butterMap.put(B_WEIGHT, 8);

        butterMap.put(B_TEST_SKU, 18);
        butterMap.put(B_TEST_UPC, 19);
        butterMap.put(B_TEST_COST, 21);
        butterMap.put(B_TEST_QTY, 23);
        butterMap.put(B_TEST_PRICE, 22);

        butterMap.put(FORM_BRAND, 4);

        Map<String, Integer> cosMap = new HashMap<>();
        cosMap.put(C_ITEM_NUM, 3);
        cosMap.put(C_UPC, 4);
        cosMap.put(C_PRODUCT, 6);
        cosMap.put(C_PROD_DESCR, 7);
        cosMap.put(C_PROD_SIZE, 8);
        cosMap.put(C_DIST_PRICE, 12);
        cosMap.put(C_QTY, 13);

        cosMap.put(C_TEST_SKU, 18);
        cosMap.put(C_TEST_COST, 21);
        cosMap.put(C_TEST_QTY, 23);



        colIndexMap.put(0, purForm);
        colIndexMap.put(1, butterMap);
        colIndexMap.put(2, cosMap);
    }


    Logger log = LoggerFactory.getLogger(CosmedixOrderProcessor.class);

    private String startPoint = "";
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





    @Override
    public void process(Exchange exchange, String site, String[] fileNameData) {
        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        try {
            Workbook workbook = new XSSFWorkbook(inputStream); // Object to hold multiple Excel sheets
            int numOfSheet = workbook.getNumberOfSheets();

            String headerStr= "";
            StringBuilder prodEntry = new StringBuilder();
            log.info("Number of sheets we are processing :" + numOfSheet);
            // Loop to process multiple sheets
            for (int i = 0; i < numOfSheet; i++) {
                Sheet firstSheet = workbook.getSheetAt(i);
                headerStr = populateHeader(firstSheet, site); // Populates the first line in the TXT file
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                readSheet(firstSheet, prodEntry, evaluator, i); // Populates prodEntry with product info
                log.info(i + " sheet name: " + firstSheet);
            }
            log.info("data entry : " + prodEntry.toString());
            String data = headerStr + NEW_LINE_STR + prodEntry.toString(); // Formats a String with both Header line and product info lines
            if (data.length() > 0) { // Ensures the file isn't empty or invalid
                exchange.getMessage().setBody(data);
                exchange.setProperty(CSV_DATA, data.replace(TILDE, COMMA)); // CSV file data for later processing
                exchange.setProperty("IFILE", data);
                exchange.getMessage().setHeader(Exchange.FILE_NAME, exchange.getProperty(INPUT_FILE_NAME) + DOT_TXT); // txt file
                exchange.setProperty(IS_DATA_PRESENT, true);
                exchange.setProperty(SITE_NAME, Site);
                exchange.setProperty(BRAND, "International Brand Export Forms");
            } else {
                exchange.setProperty(IS_DATA_PRESENT, false);
            }
        } catch (IOException e) {
            exchange.setProperty(IS_DATA_PRESENT, false);
        }
    }

    /*
        Method to process an Excel sheet and iterate through every row
        adding cell values for the columns regarding product information
        If the Quantity column is 0 or blank it will be skipped.
        ---------Builds/Adds the products info lines in the TXT file----------
     */
    private void readSheet(Sheet firstSheet, StringBuilder dataEntry, FormulaEvaluator evaluator, int sheetIndex) {
        boolean entryStart = false;
        Map<String, Integer> indexMap = new HashMap<>();
        Optional<Integer> maxValue = indexMap.entrySet().stream().map(entry -> entry.getValue())
                .max(Comparator.comparingInt(Integer::valueOf));
        log.info("sheet index :" + sheetIndex + " and max index :" + maxValue);

        getFormType(firstSheet);
        if(site.equalsIgnoreCase("PURCO")) {
            indexMap = colIndexMap.get(0);
        } else if(site.equalsIgnoreCase("BUTCO")) {
            indexMap = colIndexMap.get(1);
        } else if(site.equalsIgnoreCase("COSCO")) {
            indexMap = colIndexMap.get(2);
        }

        int index = 0;
        StringJoiner entry;
        for (Row row : firstSheet) {
            entry = new StringJoiner(TILDE);
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);
            Cell firstCol = row.getCell(indexMap.get(FORM_BRAND)); // Gets the cell's position
            String firstHeader = getData(firstCol);// Converts the value to a String  - Name of the first column
            if (sheetIndex == 0) {

            }
            index++;
            // Iterates through each row in the sheet until it reaches the column names
            if (firstHeader != null && startPoint.equals(firstHeader.trim())) {
                entryStart = true;
                continue;
            }
            if (!entryStart) {
                continue;
            }
            if (entryStart && cells.size() >= maxValue.get()) {

                // Cell objects to hold value for the position of a column
                Cell prodCol = row.getCell(indexMap.get(RETAIL_ITEM_NUM));// Cell value for US Item Number
                Cell boUpc = row.getCell(indexMap.get(RETAIL_UPC));
                Cell descr = row.getCell(indexMap.get(BO_PRODUCT_DESCRIPTION));
                Cell descr1 = row.getCell(indexMap.get(C_F_S));
                Cell qtcol = row.getCell(indexMap.get(BO_QTY_ORDERED)); // Cell value for the Quantity column
                Cell distCost = row.getCell(indexMap.get(DIST_PRICE));
                Cell priceCol = row.getCell(indexMap.get(BO_PRICE));// Cell value for the product price column
                Cell euItmNum = row.getCell(indexMap.get(EU_RETAIL_NUM));
                Cell euUpc = row.getCell(indexMap.get(EU_UPC_NUM));



                String itmNum;
                String upc;
                String description = (getData(descr) + " " + getData(descr1));
                String cost = getData(distCost);
//				log.info(index + " index " + getData(firstCol) + ", prodcol :" + getData(prodCol)
//						+ ", qtCol :" + getData(qtcol) + ", price col :" + getData(priceCol));
                String quantity = getData(qtcol); // Gets the value for the Quantity column
                // Builds/Adds the product info lines into the ifile - Only if the Quantity column is greater than 0
                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {
//                    if(euItmNum != null) {
//                         itmNum = getData(euItmNum);
//                         upc = getData(euUpc);
//                    } else {
//                         itmNum = getData(prodCol);
//                         upc = getData(boUpc);
//                    }
                    entry.add(CHAR_L);
//                    entry.add(itmNum); //ITMREF
                    entry.add(description); //Product Description
                    entry.add(site); //Stock site
                    entry.add(EA_STR); //Sales Unit
                    entry.add(getData(qtcol));//Quantity
                    entry.add(getValue(distCost, evaluator));//price
                    entry.add(EMPTY_STR);
                    entry.add(EMPTY_STR);
                    entry.add(EMPTY_STR);
                    dataEntry.append(entry).append(NEW_LINE_STR);
                }
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

    /*
        Populates the first line in the text file with
        information regarding the order's origin
     */
    public String populateHeader(Sheet a, String site1) {
        StringJoiner header = new StringJoiner(TILDE);
        getFormType(a);
        header.add(CHAR_E);
        header.add(site);//Sales site
        header.add(siteID);//Order type
        header.add(EMPTY_STR);//Order number (blank)
        header.add(customerRefNumber);
        header.add(date); // Date
        header.add(date); // Date
        header.add(site); // Sales Site
        header.add(US_CURR); // Currency type
        // Loop to add 26 empty strings ('~') for the correct ifile format
        for (int i = 0; i < 26; i++) {
            header.add(EMPTY_STR);
        }
        header.add(shipVia);
        header.add(shipVia);
        header.add(x3BPCustomerDao.getPaymentTerms(customerRefNumber));
        return header.toString();
    }

    private void getFormType(String site) {

    }

    private void getFormType(Sheet first) {
        Map<String, Integer> map = colIndexMap.get(0);
//        String brandType = "", cust_numType = "", cust_nameType = "", termsType = "", dateType = "",
//                shipType = "", poType = "";

        for(Row row : first) {
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);

            Cell brandOne = row.getCell(map.get(FORM_BRAND) - 1);
            String b = getData(brandOne);

            if(b.equalsIgnoreCase("Brand:")) {
               Cell brand = row.getCell(map.get(FORM_BRAND));
               brandType = getData(brand);
               brandType = brandType.substring(0, 1);
               if(brandType.equalsIgnoreCase("P")) {
                   site = "PURCO";
                   siteID = "PBLK";
                   startPoint = "Retail Item #";
               }
               if(brandType.equalsIgnoreCase("B")) {
                   site = "BUTCO";
                   siteID = "BBLK";
                   startPoint = "Vendor ID";
               }
               if(brandType.equalsIgnoreCase("C")) {
                   site = "COSCO";
                   siteID = "CBLK";
                   startPoint = "Item #";
               }
            }
//            if(map.get(FORM_BRAND).toString().equalsIgnoreCase("ASTRAL Customer Number:")) {
//                Cell cust_num = row.getCell(map.get(FORM_BRAND));
//                cust_numType = getData(cust_num);
//            }
//            if(map.get(FORM_BRAND).toString().equalsIgnoreCase("CUSTOMER NAME:")) {
//                Cell cust_name = row.getCell(map.get(FORM_BRAND));
//                cust_nameType = getData(cust_name);
//            }
//            if(map.get(FORM_BRAND).toString().equalsIgnoreCase("TERMS:")) {
//                Cell terms = row.getCell(map.get(FORM_BRAND));
//                termsType = getData(terms);
//            }
//            if(map.get(FORM_BRAND).toString().equalsIgnoreCase("Date:")) {
//                Cell date = row.getCell(map.get(FORM_BRAND));
//                dateType = getData(date);
//            }
//            if(map.get(FORM_BRAND).toString().equalsIgnoreCase("SHIP VIA:")) {
//                Cell ship = row.getCell(map.get(FORM_BRAND));
//                shipType = getData(ship);
//            }
//            if(map.get(FORM_BRAND).toString().equalsIgnoreCase("PO #:")) {
//                Cell po_num = row.getCell(map.get(FORM_BRAND));
//                poType = getData(po_num);
//            }

        }

    }

    private int checkEU(Cell cell) {
        Map<String, Integer> map = colIndexMap.get(0);
        int skuIndex = 0;
        if(getData(cell) == null || (getData(cell)).length() < 1) {
            skuIndex = map.get(RETAIL_ITEM_NUM);
        } else {
            skuIndex = map.get(EU_RETAIL_NUM);
        }
        return skuIndex;
    }
}

//        if(brandType.equalsIgnoreCase("PUR")) {
//        siteID = "P";
////            siteID = "19999991";
//        site = "PURCO";
//        sellerID = "PNYK";
//        } else if(brandType.equalsIgnoreCase("COS")) {
//        siteID = "C";
////            siteID = "29999991";
//        site = "COSCO";
//        sellerID = "CNYK";
//        } else if(brandType.equalsIgnoreCase("BUT")) {
//        siteID = "B";
////            siteID = "69999991";
//        site = "BUTCO";
//        sellerID = "BNYK";