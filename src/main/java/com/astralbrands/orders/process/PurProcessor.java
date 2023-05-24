package com.astralbrands.orders.process;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.constants.AppConstants;
import com.astralbrands.orders.dao.X3BPCustomerDao;



@Component
public class PurProcessor implements BrandOrderForms, AppConstants{

//    Logger log = LoggerFactory.getLogger(MockProcessor.class);

    @Autowired
    X3BPCustomerDao x3BPCustomerDao;

    /*
        Static declaration of a Map object to hold key/value pairs for
        the column's name in the Excel order form sheet, and it's position.

        -------In case Oder Form changes format - Easy to modify program-------
     */
    static Map<String, Integer> colName = new HashMap<>();
    static {
        colName.put(INS_DATE, 0); // Not used
        colName.put(ORDER_DATE, 1);
        colName.put(STATUS, 2); // Not used
        colName.put(PO_NUM, 3);
        colName.put(CUST_ORD_NUM, 4);
        colName.put(BILL_NAME, 5);
        colName.put(BILL_ADD, 6);
        colName.put(BILL_ADD2, 7);
        colName.put(BILL_CITY, 8);
        colName.put(BILL_ST, 9);
        colName.put(BILL_ZIP, 10);
        colName.put(BILL_COUNTRY, 11);
        colName.put(SHIP_NAME, 12);
        colName.put(SHIP_ADD, 13);
        colName.put(SHIP_ADD2, 14);
        colName.put(SHIP_CITY, 15);
        colName.put(SHIP_STATE, 16);
        colName.put(SHIP_ZIP, 17);
        colName.put(SHIP_COUNTRY, 18);
        colName.put(TAX, 19); // Not used
//        colName.put(TAX_CURRENCY, 20); // Not used
        colName.put(VEN_SKU, 20);
        colName.put(DESCRIPTION, 21);
        colName.put(QTY_HUB, 22);
        colName.put(UNIT_COST, 23);
    }

    static int index = 0;


    @Override
    public void process(Exchange exchange, String site, String[] fileNameData) {
        try {
            InputStream inputStream = exchange.getIn().getBody(InputStream.class);
            Workbook workbook = new XSSFWorkbook(inputStream); // For future implementation of processing multiple sheets
            StringBuilder st = new StringBuilder();
            int numSheets = workbook.getNumberOfSheets();
             // For future implementation of processing multiple sheets

            for (int i = 0; i < numSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                populateString(sheet, st);
            }
            String data = st.toString();
            String today = currentDate();
            index++;
            // Ensures there is data in the current Excel Sheet Order Form
            if(data.length() > 0) {
                exchange.getMessage().setBody(data); // Exchange data is set to the data processed in this class
                exchange.setProperty(CSV_DATA, data.replace(TILDE, COMMA)); // CSV file data for later processing
//                exchange.setProperty(INPUT_FILE_NAME, "CommerceHub(PUR)_" + index); // 1st implementation of CSV file
                exchange.setProperty("IFILE", data);
                exchange.getMessage().setHeader(Exchange.FILE_NAME, today + "_PurCosmetics" + index + DOT_TXT); // Formatting the TXT file
                exchange.setProperty(IS_DATA_PRESENT, true);
                exchange.setProperty(SITE_TWO, fileNameData[0]);
                exchange.setProperty(LOCAL_ORDER, LOCAL);
            } else {
                exchange.setProperty(IS_DATA_PRESENT, false);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    /*
        Builds/Populates a String with the properly formatted info from processing
        the current Order form Excel Sheet. It uses two functions for obtaining
        product info line and the customer info line.
     */
    private String populateString(Sheet sheet, StringBuilder st){  // int pageIndex
        boolean skipHeader = true;

        String orderNum = EMPTY_STR; // Variable to hold the Order # column's value

        for(Row row : sheet) {
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);
            Cell c1 = row.getCell(colName.get(PO_NUM)); // Gets the value for the current row's Order # column
            String OrderNum = getData(c1); // Formats that value into a String
            // Starts retrieving data after the first couple rows in the Sheet - skips unnecessary info
            if(cells.size() > 3) {
                if(skipHeader) {
                    skipHeader = false;
                }
                // If line has same Order # as another, add under the same customer header line
                else if (orderNum.equals(getData(c1))) {
                    st.append(getProdOrders(row));
                    st.append(NEW_LINE_STR);
                    orderNum = getData(c1); // New Order #
                }
                // Obtains both the customer's info and product info on the current row
                else {
//                    Object qVal = getStringValue(cells.get(6));
//                    double qty = 0;
//                    if(qVal instanceof Double) {
//                        qty = (Double) qVal;
//                    }
//                    if(qty > 0) {
                        st.append(getProdCustInfo(row)); // Customer's info header line
                        st.append(NEW_LINE_STR);
                        st.append(getProdOrders(row)); // Customer's products ordered
                        st.append(NEW_LINE_STR);
                        orderNum = getData(c1); // Order #
//                    }
                }
            }
        }
        System.out.println("Current File is : " + st.toString());
        return st.toString();
    }

    // Simple function to format a cell's data value
    public String getData(Cell cell) {
        return new DataFormatter().formatCellValue(cell);
    }

    // Formats a cell's data value and returns it as a String
    private String getStringValue(Cell cell) {
        System.out.println("Value is : " + cell.toString());
        String value = getData(cell); // Formats the cell's value into a String
        if(value.toString().equalsIgnoreCase("N/A")) {
            return EMPTY_STR;
        }
        return value.toString();
    }

    // Retrieves the product's information for the current row in the Excel sheet Order form
    private String getProdOrders(Row row) {
        ArrayList<Cell> cells = new ArrayList<>(); // Holds every cell's value in the current row
        Iterator<Cell> cellIterator = row.cellIterator(); // Iterates through the entire row
        cellIterator.forEachRemaining(cells::add); // Adds each cell value to the ArrayList
//        Map<String, Integer> curSheet = colName;
        // Cell objects to hold a cell's value for the specified column in the current row
        Cell sku = row.getCell(colName.get(VEN_SKU)); // Product's SKU #
        Cell desc = row.getCell(colName.get(DESCRIPTION)); // Product's description
        Cell qty = row.getCell(colName.get(QTY_HUB)); // Amount ordered
        Cell tCost = row.getCell(colName.get(UNIT_COST)); // Total cost of the order
        StringJoiner lb = new StringJoiner("~");

        lb.add("L");
        lb.add(getData(sku)); // Product SKU #
        lb.add(getData(desc)); // Product Description
        lb.add("PURCO"); // Site
        lb.add(EA_STR); // Sales Unit
        lb.add(getData(qty)); // Quantity
        lb.add(getData(tCost)); // Total Price
        lb.add(ZERO); // Zero
        lb.add(EMPTY_STR); // Empty String
         // Empty String

        return lb.toString();
    }


    // Retrieves the customer's info from current row in the Excel sheet Order Form

    private String getProdCustInfo(Row row) {
        ArrayList<Cell> cells = new ArrayList<>();
        Iterator<Cell> cellIterator = row.cellIterator();
        cellIterator.forEachRemaining(cells::add);
        // Gets the cell's value in the given column for the current row
//        Cell name = row.getCell(colName.get(NAME)); // Gets customer's name
//        Cell street = row.getCell(colName.get(STREET)); // Gets customer's Street address
//        Cell city = row.getCell(colName.get(CITY)); // Gets customer's city
//        Cell shipTo = row.getCell(colName.get(SHIPTO)); // Gets customer's State and Country
//        Cell orderNum = row.getCell(colName.get(ORDER_NUM)); // Gets the customer's Order #
        Cell poNum = row.getCell(colName.get(PO_NUM));
        Cell orderDate = row.getCell(colName.get(ORDER_DATE));
        Cell custOrdNum = row.getCell(colName.get(CUST_ORD_NUM));
        Cell billName = row.getCell(colName.get(BILL_NAME));
        Cell country = row.getCell(colName.get(BILL_COUNTRY));
        Cell billAdd = row.getCell(colName.get(BILL_ADD));
        Cell billAdd2 = row.getCell(colName.get(BILL_ADD2));
        Cell zip = row.getCell(colName.get(BILL_ZIP));
        Cell billCity = row.getCell(colName.get(BILL_CITY));
        Cell billState = row.getCell(colName.get(BILL_ST));
        Cell shipName = row.getCell(colName.get(SHIP_NAME));
        Cell shipCountry = row.getCell(colName.get(SHIP_COUNTRY));
        Cell shipAdd = row.getCell(colName.get(SHIP_ADD));
        Cell shipAdd2 = row.getCell(colName.get(SHIP_ADD2));
        Cell shipZip = row.getCell(colName.get(SHIP_ZIP));
        Cell shipCity = row.getCell(colName.get(SHIP_CITY));
        Cell shipState = row.getCell(colName.get(SHIP_STATE));

        StringJoiner customerLine = new StringJoiner("~");
        customerLine.add("E");
        customerLine.add("PURCO"); // Sales Site
        customerLine.add("PQVCD"); // Order Type
        customerLine.add(getData(poNum)); // Order #
        customerLine.add("410000042");// Customer
        customerLine.add(getDate(getData(orderDate)));
        customerLine.add(getData(custOrdNum));
        customerLine.add("PURCO"); // Sales Site
        customerLine.add("USD"); // Currency
        for(int i = 0; i < 5; i++){
            customerLine.add(EMPTY_STR); // Blank spaces to separate data for x3 format
        }
        customerLine.add(getData(billName)); // Customer's first name
        customerLine.add(EMPTY_STR);
        customerLine.add(getData(country)); // Customer's country
        customerLine.add(getData(billAdd)); // Customer's Address
        customerLine.add(getData(billAdd2)); // Customer's Address_2
        customerLine.add(getData(zip)); // Zip code
        customerLine.add(getData(billCity)); // City
        customerLine.add(getData(billState)); // State
        customerLine.add(getData(shipName)); // Name
        customerLine.add(EMPTY_STR); // Last Name
        customerLine.add(getData(shipCountry));
        customerLine.add(getData(shipAdd));
        customerLine.add(getData(shipAdd2));
        customerLine.add(getData(shipZip));
        customerLine.add(getData(shipCity));
        customerLine.add(getData(shipState));
        customerLine.add(ZERO);
        customerLine.add(ZERO);
        customerLine.add(EMPTY_STR);
        customerLine.add(EMPTY_STR);
        customerLine.add(EMPTY_STR);

        customerLine.add("NET90");

        return customerLine.toString();
    }
    // Obtains the current date and formats it
    private String currentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // Formats the date in the given format
        LocalDate date = LocalDate.now(); // Gets the current date when the program runs
//		LocalDate date = LocalDate.parse(today, formatter);
        String td = date.format(formatter); // Formats the current date and returns the value as a String
        return td;
    }

    private String getDate(String date) {
        System.out.println("Date is : "+date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yy");
        LocalDate ld = LocalDate.parse(date,formatter);
        //System.out.println("Date is : "+ld.getMonthValue()+" "+ld.getDayOfMonth()+" "+ld.getYear());
        return ld.getYear()+""+(ld.getMonthValue()<10?("0"+ld.getMonthValue()):ld.getMonthValue())+""+(ld.getDayOfMonth()<10?("0"+ld.getDayOfMonth()):ld.getDayOfMonth());
    }

}
