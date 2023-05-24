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
public class PurFormProcessor implements BrandOrderForms, AppConstants {
    //    static Map<Integer, Map<String, Integer>> colIndexMap = new HashMap<>();
    static Map<Integer, Map<String, Integer>> purFormIndex = new HashMap<>();
    static {
        // PUR Form Mappings
        Map<String, Integer> purForm = new HashMap<>();
        purForm.put(CORE_KIT, 0);
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

        purForm.put(FORM_BRAND, 3);

//        purForm.put(TESTER_SKU, 20);
//        purForm.put(TESTER_UPC, 21);
//        purForm.put(TESTER_COST, 23);
//        purForm.put(TESTER_QTY, 25);

        // TESTER ITEMS & GRAND TOTAL FOR ORDER
        Map<String, Integer> testGross = new HashMap<>();
        testGross.put(TESTER_SKU, 20);
        testGross.put(TESTER_UPC, 21);
        testGross.put(TESTER_COST, 23);
        testGross.put(TESTER_QTY, 25);

        testGross.put(GRAND_TOTAL, 20);
        testGross.put(TOTAL_LINES, 24);
        testGross.put(TOTAL_PRICE, 25);

        // CORE KIT SECTION MAPPINGS
        Map<String, Integer> coreKit = new HashMap<>();
        coreKit.put(CORE_KIT, 0);
        coreKit.put(RETAIL_ITEM_NUM, 3);
        coreKit.put(UPC_NUMBER, 4);
        coreKit.put(EU_RETAIL_NUM, 5);
        coreKit.put(EU_UPC_NUM, 6);
        coreKit.put(PROD_DESC, 8);
        coreKit.put(DIST_COST, 14);
        coreKit.put(BO_QTY_ORDERED, 15);

        //SEASONAL KITS
        Map<String, Integer> seasonal = new HashMap<>();
        seasonal.put(RETAIL_ITEM_NUM, 3);
        seasonal.put(UPC_NUMBER, 4);
        seasonal.put(EU_RETAIL_NUM, 5);
        seasonal.put(EU_UPC_NUM, 6);
        seasonal.put(PROD_DESC, 8);
        seasonal.put(DIST_COST, 14);
        seasonal.put(BO_QTY_ORDERED, 15);

        purFormIndex.put(0, purForm);
        purFormIndex.put(1, testGross);
        purFormIndex.put(2, coreKit);
        purFormIndex.put(3, seasonal);
    }
    private String startPoint1 = "";
    private String startPoint2 = "";
    private String startPoint3 = "";
    private String site = "";
    private String sellerID = "";
    private String siteID = "";
    private String customerRefNumber = "";
    private String date = "";
    private String shipVia = "";
    private String formBrand = "";
    private String brandType = "";
    private String cust_numType = "";
    private String totalQty = "";
    private String grossPrice = "";
//    private String cust_nameType = "";
//    private String termsType = "";
//    private String dateType = "";
//    private String shipType = "";
    private String poType = "";

    @Autowired
    X3BPCustomerDao x3Database;

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
            StringBuilder bb = new StringBuilder();
            log.info("Number of sheets we are processing :" + numOfSheet);
            // Loop to process multiple sheets
            for (int i = 0; i < numOfSheet; i++) {
                Sheet firstSheet = workbook.getSheetAt(i);
                FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
                readSheet(firstSheet, prodEntry, evaluator, i); // Populates prodEntry with product info
                log.info(i + " sheet name: " + firstSheet);
                if(i == 0) {
//                    getCoreKit(firstSheet, ab, evaluator);
//                    getSeasonalKit(firstSheet, bb, evaluator);
                    getTestItems(firstSheet, prodEntry, evaluator);
                }
            }
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            String date = currentDate();

            String header = populateHeader(fileName);
            log.info("data entry : " + prodEntry.toString());
            String data = header + NEW_LINE_STR + prodEntry.toString(); //+ sb.toString() + ab.toString() + bb.toString(); // Formats a String with both Header line and product info lines
            if (data.length() > 0) { // Ensures the file isn't empty or invalid
                exchange.getMessage().setBody(data);
                exchange.setProperty(CSV_DATA, data.replace(TILDE, COMMA)); // CSV file data for later processing
                exchange.setProperty(BRAND, fileName[0]);
                exchange.setProperty("IFILE", data);
                exchange.getMessage().setHeader(Exchange.FILE_NAME, "PUR_" + date + "_" + exchange.getProperty(CUST_NUMBER) + DOT_TXT); // txt file
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
        Map<String, Integer> indexMap = new HashMap<>(purFormIndex.get(0));
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
            Cell firstCol = row.getCell(indexMap.get(RETAIL_ITEM_NUM)); // Gets the cell's position
            String firstHeader = getData(firstCol);// Converts the value to a String  - Name of the first column
            if (sheetIndex == 0) {
                formBrand = getData(row.getCell(3));
//                if("Brand:".equals(formBrand)) {
//                    brandType = getData(row.getCell(4));
//                }
                if("ASTRAL Customer Number:".equals(formBrand)) {
                    cust_numType = getData(row.getCell(4));
                }
//                }
//                if("CUSTOMER NAME:".equalsIgnoreCase(formBrand)) {
//                    cust_nameType = getData(row.getCell(4));
//                }
                if("CUSTOMER NAME:".equals(formBrand)) {
                    poType = getData(row.getCell(4));
                    System.out.println("Purchase Order number in the File is " + poType);
                }
            }
            startPoint1 = "Retail Item #";
            index++;
            // Iterates through each row in the sheet until it reaches the column names
//            if (firstHeader != null && startPoint1.equals(firstHeader.trim())) {
//                entryStart = true;
//                continue;
//            }
//            String flag = getData(row.getCell(3));
//            if (flag == null || flag.length() <= 0) {
//                continue;
//            }
//            if (!entryStart) {
//                continue;
//            }
//            if (entryStart && cells.size() >= maxValue.get()) {

                // Cell objects to hold value for the position of a column
                Cell retailItmNum = row.getCell(indexMap.get(RETAIL_ITEM_NUM));// Cell value for US Item Number
                Cell boUpc = row.getCell(indexMap.get(RETAIL_UPC));
                Cell descr = row.getCell(indexMap.get(BO_PRODUCT_DESCRIPTION));
                Cell descr1 = row.getCell(indexMap.get(C_F_S));
                Cell qtcol = row.getCell(indexMap.get(BO_QTY_ORDERED)); // Cell value for the Quantity column
                Cell distCost = row.getCell(indexMap.get(DIST_PRICE));
                Cell priceCol = row.getCell(indexMap.get(BO_PRICE));// Cell value for the product price column
                Cell euItmNum = row.getCell(indexMap.get(EU_RETAIL_NUM));
                Cell euUpc = row.getCell(indexMap.get(EU_UPC_NUM));

                Cell core = row.getCell(indexMap.get(CORE_KIT));
//                String flag = "";
//                if(getData(core) != null) {
//                    flag = getData(core);
//                }


                Cell coreItmNum = row.getCell(indexMap.get(RETAIL_ITEM_NUM));
                Cell testUpc = row.getCell(indexMap.get(RETAIL_UPC));
                Cell testCost = row.getCell(indexMap.get(EU_RETAIL_NUM)); // Cell value for the Quantity column
                Cell coreQty = row.getCell(indexMap.get(BO_QTY_ORDERED));

//				log.info(index + " index " + getData(firstCol) + ", prodcol :" + getData(prodCol)
//						+ ", qtCol :" + getData(qtcol) + ", price col :" + getData(priceCol));
                String quantity = getData(qtcol); // Gets the value for the Quantity column
                String description = "";
                // Builds/Adds the product info lines into the ifile - Only if the Quantity column is greater than 0
                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {

                    if(getData(core).startsWith("CORE")) {
                        description = "Multitasking Essentials - Best Sellers Kit";
                    }
                    else if(getData(core) != null && getData(core).startsWith("LE")) {
                        description = getData(descr);
                        description = description.substring(0, description.indexOf(":"));
                    } else {
                        description = getData(descr);
                    }
                            //String description = getData(descr);
                            //description = description.substring(0, description.indexOf(':'));

//                            entry.add(CHAR_L);
//                            entry.add(getData(coreItmNum));
//                            entry.add(description.trim());  //  "Multitasking Essentials - Best Sellers Kit"
//                            entry.add("PURCO");
//                            entry.add(EA_STR);
//                            entry.add(quantity);
//                            entry.add(getValue(distCost, evaluator));
//                            entry.add(ZERO);
//                            entry.add(EMPTY_STR);
//                            entry.add(EMPTY_STR);
//                            entry.add(EMPTY_STR);
//                            dataEntry.append(entry).append(NEW_LINE_STR);

                    entry.add(CHAR_L);
                    if(getData(euItmNum) == null || euItmNum.getCellType() == Cell.CELL_TYPE_BLANK) {
                        entry.add(getData(retailItmNum).trim());
                    } else {
                        entry.add(getData(euItmNum).trim());
                    }
                    //---OLD DESCRIPTION --> getData(descr)
                    entry.add(description); //Product Description
                    entry.add("PURCO"); //Stock site
                    entry.add(EA_STR); //Sales Unit
                    entry.add(getData(qtcol));//Quantity
                    entry.add(getValue(distCost, evaluator));//price
                    entry.add(ZERO);
                    entry.add(EMPTY_STR);
                    entry.add(EMPTY_STR);
//                    entry.add(EMPTY_STR);
                    dataEntry.append(entry).append(NEW_LINE_STR);
                    System.out.println(dataEntry.toString());
                }

            }
        }
    //}


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

    private int getNumeric(String quantity) {
        try {
            return Integer.parseInt(quantity);
        } catch (Exception e) {
            return 0;
        }
    }

    private void getTestItems(Sheet sheet, StringBuilder sb, FormulaEvaluator evaluator) {
        boolean entryStart = false;
        Map<String, Integer> indexMap = new HashMap<>(purFormIndex.get(1));
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

            Cell firstCol = row.getCell(indexMap.get(TESTER_SKU));
            String firstHeader = getData(firstCol);

            Cell grandTotal = row.getCell(indexMap.get(GRAND_TOTAL));
            Cell grandQty = row.getCell(indexMap.get(TOTAL_LINES));
            Cell grandCost = row.getCell(indexMap.get(TOTAL_PRICE));
            String gTotal = "";

            if(getData(grandTotal) != null) {
                gTotal = getData(grandTotal);
            }

            if(GRAND_TOTAL.equalsIgnoreCase(gTotal.trim())) {
                totalQty = getData(grandQty);
                grossPrice = getData(grandCost);
            }

            if (firstHeader != null && "Tester SKU".equals(firstHeader.trim())) {
                entryStart = true;
                continue;
            }
            if (!entryStart) {
                continue;
            }
            if (entryStart && cells.size() >= maxValue.get()) {
                Cell testCol = row.getCell(indexMap.get(TESTER_SKU));
                Cell testUpc = row.getCell(indexMap.get(TESTER_UPC));
                Cell testCost = row.getCell(indexMap.get(TESTER_COST)); // Cell value for the Quantity column
                Cell testQty = row.getCell(indexMap.get(TESTER_QTY));

                String itemNum = getData(testCol);
                itemNum = itemNum.trim();
                itemNum = itemNum.replace(" ", "");
                String quantity = getData(testQty);
                String description = "";
                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {
                    description = x3Database.getItemDescription(getData(testUpc));
                    entry.add(CHAR_L);
                    entry.add(itemNum);
                    entry.add(description);
                    entry.add("PURCO");
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
//        return sb.toString();
    }

//    private void getCoreKit(Sheet sheet, StringBuilder ab, FormulaEvaluator evaluator) {
//        boolean entryStart = false;
//
//        Optional<Integer> maxValue = indexMap.entrySet().stream().map(entry -> entry.getValue())
//                .max(Comparator.comparingInt(Integer::valueOf));
//        log.info("sheet index :" + sheet + " and max index :" + maxValue);
//
//        int index = 0;
//        StringJoiner entry;
//        for (Row row : sheet) {
//            entry = new StringJoiner(TILDE);
//            ArrayList<Cell> cells = new ArrayList<>();
//            Iterator<Cell> cellIterator = row.cellIterator();
//            cellIterator.forEachRemaining(cells::add);
//
//            Cell firstCol = row.getCell(0);
//            String firstHeader = getData(firstCol);
//
//            if (firstHeader != null && "CORE KIT".equals(firstHeader.trim())) {
//                entryStart = true;
//                continue;
//            }
//            if (!entryStart) {
//                continue;
//            }
//            if (entryStart && cells.size() >= maxValue.get()) {
//                Cell coreItmNum = row.getCell(indexMap.get(RETAIL_ITEM_NUM));
//                Cell testUpc = row.getCell(indexMap.get(RETAIL_UPC));
//                Cell testCost = row.getCell(indexMap.get(EU_RETAIL_NUM)); // Cell value for the Quantity column
//                Cell euUpc = row.getCell(indexMap.get(EU_UPC_NUM));
//                Cell descr = row.getCell(indexMap.get(PROD_DESC));
//                Cell distCost = row.getCell(indexMap.get(DIST_PRICE));
//                Cell coreQty = row.getCell(indexMap.get(BO_QTY_ORDERED));
//
//                Cell seasonal = row.getCell(0);
//
//                String description = getData(descr);
//                description = description.substring(0, description.indexOf(':'));
//
//                String quantity = getData(coreQty);
//                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {
//                    entry.add(CHAR_L);
//                    entry.add(getData(coreItmNum));
//                    entry.add(description.trim());  //  "Multitasking Essentials - Best Sellers Kit"
//                    entry.add("PURCO");
//                    entry.add(EA_STR);
//                    entry.add(quantity);
//                    entry.add(getValue(distCost, evaluator));
//                    entry.add(ZERO);
//                    entry.add(EMPTY_STR);
//                    entry.add(EMPTY_STR);
//                    entry.add(EMPTY_STR);
//                    ab.append(entry).append(NEW_LINE_STR);
//                }
//            }
//        }
////        return sb.toString();
//    }

//    private void getSeasonalKit(Sheet sheet, StringBuilder ab, FormulaEvaluator evaluator) {
//        boolean entryStart = false;
//        Map<String, Integer> indexMap = new HashMap<>(purFormIndex.get(2));
//        Optional<Integer> maxValue = indexMap.entrySet().stream().map(entry -> entry.getValue())
//                .max(Comparator.comparingInt(Integer::valueOf));
//        log.info("sheet index :" + sheet + " and max index :" + maxValue);
//
//        int index = 0;
//        StringJoiner entry;
//        for (Row row : sheet) {
//            entry = new StringJoiner(TILDE);
//            ArrayList<Cell> cells = new ArrayList<>();
//            Iterator<Cell> cellIterator = row.cellIterator();
//            cellIterator.forEachRemaining(cells::add);
//
//            Cell firstCol = row.getCell(0);
//            String firstHeader = getData(firstCol);
////            String seasonal = "";
////            if() {
////                seasonal = firstHeader;
////            }
//
//
//            if (firstHeader != null && firstHeader.trim().contains("SEASONAL")) {
//                entryStart = true;
//                continue;
//            }
//            if (!entryStart) {
//                continue;
//            }
//            if (entryStart && cells.size() >= maxValue.get()) {
//                Cell coreItmNum = row.getCell(indexMap.get(RETAIL_ITEM_NUM));
//                Cell testUpc = row.getCell(indexMap.get(RETAIL_UPC));
//                Cell testCost = row.getCell(indexMap.get(EU_RETAIL_NUM)); // Cell value for the Quantity column
//                Cell euUpc = row.getCell(indexMap.get(EU_UPC_NUM));
//                Cell descr = row.getCell(indexMap.get(PROD_DESC));
//                Cell distCost = row.getCell(indexMap.get(DIST_PRICE));
//                Cell coreQty = row.getCell(indexMap.get(BO_QTY_ORDERED));
//
//                Cell seasonal = row.getCell(0);
//
//                String description = getData(descr);
//                description = description.substring(0, description.indexOf("Contents:"));
//
//                String quantity = getData(coreQty);
//                if (quantity != null && quantity.trim().length() > 0 && getNumeric(quantity) > 0) {
//                    entry.add(CHAR_L);
//                    entry.add(getData(coreItmNum));
//                    entry.add(description.trim());  //  "Multitasking Essentials - Best Sellers Kit"
//                    entry.add("PURCO");
//                    entry.add(EA_STR);
//                    entry.add(quantity);
//                    entry.add(getValue(distCost, evaluator));
//                    entry.add(ZERO);
//                    entry.add(EMPTY_STR);
//                    entry.add(EMPTY_STR);
//                    entry.add(EMPTY_STR);
//                    ab.append(entry).append(NEW_LINE_STR);
//                }
//            }
//        }
//        return sb.toString();
    //}

    public String populateHeader(String[] fileName) {
        StringJoiner header = new StringJoiner(TILDE);
        fileName.toString();
        String name = fileName[2];
        name = name.substring(0, name.indexOf(DOT));
        header.add(CHAR_E);
        header.add("PURCO");//Sales site
        header.add("PBLK");//Order type
        header.add(EMPTY_STR);// (blank)
        header.add(name); // Order number
        header.add(fileName[1]); // Date
        header.add(fileName[1]); // Date
        header.add("PURCO"); // Sales Site
        header.add(US_CURR); // Currency type
        // Loop to add 26 empty strings ('~') for the correct ifile format
        for (int i = 0; i < 26; i++) {
            header.add(EMPTY_STR);
        }
//        header.add(shipVia);
//        header.add(shipVia);
        header.add(x3Database.getPaymentTerms(cust_numType));
        return header.toString();
    }

    private String currentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.now();
//		LocalDate date = LocalDate.parse(today, formatter);
        String td = date.format(formatter);
        return td;
    }

    private String removeExtention(String fileName) {
        return fileName.substring(0, fileName.indexOf(DOT));
    }


}