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
public class BoxesExportProcess implements BrandOrderForms, AppConstants {

    Logger log = LoggerFactory.getLogger(BoxesExportProcess.class);

    @Autowired
    X3BPCustomerDao x3BPCustomerDao;

//    @Autowired
//    EmailProcessor emailProcessor;

    // Map Object to hold every column name, and it's position in the Order Form
    static Map<Integer, Map<String, Integer>> siteIndexMap = new HashMap<>();
    static Map<String, Integer> colName1_Ebay = new HashMap<>();
    static Map<String, Integer> colName = new HashMap<>();
    static {
        //Map the fields
        colName.put(SAL_FCY, 0);
        colName.put(SOH_TYP, 1);
        colName.put(ORD_NUM, 2);
        colName.put(SOLD_TO, 3);
        colName.put(ORD_DAT, 4);
        colName.put(CUST_ORDREF, 5);
        colName.put(SITE_2, 6);
        colName.put(CUR, 7);
        colName.put(BPI_NAM_0, 13);
        colName.put(BPI_ADD1, 15);
        colName.put(BPI_ADD2, 16);
        colName.put(BPI_ZIP, 17);
        colName.put(BPI_CTY, 18);
        colName.put(BPI_STAT, 19);
        colName.put(BPD_NAM0, 20);
        colName.put(BPD_ADD_1, 22);
        colName.put(BPD_ADD_3, 23);
        colName.put(BPD_ZIP, 24);
        colName.put(BPD_CITY, 25);
        colName.put(BPD_STAT, 26);
        colName.put(INV_AMT, 27);
        colName.put(INV_AMT_1, 29);
        colName.put(SHIP_FIRST, 30);
        colName.put(SHIP_SECOND, 31);
        colName.put(ITM_REF, 33);
        colName.put(ITM_DESC, 34);
        colName.put(QTY_0, 36);
        colName.put(NET_PRICE, 37);

        //----PUR-EBAY-ORDER-FORMS----\\
        colName1_Ebay.put(SALFCY, 0);
        colName1_Ebay.put(SOHTYP, 1);
        colName1_Ebay.put(SOHNUM, 2);
        colName1_Ebay.put(SOLDTO, 3);
        colName1_Ebay.put(ORDDAT, 4);
        colName1_Ebay.put(CUSORDREF, 5);
        colName1_Ebay.put(CUR_PEBAY, 6);
        colName1_Ebay.put("PJT", 7);//--EMPTY-COLUMN
        colName1_Ebay.put(BPINAM_0, 8);
        colName1_Ebay.put(BPIADD1, 9);
        colName1_Ebay.put(BPIADD2, 10);
        colName1_Ebay.put(BPIZIP, 11);
        colName1_Ebay.put(BPICTY, 12);
        colName1_Ebay.put(BPISTAT, 13);
        colName1_Ebay.put(BPINAM0, 14);
        colName1_Ebay.put(BPDNAM0, 15);
        colName1_Ebay.put(BPDADD_1, 16);
        colName1_Ebay.put(BPDADD_3, 17);
        colName1_Ebay.put(BPDZIP, 18);
        colName1_Ebay.put(BPDCITY, 19);
        colName1_Ebay.put(BPDSTAT, 20);
        colName1_Ebay.put(INVAMT, 21);
        colName1_Ebay.put(INVAMT_2, 22);
        colName1_Ebay.put(INVAMT_1, 23);
        colName1_Ebay.put(SHIPFIRST, 24);
        colName1_Ebay.put(SHIPSECOND, 25);
        colName1_Ebay.put(ITMREF, 26);
        colName1_Ebay.put(ITMDESC, 27);
        colName1_Ebay.put(UOM, 28);
        colName1_Ebay.put(QTY0, 29);
        colName1_Ebay.put(NETPRICE, 30);

    }

    public String flag = "";
    public String flag_2 = "";
    public boolean skipRow;
//    static Map<String, Integer> purFormMap = new HashMap<>();
//    static {
//        purFormMap.put(SAL_FCY, 0);
//        purFormMap.put(SOH_TYP, 1);
//        purFormMap.put()
//    }

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
            exchange.setProperty("ORDER-TYPE", flag);
            String custNum = exchange.getProperty(CUST_NUMBER).toString();
            String formDate = "";
            if(exchange.getProperty(ORDER_DATE) != null) {
                formDate = exchange.getProperty(ORDER_DATE).toString();
            } else {
                formDate = currentDate();
            }

            System.out.println("Output data is : "+ txtFileData);
            index++;

            if (txtFileData != null) { // Ensures the current sheet is valid/contains data
                exchange.setProperty(CSV_DATA, txtFileData.replace(TILDE, COMMA)); //Removed '.txt' for the '.csv' output file name
                exchange.getMessage().setBody(txtFileData); // Sets the exchange value as the new formatted data in this Processor class
                exchange.setProperty("IFILE", txtFileData);
                exchange.getMessage().setHeader(Exchange.FILE_NAME,  formDate + "_40Boxes" + index + DOT_TXT); //Formats '.txt' file with today's date    --   + "_" + custNum
//                exchange.setProperty(SITE_NAME, site);
                exchange.setProperty(BOXES, "BOXES");
                exchange.setProperty(BRAND, "40_BOXES");
                exchange.setProperty(SITE_NAME, "BOXES");
                exchange.setProperty(IS_DATA_PRESENT, true);
            } else {
                exchange.setProperty(IS_DATA_PRESENT, false);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }




    /*------------------------------------------------------------------------------------
        Function to build the structure for the files || Calls two local functions
        to populate the StringBuilder with the sheet's cell values in a specific format
        for X3.
    *******--Builds/Populates a String formatted with all header lines & product info lines from the sheet--*******
     ------------------------------------------------------------------------------------------------------------*/
    private String populateTxtString(Sheet firstSheet, StringBuilder txtFileBuilder) {

        boolean skipHeader = true;

        String tmpPO = EMPTY_STR; // Order #
//        System.out.println("String builder is : "+ txtFileBuilder.toString());
//        System.out.println("Number of cells are : "+firstSheet);
        DataFormatter df = new DataFormatter();
        List<String> productSKUs = new ArrayList<>();

        for(Row row : firstSheet) {
            ArrayList<Cell> cells = new ArrayList<>();
            Iterator<Cell> cellIterator = row.cellIterator();
            cellIterator.forEachRemaining(cells::add);
            Cell poNum = row.getCell(colName.get(ORD_NUM));
            Cell emailCell = row.getCell(1);
//            String e = getData(emailCell);
//            emailProcessor.getFormEmail(e);
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
//                    System.out.println("row value is "+row.getCell(0));
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

    private String setFlagTwo(Cell ordSite) {
        String e = getData(ordSite);
        e = e.toUpperCase();
        switch(e) {
            case "PGMA":
                return this.flag_2 = "PGMA";
            case "BGMA":
                return this.flag_2 = "BGMA";
            case "PEBAY":
                return this.flag_2 = "PEBAY";
            default:
                return this.flag_2 = "SOMETHING WENT WRONG";
        }
    }

    private String getFlagTwo() {
        if(this.flag_2.length() > 0 && !this.flag_2.isEmpty()) {
            return this.flag_2;
        } else {
            return "EMPTY";
        }
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

//        // Determine which brand
//        Cell sellerId = row.getCell(colName.get(SELLER_ID));
//        getFormType(sellerId)Z
        if(getFlagTwo().length() > 0 && getFlagTwo().startsWith("P") && getFlagTwo().endsWith("Y")) {

            //--PUR-EBAY-CELL-MAPPINGS--\\
            Cell itmRef = row.getCell(colName.get(ITMREF));
            Cell itmDesc = row.getCell(colName.get(ITMDESC));
            Cell uom = row.getCell(colName.get(UOM));
            Cell qty0 = row.getCell(colName.get(SHIPSECOND));
            Cell netPrice_1 = row.getCell(colName.get(NETPRICE));

            StringJoiner lineBuilder = new StringJoiner("~");
            System.out.println("Line builder is : "+ lineBuilder.toString() + "\n");
            lineBuilder.add("L");
            lineBuilder.add(getData(itmRef)); //ITM_REF #
            lineBuilder.add(getData(itmDesc)); //Description
            lineBuilder.add("EA"); //Sales Unit
            lineBuilder.add(getData(qty0)); //Quantity
            lineBuilder.add(getData(netPrice_1)); //Base Price
            for(int a = 0; a <= 5; a++) {
                lineBuilder.add(EMPTY_STR);
            }
            return lineBuilder.toString();
        } else {

            Cell itemRefNum = row.getCell(colName.get(ITM_REF));
            Cell description = row.getCell(colName.get(ITM_DESC));
            Cell qty = row.getCell(colName.get(QTY_0));
            Cell netPrice = row.getCell(colName.get(NET_PRICE));

            StringJoiner lineBuilder = new StringJoiner("~");
            System.out.println("Line builder is : "+ lineBuilder.toString() + "\n");
            lineBuilder.add("L");
            lineBuilder.add(getData(itemRefNum)); //ITM_REF #
            lineBuilder.add(getData(description)); //Description
            lineBuilder.add("EA"); //Sales Unit
            lineBuilder.add(getData(qty)); //Quantity
            lineBuilder.add(getData(netPrice)); //Base Price
            for(int a = 0; a <= 5; a++) {
                lineBuilder.add(EMPTY_STR);
            }
            return lineBuilder.toString();
        }
        // Get the PRICE from X3
//        Cell price = row.getCell(colName.get(UNIT_COST));
//		System.out.println("row is : "+cellIterator); // - Unnecessary - Program takes longer with this line
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

        this.flag_2 = setFlagTwo(row.getCell(1));




        //---------CELL OBJECTS TO HOLD A CELL'S VALUE FOR THE POSITION OF COLUMN NAMES---------


        StringJoiner headerBuilder = new StringJoiner("~");

        if(this.flag_2.equalsIgnoreCase("PEBAY")) {
            //-------PUR-EBAY-FORM-CELL-OBJECTS------\\

            Cell site1 = row.getCell(colName.get(SALFCY));
            Cell ordType1 = row.getCell(colName.get(SOHTYP));
            Cell ordNum1 = row.getCell(colName.get(SOHNUM));
            Cell soldTo1 = row.getCell(colName.get(SOLDTO));
            Cell orderDate1 = row.getCell(colName.get(ORDDAT));
            Cell custOrdNum1 = row.getCell(colName.get(CUSORDREF));
            Cell currency1 = row.getCell(colName.get(CUR_PEBAY));
            Cell bpiName1 = row.getCell(colName.get(BPINAM_0));
            Cell custAdd1 = row.getCell(colName.get(BPIADD1));
            Cell custAdd2 = row.getCell(colName.get(BPIADD2));
            Cell custZip1 = row.getCell(colName.get(BPIZIP));
            Cell custCity1 = row.getCell(colName.get(BPICTY));
            Cell custState1 = row.getCell(colName.get(BPISTAT));
//        Cell custName2 = row.getCell(colName.get(BPINAM0));
            Cell bpdName1 = row.getCell(colName.get(BPDNAM0));
            Cell bpdAdd1 = row.getCell(colName.get(BPDADD_1));
            Cell bpdAdd3 = row.getCell(colName.get(BPDADD_3));
            Cell bpdZip1 = row.getCell(colName.get(BPDZIP));
            Cell bpdCity1 = row.getCell(colName.get(BPDCITY));
            Cell bpdState1 = row.getCell(colName.get(BPDSTAT));
            Cell invAmt1 = row.getCell(colName.get(INVAMT));
            Cell invAmt_12 = row.getCell(colName.get(INVAMT_1));
            Cell invAmt_23 = row.getCell(colName.get(INVAMT_2));
            Cell carrier_1 = row.getCell(colName.get(SHIPFIRST));
            Cell carrier_2 = row.getCell(colName.get(SHIPSECOND));


            headerBuilder.add("E");
            headerBuilder.add(getData(site1)); //Sales Site/BUTCO/PURCO
            headerBuilder.add(getData(ordType1)); //Order Type
            headerBuilder.add(getData(ordNum1)); //Order #
            headerBuilder.add(getData(soldTo1)); //Customer ID or Customer Order #  --  EXAMPLE -
            //E~PURCO~PGMA~FB375042~107793  ---- '107793' = CUSTOMER ID/#
            headerBuilder.add(getData(orderDate1)); //Order Site ID/BUTTER - '460000147'/PUR - '410000042'
            headerBuilder.add(getData(custOrdNum1)); //CUST ORD REF #
            headerBuilder.add(getData(site1)); // SITE

            headerBuilder.add(getData(currency1)); //Currency
            for(int i=0; i<5; i++) {
                headerBuilder.add(EMPTY_STR);
            }
            headerBuilder.add(getData(bpiName1)); //Customer's Name
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(getData(custAdd1)); // Cust Address 1
            headerBuilder.add(getData(custAdd2));
            headerBuilder.add(getData(custZip1)); //Bill country
            headerBuilder.add(getData(custCity1)); //Customer city
            headerBuilder.add(getData(custState1)); //Customer State
            headerBuilder.add(getData(bpdName1)); //Customer Name
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(getData(bpdAdd1));
            headerBuilder.add(getData(bpdAdd3));
            headerBuilder.add(getData(bpdZip1));
            headerBuilder.add(getValue(bpdCity1));
            headerBuilder.add(getData(bpdState1));
            headerBuilder.add(getData(invAmt1));
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(getData(invAmt_12));
            headerBuilder.add(getData(carrier_1));
            headerBuilder.add(getData(carrier_2));
            return headerBuilder.toString();
        } else {
            Cell site = row.getCell(colName.get(SAL_FCY));
            Cell ordType = row.getCell(colName.get(SOH_TYP));
            Cell soldTo = row.getCell(colName.get(SOLD_TO));
            Cell ordNum = row.getCell(colName.get(ORD_NUM));
            Cell orderDate = row.getCell(colName.get(ORD_DAT));
            Cell custOrdNum = row.getCell(colName.get(CUST_ORDREF));
            Cell site_2 = row.getCell(colName.get(SITE_2));
            Cell currency = row.getCell(colName.get(CUR));
            Cell custName = row.getCell(colName.get(BPI_NAM_0));
            Cell address = row.getCell(colName.get(BPI_ADD1));
            Cell address_2 = row.getCell(colName.get(BPI_ADD2));
            Cell zip = row.getCell(colName.get(BPI_ZIP));
            Cell custCity = row.getCell(colName.get(BPI_CTY));
            Cell custState = row.getCell(colName.get(BPI_STAT));
            Cell bpdName = row.getCell(colName.get(BPD_NAM0));
            Cell bpdAddress_1 = row.getCell(colName.get(BPD_ADD_1));
            Cell bpdAddress_2 = row.getCell(colName.get(BPD_ADD_3));
            Cell bpdZip = row.getCell(colName.get(BPD_ZIP));
            Cell bpdCity = row.getCell(colName.get(BPD_CITY));
            Cell bpdState = row.getCell(colName.get(BPD_STAT));
            Cell invAmt = row.getCell(colName.get(INV_AMT));
            Cell invAmt_1 = row.getCell(colName.get(INV_AMT_1));
            Cell deliveryMode = row.getCell(colName.get(SHIP_FIRST));
            Cell carrier = row.getCell(colName.get(SHIP_SECOND));

            typeEmail(ordType);

            String zipAreaCode = getData(zip);
            String zipAreaCode2 = getData(bpdZip);

            if(zipAreaCode.length() < 5) {
                zipAreaCode = "0" + zipAreaCode;
            }
            if(zipAreaCode2.length() < 5) {
                zipAreaCode2 = "0" + zipAreaCode2;
            }

            headerBuilder.add("E");
            headerBuilder.add(getData(site)); //Sales Site/BUTCO/PURCO
            headerBuilder.add(getData(ordType)); //Order Type
            headerBuilder.add(getData(ordNum)); //Order #
            headerBuilder.add(getData(soldTo)); //Customer ID or Customer Order #  --  EXAMPLE -
            //E~PURCO~PGMA~FB375042~107793  ---- '107793' = CUSTOMER ID/#
            headerBuilder.add(getData(orderDate)); //Order Site ID/BUTTER - '460000147'/PUR - '410000042'
            headerBuilder.add(getData(custOrdNum)); //CUST ORD REF #
            headerBuilder.add(getData(site_2)); // SITE

            headerBuilder.add("USD"); //Currency
            for(int i=0; i<5; i++) {
                headerBuilder.add(EMPTY_STR);
            }
            headerBuilder.add(getData(custName)); //Customer's Name
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(getData(address)); // Cust Address 1
            headerBuilder.add(getData(address_2));
            headerBuilder.add(zipAreaCode); //Bill country
            headerBuilder.add(getData(custCity)); //Customer city
            headerBuilder.add(getData(custState)); //Customer State
            headerBuilder.add(getData(bpdName)); //Customer Name
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(getData(bpdAddress_1));
            headerBuilder.add(getData(bpdAddress_2));
            headerBuilder.add(zipAreaCode2);
            headerBuilder.add(getValue(bpdCity));
            headerBuilder.add(getData(bpdState));
            headerBuilder.add(getData(invAmt));
            headerBuilder.add(EMPTY_STR);
            headerBuilder.add(getData(invAmt_1));
            headerBuilder.add(getData(deliveryMode));
            headerBuilder.add(getData(carrier));
            return headerBuilder.toString();
        }
    }



    /*--------------------------------------------------------------------------
        Function to take the value of 'Vendor SKU' column & compare it to a value
        which will assign the relevant Order Site ID, Sales Site ID, and the corresponding
        Order Type.
    ----------------------------------------------------------------------------*/
//    private void getSite(Row row) {
//        //Get Vendor SKU Column/Row value
//        Cell ven_sku = row.getCell(colName.get(VEN_SKU));
//
//        int venSku;
//        venSku = Integer.parseInt(getData(ven_sku));
//        try {
//            if ((venSku >= 950000000 && venSku <= 959999999) || (venSku >= 911700000 && venSku <= 911700050)) {
//                id = "410000042";
//                orderType = "PQVCD";
//                site = "PURCO";
//            } else if(venSku >= 920000000 && venSku <= 929999999) {
//                id = "460000147";
//                orderType = "BQVCD";
//                site = "BUTCO";
//            } else if (venSku >= 960000000 && venSku <= 969999999) {
//                id = "460000139";
//                orderType = "LQVCD";
//                site = "LYSCO";
//            }
//        } catch (Exception e) {
//            System.out.println("Wrong Vendor SKU Entry");
//        }
//    }
    private String getDate(String date) {
        System.out.println("Date is : "+date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        LocalDate ld = LocalDate.parse(date,formatter);
        //System.out.println("Date is : "+ld.getMonthValue()+" "+ld.getDayOfMonth()+" "+ld.getYear());
        return ld.getYear()+""+(ld.getMonthValue()<10?("0"+ld.getMonthValue()):ld.getMonthValue())+""+(ld.getDayOfMonth()<10?("0"+ld.getDayOfMonth()):ld.getDayOfMonth());
    }

    public String salesTypeEmail(String type) {
        if(type.equalsIgnoreCase("PURCO")) {
            return "darmstrong@astralbrands.com;jschirm@astralbrands.com";
        } else if (type.equalsIgnoreCase("BUTCO")) {
            return "gallen@astralbrands.com;jschirm@astralbrands.com";
        } else {
            return "jschirm@astralbrands.com";
        }
    }



    public void typeEmail(Cell ordType) {
        String orderType1 = getData(ordType).toString().trim();
        if(orderType1.equalsIgnoreCase("PGMA")) {
            this.flag = "darmstrong@astralbrands.com;";
        } else if (orderType1.equalsIgnoreCase("BGMA")) {
            this.flag = "gallen@astralbrands.com;";
        }
        else {
            this.flag = "rschonfeld@astralbrands.com";
        }
    }

    private String currentDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        LocalDate date = LocalDate.now();
//		LocalDate date = LocalDate.parse(today, formatter);
        String td = date.format(formatter);
        return td;
    }

}
