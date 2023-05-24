package com.astralbrands.orders.process;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.astralbrands.orders.constants.AppConstants;
import com.astralbrands.orders.dao.X3BPCustomerDao;

/*------------------------------------------------------------
	This Processor takes the input file and formats
	a text file containing orders from the input file. It formats the data
	in an 'IFILE' format in a TXT file for X3. It stores the same data to
	later be processed into a new '.csv' file
 -----------------------------------------------------------------------*/
@Component
public class NYKKAProcessor implements BrandOrderForms, AppConstants {

    Logger log = LoggerFactory.getLogger(NYKKAProcessor.class);

    @Autowired
    X3BPCustomerDao x3BPCustomerDao;

    // Map Object to hold every column name, and it's position in the Order Form
    static Map<Integer, Map<String, Integer>> siteIndexMap = new HashMap<>();
    static Map<String, Integer> colName1_but = new HashMap<>();
    static Map<String, Integer> colName = new HashMap<>();
    static {
        //Map the fields
        colName.put(ORDER_NO, 0);
        colName.put(WEB_ORD_NO, 1);
        colName.put(SELLER_ID, 3);
        colName.put(ISBN_UPC, 8);
        colName.put(ORDER_DATE_1, 9);
        colName.put(CUST_NAME, 10);
        colName.put(SKU, 12);
        colName.put(SKU_NAME, 14);
        colName.put(LINE_AMT, 19);
        colName.put(QTY_EXPORT, 20);
        colName.put(BUYER_CITY, 23);
        colName.put(BUYER_STATE, 24);
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
    static String siteID;
    static String siteName;
    static String sellerID;
    static String id;
    static String orderType;
    static String site;
    static int index = 0;

    @Override
    public void process(Exchange exchange, String site, String[] fileNameData) {
        try {
            InputStream inputStream = exchange.getIn().getBody(InputStream.class);
            Workbook workbook = new XSSFWorkbook(inputStream); // Object to hold multiple sheets for processing
            StringBuilder txtFileBuilder = new StringBuilder();
            Sheet firstSheet = workbook.getSheetAt(0); // Gets the first sheet from the Workbook Object
            String txtFileData = populateTxtString(firstSheet,txtFileBuilder); // Processes the sheet and formats the data for the new TXT & CSV files
            String today = currentDate(); // Returns the current date when this program runs
            System.out.println("Output data is : "+ txtFileData);
            index++;

            if (txtFileData != null) { // Ensures the current sheet is valid/contains data
                exchange.setProperty(CSV_DATA, txtFileData.replace(TILDE, COMMA)); //Removed '.txt' for the '.csv' output file name
                exchange.getMessage().setBody(txtFileData); // Sets the exchange value as the new formatted data in this Processor class
                exchange.setProperty("IFILE", txtFileData);
                exchange.getMessage().setHeader(Exchange.FILE_NAME,  today + "_NYKKA" + index + DOT_TXT); //Formats '.txt' file with today's date
                exchange.setProperty(SITE_NAME, site);
//                exchange.setProperty(NYKKA, "NYKKA");
                exchange.setProperty(BRAND, "NYKKA_BRAND");
                exchange.setProperty(IS_DATA_PRESENT, true);
            } else {
                exchange.setProperty(IS_DATA_PRESENT, false);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void getFormType(Cell cell) {
        String cellValue = getValue(cell);

        if(cellValue.equalsIgnoreCase("PCNGS")) {
            site = "PUR";
            siteID = "19999991";
            siteName = "PURCO";
            sellerID = "PNYK";
        } else if(cellValue.equalsIgnoreCase("CLNGS")) {
            site = "COS";
            siteID = "29999991";
            siteName = "COSCO";
            sellerID = "CNYK";
        } else if(cellValue.equalsIgnoreCase("BNYK")) {
            site = "BUT";
            siteID = "69999991";
            siteName = "BUTCO";
            sellerID = "BNYK";
        }
    }

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
    private String populateTxtString(Sheet firstSheet, StringBuilder txtFileBuilder) {

        boolean skipHeader = true;

        String tmpPO = EMPTY_STR; // Order #
        System.out.println("String builder is : "+ txtFileBuilder.toString());
        System.out.println("Number of cells are : "+firstSheet);
        DataFormatter df = new DataFormatter();
        Integer scale = 13;
        Integer precision = 27;
//        BigDecimal bd = new BigDecimal(scale, precision);
        List<String> productSKUs = new ArrayList<>();



        for(Row row : firstSheet) {
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);
            Cell poNum = row.getCell(colName.get(ORDER_NO));
            //cells.size();
            System.out.println(cells.size());
            if(cells.size()>0) { // Starts after the row with column names
                if(skipHeader) {
                    skipHeader=false;
                }
                // If a product has the same Order# then add product info under same customer line
                else if(tmpPO.equals(getData(poNum))){
                    txtFileBuilder.append(getOrderLine(row));
                    //System.out.println("String builder is : "+ txtFileBuilder.toString());
                    txtFileBuilder.append(NEW_LINE_STR);
                    tmpPO = getData(poNum); // Order #
                    System.out.println("tmpPo number is : "+ tmpPO);
                }
                // Builds the formatted data from the Excel sheet in a String
                else {
                    System.out.println("row value is "+row.getCell(0));
                    txtFileBuilder.append(getHeader(row)); // Builds the customer info line
                    //System.out.println("String builder is : "+ txtFileBuilder.toString()); //Duplicate line
                    txtFileBuilder.append(NEW_LINE_STR);
                    txtFileBuilder.append(getOrderLine(row)); // Builds the corresponding product info line
                    System.out.println("String builder is : "+ txtFileBuilder.toString());
                    txtFileBuilder.append(NEW_LINE_STR);
                    tmpPO = getData(poNum); // Order #
                    System.out.println("tmpPo number is : "+ tmpPO);
                }
            }
        }
        System.out.println("Text file is : "+txtFileBuilder.toString());
        return txtFileBuilder.toString();
    }


    /*---------------------------------------------------------------------------------------------------------------
        Iterates through cells pertaining to Product information in the given row and adds the values to an ArrayList
        Returns a formatted String separated by '~' after each value obtained from each cell
        in the current row of the Excel Sheet
        ----------Builds the product info line in the TXT file-----------
     --------------------------------------------------------------------------------------------------------------*/
    private String getOrderLine(Row row) {

        ArrayList<Cell> cells = new ArrayList<>();
        Iterator<Cell> cellIterator = row.cellIterator();
        cellIterator.forEachRemaining(cells::add);

        // Determine which brand
        Cell sellerId = row.getCell(colName.get(SELLER_ID));
        Cell skuCode = row.getCell(colName.get(ISBN_UPC));
        Cell description = row.getCell(colName.get(SKU_NAME));
        Cell qty = row.getCell(colName.get(QTY_EXPORT));

        getFormType(sellerId);
        String baspri = "00";
        String sku = getValue(skuCode);

        // CALL DB OBJECT & EXECUTE QUERY
        baspri = x3BPCustomerDao.getBasePrice(sku);
        if(baspri != null) {
            baspri = baspri.substring(0, 2);
        } else {
            baspri = "00";
        }


        StringJoiner lineBuilder = new StringJoiner("~");
        System.out.println("Line builder is : "+ lineBuilder.toString() + "\n");
        lineBuilder.add("L");
        lineBuilder.add(getValue(skuCode)); //SKU
        lineBuilder.add(getValue(description)); //Description
        lineBuilder.add("EA"); //Sales Unit
        lineBuilder.add(getValue(qty)); //Quantity
        lineBuilder.add(baspri); //Base Price
//		lineBuilder.add("0" + getValue(cells.get(24)).replace(".", "") + "0"); //Gross price - Formatting the price section with no '.' and a '0' at the beginning and end
        lineBuilder.add(EMPTY_STR);
        lineBuilder.add(EMPTY_STR);
        lineBuilder.add(EMPTY_STR);
        System.out.println(lineBuilder.toString());
        return lineBuilder.toString();
    }

    // Formats a cell's data value within a row/column of an Excel sheet
    public String getData(Cell cell) {
        return new DataFormatter().formatCellValue(cell);
    }

    // Converts a Cell object value to a String
    private String getValue(Cell cell) {
        System.out.println("Value is : "+cell.toString());
        String value = getData(cell);
//		System.out.println("Value is : "+value);
        if(value.toString().equalsIgnoreCase("N/A")) {
            return EMPTY_STR;
        }
        return value.toString();
    }


    /*--------------------------------------------------------------------------------
        Obtains the customer's information along with the order number, Shipping site,
        Sales Site, Order Type, Customer order reference, Date, and currency.
        Formatted in a specific order for X3
        ----------Builds each header line in the TXT file-----------
     -------------------------------------------------------------------------------*/
    private String getHeader(Row row) {

        ArrayList<Cell> cells = new ArrayList<>();
        Iterator<Cell> cellIterator = row.cellIterator();
        cellIterator.forEachRemaining(cells::add);

        Cell brand = row.getCell(colName.get(SELLER_ID));
        getFormType(brand);

//		Cell description = row.getCell(colName.get(SKU_NAME));
//        Cell ven_sku = row.getCell(colName.get(SKU));
        Cell orderNumber = row.getCell(colName.get(ORDER_NO));
        Cell orderDate = row.getCell(colName.get(ORDER_DATE_1));
        Cell webOrderNo = row.getCell(colName.get(WEB_ORD_NO));
        Cell custName = row.getCell(colName.get(CUST_NAME));
        Cell custCity = row.getCell(colName.get(BUYER_CITY));
        Cell custState = row.getCell(colName.get(BUYER_STATE));
        String date = getValue(orderDate);
        date = date.substring(0, 10);
//        date = getDate(date);


        //---------CELL OBJECTS TO HOLD A CELL'S VALUE FOR THE POSITION OF COLUMN NAMES---------


        StringJoiner headerBuilder = new StringJoiner("~");
        System.out.println("headerBuilder is : "+ headerBuilder.toString());
        //System.out.println(Header is : "+row);
        headerBuilder.add("E");
        headerBuilder.add(siteName); //Sales Site/BUTCO/PURCO
        headerBuilder.add(sellerID); //Order Type/BQVCD/PQVCD
        headerBuilder.add(getValue(orderNumber)); //PO number
        headerBuilder.add(siteID); //Order Site ID/BUTTER - '460000147'/PUR - '410000042'
        headerBuilder.add(getDate(date)); //Date
        headerBuilder.add(getValue(webOrderNo)); //Customer order reference
        headerBuilder.add(siteName); // Sales Site
        headerBuilder.add("USD"); //Currency
        for(int i=0; i<5; i++) {
            headerBuilder.add(EMPTY_STR);
        }
        headerBuilder.add(getValue(custName)); //Bill firstName
        headerBuilder.add(EMPTY_STR); //Bill lastName
        headerBuilder.add("IN"); //Bill country
        for(int i=0; i<3; i++) {
            headerBuilder.add(EMPTY_STR);
        }
        headerBuilder.add(getValue(custCity)); //Customer city
        headerBuilder.add(getValue(custState)); //Customer State
        headerBuilder.add(getValue(custName)); //Customer Name
        headerBuilder.add(EMPTY_STR);
        headerBuilder.add("IN"); //Customer state
        headerBuilder.add(EMPTY_STR); //Ship LastName
        headerBuilder.add(EMPTY_STR); //Ship LastName
        headerBuilder.add(EMPTY_STR);
        headerBuilder.add(getValue(custCity)); //Ship LastName
        headerBuilder.add(getValue(custState)); //Ship firstname

        headerBuilder.add("NC"); //Ship country
        headerBuilder.add("NC"); //Ship Add 1
        headerBuilder.add(EMPTY_STR);
        headerBuilder.add(EMPTY_STR);
        headerBuilder.add(EMPTY_STR);
        headerBuilder.add("NET30WTR");
        System.out.println(headerBuilder.toString());
        return headerBuilder.toString();
    }

    /*--------------------------------------------------------------------------
        Function to take the value of 'Vendor SKU' column & compare it to a value
        which will assign the relevant Order Site ID, Sales Site ID, and the corresponding
        Order Type.
    ----------------------------------------------------------------------------*/
    private void getSite(Row row) {
        //Get Vendor SKU Column/Row value
        Cell ven_sku = row.getCell(colName.get(VEN_SKU));

        int venSku;
        venSku = Integer.parseInt(getData(ven_sku));
        try {
            if ((venSku >= 950000000 && venSku <= 959999999) || (venSku >= 911700000 && venSku <= 911700050)) {
                id = "410000042";
                orderType = "PQVCD";
                site = "PURCO";
            } else if(venSku >= 920000000 && venSku <= 929999999) {
                id = "460000147";
                orderType = "BQVCD";
                site = "BUTCO";
            } else if (venSku >= 960000000 && venSku <= 969999999) {
                id = "460000139";
                orderType = "LQVCD";
                site = "LYSCO";
            }
        } catch (Exception e) {
            System.out.println("Wrong Vendor SKU Entry");
        }
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